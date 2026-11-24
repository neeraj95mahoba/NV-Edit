package com.example.media

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import com.example.model.CutoutMode
import com.example.model.CutoutSettings
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CutoutProcessor {

    private val processedCache = object : LruCache<String, Bitmap>(28 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /**
     * Processes a bitmap using either Chroma Key (color distance alpha masking + spill suppression)
     * or Smart Auto-Cutout (border background color modeling + center saliency & edge contrast).
     */
    fun process(source: Bitmap, settings: CutoutSettings): Bitmap {
        if (settings.mode == CutoutMode.OFF) return source

        val quantIntensity = (settings.intensity * 25f).roundToInt()
        val quantShadow = (settings.shadowEdge * 25f).roundToInt()
        val cacheKey = "${System.identityHashCode(source)}_${settings.mode.name}_${settings.keyColorHex}_${quantIntensity}_$quantShadow"
        processedCache.get(cacheKey)?.let { return it }

        // Downscale to fast real-time working resolution (up to 360px) to maintain silky 60fps
        val maxSide = max(source.width, source.height)
        val scale = if (maxSide > 360) 360f / maxSide.toFloat() else 1f
        val w = max(16, (source.width * scale).roundToInt())
        val h = max(16, (source.height * scale).roundToInt())
        val workBmp = if (scale < 1f) Bitmap.createScaledBitmap(source, w, h, true) else source.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(w * h)
        workBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val outPixels = when (settings.mode) {
            CutoutMode.CHROMA_KEY -> applyChromaKey(pixels, settings)
            CutoutMode.AUTO_CUTOUT -> applySmartAutoCutout(pixels, w, h, settings)
            CutoutMode.OFF -> pixels
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, w, 0, 0, w, h)
        if (workBmp != source) workBmp.recycle()

        processedCache.put(cacheKey, outBitmap)
        return outBitmap
    }

    private fun applyChromaKey(pixels: IntArray, settings: CutoutSettings): IntArray {
        val keyInt = settings.keyColorHex.toInt()
        val kr = Color.red(keyInt)
        val kg = Color.green(keyInt)
        val kb = Color.blue(keyInt)

        // Convert key color to Cb/Cr chroma coordinates for shadow-resilient keying
        val keyCb = -0.1687f * kr - 0.3313f * kg + 0.5000f * kb + 128f
        val keyCr = 0.5000f * kr - 0.4187f * kg - 0.0813f * kb + 128f

        val tol = (22f + settings.intensity * 130f)
        val feather = (14f + settings.shadowEdge * 72f)
        val out = IntArray(pixels.size)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            val cb = -0.1687f * r - 0.3313f * g + 0.5000f * b + 128f
            val cr = 0.5000f * r - 0.4187f * g - 0.0813f * b + 128f

            val chromaDist = hypot((cb - keyCb).toDouble(), (cr - keyCr).toDouble()).toFloat()
            val rgbDist = sqrt(((r - kr) * (r - kr) + (g - kg) * (g - kg) + (b - kb) * (b - kb)).toDouble()).toFloat() * 0.45f
            val dist = chromaDist * 0.65f + rgbDist * 0.35f

            if (dist <= tol) {
                out[i] = Color.argb(0, r, g, b)
            } else if (dist < tol + feather) {
                val factor = ((dist - tol) / feather).coerceIn(0f, 1f)
                val newAlpha = (a * factor * factor).roundToInt().coerceIn(0, 255)
                // Gentle spill suppression along feathered edge
                val sr = (r * factor + (r + g + b) / 3f * (1f - factor)).roundToInt().coerceIn(0, 255)
                val sg = (g * factor + (r + g + b) / 3f * (1f - factor)).roundToInt().coerceIn(0, 255)
                val sb = (b * factor + (r + g + b) / 3f * (1f - factor)).roundToInt().coerceIn(0, 255)
                out[i] = Color.argb(newAlpha, sr, sg, sb)
            } else {
                out[i] = c
            }
        }
        return out
    }

    private fun applySmartAutoCutout(pixels: IntArray, w: Int, h: Int, settings: CutoutSettings): IntArray {
        // 1. Sample 4 corners & top/side borders to estimate background color distribution
        var bgR = 0L
        var bgG = 0L
        var bgB = 0L
        var bgCount = 0L
        for (x in 0 until w step 3) {
            val topC = pixels[x]
            bgR += Color.red(topC); bgG += Color.green(topC); bgB += Color.blue(topC); bgCount++
            val botC = pixels[(h - 1) * w + x]
            bgR += Color.red(botC); bgG += Color.green(botC); bgB += Color.blue(botC); bgCount++
        }
        for (y in 0 until h step 3) {
            val leftC = pixels[y * w]
            bgR += Color.red(leftC); bgG += Color.green(leftC); bgB += Color.blue(leftC); bgCount++
            val rightC = pixels[y * w + (w - 1)]
            bgR += Color.red(rightC); bgG += Color.green(rightC); bgB += Color.blue(rightC); bgCount++
        }
        val avgBgR = (bgR / max(1L, bgCount)).toFloat()
        val avgBgG = (bgG / max(1L, bgCount)).toFloat()
        val avgBgB = (bgB / max(1L, bgCount)).toFloat()

        val threshold = 48f + settings.intensity * 105f
        val feather = 24f + settings.shadowEdge * 65f
        val cx = w * 0.5f
        val cy = h * 0.52f
        val out = IntArray(pixels.size)

        for (y in 0 until h) {
            val dy = (y - cy) / (h * 0.52f)
            val rowOffset = y * w
            for (x in 0 until w) {
                val c = pixels[rowOffset + x]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)

                val dx = (x - cx) / (w * 0.44f)
                val radial = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Local horizontal/vertical contrast gradient (strong subject edges stay opaque)
                val rightC = pixels[rowOffset + min(w - 1, x + 1)]
                val downC = pixels[min(h - 1, y + 1) * w + x]
                val edgeContrast = (abs(r - Color.red(rightC)) + abs(g - Color.green(rightC)) +
                    abs(r - Color.red(downC)) + abs(g - Color.green(downC))) * 0.35f

                val bgDist = sqrt(
                    ((r - avgBgR) * (r - avgBgR) +
                        (g - avgBgG) * (g - avgBgG) +
                        (b - avgBgB) * (b - avgBgB)).toDouble()
                ).toFloat()

                // Center subject prior boosts foreground confidence inside portrait silhouette oval
                val subjectScore = bgDist + edgeContrast + (1.05f - radial).coerceIn(-0.45f, 0.85f) * 82f

                if (subjectScore <= threshold) {
                    out[rowOffset + x] = Color.argb(0, r, g, b)
                } else if (subjectScore < threshold + feather) {
                    val f = ((subjectScore - threshold) / feather).coerceIn(0f, 1f)
                    val smooth = f * f * (3f - 2f * f)
                    out[rowOffset + x] = Color.argb((smooth * 255f).roundToInt().coerceIn(0, 255), r, g, b)
                } else {
                    out[rowOffset + x] = c
                }
            }
        }
        return out
    }
}
