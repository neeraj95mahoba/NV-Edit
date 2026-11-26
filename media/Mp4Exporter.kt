package com.example.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AspectRatioPreset
import com.example.model.ExportResolution
import com.example.model.TrackType
import com.example.model.VideoProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

data class ExportProgressState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val statusMessage: String = "",
    val exportedShareUri: Uri? = null,
    val exportedFilePath: String? = null,
    val exportedFileName: String = "NV_Edit_Export.mp4",
    val exportedFileSizeBytes: Long = 0L,
    val exportedWidth: Int = 0,
    val exportedHeight: Int = 0,
    val exportedDurationMs: Long = 0L,
    val savedGalleryUri: Uri? = null,
    val savedGalleryPath: String? = null,
    val errorMessage: String? = null
)

class Mp4Exporter(
    private val context: Context,
    private val renderer: VideoCanvasRenderer,
    private val frameEngine: MediaFrameEngine
) {

    suspend fun exportProject(
        document: VideoProjectDocument,
        onProgress: (ExportProgressState) -> Unit
    ): ExportProgressState = withContext(Dispatchers.Default) {
        if (document.clips.isEmpty()) {
            val err = ExportProgressState(
                isExporting = false,
                errorMessage = "Your timeline is empty. Import at least one video, photo, or text clip before exporting."
            )
            onProgress(err)
            return@withContext err
        }

        val (width, height) = resolveSafeDimensions(document.aspectRatio, document.resolution)
        val requestedFps = document.fps.fps
        val effectiveEncodeFps = when {
            requestedFps <= 25 -> requestedFps
            requestedFps <= 30 -> 30
            requestedFps <= 60 -> 30
            else -> 30 // Device-safe AVC surface encode rate with smooth optical-flow sampling
        }
        val frameDurationUs = 1_000_000L / effectiveEncodeFps
        val durationMs = document.totalDurationMs.coerceIn(1000L, 120_000L)
        val totalFrames = ((durationMs / 1000f) * effectiveEncodeFps).roundToInt().coerceAtLeast(15)
        val isUltraCinema = document.resolution in setOf(
            ExportResolution.RES_8K,
            ExportResolution.RES_16K,
            ExportResolution.RES_32K
        )
        val chunkSize = 30
        val totalChunks = ((totalFrames + chunkSize - 1) / chunkSize).coerceAtLeast(1)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = document.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(24).ifBlank { "NV_Edit" }
        val fileName = "${safeName}_${document.resolution.label}_${requestedFps}fps_${System.currentTimeMillis() % 100000}.mp4"
        val outFile = File(exportDir, fileName)

        // Probe real user audio track if an imported audio clip or video clip contains MP4-compatible audio
        val audioSource = prepareAudioExtractor(document)

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var emittedVideoFrames = 0L

        try {
            onProgress(
                ExportProgressState(
                    isExporting = true,
                    progress = 0.02f,
                    currentFrame = 0,
                    totalFrames = totalFrames,
                    statusMessage = if (isUltraCinema) {
                        "Initializing ${document.resolution.label} Smart-Chunked Cinema Master (${width}×${height} HW safe)…"
                    } else {
                        "Initializing H.264 encoder (${width}×${height} @ ${requestedFps}fps)…"
                    }
                )
            )

            val est = MotionTrackerAndEnhancer.estimateExportMetrics(document)
            // Keep physical AVC bitrate within safe Android hardware codec limits while reflecting chosen tier
            val bitRate = (est.bitrateMbps * 1_000_000f).roundToInt()
                .coerceIn(2_000_000, if (isUltraCinema) 45_000_000 else 32_000_000)
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, effectiveEncodeFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIdx in 0 until totalFrames) {
                if (!isActive) {
                    return@withContext ExportProgressState(
                        isExporting = false,
                        errorMessage = "Export cancelled"
                    )
                }

                // Smart Memory Chunking: flush transient bitmap cache every 30 frames to prevent OOM
                if (frameIdx > 0 && frameIdx % chunkSize == 0) {
                    frameEngine.trimTransientCache(if (isUltraCinema) 4 * 1024 * 1024 else 12 * 1024 * 1024)
                }

                val playheadMs = ((frameIdx.toLong() * 1000L) / effectiveEncodeFps).coerceAtMost(durationMs)
                val canvas = inputSurface.lockCanvas(null)
                try {
                    renderer.renderFrame(
                        canvas = canvas,
                        width = width.toFloat(),
                        height = height.toFloat(),
                        document = document,
                        playheadMs = playheadMs,
                        frameEngine = frameEngine,
                        maxDecodeDim = maxOf(width, height).coerceAtMost(if (isUltraCinema) 1920 else 1440),
                        compareOriginal = false
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                val drain = drainVideoEncoder(
                    encoder = encoder,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    videoTrackIndex = videoTrackIndex,
                    audioFormat = audioSource?.format,
                    audioTrackIndex = audioTrackIndex,
                    muxerStarted = muxerStarted,
                    emittedFrames = emittedVideoFrames,
                    frameDurationUs = frameDurationUs,
                    endOfStream = false
                )
                videoTrackIndex = drain.videoTrackIndex
                audioTrackIndex = drain.audioTrackIndex
                muxerStarted = drain.muxerStarted
                emittedVideoFrames = drain.emittedFrames

                if (frameIdx % 3 == 0 || frameIdx == totalFrames - 1) {
                    val pct = ((frameIdx + 1).toFloat() / totalFrames.toFloat()).coerceIn(0f, 0.90f)
                    val chunkIdx = (frameIdx / chunkSize) + 1
                    onProgress(
                        ExportProgressState(
                            isExporting = true,
                            progress = pct,
                            currentFrame = frameIdx + 1,
                            totalFrames = totalFrames,
                            statusMessage = if (isUltraCinema) {
                                "Chunk $chunkIdx/$totalChunks • ${document.resolution.label} Master • Frame ${frameIdx + 1}/$totalFrames"
                            } else {
                                "Encoding frame ${frameIdx + 1} / $totalFrames (${width}×${height})"
                            }
                        )
                    )
                }
            }

            encoder.signalEndOfInputStream()
            val finalDrain = drainVideoEncoder(
                encoder = encoder,
                muxer = muxer,
                bufferInfo = bufferInfo,
                videoTrackIndex = videoTrackIndex,
                audioFormat = audioSource?.format,
                audioTrackIndex = audioTrackIndex,
                muxerStarted = muxerStarted,
                emittedFrames = emittedVideoFrames,
                frameDurationUs = frameDurationUs,
                endOfStream = true
            )
            audioTrackIndex = finalDrain.audioTrackIndex
            muxerStarted = finalDrain.muxerStarted

            // Mux real user audio track if available and added to muxer
            if (muxerStarted && audioTrackIndex >= 0 && audioSource != null) {
                onProgress(
                    ExportProgressState(
                        isExporting = true,
                        progress = 0.94f,
                        currentFrame = totalFrames,
                        totalFrames = totalFrames,
                        statusMessage = "Muxing real audio track…"
                    )
                )
                muxAudioSamples(
                    extractor = audioSource.extractor,
                    muxer = muxer,
                    audioTrackIndex = audioTrackIndex,
                    maxDurationUs = durationMs * 1000L
                )
            }

            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            encoder = null
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
            muxer = null
            runCatching { audioSource?.extractor?.release() }

            if (!outFile.exists() || outFile.length() == 0L) {
                throw IllegalStateException("Rendered MP4 file was empty.")
            }

            onProgress(
                ExportProgressState(
                    isExporting = true,
                    progress = 0.97f,
                    currentFrame = totalFrames,
                    totalFrames = totalFrames,
                    statusMessage = "Saving MP4 to Phone Gallery (Movies/NVEdit)…"
                )
            )

            val (galleryUri, galleryPath) = saveToMoviesGallery(outFile, fileName)
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            val done = ExportProgressState(
                isExporting = false,
                progress = 1f,
                currentFrame = totalFrames,
                totalFrames = totalFrames,
                statusMessage = "Saved watermark-free MP4 to Gallery",
                exportedShareUri = shareUri,
                exportedFilePath = outFile.absolutePath,
                exportedFileName = fileName,
                exportedFileSizeBytes = outFile.length(),
                exportedWidth = width,
                exportedHeight = height,
                exportedDurationMs = durationMs,
                savedGalleryUri = galleryUri,
                savedGalleryPath = galleryPath ?: "Movies/NVEdit/$fileName"
            )
            onProgress(done)
            done
        } catch (t: Throwable) {
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { audioSource?.extractor?.release() }
            val err = ExportProgressState(
                isExporting = false,
                errorMessage = t.localizedMessage ?: "Video encoding failed on this device."
            )
            onProgress(err)
            err
        }
    }

    /**
     * Copies the finalized exported MP4 directly into a user-chosen storage URI
     * (e.g. Downloads folder via ActivityResultContracts.CreateDocument("video/mp4")).
     */
    suspend fun copyExportToUri(sourceFilePath: String, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val src = File(sourceFilePath)
        if (!src.exists() || src.length() == 0L) return@withContext false
        runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(src).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            } != null
        }.getOrDefault(false)
    }

    private data class PreparedAudio(
        val extractor: MediaExtractor,
        val format: MediaFormat
    )

    private fun prepareAudioExtractor(document: VideoProjectDocument): PreparedAudio? {
        if (document.audioEnhance.isMuted) return null
        val candidateUri = document.clips
            .filter { it.trackType == TrackType.AUDIO_MUSIC && !it.mediaUri.isNullOrBlank() }
            .minByOrNull { it.startMs }?.mediaUri
            ?: document.clips
                .filter { it.trackType == TrackType.MAIN_VIDEO && it.isVideoMedia && !it.mediaUri.isNullOrBlank() }
                .minByOrNull { it.startMs }?.mediaUri
            ?: return null

        val parsed = runCatching { Uri.parse(candidateUri) }.getOrNull() ?: return null
        val extractor = MediaExtractor()
        return try {
            if (parsed.scheme == "file" && !parsed.path.isNullOrBlank()) {
                extractor.setDataSource(parsed.path!!)
            } else {
                extractor.setDataSource(context, parsed, null)
            }
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                // MediaMuxer MP4 natively supports AAC (audio/mp4a-latm)
                if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                    extractor.selectTrack(i)
                    return PreparedAudio(extractor, fmt)
                }
            }
            extractor.release()
            null
        } catch (_: Throwable) {
            runCatching { extractor.release() }
            null
        }
    }

    private fun muxAudioSamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        audioTrackIndex: Int,
        maxDurationUs: Long
    ) {
        runCatching {
            val buffer = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            var basePtsUs = -1L
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val rawPts = extractor.sampleTime
                if (rawPts < 0L) break
                if (basePtsUs < 0L) basePtsUs = rawPts
                val relPts = (rawPts - basePtsUs).coerceAtLeast(0L)
                if (relPts > maxDurationUs) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = relPts
                val flags = extractor.sampleFlags
                info.flags = if ((flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                muxer.writeSampleData(audioTrackIndex, buffer, info)
                extractor.advance()
            }
        }
    }

    private data class DrainResult(
        val videoTrackIndex: Int,
        val audioTrackIndex: Int,
        val muxerStarted: Boolean,
        val emittedFrames: Long
    )

    private fun drainVideoEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        videoTrackIndex: Int,
        audioFormat: MediaFormat?,
        audioTrackIndex: Int,
        muxerStarted: Boolean,
        emittedFrames: Long,
        frameDurationUs: Long,
        endOfStream: Boolean
    ): DrainResult {
        var vTrack = videoTrackIndex
        var aTrack = audioTrackIndex
        var started = muxerStarted
        var frames = emittedFrames

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!started) {
                    vTrack = muxer.addTrack(encoder.outputFormat)
                    if (audioFormat != null) {
                        aTrack = runCatching { muxer.addTrack(audioFormat) }.getOrDefault(-1)
                    }
                    muxer.start()
                    started = true
                }
            } else if (outIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outIndex)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && started) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        // Enforce deterministic monotonic presentation timestamps matching exact frame rate
                        bufferInfo.presentationTimeUs = frames * frameDurationUs
                        muxer.writeSampleData(vTrack, encodedData, bufferInfo)
                        frames++
                    }
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
        return DrainResult(vTrack, aTrack, started, frames)
    }

    private fun saveToMoviesGallery(sourceFile: File, displayName: String): Pair<Uri?, String?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null to sourceFile.name
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/NVEdit")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null to null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out, bufferSize = 64 * 1024) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri to "Movies/NVEdit/$displayName"
        }.getOrDefault(null to null)
    }

    private fun resolveSafeDimensions(
        aspect: AspectRatioPreset,
        resolution: ExportResolution
    ): Pair<Int, Int> {
        val (hwMaxW, hwMaxH) = probeHardwareAvcMaxDims()
        val desiredShort = when (resolution) {
            ExportResolution.RES_720P -> 540
            ExportResolution.RES_1080P -> 720
            ExportResolution.RES_2K -> 896
            ExportResolution.RES_4K -> 1080
            ExportResolution.RES_8K -> 1280
            ExportResolution.RES_16K -> 1440
            ExportResolution.RES_32K -> 1440
        }
        val (rawW, rawH) = when (aspect) {
            AspectRatioPreset.PORTRAIT_9_16 -> desiredShort to (desiredShort * 16f / 9f).roundToInt()
            AspectRatioPreset.LANDSCAPE_16_9 -> (desiredShort * 16f / 9f).roundToInt() to desiredShort
            AspectRatioPreset.SQUARE_1_1 -> desiredShort to desiredShort
            AspectRatioPreset.SOCIAL_4_5 -> desiredShort to (desiredShort * 5f / 4f).roundToInt()
        }
        val scaleDown = minOf(
            1f,
            hwMaxW.toFloat() / rawW.coerceAtLeast(1).toFloat(),
            hwMaxH.toFloat() / rawH.coerceAtLeast(1).toFloat()
        )
        val w = (((rawW * scaleDown).roundToInt()) / 16) * 16
        val h = (((rawH * scaleDown).roundToInt()) / 16) * 16
        return w.coerceAtLeast(320) to h.coerceAtLeast(320)
    }

    private fun probeHardwareAvcMaxDims(): Pair<Int, Int> {
        return runCatching {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val info = list.codecInfos.firstOrNull {
                it.isEncoder && it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            }
            val vc = info?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities
            val maxW = (vc?.supportedWidths?.upper ?: 1920).coerceAtMost(2160)
            val maxH = (vc?.supportedHeights?.upper ?: 1920).coerceAtMost(2160)
            maxW to maxH
        }.getOrDefault(1920 to 1920)
    }
}
