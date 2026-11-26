package com.example.model

import android.net.Uri
import java.util.Locale
import kotlin.math.roundToInt

enum class PhotoUpscaleMultiplier(
    val label: String,
    val detailLabel: String,
    val factor: Int,
    val requiredRewardedAds: Int = 0
) {
    X1("1x", "Original Native", 1, requiredRewardedAds = 0),
    X2("2x", "2x Super-Res HD", 2, requiredRewardedAds = 0),
    X4("4x", "4x Ultra Sharp", 4, requiredRewardedAds = 0),
    X8("8x", "8x AI Cinema", 8, requiredRewardedAds = 1),
    X10("10x", "10x Ultra-HD Master", 10, requiredRewardedAds = 2);

    val adBadgeLabel: String?
        get() = when (requiredRewardedAds) {
            0 -> null
            1 -> "1 Ad"
            else -> "$requiredRewardedAds Ads"
        }

    val ctaLabel: String
        get() = when (requiredRewardedAds) {
            0 -> "Enhance $label"
            1 -> "Watch 1 Ad & Enhance $label"
            else -> "Watch $requiredRewardedAds Ads & Enhance $label"
        }
}

enum class PhotoExportResolution(
    val label: String,
    val detailLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val requiredRewardedAds: Int = 0
) {
    RES_1080P("1080p", "1920×1080 HD", 1920, 1080, requiredRewardedAds = 0),
    RES_2K("2K", "2560×1440 QHD", 2560, 1440, requiredRewardedAds = 0),
    RES_4K("4K", "3840×2160 4K UHD", 3840, 2160, requiredRewardedAds = 0),
    RES_8K("8K", "7680×4320 8K Cinema", 7680, 4320, requiredRewardedAds = 1),
    RES_16K("16K", "15360×8640 16K Pro", 15360, 8640, requiredRewardedAds = 2),
    RES_32K("32K", "30720×17280 Ultra-Print", 30720, 17280, requiredRewardedAds = 3);

    val adBadgeLabel: String?
        get() = when (requiredRewardedAds) {
            0 -> null
            1 -> "1 Ad"
            else -> "$requiredRewardedAds Ads"
        }

    val exportCtaLabel: String
        get() = when (requiredRewardedAds) {
            0 -> "Save $label Photo"
            1 -> "Watch 1 Ad & Export $label"
            else -> "Watch $requiredRewardedAds Ads & Export $label"
        }
}

enum class PhotoExportFormat(
    val label: String,
    val subtitle: String,
    val mimeType: String,
    val extension: String,
    val bytesPerPixelEstimate: Float
) {
    PNG("PNG", "Lossless 16-bit feel", "image/png", "png", 1.35f),
    JPG("JPG 100%", "Zero-loss JPEG", "image/jpeg", "jpg", 0.48f),
    WEBP("WEBP", "Ultra HDR Compact", "image/webp", "webp", 0.34f)
}

enum class DslrApertureStop(
    val fStopLabel: String,
    val blurAmount: Float
) {
    F8_0("f/8.0", 0.0f),
    F5_6("f/5.6", 0.16f),
    F4_0("f/4.0", 0.32f),
    F2_8("f/2.8", 0.52f),
    F1_8("f/1.8", 0.72f),
    F1_4("f/1.4", 0.88f),
    F1_2("f/1.2", 1.0f)
}

enum class SkyReplacementPreset(
    val label: String,
    val topColorHex: Long,
    val horizonColorHex: Long,
    val glowHex: Long
) {
    NONE("Original", 0xFF000000, 0xFF000000, 0xFF000000),
    GOLDEN_SUNSET("Golden Hour", 0xFF28193D, 0xFFFF7A3D, 0xFFFFD166),
    NEON_CYBER_NIGHT("Cyber Night", 0xFF0B0E26, 0xFF5B1F8A, 0xFF00F0FF),
    DRAMATIC_STORM("Epic Storm", 0xFF1A2233, 0xFF4F647B, 0xFFCBD5E1),
    AURORA_BOREALIS("Aurora Sky", 0xFF071B26, 0xFF10B981, 0xFF38BDF8),
    CLEAR_ALPINE_BLUE("Alpine Blue", 0xFF0F4C81, 0xFF7DD3FC, 0xFFF8FAFC)
}

