package com.example.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.VideoProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class BeatSyncAnalyzer(private val context: Context) {

    /**
     * Detects bass/energy beat peaks from a real imported local audio/video file via MediaExtractor + MediaCodec,
     * or falls back to musical peak analysis from waveform/duration when using built-in studio tracks.
     */
    suspend fun detectBeats(document: VideoProjectDocument): List<Long> = withContext(Dispatchers.IO) {
        val totalMs = document.totalDurationMs.coerceAtLeast(3000L)
        val mediaAudioClip = document.clips.firstOrNull {
            it.trackType == TrackType.AUDIO_MUSIC && !it.mediaUri.isNullOrBlank()
        } ?: document.clips.firstOrNull {
            it.trackType == TrackType.MAIN_VIDEO && it.isVideoMedia && !it.mediaUri.isNullOrBlank()
        }

        if (mediaAudioClip?.mediaUri != null) {
            val realBeats = runCatching {
                extractPcmBeats(Uri.parse(mediaAudioClip.mediaUri), mediaAudioClip.startMs, totalMs)
            }.getOrNull()
            if (!realBeats.isNullOrEmpty()) {
                return@withContext realBeats
            }
        }

        // Fallback: analyze waveform peaks on audio track or generate tight musical BPM grid
        val audioClip = document.clips.firstOrNull { it.trackType == TrackType.AUDIO_MUSIC }
        return@withContext detectBeatsFromWaveform(audioClip, totalMs)
    }

    private fun extractPcmBeats(uri: Uri, offsetMs: Long, maxDurationMs: Long): List<Long> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                extractor.setDataSource(uri.path!!)
            } else {
                extractor.setDataSource(context, uri, null)
            }
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return emptyList()

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // Collect RMS energy in 50ms windows (low-pass smoothed for bass kick onset)
            val windowEnergies = mutableListOf<Pair<Long, Float>>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val maxUs = (maxDurationMs.coerceAtMost(30_000L)) * 1000L
            var lowPassPrev = 0f

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        val pts = extractor.sampleTime
                        if (sampleSize < 0 || pts > maxUs) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 5000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        var sumSq = 0.0
                        var count = 0
                        val stride = (channels * 2).coerceAtLeast(1)
                        var idx = 0
                        while (idx < shortBuf.limit()) {
                            val raw = shortBuf.get(idx) / 32768f
                            // Simple 1-pole low-pass filter to emphasize bass kicks (< 180Hz)
                            lowPassPrev = lowPassPrev * 0.88f + raw * 0.12f
                            sumSq += (lowPassPrev * lowPassPrev)
                            count++
                            idx += stride
                        }
                        if (count > 0) {
                            val rms = sqrt(sumSq / count).toFloat()
                            val tMs = offsetMs + (bufferInfo.presentationTimeUs / 1000L)
                            if (tMs <= maxDurationMs) {
                                windowEnergies.add(tMs to rms)
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }

            if (windowEnergies.size < 4) return emptyList()
            val avg = windowEnergies.map { it.second }.average().toFloat()
            val threshold = avg * 1.28f
            val beats = mutableListOf<Long>()
            var lastBeatMs = -500L
            for (i in 1 until windowEnergies.lastIndex) {
                val (tMs, e) = windowEnergies[i]
                if (e >= threshold &&
                    e >= windowEnergies[i - 1].second &&
                    e >= windowEnergies[i + 1].second &&
                    tMs - lastBeatMs >= 360L
                ) {
                    beats.add(tMs)
                    lastBeatMs = tMs
                }
            }
            return beats
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun detectBeatsFromWaveform(audioClip: TimelineClip?, totalMs: Long): List<Long> {
        val bpm = Regex("(\\d{2,3})\\s*BPM", RegexOption.IGNORE_CASE)
            .find(audioClip?.label.orEmpty())
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.coerceIn(85, 165)
            ?: 124

        val beatIntervalMs = (60_000.0 / bpm).toLong().coerceIn(360L, 850L)
        val beats = mutableListOf<Long>()
        val peaks = audioClip?.waveformPeaks.orEmpty()
        var cursor = beatIntervalMs
        var step = 0
        while (cursor < totalMs - 200L) {
            val jitter = if (peaks.isNotEmpty()) {
                val p = peaks[step % peaks.size]
                if (p > 0.45f) 0L else 20L
            } else 0L
            beats.add((cursor + jitter).coerceIn(200L, totalMs - 100L))
            cursor += beatIntervalMs
            step++
        }
        return beats
    }
}
