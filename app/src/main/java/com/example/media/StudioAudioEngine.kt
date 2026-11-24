package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.net.Uri
import com.example.model.AudioEffectPreset
import com.example.model.AudioEnhanceSettings
import com.example.model.AudioFilterPreset
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.VideoProjectDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class StudioAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var activeMediaUri: String? = null

    private var synthJob: Job? = null
    private var synthTrack: AudioTrack? = null

    @Volatile
    private var latestSettings: AudioEnhanceSettings = AudioEnhanceSettings()

    @Volatile
    private var latestPlayheadMs: Long = 0L

    private val _spectrumLevels = MutableStateFlow(List(16) { 0.08f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    fun updateSettings(settings: AudioEnhanceSettings, playheadMs: Long) {
        latestSettings = settings
        latestPlayheadMs = playheadMs
        applyHardwareAudioFx(settings, playheadMs)
    }

    fun startPlayback(document: VideoProjectDocument, playheadMs: Long) {
        stopPlayback()
        latestSettings = document.audioEnhance
        latestPlayheadMs = playheadMs
        if (document.audioEnhance.isMuted) {
            _spectrumLevels.value = List(16) { 0.04f }
            return
        }

        val audioClip = findPlayableMediaClip(document, playheadMs)
        if (audioClip?.mediaUri != null) {
            startMediaPlayer(audioClip, playheadMs, document.audioEnhance)
        } else if (document.clips.any { it.trackType == TrackType.AUDIO_MUSIC }) {
            startSynthAudioLoop()
        }
    }

    fun syncPlayhead(document: VideoProjectDocument, playheadMs: Long, isPlaying: Boolean) {
        latestPlayheadMs = playheadMs
        latestSettings = document.audioEnhance

        if (!isPlaying || document.audioEnhance.isMuted) {
            _spectrumLevels.value = List(16) { 0.05f }
            return
        }

        val cfg = document.audioEnhance
        val gain = computeEffectiveVolume(cfg, playheadMs)
        val t = (playheadMs / 1000.0) * cfg.audioEffect.speed.toDouble()
        val bassBoostExtra = if (cfg.audioFilter == AudioFilterPreset.DEEP_BASS_BOOST) 0.45f else 0f
        val voiceExtra = if (cfg.audioFilter == AudioFilterPreset.STUDIO_PODCAST) 0.35f else 0f
        val trebleExtra = if (cfg.audioFilter == AudioFilterPreset.TREBLE_CRISP) 0.40f else 0f

        val bassFactor = 0.65f + (cfg.bassBoost + bassBoostExtra) * 0.72f
        val voiceFactor = 0.65f + (cfg.voiceBoost + voiceExtra) * 0.68f
        val trebleFactor = (1f - cfg.noiseReduction * 0.40f) + trebleExtra

        _spectrumLevels.value = List(16) { band ->
            val bandWeight = when {
                band < 4 -> bassFactor
                band in 5..11 -> voiceFactor
                else -> trebleFactor
            }
            val wave = abs(sin(t * (5.2 + band * 0.9) + band * 0.7)).toFloat()
            val pulse = abs(sin(t * 2.13 * PI)).toFloat() * 0.45f
            ((wave * 0.6f + pulse) * bandWeight * gain).coerceIn(0.06f, 1.0f)
        }

        applyHardwareAudioFx(cfg, playheadMs)
    }

    fun stopPlayback() {
        synthJob?.cancel()
        synthJob = null
        runCatching {
            synthTrack?.pause()
            synthTrack?.flush()
            synthTrack?.release()
        }
        synthTrack = null

        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        runCatching { presetReverb?.release() }
        presetReverb = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        activeMediaUri = null
        _spectrumLevels.value = List(16) { 0.05f }
    }

    fun release() {
        stopPlayback()
        scope.cancel()
    }

    private fun findPlayableMediaClip(document: VideoProjectDocument, playheadMs: Long): TimelineClip? {
        val activeAudio = document.clips.firstOrNull {
            it.trackType == TrackType.AUDIO_MUSIC && !it.mediaUri.isNullOrBlank() && playheadMs in it.startMs..it.endMs
        }
        if (activeAudio != null) return activeAudio
        return document.clips.firstOrNull {
            it.trackType == TrackType.MAIN_VIDEO && it.isVideoMedia && !it.mediaUri.isNullOrBlank() && playheadMs in it.startMs..it.endMs
        }
    }

    private fun startMediaPlayer(clip: TimelineClip, playheadMs: Long, settings: AudioEnhanceSettings) {
        val uriStr = clip.mediaUri ?: return
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val parsed = Uri.parse(uriStr)
                if (parsed.scheme == "file" && !parsed.path.isNullOrBlank()) {
                    setDataSource(parsed.path)
                } else {
                    setDataSource(context, parsed)
                }
                prepare()
                val seekTarget = ((playheadMs - clip.startMs) + clip.sourceStartMs).toInt()
                    .coerceIn(0, (duration - 100).coerceAtLeast(0))
                seekTo(seekTarget)
            }
            mediaPlayer = mp
            activeMediaUri = uriStr
            attachAudioFx(mp.audioSessionId)
            applyHardwareAudioFx(settings, playheadMs)
            mp.start()
        }
    }

    private fun attachAudioFx(sessionId: Int) {
        runCatching { equalizer = Equalizer(0, sessionId).apply { enabled = true } }
        runCatching { bassBoost = BassBoost(0, sessionId).apply { enabled = true } }
        runCatching { loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true } }
        runCatching { presetReverb = PresetReverb(0, sessionId).apply { enabled = true } }
    }

    private fun applyHardwareAudioFx(settings: AudioEnhanceSettings, playheadMs: Long) {
        val vol = if (settings.isMuted) 0f else computeEffectiveVolume(settings, playheadMs).coerceIn(0f, 1f)
        runCatching { mediaPlayer?.setVolume(vol, vol) }

        // Apply Voice Changer / Speed & Pitch FX (Chipmunk, Deep Monster, Slowed + Reverb, Nightcore, Robot, Chorus)
        runCatching {
            val mp = mediaPlayer
            if (mp != null) {
                val intensity = settings.effectIntensity.coerceIn(0.15f, 1f)
                val targetPitch = 1f + (settings.audioEffect.pitch - 1f) * intensity
                val targetSpeed = 1f + (settings.audioEffect.speed - 1f) * intensity
                val params = PlaybackParams()
                    .setPitch(targetPitch.coerceIn(0.5f, 2.0f))
                    .setSpeed(targetSpeed.coerceIn(0.5f, 2.0f))
                mp.playbackParams = params
            }
        }

        // Apply Hall Reverb / Room Echo / Slowed+Reverb / Chorus via PresetReverb
        runCatching {
            val reverbPreset = when (settings.audioEffect) {
                AudioEffectPreset.HALL_REVERB -> PresetReverb.PRESET_LARGEHALL
                AudioEffectPreset.ROOM_ECHO -> PresetReverb.PRESET_MEDIUMROOM
                AudioEffectPreset.SLOWED_REVERB -> PresetReverb.PRESET_LARGEHALL
                AudioEffectPreset.CHORUS_DOUBLE -> PresetReverb.PRESET_PLATE
                AudioEffectPreset.ROBOT_METALLIC -> PresetReverb.PRESET_SMALLROOM
                else -> PresetReverb.PRESET_NONE
            }
            presetReverb?.preset = reverbPreset
        }

        // Apply BassBoost combining manual slider + Audio Filter preset
        runCatching {
            val extraBass = when (settings.audioFilter) {
                AudioFilterPreset.DEEP_BASS_BOOST -> 0.55f
                AudioFilterPreset.LOFI_VINYL -> 0.25f
                AudioFilterPreset.TELEPHONE_CALL, AudioFilterPreset.OLD_RADIO -> -0.5f
                else -> 0f
            }
            val strength = ((settings.bassBoost + extraBass).coerceIn(0f, 1f) * 950f).toInt().coerceIn(0, 1000)
            bassBoost?.setStrength(strength.toShort())
        }

        // Apply LoudnessEnhancer for Voice Boost & Podcast preset
        runCatching {
            val extraVoice = if (settings.audioFilter == AudioFilterPreset.STUDIO_PODCAST) 0.40f else 0f
            val gainMb = ((settings.voiceBoost + extraVoice).coerceIn(0f, 1.4f) * 750f).toInt().coerceIn(0, 1400)
            loudnessEnhancer?.setTargetGain(gainMb)
        }

        // Apply 9-mode Equalizer Filter Profile + manual sliders
        runCatching {
            equalizer?.let { eq ->
                val bands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLvl = range[0].toInt()
                val maxLvl = range[1].toInt()
                val strength = settings.effectIntensity.coerceIn(0.2f, 1f)

                for (b in 0 until bands) {
                    val centerHz = eq.getCenterFreq(b.toShort()) / 1000
                    val manualMb = when {
                        centerHz < 220 -> (settings.bassBoost * maxLvl * 0.55f - settings.noiseReduction * 220f).toInt()
                        centerHz in 900..4200 -> (settings.voiceBoost * maxLvl * 0.65f).toInt()
                        centerHz > 7500 -> (-settings.noiseReduction * abs(minLvl) * 0.40f).toInt()
                        else -> 0
                    }

                    val filterMb = when (settings.audioFilter) {
                        AudioFilterPreset.ORIGINAL -> 0
                        AudioFilterPreset.STUDIO_PODCAST -> when {
                            centerHz < 120 -> (minLvl * 0.35f).toInt()
                            centerHz in 1200..4500 -> (maxLvl * 0.55f).toInt()
                            else -> (maxLvl * 0.15f).toInt()
                        }
                        AudioFilterPreset.AI_NOISE_REDUCER -> when {
                            centerHz < 140 -> (minLvl * 0.55f).toInt()
                            centerHz > 6000 -> (minLvl * 0.50f).toInt()
                            else -> (maxLvl * 0.20f).toInt()
                        }
                        AudioFilterPreset.DEEP_BASS_BOOST -> when {
                            centerHz < 250 -> (maxLvl * 0.85f).toInt()
                            else -> (-maxLvl * 0.10f).toInt()
                        }
                        AudioFilterPreset.OLD_RADIO -> when {
                            centerHz < 400 || centerHz > 4000 -> (minLvl * 0.78f).toInt()
                            else -> (maxLvl * 0.55f).toInt()
                        }
                        AudioFilterPreset.TELEPHONE_CALL -> when {
                            centerHz < 500 || centerHz > 3200 -> (minLvl * 0.90f).toInt()
                            else -> (maxLvl * 0.45f).toInt()
                        }
                        AudioFilterPreset.UNDERWATER -> when {
                            centerHz < 350 -> (maxLvl * 0.40f).toInt()
                            else -> (minLvl * 0.88f).toInt()
                        }
                        AudioFilterPreset.TREBLE_CRISP -> when {
                            centerHz > 4000 -> (maxLvl * 0.82f).toInt()
                            else -> 0
                        }
                        AudioFilterPreset.LOFI_VINYL -> when {
                            centerHz < 260 -> (maxLvl * 0.30f).toInt()
                            centerHz > 5000 -> (minLvl * 0.62f).toInt()
                            else -> (maxLvl * 0.18f).toInt()
                        }
                    }

                    val combined = (manualMb + (filterMb * strength).toInt()).coerceIn(minLvl, maxLvl)
                    eq.setBandLevel(b.toShort(), combined.toShort())
                }
            }
        }
    }

    private fun startSynthAudioLoop() {
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        synthTrack = track
        runCatching { track.play() }

        synthJob = scope.launch {
            val buf = ShortArray(1024)
            var sampleCursor = 0L
            while (isActive) {
                val cfg = latestSettings
                val gain = if (cfg.isMuted) 0f else computeEffectiveVolume(cfg, latestPlayheadMs)
                val pitchMul = cfg.audioEffect.pitch.toDouble()
                val tempoMul = cfg.audioEffect.speed.toDouble()
                val underwaterDamp = if (cfg.audioFilter == AudioFilterPreset.UNDERWATER) 0.25 else 1.0
                val bassExtra = if (cfg.audioFilter == AudioFilterPreset.DEEP_BASS_BOOST) 0.35 else 0.0

                for (i in buf.indices) {
                    val t = ((sampleCursor + i).toDouble() / sampleRate) * tempoMul
                    val beatPhase = (t * 2.0) % 1.0
                    val kickEnv = kotlin.math.exp(-beatPhase * 9.0)
                    val bassTone = sin(2.0 * PI * (58.0 + 30.0 * kickEnv) * t) * kickEnv * (0.28 + cfg.bassBoost * 0.42 + bassExtra)
                    val chordFreq = when (((t / 2.0).toInt()) % 4) {
                        0 -> 220.0
                        1 -> 261.63
                        2 -> 196.0
                        else -> 246.94
                    } * pitchMul
                    val padTone = sin(2.0 * PI * chordFreq * t) * (0.14 + cfg.voiceBoost * 0.22) * underwaterDamp
                    val robotMod = if (cfg.audioEffect == AudioEffectPreset.ROBOT_METALLIC) {
                        0.6 + 0.4 * sin(2.0 * PI * 42.0 * t)
                    } else 1.0
                    val shimmer = sin(2.0 * PI * (chordFreq * 2.0) * t) * (0.06 * (1.0 - cfg.noiseReduction * 0.6)) * underwaterDamp
                    val mixed = ((bassTone + padTone + shimmer) * robotMod * gain * 0.45).coerceIn(-0.92, 0.92)
                    buf[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                }
                sampleCursor += buf.size
                val written = runCatching { track.write(buf, 0, buf.size) }.getOrDefault(-1)
                if (written < 0) break
            }
        }
    }

    fun computeEffectiveVolume(settings: AudioEnhanceSettings, playheadMs: Long): Float {
        val kfs = settings.volumeKeyframes.sortedBy { it.timeMs }
        if (kfs.isEmpty()) return settings.masterVolume.coerceIn(0f, 1.5f)
        if (playheadMs <= kfs.first().timeMs) return (kfs.first().volume * settings.masterVolume).coerceIn(0f, 1.5f)
        if (playheadMs >= kfs.last().timeMs) return (kfs.last().volume * settings.masterVolume).coerceIn(0f, 1.5f)
        for (i in 0 until kfs.lastIndex) {
            val a = kfs[i]
            val b = kfs[i + 1]
            if (playheadMs in a.timeMs..b.timeMs) {
                val t = (playheadMs - a.timeMs).toFloat() / (b.timeMs - a.timeMs).coerceAtLeast(1L).toFloat()
                val interp = a.volume + (b.volume - a.volume) * t
                return (interp * settings.masterVolume).coerceIn(0f, 1.5f)
            }
        }
        return settings.masterVolume.coerceIn(0f, 1.5f)
    }
}
