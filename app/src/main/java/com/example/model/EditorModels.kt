package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class AspectRatioPreset(val label: String, val ratio: Float) {
    PORTRAIT_9_16("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    SQUARE_1_1("1:1", 1f),
    SOCIAL_4_5("4:5", 4f / 5f)
}

enum class SafeZonePreset(val label: String, val shortLabel: String) {
    OFF("Off", "Off"),
    REELS_SHORTS("9:16 Reels/Shorts", "Reels"),
    YOUTUBE_16_9("16:9 YouTube", "YT"),
    INSTAGRAM_FEED("4:5 / 1:1 IG/FB", "IG/FB");

    fun next(): SafeZonePreset = entries[(ordinal + 1) % entries.size]
}

enum class CanvasBgMode(val label: String) {
    SOLID("Solid"),
    BLUR_LOW("Blur Low"),
    BLUR_MED("Blur Med"),
    BLUR_HIGH("Blur High"),
    GRADIENT("Gradient"),
    CUSTOM_IMAGE("Image")
}

@JsonClass(generateAdapter = true)
data class CanvasBackgroundSettings(
    val mode: CanvasBgMode = CanvasBgMode.SOLID,
    val solidColorHex: Long = 0xFF0A0A0DL,
    val gradientIndex: Int = 0,
    val customImageUri: String? = null
)

enum class ExportResolution(val label: String, val detailLabel: String, val shortEdge: Int) {
    RES_720P("720p", "HD 720p", 720),
    RES_1080P("1080p", "Full HD 1080p", 1080),
    RES_2K("2K", "QHD 1440p", 1440),
    RES_4K("4K", "Ultra HD 2160p", 2160)
}

enum class ExportFps(val label: String, val fps: Int) {
    FPS_30("30fps", 30),
    FPS_60("60fps", 60)
}

enum class ExportBitrate(val label: String, val multiplier: Float) {
    STANDARD("High", 1.0f),
    ULTRA_PRO("Ultra High", 1.6f)
}

enum class TrackType(val label: String) {
    MAIN_VIDEO("Video"),
    OVERLAY_PIP("Overlay"),
    TEXT_STICKER("Text"),
    AUDIO_MUSIC("Audio")
}

// 20 Pro Video Filters / LUT Presets (all unlocked + live 0%..100% intensity slider)
enum class LutPreset(
    val displayName: String,
    val badgeColorHex: Long,
    val rScale: Float = 1f,
    val gScale: Float = 1f,
    val bScale: Float = 1f,
    val rBias: Float = 0f,
    val gBias: Float = 0f,
    val bBias: Float = 0f,
    val saturation: Float = 1f,
    val contrast: Float = 1f
) {
    ORIGINAL("Original", 0xFF6B6B7A),
    TEAL_ORANGE("Teal & Orange", 0xFFFF8C42, rScale = 1.14f, gScale = 1.02f, bScale = 0.88f, rBias = 8f, gBias = 4f, bBias = 14f, saturation = 1.22f, contrast = 1.12f),
    MOODY_DARK("Moody Dark", 0xFF3E4C5E, rScale = 0.90f, gScale = 0.94f, bScale = 1.04f, rBias = -14f, gBias = -10f, bBias = -4f, saturation = 0.78f, contrast = 1.24f),
    VINTAGE_FILM("Retro 1998", 0xFFD4A373, rScale = 1.10f, gScale = 1.03f, bScale = 0.84f, rBias = 14f, gBias = 8f, bBias = -4f, saturation = 0.85f, contrast = 0.94f),
    KODAK_PORTRA("Kodak Portra", 0xFFE8B88A, rScale = 1.12f, gScale = 1.04f, bScale = 0.92f, rBias = 10f, gBias = 6f, bBias = 2f, saturation = 0.96f, contrast = 0.98f),
    WARM_GOLDEN("Golden Hour", 0xFFFFB703, rScale = 1.18f, gScale = 1.07f, bScale = 0.82f, rBias = 10f, gBias = 6f, bBias = -8f, saturation = 1.18f, contrast = 1.06f),
    CYBERPUNK("Cyberpunk", 0xFF9B5CFF, rScale = 1.18f, gScale = 0.88f, bScale = 1.26f, rBias = 10f, gBias = -8f, bBias = 18f, saturation = 1.38f, contrast = 1.18f),
    TOKYO_NEON("Tokyo Night", 0xFF00F0FF, rScale = 0.92f, gScale = 1.08f, bScale = 1.24f, rBias = 4f, gBias = 6f, bBias = 16f, saturation = 1.32f, contrast = 1.16f),
    BW_NOIR("Noir Classic", 0xFFB5B5C0, rScale = 1f, gScale = 1f, bScale = 1f, rBias = -8f, gBias = -8f, bBias = -8f, saturation = 0f, contrast = 1.36f),
    HIGH_CONTRAST_BW("Hi-Con B&W", 0xFFE5E5EC, rScale = 1.04f, gScale = 1.04f, bScale = 1.04f, rBias = -14f, gBias = -14f, bBias = -14f, saturation = 0f, contrast = 1.62f),
    WARM_SEPIA("Warm Sepia", 0xFFC89B6E, rScale = 1.24f, gScale = 1.02f, bScale = 0.72f, rBias = 14f, gBias = 4f, bBias = -12f, saturation = 0.45f, contrast = 1.04f),
    VIVID_POP("Vivid Pop", 0xFFFF3366, rScale = 1.10f, gScale = 1.08f, bScale = 1.12f, rBias = 4f, gBias = 4f, bBias = 6f, saturation = 1.52f, contrast = 1.18f),
    KODAK_GOLD("Kodak Gold", 0xFFE9C46A, rScale = 1.15f, gScale = 1.06f, bScale = 0.88f, rBias = 8f, gBias = 4f, bBias = -6f, saturation = 1.14f, contrast = 1.08f),
    EMERALD_CINEMA("Emerald", 0xFF10B981, rScale = 0.86f, gScale = 1.14f, bScale = 1.02f, rBias = -6f, gBias = 8f, bBias = 2f, saturation = 1.06f, contrast = 1.14f),
    PASTEL_DREAM("Dreamy Pastel", 0xFFFF99C8, rScale = 1.08f, gScale = 1.04f, bScale = 1.10f, rBias = 16f, gBias = 12f, bBias = 16f, saturation = 0.88f, contrast = 0.90f),
    BLEACH_BYPASS("Bleach Bypass", 0xFF7D8597, rScale = 0.98f, gScale = 1.00f, bScale = 1.02f, rBias = -6f, gBias = -6f, bBias = -4f, saturation = 0.48f, contrast = 1.32f),
    ARCTIC_BLUE("Arctic Blue", 0xFF38BDF8, rScale = 0.84f, gScale = 1.02f, bScale = 1.22f, rBias = -8f, gBias = 2f, bBias = 16f, saturation = 1.04f, contrast = 1.10f),
    SUNSET_GLOW("Sunset Glow", 0xFFFF4D67, rScale = 1.22f, gScale = 0.95f, bScale = 0.92f, rBias = 16f, gBias = -2f, bBias = 4f, saturation = 1.24f, contrast = 1.08f),
    MATTE_FADE("Matte Fade", 0xFF8D99AE, rScale = 0.96f, gScale = 0.97f, bScale = 1.02f, rBias = 18f, gBias = 18f, bBias = 20f, saturation = 0.82f, contrast = 0.86f),
    FUJI_VELVIA("Fuji Velvia", 0xFF2EC4B6, rScale = 1.06f, gScale = 1.10f, bScale = 1.08f, rBias = -2f, gBias = 2f, bBias = 2f, saturation = 1.34f, contrast = 1.16f)
}

@JsonClass(generateAdapter = true)
data class ColorWheelTint(
    val hueDeg: Float = 0f,
    val strength: Float = 0f
)

@JsonClass(generateAdapter = true)
data class BeautyRetouchSettings(
    val skinSmooth: Float = 0f,
    val faceGlow: Float = 0f
)

@JsonClass(generateAdapter = true)
data class ColorGrading(
    val lutPreset: LutPreset = LutPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f, // 0f..1f live LUT strength
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val vignette: Float = 0f,
    val sharpen: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val fade: Float = 0f,
    val shadowWheel: ColorWheelTint = ColorWheelTint(),
    val midtoneWheel: ColorWheelTint = ColorWheelTint(),
    val highlightWheel: ColorWheelTint = ColorWheelTint(),
    val beauty: BeautyRetouchSettings = BeautyRetouchSettings()
)

enum class KeyframeEasing(val label: String) {
    LINEAR("Linear"),
    EASE_IN_OUT("Ease In/Out"),
    EASE_OUT_BACK("Overshoot")
}

@JsonClass(generateAdapter = true)
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeOffsetMs: Long,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val easing: KeyframeEasing = KeyframeEasing.EASE_IN_OUT
)

