package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import com.example.model.StudioCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ImportedMediaInfo(
    val uriString: String,
    val displayName: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val isAudio: Boolean,
    val waveformPeaks: List<Float>
)

class MediaFrameEngine(private val context: Context) {

    // Memory-safe LRU bitmap cache (up to 40 MB) for smooth scrubbing and 60fps canvas preview
    private val bitmapCache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /**
     * Releases excess decoded frames during chunked 8K/16K/32K rendering passes so memory stays bounded.
     */
    fun trimTransientCache(maxBytes: Int = 8 * 1024 * 1024) {
        runCatching { bitmapCache.trimToSize(maxBytes.coerceAtLeast(0)) }
    }

    /**
     * Copies a picked content:// URI into app-private storage so the user's real video,
     * photo, or audio file remains 100% accessible across scrubbing, project reloads, and MP4 export
     * without expiring temporary Photo Picker permissions.
     */
    suspend fun persistPickedMediaLocally(uri: Uri): Pair<Uri, String?> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var originalName: String? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    originalName = cursor.getString(0)
                }
            }
        }

        // If already stored inside our app filesDir, reuse directly
        if (uri.scheme == "file" && uri.path?.startsWith(context.filesDir.absolutePath) == true) {
            return@withContext uri to originalName
        }

        val mime = runCatching { resolver.getType(uri) }.getOrNull().orEmpty()
        val ext = originalName?.substringAfterLast('.', "")
            ?.takeIf { it.length in 1..6 && it.all(Char::isLetterOrDigit) }
            ?: when {
                mime.contains("mp4") -> "mp4"
                mime.contains("webm") -> "webm"
                mime.contains("quicktime") || mime.contains("mov") -> "mov"
                mime.startsWith("video/") -> "mp4"
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                mime.startsWith("image/") -> "jpg"
                mime.contains("mpeg") -> "mp3"
                mime.contains("wav") -> "wav"
                mime.contains("aac") || mime.contains("mp4a") -> "m4a"
                mime.startsWith("audio/") -> "m4a"
                else -> "bin"
            }

        val mediaDir = File(context.filesDir, "imported_media").apply { mkdirs() }
        val targetFile = File(mediaDir, "media_${UUID.randomUUID().toString().take(12)}.$ext")

        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } != null && targetFile.length() > 0L
        }.getOrDefault(false)

        if (copied) {
            Uri.fromFile(targetFile) to originalName
        } else {
            runCatching { targetFile.delete() }
            uri to originalName
        }
    }

    fun inspectMedia(
        uri: Uri,
        fallbackVideo: Boolean,
        fallbackAudio: Boolean,
        knownDisplayName: String? = null
    ): ImportedMediaInfo {
        val resolver = context.contentResolver
        val mime = runCatching { resolver.getType(uri) }.getOrNull().orEmpty()
        val pathExt = uri.path?.substringAfterLast('.', "")?.lowercase().orEmpty()

        val extIsAudio = pathExt in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
        val extIsVideo = pathExt in setOf("mp4", "mov", "mkv", "webm", "3gp", "avi", "m4v")
        val extIsImage = pathExt in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")

        val isAudio = when {
            mime.startsWith("audio/") || extIsAudio -> true
            mime.startsWith("video/") || extIsVideo || mime.startsWith("image/") || extIsImage -> false
            else -> fallbackAudio
        }
        val isVideo = when {
            isAudio -> false
            mime.startsWith("video/") || extIsVideo -> true
            mime.startsWith("image/") || extIsImage -> false
            else -> fallbackVideo
        }

        var displayName = knownDisplayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: if (isAudio) "Audio Track" else if (isVideo) "Video Clip" else "Photo Clip"

        if (knownDisplayName.isNullOrBlank() && uri.scheme == "content") {
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val raw = cursor.getString(0)
                        if (!raw.isNullOrBlank()) {
                            displayName = raw.substringBeforeLast('.')
                        }
                    }
                }
            }
        }

        var durationMs = if (isVideo || isAudio) 6000L else 5000L
        if (isVideo || isAudio) {
            val retriever = MediaMetadataRetriever()
            runCatching {
                setRetrieverDataSource(retriever, uri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val parsed = durStr?.toLongOrNull()
                if (parsed != null && parsed > 0L) {
                    durationMs = parsed.coerceIn(500L, 600_000L)
                }
            }
            runCatching { retriever.release() }
        }

        val peaks = if (isAudio || isVideo) {
            extractQuickWaveformPeaks(uri, bars = 56)
                ?: StudioCatalog.generateWaveform(seed = uri.toString().hashCode() and 0xFF, bars = 56)
        } else {
            emptyList()
        }

        return ImportedMediaInfo(
            uriString = uri.toString(),
            displayName = displayName.take(32),
            durationMs = durationMs,
            isVideo = isVideo,
            isAudio = isAudio,
            waveformPeaks = peaks
        )
    }

    fun getDrawableBitmap(drawableKey: String?, maxDim: Int = 900): Bitmap? {
        if (drawableKey.isNullOrBlank()) return null
        val cacheKey = "drawable_${drawableKey}_$maxDim"
        bitmapCache.get(cacheKey)?.let { return it }

        val resId = StudioCatalog.drawableForKey(drawableKey)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeResource(context.resources, resId, opts)
        if (bmp != null) {
            bitmapCache.put(cacheKey, bmp)
        }
        return bmp
    }

    /**
     * Strictly resolves the clip's real mediaUri when present — NEVER substitutes a stock image
     * for user media. Only resolves drawableKey when mediaUri is null and drawableKey is non-null.
     */
    fun getClipFrameSync(
        mediaUri: String?,
        drawableKey: String?,
        isVideoMedia: Boolean,
        sourceTimeMs: Long,
        maxDim: Int = 720,
        bucketMs: Long = 160L
    ): Bitmap? {
        if (mediaUri.isNullOrBlank()) {
            return getDrawableBitmap(drawableKey, maxDim)
        }

        val parsedUri = runCatching { Uri.parse(mediaUri) }.getOrNull() ?: return null

        if (!isVideoMedia) {
            val cacheKey = "img_${mediaUri}_$maxDim"
            bitmapCache.get(cacheKey)?.let { return it }
            val decoded = decodeUriImage(parsedUri, maxDim)
            if (decoded != null) {
                bitmapCache.put(cacheKey, decoded)
            }
            return decoded
        }

        val bucket = (sourceTimeMs / bucketMs) * bucketMs
        val cacheKey = "vid_${mediaUri}_${bucket}_$maxDim"
        bitmapCache.get(cacheKey)?.let { return it }

        val extracted = extractVideoFrame(parsedUri, bucket * 1000L, maxDim)
        if (extracted != null) {
            bitmapCache.put(cacheKey, extracted)
            return extracted
        }

        // Fall back to frame 0 of the SAME user video (never a stock image)
        val firstFrameKey = "vid_${mediaUri}_0_$maxDim"
        bitmapCache.get(firstFrameKey)?.let { return it }
        val firstFrame = extractVideoFrame(parsedUri, 0L, maxDim)
        if (firstFrame != null) {
            bitmapCache.put(firstFrameKey, firstFrame)
        }
        return firstFrame
    }

    suspend fun prefetchVideoFrame(
        mediaUri: String,
        sourceTimeMs: Long,
        maxDim: Int = 720,
        bucketMs: Long = 160L
    ): Bitmap? = withContext(Dispatchers.IO) {
        getClipFrameSync(
            mediaUri = mediaUri,
            drawableKey = null,
            isVideoMedia = true,
            sourceTimeMs = sourceTimeMs,
            maxDim = maxDim,
            bucketMs = bucketMs
        )
    }

    suspend fun loadThumbnailBitmap(
        uriString: String?,
        drawableKey: String?,
        maxDim: Int = 420
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!uriString.isNullOrBlank()) {
            val cacheKey = "thumb_${uriString}_$maxDim"
            bitmapCache.get(cacheKey)?.let { return@withContext it }

            val parsedUri = runCatching { Uri.parse(uriString) }.getOrNull()
            if (parsedUri != null) {
                val mime = runCatching { context.contentResolver.getType(parsedUri) }.getOrNull().orEmpty()
                val ext = parsedUri.path?.substringAfterLast('.', "")?.lowercase().orEmpty()
                val looksLikeImage = mime.startsWith("image/") ||
                    ext in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")
                val decoded = if (looksLikeImage) {
                    decodeUriImage(parsedUri, maxDim)
                } else {
                    extractVideoFrame(parsedUri, 250_000L, maxDim)
                        ?: extractVideoFrame(parsedUri, 0L, maxDim)
                        ?: decodeUriImage(parsedUri, maxDim)
                }
                if (decoded != null) {
                    bitmapCache.put(cacheKey, decoded)
                    return@withContext decoded
                }
            }
            // Never substitute a stock photo when the project references user media
            return@withContext null
        }
        getDrawableBitmap(drawableKey, maxDim)
    }

    private fun setRetrieverDataSource(retriever: MediaMetadataRetriever, uri: Uri) {
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            retriever.setDataSource(uri.path)
        } else {
            retriever.setDataSource(context, uri)
        }
    }

    private fun extractVideoFrame(uri: Uri, timeUs: Long, maxDim: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            setRetrieverDataSource(retriever, uri)
            val raw = retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            raw?.let { scaleDownIfNeeded(it, maxDim) }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeUriImage(uri: Uri, maxDim: Int): Bitmap? {
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(uri.path, bounds)
                val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(uri.path, opts)?.let { scaleDownIfNeeded(it, maxDim) }
            } else {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?.let { scaleDownIfNeeded(it, maxDim) }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractQuickWaveformPeaks(uri: Uri, bars: Int): List<Float>? {
        val extractor = MediaExtractor()
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                extractor.setDataSource(uri.path!!)
            } else {
                extractor.setDataSource(context, uri, null)
            }
            var audioIdx = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    audioIdx = i
                    break
                }
            }
            if (audioIdx < 0) return null
            extractor.selectTrack(audioIdx)
            val buf = java.nio.ByteBuffer.allocate(8192)
            val sizes = ArrayList<Float>(bars * 2)
            var count = 0
            while (count < bars * 4) {
                val read = extractor.readSampleData(buf, 0)
                if (read <= 0) break
                sizes.add(read.toFloat())
                extractor.advance()
                count++
            }
            if (sizes.isEmpty()) return null
            val maxSize = sizes.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            List(bars) { i ->
                val idx = ((i.toFloat() / bars) * sizes.size).toInt().coerceIn(0, sizes.lastIndex)
                val norm = (sizes[idx] / maxSize)
                (0.18f + abs(norm) * 0.82f).coerceIn(0.14f, 1.0f)
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val maxSide = max(src.width, src.height)
        if (maxSide <= maxDim) return src
        val scale = maxDim.toFloat() / maxSide.toFloat()
        val w = max(1, (src.width * scale).toInt())
        val h = max(1, (src.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        if (scaled != src) src.recycle()
        return scaled
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (max(width / sample, height / sample) > maxDim * 2) {
            sample *= 2
        }
        return min(sample, 8)
    }
}
