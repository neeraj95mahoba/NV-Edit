package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.CanvasBackgroundSettings
import com.example.model.CanvasBgMode
import com.example.model.ColorGrading
import com.example.model.ColorWheelTint
import com.example.model.CutoutBackgroundType
import com.example.model.CutoutMode
import com.example.model.CutoutSettings
import com.example.model.Keyframe
import com.example.model.KeyframeEasing
import com.example.model.MusicVisualizerStyle
import com.example.model.PipBlendMode
import com.example.model.PipMaskShape
import com.example.model.RelightPreset
import com.example.model.RelightSettings
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimLoop
import com.example.model.TextAnimOut
import com.example.model.TextAnimationType
import com.example.model.TextClipStyle
import com.example.model.TextEffectStyle
import com.example.model.TextFontStyle
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.TransitionType
import com.example.model.VideoAnimCombo
import com.example.model.VideoAnimIn
import com.example.model.VideoAnimOut
import com.example.model.VideoProjectDocument
import com.example.model.VisualFxSettings
import com.example.model.VisualFxType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class EvaluatedTransform(
    val scale: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val extraBlur: Float = 0f
)

class VideoCanvasRenderer(private val context: Context) {

    private val typefaceCache = mutableMapOf<TextFontStyle, Typeface>()
    private val cutoutProcessor = CutoutProcessor()

    fun evaluateKeyframes(keyframes: List<Keyframe>, localTimeMs: Long): EvaluatedTransform {
        if (keyframes.isEmpty()) return EvaluatedTransform()
        val sorted = keyframes.sortedBy { it.timeOffsetMs }
        if (localTimeMs <= sorted.first().timeOffsetMs) {
            val k = sorted.first()
            return EvaluatedTransform(scale = k.scale, offsetX = k.offsetX, offsetY = k.offsetY, rotationDeg = k.rotationDeg, opacity = k.opacity)
        }
        if (localTimeMs >= sorted.last().timeOffsetMs) {
            val k = sorted.last()
            return EvaluatedTransform(scale = k.scale, offsetX = k.offsetX, offsetY = k.offsetY, rotationDeg = k.rotationDeg, opacity = k.opacity)
        }
        var prev = sorted.first()
        var next = sorted.last()
        for (i in 0 until sorted.lastIndex) {
            if (localTimeMs >= sorted[i].timeOffsetMs && localTimeMs <= sorted[i + 1].timeOffsetMs) {
                prev = sorted[i]
                next = sorted[i + 1]
                break
            }
        }
        val span = max(1L, next.timeOffsetMs - prev.timeOffsetMs).toFloat()
        val rawT = ((localTimeMs - prev.timeOffsetMs) / span).coerceIn(0f, 1f)
        val t = applyEasing(rawT, next.easing, next.bezierX1, next.bezierY1, next.bezierX2, next.bezierY2)
        return EvaluatedTransform(
            scale = lerp(prev.scale, next.scale, t),
            offsetX = lerp(prev.offsetX, next.offsetX, t),
            offsetY = lerp(prev.offsetY, next.offsetY, t),
            rotationDeg = lerp(prev.rotationDeg, next.rotationDeg, t),
            opacity = lerp(prev.opacity, next.opacity, t).coerceIn(0f, 1f)
        )
    }

    fun evaluateEasingCurve(
        t: Float,
        easing: KeyframeEasing,
        x1: Float = 0.25f,
        y1: Float = 0.10f,
        x2: Float = 0.25f,
        y2: Float = 1.00f
    ): Float = applyEasing(t.coerceIn(0f, 1f), easing, x1, y1, x2, y2)

    private fun applyEasing(
        t: Float,
        easing: KeyframeEasing,
        x1: Float = 0.25f,
        y1: Float = 0.10f,
        x2: Float = 0.25f,
        y2: Float = 1.00f
    ): Float = when (easing) {
        KeyframeEasing.LINEAR -> t
        KeyframeEasing.EASE_IN -> t * t * t
        KeyframeEasing.EASE_OUT -> 1f - (1f - t).pow(3)
        KeyframeEasing.EASE_IN_OUT -> evaluateCubicBezierY(t, x1, y1, x2, y2)
        KeyframeEasing.EASE_OUT_BACK -> {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
        }
        KeyframeEasing.BOUNCE -> {
            val n1 = 7.5625f
            val d1 = 2.75f
            when {
                t < 1f / d1 -> n1 * t * t
                t < 2f / d1 -> { val u = t - 1.5f / d1; n1 * u * u + 0.75f }
                t < 2.5f / d1 -> { val u = t - 2.25f / d1; n1 * u * u + 0.9375f }
                else -> { val u = t - 2.625f / d1; n1 * u * u + 0.984375f }
            }
        }
        KeyframeEasing.CUSTOM_ELASTIC -> {
            if (t <= 0f) 0f
            else if (t >= 1f) 1f
            else {
                val c4 = (2.0 * PI / 3.0).toFloat()
                (2f.pow(-10f * t) * sin((t * 10f - 0.75f) * c4) + 1f)
            }
        }
    }

    private fun evaluateCubicBezierY(t: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        // Solve cubic Bezier x(u)=t via 5 Newton/bisection steps then evaluate y(u)
        var lo = 0f
        var hi = 1f
        var u = t
        for (i in 0 until 6) {
            val inv = 1f - u
            val bx = 3f * inv * inv * u * x1 + 3f * inv * u * u * x2 + u * u * u
            if (bx < t) lo = u else hi = u
            u = (lo + hi) * 0.5f
        }
        val inv = 1f - u
        return 3f * inv * inv * u * y1 + 3f * inv * u * u * y2 + u * u * u
    }

    fun evaluateSpeedMultiplier(clip: TimelineClip, normalizedTime: Float): Float {
        if (clip.speedCurvePreset == SpeedCurvePreset.NONE) {
            return clip.speed.coerceIn(0.05f, 100f)
        }
        val pts = clip.speedCurvePoints.sortedBy { it.normalizedTime }
        if (pts.size < 2) return clip.speed.coerceIn(0.05f, 100f)
        val u = normalizedTime.coerceIn(0f, 1f)
        for (i in 0 until pts.lastIndex) {
            val a = pts[i]
            val b = pts[i + 1]
            if (u >= a.normalizedTime && u <= b.normalizedTime) {
                val span = max(0.0001f, b.normalizedTime - a.normalizedTime)
                val localT = ((u - a.normalizedTime) / span).coerceIn(0f, 1f)
                val smoothT = (0.5f - 0.5f * cos(localT * PI)).toFloat()
                return lerp(a.speedMultiplier, b.speedMultiplier, smoothT).coerceIn(0.05f, 100f)
            }
        }
        return pts.last().speedMultiplier.coerceIn(0.05f, 100f)
    }

    fun evaluateSourceTimeMs(clip: TimelineClip, localTimeMs: Long): Long {
        if (clip.isFreezeFrame) {
            return clip.freezeSourceMs.coerceAtLeast(0L)
        }
        val clamped = localTimeMs.coerceIn(0L, clip.durationMs)
        val mapped = if (clip.speedCurvePreset == SpeedCurvePreset.NONE) {
            (clamped * clip.speed).toLong()
        } else {
            val steps = 16
            val uTarget = clamped.toFloat() / max(1L, clip.durationMs).toFloat()
            var accumMs = 0f
            val du = uTarget / steps
            for (i in 0 until steps) {
                val uMid = (i + 0.5f) * du
                val spd = evaluateSpeedMultiplier(clip, uMid)
                accumMs += (clip.durationMs * du) * spd
            }
            accumMs.toLong()
        }
        val srcLen = max(1000L, clip.sourceDurationMs)
        val forwardMs = (clip.sourceStartMs + mapped) % srcLen
        return if (clip.isReversed) {
            (srcLen - forwardMs).coerceIn(0L, srcLen)
        } else {
            forwardMs
        }
    }