// Complete Video Animation Engine (In / Out / Combo Beat-Sync)
enum class VideoAnimIn(val label: String) {
    NONE("None"),
    FADE_IN("Fade In"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    SLIDE_LEFT("Slide L"),
    SLIDE_RIGHT("Slide R"),
    SLIDE_UP("Slide Up"),
    SLIDE_DOWN("Slide Dn"),
    SPIN_IN("Spin In"),
    WHIP_PAN("Whip Pan"),
    BLUR_IN("Blur In"),
    BOUNCE_DROP("Bounce Drop"),
    SHAKE_IN("Shake In"),
    POP_SCALE("Pop Scale")
}

enum class VideoAnimOut(val label: String) {
    NONE("None"),
    FADE_OUT("Fade Out"),
    ZOOM_OUT("Zoom Out"),
    ZOOM_WARP("Zoom Warp"),
    SLIDE_LEFT("Slide L"),
    SLIDE_RIGHT("Slide R"),
    SLIDE_UP("Slide Up"),
    SLIDE_DOWN("Slide Dn"),
    SPIN_OUT("Spin Out"),
    BLUR_OUT("Blur Out"),
    SHRINK_AWAY("Shrink Away")
}

enum class VideoAnimCombo(val label: String) {
    NONE("None"),
    PENDULUM_1("Pendulum 1"),
    PENDULUM_2("Pendulum 2"),
    BOUNCE_1("Bounce 1"),
    BOUNCE_2("Bounce 2"),
    WAVE_SLIDE("Wave Slide"),
    DISTORT_LEFT("Distort L"),
    DISTORT_RIGHT("Distort R"),
    ZOOM_TWIST("Zoom Twist"),
    FLIP_3D_H("3D Flip H"),
    FLIP_3D_V("3D Flip V"),
    ELASTIC_WOBBLE("Elastic"),
    BEAT_FLASH_ZOOM("Beat Flash")
}

enum class SpeedCurvePreset(val label: String) {
    NONE("Uniform"),
    MONTAGE("Montage"),
    HERO("Hero"),
    BULLET("Bullet"),
    JUMP_CUT("Jump Cut"),
    FLASH_IN("Flash In"),
    FLASH_OUT("Flash Out"),
    CUSTOM("Custom")
}

@JsonClass(generateAdapter = true)
data class SpeedCurvePoint(
    val normalizedTime: Float,
    val speedMultiplier: Float
)

enum class TextFontStyle(val label: String) {
    SPACE_GROTESK("Studio Sans"),
    BEBAS_NEUE("Bebas Bold"),
    PLAYFAIR("Editorial Serif"),
    PACIFICO("Script Flow"),
    SYNE("Avant Garde"),
    JETBRAINS_MONO("Mono Code"),
    PLUS_JAKARTA("Clean Modern")
}

enum class TextEffectStyle(val label: String) {
    CLEAN("Clean"),
    NEON_GLOW("Neon Glow"),
    DROP_SHADOW("3D Shadow"),
    OUTLINE_BOLD("Bold Stroke"),
    PILL_HIGHLIGHT("Caption Pill"),
    GRADIENT_POP("Prism Pop")
}

// Text IN Animations
enum class TextAnimationType(val label: String) {
    NONE("Static"),
    TYPEWRITER("Typewriter"),
    FADE_UP("Fade Up"),
    BOUNCE("Pop Bounce"),
    GLITCH("Glitch Reveal"),
    SLIDE_MASK("Slide Mask"),
    POP("Zoom Burst"),
    NEON_FLICKER("Neon Flicker"),
    WAVE_DROP("Wave Drop"),
    KARAOKE_HIGHLIGHT("Karaoke")
}

// Text OUT Animations
enum class TextAnimOut(val label: String) {
    NONE("None"),
    FADE_OUT("Fade Out"),
    TYPEWRITER_ERASE("Erase"),
    SLIDE_DOWN("Slide Down"),
    SHRINK_POP("Shrink Pop"),
    GLITCH_OUT("Glitch Out")
}

// Text LOOP Animations
enum class TextAnimLoop(val label: String) {
    NONE("None"),
    FLOATING_HOVER("Float"),
    HEARTBEAT_PULSE("Heartbeat"),
    NEON_GLOW_FLASH("Neon Flash"),
    SHAKE_VIBRATE("Shake"),
    WAVE_TEXT("Wave"),
    GENTLE_SWING("Swing"),
    SPIN_360("Spin 360")
}

enum class AutoCaptionStyle(val label: String, val badgeHex: Long) {
    HORMOZI("Hormozi Viral", 0xFFFFD84D),
    NEON_POP("Neon Pop", 0xFF00F0FF),
    KARAOKE("Karaoke Flow", 0xFFA3FF12),
    MINIMALIST_BOLD("Minimal Bold", 0xFFF6F6FA)
}

@JsonClass(generateAdapter = true)
data class TextClipStyle(
    val text: String = "MAKE IT CINEMATIC",
    val fontStyle: TextFontStyle = TextFontStyle.BEBAS_NEUE,
    val effectStyle: TextEffectStyle = TextEffectStyle.NEON_GLOW,
    val animationType: TextAnimationType = TextAnimationType.FADE_UP,
    val outAnimation: TextAnimOut = TextAnimOut.FADE_OUT,
    val loopAnimation: TextAnimLoop = TextAnimLoop.NONE,
    val textColorHex: Long = 0xFFFFFFFF,
    val accentColorHex: Long = 0xFF00F0FF,
    val fontSizeSp: Float = 32f,
    val isSticker: Boolean = false
)

// 18 Real-Time Visual FX (all unlocked + live 0%..100% intensity slider)
enum class VisualFxType(val label: String, val accentHex: Long) {
    NONE("None", 0xFF6B6B7A),
    RGB_SPLIT("RGB Split", 0xFF00F0FF),
    RETRO_GLITCH("Retro Glitch", 0xFFFF2E93),
    VHS_STATIC("VHS Cam", 0xFFFFB020),
    SHAKE("Cam Shake", 0xFFFF5E5B),
    VIGNETTE_FLASH("Flash Strobe", 0xFFFFFFFF),
    ZOOM_PULSE("Zoom Pulse", 0xFF9B5CFF),
    DREAMY_BLOOM("Dreamy Glow", 0xFFFF99C8),
    FILM_GRAIN("Film Grain", 0xFFD4A373),
    MOTION_BLUR_FX("Motion Blur", 0xFF38BDF8),
    VIGNETTE_DARK("Vignette Dark", 0xFF64748B),
    FISHEYE_LENS("Fisheye Lens", 0xFF10E896),
    MIRROR_SPLIT_2X("Mirror 2x", 0xFF00D4FF),
    MIRROR_SPLIT_4X("Mirror 4x", 0xFF7B61FF),
    LIGHT_LEAK_WARM("Light Leak", 0xFFFF8C42),
    PIXELATE_8BIT("Pixel 8-Bit", 0xFFA3FF12),
    BLUR_FOCUS("Blur Focus", 0xFF9B5CFF),
    SPARKLE("Sparkle", 0xFFFFD84D)
}

@JsonClass(generateAdapter = true)
data class VisualFxSettings(
    val activeFx: VisualFxType = VisualFxType.NONE,
    val intensity: Float = 0.65f,
    val speed: Float = 1.0f,
    val motionBlur: Float = 0f
)

enum class MusicVisualizerStyle(val label: String) {
    NONE("Off"),
    NEON_BARS("Neon Bars"),
    CYBER_WAVE("Cyber Wave"),
    PULSE_RING("Pulse Ring")
}

// PRO Chroma Key & Smart Auto-Cutout
enum class CutoutMode(val label: String) {
    OFF("Off"),
    CHROMA_KEY("Chroma Key"),
    AUTO_CUTOUT("Smart Cutout")
}

enum class CutoutBackgroundType(val label: String) {
    TRANSPARENT_DARK("Transparent"),
    BLUR_BG("DSLR Blur"),
    SOLID_COLOR("Solid Color"),
    NEON_GRADIENT("Neon Gradient"),
    CUSTOM_MEDIA("Custom BG")
}

@JsonClass(generateAdapter = true)
data class CutoutSettings(
    val mode: CutoutMode = CutoutMode.OFF,
    val keyColorHex: Long = 0xFF00B140,
    val intensity: Float = 0.42f,
    val shadowEdge: Float = 0.28f,
    val bgType: CutoutBackgroundType = CutoutBackgroundType.NEON_GRADIENT,
    val bgSolidColorHex: Long = 0xFF1A1038,
    val bgGradientIndex: Int = 0,
    val bgCustomUri: String? = null,
    val bgCustomDrawableKey: String? = null
)

// 32 PRO Transitions between adjacent Main Video clips
enum class TransitionType(val displayName: String, val category: String, val accentHex: Long) {
    NONE("None", "Basic", 0xFF6B6B7A),
    DISSOLVE("Dissolve", "Basic", 0xFF9B5CFF),
    CROSS_FADE("Cross Fade", "Basic", 0xFF38BDF8),
    DIP_BLACK("Dip Black", "Basic", 0xFF8E8E98),
    FLASH_WHITE("Flash White", "Light", 0xFFFFFFFF),
    WHIP_PAN_LEFT("Whip Pan L", "Camera", 0xFF00F0FF),
    WHIP_PAN_RIGHT("Whip Pan R", "Camera", 0xFF00F0FF),
    WHIP_PAN_UP("Whip Pan Up", "Camera", 0xFF00F0FF),
    WHIP_PAN_DOWN("Whip Pan Dn", "Camera", 0xFF00F0FF),
    GLITCH_ZOOM("Glitch Zoom", "Glitch", 0xFFFF2E93),
    GLITCH_SLICE("Glitch Slice", "Glitch", 0xFFFF2E93),
    RGB_WARP("RGB Warp", "Glitch", 0xFF9B5CFF),
    SPIN_BLUR_CW("Spin Blur CW", "Spin", 0xFFFFB020),
    SPIN_BLUR_CCW("Spin Blur CCW", "Spin", 0xFFFFB020),
    ZOOM_IN_BLUR("Zoom In Blur", "Camera", 0xFFA3FF12),
    ZOOM_OUT_PULL("Zoom Pull", "Camera", 0xFFA3FF12),
    FILM_BURN("Film Burn", "Light", 0xFFFF6B35),
    LIGHT_LEAK("Light Leak", "Light", 0xFFFFD84D),
    LENS_FLARE("Lens Flare", "Light", 0xFFFFE57F),
    SLIDE_PUSH_LEFT("Slide Left", "Slide", 0xFF38BDF8),
    SLIDE_PUSH_RIGHT("Slide Right", "Slide", 0xFF38BDF8),
    SLIDE_PUSH_UP("Slide Up", "Slide", 0xFF38BDF8),
    SLIDE_PUSH_DOWN("Slide Down", "Slide", 0xFF38BDF8),
    SPLIT_OPEN_H("Split Open H", "Slide", 0xFF9B5CFF),
    SPLIT_OPEN_V("Split Open V", "Slide", 0xFF9B5CFF),
    IRIS_CIRCLE("Iris Circle", "Mask", 0xFF00F0FF),
    DIAMOND_REVEAL("Diamond Wipe", "Mask", 0xFFFFD84D),
    SHAKE_IMPACT("Shake Impact", "Camera", 0xFFFF4D67),
    PIXELATE_MORPH("Pixel Morph", "Glitch", 0xFFA3FF12),
    SWIRL_TWIST("Swirl Twist", "Spin", 0xFF9B5CFF),
    CHROMATIC_FLASH("Chroma Flash", "Light", 0xFFFF2E93),
    SHUTTER_BLINK("Shutter Blink", "Basic", 0xFFF6F6FA)
}

@JsonClass(generateAdapter = true)
data class ClipTransition(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 600L
)

enum class PipBlendMode(val label: String) {
    NORMAL("Normal"),
    SCREEN("Screen"),
    MULTIPLY("Multiply"),
    OVERLAY("Overlay"),
    LIGHTEN("Lighten")
}

enum class PipMaskShape(val label: String) {
    NONE("None"),
    CIRCLE("Circle"),
    HEART("Heart"),
    RECTANGLE("Rounded"),
    SPLIT_LEFT_RIGHT("Split L/R"),
    SPLIT_TOP_BOTTOM("Split T/B"),
    LINEAR_FEATHER("Feather")
}

// 9 Audio Filters (EQ & Enhancement)
enum class AudioFilterPreset(val label: String) {
    ORIGINAL("Original"),
    STUDIO_PODCAST("Studio Podcast"),
    AI_NOISE_REDUCER("AI Noise Reduce"),
    DEEP_BASS_BOOST("Deep Bass"),
    OLD_RADIO("Old Radio"),
    TELEPHONE_CALL("Telephone"),
    UNDERWATER("Underwater"),
    TREBLE_CRISP("Treble Crisp"),
    LOFI_VINYL("Lo-Fi Vinyl")
}

// 9 Audio Effects & Voice Changers
enum class AudioEffectPreset(val label: String, val pitch: Float = 1.0f, val speed: Float = 1.0f) {
    NONE("None"),
    HALL_REVERB("Hall Reverb"),
    ROOM_ECHO("Room Echo"),
    CHIPMUNK_HIGH("Chipmunk", pitch = 1.55f, speed = 1.08f),
    DEEP_MONSTER("Deep Monster", pitch = 0.68f, speed = 0.94f),
    ROBOT_METALLIC("Robot Metallic", pitch = 0.88f, speed = 1.0f),
    SLOWED_REVERB("Slowed + Reverb", pitch = 0.84f, speed = 0.84f),
    NIGHTCORE_SPED_UP("Nightcore Sped Up", pitch = 1.24f, speed = 1.24f),
    CHORUS_DOUBLE("Chorus Double", pitch = 1.03f, speed = 1.0f)
}

@JsonClass(generateAdapter = true)
data class VolumeKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val volume: Float
)

