package com.example.media.gpu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.TextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live telemetry reported by the OpenGL ES 2.0 GPU filter engine.
 */
data class GpuEngineMetrics(
    val isReady: Boolean = false,
    val hasFailed: Boolean = false,
    val rendererName: String = "OpenGL ES 2.0",
    val lastRenderMs: Float = 0f,
    val uploadedTextureWidth: Int = 0,
    val uploadedTextureHeight: Int = 0
)

/**
 * Real-time GPU-accelerated OpenGL ES 2.0 video filter surface built on TextureView + EGL14.
 *
 * Why TextureView + dedicated EGL14 thread instead of GLSurfaceView:
 * - Participates cleanly in Jetpack Compose clipping (RoundedCornerShape), graphicsLayer
 *   pinch-to-zoom/pan, safe-zone overlays, live scopes, and modal sheets with zero Z-order holes.
 * - Caches the decoded video frame in GPU VRAM (GL_TEXTURE_2D); adjusting any color wheel,
 *   LUT, tone curve, HSL band, 3D relight, or split wipe re-renders directly on GPU in <2ms
 *   with zero CPU bitmap re-upload.
 * - Automatically notifies [onMetricsChanged] with live GPU latency or fallback signal if
 *   EGL initialization is unavailable on the host device.
 */
class CinemaGlFilterTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private val frameLock = Any()
    private var pendingBitmap: Bitmap? = null
    private var pendingFrameKey: Long = -1L
    private var uploadedFrameKey: Long = -1L
    private var uploadedTexWidth: Int = 0
    private var uploadedTexHeight: Int = 0

    @Volatile
    private var currentUniforms: GpuColorGradeUniforms = GpuColorGradeUniforms()

    @Volatile
    private var onMetricsChanged: ((GpuEngineMetrics) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null

    // EGL / GLES20 state (accessed exclusively on glThread)
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var programId: Int = 0
    private var textureId: Int = 0
    private var surfaceW: Int = 0
    private var surfaceH: Int = 0
    private var lastMetricsDispatchMs: Long = 0L
    private var gpuRendererLabel: String = "OpenGL ES 2.0"

    private val renderQueued = AtomicBoolean(false)
    private val isGlReady = AtomicBoolean(false)

    // Cached GLSL attribute & uniform locations
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uTextureLoc = -1
    private var uTexelSizeLoc = -1
    private var uTimeSecLoc = -1
    private var uBypassGradeLoc = -1
    private var uSplitWipePosLoc = -1
    private var uLutScaleLoc = -1
    private var uLutBiasLoc = -1
    private var uLutSaturationLoc = -1
    private var uLutContrastLoc = -1
    private var uExposureLoc = -1
    private var uBrightnessLoc = -1
    private var uContrastLoc = -1
    private var uSaturationLoc = -1
    private var uTemperatureLoc = -1
    private var uTintLoc = -1
    private var uHighlightsLoc = -1
    private var uShadowsLoc = -1
    private var uFadeLoc = -1
    private var uHdrBoostLoc = -1
    private var uDenoiseLoc = -1
    private var uSharpenLoc = -1
    private var uVignetteLoc = -1
    private var uFilmGrainLoc = -1
    private var uHalationLoc = -1
    private var uSkinSmoothLoc = -1
    private var uFaceGlowLoc = -1
    private var uShadowWheelLoc = -1
    private var uMidtoneWheelLoc = -1
    private var uHighlightWheelLoc = -1
    private var uCurveMasterLoc = -1
    private var uCurveRedLoc = -1
    private var uCurveGreenLoc = -1
    private var uCurveBlueLoc = -1
    private var uHslHueLoc = -1
    private var uHslSatLoc = -1
    private var uHslLumLoc = -1
    private var uRelightActiveLoc = -1
    private var uRelightKeyPosLoc = -1
    private var uRelightKeyColorLoc = -1
    private var uRelightRimColorLoc = -1
    private var uRelightIntensityLoc = -1
    private var uRelightSpreadLoc = -1
    private var uRelightRimBoostLoc = -1
    private var uDslrBokehLoc = -1
    private var uFocusRadiusLoc = -1

    // Fullscreen quad vertices (XY) + texture UV coordinates (top-down matching Android Bitmap)
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f,
                    1f, -1f,
                    -1f, 1f,
                    1f, 1f
                )
            )
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    0f, 1f,
                    1f, 1f,
                    0f, 0f,
                    1f, 0f
                )
            )
            position(0)
        }

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    fun setMetricsListener(listener: ((GpuEngineMetrics) -> Unit)?) {
        onMetricsChanged = listener
    }

    /**
     * Submits a newly decoded/composited raw video track frame for GPU texture upload,
     * paired with the latest color grading uniforms. If [frameKey] matches the texture
     * already resident in GPU VRAM, texture upload is skipped completely.
     */
    fun updateFrameAndUniforms(
        frameBitmap: Bitmap?,
        frameKey: Long,
        uniforms: GpuColorGradeUniforms
    ) {
        currentUniforms = uniforms
        if (frameBitmap != null && !frameBitmap.isRecycled) {
            synchronized(frameLock) {
                if (frameKey != pendingFrameKey) {
                    // Snapshot into an immutable copy or swap safely so the GL thread never reads mid-draw
                    val prev = pendingBitmap
                    if (prev != null && prev.width == frameBitmap.width && prev.height == frameBitmap.height && !prev.isRecycled) {
                        val canvas = android.graphics.Canvas(prev)
                        canvas.drawBitmap(frameBitmap, 0f, 0f, null)
                    } else {
                        prev?.recycle()
                        pendingBitmap = frameBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    pendingFrameKey = frameKey
                }
            }
        }
        requestGpuRender()
    }

    /**
     * Instant shader-only uniform update (used when dragging Color Wheels, LUTs, Tone Curves,
     * HSL sliders, 3D Relight, or Split-Wipe while the video frame stays cached in GPU VRAM).
     */
    fun updateUniformsOnly(uniforms: GpuColorGradeUniforms) {
        currentUniforms = uniforms
        requestGpuRender()
    }

    private fun requestGpuRender() {
        val handler = glHandler ?: return
        if (!isGlReady.get()) return
        if (renderQueued.compareAndSet(false, true)) {
            handler.post {
                renderQueued.set(false)
                drawGlFrame()
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceW = width.coerceAtLeast(1)
        surfaceH = height.coerceAtLeast(1)
        startGlThread(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceW = width.coerceAtLeast(1)
        surfaceH = height.coerceAtLeast(1)
        glHandler?.post {
            if (isGlReady.get()) {
                GLES20.glViewport(0, 0, surfaceW, surfaceH)
                drawGlFrame()
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopGlThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Handled by EGL swapBuffers
    }

    private fun startGlThread(surface: SurfaceTexture) {
        stopGlThread()
        val thread = HandlerThread("NVEdit-GLES20-FilterEngine").apply { start() }
        val handler = Handler(thread.looper)
        glThread = thread
        glHandler = handler

        handler.post {
            val ok = initEglAndShaders(surface)
            if (ok) {
                isGlReady.set(true)
                GLES20.glViewport(0, 0, surfaceW, surfaceH)
                drawGlFrame()
            } else {
                isGlReady.set(false)
                dispatchMetrics(
                    GpuEngineMetrics(
                        isReady = false,
                        hasFailed = true,
                        rendererName = "CPU Fallback"
                    ),
                    force = true
                )
            }
        }
    }

    private fun stopGlThread() {
        isGlReady.set(false)
        renderQueued.set(false)
        val handler = glHandler
        val thread = glThread
        glHandler = null
        glThread = null

        handler?.post {
            releaseEglResources()
            thread?.quitSafely()
        } ?: run {
            thread?.quitSafely()
        }

        synchronized(frameLock) {
            pendingBitmap?.recycle()
            pendingBitmap = null
            pendingFrameKey = -1L
            uploadedFrameKey = -1L
        }
    }

    private fun initEglAndShaders(surface: SurfaceTexture): Boolean {
        return runCatching {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
                return false
            }
            eglConfig = configs[0] ?: return false

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return false

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return false

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

            val rawRenderer = GLES20.glGetString(GLES20.GL_RENDERER)?.trim().orEmpty()
            gpuRendererLabel = if (rawRenderer.isNotEmpty()) {
                rawRenderer.take(26)
            } else {
                "OpenGL ES 2.0"
            }

            programId = buildProgram(CinemaGlShaders.VERTEX_SHADER, CinemaGlShaders.FRAGMENT_SHADER)
            if (programId == 0) return false

            cacheShaderLocations(programId)

            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            textureId = texIds[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            uploadedFrameKey = -1L
            uploadedTexWidth = 0
            uploadedTexHeight = 0
            true
        }.onFailure { err ->
            Log.e(TAG, "OpenGL ES 2.0 initialization failed; falling back to CPU Canvas", err)
            releaseEglResources()
        }.getOrDefault(false)
    }

    private fun drawGlFrame() {
        if (!isGlReady.get() || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return

        val startNs = System.nanoTime()

        // 1. Upload video texture to GPU VRAM ONLY if the frame key changed
        synchronized(frameLock) {
            val bmp = pendingBitmap
            if (bmp != null && !bmp.isRecycled && pendingFrameKey != uploadedFrameKey) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                if (bmp.width == uploadedTexWidth && bmp.height == uploadedTexHeight) {
                    GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp)
                } else {
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
                    uploadedTexWidth = bmp.width
                    uploadedTexHeight = bmp.height
                }
                uploadedFrameKey = pendingFrameKey
            }
        }

        if (uploadedTexWidth <= 0 || uploadedTexHeight <= 0) {
            GLES20.glClearColor(0.035f, 0.035f, 0.055f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
            return
        }

        // 2. Execute Real-Time OpenGL ES 2.0 Cinema Color Grading Fragment Shader
        val u = currentUniforms
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
        GLES20.glClearColor(0.035f, 0.035f, 0.055f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programId)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        GLES20.glUniform2f(uTexelSizeLoc, 1f / uploadedTexWidth.toFloat(), 1f / uploadedTexHeight.toFloat())
        GLES20.glUniform1f(uTimeSecLoc, u.timeSec)
        GLES20.glUniform1f(uBypassGradeLoc, if (u.bypassGrade) 1f else 0f)
        GLES20.glUniform1f(uSplitWipePosLoc, u.splitWipePos)

        GLES20.glUniform3f(uLutScaleLoc, u.lutScaleR, u.lutScaleG, u.lutScaleB)
        GLES20.glUniform3f(uLutBiasLoc, u.lutBiasR, u.lutBiasG, u.lutBiasB)
        GLES20.glUniform1f(uLutSaturationLoc, u.lutSaturation)
        GLES20.glUniform1f(uLutContrastLoc, u.lutContrast)

        GLES20.glUniform1f(uExposureLoc, u.exposure)
        GLES20.glUniform1f(uBrightnessLoc, u.brightness)
        GLES20.glUniform1f(uContrastLoc, u.contrast)
        GLES20.glUniform1f(uSaturationLoc, u.saturation)
        GLES20.glUniform1f(uTemperatureLoc, u.temperature)
        GLES20.glUniform1f(uTintLoc, u.tint)
        GLES20.glUniform1f(uHighlightsLoc, u.highlights)
        GLES20.glUniform1f(uShadowsLoc, u.shadows)
        GLES20.glUniform1f(uFadeLoc, u.fade)
        GLES20.glUniform1f(uHdrBoostLoc, u.hdrBoost)
        GLES20.glUniform1f(uDenoiseLoc, u.lowLightDenoise)

        GLES20.glUniform1f(uSharpenLoc, u.sharpen)
        GLES20.glUniform1f(uVignetteLoc, u.vignette)
        GLES20.glUniform1f(uFilmGrainLoc, u.filmGrain)
        GLES20.glUniform1f(uHalationLoc, u.halation)
        GLES20.glUniform1f(uSkinSmoothLoc, u.skinSmooth)
        GLES20.glUniform1f(uFaceGlowLoc, u.faceGlow)

        GLES20.glUniform3f(uShadowWheelLoc, u.shadowWheelR, u.shadowWheelG, u.shadowWheelB)
        GLES20.glUniform3f(uMidtoneWheelLoc, u.midtoneWheelR, u.midtoneWheelG, u.midtoneWheelB)
        GLES20.glUniform3f(uHighlightWheelLoc, u.highlightWheelR, u.highlightWheelG, u.highlightWheelB)

        GLES20.glUniform1fv(uCurveMasterLoc, 5, u.curveMaster, 0)
        GLES20.glUniform1fv(uCurveRedLoc, 5, u.curveRed, 0)
        GLES20.glUniform1fv(uCurveGreenLoc, 5, u.curveGreen, 0)
        GLES20.glUniform1fv(uCurveBlueLoc, 5, u.curveBlue, 0)

        GLES20.glUniform1fv(uHslHueLoc, 8, u.hslHue, 0)
        GLES20.glUniform1fv(uHslSatLoc, 8, u.hslSat, 0)
        GLES20.glUniform1fv(uHslLumLoc, 8, u.hslLum, 0)

        GLES20.glUniform1f(uRelightActiveLoc, if (u.relightActive) 1f else 0f)
        GLES20.glUniform2f(uRelightKeyPosLoc, u.relightKeyX, u.relightKeyY)
        GLES20.glUniform3f(uRelightKeyColorLoc, u.relightKeyR, u.relightKeyG, u.relightKeyB)
        GLES20.glUniform3f(uRelightRimColorLoc, u.relightRimR, u.relightRimG, u.relightRimB)
        GLES20.glUniform1f(uRelightIntensityLoc, u.relightIntensity)
        GLES20.glUniform1f(uRelightSpreadLoc, u.relightSpread)
        GLES20.glUniform1f(uRelightRimBoostLoc, u.relightRimBoost)
        GLES20.glUniform1f(uDslrBokehLoc, u.dslrBokeh)
        GLES20.glUniform1f(uFocusRadiusLoc, u.focusRadius)

        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        val renderMs = (System.nanoTime() - startNs) / 1_000_000f
        dispatchMetrics(
            GpuEngineMetrics(
                isReady = true,
                hasFailed = false,
                rendererName = gpuRendererLabel,
                lastRenderMs = renderMs,
                uploadedTextureWidth = uploadedTexWidth,
                uploadedTextureHeight = uploadedTexHeight
            ),
            force = false
        )
    }

    private fun dispatchMetrics(metrics: GpuEngineMetrics, force: Boolean) {
        val listener = onMetricsChanged ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastMetricsDispatchMs < 240L) return
        lastMetricsDispatchMs = now
        mainHandler.post { listener(metrics) }
    }

    private fun cacheShaderLocations(prog: Int) {
        aPositionLoc = GLES20.glGetAttribLocation(prog, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(prog, "aTexCoord")
        uTextureLoc = GLES20.glGetUniformLocation(prog, "uTexture")
        uTexelSizeLoc = GLES20.glGetUniformLocation(prog, "uTexelSize")
        uTimeSecLoc = GLES20.glGetUniformLocation(prog, "uTimeSec")
        uBypassGradeLoc = GLES20.glGetUniformLocation(prog, "uBypassGrade")
        uSplitWipePosLoc = GLES20.glGetUniformLocation(prog, "uSplitWipePos")
        uLutScaleLoc = GLES20.glGetUniformLocation(prog, "uLutScale")
        uLutBiasLoc = GLES20.glGetUniformLocation(prog, "uLutBias")
        uLutSaturationLoc = GLES20.glGetUniformLocation(prog, "uLutSaturation")
        uLutContrastLoc = GLES20.glGetUniformLocation(prog, "uLutContrast")
        uExposureLoc = GLES20.glGetUniformLocation(prog, "uExposure")
        uBrightnessLoc = GLES20.glGetUniformLocation(prog, "uBrightness")
        uContrastLoc = GLES20.glGetUniformLocation(prog, "uContrast")
        uSaturationLoc = GLES20.glGetUniformLocation(prog, "uSaturation")
        uTemperatureLoc = GLES20.glGetUniformLocation(prog, "uTemperature")
        uTintLoc = GLES20.glGetUniformLocation(prog, "uTint")
        uHighlightsLoc = GLES20.glGetUniformLocation(prog, "uHighlights")
        uShadowsLoc = GLES20.glGetUniformLocation(prog, "uShadows")
        uFadeLoc = GLES20.glGetUniformLocation(prog, "uFade")
        uHdrBoostLoc = GLES20.glGetUniformLocation(prog, "uHdrBoost")
        uDenoiseLoc = GLES20.glGetUniformLocation(prog, "uDenoise")
        uSharpenLoc = GLES20.glGetUniformLocation(prog, "uSharpen")
        uVignetteLoc = GLES20.glGetUniformLocation(prog, "uVignette")
        uFilmGrainLoc = GLES20.glGetUniformLocation(prog, "uFilmGrain")
        uHalationLoc = GLES20.glGetUniformLocation(prog, "uHalation")
        uSkinSmoothLoc = GLES20.glGetUniformLocation(prog, "uSkinSmooth")
        uFaceGlowLoc = GLES20.glGetUniformLocation(prog, "uFaceGlow")
        uShadowWheelLoc = GLES20.glGetUniformLocation(prog, "uShadowWheel")
        uMidtoneWheelLoc = GLES20.glGetUniformLocation(prog, "uMidtoneWheel")
        uHighlightWheelLoc = GLES20.glGetUniformLocation(prog, "uHighlightWheel")
        uCurveMasterLoc = GLES20.glGetUniformLocation(prog, "uCurveMaster")
        uCurveRedLoc = GLES20.glGetUniformLocation(prog, "uCurveRed")
        uCurveGreenLoc = GLES20.glGetUniformLocation(prog, "uCurveGreen")
        uCurveBlueLoc = GLES20.glGetUniformLocation(prog, "uCurveBlue")
        uHslHueLoc = GLES20.glGetUniformLocation(prog, "uHslHue")
        uHslSatLoc = GLES20.glGetUniformLocation(prog, "uHslSat")
        uHslLumLoc = GLES20.glGetUniformLocation(prog, "uHslLum")
        uRelightActiveLoc = GLES20.glGetUniformLocation(prog, "uRelightActive")
        uRelightKeyPosLoc = GLES20.glGetUniformLocation(prog, "uRelightKeyPos")
        uRelightKeyColorLoc = GLES20.glGetUniformLocation(prog, "uRelightKeyColor")
        uRelightRimColorLoc = GLES20.glGetUniformLocation(prog, "uRelightRimColor")
        uRelightIntensityLoc = GLES20.glGetUniformLocation(prog, "uRelightIntensity")
        uRelightSpreadLoc = GLES20.glGetUniformLocation(prog, "uRelightSpread")
        uRelightRimBoostLoc = GLES20.glGetUniformLocation(prog, "uRelightRimBoost")
        uDslrBokehLoc = GLES20.glGetUniformLocation(prog, "uDslrBokeh")
        uFocusRadiusLoc = GLES20.glGetUniformLocation(prog, "uFocusRadius")
    }

    private fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        if (vShader == 0) return 0
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        if (fShader == 0) {
            GLES20.glDeleteShader(vShader)
            return 0
        }
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vShader)
        GLES20.glAttachShader(prog, fShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vShader)
        GLES20.glDeleteShader(fShader)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "GLSL link failed: ${GLES20.glGetProgramInfoLog(prog)}")
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "GLSL compile failed (type=$type): ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun releaseEglResources() {
        runCatching {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                if (eglContext != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                }
                if (textureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                    textureId = 0
                }
                if (programId != 0) {
                    GLES20.glDeleteProgram(programId)
                    programId = 0
                }
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }
    }

    companion object {
        private const val TAG = "CinemaGlFilterEngine"
    }
}