    fun buildColorMatrix(grading: ColorGrading): ColorMatrix {
        val lut = grading.lutPreset
        val fi = grading.filterIntensity.coerceIn(0f, 1f)
        val cm = ColorMatrix()

        // Blend LUT parameters from neutral by filterIntensity (0%..100%)
        val lutSat = lerp(1f, lut.saturation, fi)
        val lutContrast = lerp(1f, lut.contrast, fi)
        val lutRScale = lerp(1f, lut.rScale, fi)
        val lutGScale = lerp(1f, lut.gScale, fi)
        val lutBScale = lerp(1f, lut.bScale, fi)
        val lutRBias = lut.rBias * fi
        val lutGBias = lut.gBias * fi
        val lutBBias = lut.bBias * fi

        // 8-Color HSL Mixer channel weights + RGB Tone Curve lift/gamma/gain extraction
        val hslSatDelta = if (grading.hslBands.isNotEmpty()) {
            grading.hslBands.map { it.saturationShift }.average().toFloat() * 0.48f
        } else 0f
        var hslRScale = 1f
        var hslGScale = 1f
        var hslBScale = 1f
        var hslRBias = 0f
        var hslGBias = 0f
        var hslBBias = 0f
        grading.hslBands.forEachIndexed { idx, band ->
            val lum = band.luminanceShift * 20f
            val hue = band.hueShift * 0.14f
            val satB = band.saturationShift * 0.12f
            when (idx) {
                0, 1 -> { hslRScale += satB; hslRBias += lum; hslGBias += hue * 22f } // Red, Orange
                2 -> { hslRScale += satB * 0.6f; hslGScale += satB * 0.6f; hslRBias += lum * 0.7f; hslGBias += lum * 0.7f } // Yellow
                3 -> { hslGScale += satB; hslGBias += lum; hslBBias += hue * 20f } // Green
                4 -> { hslGScale += satB * 0.6f; hslBScale += satB * 0.6f; hslGBias += lum * 0.7f; hslBBias += lum * 0.7f } // Aqua
                5 -> { hslBScale += satB; hslBBias += lum; hslRBias += hue * 20f } // Blue
                6, 7 -> { hslRScale += satB * 0.65f; hslBScale += satB * 0.65f; hslRBias += lum * 0.6f; hslBBias += lum * 0.6f } // Purple, Magenta
            }
        }

        val (mSlope, mOffset) = extractToneCurveParams(grading.toneCurveMaster)
        val (rCurveSlope, rCurveOffset) = extractToneCurveParams(grading.toneCurveRed)
        val (gCurveSlope, gCurveOffset) = extractToneCurveParams(grading.toneCurveGreen)
        val (bCurveSlope, bCurveOffset) = extractToneCurveParams(grading.toneCurveBlue)

        val sat = (lutSat + grading.saturation + hslSatDelta + grading.hdrBoost * 0.24f - grading.lowLightDenoise * 0.06f).coerceIn(0f, 2.5f)
        val satMatrix = ColorMatrix().apply { setSaturation(sat) }
        cm.postConcat(satMatrix)

        val exposureScale = 2f.pow((grading.exposure + grading.lowLightDenoise * 0.14f) * 0.85f)
        val beautyBright = grading.beauty.faceGlow * 18f
        val contrastScale = (
            (lutContrast +
                grading.contrast * 0.7f +
                grading.sharpen * 0.20f +
                grading.hdrBoost * 0.28f +
                grading.highlights * 0.18f -
                grading.shadows * 0.12f -
                grading.fade * 0.24f -
                grading.lowLightDenoise * 0.08f -
                grading.beauty.skinSmooth * 0.08f) * mSlope
            ).coerceIn(0.25f, 2.6f)

        val brightnessOffset = grading.brightness * 100f +
            grading.shadows * 26f +
            grading.highlights * 14f +
            grading.fade * 36f +
            grading.hdrBoost * 10f +
            grading.lowLightDenoise * 16f +
            mOffset +
            beautyBright
        val contrastTranslate = 128f * (1f - contrastScale)

        val tempR = grading.temperature * 26f
        val tempB = -grading.temperature * 26f
        val tintG = -grading.tint * 24f
        val tintRB = grading.tint * 12f

        val (shR, shG, shB) = wheelVector(grading.shadowWheel, 34f)
        val (midR, midG, midB) = wheelVector(grading.midtoneWheel, 0.24f)
        val (hiR, hiG, hiB) = wheelVector(grading.highlightWheel, 0.26f)

        val rM = (lutRScale * hslRScale * rCurveSlope * exposureScale * contrastScale * (1f + midR + hiR)).coerceIn(0.15f, 3.2f)
        val gM = (lutGScale * hslGScale * gCurveSlope * exposureScale * contrastScale * (1f + midG + hiG)).coerceIn(0.15f, 3.2f)
        val bM = (lutBScale * hslBScale * bCurveSlope * exposureScale * contrastScale * (1f + midB + hiB)).coerceIn(0.15f, 3.2f)

        val rT = lutRBias + hslRBias + rCurveOffset + brightnessOffset + contrastTranslate + tempR + tintRB + shR + grading.beauty.faceGlow * 8f
        val gT = lutGBias + hslGBias + gCurveOffset + brightnessOffset + contrastTranslate + tintG + shG + grading.beauty.faceGlow * 4f
        val bT = lutBBias + hslBBias + bCurveOffset + brightnessOffset + contrastTranslate + tempB + tintRB + shB

        val toneMatrix = ColorMatrix(
            floatArrayOf(
                rM, 0f, 0f, 0f, rT,
                0f, gM, 0f, 0f, gT,
                0f, 0f, bM, 0f, bT,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(toneMatrix)
        return cm
    }

    private fun extractToneCurveParams(curve: List<Float>): Pair<Float, Float> {
        if (curve.size < 5) return 1f to 0f
        val p0 = curve[0].coerceIn(0f, 1f)
        val p1 = curve[1].coerceIn(0f, 1f)
        val p2 = curve[2].coerceIn(0f, 1f)
        val p3 = curve[3].coerceIn(0f, 1f)
        val p4 = curve[4].coerceIn(0f, 1f)
        val slope = ((p4 - p0) + (p3 - p1) * 0.7f).coerceIn(0.45f, 2.1f) / 1.35f
        val midShift = ((p2 - 0.5f) * 115f + (p0 * 48f) + ((p4 - 1f) * 32f)).coerceIn(-90f, 90f)
        return slope.coerceIn(0.45f, 2.0f) to midShift
    }

    private fun wheelVector(wheel: ColorWheelTint, magnitude: Float): Triple<Float, Float, Float> {
        if (wheel.strength <= 0.01f) return Triple(0f, 0f, 0f)
        val hsv = floatArrayOf(wheel.hueDeg % 360f, 1f, 1f)
        val rgb = Color.HSVToColor(hsv)
        val r = (Color.red(rgb) / 255f - 0.5f) * 2f * wheel.strength * magnitude
        val g = (Color.green(rgb) / 255f - 0.5f) * 2f * wheel.strength * magnitude
        val b = (Color.blue(rgb) / 255f - 0.5f) * 2f * wheel.strength * magnitude
        return Triple(r, g, b)
    }

    private fun evaluateClipAnimation(clip: TimelineClip, localMs: Long): EvaluatedTransform {
        val animDur = clip.animDurationMs.coerceIn(200L, min(3000L, max(200L, clip.durationMs)))
        val inT = (localMs.toFloat() / animDur.toFloat()).coerceIn(0f, 1f)
        val remMs = (clip.durationMs - localMs).coerceAtLeast(0L)
        val outT = (1f - (remMs.toFloat() / animDur.toFloat())).coerceIn(0f, 1f)
        val progress = (localMs.toFloat() / max(1L, clip.durationMs).toFloat()).coerceIn(0f, 1f)

        var sx = 1f
        var sy = 1f
        var dx = 0f
        var dy = 0f
        var rot = 0f
        var alpha = 1f
        var blur = 0f

        // 1. IN Animations
        if (localMs < animDur && clip.animIn != VideoAnimIn.NONE) {
            val ease = (0.5f - 0.5f * cos(inT * PI)).toFloat()
            when (clip.animIn) {
                VideoAnimIn.FADE_IN -> alpha *= ease
                VideoAnimIn.ZOOM_IN -> { val s = lerp(0.55f, 1f, ease); sx *= s; sy *= s; alpha *= inT }
                VideoAnimIn.ZOOM_OUT -> { val s = lerp(1.65f, 1f, ease); sx *= s; sy *= s; alpha *= inT }
                VideoAnimIn.SLIDE_LEFT -> dx += lerp(1f, 0f, ease)
                VideoAnimIn.SLIDE_RIGHT -> dx += lerp(-1f, 0f, ease)
                VideoAnimIn.SLIDE_UP -> dy += lerp(1f, 0f, ease)
                VideoAnimIn.SLIDE_DOWN -> dy += lerp(-1f, 0f, ease)
                VideoAnimIn.SPIN_IN -> { rot += lerp(-180f, 0f, ease); val s = lerp(0.4f, 1f, ease); sx *= s; sy *= s; alpha *= inT }
                VideoAnimIn.WHIP_PAN -> { dx += lerp(-1.2f, 0f, ease); blur += (1f - ease) * 0.7f }
                VideoAnimIn.BLUR_IN -> { blur += (1f - ease) * 0.85f; alpha *= ease }
                VideoAnimIn.BOUNCE_DROP -> {
                    val bounce = abs(sin((1f - inT) * PI * 2.5)).toFloat() * (1f - inT)
                    dy -= bounce * 0.45f
                }
                VideoAnimIn.SHAKE_IN -> {
                    val amp = (1f - inT) * 0.06f
                    dx += sin(inT * PI * 10).toFloat() * amp
                    dy += cos(inT * PI * 9).toFloat() * amp
                }
                VideoAnimIn.POP_SCALE -> {
                    val s = applyEasing(inT, KeyframeEasing.EASE_OUT_BACK).coerceAtLeast(0.1f)
                    sx *= s; sy *= s
                }
                VideoAnimIn.NONE -> Unit
            }
        }

        // 2. OUT Animations
        if (remMs < animDur && clip.animOut != VideoAnimOut.NONE) {
            val ease = (0.5f - 0.5f * cos(outT * PI)).toFloat()
            when (clip.animOut) {
                VideoAnimOut.FADE_OUT -> alpha *= (1f - ease)
                VideoAnimOut.ZOOM_OUT -> { val s = lerp(1f, 0.45f, ease); sx *= s; sy *= s; alpha *= (1f - ease) }
                VideoAnimOut.ZOOM_WARP -> { val s = lerp(1f, 2.2f, ease); sx *= s; sy *= s; alpha *= (1f - ease); blur += ease * 0.7f }
                VideoAnimOut.SLIDE_LEFT -> dx -= ease
                VideoAnimOut.SLIDE_RIGHT -> dx += ease
                VideoAnimOut.SLIDE_UP -> dy -= ease
                VideoAnimOut.SLIDE_DOWN -> dy += ease
                VideoAnimOut.SPIN_OUT -> { rot += ease * 180f; val s = lerp(1f, 0.3f, ease); sx *= s; sy *= s; alpha *= (1f - ease) }
                VideoAnimOut.BLUR_OUT -> { blur += ease * 0.85f; alpha *= (1f - ease) }
                VideoAnimOut.SHRINK_AWAY -> { val s = (1f - ease).coerceAtLeast(0.05f); sx *= s; sy *= s }
                VideoAnimOut.NONE -> Unit
            }
        }

        // 3. COMBO Beat-Sync Animations
        if (clip.animCombo != VideoAnimCombo.NONE) {
            val tSec = localMs / 1000f
            when (clip.animCombo) {
                VideoAnimCombo.PENDULUM_1 -> { rot += sin(tSec * 4.5f) * 11f; val s = 1.08f; sx *= s; sy *= s }
                VideoAnimCombo.PENDULUM_2 -> { rot += cos(tSec * 6.5f) * 16f; dx += sin(tSec * 6.5f) * 0.05f; sx *= 1.12f; sy *= 1.12f }
                VideoAnimCombo.BOUNCE_1 -> { dy -= abs(sin(tSec * 5.2f)) * 0.07f; sx *= 1.06f; sy *= 1.06f }
                VideoAnimCombo.BOUNCE_2 -> {
                    val b = abs(sin(tSec * 7.2f))
                    sx *= (1f + b * 0.14f); sy *= (1f - b * 0.06f)
                }
                VideoAnimCombo.WAVE_SLIDE -> {
                    dx += sin(tSec * 4f) * 0.08f; dy += cos(tSec * 3.2f) * 0.05f; sx *= 1.12f; sy *= 1.12f
                }
                VideoAnimCombo.DISTORT_LEFT -> {
                    sx *= (1.12f + 0.10f * sin(progress * PI.toFloat() * 4f))
                    rot -= 6f * sin(progress * PI.toFloat() * 4f)
                }
                VideoAnimCombo.DISTORT_RIGHT -> {
                    sy *= (1.12f + 0.10f * cos(progress * PI.toFloat() * 4f))
                    rot += 6f * sin(progress * PI.toFloat() * 4f)
                }
                VideoAnimCombo.ZOOM_TWIST -> {
                    val pulse = sin(progress * PI.toFloat()).coerceAtLeast(0f)
                    val s = 1f + 0.30f * pulse
                    sx *= s; sy *= s; rot += pulse * 18f
                }
                VideoAnimCombo.FLIP_3D_H -> {
                    val c = cos(progress * PI * 2.0).toFloat()
                    sx *= (if (abs(c) < 0.08f) 0.08f else c)
                }
                VideoAnimCombo.FLIP_3D_V -> {
                    val c = cos(progress * PI * 2.0).toFloat()
                    sy *= (if (abs(c) < 0.08f) 0.08f else c)
                }
                VideoAnimCombo.ELASTIC_WOBBLE -> {
                    sx *= (1.05f + 0.12f * sin(tSec * 8.5f))
                    sy *= (1.05f - 0.12f * sin(tSec * 8.5f))
                }
                VideoAnimCombo.BEAT_FLASH_ZOOM -> {
                    val beat = abs(sin(tSec * PI.toFloat() * 2.2f)).pow(3)
                    val s = 1f + beat * 0.18f
                    sx *= s; sy *= s
                }
                VideoAnimCombo.NONE -> Unit
            }
        }

        return EvaluatedTransform(
            scale = 1f,
            scaleX = sx,
            scaleY = sy,
            offsetX = dx,
            offsetY = dy,
            rotationDeg = rot,
            opacity = alpha.coerceIn(0f, 1f),
            extraBlur = blur.coerceIn(0f, 1f)
        )
    }

    fun resolveEffectiveGrading(
        document: VideoProjectDocument,
        playheadMs: Long,
        compareOriginal: Boolean = false
    ): ColorGrading {
        if (compareOriginal) return ColorGrading()
        val activeAdjustmentClip = document.clips.lastOrNull {
            it.trackType == TrackType.ADJUSTMENT_LAYER && playheadMs in it.startMs..it.endMs
        }
        return if (activeAdjustmentClip?.adjustmentGrading != null) {
            mergeGrading(document.colorGrading, activeAdjustmentClip.adjustmentGrading)
        } else {
            document.colorGrading
        }
    }

    /**
     * Renders ONLY the ungraded base video/photo layers (canvas background, main video track,
     * transitions, chroma cutout, transforms/keyframes, smooth slow-mo, and PIP overlays)
     * into an offscreen backing canvas so the OpenGL ES 2.0 GPU fragment shader can perform
     * real-time cinematic color grading, LUTs, tone curves, 8-band HSL, film grain, halation,
     * vignette, and 3D relight + bokeh directly on GPU.
     */
    fun renderBaseVideoTrackPass(
        canvas: Canvas,
        width: Float,
        height: Float,
        document: VideoProjectDocument,
        playheadMs: Long,
        frameEngine: MediaFrameEngine,
        maxDecodeDim: Int = 720,
        compareOriginal: Boolean = false
    ) {
        renderFrameInternal(
            canvas = canvas,
            width = width,
            height = height,
            document = document,
            playheadMs = playheadMs,
            frameEngine = frameEngine,
            maxDecodeDim = maxDecodeDim,
            compareOriginal = compareOriginal,
            applyCpuColorGradeAndPost = false,
            renderPostOverlays = false
        )
    }

    /**
     * Renders crisp vector overlays on top of the OpenGL ES 2.0 GPU surface:
     * Visual FX overlays, reactive Music Visualizer, and animated Text / Stickers / Captions.
     */
    fun renderPostOverlayPass(
        canvas: Canvas,
        width: Float,
        height: Float,
        document: VideoProjectDocument,
        playheadMs: Long,
        compareOriginal: Boolean = false
    ) {
        val fx = if (compareOriginal) VisualFxSettings() else document.visualFx
        val timeSec = playheadMs / 1000f

        if (!compareOriginal) {
            drawVisualFxOverlay(canvas, width, height, fx, timeSec)
        }

        if (!compareOriginal && document.musicVisualizer != MusicVisualizerStyle.NONE) {
            drawMusicVisualizerOverlay(canvas, width, height, document.musicVisualizer, timeSec, document.beatMarkersMs, playheadMs)
        }

        val textClips = document.clips.filter {
            it.trackType == TrackType.TEXT_STICKER && playheadMs in it.startMs..it.endMs
        }
        textClips.forEach { clip ->
            val style = clip.textStyle ?: return@forEach
            val localMs = (playheadMs - clip.startMs).coerceAtLeast(0L)
            val tf = evaluateKeyframes(clip.keyframes, localMs)
            drawAnimatedText(canvas, width, height, clip, style, localMs, tf)
        }
    }

    fun renderFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        document: VideoProjectDocument,
        playheadMs: Long,
        frameEngine: MediaFrameEngine,
        maxDecodeDim: Int = 720,
        compareOriginal: Boolean = false
    ) {
        renderFrameInternal(
            canvas = canvas,
            width = width,
            height = height,
            document = document,
            playheadMs = playheadMs,
            frameEngine = frameEngine,
            maxDecodeDim = maxDecodeDim,
            compareOriginal = compareOriginal,
            applyCpuColorGradeAndPost = true,
            renderPostOverlays = true
        )
    }

    private fun renderFrameInternal(
        canvas: Canvas,
        width: Float,
        height: Float,
        document: VideoProjectDocument,
        playheadMs: Long,
        frameEngine: MediaFrameEngine,
        maxDecodeDim: Int,
        compareOriginal: Boolean,
        applyCpuColorGradeAndPost: Boolean,
        renderPostOverlays: Boolean
    ) {
        // 1. Canvas Background Pass
        drawCanvasBaseBackground(canvas, width, height, document.canvasBackground, null, frameEngine, maxDecodeDim)

        val fx = if (compareOriginal) VisualFxSettings() else document.visualFx
        val grading = if (applyCpuColorGradeAndPost) {
            resolveEffectiveGrading(document, playheadMs, compareOriginal)
        } else {
            ColorGrading()
        }
        val cutout = if (compareOriginal) CutoutSettings() else document.cutout

        val timeSec = playheadMs / 1000f
        val shakeX = when (fx.activeFx) {
            VisualFxType.SHAKE -> sin(timeSec * 42f * fx.speed) * width * 0.025f * fx.intensity
            VisualFxType.HANDHELD_CAM -> (sin(timeSec * 2.7f * fx.speed) * 0.65f + sin(timeSec * 6.3f * fx.speed) * 0.35f) * width * 0.018f * fx.intensity
            else -> 0f
        }
        val shakeY = when (fx.activeFx) {
            VisualFxType.SHAKE -> cos(timeSec * 37f * fx.speed) * height * 0.022f * fx.intensity
            VisualFxType.HANDHELD_CAM -> (cos(timeSec * 2.1f * fx.speed) * 0.70f + cos(timeSec * 5.4f * fx.speed) * 0.30f) * height * 0.016f * fx.intensity
            else -> 0f
        }
        val handheldRot = if (fx.activeFx == VisualFxType.HANDHELD_CAM) {
            sin(timeSec * 1.9f * fx.speed) * 2.4f * fx.intensity
        } else 0f

        val zoomPulse = if (fx.activeFx == VisualFxType.ZOOM_PULSE) {
            1f + abs(sin(timeSec * PI.toFloat() * 2.2f * fx.speed)).pow(2) * 0.22f * fx.intensity
        } else 1f

        val colorFilter = ColorMatrixColorFilter(buildColorMatrix(grading))

        // 2. Main Video Track (+ Canvas Blur BG + Pro Transitions + Cutout + Video Animations + 18 Visual FX)
        val mainClips = document.clips
            .filter { it.trackType == TrackType.MAIN_VIDEO }
            .sortedBy { it.startMs }
        val activeIndex = mainClips.indexOfFirst { playheadMs in it.startMs..it.endMs }

        if (activeIndex >= 0) {
            val mainClip = mainClips[activeIndex]
            val localMs = (playheadMs - mainClip.startMs).coerceAtLeast(0L)
            val srcMs = evaluateSourceTimeMs(mainClip, localMs)
            val rawBmp = frameEngine.getClipFrameSync(
                mediaUri = mainClip.mediaUri,
                drawableKey = mainClip.drawableKey,
                isVideoMedia = mainClip.isVideoMedia,
                sourceTimeMs = srcMs,
                maxDim = maxDecodeDim
            )

                val prevClip = if (activeIndex > 0) mainClips[activeIndex - 1] else null
                val trans = mainClip.transitionIn
                val inTransition = !compareOriginal &&
                    trans.type != TransitionType.NONE &&
                    localMs < trans.durationMs

                if (rawBmp != null) {
                    // If Canvas BG is blur mode, paint blurred frame underneath
                    if (document.canvasBackground.mode in setOf(CanvasBgMode.BLUR_LOW, CanvasBgMode.BLUR_MED, CanvasBgMode.BLUR_HIGH)) {
                        drawCanvasBaseBackground(canvas, width, height, document.canvasBackground, rawBmp, frameEngine, maxDecodeDim)
                    }

                    // If Cutout/Chroma Key is active, paint replacement background
                    if (cutout.mode != CutoutMode.OFF) {
                        drawCutoutBackground(canvas, width, height, cutout, rawBmp, frameEngine, maxDecodeDim)
                    }

                    val fgBmp = if (cutout.mode != CutoutMode.OFF) {
                        cutoutProcessor.process(rawBmp, cutout)
                    } else rawBmp

                    val kfTf = evaluateKeyframes(mainClip.keyframes, localMs)
                    val animTf = if (compareOriginal) EvaluatedTransform() else evaluateClipAnimation(mainClip, localMs)
                    val subtleMotion = if (!mainClip.isVideoMedia && mainClip.keyframes.isEmpty() && mainClip.animCombo == VideoAnimCombo.NONE && !mainClip.isFreezeFrame) {
                        1f + 0.03f * sin(timeSec * 0.9f)
                    } else 1f

                    val flipX = if (mainClip.flipHorizontal) -1f else 1f
                    val flipY = if (mainClip.flipVertical) -1f else 1f
                    val effectiveTf = EvaluatedTransform(
                        scale = kfTf.scale * subtleMotion * mainClip.cropZoom * zoomPulse * (if (fx.activeFx == VisualFxType.HANDHELD_CAM) 1.05f else 1f),
                        scaleX = animTf.scaleX * flipX,
                        scaleY = animTf.scaleY * flipY,
                        offsetX = kfTf.offsetX + animTf.offsetX,
                        offsetY = kfTf.offsetY + animTf.offsetY,
                        rotationDeg = kfTf.rotationDeg + animTf.rotationDeg + (mainClip.rotationQuarterTurns * 90f) + handheldRot,
                        opacity = (kfTf.opacity * animTf.opacity).coerceIn(0f, 1f),
                        extraBlur = animTf.extraBlur
                    )

                    val normU = (localMs.toFloat() / max(1L, mainClip.durationMs).toFloat()).coerceIn(0f, 1f)
                    val instSpeed = evaluateSpeedMultiplier(mainClip, normU)
                    val motionBlurAmt = if (compareOriginal) 0f else {
                        val speedBoost = (abs(instSpeed - 1f) * 0.35f).coerceIn(0f, 0.85f)
                        val fxBlur = if (fx.activeFx == VisualFxType.MOTION_BLUR_FX) fx.intensity * 0.85f else 0f
                        max(fxBlur, max(effectiveTf.extraBlur, fx.motionBlur * (0.45f + speedBoost))).coerceIn(0f, 1f)
                    }

                    if (inTransition && prevClip != null) {
                    val prevSrcMs = evaluateSourceTimeMs(prevClip, prevClip.durationMs)
                    val prevBmp = frameEngine.getClipFrameSync(
                        mediaUri = prevClip.mediaUri,
                        drawableKey = prevClip.drawableKey,
                        isVideoMedia = prevClip.isVideoMedia,
                        sourceTimeMs = prevSrcMs,
                        maxDim = maxDecodeDim
                    )
                    val transProgress = (localMs.toFloat() / max(1L, trans.durationMs).toFloat()).coerceIn(0f, 1f)
                    renderProTransition(
                        canvas = canvas,
                        width = width,
                        height = height,
                        fromBmp = prevBmp ?: fgBmp,
                        toBmp = fgBmp,
                        transitionType = trans.type,
                        progress = transProgress,
                        colorFilter = colorFilter
                    )
                } else {
                    drawTransformedBitmap(
                        canvas = canvas,
                        bitmap = fgBmp,
                        canvasWidth = width,
                        canvasHeight = height,
                        transform = effectiveTf,
                        shakeX = shakeX,
                        shakeY = shakeY,
                        colorFilter = colorFilter,
                        fx = fx,
                        motionBlurAmt = motionBlurAmt,
                        timeSec = timeSec
                    )
                    // Optical-Flow Multi-Frame Fractional Interpolation for Butter-Smooth Slow-Mo (0.05x..0.95x)
                    if (!compareOriginal && mainClip.smoothSlowMo && mainClip.isVideoMedia && instSpeed < 0.95f) {
                        val frameStepMs = 33L
                        val subFrac = ((srcMs % frameStepMs).toFloat() / frameStepMs.toFloat()).coerceIn(0.15f, 0.85f)
                        val nextSrcMs = (srcMs + frameStepMs).coerceAtMost(max(1000L, mainClip.sourceDurationMs))
                        val nextRaw = frameEngine.getClipFrameSync(
                            mediaUri = mainClip.mediaUri,
                            drawableKey = mainClip.drawableKey,
                            isVideoMedia = true,
                            sourceTimeMs = nextSrcMs,
                            maxDim = maxDecodeDim
                        )
                        if (nextRaw != null && nextRaw !== rawBmp) {
                            val blendTf = effectiveTf.copy(opacity = (effectiveTf.opacity * subFrac * 0.55f).coerceIn(0.08f, 0.55f))
                            drawTransformedBitmap(
                                canvas = canvas,
                                bitmap = nextRaw,
                                canvasWidth = width,
                                canvasHeight = height,
                                transform = blendTf,
                                shakeX = shakeX,
                                shakeY = shakeY,
                                colorFilter = colorFilter,
                                fx = VisualFxSettings(),
                                motionBlurAmt = 0f,
                                timeSec = timeSec
                            )
                        }
                    }
                }

                // Pro Skin Retouch / Soft-Focus Beauty Glow Pass (CPU path only; GPU shader computes live)
                if (applyCpuColorGradeAndPost && !compareOriginal && (grading.beauty.skinSmooth > 0.02f || grading.beauty.faceGlow > 0.02f)) {
                    drawBeautyRetouchPass(canvas, fgBmp, width, height, effectiveTf, grading.beauty.skinSmooth, grading.beauty.faceGlow)
                }

                // AI Low-Light Video Denoiser & HDR Dynamic Bloom Pass (CPU path only; GPU shader computes live)
                if (applyCpuColorGradeAndPost && !compareOriginal && (grading.lowLightDenoise > 0.03f || grading.hdrBoost > 0.03f)) {
                    drawHdrAndDenoisePass(canvas, fgBmp, width, height, effectiveTf, grading.hdrBoost, grading.lowLightDenoise)
                }

                // Cinema Halation (35mm Red-Amber Optical Highlight Bloom — CPU path only; GPU shader computes live)
                if (applyCpuColorGradeAndPost && !compareOriginal && grading.halation > 0.02f) {
                    drawCinemaHalationPass(canvas, fgBmp, width, height, effectiveTf, grading.halation)
                }
            }
        } else {
            drawEmptyStageGrid(canvas, width, height)
        }

        // 3. Multi-Layer Overlay / PIP Track with Blend Modes & Shape Masking
        val pipClips = document.clips.filter {
            it.trackType == TrackType.OVERLAY_PIP && playheadMs in it.startMs..it.endMs
        }
        pipClips.forEach { pip ->
            val localMs = (playheadMs - pip.startMs).coerceAtLeast(0L)
            val srcMs = evaluateSourceTimeMs(pip, localMs)
            val bmp = frameEngine.getClipFrameSync(
                mediaUri = pip.mediaUri,
                drawableKey = pip.drawableKey,
                isVideoMedia = pip.isVideoMedia,
                sourceTimeMs = srcMs,
                maxDim = max(360, maxDecodeDim / 2)
            )
            if (bmp != null) {
                val tf = evaluateKeyframes(pip.keyframes, localMs)
                drawPipOverlay(canvas, bmp, width, height, pip, tf, colorFilter, compareOriginal)
            }
        }

        // 4. Visual FX Post-Processing Overlays
        if (renderPostOverlays && !compareOriginal) {
            drawVisualFxOverlay(canvas, width, height, fx, timeSec)
        }

        // 4b. Dedicated Cinema 35mm Film Grain Pass from Color Grading (CPU path)
        if (applyCpuColorGradeAndPost && !compareOriginal && grading.filmGrain > 0.02f) {
            drawCinemaFilmGrainPass(canvas, width, height, grading.filmGrain, timeSec)
        }

        // 4c. AI 3D Relight Studio (Virtual Directional Key/Rim Light + DSLR Depth-of-Field Bokeh — CPU path)
        if (applyCpuColorGradeAndPost && !compareOriginal && (document.relight.preset != RelightPreset.OFF || document.relight.dslrBokeh > 0.02f)) {
            drawAi3dRelightAndBokeh(canvas, width, height, document.relight, timeSec)
        }

        // 5. Music Visualizer Reactive Overlay (for AI Lyrics & Music Visualizer)
        if (renderPostOverlays && !compareOriginal && document.musicVisualizer != MusicVisualizerStyle.NONE) {
            drawMusicVisualizerOverlay(canvas, width, height, document.musicVisualizer, timeSec, document.beatMarkersMs, playheadMs)
        }

        // 6. Color Grading Vignette (CPU path)
        if (applyCpuColorGradeAndPost && !compareOriginal && grading.vignette > 0.01f) {
            val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f,
                    height / 2f,
                    max(width, height) * 0.74f,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.argb((grading.vignette * 195).toInt().coerceIn(0, 220), 0, 0, 0)
                    ),
                    floatArrayOf(0.42f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, vigPaint)
        }

        // 7. Text, Stickers & Auto-Captions Track (In + Out + Loop Animations)
        if (renderPostOverlays) {
            val textClips = document.clips.filter {
                it.trackType == TrackType.TEXT_STICKER && playheadMs in it.startMs..it.endMs
            }
            textClips.forEach { clip ->
                val style = clip.textStyle ?: return@forEach
                val localMs = (playheadMs - clip.startMs).coerceAtLeast(0L)
                val tf = evaluateKeyframes(clip.keyframes, localMs)
                drawAnimatedText(canvas, width, height, clip, style, localMs, tf)
            }
        }
    }