@JsonClass(generateAdapter = true)
data class AudioEnhanceSettings(
    val noiseReduction: Float = 0.25f,
    val voiceBoost: Float = 0.35f,
    val bassBoost: Float = 0.30f,
    val masterVolume: Float = 1.0f,
    val isMuted: Boolean = false,
    val audioFilter: AudioFilterPreset = AudioFilterPreset.ORIGINAL,
    val audioEffect: AudioEffectPreset = AudioEffectPreset.NONE,
    val effectIntensity: Float = 0.72f,
    val volumeKeyframes: List<VolumeKeyframe> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TimelineClip(
    val id: String = UUID.randomUUID().toString(),
    val trackType: TrackType,
    val label: String,
    val startMs: Long,
    val durationMs: Long,
    val sourceStartMs: Long = 0L,
    val sourceDurationMs: Long = durationMs,
    val mediaUri: String? = null,
    val drawableKey: String? = null,
    val isVideoMedia: Boolean = false,
    val accentColorHex: Long = 0xFF7B61FF,
    val speed: Float = 1.0f,
    val speedCurvePreset: SpeedCurvePreset = SpeedCurvePreset.NONE,
    val speedCurvePoints: List<SpeedCurvePoint> = defaultCurvePoints(SpeedCurvePreset.NONE),
    val keyframes: List<Keyframe> = emptyList(),
    val textStyle: TextClipStyle? = null,
    val volume: Float = 1.0f,
    val waveformPeaks: List<Float> = emptyList(),
    // Essential clip transforms (Freeze Frame, Reverse, Rotate/Mirror/Crop)
    val isReversed: Boolean = false,
    val isFreezeFrame: Boolean = false,
    val freezeSourceMs: Long = 0L,
    val rotationQuarterTurns: Int = 0, // 0, 1, 2, 3 (90 deg steps)
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropZoom: Float = 1.0f,
    // Video Animation Engine (In / Out / Combo)
    val animIn: VideoAnimIn = VideoAnimIn.NONE,
    val animOut: VideoAnimOut = VideoAnimOut.NONE,
    val animCombo: VideoAnimCombo = VideoAnimCombo.NONE,
    val animDurationMs: Long = 800L, // 200ms..3000ms
    // PIP Pro Settings
    val pipScale: Float = 0.42f,
    val pipOffsetX: Float = 0.22f,
    val pipOffsetY: Float = -0.22f,
    val pipOpacity: Float = 1.0f,
    val pipBlendMode: PipBlendMode = PipBlendMode.NORMAL,
    val pipMaskShape: PipMaskShape = PipMaskShape.RECTANGLE,
    // Incoming transition from previous adjacent clip on Main Video Track
    val transitionIn: ClipTransition = ClipTransition()
) {
    val endMs: Long get() = startMs + durationMs
}

@JsonClass(generateAdapter = true)
data class VideoProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Edit",
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.PORTRAIT_9_16,
    val resolution: ExportResolution = ExportResolution.RES_4K,
    val fps: ExportFps = ExportFps.FPS_60,
    val bitrate: ExportBitrate = ExportBitrate.ULTRA_PRO,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val thumbnailDrawableKey: String? = null,
    val thumbnailUri: String? = null,
    val clips: List<TimelineClip> = emptyList(),
    val colorGrading: ColorGrading = ColorGrading(),
    val visualFx: VisualFxSettings = VisualFxSettings(),
    val cutout: CutoutSettings = CutoutSettings(),
    val audioEnhance: AudioEnhanceSettings = AudioEnhanceSettings(),
    val canvasBackground: CanvasBackgroundSettings = CanvasBackgroundSettings(),
    val musicVisualizer: MusicVisualizerStyle = MusicVisualizerStyle.NONE,
    val beatMarkersMs: List<Long> = emptyList()
) {
    val totalDurationMs: Long
        get() = clips.maxOfOrNull { it.endMs }?.coerceAtLeast(500L) ?: 0L
}

