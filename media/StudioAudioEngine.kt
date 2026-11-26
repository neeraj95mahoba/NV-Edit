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
import com.example.model.ViralSfxType
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
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

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

    @Volatile
    private var latestTotalDurationMs: Long = 10_000L

    @Volatile
    private var isVoiceActiveAtPlayhead: Boolean = false

    private val parametricFreqsHz = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

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
        latestTotalDurationMs = document.totalDurationMs.coerceAtLeast(1000L)
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
        latestTotalDurationMs = document.totalDurationMs.coerceAtLeast(1000L)
        isVoiceActiveAtPlayhead = document.clips.any {
            it.trackType == TrackType.TEXT_STICKER && playheadMs in it.startMs..it.endMs
        }

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

        val stem = cfg.stemMix
        val bassStemMul = if (stem.isSplitActive) stem.effectiveBass() else 1f
        val drumStemMul = if (stem.isSplitActive) stem.effectiveDrums() else 1f
        val vocalStemMul = if (stem.isSplitActive) stem.effectiveVocals() else 1f
        val melodyStemMul = if (stem.isSplitActive) stem.effectiveMelody() else 1f

        val bassFactor = (0.65f + (cfg.bassBoost + bassBoostExtra - cfg.voiceIsolation * 0.45f) * 0.72f) * bassStemMul
        val voiceFactor = (0.65f + (cfg.voiceBoost + voiceExtra + cfg.voiceIsolation * 0.55f) * 0.68f) * vocalStemMul
        val trebleFactor = ((1f - cfg.noiseReduction * 0.40f - cfg.voiceIsolation * 0.32f) + trebleExtra) * ((melodyStemMul + drumStemMul) * 0.5f)

        _spectrumLevels.value = List(16) { band ->
            val eqIdx = ((band * 10) / 16).coerceIn(0, 9)
            val eqDb = cfg.eqBandsDb.getOrElse(eqIdx) { 0f }.coerceIn(-12f, 12f)
            val eqMul = (1f + eqDb / 18f).coerceIn(0.35f, 1.75f)
            val bandWeight = when {
                band < 4 -> bassFactor
                band in 4..5 -> (bassFactor * 0.45f + drumStemMul * 0.75f)
                band in 6..11 -> voiceFactor
                else -> trebleFactor
            } * eqMul
            val wave = abs(sin(t * (5.2 + band * 0.9) + band * 0.7)).toFloat()
            val pulse = abs(sin(t * 2.13 * PI)).toFloat() * 0.45f * drumStemMul
            ((wave * 0.6f + pulse) * bandWeight * gain).coerceIn(0.05f, 1.0f)
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

        // Apply Voice Changer / Manual Pitch Shifter / Speed FX
        runCatching {
            val mp = mediaPlayer
            if (mp != null) {
                val intensity = settings.effectIntensity.coerceIn(0.15f, 1f)
                val fxPitch = 1f + (settings.audioEffect.pitch - 1f) * intensity
                val targetPitch = (fxPitch * settings.manualPitch).coerceIn(0.5f, 2.0f)
                val targetSpeed = (1f + (settings.audioEffect.speed - 1f) * intensity).coerceIn(0.5f, 2.0f)
                val params = PlaybackParams()
                    .setPitch(targetPitch)
                    .setSpeed(targetSpeed)
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

                    // Map nearest 10-band Parametric EQ dB setting + AI Voice Isolation curve
                    var nearestEqIdx = 0
                    var bestDiff = Int.MAX_VALUE
                    for (idx in parametricFreqsHz.indices) {
                        val diff = abs(centerHz - parametricFreqsHz[idx])
                        if (diff < bestDiff) {
                            bestDiff = diff
                            nearestEqIdx = idx
                        }
                    }
                    val parametricMb = (settings.eqBandsDb.getOrElse(nearestEqIdx) { 0f }.coerceIn(-12f, 12f) * 100f).toInt()
                    val voiceIsoMb = when {
                        centerHz in 750..4000 -> (settings.voiceIsolation * maxLvl * 0.60f).toInt()
                        else -> (-settings.voiceIsolation * abs(minLvl) * 0.65f).toInt()
                    }
                    val stemMb = if (settings.stemMix.isSplitActive) {
                        val level = when {
                            centerHz < 160 -> settings.stemMix.effectiveBass()
                            centerHz in 160..600 -> settings.stemMix.effectiveDrums()
                            centerHz in 601..3600 -> settings.stemMix.effectiveVocals()
                            else -> settings.stemMix.effectiveMelody()
                        }
                        if (level <= 0.02f) minLvl else ((level - 1f) * 900f).toInt()
                    } else 0

                    val combined = (manualMb + parametricMb + voiceIsoMb + stemMb + (filterMb * strength).toInt()).coerceIn(minLvl, maxLvl)
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
                val pitchMul = (cfg.audioEffect.pitch * cfg.manualPitch).coerceIn(0.5f, 2.0f).toDouble()
                val tempoMul = cfg.audioEffect.speed.toDouble()
                val underwaterDamp = if (cfg.audioFilter == AudioFilterPreset.UNDERWATER) 0.25 else 1.0
                val bassExtra = if (cfg.audioFilter == AudioFilterPreset.DEEP_BASS_BOOST) 0.35 else 0.0

                val stem = cfg.stemMix
                val bassGain = if (stem.isSplitActive) stem.effectiveBass().toDouble() else 1.0
                val drumsGain = if (stem.isSplitActive) stem.effectiveDrums().toDouble() else 1.0
                val vocalsGain = if (stem.isSplitActive) stem.effectiveVocals().toDouble() else 1.0
                val melodyGain = if (stem.isSplitActive) stem.effectiveMelody().toDouble() else 1.0

                for (i in buf.indices) {
                    val t = ((sampleCursor + i).toDouble() / sampleRate) * tempoMul
                    val beatPhase = (t * 2.0) % 1.0
                    val kickEnv = exp(-beatPhase * 9.0)
                    val bassTone = sin(2.0 * PI * (58.0 + 30.0 * kickEnv) * t) * kickEnv * (0.28 + cfg.bassBoost * 0.42 + bassExtra) * bassGain
                    val snareHit = exp(-(((t * 2.0 + 0.5) % 1.0)) * 22.0) * sin(2.0 * PI * 320.0 * t) * 0.14 * drumsGain
                    val chordFreq = when (((t / 2.0).toInt()) % 4) {
                        0 -> 220.0
                        1 -> 261.63
                        2 -> 196.0
                        else -> 246.94
                    } * pitchMul
                    val padTone = sin(2.0 * PI * chordFreq * t) * (0.14 + cfg.voiceBoost * 0.22) * underwaterDamp * vocalsGain
                    val robotMod = if (cfg.audioEffect == AudioEffectPreset.ROBOT_METALLIC) {
                        0.6 + 0.4 * sin(2.0 * PI * 42.0 * t)
                    } else 1.0
                    val shimmer = sin(2.0 * PI * (chordFreq * 2.0) * t) * (0.08 * (1.0 - cfg.noiseReduction * 0.6)) * underwaterDamp * melodyGain
                    val mixed = ((bassTone + snareHit + padTone + shimmer) * robotMod * gain * 0.45).coerceIn(-0.92, 0.92)
                    buf[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                }
                sampleCursor += buf.size
                val written = runCatching { track.write(buf, 0, buf.size) }.getOrDefault(-1)
                if (written < 0) break
            }
        }
    }

    /**
     * Synthesizes a genuine 16-bit PCM WAV file for any built-in Viral SFX (Whoosh, Bass Drop,
     * Click Pop, Applause, Cinematic Riser, Glitch Zap), caches it locally, and returns the file
     * plus extracted waveform peaks for the timeline.
     */
    fun synthesizeViralSfxWav(sfx: ViralSfxType): Pair<File, List<Float>> {
        val sampleRate = 22050
        val totalSamples = ((sfx.durationMs / 1000.0) * sampleRate).toInt().coerceAtLeast(2048)
        val pcm = ShortArray(totalSamples)
        val rng = Random(sfx.ordinal * 7919 + 42)
        var lpState = 0.0
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val u = i.toDouble() / totalSamples.toDouble()
            val t = i.toDouble() / sampleRate.toDouble()
            val sample = when (sfx) {
                ViralSfxType.WHOOSH -> {
                    val env = sin(u * PI)
                    val white = rng.nextDouble(-1.0, 1.0)
                    val cutoff = 0.05 + 0.35 * sin(u * PI)
                    lpState += cutoff * (white - lpState)
                    val tone = sin(2.0 * PI * (180.0 + 520.0 * sin(u * PI)) * t) * 0.25
                    (lpState * 0.85 + tone) * env * 0.90
                }
                ViralSfxType.BASS_DROP -> {
                    val freq = 135.0 * exp(-u * 2.4) + 34.0
                    phase += 2.0 * PI * freq / sampleRate
                    val env = if (u < 0.02) u / 0.02 else exp(-(u - 0.02) * 3.1)
                    val sub = sin(phase) * 0.88
                    val punch = sin(phase * 2.0) * exp(-u * 12.0) * 0.32
                    (sub + punch) * env
                }
                ViralSfxType.CLICK_POP -> {
                    val freq = 1400.0 * exp(-u * 16.0) + 260.0
                    phase += 2.0 * PI * freq / sampleRate
                    val env = exp(-u * 14.0)
                    val snap = rng.nextDouble(-1.0, 1.0) * exp(-u * 30.0) * 0.45
                    (sin(phase) * 0.72 + snap) * env
                }
                ViralSfxType.APPLAUSE -> {
                    val env = sin(u * PI).let { it * it } * 0.85 + 0.15 * exp(-u * 2.0)
                    val burst = 0.45 + 0.55 * abs(sin(t * 28.0) * sin(t * 43.0))
                    val white = rng.nextDouble(-1.0, 1.0)
                    lpState += 0.28 * (white - lpState)
                    (white - lpState * 0.55) * burst * env * 0.80
                }
                ViralSfxType.RISER -> {
                    val freq = 110.0 + (u * u) * 1150.0
                    phase += 2.0 * PI * freq / sampleRate
                    val white = rng.nextDouble(-1.0, 1.0)
                    lpState += (0.04 + 0.42 * u) * (white - lpState)
                    val env = if (u > 0.94) (1.0 - u) / 0.06 else u
                    (sin(phase) * 0.52 + lpState * 0.55) * env * 0.88
                }
                ViralSfxType.GLITCH_ZAP -> {
                    val stepHz = if (((t * 32.0).toInt() % 2) == 0) 980.0 else 320.0
                    phase += 2.0 * PI * stepHz / sampleRate
                    val sq = if (sin(phase) >= 0.0) 0.7 else -0.7
                    val env = exp(-u * 3.8)
                    val noise = rng.nextDouble(-1.0, 1.0) * 0.28
                    (sq + noise) * env * 0.78
                }
            }
            pcm[i] = (sample.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }

        val dir = File(context.cacheDir, "viral_sfx").apply { mkdirs() }
        val wavFile = File(dir, "sfx_${sfx.name.lowercase()}.wav")
        writePcm16Wav(wavFile, pcm, sampleRate)

        val bars = 28
        val chunk = (pcm.size / bars).coerceAtLeast(1)
        val peaks = List(bars) { b ->
            var maxAbs = 0
            val start = b * chunk
            val end = (start + chunk).coerceAtMost(pcm.size)
            for (k in start until end) {
                val a = abs(pcm[k].toInt())
                if (a > maxAbs) maxAbs = a
            }
            (maxAbs / 32767f).coerceIn(0.12f, 1.0f)
        }
        return wavFile to peaks
    }

    fun previewViralSfx(sfx: ViralSfxType) {
        scope.launch {
            val (file, _) = synthesizeViralSfxWav(sfx)
            runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener { player -> runCatching { player.release() } }
                    start()
                }
                mp.setVolume(0.95f, 0.95f)
            }
        }
    }

    private fun writePcm16Wav(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1) // PCM
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
        }
        val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { body.putShort(it) }
        FileOutputStream(file).use { out ->
            out.write(header.array())
            out.write(body.array())
        }
    }

    fun computeEffectiveVolume(settings: AudioEnhanceSettings, playheadMs: Long): Float {
        val kfs = settings.volumeKeyframes.sortedBy { it.timeMs }
        val baseVol = when {
            kfs.isEmpty() -> settings.masterVolume
            playheadMs <= kfs.first().timeMs -> kfs.first().volume * settings.masterVolume
            playheadMs >= kfs.last().timeMs -> kfs.last().volume * settings.masterVolume
            else -> {
                var resolved = settings.masterVolume
                for (i in 0 until kfs.lastIndex) {
                    val a = kfs[i]
                    val b = kfs[i + 1]
                    if (playheadMs in a.timeMs..b.timeMs) {
                        val t = (playheadMs - a.timeMs).toFloat() / (b.timeMs - a.timeMs).coerceAtLeast(1L).toFloat()
                        resolved = (a.volume + (b.volume - a.volume) * t) * settings.masterVolume
                        break
                    }
                }
                resolved
            }
        }
        val fadeInFactor = if (settings.fadeInMs > 50L && playheadMs < settings.fadeInMs) {
            (playheadMs.toFloat() / settings.fadeInMs.toFloat()).coerceIn(0f, 1f)
        } else 1f
        val remMs = (latestTotalDurationMs - playheadMs).coerceAtLeast(0L)
        val fadeOutFactor = if (settings.fadeOutMs > 50L && remMs < settings.fadeOutMs) {
            (remMs.toFloat() / settings.fadeOutMs.toFloat()).coerceIn(0f, 1f)
        } else 1f
        val duckFactor = if (settings.autoDucking && isVoiceActiveAtPlayhead) {
            val depthDb = settings.duckingDepthDb.coerceIn(-24f, -3f)
            Math.pow(10.0, (depthDb / 20.0)).toFloat().coerceIn(0.08f, 0.78f)
        } else 1.0f
        return (baseVol * fadeInFactor * fadeOutFactor * duckFactor).coerceIn(0f, 1.5f)
    }
}
