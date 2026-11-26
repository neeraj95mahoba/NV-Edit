package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.model.VideoProjectDocument
import kotlin.math.max

/**
 * Real-time Cinema Color Scopes payload (RGB Parade, Luma Waveform, RGB/Luma Histogram).
 * Computed directly from rendered preview pixels at 48x28 resolution (< 1.5ms per frame).
 */
data class ColorScopeData(
    val columns: Int = 48,
    val bins: Int = 32,
    val paradeRMin: FloatArray = FloatArray(48),
    val paradeRMax: FloatArray = FloatArray(48),
    val paradeRAvg: FloatArray = FloatArray(48),
    val paradeGMin: FloatArray = FloatArray(48),
    val paradeGMax: FloatArray = FloatArray(48),
    val paradeGAvg: FloatArray = FloatArray(48),
    val paradeBMin: FloatArray = FloatArray(48),
    val paradeBMax: FloatArray = FloatArray(48),
    val paradeBAvg: FloatArray = FloatArray(48),
    val lumaMin: FloatArray = FloatArray(48),
    val lumaMax: FloatArray = FloatArray(48),
    val lumaAvg: FloatArray = FloatArray(48),
    val histR: FloatArray = FloatArray(32),
    val histG: FloatArray = FloatArray(32),
    val histB: FloatArray = FloatArray(32),
    val histLuma: FloatArray = FloatArray(32),
    val peakLumaIre: Int = 0,
    val avgLumaIre: Int = 0
)

object ColorScopeAnalyzer {
    private const val SAMPLE_W = 48
    private const val SAMPLE_H = 28
    private const val HIST_BINS = 32

    fun analyzeFrame(
        document: VideoProjectDocument,
        playheadMs: Long,
        comparingOriginal: Boolean,
        canvasRenderer: VideoCanvasRenderer,
        frameEngine: MediaFrameEngine
    ): ColorScopeData {
        val bmp = Bitmap.createBitmap(SAMPLE_W, SAMPLE_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvasRenderer.renderFrame(
            canvas = canvas,
            width = SAMPLE_W.toFloat(),
            height = SAMPLE_H.toFloat(),
            document = document,
            playheadMs = playheadMs,
            frameEngine = frameEngine,
            compareOriginal = comparingOriginal,
            maxDecodeDim = 96
        )

        val pixels = IntArray(SAMPLE_W * SAMPLE_H)
        bmp.getPixels(pixels, 0, SAMPLE_W, 0, 0, SAMPLE_W, SAMPLE_H)
        bmp.recycle()

        val pRMin = FloatArray(SAMPLE_W) { 1f }
        val pRMax = FloatArray(SAMPLE_W)
        val pRAvg = FloatArray(SAMPLE_W)
        val pGMin = FloatArray(SAMPLE_W) { 1f }
        val pGMax = FloatArray(SAMPLE_W)
        val pGAvg = FloatArray(SAMPLE_W)
        val pBMin = FloatArray(SAMPLE_W) { 1f }
        val pBMax = FloatArray(SAMPLE_W)
        val pBAvg = FloatArray(SAMPLE_W)
        val lMin = FloatArray(SAMPLE_W) { 1f }
        val lMax = FloatArray(SAMPLE_W)
        val lAvg = FloatArray(SAMPLE_W)

        val hR = FloatArray(HIST_BINS)
        val hG = FloatArray(HIST_BINS)
        val hB = FloatArray(HIST_BINS)
        val hL = FloatArray(HIST_BINS)

        var globalMaxLuma = 0f
        var globalLumaSum = 0f

        for (x in 0 until SAMPLE_W) {
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            var sumL = 0f
            for (y in 0 until SAMPLE_H) {
                val c = pixels[y * SAMPLE_W + x]
                val r = Color.red(c) / 255f
                val g = Color.green(c) / 255f
                val b = Color.blue(c) / 255f
                val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)

                if (r < pRMin[x]) pRMin[x] = r
                if (r > pRMax[x]) pRMax[x] = r
                sumR += r

                if (g < pGMin[x]) pGMin[x] = g
                if (g > pGMax[x]) pGMax[x] = g
                sumG += g

                if (b < pBMin[x]) pBMin[x] = b
                if (b > pBMax[x]) pBMax[x] = b
                sumB += b

                if (luma < lMin[x]) lMin[x] = luma
                if (luma > lMax[x]) lMax[x] = luma
                sumL += luma

                if (luma > globalMaxLuma) globalMaxLuma = luma
                globalLumaSum += luma

                hR[(r * (HIST_BINS - 1)).toInt().coerceIn(0, HIST_BINS - 1)] += 1f
                hG[(g * (HIST_BINS - 1)).toInt().coerceIn(0, HIST_BINS - 1)] += 1f
                hB[(b * (HIST_BINS - 1)).toInt().coerceIn(0, HIST_BINS - 1)] += 1f
                hL[(luma * (HIST_BINS - 1)).toInt().coerceIn(0, HIST_BINS - 1)] += 1f
            }
            pRAvg[x] = sumR / SAMPLE_H
            pGAvg[x] = sumG / SAMPLE_H
            pBAvg[x] = sumB / SAMPLE_H
            lAvg[x] = sumL / SAMPLE_H
        }

        val maxHist = max(
            max(hR.maxOrNull() ?: 1f, hG.maxOrNull() ?: 1f),
            max(hB.maxOrNull() ?: 1f, hL.maxOrNull() ?: 1f)
        ).coerceAtLeast(1f)
        for (i in 0 until HIST_BINS) {
            hR[i] = (hR[i] / maxHist).coerceIn(0f, 1f)
            hG[i] = (hG[i] / maxHist).coerceIn(0f, 1f)
            hB[i] = (hB[i] / maxHist).coerceIn(0f, 1f)
            hL[i] = (hL[i] / maxHist).coerceIn(0f, 1f)
        }

        val totalPx = (SAMPLE_W * SAMPLE_H).toFloat()
        return ColorScopeData(
            columns = SAMPLE_W,
            bins = HIST_BINS,
            paradeRMin = pRMin,
            paradeRMax = pRMax,
            paradeRAvg = pRAvg,
            paradeGMin = pGMin,
            paradeGMax = pGMax,
            paradeGAvg = pGAvg,
            paradeBMin = pBMin,
            paradeBMax = pBMax,
            paradeBAvg = pBAvg,
            lumaMin = lMin,
            lumaMax = lMax,
            lumaAvg = lAvg,
            histR = hR,
            histG = hG,
            histB = hB,
            histLuma = hL,
            peakLumaIre = (globalMaxLuma * 100f).toInt().coerceIn(0, 100),
            avgLumaIre = ((globalLumaSum / totalPx) * 100f).toInt().coerceIn(0, 100)
        )
    }
}