fun defaultCurvePoints(preset: SpeedCurvePreset): List<SpeedCurvePoint> = when (preset) {
    SpeedCurvePreset.NONE, SpeedCurvePreset.CUSTOM -> listOf(
        SpeedCurvePoint(0f, 1.0f),
        SpeedCurvePoint(0.25f, 1.0f),
        SpeedCurvePoint(0.5f, 1.0f),
        SpeedCurvePoint(0.75f, 1.0f),
        SpeedCurvePoint(1f, 1.0f)
    )
    SpeedCurvePreset.MONTAGE -> listOf(
        SpeedCurvePoint(0f, 0.5f),
        SpeedCurvePoint(0.22f, 2.8f),
        SpeedCurvePoint(0.50f, 0.45f),
        SpeedCurvePoint(0.78f, 3.2f),
        SpeedCurvePoint(1f, 0.9f)
    )
    SpeedCurvePreset.HERO -> listOf(
        SpeedCurvePoint(0f, 2.4f),
        SpeedCurvePoint(0.28f, 2.0f),
        SpeedCurvePoint(0.50f, 0.28f),
        SpeedCurvePoint(0.72f, 2.2f),
        SpeedCurvePoint(1f, 1.4f)
    )
    SpeedCurvePreset.BULLET -> listOf(
        SpeedCurvePoint(0f, 4.2f),
        SpeedCurvePoint(0.30f, 0.35f),
        SpeedCurvePoint(0.65f, 0.35f),
        SpeedCurvePoint(0.85f, 3.8f),
        SpeedCurvePoint(1f, 1.2f)
    )
    SpeedCurvePreset.JUMP_CUT -> listOf(
        SpeedCurvePoint(0f, 1.0f),
        SpeedCurvePoint(0.20f, 5.2f),
        SpeedCurvePoint(0.45f, 0.6f),
        SpeedCurvePoint(0.70f, 5.5f),
        SpeedCurvePoint(1f, 1.0f)
    )
    SpeedCurvePreset.FLASH_IN -> listOf(
        SpeedCurvePoint(0f, 5.0f),
        SpeedCurvePoint(0.25f, 2.2f),
        SpeedCurvePoint(0.55f, 1.0f),
        SpeedCurvePoint(0.80f, 0.9f),
        SpeedCurvePoint(1f, 1.0f)
    )
    SpeedCurvePreset.FLASH_OUT -> listOf(
        SpeedCurvePoint(0f, 1.0f),
        SpeedCurvePoint(0.30f, 0.9f),
        SpeedCurvePoint(0.60f, 1.1f),
        SpeedCurvePoint(0.82f, 2.8f),
        SpeedCurvePoint(1f, 5.4f)
    )
}

