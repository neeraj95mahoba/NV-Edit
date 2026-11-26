package com.example.model

import androidx.annotation.DrawableRes
import com.example.R
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

object StudioCatalog {

    @DrawableRes
    fun drawableForKey(key: String?): Int = when (key) {
        "tpl_beat_sync" -> R.drawable.img_tpl_beat_sync_1795178916338
        "tpl_cinematic_travel" -> R.drawable.img_tpl_cinematic_travel_1795178905731
        "tpl_velocity" -> R.drawable.img_tpl_velocity_1795178906858
        "tpl_retro_vhs" -> R.drawable.img_tpl_retro_vhs_1795178903004
        else -> R.drawable.img_tpl_beat_sync_1795178916338
    }

    fun generateWaveform(seed: Int, bars: Int = 48): List<Float> =
        List(bars) { idx ->
            val a = abs(sin((idx + seed * 3) * 0.42))
            val b = abs(sin((idx * 1.3 + seed) * 0.17))
            ((a * 0.65 + b * 0.35).toFloat()).coerceIn(0.14f, 1.0f)
        }

    val templates: List<VideoTemplate> = listOf(
        VideoTemplate(
            id = "hollywood_trailer",
            title = "Hollywood Trailer",
            subtitle = "3 slots • IMAX 16:9 24fps, 35mm Film Grain + Halation & 3D Cinema Titles",
            category = "Cinematic",
            aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
            durationMs = 11_400L,
            bpm = 100,
            drawableKey = "tpl_cinematic_travel",
            tags = listOf("IMAX 16:9", "35mm Grain", "Halation"),
            usesLabel = "912K uses",
            accentHex = 0xFFFFD84D,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Hollywood IMAX Trailer",
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    resolution = ExportResolution.RES_8K,
                    fps = ExportFps.FPS_24,
                    bitrate = ExportBitrate.ULTRA_PRO,
                    thumbnailDrawableKey = "tpl_cinematic_travel",
                    beatMarkersMs = listOf(1800L, 3800L, 5800L, 7600L, 9600L),
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.IMAX_BLOCKBUSTER,
                        filterIntensity = 0.85f,
                        contrast = 0.22f,
                        hdrBoost = 0.48f,
                        vignette = 0.36f,
                        filmGrain = 0.28f,
                        halation = 0.40f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.LIGHT_LEAK_WARM, intensity = 0.32f, motionBlur = 0.28f),
                    audioEnhance = AudioEnhanceSettings(
                        noiseReduction = 0.35f,
                        voiceBoost = 0.55f,
                        bassBoost = 0.78f,
                        fadeInMs = 700L,
                        fadeOutMs = 1300L,
                        autoDucking = true
                    ),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Act I • Opening Wide",
                            startMs = 0L,
                            durationMs = 3800L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFFFD84D,
                            smoothSlowMo = true,
                            speedCurvePreset = SpeedCurvePreset.HERO,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.HERO),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.02f, easing = KeyframeEasing.EASE_IN_OUT),
                                Keyframe(timeOffsetMs = 3700L, scale = 1.24f, easing = KeyframeEasing.EASE_OUT)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Act II • Action Escalation",
                            startMs = 3800L,
                            durationMs = 3800L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF00F0FF,
                            transitionIn = ClipTransition(type = TransitionType.DIP_BLACK, durationMs = 500L),
                            speedCurvePreset = SpeedCurvePreset.BULLET,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.BULLET),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.28f, rotationDeg = -1.5f, easing = KeyframeEasing.EASE_IN),
                                Keyframe(timeOffsetMs = 3700L, scale = 1.06f, rotationDeg = 1.0f, easing = KeyframeEasing.EASE_OUT)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Act III • Climax Reveal",
                            startMs = 7600L,
                            durationMs = 3800L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF9B5CFF,
                            transitionIn = ClipTransition(type = TransitionType.FLASH_WHITE, durationMs = 450L),
                            speedCurvePreset = SpeedCurvePreset.FLASH_IN,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.FLASH_IN),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.18f),
                                Keyframe(timeOffsetMs = 3700L, scale = 1.02f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "THIS SUMMER",
                            startMs = 500L,
                            durationMs = 3100L,
                            accentColorHex = 0xFFFFD84D,
                            textStyle = TextClipStyle(
                                text = "THIS SUMMER • IN IMAX",
                                fontStyle = TextFontStyle.CINZEL_IMAX,
                                effectStyle = TextEffectStyle.EXTRUDE_3D,
                                animationType = TextAnimationType.FADE_UP,
                                textColorHex = 0xFFFFF5E1,
                                accentColorHex = 0xFFFFD84D,
                                fontSizeSp = 30f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "THE FINAL CUT",
                            startMs = 7900L,
                            durationMs = 3200L,
                            accentColorHex = 0xFF00F0FF,
                            textStyle = TextClipStyle(
                                text = "THE FINAL CUT",
                                fontStyle = TextFontStyle.CINZEL_EPIC,
                                effectStyle = TextEffectStyle.NEON_GLOW,
                                animationType = TextAnimationType.POP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 38f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "IMAX Sub-Bass Score 100 BPM",
                            startMs = 0L,
                            durationMs = 11400L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 99)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "beat_sync_attitude",
            title = "Beat-Sync Attitude",
            subtitle = "3 slots • 128 BPM beat cuts + RGB glitch & bold attitude titles",
            category = "Trending",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 10_200L,
            bpm = 128,
            drawableKey = "tpl_beat_sync",
            tags = listOf("3 Slots", "Beat Sync", "RGB Glitch"),
            usesLabel = "842K uses",
            accentHex = 0xFF00F0FF,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Beat-Sync Attitude Reel",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_beat_sync",
                    beatMarkersMs = listOf(1100L, 2200L, 3400L, 4800L, 6200L, 7600L, 9000L),
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.VIBRANT_REEL,
                        contrast = 0.18f,
                        saturation = 0.14f,
                        hdrBoost = 0.35f,
                        vignette = 0.25f
                    ),
                    visualFx = VisualFxSettings(
                        activeFx = VisualFxType.RETRO_GLITCH,
                        intensity = 0.48f,
                        speed = 1.25f,
                        motionBlur = 0.32f
                    ),
                    audioEnhance = AudioEnhanceSettings(
                        noiseReduction = 0.30f,
                        voiceBoost = 0.45f,
                        bassBoost = 0.72f
                    ),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 1 • Entry",
                            startMs = 0L,
                            durationMs = 3400L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF9B5CFF,
                            speedCurvePreset = SpeedCurvePreset.MONTAGE,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.MONTAGE),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.04f, offsetY = 0.02f),
                                Keyframe(timeOffsetMs = 1700L, scale = 1.26f, offsetX = -0.04f, rotationDeg = -2.5f),
                                Keyframe(timeOffsetMs = 3300L, scale = 1.12f, offsetX = 0.03f, rotationDeg = 1.5f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 2 • Beat Drop",
                            startMs = 3400L,
                            durationMs = 3400L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF00F0FF,
                            transitionIn = ClipTransition(type = TransitionType.GLITCH_ZOOM, durationMs = 550L),
                            speedCurvePreset = SpeedCurvePreset.FLASH_IN,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.FLASH_IN),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.32f, rotationDeg = 3f),
                                Keyframe(timeOffsetMs = 1700L, scale = 1.06f, rotationDeg = 0f),
                                Keyframe(timeOffsetMs = 3300L, scale = 1.22f, offsetY = -0.03f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 3 • Attitude Outro",
                            startMs = 6800L,
                            durationMs = 3400L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFFFD84D,
                            transitionIn = ClipTransition(type = TransitionType.WHIP_PAN_LEFT, durationMs = 500L),
                            speedCurvePreset = SpeedCurvePreset.JUMP_CUT,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.JUMP_CUT),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.18f),
                                Keyframe(timeOffsetMs = 3300L, scale = 1.02f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "OWN THE FRAME",
                            startMs = 300L,
                            durationMs = 3100L,
                            accentColorHex = 0xFFFFD84D,
                            textStyle = TextClipStyle(
                                text = "OWN THE FRAME",
                                fontStyle = TextFontStyle.ANTON_IMPACT,
                                effectStyle = TextEffectStyle.EXTRUDE_3D,
                                animationType = TextAnimationType.WORD_POP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 38f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "LEVEL UP 🔥",
                            startMs = 3600L,
                            durationMs = 3200L,
                            accentColorHex = 0xFFFF2E93,
                            textStyle = TextClipStyle(
                                text = "LEVEL UP 60FPS",
                                fontStyle = TextFontStyle.HINDI_TEKO,
                                effectStyle = TextEffectStyle.MULTI_SHADOW,
                                animationType = TextAnimationType.GLITCH,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFFFF2E93,
                                fontSizeSp = 36f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Attitude Phonk 128 BPM",
                            startMs = 0L,
                            durationMs = 10200L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 11)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "cinematic_slow_mo",
            title = "Cinematic Slow-Mo",
            subtitle = "2 slots • Optical-flow 0.3x smooth slow-mo + Teal & Orange HDR",
            category = "Cinematic",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 11_000L,
            bpm = 96,
            drawableKey = "tpl_cinematic_travel",
            tags = listOf("2 Slots", "Smooth 0.3x", "Teal & Orange"),
            usesLabel = "615K uses",
            accentHex = 0xFFFFB020,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Cinematic Slow-Mo Film",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_cinematic_travel",
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.TEAL_ORANGE,
                        exposure = 0.08f,
                        contrast = 0.16f,
                        hdrBoost = 0.42f,
                        vignette = 0.28f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.HALO_BLUR, intensity = 0.28f, motionBlur = 0.25f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.45f, voiceBoost = 0.45f, bassBoost = 0.35f, fadeInMs = 800L, fadeOutMs = 1200L),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 1 • Hero Slow-Mo",
                            startMs = 0L,
                            durationMs = 5800L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFFFB020,
                            speed = 0.5f,
                            smoothSlowMo = true,
                            speedCurvePreset = SpeedCurvePreset.HERO,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.HERO),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.02f, offsetY = 0.03f),
                                Keyframe(timeOffsetMs = 5700L, scale = 1.24f, offsetY = -0.03f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 2 • Bullet Ramp",
                            startMs = 5800L,
                            durationMs = 5200L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF38BDF8,
                            speed = 0.5f,
                            smoothSlowMo = true,
                            transitionIn = ClipTransition(type = TransitionType.LIGHT_LEAK, durationMs = 750L),
                            speedCurvePreset = SpeedCurvePreset.BULLET,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.BULLET),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.18f, offsetX = -0.03f),
                                Keyframe(timeOffsetMs = 5100L, scale = 1.02f, offsetX = 0.03f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "GOLDEN MOMENT",
                            startMs = 700L,
                            durationMs = 4600L,
                            accentColorHex = 0xFFFFD84D,
                            textStyle = TextClipStyle(
                                text = "CINEMATIC SLOW-MO",
                                fontStyle = TextFontStyle.CINZEL_ROYAL,
                                effectStyle = TextEffectStyle.DROP_SHADOW,
                                animationType = TextAnimationType.FADE_UP,
                                textColorHex = 0xFFFFF5E1,
                                accentColorHex = 0xFFFFB020,
                                fontSizeSp = 28f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Cinematic Ambient 96 BPM",
                            startMs = 0L,
                            durationMs = 11000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 23)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "lyrical_song_status",
            title = "Lyrical Song Status",
            subtitle = "2 slots • Word-by-word Karaoke Hindi/English lyrics + Neon Visualizer",
            category = "Lyrical",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 10_000L,
            bpm = 110,
            drawableKey = "tpl_retro_vhs",
            tags = listOf("2 Slots", "Karaoke", "Visualizer"),
            usesLabel = "734K uses",
            accentHex = 0xFFFF2E93,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Lyrical Song Status",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_retro_vhs",
                    musicVisualizer = MusicVisualizerStyle.NEON_BARS,
                    beatMarkersMs = listOf(1250L, 2500L, 3750L, 5000L, 6250L, 7500L, 8750L),
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.WARM_SUNSET,
                        contrast = 0.12f,
                        vignette = 0.35f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.DREAMY_BLOOM, intensity = 0.45f, speed = 1.0f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.25f, voiceBoost = 0.60f, bassBoost = 0.50f, fadeInMs = 600L, fadeOutMs = 1000L),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 1 • Verse Scene",
                            startMs = 0L,
                            durationMs = 5000L,
                            drawableKey = "tpl_retro_vhs",
                            accentColorHex = 0xFFFF2E93,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.04f),
                                Keyframe(timeOffsetMs = 4900L, scale = 1.18f, offsetX = 0.03f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 2 • Chorus Glow",
                            startMs = 5000L,
                            durationMs = 5000L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFF9B5CFF,
                            transitionIn = ClipTransition(type = TransitionType.CROSS_FADE, durationMs = 700L),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.16f),
                                Keyframe(timeOffsetMs = 4900L, scale = 1.04f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "Lyric Line 1",
                            startMs = 400L,
                            durationMs = 4400L,
                            accentColorHex = 0xFFFFD84D,
                            textStyle = TextClipStyle(
                                text = "दिल की हर धड़कन में तेरा नाम है",
                                fontStyle = TextFontStyle.HINDI_ROZHA,
                                effectStyle = TextEffectStyle.NEON_GLOW,
                                animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                                loopAnimation = TextAnimLoop.FLOATING_HOVER,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFFFFD84D,
                                fontSizeSp = 30f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "Lyric Line 2",
                            startMs = 5100L,
                            durationMs = 4500L,
                            accentColorHex = 0xFF00F0FF,
                            textStyle = TextClipStyle(
                                text = "EVERY BEAT FEELS LIKE MAGIC",
                                fontStyle = TextFontStyle.DANCING_GLOW,
                                effectStyle = TextEffectStyle.CURVED_ARC,
                                animationType = TextAnimationType.WORD_POP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 32f,
                                curveAmount = 0.42f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Lo-Fi Melody 110 BPM",
                            startMs = 0L,
                            durationMs = 10000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 44)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "photo_3d_zoom",
            title = "3D Photo Zoom",
            subtitle = "3 photo slots • 3D Flip transitions + dynamic zoom bursts",
            category = "3D Zoom",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 9_000L,
            bpm = 120,
            drawableKey = "tpl_velocity",
            tags = listOf("3 Slots", "3D Flip", "Zoom Burst"),
            usesLabel = "529K uses",
            accentHex = 0xFF9B5CFF,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "3D Photo Zoom Reel",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_velocity",
                    beatMarkersMs = listOf(1500L, 3000L, 4500L, 6000L, 7500L),
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.TOKYO_NEON,
                        contrast = 0.16f,
                        saturation = 0.15f,
                        sharpen = 0.30f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.ZOOM_PULSE, intensity = 0.45f, speed = 1.2f, motionBlur = 0.30f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Photo Slot 1",
                            startMs = 0L,
                            durationMs = 3000L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF9B5CFF,
                            animCombo = VideoAnimCombo.ZOOM_TWIST,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.0f, rotationDeg = -2f),
                                Keyframe(timeOffsetMs = 2900L, scale = 1.34f, rotationDeg = 2f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Photo Slot 2",
                            startMs = 3000L,
                            durationMs = 3000L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF00F0FF,
                            transitionIn = ClipTransition(type = TransitionType.FLIP_3D_H, durationMs = 600L),
                            animCombo = VideoAnimCombo.PENDULUM_1,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.30f),
                                Keyframe(timeOffsetMs = 2900L, scale = 1.04f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Photo Slot 3",
                            startMs = 6000L,
                            durationMs = 3000L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFA3FF12,
                            transitionIn = ClipTransition(type = TransitionType.FLIP_3D_V, durationMs = 600L),
                            animCombo = VideoAnimCombo.BEAT_FLASH_ZOOM,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.05f),
                                Keyframe(timeOffsetMs = 2900L, scale = 1.32f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "3D PHOTO ZOOM",
                            startMs = 300L,
                            durationMs = 3800L,
                            accentColorHex = 0xFF00F0FF,
                            textStyle = TextClipStyle(
                                text = "3D PHOTO MOTION",
                                fontStyle = TextFontStyle.ORBITRON_CYBER,
                                effectStyle = TextEffectStyle.GRADIENT_POP,
                                animationType = TextAnimationType.POP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 32f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Hyper Zoom 120 BPM",
                            startMs = 0L,
                            durationMs = 9000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 37)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "vlog_intro",
            title = "Vlog Intro",
            subtitle = "2 slots • Handheld glow + bouncy script intro & starlight FX",
            category = "Vlog",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 8_500L,
            bpm = 116,
            drawableKey = "tpl_cinematic_travel",
            tags = listOf("2 Slots", "Starlight", "Bounce Title"),
            usesLabel = "394K uses",
            accentHex = 0xFFA3FF12,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Daily Vlog Intro",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_cinematic_travel",
                    colorGrading = ColorGrading(lutPreset = LutPreset.KOREA_GLOW, brightness = 0.05f),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.SPARKLE, intensity = 0.62f, speed = 1.0f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.35f, voiceBoost = 0.55f, fadeInMs = 500L, fadeOutMs = 800L),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 1 • Vlog Hook",
                            startMs = 0L,
                            durationMs = 4200L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFA3FF12,
                            animIn = VideoAnimIn.ZOOM_IN,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.02f),
                                Keyframe(timeOffsetMs = 4100L, scale = 1.16f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Slot 2 • B-Roll Cut",
                            startMs = 4200L,
                            durationMs = 4300L,
                            drawableKey = "tpl_retro_vhs",
                            accentColorHex = 0xFF00F0FF,
                            transitionIn = ClipTransition(type = TransitionType.WHIP_PAN_RIGHT, durationMs = 480L),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.14f),
                                Keyframe(timeOffsetMs = 4200L, scale = 1.02f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "WELCOME TO MY VLOG",
                            startMs = 400L,
                            durationMs = 4600L,
                            accentColorHex = 0xFFA3FF12,
                            textStyle = TextClipStyle(
                                text = "Welcome To My Vlog ✨",
                                fontStyle = TextFontStyle.CAVEAT_VLOG,
                                effectStyle = TextEffectStyle.PILL_HIGHLIGHT,
                                animationType = TextAnimationType.BOUNCE,
                                textColorHex = 0xFF08080A,
                                accentColorHex = 0xFFA3FF12,
                                fontSizeSp = 34f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Sunny Vlog Beat 116 BPM",
                            startMs = 0L,
                            durationMs = 8500L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 67)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "retro_vhs",
            title = "Retro VHS 1994",
            subtitle = "1 slot • Analog scanlines + warm sunset halation",
            category = "Retro",
            aspectRatio = AspectRatioPreset.SQUARE_1_1,
            durationMs = 10_000L,
            bpm = 108,
            drawableKey = "tpl_retro_vhs",
            tags = listOf("VHS Static", "Vintage Film", "Typewriter"),
            usesLabel = "289K uses",
            accentHex = 0xFFFF2E93,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Boardwalk VHS '94",
                    aspectRatio = AspectRatioPreset.SQUARE_1_1,
                    resolution = ExportResolution.RES_1080P,
                    fps = ExportFps.FPS_30,
                    thumbnailDrawableKey = "tpl_retro_vhs",
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.VINTAGE_FILM,
                        brightness = 0.04f,
                        vignette = 0.42f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.VHS_STATIC, intensity = 0.72f, speed = 0.9f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.15f, voiceBoost = 0.40f, bassBoost = 0.45f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Sunset Strip Tape",
                            startMs = 0L,
                            durationMs = 10000L,
                            drawableKey = "tpl_retro_vhs",
                            accentColorHex = 0xFFFF2E93,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.04f),
                                Keyframe(timeOffsetMs = 5000L, scale = 1.15f, offsetX = 0.03f),
                                Keyframe(timeOffsetMs = 9900L, scale = 1.06f, offsetX = -0.02f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "SUMMER 1994",
                            startMs = 600L,
                            durationMs = 7400L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "REC 1994 • VENICE BEACH",
                                fontStyle = TextFontStyle.JETBRAINS_MONO,
                                effectStyle = TextEffectStyle.PILL_HIGHLIGHT,
                                animationType = TextAnimationType.TYPEWRITER,
                                textColorHex = 0xFFFFE5B4,
                                accentColorHex = 0xFFFF2E93,
                                fontSizeSp = 22f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Lo-Fi Cassette Groove",
                            startMs = 0L,
                            durationMs = 10000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 51)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "noir_trailer",
            title = "Noir Film Cut",
            subtitle = "High-contrast B&W + Jump Cut velocity",
            category = "Cinematic",
            aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
            durationMs = 9_000L,
            bpm = 120,
            drawableKey = "tpl_beat_sync",
            tags = listOf("B&W Noir", "Jump Cut", "16:9"),
            usesLabel = "156K uses",
            accentHex = 0xFF38BDF8,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Noir Studio Teaser",
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_30,
                    thumbnailDrawableKey = "tpl_beat_sync",
                    colorGrading = ColorGrading(lutPreset = LutPreset.BW_NOIR, contrast = 0.22f, vignette = 0.45f),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.VIGNETTE_FLASH, intensity = 0.50f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Stage Silhouette",
                            startMs = 0L,
                            durationMs = 9000L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF38BDF8,
                            speedCurvePreset = SpeedCurvePreset.JUMP_CUT,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.JUMP_CUT)
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "DIRECTOR'S CUT",
                            startMs = 900L,
                            durationMs = 6400L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "DIRECTOR'S CUT",
                                fontStyle = TextFontStyle.SPACE_GROTESK,
                                effectStyle = TextEffectStyle.DROP_SHADOW,
                                animationType = TextAnimationType.FADE_UP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF38BDF8,
                                fontSizeSp = 32f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Cinematic Sub Pulse",
                            startMs = 0L,
                            durationMs = 9000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 83)
                        )
                    )
                )
            }
        )
    )

    val defaultStoryboards: List<StoryboardScene> = listOf(
        StoryboardScene(
            id = "sb_1",
            orderIndex = 0,
            title = "Hook: Neon Light Reveal",
            scriptNotes = "Open with tight silhouette under cyan rim light; snap-zoom on beat 1 with bold hook caption.",
            durationSec = 3,
            cameraAngle = "Low Angle",
            lightingTag = "Cyan Rim",
            isFilmed = true
        ),
        StoryboardScene(
            id = "sb_2",
            orderIndex = 1,
            title = "Motion Whip Transition",
            scriptNotes = "Fast right-to-left whip pan into street scene; apply Flash In speed curve for seamless cut.",
            durationSec = 4,
            cameraAngle = "Tracking",
            lightingTag = "Street Neon",
            isFilmed = true
        ),
        StoryboardScene(
            id = "sb_3",
            orderIndex = 2,
            title = "Hero Slow-Mo Detail",
            scriptNotes = "Capture 60fps close-up of motion droplets; dip curve to 0.3x at midpoint with RGB Split accent.",
            durationSec = 5,
            cameraAngle = "Close-Up",
            lightingTag = "Backlit Mist",
            isFilmed = false
        ),
        StoryboardScene(
            id = "sb_4",
            orderIndex = 3,
            title = "Outro CTA & Title Lockup",
            scriptNotes = "Pull back wide shot overlooking horizon; fade up Karaoke title card and soft bass tail.",
            durationSec = 4,
            cameraAngle = "Drone Aerial",
            lightingTag = "Golden Hour",
            isFilmed = false
        )
    )

    fun insightsForPeriod(period: String): CreatorInsightsData = when (period) {
        "30D" -> CreatorInsightsData(
            periodLabel = "Last 30 Days",
            totalViews = "1.42M",
            viewsDelta = "+38.4%",
            avgWatchTime = "14.8s",
            completionRate = "64%",
            followersDelta = "+12.6K",
            retentionCurve = listOf(1.00f, 0.91f, 0.85f, 0.80f, 0.76f, 0.73f, 0.69f, 0.66f, 0.65f, 0.62f, 0.59f, 0.56f),
            bestPostingSlots = listOf(
                PostingTimeSlot("Tue", "6:30 PM", 0.92f),
                PostingTimeSlot("Thu", "7:00 PM", 0.98f),
                PostingTimeSlot("Fri", "5:45 PM", 0.87f),
                PostingTimeSlot("Sun", "11:30 AM", 0.94f)
            ),
            trendingAudios = sampleTrendingAudios,
            topReels = listOf(
                ReelPerformanceMetric("Midnight Velocity Edit", "482K", 74, "19.2K", "tpl_velocity"),
                ReelPerformanceMetric("Alpine Fjord Teal LUT", "365K", 68, "14.8K", "tpl_cinematic_travel"),
                ReelPerformanceMetric("Beat Sync Studio Cut", "294K", 63, "11.1K", "tpl_beat_sync")
            )
        )
        "90D" -> CreatorInsightsData(
            periodLabel = "Last 90 Days",
            totalViews = "4.89M",
            viewsDelta = "+61.2%",
            avgWatchTime = "15.4s",
            completionRate = "67%",
            followersDelta = "+41.3K",
            retentionCurve = listOf(1.00f, 0.93f, 0.88f, 0.84f, 0.79f, 0.75f, 0.72f, 0.70f, 0.68f, 0.65f, 0.63f, 0.60f),
            bestPostingSlots = listOf(
                PostingTimeSlot("Mon", "8:00 PM", 0.85f),
                PostingTimeSlot("Wed", "6:15 PM", 0.91f),
                PostingTimeSlot("Thu", "7:00 PM", 0.99f),
                PostingTimeSlot("Sat", "12:00 PM", 0.95f)
            ),
            trendingAudios = sampleTrendingAudios,
            topReels = listOf(
                ReelPerformanceMetric("Midnight Velocity Edit", "1.21M", 76, "44.5K", "tpl_velocity"),
                ReelPerformanceMetric("Venice VHS 1994", "940K", 71, "32.1K", "tpl_retro_vhs"),
                ReelPerformanceMetric("Alpine Fjord Teal LUT", "810K", 69, "28.9K", "tpl_cinematic_travel")
            )
        )
        else -> CreatorInsightsData(
            periodLabel = "Last 7 Days",
            totalViews = "384.2K",
            viewsDelta = "+24.8%",
            avgWatchTime = "13.9s",
            completionRate = "61%",
            followersDelta = "+3.4K",
            retentionCurve = listOf(1.00f, 0.89f, 0.82f, 0.77f, 0.72f, 0.68f, 0.65f, 0.62f, 0.60f, 0.57f, 0.54f, 0.51f),
            bestPostingSlots = listOf(
                PostingTimeSlot("Tue", "6:30 PM", 0.89f),
                PostingTimeSlot("Thu", "7:15 PM", 0.97f),
                PostingTimeSlot("Fri", "6:00 PM", 0.86f),
                PostingTimeSlot("Sun", "11:00 AM", 0.93f)
            ),
            trendingAudios = sampleTrendingAudios,
            topReels = listOf(
                ReelPerformanceMetric("Beat Sync Studio Cut", "148K", 69, "6.4K", "tpl_beat_sync"),
                ReelPerformanceMetric("Midnight Velocity Edit", "119K", 72, "5.1K", "tpl_velocity"),
                ReelPerformanceMetric("Venice VHS 1994", "84K", 64, "3.8K", "tpl_retro_vhs")
            )
        )
    }

    private val sampleTrendingAudios = listOf(
        TrendingAudioSuggestion("aud_1", "Neon Overdrive (Sped Up)", "Kairo Synth", 132, 10_000L, "+214% this wk", "Velocity", 0xFF9B5CFF),
        TrendingAudioSuggestion("aud_2", "Golden Fjord Echoes", "Lumina Waves", 98, 12_000L, "+168% this wk", "Cinematic", 0xFF00F0FF),
        TrendingAudioSuggestion("aud_3", "Cassette Sunset '94", "Tapeclub", 110, 10_000L, "+142% this wk", "Retro VHS", 0xFFFF2E93),
        TrendingAudioSuggestion("aud_4", "Sub-Zero Bass Drop", "Vexel Beats", 128, 9_000L, "+189% this wk", "Beat Sync", 0xFFA3FF12)
    )
}