    private fun drawAi3dRelightAndBokeh(
        canvas: Canvas,
        width: Float,
        height: Float,
        relight: RelightSettings,
        timeSec: Float
    ) {
        val minSide = min(width, height)
        val cx = width * 0.5f
        val cy = height * 0.48f

        // 1. Virtual DSLR Depth-of-Field Bokeh (Shallow focus ring + optical circular bokeh orbs)
        if (relight.dslrBokeh > 0.02f) {
            val bokehAmt = relight.dslrBokeh.coerceIn(0f, 1f)
            val focusR = (minSide * relight.focusRadius.coerceIn(0.18f, 0.78f))
            val outerR = max(width, height) * 0.85f
            val dofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    outerR,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        Color.argb((bokehAmt * 115).toInt().coerceIn(0, 140), 10, 12, 22),
                        Color.argb((bokehAmt * 175).toInt().coerceIn(0, 200), 6, 8, 16)
                    ),
                    floatArrayOf(0f, (focusR / outerR).coerceIn(0.15f, 0.65f), 0.78f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, dofPaint)

            // Optical circular bokeh highlights around outer out-of-focus plane
            val keyHex = if (relight.preset != RelightPreset.OFF) relight.preset.keyColorHex else 0xFFFFD88A
            val rimHex = if (relight.preset != RelightPreset.OFF) relight.preset.rimColorHex else 0xFF00F0FF
            val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            val orbCount = 14
            for (i in 0 until orbCount) {
                val angle = (i * 2.39996f) + timeSec * 0.08f
                val distNorm = 0.56f + ((i % 4) * 0.13f)
                val ox = cx + cos(angle) * width * 0.54f * distNorm
                val oy = cy + sin(angle) * height * 0.52f * distNorm
                val orbRadius = minSide * (0.032f + (i % 3) * 0.018f) * (0.65f + bokehAmt * 0.7f)
                val baseColor = if (i % 2 == 0) keyHex.toInt() else rimHex.toInt()
                val alpha = ((42 + (i % 3) * 20) * bokehAmt).toInt().coerceIn(8, 125)
                orbPaint.shader = RadialGradient(
                    ox,
                    oy,
                    orbRadius,
                    intArrayOf(
                        Color.argb((alpha * 0.55f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                        Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.76f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(ox, oy, orbRadius, orbPaint)
            }
        }

        // 2. Virtual 3D Directional Key Light + Opposing Edge Rim Light
        if (relight.preset != RelightPreset.OFF && relight.intensity > 0.02f) {
            val rad = Math.toRadians(relight.azimuthDeg.toDouble())
            val elevNorm = ((relight.elevationDeg + 45f) / 130f).coerceIn(0f, 1f)
            val keyX = cx + (cos(rad).toFloat() * width * 0.58f)
            val keyY = (height * (0.78f - elevNorm * 0.68f)) + (sin(rad).toFloat() * height * 0.18f)

            val keyColor = relight.preset.keyColorHex.toInt()
            val rimColor = relight.preset.rimColorHex.toInt()
            val keyAlpha = (relight.intensity.coerceIn(0f, 1f) * 165f).toInt().coerceIn(10, 185)
            val keyRadius = max(width, height) * (0.48f + relight.spread.coerceIn(0.2f, 1f) * 0.52f)

            val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                shader = RadialGradient(
                    keyX,
                    keyY,
                    keyRadius,
                    intArrayOf(
                        Color.argb(keyAlpha, Color.red(keyColor), Color.green(keyColor), Color.blue(keyColor)),
                        Color.argb((keyAlpha * 0.45f).toInt(), Color.red(keyColor), Color.green(keyColor), Color.blue(keyColor)),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.46f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, keyPaint)

            // Directional opposing 3D rim light edge sweep
            if (relight.rimBoost > 0.04f) {
                val rimX = width - keyX
                val rimY = height - keyY * 0.7f
                val rimAlpha = (relight.intensity * relight.rimBoost * 155f).toInt().coerceIn(8, 170)
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    shader = LinearGradient(
                        rimX,
                        rimY,
                        cx,
                        cy,
                        intArrayOf(
                            Color.argb(rimAlpha, Color.red(rimColor), Color.green(rimColor), Color.blue(rimColor)),
                            Color.argb((rimAlpha * 0.30f).toInt(), Color.red(rimColor), Color.green(rimColor), Color.blue(rimColor)),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.38f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, rimPaint)
            }
        }
    }

    private fun drawCanvasBaseBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        bg: CanvasBackgroundSettings,
        activeBmp: Bitmap?,
        frameEngine: MediaFrameEngine,
        maxDecodeDim: Int
    ) {
        when (bg.mode) {
            CanvasBgMode.SOLID -> canvas.drawColor(bg.solidColorHex.toInt())
            CanvasBgMode.GRADIENT -> {
                val colors = when (bg.gradientIndex % 5) {
                    1 -> intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#6E3AFF"), Color.parseColor("#0D061A"))
                    2 -> intArrayOf(Color.parseColor("#FFD84D"), Color.parseColor("#FF5E5B"), Color.parseColor("#1D0B28"))
                    3 -> intArrayOf(Color.parseColor("#10E896"), Color.parseColor("#00A8CC"), Color.parseColor("#071521"))
                    4 -> intArrayOf(Color.parseColor("#1E293B"), Color.parseColor("#0F172A"), Color.parseColor("#020617"))
                    else -> intArrayOf(Color.parseColor("#9B5CFF"), Color.parseColor("#00F0FF"), Color.parseColor("#0A0A14"))
                }
                val p = Paint().apply {
                    shader = LinearGradient(0f, 0f, width, height, colors, null, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, width, height, p)
            }
            CanvasBgMode.BLUR_LOW, CanvasBgMode.BLUR_MED, CanvasBgMode.BLUR_HIGH -> {
                if (activeBmp != null) {
                    val div = when (bg.mode) {
                        CanvasBgMode.BLUR_LOW -> 14f
                        CanvasBgMode.BLUR_MED -> 26f
                        else -> 42f
                    }
                    val tinyW = max(8, (width / div).toInt())
                    val tinyH = max(8, (height / div).toInt())
                    val blurred = Bitmap.createScaledBitmap(activeBmp, tinyW, tinyH, true)
                    canvas.drawBitmap(blurred, null, RectF(0f, 0f, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
                    blurred.recycle()
                    canvas.drawColor(Color.argb(48, 0, 0, 0))
                } else {
                    canvas.drawColor(Color.parseColor("#0A0A0D"))
                }
            }
            CanvasBgMode.CUSTOM_IMAGE -> {
                val bgBmp = bg.customImageUri?.let {
                    frameEngine.getClipFrameSync(it, null, false, 0L, maxDecodeDim)
                }
                if (bgBmp != null) {
                    drawFitCoverBitmap(canvas, bgBmp, width, height, Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    canvas.drawColor(Color.parseColor("#0A0A0D"))
                }
            }
        }
    }

    private fun drawCutoutBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        cutout: CutoutSettings,
        originalBmp: Bitmap,
        frameEngine: MediaFrameEngine,
        maxDecodeDim: Int
    ) {
        when (cutout.bgType) {
            CutoutBackgroundType.TRANSPARENT_DARK -> canvas.drawColor(Color.parseColor("#0A0A0D"))
            CutoutBackgroundType.SOLID_COLOR -> canvas.drawColor(cutout.bgSolidColorHex.toInt())
            CutoutBackgroundType.NEON_GRADIENT -> {
                val colors = when (cutout.bgGradientIndex % 4) {
                    1 -> intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#6E3AFF"), Color.parseColor("#0A0A14"))
                    2 -> intArrayOf(Color.parseColor("#FFD84D"), Color.parseColor("#FF5E5B"), Color.parseColor("#180C24"))
                    3 -> intArrayOf(Color.parseColor("#10E896"), Color.parseColor("#00A8CC"), Color.parseColor("#071521"))
                    else -> intArrayOf(Color.parseColor("#9B5CFF"), Color.parseColor("#00F0FF"), Color.parseColor("#0A0A14"))
                }
                val bgPaint = Paint().apply {
                    shader = LinearGradient(0f, 0f, width, height, colors, null, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, width, height, bgPaint)
            }
            CutoutBackgroundType.BLUR_BG -> {
                val tinyW = max(12, (width / 22f).toInt())
                val tinyH = max(12, (height / 22f).toInt())
                val blurred = Bitmap.createScaledBitmap(originalBmp, tinyW, tinyH, true)
                canvas.drawBitmap(blurred, null, RectF(0f, 0f, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
                blurred.recycle()
                canvas.drawColor(Color.argb(65, 10, 10, 16))
            }
            CutoutBackgroundType.CUSTOM_MEDIA -> {
                val bgBmp = frameEngine.getClipFrameSync(
                    mediaUri = cutout.bgCustomUri,
                    drawableKey = cutout.bgCustomDrawableKey,
                    isVideoMedia = false,
                    sourceTimeMs = 0L,
                    maxDim = maxDecodeDim
                )
                if (bgBmp != null) {
                    drawFitCoverBitmap(canvas, bgBmp, width, height, Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    canvas.drawColor(Color.parseColor("#12121A"))
                }
            }
        }
    }

    private fun drawBeautyRetouchPass(
        canvas: Canvas,
        bmp: Bitmap,
        width: Float,
        height: Float,
        transform: EvaluatedTransform,
        skinSmooth: Float,
        faceGlow: Float
    ) {
        if (skinSmooth > 0.02f) {
            val tiny = Bitmap.createScaledBitmap(bmp, max(24, bmp.width / 8), max(24, bmp.height / 8), true)
            val bmpAspect = tiny.width.toFloat() / max(1, tiny.height).toFloat()
            val canvasAspect = width / max(1f, height)
            val drawW = if (bmpAspect > canvasAspect) height * bmpAspect else width
            val drawH = if (bmpAspect > canvasAspect) height else width / bmpAspect
            val rect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

            canvas.save()
            canvas.translate(width / 2f + transform.offsetX * width, height / 2f + transform.offsetY * height)
            canvas.rotate(transform.rotationDeg)
            canvas.scale(transform.scale * transform.scaleX, transform.scale * transform.scaleY)
            val softPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (skinSmooth * 105f).toInt().coerceIn(0, 125)
            }
            canvas.drawBitmap(tiny, null, rect, softPaint)
            canvas.restore()
            tiny.recycle()
        }
        if (faceGlow > 0.02f) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.5f,
                    height * 0.42f,
                    max(width, height) * 0.55f,
                    intArrayOf(
                        Color.argb((faceGlow * 64f).toInt().coerceIn(0, 80), 255, 240, 224),
                        Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, glowPaint)
        }
    }

    private fun mergeGrading(base: ColorGrading, adj: ColorGrading): ColorGrading {
        val useAdjLut = adj.lutPreset != com.example.model.LutPreset.ORIGINAL
        return base.copy(
            lutPreset = if (useAdjLut) adj.lutPreset else base.lutPreset,
            filterIntensity = if (useAdjLut) adj.filterIntensity else base.filterIntensity,
            brightness = (base.brightness + adj.brightness).coerceIn(-1f, 1f),
            contrast = (base.contrast + adj.contrast).coerceIn(-1f, 1f),
            saturation = (base.saturation + adj.saturation).coerceIn(-1f, 1f),
            exposure = (base.exposure + adj.exposure).coerceIn(-1f, 1f),
            vignette = max(base.vignette, adj.vignette),
            sharpen = max(base.sharpen, adj.sharpen),
            temperature = (base.temperature + adj.temperature).coerceIn(-1f, 1f),
            tint = (base.tint + adj.tint).coerceIn(-1f, 1f),
            highlights = (base.highlights + adj.highlights).coerceIn(-1f, 1f),
            shadows = (base.shadows + adj.shadows).coerceIn(-1f, 1f),
            fade = max(base.fade, adj.fade),
            hdrBoost = max(base.hdrBoost, adj.hdrBoost),
            lowLightDenoise = max(base.lowLightDenoise, adj.lowLightDenoise),
            filmGrain = max(base.filmGrain, adj.filmGrain),
            halation = max(base.halation, adj.halation)
        )
    }

    private fun drawCinemaHalationPass(
        canvas: Canvas,
        bmp: Bitmap,
        width: Float,
        height: Float,
        transform: EvaluatedTransform,
        halation: Float
    ) {
        val tiny = Bitmap.createScaledBitmap(bmp, max(16, bmp.width / 14), max(16, bmp.height / 14), true)
        val bmpAspect = tiny.width.toFloat() / max(1, tiny.height).toFloat()
        val canvasAspect = width / max(1f, height)
        val drawW = if (bmpAspect > canvasAspect) height * bmpAspect else width
        val drawH = if (bmpAspect > canvasAspect) height else width / bmpAspect
        val rect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

        canvas.save()
        canvas.translate(width / 2f + transform.offsetX * width, height / 2f + transform.offsetY * height)
        canvas.rotate(transform.rotationDeg)
        canvas.scale(transform.scale * transform.scaleX * 1.015f, transform.scale * transform.scaleY * 1.015f)
        val halationPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.argb(185, 255, 76, 36), PorterDuff.Mode.SRC_ATOP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (halation * 135f).toInt().coerceIn(0, 165)
        }
        canvas.drawBitmap(tiny, null, rect, halationPaint)
        canvas.restore()
        tiny.recycle()
    }

    private fun drawCinemaFilmGrainPass(
        canvas: Canvas,
        width: Float,
        height: Float,
        grain: Float,
        timeSec: Float
    ) {
        val gPaint = Paint()
        val frameSeed = (timeSec * 24f).toInt()
        val count = (45 + grain * 130).toInt().coerceIn(30, 180)
        val baseAlpha = (grain * 96f).toInt().coerceIn(10, 125)
        val radius = max(1f, width * 0.0018f)
        for (i in 0 until count) {
            val gx = abs(sin((frameSeed * 23 + i * 37).toDouble())).toFloat() * width
            val gy = abs(cos((frameSeed * 17 + i * 53).toDouble())).toFloat() * height
            gPaint.color = if (i % 2 == 0) {
                Color.argb(baseAlpha, 255, 248, 236)
            } else {
                Color.argb(baseAlpha / 2, 16, 14, 20)
            }
            canvas.drawCircle(gx, gy, radius, gPaint)
        }
    }

    private fun drawHdrAndDenoisePass(
        canvas: Canvas,
        bmp: Bitmap,
        width: Float,
        height: Float,
        transform: EvaluatedTransform,
        hdrBoost: Float,
        lowLightDenoise: Float
    ) {
        if (lowLightDenoise > 0.03f) {
            // Fast bilateral-style spatial chroma/luma smoothing pass that suppresses grain in shadows
            val downW = max(32, bmp.width / 6)
            val downH = max(32, bmp.height / 6)
            val smooth = Bitmap.createScaledBitmap(bmp, downW, downH, true)
            val bmpAspect = smooth.width.toFloat() / max(1, smooth.height).toFloat()
            val canvasAspect = width / max(1f, height)
            val drawW = if (bmpAspect > canvasAspect) height * bmpAspect else width
            val drawH = if (bmpAspect > canvasAspect) height else width / bmpAspect
            val rect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

            canvas.save()
            canvas.translate(width / 2f + transform.offsetX * width, height / 2f + transform.offsetY * height)
            canvas.rotate(transform.rotationDeg)
            canvas.scale(transform.scale * transform.scaleX, transform.scale * transform.scaleY)
            val denoisePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (lowLightDenoise * 92f).toInt().coerceIn(0, 110)
            }
            canvas.drawBitmap(smooth, null, rect, denoisePaint)
            canvas.restore()
            smooth.recycle()
        }
        if (hdrBoost > 0.03f) {
            // Subtle HDR radiant clarity sheen across mid-highlights
            val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.5f,
                    height * 0.48f,
                    max(width, height) * 0.78f,
                    intArrayOf(
                        Color.argb((hdrBoost * 34f).toInt().coerceIn(0, 44), 255, 252, 245),
                        Color.TRANSPARENT,
                        Color.argb((hdrBoost * 28f).toInt().coerceIn(0, 36), 8, 12, 24)
                    ),
                    floatArrayOf(0f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, hdrPaint)
        }
    }

    private fun renderProTransition(
        canvas: Canvas,
        width: Float,
        height: Float,
        fromBmp: Bitmap,
        toBmp: Bitmap,
        transitionType: TransitionType,
        progress: Float,
        colorFilter: ColorMatrixColorFilter
    ) {
        val t = (0.5f - 0.5f * cos(progress * PI)).toFloat()
        val pFrom = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.colorFilter = colorFilter }
        val pTo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.colorFilter = colorFilter }

        when (transitionType) {
            TransitionType.DISSOLVE, TransitionType.CROSS_FADE -> {
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                pTo.alpha = (t * 255).toInt().coerceIn(0, 255)
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
            }
            TransitionType.DIP_BLACK -> {
                if (t < 0.5f) {
                    pFrom.alpha = ((1f - t * 2f) * 255).toInt().coerceIn(0, 255)
                    drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                } else {
                    pTo.alpha = (((t - 0.5f) * 2f) * 255).toInt().coerceIn(0, 255)
                    drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                }
            }
            TransitionType.FLASH_WHITE, TransitionType.LENS_FLARE, TransitionType.CHROMATIC_FLASH -> {
                drawFitCoverBitmap(canvas, if (t < 0.5f) fromBmp else toBmp, width, height, pTo)
                val peak = sin(t * PI).toFloat()
                val flashColor = when (transitionType) {
                    TransitionType.LENS_FLARE -> Color.argb((peak * 225).toInt(), 255, 232, 160)
                    TransitionType.CHROMATIC_FLASH -> Color.argb((peak * 210).toInt(), 0, 240, 255)
                    else -> Color.argb((peak * 245).toInt(), 255, 255, 255)
                }
                canvas.drawColor(flashColor)
            }
            TransitionType.WHIP_PAN_LEFT, TransitionType.SLIDE_PUSH_LEFT -> {
                val dx = -t * width
                canvas.save(); canvas.translate(dx, 0f); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.translate(dx + width, 0f); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.WHIP_PAN_RIGHT, TransitionType.SLIDE_PUSH_RIGHT -> {
                val dx = t * width
                canvas.save(); canvas.translate(dx, 0f); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.translate(dx - width, 0f); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.WHIP_PAN_UP, TransitionType.SLIDE_PUSH_UP -> {
                val dy = -t * height
                canvas.save(); canvas.translate(0f, dy); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.translate(0f, dy + height); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.WHIP_PAN_DOWN, TransitionType.SLIDE_PUSH_DOWN -> {
                val dy = t * height
                canvas.save(); canvas.translate(0f, dy); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.translate(0f, dy - height); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.SPIN_BLUR_CW, TransitionType.SPIN_BLUR_CCW, TransitionType.SWIRL_TWIST -> {
                val dir = if (transitionType == TransitionType.SPIN_BLUR_CCW) -1f else 1f
                val bmp = if (t < 0.5f) fromBmp else toBmp
                val angle = dir * sin(t * PI).toFloat() * 140f
                val zoom = 1f + sin(t * PI).toFloat() * 0.45f
                canvas.save()
                canvas.translate(width / 2f, height / 2f)
                canvas.rotate(angle)
                canvas.scale(zoom, zoom)
                canvas.translate(-width / 2f, -height / 2f)
                drawFitCoverBitmap(canvas, bmp, width, height, pTo)
                canvas.restore()
            }
            TransitionType.ZOOM_IN_BLUR, TransitionType.GLITCH_ZOOM -> {
                val scaleFrom = 1f + t * 1.4f
                val scaleTo = 0.65f + t * 0.35f
                canvas.save()
                canvas.scale(scaleFrom, scaleFrom, width / 2f, height / 2f)
                pFrom.alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                canvas.restore()
                canvas.save()
                canvas.scale(scaleTo, scaleTo, width / 2f, height / 2f)
                pTo.alpha = (t * 255).toInt().coerceIn(0, 255)
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                canvas.restore()
                if (transitionType == TransitionType.GLITCH_ZOOM) {
                    val glitchBar = Paint().apply { color = Color.argb(130, 255, 46, 147) }
                    canvas.drawRect(0f, height * t, width, height * t + height * 0.06f, glitchBar)
                }
            }
            TransitionType.ZOOM_OUT_PULL -> {
                val scaleFrom = (1f - t * 0.55f).coerceAtLeast(0.2f)
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                canvas.save()
                canvas.scale(scaleFrom, scaleFrom, width / 2f, height / 2f)
                pFrom.alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                canvas.restore()
            }
            TransitionType.FILM_BURN, TransitionType.LIGHT_LEAK -> {
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                pTo.alpha = (t * 255).toInt().coerceIn(0, 255)
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                val peak = sin(t * PI).toFloat()
                val burnPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width, height,
                        intArrayOf(
                            Color.argb((peak * 220).toInt(), 255, 80, 20),
                            Color.argb((peak * 195).toInt(), 255, 200, 50),
                            Color.TRANSPARENT
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, burnPaint)
            }
            TransitionType.IRIS_CIRCLE -> {
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                val maxR = max(width, height) * 0.78f
                val path = Path().apply { addCircle(width / 2f, height / 2f, maxR * t, Path.Direction.CW) }
                canvas.save(); canvas.clipPath(path); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.DIAMOND_REVEAL -> {
                drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom)
                val r = max(width, height) * 0.95f * t
                val cx = width / 2f; val cy = height / 2f
                val path = Path().apply {
                    moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
                }
                canvas.save(); canvas.clipPath(path); drawFitCoverBitmap(canvas, toBmp, width, height, pTo); canvas.restore()
            }
            TransitionType.SPLIT_OPEN_H -> {
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                val shift = width * 0.55f * t
                canvas.save(); canvas.clipRect(0f, 0f, width / 2f - shift, height); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.clipRect(width / 2f + shift, 0f, width, height); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
            }
            TransitionType.SPLIT_OPEN_V, TransitionType.SHUTTER_BLINK -> {
                drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
                val shift = height * 0.55f * t
                canvas.save(); canvas.clipRect(0f, 0f, width, height / 2f - shift); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
                canvas.save(); canvas.clipRect(0f, height / 2f + shift, width, height); drawFitCoverBitmap(canvas, fromBmp, width, height, pFrom); canvas.restore()
            }
            TransitionType.GLITCH_SLICE, TransitionType.RGB_WARP, TransitionType.PIXELATE_MORPH, TransitionType.SHAKE_IMPACT -> {
                val bmp = if (t < 0.5f) fromBmp else toBmp
                val jitter = sin(t * PI * 8f).toFloat() * width * 0.045f * sin(t * PI).toFloat()
                canvas.save(); canvas.translate(jitter, -jitter * 0.5f); drawFitCoverBitmap(canvas, bmp, width, height, pTo); canvas.restore()
                val stripe = Paint().apply { color = Color.argb((sin(t * PI).toFloat() * 140).toInt(), 0, 240, 255) }
                canvas.drawRect(0f, height * 0.35f, width, height * 0.42f, stripe)
            }
            TransitionType.FLIP_3D_H, TransitionType.FLIP_3D_V -> {
                val isFirstHalf = t < 0.5f
                val bmp = if (isFirstHalf) fromBmp else toBmp
                val angle = if (isFirstHalf) (t * 2f) * 90f else -90f + ((t - 0.5f) * 2f) * 90f
                val depthScale = 1f - sin(t * PI).toFloat() * 0.16f
                val cam = Camera()
                val mat = Matrix()
                cam.save()
                cam.setLocation(0f, 0f, -12f)
                if (transitionType == TransitionType.FLIP_3D_H) {
                    cam.rotateY(angle)
                } else {
                    cam.rotateX(angle)
                }
                cam.getMatrix(mat)
                cam.restore()
                mat.preTranslate(-width / 2f, -height / 2f)
                mat.postScale(depthScale, depthScale)
                mat.postTranslate(width / 2f, height / 2f)

                canvas.save()
                canvas.concat(mat)
                drawFitCoverBitmap(canvas, bmp, width, height, pTo)
                val shadeAlpha = (sin(t * PI).toFloat() * 145f).toInt().coerceIn(0, 160)
                canvas.drawColor(Color.argb(shadeAlpha, 0, 0, 0))
                canvas.restore()
            }
            TransitionType.NONE -> drawFitCoverBitmap(canvas, toBmp, width, height, pTo)
        }
    }

    private fun drawFitCoverBitmap(canvas: Canvas, bitmap: Bitmap, width: Float, height: Float, paint: Paint) {
        val bmpAspect = bitmap.width.toFloat() / max(1, bitmap.height).toFloat()
        val canvasAspect = width / max(1f, height)
        val drawW: Float
        val drawH: Float
        if (bmpAspect > canvasAspect) {
            drawH = height
            drawW = height * bmpAspect
        } else {
            drawW = width
            drawH = width / bmpAspect
        }
        val left = (width - drawW) / 2f
        val top = (height - drawH) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
    }

    private fun drawTransformedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        canvasWidth: Float,
        canvasHeight: Float,
        transform: EvaluatedTransform,
        shakeX: Float,
        shakeY: Float,
        colorFilter: ColorMatrixColorFilter,
        fx: VisualFxSettings,
        motionBlurAmt: Float,
        timeSec: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
            alpha = (transform.opacity * 255).toInt().coerceIn(0, 255)
        }

        // Special full-frame geometry FX: Pixelate 8-Bit, Mirror 2x, Mirror 4x
        if (fx.activeFx == VisualFxType.PIXELATE_8BIT && fx.intensity > 0.05f) {
            val blockScale = (8 + (fx.intensity * 32).toInt()).coerceIn(6, 48)
            val pw = max(12, bitmap.width / blockScale)
            val ph = max(12, bitmap.height / blockScale)
            val tiny = Bitmap.createScaledBitmap(bitmap, pw, ph, false)
            val crispPaint = Paint(paint).apply { isFilterBitmap = false }
            drawFitCoverBitmap(canvas, tiny, canvasWidth, canvasHeight, crispPaint)
            tiny.recycle()
            return
        }

        if (fx.activeFx == VisualFxType.MIRROR_SPLIT_2X && fx.intensity > 0.05f) {
            canvas.save(); canvas.clipRect(0f, 0f, canvasWidth / 2f, canvasHeight); drawFitCoverBitmap(canvas, bitmap, canvasWidth, canvasHeight, paint); canvas.restore()
            canvas.save(); canvas.clipRect(canvasWidth / 2f, 0f, canvasWidth, canvasHeight); canvas.scale(-1f, 1f, canvasWidth / 2f, canvasHeight / 2f); drawFitCoverBitmap(canvas, bitmap, canvasWidth, canvasHeight, paint); canvas.restore()
            return
        }

        if (fx.activeFx == VisualFxType.MIRROR_SPLIT_4X && fx.intensity > 0.05f) {
            val hw = canvasWidth / 2f
            val hh = canvasHeight / 2f
            val quadrants = listOf(
                RectF(0f, 0f, hw, hh) to Pair(1f, 1f),
                RectF(hw, 0f, canvasWidth, hh) to Pair(-1f, 1f),
                RectF(0f, hh, hw, canvasHeight) to Pair(1f, -1f),
                RectF(hw, hh, canvasWidth, canvasHeight) to Pair(-1f, -1f)
            )
            quadrants.forEach { (r, flips) ->
                canvas.save()
                canvas.clipRect(r)
                canvas.translate(r.centerX(), r.centerY())
                canvas.scale(flips.first, flips.second)
                canvas.drawBitmap(bitmap, null, RectF(-hw / 2f, -hh / 2f, hw / 2f, hh / 2f), paint)
                canvas.restore()
            }
            return
        }

        val bmpAspect = bitmap.width.toFloat() / max(1, bitmap.height).toFloat()
        val canvasAspect = canvasWidth / max(1f, canvasHeight)
        val drawW: Float
        val drawH: Float
        if (bmpAspect > canvasAspect) {
            drawH = canvasHeight
            drawW = canvasHeight * bmpAspect
        } else {
            drawW = canvasWidth
            drawH = canvasWidth / bmpAspect
        }
        val destRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

        canvas.save()
        canvas.translate(
            canvasWidth / 2f + transform.offsetX * canvasWidth + shakeX,
            canvasHeight / 2f + transform.offsetY * canvasHeight + shakeY
        )
        canvas.rotate(transform.rotationDeg)
        val fisheyeBoost = if (fx.activeFx == VisualFxType.FISHEYE_LENS) 1f + fx.intensity * 0.25f else 1f
        canvas.scale(
            transform.scale * transform.scaleX * fisheyeBoost,
            transform.scale * transform.scaleY * fisheyeBoost
        )

        // Pro Velocity Motion Blur / Anim Blur
        if (motionBlurAmt > 0.03f) {
            val taps = 4
            val spread = canvasWidth * 0.032f * motionBlurAmt
            val scaleStep = 0.025f * motionBlurAmt
            for (k in 1..taps) {
                val trailPaint = Paint(paint).apply {
                    alpha = ((transform.opacity * 95f * motionBlurAmt) / k).toInt().coerceIn(10, 110)
                }
                canvas.save()
                val dir = if (k % 2 == 0) 1f else -1f
                canvas.translate(dir * spread * k * 0.55f, 0f)
                canvas.scale(1f + scaleStep * k, 1f + scaleStep * k)
                canvas.drawBitmap(bitmap, null, destRect, trailPaint)
                canvas.restore()
            }
        }

        // RGB Split chromatic aberration
        if (fx.activeFx == VisualFxType.RGB_SPLIT && fx.intensity > 0.02f) {
            val shift = canvasWidth * 0.018f * fx.intensity * (0.6f + 0.4f * abs(sin(timeSec * 8f * fx.speed)))
            val redPaint = Paint(paint).apply {
                this.colorFilter = PorterDuffColorFilter(Color.argb(135, 255, 40, 120), PorterDuff.Mode.SRC_ATOP)
                alpha = 155
            }
            val cyanPaint = Paint(paint).apply {
                this.colorFilter = PorterDuffColorFilter(Color.argb(135, 0, 240, 255), PorterDuff.Mode.SRC_ATOP)
                alpha = 155
            }
            canvas.save(); canvas.translate(-shift, 0f); canvas.drawBitmap(bitmap, null, destRect, redPaint); canvas.restore()
            canvas.save(); canvas.translate(shift, 0f); canvas.drawBitmap(bitmap, null, destRect, cyanPaint); canvas.restore()
        }

        // Main frame draw
        canvas.drawBitmap(bitmap, null, destRect, paint)

        // Dreamy Glow / Bloom soft additive pass
        if (fx.activeFx == VisualFxType.DREAMY_BLOOM && fx.intensity > 0.02f) {
            val tiny = Bitmap.createScaledBitmap(bitmap, max(16, bitmap.width / 12), max(16, bitmap.height / 12), true)
            val bloomPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (fx.intensity * 135f).toInt().coerceIn(0, 165)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            canvas.drawBitmap(tiny, null, destRect, bloomPaint)
            tiny.recycle()
        }

        // Retro Glitch sliced horizontal displacement
        if (fx.activeFx == VisualFxType.RETRO_GLITCH && fx.intensity > 0.02f) {
            val sliceCount = 5
            val phase = (timeSec * 12f * fx.speed).toInt()
            for (i in 0 until sliceCount) {
                val hash = sin((phase * 13 + i * 7).toDouble()).toFloat()
                if (abs(hash) > 0.35f) {
                    val yNorm = ((i + 0.5f) / sliceCount) - 0.5f
                    val sliceH = drawH * (0.04f + 0.04f * abs(hash))
                    val offset = hash * canvasWidth * 0.07f * fx.intensity
                    canvas.save()
                    canvas.clipRect(-drawW, yNorm * drawH - sliceH / 2f, drawW, yNorm * drawH + sliceH / 2f)
                    canvas.translate(offset, 0f)
                    canvas.drawBitmap(bitmap, null, destRect, paint)
                    canvas.restore()
                }
            }
        }

        canvas.restore()
    }

    private fun drawPipOverlay(
        canvas: Canvas,
        bitmap: Bitmap,
        width: Float,
        height: Float,
        pip: TimelineClip,
        tf: EvaluatedTransform,
        colorFilter: ColorMatrixColorFilter,
        compareOriginal: Boolean
    ) {
        val mask = if (compareOriginal) PipMaskShape.RECTANGLE else pip.pipMaskShape
        val blend = if (compareOriginal) PipBlendMode.NORMAL else pip.pipBlendMode

        if (mask == PipMaskShape.SPLIT_LEFT_RIGHT || mask == PipMaskShape.SPLIT_TOP_BOTTOM) {
            val saveCount = canvas.saveLayer(0f, 0f, width, height, null)
            if (mask == PipMaskShape.SPLIT_LEFT_RIGHT) {
                canvas.clipRect(width / 2f, 0f, width, height)
            } else {
                canvas.clipRect(0f, height / 2f, width, height)
            }
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.colorFilter = colorFilter
                alpha = (tf.opacity * pip.pipOpacity * 255).toInt().coerceIn(0, 255)
                applyBlendMode(this, blend)
            }
            drawFitCoverBitmap(canvas, bitmap, width, height, p)
            canvas.restoreToCount(saveCount)

            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00F0FF")
                strokeWidth = max(2.5f, width * 0.005f)
            }
            if (mask == PipMaskShape.SPLIT_LEFT_RIGHT) {
                canvas.drawLine(width / 2f, 0f, width / 2f, height, divPaint)
            } else {
                canvas.drawLine(0f, height / 2f, width, height / 2f, divPaint)
            }
            return
        }

        val boxW = width * pip.pipScale * tf.scale
        val boxH = boxW * (bitmap.height.toFloat() / max(1, bitmap.width).toFloat())
        val cx = width / 2f + (pip.pipOffsetX + tf.offsetX) * width
        val cy = height / 2f + (pip.pipOffsetY + tf.offsetY) * height
        val rect = RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f)

        val layerId = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.translate(cx, cy)
        canvas.rotate(tf.rotationDeg)

        val clipPath = buildMaskPath(mask, rect, pip.freehandMaskPoints)
        val featherPx = if (!compareOriginal) (pip.maskFeather.coerceIn(0f, 1f) * min(boxW, boxH) * 0.14f) else 0f
        val invertMask = !compareOriginal && pip.maskInvert && clipPath != null

        canvas.save()
        if (clipPath != null && featherPx <= 1f) {
            if (invertMask) {
                clipPath.fillType = Path.FillType.INVERSE_EVEN_ODD
            }
            canvas.clipPath(clipPath)
        }

        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
            alpha = (tf.opacity * pip.pipOpacity * 255).toInt().coerceIn(0, 255)
            applyBlendMode(this, blend)
        }
        canvas.drawBitmap(bitmap, null, rect, p)

        // Feathered Soft Mask Pass using BlurMaskFilter + DST_IN / DST_OUT
        if (clipPath != null && featherPx > 1f) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                maskFilter = BlurMaskFilter(featherPx, BlurMaskFilter.Blur.NORMAL)
                xfermode = PorterDuffXfermode(if (invertMask) PorterDuff.Mode.DST_OUT else PorterDuff.Mode.DST_IN)
            }
            canvas.drawPath(clipPath, maskPaint)
        }

        if (mask == PipMaskShape.LINEAR_FEATHER) {
            val featherPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.25f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, featherPaint)
        }
        canvas.restore()

        if (clipPath != null && mask != PipMaskShape.LINEAR_FEATHER && mask != PipMaskShape.NONE && !invertMask) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(2.2f, width * 0.0045f)
                color = Color.parseColor("#00F0FF")
                alpha = (tf.opacity * pip.pipOpacity * 220).toInt().coerceIn(0, 255)
            }
            canvas.drawPath(clipPath, borderPaint)
        }
        canvas.restoreToCount(layerId)
    }

    private fun applyBlendMode(paint: Paint, blend: PipBlendMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = when (blend) {
                PipBlendMode.NORMAL -> null
                PipBlendMode.SCREEN -> BlendMode.SCREEN
                PipBlendMode.MULTIPLY -> BlendMode.MULTIPLY
                PipBlendMode.OVERLAY -> BlendMode.OVERLAY
                PipBlendMode.LIGHTEN -> BlendMode.LIGHTEN
                PipBlendMode.HARD_LIGHT -> BlendMode.HARD_LIGHT
                PipBlendMode.COLOR_DODGE -> BlendMode.COLOR_DODGE
                PipBlendMode.DIFFERENCE -> BlendMode.DIFFERENCE
            }
            if (bm != null) {
                paint.blendMode = bm
                return
            }
        }
        val mode = when (blend) {
            PipBlendMode.NORMAL -> null
            PipBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            PipBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            PipBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            PipBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            PipBlendMode.HARD_LIGHT -> PorterDuff.Mode.OVERLAY
            PipBlendMode.COLOR_DODGE -> PorterDuff.Mode.ADD
            PipBlendMode.DIFFERENCE -> PorterDuff.Mode.XOR
        }
        if (mode != null) paint.xfermode = PorterDuffXfermode(mode)
    }

    private fun buildMaskPath(mask: PipMaskShape, rect: RectF, freehandPoints: List<Float>): Path? = when (mask) {
        PipMaskShape.NONE, PipMaskShape.LINEAR_FEATHER -> null
        PipMaskShape.RECTANGLE -> Path().apply {
            val r = rect.width() * 0.10f
            addRoundRect(rect, r, r, Path.Direction.CW)
        }
        PipMaskShape.CIRCLE -> Path().apply {
            val radius = min(rect.width(), rect.height()) * 0.48f
            addCircle(0f, 0f, radius, Path.Direction.CW)
        }
        PipMaskShape.STAR -> Path().apply {
            val outerR = min(rect.width(), rect.height()) * 0.48f
            val innerR = outerR * 0.42f
            val spikes = 5
            for (i in 0 until spikes * 2) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = -PI / 2.0 + i * (PI / spikes)
                val x = (cos(angle) * r).toFloat()
                val y = (sin(angle) * r).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        PipMaskShape.HEART -> Path().apply {
            val w = rect.width() * 0.48f
            val h = rect.height() * 0.48f
            moveTo(0f, h * 0.85f)
            cubicTo(-w * 1.15f, h * 0.15f, -w * 0.95f, -h * 0.95f, 0f, -h * 0.25f)
            cubicTo(w * 0.95f, -h * 0.95f, w * 1.15f, h * 0.15f, 0f, h * 0.85f)
            close()
        }
        PipMaskShape.FREEHAND -> Path().apply {
            if (freehandPoints.size >= 6) {
                val pairs = freehandPoints.size / 2
                for (i in 0 until pairs) {
                    val nx = freehandPoints[i * 2].coerceIn(-0.5f, 0.5f)
                    val ny = freehandPoints[i * 2 + 1].coerceIn(-0.5f, 0.5f)
                    val px = nx * rect.width()
                    val py = ny * rect.height()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            } else {
                // Organic cinema lasso polygon fallback when user selects Freehand before custom drawing
                val w = rect.width() * 0.46f
                val h = rect.height() * 0.46f
                moveTo(0f, -h)
                cubicTo(w * 0.9f, -h * 0.85f, w * 1.05f, -h * 0.1f, w * 0.72f, h * 0.65f)
                cubicTo(w * 0.35f, h * 1.05f, -w * 0.45f, h * 0.95f, -w * 0.82f, h * 0.42f)
                cubicTo(-w * 1.08f, -h * 0.22f, -w * 0.72f, -h * 0.92f, 0f, -h)
                close()
            }
        }
        PipMaskShape.SPLIT_LEFT_RIGHT, PipMaskShape.SPLIT_TOP_BOTTOM -> null
    }

    private fun drawVisualFxOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        fx: VisualFxSettings,
        timeSec: Float
    ) {
        if (fx.activeFx == VisualFxType.NONE || fx.intensity <= 0.01f) return
        val intensity = fx.intensity.coerceIn(0f, 1f)

        when (fx.activeFx) {
            VisualFxType.RETRO_GLITCH -> {
                val barPaint = Paint()
                val step = (timeSec * 16f * fx.speed).toInt()
                for (i in 0 until 6) {
                    val y = ((abs(sin((step + i * 3.1))) * height).toFloat())
                    val h = height * 0.008f
                    barPaint.color = if (i % 2 == 0) {
                        Color.argb((120 * intensity).toInt(), 0, 240, 255)
                    } else {
                        Color.argb((120 * intensity).toInt(), 255, 46, 147)
                    }
                    canvas.drawRect(0f, y, width, y + h, barPaint)
                }
            }
            VisualFxType.VHS_STATIC -> {
                val linePaint = Paint().apply {
                    color = Color.argb((68 * intensity).toInt(), 0, 0, 0)
                    strokeWidth = max(1.5f, height * 0.0025f)
                }
                val gap = max(5f, height * 0.009f)
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += gap
                }
                val rollY = ((timeSec * 0.25f * fx.speed) % 1f) * height
                val trackPaint = Paint().apply { color = Color.argb((55 * intensity).toInt(), 255, 255, 255) }
                canvas.drawRect(0f, rollY, width, rollY + height * 0.035f, trackPaint)

                val recPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((215 * intensity).toInt(), 255, 70, 90)
                    textSize = width * 0.036f
                    typeface = Typeface.MONOSPACE
                }
                if (((timeSec * 2f).toInt() % 2) == 0) {
                    canvas.drawCircle(width * 0.075f, height * 0.065f, width * 0.012f, recPaint)
                }
                recPaint.color = Color.argb((210 * intensity).toInt(), 240, 240, 245)
                canvas.drawText("PLAY  SP  1998", width * 0.105f, height * 0.073f, recPaint)
            }
            VisualFxType.FILM_GRAIN -> {
                val grainPaint = Paint().apply { color = Color.argb((115 * intensity).toInt(), 255, 248, 232) }
                val scratchPaint = Paint().apply {
                    color = Color.argb((80 * intensity).toInt(), 255, 240, 210)
                    strokeWidth = max(1f, width * 0.002f)
                }
                val frameSeed = (timeSec * 24f * fx.speed).toInt()
                for (i in 0 until 75) {
                    val gx = abs(sin((frameSeed * 19 + i * 31).toDouble())).toFloat() * width
                    val gy = abs(cos((frameSeed * 11 + i * 47).toDouble())).toFloat() * height
                    canvas.drawCircle(gx, gy, max(1f, width * 0.002f), grainPaint)
                }
                val sx = abs(sin(frameSeed * 3.7)).toFloat() * width
                canvas.drawLine(sx, 0f, sx, height, scratchPaint)
            }
            VisualFxType.LIGHT_LEAK_WARM -> {
                val pulse = 0.65f + 0.35f * sin(timeSec * 2.4f * fx.speed)
                val leakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width * 0.75f, height * 0.65f,
                        intArrayOf(
                            Color.argb((165 * intensity * pulse).toInt().coerceIn(0, 220), 255, 92, 24),
                            Color.argb((115 * intensity * pulse).toInt().coerceIn(0, 180), 255, 200, 64),
                            Color.TRANSPARENT
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, leakPaint)
            }
            VisualFxType.VIGNETTE_DARK -> {
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width / 2f,
                        height / 2f,
                        max(width, height) * 0.68f,
                        intArrayOf(Color.TRANSPARENT, Color.argb((225 * intensity).toInt().coerceIn(0, 245), 0, 0, 0)),
                        floatArrayOf(0.32f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, ringPaint)
            }
            VisualFxType.FISHEYE_LENS -> {
                val barrelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width / 2f,
                        height / 2f,
                        min(width, height) * 0.56f,
                        intArrayOf(
                            Color.TRANSPARENT,
                            Color.argb((75 * intensity).toInt(), 0, 240, 255),
                            Color.argb((235 * intensity).toInt().coerceIn(0, 255), 4, 4, 8)
                        ),
                        floatArrayOf(0.72f, 0.90f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, barrelPaint)
            }
            VisualFxType.BLUR_FOCUS -> {
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width / 2f,
                        height / 2f,
                        max(width, height) * 0.65f,
                        intArrayOf(
                            Color.TRANSPARENT,
                            Color.argb((90 * intensity).toInt(), 155, 92, 255),
                            Color.argb((175 * intensity).toInt(), 10, 10, 18)
                        ),
                        floatArrayOf(0.32f, 0.72f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, haloPaint)
            }
            VisualFxType.SPARKLE -> {
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
                for (i in 0 until 16) {
                    val sx = ((i * 73 + 29) % 100) / 100f * width
                    val sy = ((i * 41 + 17) % 100) / 100f * height
                    val pulse = (0.5f + 0.5f * sin(timeSec * (3.5f + (i % 4)) * fx.speed + i)).pow(2)
                    val radius = width * (0.015f + 0.025f * pulse) * intensity
                    val alpha = (230 * pulse * intensity).toInt().coerceIn(0, 255)
                    starPaint.color = when (i % 3) {
                        0 -> Color.argb(alpha, 0, 240, 255)
                        1 -> Color.argb(alpha, 255, 255, 255)
                        else -> Color.argb(alpha, 163, 255, 18)
                    }
                    starPaint.strokeWidth = max(1.8f, width * 0.004f)
                    canvas.drawLine(sx - radius, sy, sx + radius, sy, starPaint)
                    canvas.drawLine(sx, sy - radius, sx, sy + radius, starPaint)
                    canvas.drawCircle(sx, sy, radius * 0.28f, starPaint)
                }
            }
            VisualFxType.VIGNETTE_FLASH -> {
                val beatPulse = abs(sin(timeSec * PI.toFloat() * 2.2f * fx.speed)).pow(4)
                val flashPaint = Paint().apply {
                    color = Color.argb((135 * beatPulse * intensity).toInt().coerceIn(0, 210), 255, 255, 255)
                }
                canvas.drawRect(0f, 0f, width, height, flashPaint)
            }
            VisualFxType.HALO_BLUR -> {
                val pulse = 0.78f + 0.22f * sin(timeSec * 3.1f * fx.speed)
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f,
                        height * 0.45f,
                        max(width, height) * 0.66f,
                        intArrayOf(
                            Color.argb((70 * intensity * pulse).toInt().coerceIn(0, 120), 255, 255, 255),
                            Color.argb((125 * intensity * pulse).toInt().coerceIn(0, 180), 0, 240, 255),
                            Color.argb((110 * intensity).toInt().coerceIn(0, 165), 155, 92, 255),
                            Color.argb((165 * intensity).toInt().coerceIn(0, 210), 8, 8, 16)
                        ),
                        floatArrayOf(0.0f, 0.36f, 0.68f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, haloPaint)
            }
            VisualFxType.RGB_SPLIT,
            VisualFxType.SHAKE,
            VisualFxType.HANDHELD_CAM,
            VisualFxType.ZOOM_PULSE,
            VisualFxType.DREAMY_BLOOM,
            VisualFxType.MOTION_BLUR_FX,
            VisualFxType.MIRROR_SPLIT_2X,
            VisualFxType.MIRROR_SPLIT_4X,
            VisualFxType.PIXELATE_8BIT,
            VisualFxType.NONE -> Unit
        }
    }

    private fun drawMusicVisualizerOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        style: MusicVisualizerStyle,
        timeSec: Float,
        beatMarkersMs: List<Long>,
        playheadMs: Long
    ) {
        val nearBeat = beatMarkersMs.any { abs(it - playheadMs) < 120L }
        val beatBoost = if (nearBeat) 1.38f else 1.0f

        when (style) {
            MusicVisualizerStyle.NEON_BARS -> {
                val bars = 24
                val marginX = width * 0.08f
                val spanW = width - marginX * 2f
                val step = spanW / bars
                val baseY = height * 0.90f
                val maxBarH = height * 0.14f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        marginX, baseY - maxBarH, width - marginX, baseY,
                        intArrayOf(Color.parseColor("#9B5CFF"), Color.parseColor("#00F0FF"), Color.parseColor("#FF2E93")),
                        null, Shader.TileMode.CLAMP
                    )
                }
                for (i in 0 until bars) {
                    val v = (0.25f + 0.75f * abs(sin(timeSec * (4.2 + i * 0.35) + i * 0.5)).toFloat()) * beatBoost
                    val h = (v * maxBarH).coerceAtMost(maxBarH * 1.15f)
                    val x = marginX + i * step
                    canvas.drawRoundRect(RectF(x + 2f, baseY - h, x + step - 4f, baseY), 6f, 6f, paint)
                }
            }
            MusicVisualizerStyle.CYBER_WAVE -> {
                val path = Path()
                val midY = height * 0.85f
                val amp = height * 0.045f * beatBoost
                val steps = 40
                for (i in 0..steps) {
                    val u = i.toFloat() / steps
                    val x = u * width
                    val env = sin(u * PI).toFloat()
                    val y = midY + sin(u * 18f + timeSec * 8f) * amp * env
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = max(4f, width * 0.008f)
                    color = Color.parseColor("#00F0FF")
                }
                canvas.drawPath(path, wavePaint)
            }
            MusicVisualizerStyle.PULSE_RING -> {
                val cx = width / 2f
                val cy = height * 0.48f
                val baseR = min(width, height) * 0.24f * (0.94f + 0.08f * sin(timeSec * 6f)) * beatBoost
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = max(4f, width * 0.009f)
                    color = Color.argb(200, 0, 240, 255)
                }
                canvas.drawCircle(cx, cy, baseR, ringPaint)
                ringPaint.color = Color.argb(145, 155, 92, 255)
                canvas.drawCircle(cx, cy, baseR * 1.14f, ringPaint)
            }
            MusicVisualizerStyle.NONE -> Unit
        }
    }

    private fun drawAnimatedText(
        canvas: Canvas,
        width: Float,
        height: Float,
        clip: TimelineClip,
        style: TextClipStyle,
        localMs: Long,
        tf: EvaluatedTransform
    ) {
        val speedMul = style.animSpeed.coerceIn(0.4f, 2.5f)
        val progress = (localMs.toFloat() / max(1L, clip.durationMs).toFloat()).coerceIn(0f, 1f)
        val entranceT = ((localMs * speedMul) / 480f).coerceIn(0f, 1f)
        val remMs = (clip.durationMs - localMs).coerceAtLeast(0L)
        val exitT = (1f - ((remMs * speedMul) / 360f)).coerceIn(0f, 1f) // 0 = normal, 1 = fully exited

        var displayText = when (style.animationType) {
            TextAnimationType.TYPEWRITER -> {
                val count = max(1, (style.text.length * (progress * 1.45f * speedMul).coerceIn(0f, 1f)).toInt())
                style.text.take(count)
            }
            TextAnimationType.WORD_POP -> {
                val words = style.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (words.isEmpty()) style.text else {
                    val revealed = max(1, (words.size * (progress * 1.18f * speedMul).coerceIn(0f, 1f)).toInt() + 1)
                        .coerceAtMost(words.size)
                    words.take(revealed).joinToString(" ")
                }
            }
            else -> style.text
        }
        if (style.outAnimation == TextAnimOut.TYPEWRITER_ERASE && exitT > 0.02f) {
            val keep = max(0, (displayText.length * (1f - exitT)).toInt())
            displayText = displayText.take(keep)
        }
        if (displayText.isBlank()) return

        var animScale = 1f
        var animXOffset = 0f
        var animYOffset = 0f
        var animRot = 0f
        var animAlpha = tf.opacity
        var glitchJitterX = 0f

        // 1. Text IN Animations
        when (style.animationType) {
            TextAnimationType.NONE, TextAnimationType.TYPEWRITER -> Unit
            TextAnimationType.WORD_POP -> {
                val words = style.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                val stepFrac = ((progress * max(1, words.size) * 1.18f * speedMul) % 1f).coerceIn(0f, 1f)
                val pop = applyEasing(stepFrac, KeyframeEasing.EASE_OUT_BACK).coerceIn(0.85f, 1.18f)
                animScale *= pop
            }
            TextAnimationType.FADE_UP -> {
                val ease = (0.5f - 0.5f * cos(entranceT * PI)).toFloat()
                animYOffset += (1f - ease) * height * 0.055f
                animAlpha *= ease
            }
            TextAnimationType.BOUNCE -> {
                val bounce = abs(sin((localMs * speedMul) / 220.0)).toFloat() * (1f - entranceT * 0.6f)
                animYOffset -= bounce * height * 0.036f
                animScale *= (0.85f + 0.15f * entranceT)
            }
            TextAnimationType.POP -> {
                animScale *= applyEasing(entranceT, KeyframeEasing.EASE_OUT_BACK).coerceAtLeast(0.1f)
            }
            TextAnimationType.SLIDE_MASK -> {
                val ease = (0.5f - 0.5f * cos(entranceT * PI)).toFloat()
                animXOffset -= (1f - ease) * width * 0.25f
                animAlpha *= ease
            }
            TextAnimationType.NEON_FLICKER -> {
                if (entranceT < 0.99f && (((localMs * speedMul).toLong() / 55L) % 2L == 0L)) animAlpha *= 0.35f
            }
            TextAnimationType.WAVE_DROP -> {
                val ease = (0.5f - 0.5f * cos(entranceT * PI)).toFloat()
                val wave = sin((localMs * speedMul) / 170.0).toFloat() * height * 0.014f
                animYOffset += wave - (1f - ease) * height * 0.07f
                animRot += (1f - ease) * -10f + sin((localMs * speedMul) / 260.0).toFloat() * 3.5f
            }
            TextAnimationType.GLITCH -> {
                val tick = ((localMs * speedMul) / 75f).toInt()
                if (tick % 3 == 0) glitchJitterX = sin(tick * 7.3).toFloat() * width * 0.018f
            }
            TextAnimationType.KARAOKE_HIGHLIGHT -> Unit
        }

        // 2. Text OUT Animations
        if (exitT > 0.01f) {
            val easeOut = (0.5f - 0.5f * cos(exitT * PI)).toFloat()
            when (style.outAnimation) {
                TextAnimOut.FADE_OUT -> animAlpha *= (1f - easeOut)
                TextAnimOut.SLIDE_DOWN -> { animYOffset += easeOut * height * 0.09f; animAlpha *= (1f - easeOut) }
                TextAnimOut.SHRINK_POP -> { animScale *= (1f - easeOut * 0.85f).coerceAtLeast(0.05f); animAlpha *= (1f - easeOut) }
                TextAnimOut.GLITCH_OUT -> { glitchJitterX += sin(exitT * 28f) * width * 0.04f; animAlpha *= (1f - easeOut) }
                TextAnimOut.TYPEWRITER_ERASE, TextAnimOut.NONE -> Unit
            }
        }

        // 3. Text LOOP Animations
        val tSec = (localMs / 1000f) * speedMul
        when (style.loopAnimation) {
            TextAnimLoop.FLOATING_HOVER -> animYOffset += sin(tSec * 3.2f) * height * 0.016f
            TextAnimLoop.HEARTBEAT_PULSE -> animScale *= (1f + abs(sin(tSec * 4.2f)).pow(3) * 0.14f)
            TextAnimLoop.NEON_GLOW_FLASH -> animAlpha *= (0.68f + 0.32f * abs(sin(tSec * 7.5f)))
            TextAnimLoop.SHAKE_VIBRATE -> {
                animXOffset += sin(tSec * 38f) * width * 0.008f
                animYOffset += cos(tSec * 33f) * height * 0.006f
            }
            TextAnimLoop.WAVE_TEXT -> animYOffset += sin(tSec * 5.2f) * height * 0.02f
            TextAnimLoop.GENTLE_SWING -> animRot += sin(tSec * 3.0f) * 7.5f
            TextAnimLoop.SPIN_360 -> animRot += (tSec * 120f) % 360f
            TextAnimLoop.NONE -> Unit
        }

        val scaleFactor = width / 360f
        val fontSizePx = style.fontSizeSp * scaleFactor
        val typeface = if (style.isSticker) Typeface.DEFAULT else resolveTypeface(style.fontStyle)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizePx
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            color = style.textColorHex.toInt()
            alpha = (animAlpha * 255).toInt().coerceIn(0, 255)
            if (!style.isSticker) {
                letterSpacing = style.fontStyle.letterSpacingEm
                textSkewX = style.fontStyle.skewX
                isFakeBoldText = style.fontStyle.fakeBold
            }
        }

        val defaultY = if (style.isSticker) height * 0.28f else height * 0.78f
        val cx = width / 2f + tf.offsetX * width + animXOffset + glitchJitterX
        val cy = defaultY + tf.offsetY * height + animYOffset

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(tf.rotationDeg + animRot)
        canvas.scale(tf.scale * animScale, tf.scale * animScale)

        val textWidth = textPaint.measureText(displayText)
        val fm = textPaint.fontMetrics

        // Curved Arc Special Path Renderer
        if (!style.isSticker && style.effectStyle == TextEffectStyle.CURVED_ARC) {
            val halfSpan = max(textWidth * 0.62f, width * 0.26f)
            val curveDip = style.curveAmount.coerceIn(-1f, 1f) * fontSizePx * 2.1f
            val arcPath = Path().apply {
                moveTo(-halfSpan, curveDip * 0.45f)
                quadTo(0f, -curveDip, halfSpan, curveDip * 0.45f)
            }
            val glowPaint = Paint(textPaint).apply {
                color = style.accentColorHex.toInt()
                maskFilter = BlurMaskFilter(max(3f, fontSizePx * 0.24f), BlurMaskFilter.Blur.NORMAL)
            }
            val strokePaint = Paint(textPaint).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = max(2.8f, fontSizePx * 0.12f)
                color = Color.BLACK
            }
            canvas.drawTextOnPath(displayText, arcPath, 0f, 0f, glowPaint)
            canvas.drawTextOnPath(displayText, arcPath, 0f, 0f, strokePaint)
            canvas.drawTextOnPath(displayText, arcPath, 0f, 0f, textPaint)
            canvas.restore()
            return
        }

        if (!style.isSticker) {
            when (style.effectStyle) {
                TextEffectStyle.PILL_HIGHLIGHT -> {
                    val padX = fontSizePx * 0.55f
                    val padY = fontSizePx * 0.30f
                    val pillRect = RectF(-textWidth / 2f - padX, fm.ascent - padY, textWidth / 2f + padX, fm.descent + padY)
                    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = style.accentColorHex.toInt()
                        alpha = (animAlpha * 230).toInt().coerceIn(0, 255)
                    }
                    canvas.drawRoundRect(pillRect, fontSizePx * 0.35f, fontSizePx * 0.35f, pillPaint)
                }
                TextEffectStyle.NEON_GLOW -> {
                    val glowPaint = Paint(textPaint).apply {
                        color = style.accentColorHex.toInt()
                        maskFilter = BlurMaskFilter(max(4f, fontSizePx * 0.35f), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.drawText(displayText, 0f, 0f, glowPaint)
                }
                TextEffectStyle.DROP_SHADOW -> {
                    val shadowPaint = Paint(textPaint).apply {
                        color = Color.argb((animAlpha * 210).toInt().coerceIn(0, 255), 0, 0, 0)
                    }
                    canvas.drawText(displayText, fontSizePx * 0.08f, fontSizePx * 0.10f, shadowPaint)
                }
                TextEffectStyle.MULTI_SHADOW -> {
                    val backCyan = Paint(textPaint).apply {
                        color = Color.argb((animAlpha * 185).toInt().coerceIn(0, 255), 0, 240, 255)
                    }
                    val midMagenta = Paint(textPaint).apply {
                        color = Color.argb((animAlpha * 195).toInt().coerceIn(0, 255), 255, 46, 147)
                    }
                    val nearDark = Paint(textPaint).apply {
                        color = Color.argb((animAlpha * 225).toInt().coerceIn(0, 255), 0, 0, 0)
                    }
                    canvas.drawText(displayText, -fontSizePx * 0.10f, fontSizePx * 0.12f, backCyan)
                    canvas.drawText(displayText, fontSizePx * 0.10f, fontSizePx * 0.08f, midMagenta)
                    canvas.drawText(displayText, fontSizePx * 0.04f, fontSizePx * 0.05f, nearDark)
                }
                TextEffectStyle.OUTLINE_BOLD -> {
                    val strokePaint = Paint(textPaint).apply {
                        this.style = Paint.Style.STROKE
                        strokeWidth = max(3.5f, fontSizePx * 0.16f)
                        color = Color.BLACK
                    }
                    canvas.drawText(displayText, 0f, 0f, strokePaint)
                }
                TextEffectStyle.GRADIENT_POP -> {
                    textPaint.shader = LinearGradient(
                        -textWidth / 2f, 0f, textWidth / 2f, 0f,
                        intArrayOf(Color.parseColor("#9B5CFF"), style.accentColorHex.toInt(), Color.parseColor("#00F0FF")),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                TextEffectStyle.EXTRUDE_3D -> {
                    val depthLayers = 6
                    val step = max(1.8f, fontSizePx * 0.032f)
                    for (layer in depthLayers downTo 1) {
                        val shade = (0.38f + 0.50f * (1f - layer / depthLayers.toFloat()))
                        val baseCol = style.accentColorHex.toInt()
                        val extPaint = Paint(textPaint).apply {
                            color = Color.argb(
                                (animAlpha * 235).toInt().coerceIn(0, 255),
                                (Color.red(baseCol) * shade).toInt().coerceIn(0, 255),
                                (Color.green(baseCol) * shade).toInt().coerceIn(0, 255),
                                (Color.blue(baseCol) * shade).toInt().coerceIn(0, 255)
                            )
                        }
                        canvas.drawText(displayText, layer * step, layer * step * 1.1f, extPaint)
                    }
                    val faceStroke = Paint(textPaint).apply {
                        this.style = Paint.Style.STROKE
                        strokeWidth = max(2f, fontSizePx * 0.08f)
                        color = Color.BLACK
                    }
                    canvas.drawText(displayText, 0f, 0f, faceStroke)
                }
                TextEffectStyle.CURVED_ARC,
                TextEffectStyle.CLEAN -> Unit
            }
        }

        if (style.animationType == TextAnimationType.GLITCH) {
            val cyanCopy = Paint(textPaint).apply { shader = null; color = Color.argb(150, 0, 240, 255) }
            val pinkCopy = Paint(textPaint).apply { shader = null; color = Color.argb(150, 255, 46, 147) }
            canvas.drawText(displayText, -fontSizePx * 0.07f, 0f, cyanCopy)
            canvas.drawText(displayText, fontSizePx * 0.07f, 0f, pinkCopy)
        }

        if (style.animationType == TextAnimationType.KARAOKE_HIGHLIGHT && !style.isSticker) {
            val words = displayText.split(" ").filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                val activeWordIdx = ((progress * words.size * speedMul).toInt()).coerceIn(0, words.lastIndex)
                val spaceW = textPaint.measureText(" ")
                val totalW = words.sumOf { textPaint.measureText(it).toDouble() }.toFloat() + spaceW * max(0, words.size - 1)
                var cursorX = -totalW / 2f
                textPaint.textAlign = Paint.Align.LEFT
                words.forEachIndexed { idx, word ->
                    val wWidth = textPaint.measureText(word)
                    if (idx == activeWordIdx) {
                        val hiRect = RectF(cursorX - 8f, fm.ascent - 6f, cursorX + wWidth + 8f, fm.descent + 6f)
                        val hiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = style.accentColorHex.toInt()
                            alpha = 230
                        }
                        canvas.drawRoundRect(hiRect, 10f, 10f, hiPaint)
                        val activeWordPaint = Paint(textPaint).apply { shader = null; color = Color.BLACK }
                        canvas.drawText(word, cursorX, 0f, activeWordPaint)
                    } else {
                        if (style.effectStyle == TextEffectStyle.OUTLINE_BOLD) {
                            val strokeW = Paint(textPaint).apply {
                                this.style = Paint.Style.STROKE
                                strokeWidth = max(3f, fontSizePx * 0.14f)
                                color = Color.BLACK
                            }
                            canvas.drawText(word, cursorX, 0f, strokeW)
                        }
                        canvas.drawText(word, cursorX, 0f, textPaint)
                    }
                    cursorX += wWidth + spaceW
                }
            }
        } else {
            canvas.drawText(displayText, 0f, 0f, textPaint)
        }

        canvas.restore()
    }

    private fun drawEmptyStageGrid(canvas: Canvas, width: Float, height: Float) {
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1D1D24")
            strokeWidth = 1.5f
        }
        val step = max(32f, width / 10f)
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height, gridPaint)
            x += step
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width, y, gridPaint)
            y += step
        }
    }

    private fun resolveTypeface(fontStyle: TextFontStyle): Typeface {
        typefaceCache[fontStyle]?.let { return it }
        val resId = when (fontStyle) {
            TextFontStyle.SPACE_GROTESK, TextFontStyle.SPACE_ITALIC, TextFontStyle.SPACE_WIDE, TextFontStyle.SPACE_HEAVY -> R.font.space_grotesk
            TextFontStyle.BEBAS_NEUE, TextFontStyle.BEBAS_WIDE, TextFontStyle.BEBAS_TRAILER, TextFontStyle.BEBAS_OBLIQUE -> R.font.bebas_neue
            TextFontStyle.PLAYFAIR, TextFontStyle.PLAYFAIR_WIDE, TextFontStyle.PLAYFAIR_SLANT, TextFontStyle.PLAYFAIR_CINEMA, TextFontStyle.PLAYFAIR_HEAVY -> R.font.playfair_display
            TextFontStyle.PACIFICO, TextFontStyle.PACIFICO_WIDE, TextFontStyle.PACIFICO_BOLD -> R.font.pacifico
            TextFontStyle.SYNE, TextFontStyle.SYNE_DIRECTOR, TextFontStyle.SYNE_SLANT -> R.font.syne
            TextFontStyle.JETBRAINS_MONO, TextFontStyle.MONO_CYBER -> R.font.jetbrains_mono
            TextFontStyle.PLUS_JAKARTA, TextFontStyle.JAKARTA_HEAVY, TextFontStyle.JAKARTA_WIDE, TextFontStyle.JAKARTA_SLANT -> R.font.plus_jakarta_sans

            TextFontStyle.ANTON_IMPACT, TextFontStyle.ANTON_SLANT, TextFontStyle.ANTON_POSTER, TextFontStyle.ANTON_ULTRA -> R.font.anton
            TextFontStyle.OSWALD_COND, TextFontStyle.OSWALD_WIDE, TextFontStyle.OSWALD_STUDIO, TextFontStyle.OSWALD_SLANT -> R.font.oswald
            TextFontStyle.MONTSERRAT_BOLD, TextFontStyle.MONTSERRAT_CAPS, TextFontStyle.MONTSERRAT_FILM, TextFontStyle.MONTSERRAT_SLANT -> R.font.montserrat
            TextFontStyle.POPPINS_BOLD, TextFontStyle.POPPINS_ITALIC, TextFontStyle.POPPINS_WIDE, TextFontStyle.POPPINS_CLEAN -> R.font.poppins
            TextFontStyle.BUNGEE_BLOCK, TextFontStyle.BUNGEE_TILT, TextFontStyle.BUNGEE_WIDE -> R.font.bungee
            TextFontStyle.RIGHTEOUS_POP, TextFontStyle.RIGHTEOUS_SLANT, TextFontStyle.RIGHTEOUS_WIDE -> R.font.righteous

            TextFontStyle.HINDI_YATRA, TextFontStyle.HINDI_YATRA_SLANT, TextFontStyle.HINDI_YATRA_BOLD, TextFontStyle.HINDI_YATRA_WIDE -> R.font.yatra_one
            TextFontStyle.HINDI_ROZHA, TextFontStyle.HINDI_ROZHA_GOLD, TextFontStyle.HINDI_ROZHA_SLANT, TextFontStyle.HINDI_ROZHA_WIDE -> R.font.rozha_one
            TextFontStyle.HINDI_KALAM, TextFontStyle.HINDI_KALAM_SLANT, TextFontStyle.HINDI_KALAM_WIDE -> R.font.kalam
            TextFontStyle.HINDI_TEKO, TextFontStyle.HINDI_TEKO_WIDE, TextFontStyle.HINDI_TEKO_SLANT -> R.font.teko
            TextFontStyle.HINDI_MODAK, TextFontStyle.HINDI_MODAK_BOLD, TextFontStyle.HINDI_MODAK_SLANT -> R.font.modak
            TextFontStyle.HINDI_BALOO, TextFontStyle.HINDI_BALOO_WIDE, TextFontStyle.HINDI_BALOO_SLANT -> R.font.baloo_2
            TextFontStyle.HINDI_TIRO, TextFontStyle.HINDI_TIRO_CINEMA -> R.font.tiro_devanagari_hindi

            TextFontStyle.CINZEL_ROYAL, TextFontStyle.CINZEL_EPIC, TextFontStyle.CINZEL_IMAX, TextFontStyle.CINZEL_NOIR, TextFontStyle.CINZEL_SLANT -> R.font.cinzel
            TextFontStyle.ABRIL_VOGUE, TextFontStyle.ABRIL_ITALIC, TextFontStyle.ABRIL_WIDE, TextFontStyle.ABRIL_HEAVY -> R.font.abril_fatface
            TextFontStyle.CORMORANT_LUXE, TextFontStyle.CORMORANT_ITALIC, TextFontStyle.CORMORANT_WIDE, TextFontStyle.CORMORANT_BOLD -> R.font.cormorant_garamond

            TextFontStyle.LOBSTER_NEON, TextFontStyle.LOBSTER_SLANT, TextFontStyle.LOBSTER_WIDE -> R.font.lobster
            TextFontStyle.DANCING_GLOW, TextFontStyle.DANCING_TILT, TextFontStyle.DANCING_WIDE -> R.font.dancing_script
            TextFontStyle.SATISFY_BRUSH, TextFontStyle.SATISFY_BOLD, TextFontStyle.SATISFY_SLANT -> R.font.satisfy
            TextFontStyle.CAVEAT_VLOG, TextFontStyle.CAVEAT_WIDE -> R.font.caveat

            TextFontStyle.ORBITRON_CYBER, TextFontStyle.ORBITRON_WIDE -> R.font.orbitron
            TextFontStyle.AUDIOWIDE_TECH, TextFontStyle.AUDIOWIDE_SLANT -> R.font.audiowide
            TextFontStyle.BLACK_OPS_ARMY, TextFontStyle.BLACK_OPS_SLANT -> R.font.black_ops_one
            TextFontStyle.RUSSO_ESPORTS, TextFontStyle.RUSSO_WIDE -> R.font.russo_one
            TextFontStyle.CHAKRA_MECHA, TextFontStyle.CHAKRA_SLANT -> R.font.chakra_petch
            TextFontStyle.BANGERS_COMIC, TextFontStyle.BANGERS_SLANT -> R.font.bangers
            TextFontStyle.PRESS_START_8BIT -> R.font.press_start_2p
        }
        val tf = runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        typefaceCache[fontStyle] = tf
        return tf
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
