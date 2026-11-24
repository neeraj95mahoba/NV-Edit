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
            id = "beat_sync",
            title = "Beat Sync Drop",
            subtitle = "128 BPM punchy cuts + neon glitch flash",
            category = "Trending",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 10_000L,
            bpm = 128,
            drawableKey = "tpl_beat_sync",
            tags = listOf("Beat Sync", "RGB Split", "Keyframes"),
            usesLabel = "428K uses",
            accentHex = 0xFF9B5CFF,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Beat Sync Reel",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_beat_sync",
                    beatMarkersMs = listOf(1200L, 2400L, 3600L, 5000L, 6200L, 7400L, 8600L),
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.CYBERPUNK,
                        contrast = 0.15f,
                        saturation = 0.12f,
                        vignette = 0.25f
                    ),
                    visualFx = VisualFxSettings(
                        activeFx = VisualFxType.RGB_SPLIT,
                        intensity = 0.45f,
                        speed = 1.2f,
                        motionBlur = 0.28f
                    ),
                    audioEnhance = AudioEnhanceSettings(
                        noiseReduction = 0.30f,
                        voiceBoost = 0.45f,
                        bassBoost = 0.65f
                    ),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Dancer Intro",
                            startMs = 0L,
                            durationMs = 5000L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF9B5CFF,
                            speedCurvePreset = SpeedCurvePreset.MONTAGE,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.MONTAGE),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.02f, offsetY = 0.02f),
                                Keyframe(timeOffsetMs = 2400L, scale = 1.24f, offsetX = -0.04f, rotationDeg = -2f),
                                Keyframe(timeOffsetMs = 4800L, scale = 1.06f, offsetX = 0.03f, rotationDeg = 1.5f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Drop Velocity",
                            startMs = 5000L,
                            durationMs = 5000L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF00D4FF,
                            transitionIn = ClipTransition(type = TransitionType.GLITCH_ZOOM, durationMs = 700L),
                            speedCurvePreset = SpeedCurvePreset.FLASH_IN,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.FLASH_IN),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.32f, rotationDeg = 3f),
                                Keyframe(timeOffsetMs = 2500L, scale = 1.05f, rotationDeg = 0f),
                                Keyframe(timeOffsetMs = 4900L, scale = 1.18f, offsetY = -0.04f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "FEEL THE DROP",
                            startMs = 400L,
                            durationMs = 4200L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "FEEL THE DROP",
                                fontStyle = TextFontStyle.BEBAS_NEUE,
                                effectStyle = TextEffectStyle.NEON_GLOW,
                                animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 38f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "NIGHT PULSE",
                            startMs = 5300L,
                            durationMs = 4000L,
                            accentColorHex = 0xFFFF2E93,
                            textStyle = TextClipStyle(
                                text = "NIGHT PULSE 60FPS",
                                fontStyle = TextFontStyle.SYNE,
                                effectStyle = TextEffectStyle.GRADIENT_POP,
                                animationType = TextAnimationType.GLITCH,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFFFF2E93,
                                fontSizeSp = 30f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Cyber Drift 128 BPM",
                            startMs = 0L,
                            durationMs = 10000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 11)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "cinematic_travel",
            title = "Cinematic Travel",
            subtitle = "Teal & Orange LUT + smooth crane zooms",
            category = "Cinematic",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 12_000L,
            bpm = 96,
            drawableKey = "tpl_cinematic_travel",
            tags = listOf("Teal & Orange", "Hero Curve", "Editorial"),
            usesLabel = "312K uses",
            accentHex = 0xFFFFB020,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Nordic Horizon Film",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_4K,
                    fps = ExportFps.FPS_30,
                    thumbnailDrawableKey = "tpl_cinematic_travel",
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.TEAL_ORANGE,
                        exposure = 0.08f,
                        contrast = 0.14f,
                        vignette = 0.30f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.BLUR_FOCUS, intensity = 0.25f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.50f, voiceBoost = 0.55f, bassBoost = 0.25f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Alpine Pass Aerial",
                            startMs = 0L,
                            durationMs = 6500L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFFFB020,
                            speedCurvePreset = SpeedCurvePreset.HERO,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.HERO),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.0f, offsetY = 0.04f),
                                Keyframe(timeOffsetMs = 6400L, scale = 1.22f, offsetY = -0.04f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Golden Coastline",
                            startMs = 6500L,
                            durationMs = 5500L,
                            drawableKey = "tpl_retro_vhs",
                            accentColorHex = 0xFF38BDF8,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.16f, offsetX = -0.04f),
                                Keyframe(timeOffsetMs = 5400L, scale = 1.02f, offsetX = 0.03f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "GOLDEN HOUR",
                            startMs = 800L,
                            durationMs = 5000L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "CHASE THE GOLDEN HOUR",
                                fontStyle = TextFontStyle.PLAYFAIR,
                                effectStyle = TextEffectStyle.DROP_SHADOW,
                                animationType = TextAnimationType.FADE_UP,
                                textColorHex = 0xFFFFF5E1,
                                accentColorHex = 0xFFFFB020,
                                fontSizeSp = 28f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Fjord Ambient Score",
                            startMs = 0L,
                            durationMs = 12000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 23)
                        )
                    )
                )
            }
        ),
        VideoTemplate(
            id = "slow_mo_velocity",
            title = "Slow-Mo Velocity",
            subtitle = "Bullet time speed ramps + Tokyo Neon grade",
            category = "Velocity",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 9_000L,
            bpm = 140,
            drawableKey = "tpl_velocity",
            tags = listOf("Bullet Curve", "Shake FX", "60fps"),
            usesLabel = "615K uses",
            accentHex = 0xFF00F0FF,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Midnight Velocity",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_1080P,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_velocity",
                    colorGrading = ColorGrading(
                        lutPreset = LutPreset.TOKYO_NEON,
                        contrast = 0.22f,
                        saturation = 0.18f,
                        sharpen = 0.35f
                    ),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.SHAKE, intensity = 0.35f, speed = 1.4f),
                    audioEnhance = AudioEnhanceSettings(noiseReduction = 0.2f, voiceBoost = 0.3f, bassBoost = 0.8f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Tokyo Apex Drift",
                            startMs = 0L,
                            durationMs = 9000L,
                            drawableKey = "tpl_velocity",
                            accentColorHex = 0xFF00F0FF,
                            speedCurvePreset = SpeedCurvePreset.BULLET,
                            speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.BULLET),
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.05f, offsetX = 0.04f),
                                Keyframe(timeOffsetMs = 3000L, scale = 1.30f, offsetX = -0.05f, rotationDeg = -3f),
                                Keyframe(timeOffsetMs = 6200L, scale = 1.12f, offsetX = 0.02f, rotationDeg = 2f),
                                Keyframe(timeOffsetMs = 8900L, scale = 1.24f, offsetX = 0f, rotationDeg = 0f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.OVERLAY_PIP,
                            label = "Neon Cutaway PIP",
                            startMs = 2200L,
                            durationMs = 4200L,
                            drawableKey = "tpl_beat_sync",
                            accentColorHex = 0xFF00D4FF,
                            pipScale = 0.34f,
                            pipOffsetX = 0.26f,
                            pipOffsetY = -0.26f
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "VELOCITY 0.3X",
                            startMs = 1200L,
                            durationMs = 5200L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "APEX VELOCITY",
                                fontStyle = TextFontStyle.BEBAS_NEUE,
                                effectStyle = TextEffectStyle.OUTLINE_BOLD,
                                animationType = TextAnimationType.POP,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFF00F0FF,
                                fontSizeSp = 40f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Phonk Turbo 140 BPM",
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
            id = "retro_vhs",
            title = "Retro VHS 1994",
            subtitle = "Analog scanlines + warm sunset halation",
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
            id = "sparkle_vlog",
            title = "Sparkle Aesthetic",
            subtitle = "Dreamy pastel LUT + bounce captions",
            category = "Aesthetic",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 8_000L,
            bpm = 116,
            drawableKey = "tpl_cinematic_travel",
            tags = listOf("Sparkle FX", "Pastel", "Bounce Text"),
            usesLabel = "194K uses",
            accentHex = 0xFFA3FF12,
            createDocument = {
                VideoProjectDocument(
                    id = UUID.randomUUID().toString(),
                    title = "Golden Dream Vlog",
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    resolution = ExportResolution.RES_1080P,
                    fps = ExportFps.FPS_60,
                    thumbnailDrawableKey = "tpl_cinematic_travel",
                    colorGrading = ColorGrading(lutPreset = LutPreset.PASTEL_DREAM, brightness = 0.06f),
                    visualFx = VisualFxSettings(activeFx = VisualFxType.SPARKLE, intensity = 0.65f, speed = 1.0f),
                    clips = listOf(
                        TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = "Mountain Glow",
                            startMs = 0L,
                            durationMs = 8000L,
                            drawableKey = "tpl_cinematic_travel",
                            accentColorHex = 0xFFA3FF12,
                            keyframes = listOf(
                                Keyframe(timeOffsetMs = 0L, scale = 1.02f),
                                Keyframe(timeOffsetMs = 8000L, scale = 1.18f)
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.TEXT_STICKER,
                            label = "WEEKEND ESCAPE",
                            startMs = 500L,
                            durationMs = 6000L,
                            accentColorHex = 0xFFFF9F1C,
                            textStyle = TextClipStyle(
                                text = "Weekend Escape",
                                fontStyle = TextFontStyle.PACIFICO,
                                effectStyle = TextEffectStyle.NEON_GLOW,
                                animationType = TextAnimationType.BOUNCE,
                                textColorHex = 0xFFFFFFFF,
                                accentColorHex = 0xFFA3FF12,
                                fontSizeSp = 34f
                            )
                        ),
                        TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = "Dream Pop Chords",
                            startMs = 0L,
                            durationMs = 8000L,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = generateWaveform(seed = 67)
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