enum class PhotoCanvasInteractionTool(val label: String) {
    REFOCUS_OR_SPLIT("Split / Focus"),
    BRUSH_BRIGHTEN_FACE("Brighten Face"),
    BRUSH_DARKEN_BG("Darken BG"),
    BLEMISH_HEAL("Blemish Tap"),
    MAGIC_ERASER("Magic Eraser")
}

enum class PhotoStudioToolTab(val label: String) {
    AI_UPSCALE_DSLR("AI & DSLR"),
    RAW_TONE("RAW 16-Bit"),
    CURVES_HSL("Curves & HSL"),
    RETOUCH_SKY("Retouch & Sky"),
    SELECTIVE_MASK("Masks")
}

data class PhotoBrushStroke(
    val normX: Float,
    val normY: Float,
    val radiusNorm: Float = 0.08f,
    val tool: PhotoCanvasInteractionTool
)

data class BlemishSpot(
    val normX: Float,
    val normY: Float,
    val radiusNorm: Float = 0.032f
)

data class RadialMaskSettings(
    val enabled: Boolean = false,
    val centerX: Float = 0.50f,
    val centerY: Float = 0.42f,
    val radius: Float = 0.38f,
    val feather: Float = 0.35f,
    val invertMask: Boolean = false,
    val exposureDelta: Float = 0.30f,  // + brightens face inside circle, or - darkens BG when inverted
    val clarityDelta: Float = 0.25f,
    val warmthDelta: Float = 0.08f
)

data class PhotoStudioSettings(
    // AI Super-Resolution & DSLR Engine
    val upscaleMultiplier: PhotoUpscaleMultiplier = PhotoUpscaleMultiplier.X1,
    val faceRestoration: Float = 0f,
    val apertureStop: DslrApertureStop = DslrApertureStop.F8_0,
    val bokehFocusX: Float = 0.50f,
    val bokehFocusY: Float = 0.42f,
    val autoBrightHdr: Float = 0f,
    val denoiseMicroSharpen: Float = 0f,
    // RAW 16-Bit Style Color Depth Sliders
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val texture: Float = 0f,
    val clarity: Float = 0f,
    val dehaze: Float = 0f,
    val vibrance: Float = 0f,
    // Shared ColorGrading (Exposure, Contrast, Highlights, Shadows, Saturation, Temp, Tint, LUT, 5-pt RGB Curves, 8-band HSL, Beauty)
    val colorGrading: ColorGrading = ColorGrading(),
    // Pro Retouching & AI Sky Replacement
    val teethWhiten: Float = 0f,
    val skyPreset: SkyReplacementPreset = SkyReplacementPreset.NONE,
    val skyIntensity: Float = 0.78f,
    val blemishSpots: List<BlemishSpot> = emptyList(),
    val magicEraseStrokes: List<PhotoBrushStroke> = emptyList(),
    // Selective Brush & Radial Masking
    val selectiveStrokes: List<PhotoBrushStroke> = emptyList(),
    val radialMask: RadialMaskSettings = RadialMaskSettings(),
    val selectiveFaceBrightenStrength: Float = 0.45f,
    val selectiveBgDarkenStrength: Float = 0.45f
)

data class PhotoExportEstimate(
    val targetWidthPx: Int,
    val targetHeightPx: Int,
    val dpi: Int = 300,
    val printInchesLabel: String,
    val estimatedSizeLabel: String,
    val megapixelsLabel: String,
    val tileGridLabel: String
)

