package com.example.media

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.AspectRatioPreset
import com.example.model.ColorGrading
import com.example.model.ExportResolution
import com.example.model.Keyframe
import com.example.model.KeyframeEasing
import com.example.model.LutPreset
import com.example.model.TimelineClip
import com.example.model.VideoProjectDocument
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ExportEstimate(
    val width: Int,
    val height: Int,
    val targetFps: Int,
    val bitrateMbps: Float,
    val estimatedSizeMb: Float,
    val durationSec: Float,
    val usesSmartChunking: Boolean = false,
    val chunkCount: Int = 1
)

data class AiColorMatchResult(
    val grading: ColorGrading,
    val summary: String
)

object MotionTrackerAndEnhancer {

    /**
     * Real on-device frame pixel analyzer for 1-Tap AI Color Match & Enhancer:
     * Inspects luminance distribution, shadow density (low-light noise), highlight headroom,
     * RGB color temperature cast, and average chroma to compute balanced cinematic grading,
     * HDR boost, and low-light denoiser parameters.
     */
    fun analyzeFrameForAiColorMatch(bitmap: Bitmap): AiColorMatchResult {
        val sampleW = 64
        val sampleH = 64
        val scaled = if (bitmap.width > sampleW || bitmap.height > sampleH) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        } else bitmap

        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled !== bitmap) scaled.recycle()

        var sumLuma = 0.0
        var sumLumaSq = 0.0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var sumSat = 0.0
        var darkCount = 0
        var brightCount = 0
        val count = max(1, pixels.size)

        for (px in pixels) {
            val r = Color.red(px) / 255f
            val g = Color.green(px) / 255f
            val b = Color.blue(px) / 255f
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC > 0.001f) (maxC - minC) / maxC else 0f

            sumLuma += luma
            sumLumaSq += luma * luma
            sumR += r
            sumG += g
            sumB += b
            sumSat += sat
            if (luma < 0.22f) darkCount++
            if (luma > 0.86f) brightCount++
        }

        val meanLuma = (sumLuma / count).toFloat()
        val lumaVariance = max(0.0, (sumLumaSq / count) - (meanLuma * meanLuma)).toFloat()
        val lumaStd = kotlin.math.sqrt(lumaVariance)
        val meanR = (sumR / count).toFloat()
        val meanG = (sumG / count).toFloat()
        val meanB = (sumB / count).toFloat()
        val meanSat = (sumSat / count).toFloat()
        val darkRatio = darkCount.toFloat() / count.toFloat()
        val brightRatio = brightCount.toFloat() / count.toFloat()

        val isLowLight = meanLuma < 0.36f || darkRatio > 0.42f
        val isOverexposed = meanLuma > 0.72f || brightRatio > 0.28f

        val exposure = when {
            isLowLight -> ((0.44f - meanLuma) * 0.95f).coerceIn(0.08f, 0.38f)
            isOverexposed -> ((0.56f - meanLuma) * 0.85f).coerceIn(-0.32f, -0.06f)
            else -> ((0.48f - meanLuma) * 0.45f).coerceIn(-0.15f, 0.18f)
        }

        val contrast = if (lumaStd < 0.18f) {
            ((0.24f - lumaStd) * 1.6f).coerceIn(0.10f, 0.30f)
        } else {
            0.08f
        }

        val shadows = if (darkRatio > 0.30f) (darkRatio * 0.55f).coerceIn(0.12f, 0.42f) else 0.08f
        val highlights = if (brightRatio > 0.14f) (-brightRatio * 0.65f).coerceIn(-0.36f, -0.08f) else 0.06f

        // Neutralize heavy cool/warm color cast subtly toward film balance
        val warmBalance = meanR - meanB
        val temperature = (-warmBalance * 0.45f).coerceIn(-0.22f, 0.22f)
        val tint = ((meanG - (meanR + meanB) * 0.5f) * 0.45f).coerceIn(-0.16f, 0.16f)
        val saturation = ((0.46f - meanSat) * 0.75f).coerceIn(-0.08f, 0.28f)

        val hdrBoost = when {
            lumaStd < 0.22f || brightRatio > 0.12f -> 0.48f
            else -> 0.32f
        }
        val lowLightDenoise = if (isLowLight) {
            (0.32f + darkRatio * 0.55f).coerceIn(0.35f, 0.78f)
        } else 0.10f

        val recommendedLut = when {
            isLowLight && meanB >= meanR -> LutPreset.TOKYO_NEON
            isLowLight -> LutPreset.MOODY_DARK
            warmBalance > 0.12f -> LutPreset.WARM_GOLDEN
            meanSat < 0.25f -> LutPreset.KODAK_PORTRA
            else -> LutPreset.TEAL_ORANGE
        }

        val grading = ColorGrading(
            lutPreset = recommendedLut,
            filterIntensity = 0.62f,
            brightness = 0.03f,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            vignette = 0.18f,
            sharpen = if (isLowLight) 0.16f else 0.26f,
            temperature = temperature,
            tint = tint,
            highlights = highlights,
            shadows = shadows,
            fade = 0.04f,
            hdrBoost = hdrBoost,
            lowLightDenoise = lowLightDenoise
        )

        val tag = when {
            isLowLight -> "Low-Light Denoise ${(lowLightDenoise * 100).roundToInt()}% + HDR"
            isOverexposed -> "Highlight Recovery + HDR ${(hdrBoost * 100).roundToInt()}%"
            else -> "${recommendedLut.displayName} + HDR ${(hdrBoost * 100).roundToInt()}%"
        }
        return AiColorMatchResult(grading = grading, summary = "AI Color Match: $tag")
    }

    /**
     * Real on-device Object / Face Motion Tracker:
     * Samples frames across the target overlay clip's span from the underlying video track,
     * computes a skin-tone + high-contrast salient subject centroid per timestamp,
     * smooths jitter with an exponential moving average, and generates eased Keyframes.
     */
    fun trackSubjectKeyframes(
        targetClip: TimelineClip,
        mainClips: List<TimelineClip>,
        frameEngine: MediaFrameEngine,
        renderer: VideoCanvasRenderer
    ): List<Keyframe> {
        val sampleCount = (targetClip.durationMs / 450L).toInt().coerceIn(5, 12)
        val rawPoints = mutableListOf<Pair<Long, Pair<Float, Float>>>()

        for (i in 0..sampleCount) {
            val frac = i.toFloat() / sampleCount.toFloat()
            val localMs = (targetClip.durationMs * frac).toLong().coerceIn(0L, targetClip.durationMs)
            val timelineMs = targetClip.startMs + localMs
            val main = mainClips.firstOrNull { timelineMs in it.startMs..it.endMs } ?: mainClips.firstOrNull()
            val bmp = if (main != null) {
                val mainLocal = (timelineMs - main.startMs).coerceIn(0L, main.durationMs)
                val srcMs = renderer.evaluateSourceTimeMs(main, mainLocal)
                frameEngine.getClipFrameSync(
                    mediaUri = main.mediaUri,
                    drawableKey = main.drawableKey,
                    isVideoMedia = main.isVideoMedia,
                    sourceTimeMs = srcMs,
                    maxDim = 128
                )
            } else null

            val centroid = if (bmp != null) {
                detectSubjectCentroid(bmp)
            } else {
                // Graceful subtle organic anchor fallback when tracking over blank stage
                0f to -0.14f
            }
            rawPoints += localMs to centroid
        }

        // Exponential moving average smoothing to keep sticker/text lock rock-steady
        var smoothX = rawPoints.firstOrNull()?.second?.first ?: 0f
        var smoothY = rawPoints.firstOrNull()?.second?.second ?: -0.12f
        val alpha = 0.42f

        return rawPoints.mapIndexed { idx, (timeOffsetMs, pt) ->
            if (idx > 0) {
                smoothX = smoothX * (1f - alpha) + pt.first * alpha
                smoothY = smoothY * (1f - alpha) + pt.second * alpha
            }
            // Place text/sticker cleanly perched slightly above/near tracked subject without leaving frame
            val anchorX = smoothX.coerceIn(-0.34f, 0.34f)
            val anchorY = (smoothY - 0.14f).coerceIn(-0.38f, 0.34f)
            Keyframe(
                timeOffsetMs = timeOffsetMs,
                scale = 1.0f,
                offsetX = anchorX,
                offsetY = anchorY,
                rotationDeg = (anchorX * 6f).coerceIn(-4f, 4f),
                opacity = 1.0f,
                easing = KeyframeEasing.EASE_IN_OUT
            )
        }
    }

    /**
     * Weighted Skin-Tone & Salient Contrast Centroid Detector returning normalized (-0.5..0.5) coordinates.
     */
    private fun detectSubjectCentroid(bitmap: Bitmap): Pair<Float, Float> {
        val w = 48
        val h = 48
        val scaled = if (bitmap.width > w || bitmap.height > h) {
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else bitmap

        val sw = scaled.width
        val sh = scaled.height
        val pixels = IntArray(sw * sh)
        scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        if (scaled !== bitmap) scaled.recycle()

        // Estimate background border reference color
        var bgR = 0f
        var bgG = 0f
        var bgB = 0f
        var borderCnt = 0
        for (x in 0 until sw) {
            val topPx = pixels[x]
            val botPx = pixels[(sh - 1) * sw + x]
            bgR += Color.red(topPx) + Color.red(botPx)
            bgG += Color.green(topPx) + Color.green(botPx)
            bgB += Color.blue(topPx) + Color.blue(botPx)
            borderCnt += 2
        }
        val invBorder = 1f / max(1, borderCnt)
        bgR *= invBorder
        bgG *= invBorder
        bgB *= invBorder

        var weightSum = 0.0
        var sumX = 0.0
        var sumY = 0.0

        for (y in 2 until sh - 2) {
            val ny = (y.toFloat() / sh.toFloat()) - 0.5f
            for (x in 2 until sw - 2) {
                val nx = (x.toFloat() / sw.toFloat()) - 0.5f
                val px = pixels[y * sw + x]
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)

                // YCbCr skin-tone likelihood (works reliably across diverse skin tones)
                val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
                val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
                val isSkin = cb in 77f..127f && cr in 133f..175f && r > 60 && g > 35

                // Foreground color salience relative to border background
                val diff = (abs(r - bgR) + abs(g - bgG) + abs(b - bgB)) / (3f * 255f)
                val centerBias = (1f - (nx * nx + ny * ny) * 1.4f).coerceIn(0.15f, 1.0f)

                val weight = ((if (isSkin) 2.2f else 0f) + diff * 1.35f) * centerBias
                if (weight > 0.25f) {
                    weightSum += weight
                    sumX += nx * weight
                    sumY += ny * weight
                }
            }
        }

        if (weightSum < 1.0) return 0f to -0.05f
        val cx = (sumX / weightSum).toFloat().coerceIn(-0.42f, 0.42f)
        val cy = (sumY / weightSum).toFloat().coerceIn(-0.42f, 0.42f)
        return cx to cy
    }

    /**
     * Real-time export metrics & file size estimation aligned with 720p..32K / 24..240fps / 200 Mbps engine.
     */
    fun estimateExportMetrics(document: VideoProjectDocument): ExportEstimate {
        val targetShort = document.resolution.shortEdge
        val (rawW, rawH) = when (document.aspectRatio) {
            AspectRatioPreset.PORTRAIT_9_16 -> targetShort to (targetShort * 16f / 9f).roundToInt()
            AspectRatioPreset.LANDSCAPE_16_9 -> (targetShort * 16f / 9f).roundToInt() to targetShort
            AspectRatioPreset.SQUARE_1_1 -> targetShort to targetShort
            AspectRatioPreset.SOCIAL_4_5 -> targetShort to (targetShort * 5f / 4f).roundToInt()
        }
        val width = ((rawW / 16) * 16).coerceAtLeast(320)
        val height = ((rawH / 16) * 16).coerceAtLeast(320)

        val resBaseMbps = when (document.resolution) {
            ExportResolution.RES_720P -> 6.5f
            ExportResolution.RES_1080P -> 14.0f
            ExportResolution.RES_2K -> 24.0f
            ExportResolution.RES_4K -> 48.0f
            ExportResolution.RES_8K -> 85.0f
            ExportResolution.RES_16K -> 140.0f
            ExportResolution.RES_32K -> 185.0f
        }
        val fpsFactor = when (document.fps.fps) {
            24 -> 0.85f
            25 -> 0.88f
            30 -> 1.0f
            50 -> 1.18f
            60 -> 1.28f
            120 -> 1.55f
            240 -> 1.80f
            else -> 1.0f
        }
        val videoMbps = (resBaseMbps * document.bitrate.multiplier * fpsFactor)
            .coerceIn(2.0f, document.bitrate.maxMbps.coerceAtMost(200f))
        val audioMbps = if (document.audioEnhance.isMuted) 0f else 0.32f
        val totalMbps = videoMbps + audioMbps
        val durationSec = (document.totalDurationMs.coerceAtLeast(1000L) / 1000f)
        val estimatedSizeMb = ((totalMbps / 8f) * durationSec).coerceAtLeast(0.2f)
        val usesChunking = document.resolution in setOf(
            ExportResolution.RES_8K,
            ExportResolution.RES_16K,
            ExportResolution.RES_32K
        ) || document.fps.fps >= 120
        val totalEstFrames = ((durationSec * document.fps.fps.coerceAtMost(60)).roundToInt()).coerceAtLeast(15)
        val chunks = ((totalEstFrames + 29) / 30).coerceAtLeast(1)

        return ExportEstimate(
            width = width,
            height = height,
            targetFps = document.fps.fps,
            bitrateMbps = videoMbps,
            estimatedSizeMb = estimatedSizeMb,
            durationSec = durationSec,
            usesSmartChunking = usesChunking,
            chunkCount = chunks
        )
    }
}