enum class EditorToolTab(val label: String) {
    AI_LAB("AI Lab"),
    MEDIA("Media"),
    ANIMATE("Animate"),
    CANVAS("Canvas"),
    CAPTIONS("Captions"),
    CUTOUT("Cutout"),
    TRANSITION("Trans"),
    PIP_MASK("PIP/Mask"),
    COLOR("Filters"),
    FX("FX (18)"),
    AUDIO("Audio"),
    SPEED("Speed"),
    KEYFRAME("Key"),
    TEXT("Text")
}

data class VideoTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val aspectRatio: AspectRatioPreset,
    val durationMs: Long,
    val bpm: Int,
    val drawableKey: String,
    val tags: List<String>,
    val usesLabel: String,
    val accentHex: Long,
    val createDocument: () -> VideoProjectDocument
)

data class StoryboardScene(
    val id: String = UUID.randomUUID().toString(),
    val orderIndex: Int = 0,
    val title: String,
    val scriptNotes: String,
    val durationSec: Int = 4,
    val cameraAngle: String = "Wide Shot",
    val lightingTag: String = "Neon Rim",
    val isFilmed: Boolean = false
)

data class PostingTimeSlot(
    val day: String,
    val peakHour: String,
    val engagementScore: Float
)

data class TrendingAudioSuggestion(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Int,
    val durationMs: Long,
    val growthLabel: String,
    val moodTag: String,
    val accentHex: Long
)

data class ReelPerformanceMetric(
    val title: String,
    val views: String,
    val retentionPct: Int,
    val shares: String,
    val drawableKey: String
)

data class CreatorInsightsData(
    val periodLabel: String,
    val totalViews: String,
    val viewsDelta: String,
    val avgWatchTime: String,
    val completionRate: String,
    val followersDelta: String,
    val retentionCurve: List<Float>,
    val bestPostingSlots: List<PostingTimeSlot>,
    val trendingAudios: List<TrendingAudioSuggestion>,
    val topReels: List<ReelPerformanceMetric>
)