data class PhotoExportProgressState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val currentTile: Int = 0,
    val totalTiles: Int = 0,
    val statusMessage: String = "",
    val exportedShareUri: Uri? = null,
    val exportedFilePath: String? = null,
    val exportedFileName: String = "NV_Photo_Master.png",
    val exportedFileSizeBytes: Long = 0L,
    val targetWidthPx: Int = 0,
    val targetHeightPx: Int = 0,
    val encodedWidthPx: Int = 0,
    val encodedHeightPx: Int = 0,
    val dpi: Int = 300,
    val format: PhotoExportFormat = PhotoExportFormat.PNG,
    val savedGalleryUri: Uri? = null,
    val savedGalleryPath: String? = null,
    val errorMessage: String? = null
)

data class PhotoStudioUiState(
    val sourceUri: String? = null,
    val sourceDisplayName: String = "Studio Portrait Sample",
    val sourceWidthPx: Int = 1080,
    val sourceHeightPx: Int = 1350,
    val settings: PhotoStudioSettings = PhotoStudioSettings(),
    val unlockedMultipliers: Set<PhotoUpscaleMultiplier> = setOf(
        PhotoUpscaleMultiplier.X1,
        PhotoUpscaleMultiplier.X2,
        PhotoUpscaleMultiplier.X4
    ),
    val activeToolTab: PhotoStudioToolTab = PhotoStudioToolTab.AI_UPSCALE_DSLR,
    val activeInteractionTool: PhotoCanvasInteractionTool = PhotoCanvasInteractionTool.REFOCUS_OR_SPLIT,
    val splitCompareEnabled: Boolean = true,
    val splitComparePosition: Float = 0.50f,
    val isProcessingEnhance: Boolean = false,
    val selectedExportResolution: PhotoExportResolution = PhotoExportResolution.RES_4K,
    val selectedExportFormat: PhotoExportFormat = PhotoExportFormat.PNG,
    val selectedExportDpi: Int = 300,
    val exportState: PhotoExportProgressState = PhotoExportProgressState(),
    val statusBanner: String? = null
)

object PhotoSpecCalculator {
    fun estimateExport(
        sourceW: Int,
        sourceH: Int,
        resolution: PhotoExportResolution,
        format: PhotoExportFormat,
        dpi: Int = 300
    ): PhotoExportEstimate {
        val safeW = sourceW.coerceAtLeast(1)
        val safeH = sourceH.coerceAtLeast(1)
        val aspect = safeW.toFloat() / safeH.toFloat()
        val longTarget = resolution.widthPx
        val (outW, outH) = if (aspect >= 1f) {
            longTarget to (longTarget / aspect).roundToInt().coerceAtLeast(1)
        } else {
            (longTarget * aspect).roundToInt().coerceAtLeast(1) to longTarget
        }
        val mp = (outW.toLong() * outH.toLong()) / 1_000_000.0
        val mpLabel = if (mp >= 10.0) {
            String.format(Locale.US, "%.0f MP", mp)
        } else {
            String.format(Locale.US, "%.1f MP", mp)
        }
        val estMb = ((outW.toLong() * outH.toLong()) * format.bytesPerPixelEstimate) / (1024.0 * 1024.0)
        val sizeLabel = if (estMb >= 1024.0) {
            String.format(Locale.US, "~%.2f GB", estMb / 1024.0)
        } else {
            String.format(Locale.US, "~%.1f MB", estMb.coerceAtLeast(0.4))
        }
        val safeDpi = dpi.coerceIn(72, 600)
        val printWIn = outW.toFloat() / safeDpi
        val printHIn = outH.toFloat() / safeDpi
        val printLabel = String.format(Locale.US, "%.1f\"×%.1f\" @ %d DPI", printWIn, printHIn, safeDpi)
        val tileCols = ((outW + 1023) / 1024).coerceAtLeast(1)
        val tileRows = ((outH + 1023) / 1024).coerceAtLeast(1)
        return PhotoExportEstimate(
            targetWidthPx = outW,
            targetHeightPx = outH,
            dpi = safeDpi,
            printInchesLabel = printLabel,
            estimatedSizeLabel = sizeLabel,
            megapixelsLabel = mpLabel,
            tileGridLabel = "${tileCols}×${tileRows} (${tileCols * tileRows} chunks)"
        )
    }
}
