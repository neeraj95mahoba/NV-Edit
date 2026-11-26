package com.example.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlin.math.ln

/**
 * Hardware microphone audio recorder for capturing raw AAC (.m4a) audio
 * directly into the NV Edit studio timeline.
 */
class StudioAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording: Boolean = false

    fun startRecording(): Result<File> = runCatching {
        stopInternal(deleteFile = true)
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "NV_RAW_MIC_${System.currentTimeMillis()}.m4a")

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mr
        outputFile = file
        isRecording = true
        file
    }.onFailure {
        stopInternal(deleteFile = true)
    }

    /**
     * Polls real-time normalized microphone amplitude (0f..1f) for the VU waveform meter.
     */
    fun pollAmplitudeNormalized(): Float {
        if (!isRecording) return 0f
        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        if (raw <= 1) return 0.05f
        val norm = (ln(raw.toDouble() + 1.0) / ln(32768.0)).toFloat()
        return norm.coerceIn(0.06f, 1.0f)
    }

    fun stopAndSave(): File? {
        val saved = outputFile
        val ok = stopInternal(deleteFile = false)
        return if (ok && saved != null && saved.exists() && saved.length() > 128L) {
            saved
        } else {
            runCatching { saved?.delete() }
            null
        }
    }

    fun cancelAndDiscard() {
        stopInternal(deleteFile = true)
    }

    fun release() {
        stopInternal(deleteFile = false)
    }

    private fun stopInternal(deleteFile: Boolean): Boolean {
        val mr = recorder
        val file = outputFile
        var stoppedCleanly = false
        if (mr != null) {
            if (isRecording) {
                stoppedCleanly = runCatching {
                    mr.stop()
                    true
                }.getOrDefault(false)
            }
            runCatching { mr.reset() }
            runCatching { mr.release() }
        }
        recorder = null
        isRecording = false
        if (deleteFile) {
            runCatching { file?.delete() }
            outputFile = null
        }
        return stoppedCleanly
    }
}
