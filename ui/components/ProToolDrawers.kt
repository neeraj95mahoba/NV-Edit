package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.EvaluatedTransform
import com.example.media.gpu.GpuEngineMetrics
import com.example.model.AspectRatioPreset
import com.example.model.AudioEffectPreset
import com.example.model.AudioEnhanceSettings
import com.example.model.AudioFilterPreset
import com.example.model.AudioStemMix
import com.example.model.AutoCaptionStyle
import com.example.model.CanvasBackgroundSettings
import com.example.model.ClipTransition
import com.example.model.ColorGrading
import com.example.model.ColorScopeMode
import com.example.model.CutoutSettings
import com.example.model.EditorToolTab
import com.example.model.KeyframeEasing
import com.example.model.LutPreset
import com.example.model.MusicVisualizerStyle
import com.example.model.RelightSettings
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimLoop
import com.example.model.TextAnimOut
import com.example.model.TextAnimationType
import com.example.model.TextClipStyle
import com.example.model.TextEffectStyle
import com.example.model.TextFontStyle
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.TransitionType
import com.example.model.VideoAnimCombo
import com.example.model.VideoAnimIn
import com.example.model.VideoAnimOut
import com.example.model.VideoProjectDocument
import com.example.model.ViralSfxType
import com.example.model.VisualFxSettings
import com.example.model.VisualFxType
import com.example.model.defaultHslBands
import com.example.ui.theme.AcidLime
import com.example.ui.theme.BeatMarkerYellow
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.TrackAudioAccent
import com.example.ui.theme.WarmAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun ProToolDrawerContent(
    activeTab: EditorToolTab,
    document: VideoProjectDocument,
    selectedClip: TimelineClip?,
    activeTransitionClipId: String?,
    evaluatedTransform: EvaluatedTransform,
    spectrumLevels: List<Float>,
    isAnalyzingBeats: Boolean,
    captionDraftScript: String,
    onCaptionDraftScriptChange: (String) -> Unit,
    onLaunchVoiceDictation: (String) -> Unit,
    onGenerateAutoCaptions: (String, AutoCaptionStyle, Int) -> Unit,
    onUpdateCutout: ((CutoutSettings) -> CutoutSettings) -> Unit,
    onPickCutoutCustomBg: () -> Unit,
    onDetectBeats: () -> Unit,
    onSnapVideoToBeats: () -> Unit,
    onUpdateMotionBlur: (Float) -> Unit,
    onUpdateTransition: (TransitionType?, Long?) -> Unit,
    onUpdatePipClip: ((TimelineClip) -> TimelineClip) -> Unit,
    onImportMainVisual: () -> Unit,
    onImportPipVisual: () -> Unit,
    onImportAudio: () -> Unit,
    onBrowseAllFiles: () -> Unit,
    onUpdateAudioEnhance: ((AudioEnhanceSettings) -> AudioEnhanceSettings) -> Unit,
    onAddVolumeKeyframe: (Float) -> Unit,
    onClearVolumeKeyframes: () -> Unit,
    onSelectLut: (LutPreset) -> Unit,
    onUpdateColorGrading: ((ColorGrading) -> ColorGrading) -> Unit,
    onResetColorGrading: () -> Unit,
    onRunAiColorMatch: () -> Unit = {},
    onAddAdjustmentLayer: () -> Unit = {},
    onAutoTrackSelectedOverlay: () -> Unit = {},
    onAddOrUpdateKeyframe: (Float?, Float?, Float?, Float?, Float?, KeyframeEasing?) -> Unit,
    onRemoveKeyframe: () -> Unit,
    onJumpKeyframe: (Boolean) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onApplySpeedPreset: (SpeedCurvePreset) -> Unit,
    onUpdateSpeedCurvePoint: (Int, Float) -> Unit,
    onAddTextClip: () -> Unit,
    onAddSticker: (String) -> Unit,
    onUpdateTextStyle: ((TextClipStyle) -> TextClipStyle) -> Unit,
    onSelectVisualFx: (VisualFxType) -> Unit,
    onUpdateVisualFx: ((VisualFxSettings) -> VisualFxSettings) -> Unit,
    onOpenAiLabMode: (AiLabMode) -> Unit,
    onUpdateClipAnimation: (VideoAnimIn?, VideoAnimOut?, VideoAnimCombo?, Long?) -> Unit,
    onFreezeFrame: () -> Unit,
    onToggleReverse: () -> Unit,
    onRotate90: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onUpdateCropZoom: (Float) -> Unit,
    onUpdateCanvasBg: ((CanvasBackgroundSettings) -> CanvasBackgroundSettings) -> Unit,
    onPickCustomCanvasBg: () -> Unit,
    onUpdateMusicVisualizer: (MusicVisualizerStyle) -> Unit,
    onAddViralSfx: (ViralSfxType) -> Unit = {},
    onToggleSmoothSlowMo: () -> Unit = {},
    playheadMs: Long = 0L,
    onUpdateKeyframeBezier: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onRecordRawVideo: () -> Unit = {},
    onRecordRawAudio: () -> Unit = {},
    // Next-Gen Cinema Studio callbacks
    colorScopeMode: ColorScopeMode = ColorScopeMode.OFF,
    isRunningAutoReframe: Boolean = false,
    onSetColorScopeMode: (ColorScopeMode) -> Unit = {},
    onUpdateRelight: ((RelightSettings) -> RelightSettings) -> Unit = {},
    onTranscribeTimeline: (String?) -> Unit = {},
    onToggleTranscriptWord: (String) -> Unit = {},
    onMarkFillerWords: () -> Unit = {},
    onApplyTranscriptCuts: () -> Unit = {},
    onApplyEditedParagraph: (String) -> Unit = {},
    onSeekToMs: (Long) -> Unit = {},
    onSyncMultiCamAngles: () -> Unit = {},
    onSwitchMultiCamAngle: (Int) -> Unit = {},
    onPickMultiCamAngleMedia: (Int) -> Unit = {},
    onRunAiAutoReframe: (AspectRatioPreset) -> Unit = {},
    onSplitAudioFourStems: () -> Unit = {},
    onUpdateAudioStemMix: ((AudioStemMix) -> AudioStemMix) -> Unit = {},
    // OpenGL ES 2.0 Real-Time GPU Filter Engine controls
    isGpuPreviewEnabled: Boolean = true,
    isSplitCompareEnabled: Boolean = false,
    gpuMetrics: GpuEngineMetrics = GpuEngineMetrics(),
    onToggleGpuPreview: () -> Unit = {},
    onToggleSplitCompare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val mainClips = remember(document.clips) {
        document.clips.filter { it.trackType == TrackType.MAIN_VIDEO }.sortedBy { it.startMs }
    }
    val activeTransition = remember(mainClips, activeTransitionClipId) {
        mainClips.firstOrNull { it.id == activeTransitionClipId }?.transitionIn
            ?: mainClips.getOrNull(1)?.transitionIn
            ?: ClipTransition()
    }
    val activePip = remember(document.clips, selectedClip) {
        selectedClip?.takeIf { it.trackType == TrackType.OVERLAY_PIP }
            ?: document.clips.firstOrNull { it.trackType == TrackType.OVERLAY_PIP }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(232.dp)
            .background(palette.glassSurface)
            .border(width = 0.5.dp, color = palette.accentPrimary.copy(alpha = 0.35f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (activeTab) {
            EditorToolTab.AI_LAB -> AiLabQuickDrawer(
                onOpenAiLabMode = onOpenAiLabMode
            )
            EditorToolTab.TEXT_EDIT -> TextBasedVideoEditDrawer(
                transcriptWords = document.transcriptWords,
                playheadMs = playheadMs,
                onTranscribeTimeline = onTranscribeTimeline,
                onLaunchDictation = { onLaunchVoiceDictation("en-US") },
                onToggleWordDeleted = onToggleTranscriptWord,
                onMarkFillerWords = onMarkFillerWords,
                onApplyTranscriptCuts = onApplyTranscriptCuts,
                onApplyEditedParagraph = onApplyEditedParagraph,
                onSeekToWord = onSeekToMs
            )
            EditorToolTab.MULTICAM -> MultiCamAndReframeDrawer(
                angles = document.multiCamAngles,
                activeAngleIndex = document.activeMultiCamAngle,
                currentAspectRatio = document.aspectRatio,
                isRunningAutoReframe = isRunningAutoReframe,
                onSyncAngles = onSyncMultiCamAngles,
                onSwitchAngle = onSwitchMultiCamAngle,
                onPickAngleMedia = onPickMultiCamAngleMedia,
                onRunAutoReframe = onRunAiAutoReframe
            )
            EditorToolTab.MEDIA -> MediaImportDrawer(
                onImportMainVisual = onImportMainVisual,
                onImportPipVisual = onImportPipVisual,
                onImportAudio = onImportAudio,
                onBrowseAllFiles = onBrowseAllFiles,
                onRecordRawVideo = onRecordRawVideo,
                onRecordRawAudio = onRecordRawAudio
            )
            EditorToolTab.ANIMATE -> VideoAnimationProDrawer(
                selectedClip = selectedClip ?: mainClips.firstOrNull(),
                onUpdateAnimation = onUpdateClipAnimation
            )
            EditorToolTab.CANVAS -> CanvasAndClipToolsDrawer(
                selectedClip = selectedClip ?: mainClips.firstOrNull(),
                canvasBg = document.canvasBackground,
                musicVisualizer = document.musicVisualizer,
                onFreezeFrame = onFreezeFrame,
                onToggleReverse = onToggleReverse,
                onRotate90 = onRotate90,
                onToggleFlipH = onToggleFlipH,
                onToggleFlipV = onToggleFlipV,
                onUpdateCropZoom = onUpdateCropZoom,
                onUpdateCanvasBg = onUpdateCanvasBg,
                onPickCustomCanvasBg = onPickCustomCanvasBg,
                onUpdateMusicVisualizer = onUpdateMusicVisualizer
            )
            EditorToolTab.CAPTIONS -> AutoCaptionsProDrawer(
                speechTranscript = captionDraftScript,
                onSpeechTranscriptChange = onCaptionDraftScriptChange,
                onLaunchVoiceDictation = onLaunchVoiceDictation,
                onGenerateCaptions = onGenerateAutoCaptions
            )
            EditorToolTab.CUTOUT -> CutoutAndChromaDrawer(
                cutout = document.cutout,
                onUpdateCutout = onUpdateCutout,
                onPickCustomBackground = onPickCutoutCustomBg
            )
            EditorToolTab.TRANSITION -> ProTransitionsDrawer(
                activeTransition = activeTransition,
                hasMultipleClips = mainClips.size >= 2,
                onSelectTransition = { onUpdateTransition(it, null) },
                onDurationChange = { onUpdateTransition(null, it) }
            )
            EditorToolTab.PIP_MASK -> PipAndMaskingProDrawer(
                activePip = activePip,
                onImportPipMedia = onImportPipVisual,
                onUpdatePip = onUpdatePipClip,
                onAutoTrackOverlay = onAutoTrackSelectedOverlay
            )
            EditorToolTab.AUDIO -> AudioEnhanceDrawer(
                settings = document.audioEnhance,
                spectrumLevels = spectrumLevels,
                beatCount = document.beatMarkersMs.size,
                isAnalyzingBeats = isAnalyzingBeats,
                onDetectBeats = onDetectBeats,
                onSnapVideoToBeats = onSnapVideoToBeats,
                onUpdate = onUpdateAudioEnhance,
                onAddVolumeKeyframe = onAddVolumeKeyframe,
                onClearVolumeKeyframes = onClearVolumeKeyframes,
                onAddViralSfx = onAddViralSfx,
                onRecordRawAudio = onRecordRawAudio,
                onSplitFourStems = onSplitAudioFourStems,
                onUpdateStemMix = onUpdateAudioStemMix
            )
            EditorToolTab.COLOR -> {
                val adjClip = selectedClip?.takeIf { it.trackType == TrackType.ADJUSTMENT_LAYER }
                ColorGradingProDrawer(
                    grading = adjClip?.adjustmentGrading ?: document.colorGrading,
                    relight = document.relight,
                    colorScopeMode = colorScopeMode,
                    isEditingAdjustmentLayer = adjClip != null,
                    isGpuPreviewEnabled = isGpuPreviewEnabled,
                    isSplitCompareEnabled = isSplitCompareEnabled,
                    gpuMetrics = gpuMetrics,
                    onToggleGpuPreview = onToggleGpuPreview,
                    onToggleSplitCompare = onToggleSplitCompare,
                    onSelectLut = onSelectLut,
                    onUpdate = onUpdateColorGrading,
                    onUpdateRelight = onUpdateRelight,
                    onSetColorScopeMode = onSetColorScopeMode,
                    onReset = onResetColorGrading,
                    onRunAiColorMatch = onRunAiColorMatch,
                    onAddAdjustmentLayer = onAddAdjustmentLayer
                )
            }
            EditorToolTab.KEYFRAME -> KeyframesDrawer(
                selectedClip = selectedClip,
                playheadMs = playheadMs,
                transform = evaluatedTransform,
                onAddOrUpdate = onAddOrUpdateKeyframe,
                onUpdateBezier = onUpdateKeyframeBezier,
                onRemove = onRemoveKeyframe,
                onJump = onJumpKeyframe
            )
            EditorToolTab.SPEED -> SpeedCurveDrawer(
                selectedClip = selectedClip ?: mainClips.firstOrNull(),
                motionBlur = document.visualFx.motionBlur,
                onUpdateSpeed = onUpdateSpeed,
                onApplyPreset = onApplySpeedPreset,
                onUpdatePoint = onUpdateSpeedCurvePoint,
                onUpdateMotionBlur = onUpdateMotionBlur,
                onToggleSmoothSlowMo = onToggleSmoothSlowMo
            )
            EditorToolTab.TEXT -> {
                val activeTextClip = selectedClip?.takeIf { it.trackType == TrackType.TEXT_STICKER }
                    ?: document.clips.firstOrNull { it.trackType == TrackType.TEXT_STICKER }
                TextAnimationDrawer(
                    activeTextStyle = activeTextClip?.textStyle ?: TextClipStyle(),
                    isAutoTracked = activeTextClip?.autoTrackEnabled == true,
                    onAddTextClip = onAddTextClip,
                    onAddSticker = onAddSticker,
                    onUpdateStyle = onUpdateTextStyle,
                    onAutoTrackSelectedOverlay = onAutoTrackSelectedOverlay
                )
            }
            EditorToolTab.FX -> VisualFxDrawer(
                fx = document.visualFx,
                onSelectFx = onSelectVisualFx,
                onUpdateFx = onUpdateVisualFx
            )
        }
    }
}

@Composable
private fun MediaImportDrawer(
    onImportMainVisual: () -> Unit,
    onImportPipVisual: () -> Unit,
    onImportAudio: () -> Unit,
    onBrowseAllFiles: () -> Unit,
    onRecordRawVideo: () -> Unit,
    onRecordRawAudio: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val permState = LocalStudioCaptureController.current.permissionState

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Direct Capture & Real Media", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (permState.hasCameraPermission) AcidLime.copy(alpha = 0.16f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (permState.hasCameraPermission) AcidLime.copy(alpha = 0.5f) else palette.border)
            ) {
                Text(
                    text = if (permState.hasCameraPermission) "Cam ✓" else "Cam",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (permState.hasCameraPermission) AcidLime else palette.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (permState.hasAudioPermission) AcidLime.copy(alpha = 0.16f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (permState.hasAudioPermission) AcidLime.copy(alpha = 0.5f) else palette.border)
            ) {
                Text(
                    text = if (permState.hasAudioPermission) "Mic ✓" else "Mic",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (permState.hasAudioPermission) AcidLime else palette.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    // Direct Hardware Recording into Studio (Camera Raw Video + Microphone Raw Audio)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaActionTile(
            title = "Record Video",
            subtitle = "Raw Camera",
            icon = Icons.Filled.Videocam,
            accent = CoralRed,
            testTag = "drawer_record_raw_video",
            onClick = onRecordRawVideo,
            modifier = Modifier.weight(1f)
        )
        MediaActionTile(
            title = "Record Audio",
            subtitle = "Raw Mic AAC",
            icon = Icons.Filled.Mic,
            accent = ProGold,
            testTag = "drawer_record_raw_audio",
            onClick = onRecordRawAudio,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaActionTile("Videos / Photos", "Gallery Multi", Icons.Filled.AddPhotoAlternate, ElectricPurple, "drawer_import_main_media", onImportMainVisual, Modifier.weight(1f))
        MediaActionTile("Overlay PIP", "Layer Media", Icons.Filled.Layers, NeonCyan, "drawer_import_pip_media", onImportPipVisual, Modifier.weight(1f))
        MediaActionTile("Audio / Music", "Local Sound", Icons.Filled.LibraryMusic, TrackAudioAccent, "drawer_import_audio_media", onImportAudio, Modifier.weight(1f))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBrowseAllFiles)
            .testTag("drawer_browse_all_files")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Browse Phone Storage (Video, Photo, Audio)",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "Select one or multiple local files directly onto your timeline",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Browse device files",
                tint = palette.accentSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MediaActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
    }
}

@Composable
private fun AudioEnhanceDrawer(
    settings: AudioEnhanceSettings,
    spectrumLevels: List<Float>,
    beatCount: Int,
    isAnalyzingBeats: Boolean,
    onDetectBeats: () -> Unit,
    onSnapVideoToBeats: () -> Unit,
    onUpdate: ((AudioEnhanceSettings) -> AudioEnhanceSettings) -> Unit,
    onAddVolumeKeyframe: (Float) -> Unit,
    onClearVolumeKeyframes: () -> Unit,
    onAddViralSfx: (ViralSfxType) -> Unit,
    onRecordRawAudio: () -> Unit = {},
    onSplitFourStems: () -> Unit = {},
    onUpdateStemMix: ((AudioStemMix) -> AudioStemMix) -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = TrackAudioAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Audio Mixer & Mic", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralRed.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, CoralRed),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRecordRawAudio()
                    }
                    .testTag("audio_record_voiceover_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Record raw voiceover", tint = CoralRed, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Rec Mic", style = MaterialTheme.typography.labelSmall, color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BeatMarkerYellow.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, BeatMarkerYellow),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDetectBeats()
                    }
                    .testTag("audio_detect_beats_button")
            ) {
                Text(
                    text = if (isAnalyzingBeats) "Scanning…" else "Beats ($beatCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = BeatMarkerYellow,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricPurple.copy(alpha = 0.26f),
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSnapVideoToBeats()
                    }
                    .testTag("audio_snap_cut_beats_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Snap", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }
    }

    // 9 Audio Filters (EQ & Enhancement)
    Text("Audio Filters (9 Presets)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AudioFilterPreset.entries) { filter ->
            val active = settings.audioFilter == filter
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (active) TrackAudioAccent.copy(alpha = 0.24f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) TrackAudioAccent else palette.border),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdate { it.copy(audioFilter = filter) }
                    }
                    .testTag("audio_filter_${filter.name}")
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }

    // 9 Audio Effects & Voice Changers
    Text("Voice Changers & Audio FX (9 Presets)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AudioEffectPreset.entries) { effect ->
            val active = settings.audioEffect == effect
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (active) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdate { it.copy(audioEffect = effect) }
                    }
                    .testTag("audio_fx_${effect.name}")
            ) {
                Text(
                    text = effect.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (settings.audioEffect != AudioEffectPreset.NONE) {
        CompactProSlider("Audio FX Intensity", settings.effectIntensity, 0f..1f, "${(settings.effectIntensity * 100).roundToInt()}%") { v ->
            onUpdate { it.copy(effectIntensity = v) }
        }
    }

    // Live 16-Band Spectrum Visualizer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(OledBlack)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        spectrumLevels.forEachIndexed { idx, level ->
            val barColor = when {
                idx < 4 -> ElectricPurple
                idx in 4..11 -> NeonCyan
                else -> TrackAudioAccent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((level.coerceIn(0.08f, 1f) * 22f).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeonCyan.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .clickable { onAddVolumeKeyframe(settings.masterVolume) }
                .testTag("audio_add_vol_keyframe")
        ) {
            Text(
                text = "+ Vol Keyframe (${settings.volumeKeyframes.size})",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        if (settings.volumeKeyframes.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralRed.copy(alpha = 0.16f),
                modifier = Modifier.clickable(onClick = onClearVolumeKeyframes)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelSmall, color = CoralRed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
        }
    }

    // Built-in Synthesized Viral Sound Effects (Tap to preview & drop onto timeline)
    Text("Viral SFX Library (Tap to Preview & Drop)", style = MaterialTheme.typography.labelSmall, color = ProGold)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(ViralSfxType.entries) { sfx ->
            val accent = Color(sfx.accentHex)
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddViralSfx(sfx)
                    }
                    .testTag("audio_sfx_${sfx.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add ${sfx.label} SFX", tint = accent, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = sfx.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Studio Auto-Ducking & AI Voice Isolation Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Studio Mixing & Mastering", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (settings.autoDucking) AcidLime.copy(alpha = 0.22f) else palette.surfaceElevated,
            border = BorderStroke(1.dp, if (settings.autoDucking) AcidLime else palette.border),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUpdate { it.copy(autoDucking = !it.autoDucking) }
                }
                .testTag("audio_auto_ducking_toggle")
        ) {
            Text(
                text = if (settings.autoDucking) "Auto-Ducking ON ✓" else "Auto-Ducking OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (settings.autoDucking) AcidLime else palette.textSecondary,
                fontWeight = if (settings.autoDucking) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }

    CompactProSlider("AI Voice Isolation (De-Music)", settings.voiceIsolation, 0f..1f, "${(settings.voiceIsolation * 100).roundToInt()}%") { v ->
        onUpdate { it.copy(voiceIsolation = v) }
    }

    // 4-Stem AI Audio Splitter (Vocals / Drums / Bass / Melody) + Auto-Ducking Depth
    AudioStemSplitterRack(
        stemMix = settings.stemMix,
        autoDucking = settings.autoDucking,
        duckingDepthDb = settings.duckingDepthDb,
        onSplitFourStems = onSplitFourStems,
        onUpdateStemMix = onUpdateStemMix,
        onUpdateDuckingDepth = { db -> onUpdate { it.copy(duckingDepthDb = db) } }
    )

    // 10-Band Parametric Studio Equalizer
    TenBandEqualizerRack(
        eqBandsDb = settings.eqBandsDb,
        onUpdateBand = { bandIdx, db ->
            onUpdate { curr ->
                val list = (if (curr.eqBandsDb.size == 10) curr.eqBandsDb else List(10) { 0f }).toMutableList()
                if (bandIdx in list.indices) list[bandIdx] = db.coerceIn(-12f, 12f)
                curr.copy(eqBandsDb = list)
            }
        },
        onApplyPreset = { presetBands ->
            onUpdate { it.copy(eqBandsDb = presetBands) }
        }
    )

    CompactProSlider("Pitch Shifter", settings.manualPitch, 0.5f..2.0f, String.format(Locale.US, "%.2fx", settings.manualPitch)) { v ->
        onUpdate { it.copy(manualPitch = v) }
    }
    CompactProSlider("Fade In", settings.fadeInMs / 1000f, 0f..4f, String.format(Locale.US, "%.1fs", settings.fadeInMs / 1000f)) { sec ->
        onUpdate { it.copy(fadeInMs = (sec * 1000f).toLong()) }
    }
    CompactProSlider("Fade Out", settings.fadeOutMs / 1000f, 0f..4f, String.format(Locale.US, "%.1fs", settings.fadeOutMs / 1000f)) { sec ->
        onUpdate { it.copy(fadeOutMs = (sec * 1000f).toLong()) }
    }
    CompactProSlider("Noise Reduction", settings.noiseReduction, 0f..1f, "${(settings.noiseReduction * 100).roundToInt()}%") { v ->
        onUpdate { it.copy(noiseReduction = v) }
    }
    CompactProSlider("Voice Boost (Clarity)", settings.voiceBoost, 0f..1f, "${(settings.voiceBoost * 100).roundToInt()}%") { v ->
        onUpdate { it.copy(voiceBoost = v) }
    }
    CompactProSlider("Bass Boost (Sub EQ)", settings.bassBoost, 0f..1f, "${(settings.bassBoost * 100).roundToInt()}%") { v ->
        onUpdate { it.copy(bassBoost = v) }
    }
    CompactProSlider("Master Volume", settings.masterVolume, 0f..1.5f, "${(settings.masterVolume * 100).roundToInt()}%") { v ->
        onUpdate { it.copy(masterVolume = v) }
    }
}

@Composable
private fun ColorGradingProDrawer(
    grading: ColorGrading,
    relight: RelightSettings,
    colorScopeMode: ColorScopeMode,
    isEditingAdjustmentLayer: Boolean,
    isGpuPreviewEnabled: Boolean,
    isSplitCompareEnabled: Boolean,
    gpuMetrics: GpuEngineMetrics,
    onToggleGpuPreview: () -> Unit,
    onToggleSplitCompare: () -> Unit,
    onSelectLut: (LutPreset) -> Unit,
    onUpdate: ((ColorGrading) -> ColorGrading) -> Unit,
    onUpdateRelight: ((RelightSettings) -> RelightSettings) -> Unit,
    onSetColorScopeMode: (ColorScopeMode) -> Unit,
    onReset: () -> Unit,
    onRunAiColorMatch: () -> Unit,
    onAddAdjustmentLayer: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var subTab by remember { mutableIntStateOf(0) } // 0 = LUTs, 1 = Curves & HSL, 2 = Wheels, 3 = 3D Relight & Bokeh, 4 = Film/Retouch

    // OpenGL ES 2.0 Real-Time GPU Filter Engine Status & Before/After Split Wipe Bar
    val gpuRunning = isGpuPreviewEnabled && !gpuMetrics.hasFailed
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = if (gpuRunning) NeonCyan.copy(alpha = 0.14f) else ProGold.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, if (gpuRunning) NeonCyan.copy(alpha = 0.7f) else ProGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(9.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleGpuPreview()
                }
                .testTag("color_gpu_engine_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "OpenGL ES 2.0 GPU Engine",
                        tint = if (gpuRunning) NeonCyan else ProGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (gpuRunning) "OpenGL ES GPU" else "CPU Reference",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gpuRunning) NeonCyan else ProGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (gpuRunning && gpuMetrics.lastRenderMs > 0f) {
                        String.format(Locale.US, "%.1fms · GLSL", gpuMetrics.lastRenderMs.coerceAtMost(99.9f))
                    } else if (gpuRunning) {
                        "Instant GLSL"
                    } else {
                        "Tap for GPU"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(9.dp),
            color = if (isSplitCompareEnabled && gpuRunning) ElectricPurple.copy(alpha = 0.35f) else palette.surfaceElevated,
            border = BorderStroke(1.dp, if (isSplitCompareEnabled && gpuRunning) NeonCyan else palette.border),
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleSplitCompare()
                }
                .testTag("color_gpu_split_wipe_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Compare,
                    contentDescription = "Toggle GPU Split Wipe",
                    tint = if (isSplitCompareEnabled && gpuRunning) NeonCyan else palette.textSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isSplitCompareEnabled && gpuRunning) "Wipe ON" else "Split Wipe",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSplitCompareEnabled && gpuRunning) NeonCyan else palette.textPrimary,
                    fontWeight = if (isSplitCompareEnabled && gpuRunning) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }

    // 1-Tap AI Color Match & Enhancer + Adjustment Layer Quick Strip
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NeonCyan.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRunAiColorMatch()
                }
                .testTag("ai_color_match_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Color Match", tint = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (isEditingAdjustmentLayer) "AI Match (Adj Layer)" else "AI Color Match & HDR",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ProGold.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, ProGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddAdjustmentLayer()
                }
                .testTag("color_add_adj_layer_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = "Add Adjustment Layer", tint = ProGold, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Adj Layer", style = MaterialTheme.typography.labelSmall, color = ProGold, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Live Cinema Color Scopes Selector Row (RGB Parade / Luma Waveform / Histogram)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Scopes:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        ColorScopeMode.entries.forEach { mode ->
            val active = colorScopeMode == mode
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (active) NeonCyan.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetColorScopeMode(mode)
                    }
                    .testTag("color_scope_mode_${mode.name}")
            ) {
                Text(
                    text = if (mode == ColorScopeMode.OFF) "Off" else mode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) NeonCyan else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(9.dp))
                .background(palette.background)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val subTabs = listOf(
                "LUTs (${LutPreset.entries.size})",
                "Curves & HSL",
                "Wheels",
                "3D Relight",
                "Film/Retouch"
            )
            items(subTabs.indices.toList()) { idx ->
                val active = subTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (active) ElectricPurple else Color.Transparent)
                        .clickable { subTab = idx }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("color_subtab_$idx")
                ) {
                    Text(subTabs[idx], style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.textSecondary)
                }
            }
        }

        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.surfaceElevated,
            modifier = Modifier
                .clickable(onClick = onReset)
                .testTag("color_reset_button")
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset grading", tint = palette.textSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("Reset", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            }
        }
    }

    when (subTab) {
        0 -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(LutPreset.entries) { lut ->
                    val active = lut == grading.lutPreset
                    val swatch = Color(lut.badgeColorHex)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) swatch.copy(alpha = 0.25f) else palette.surfaceElevated,
                        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) NeonCyan else palette.border),
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectLut(lut)
                            }
                            .testTag("lut_preset_${lut.name}")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(swatch))
                            Spacer(Modifier.width(6.dp))
                            Text(lut.displayName, style = MaterialTheme.typography.labelSmall, color = if (active) palette.textPrimary else palette.textSecondary, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            CompactProSlider("Filter Intensity", grading.filterIntensity, 0f..1f, "${(grading.filterIntensity * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(filterIntensity = v) }
            }
            CompactProSlider("HDR Boost", grading.hdrBoost, 0f..1f, "${(grading.hdrBoost * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(hdrBoost = v) }
            }
            CompactProSlider("Low-Light Denoise", grading.lowLightDenoise, 0f..1f, "${(grading.lowLightDenoise * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(lowLightDenoise = v) }
            }
            CompactProSlider("Brightness", grading.brightness, -1f..1f, formatSignedPct(grading.brightness)) { v -> onUpdate { it.copy(brightness = v) } }
            CompactProSlider("Contrast", grading.contrast, -1f..1f, formatSignedPct(grading.contrast)) { v -> onUpdate { it.copy(contrast = v) } }
            CompactProSlider("Saturation", grading.saturation, -1f..1f, formatSignedPct(grading.saturation)) { v -> onUpdate { it.copy(saturation = v) } }
            CompactProSlider("Exposure", grading.exposure, -1f..1f, formatSignedPct(grading.exposure)) { v -> onUpdate { it.copy(exposure = v) } }
            CompactProSlider("Vignette", grading.vignette, 0f..1f, "${(grading.vignette * 100).roundToInt()}%") { v -> onUpdate { it.copy(vignette = v) } }
            CompactProSlider("Sharpen", grading.sharpen, 0f..1f, "${(grading.sharpen * 100).roundToInt()}%") { v -> onUpdate { it.copy(sharpen = v) } }
        }
        1 -> {
            RgbToneCurveEditor(
                grading = grading,
                onUpdate = onUpdate
            )
            HslColorMixerPanel(
                bands = grading.hslBands,
                onUpdateBand = { bandIdx, transform ->
                    onUpdate { g ->
                        val base = (if (g.hslBands.size >= 8) g.hslBands else defaultHslBands()).toMutableList()
                        if (bandIdx in base.indices) {
                            base[bandIdx] = transform(base[bandIdx])
                        }
                        g.copy(hslBands = base)
                    }
                }
            )
        }
        2 -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InteractiveColorWheel("Shadows", grading.shadowWheel, { w -> onUpdate { it.copy(shadowWheel = w) } })
                InteractiveColorWheel("Midtones", grading.midtoneWheel, { w -> onUpdate { it.copy(midtoneWheel = w) } })
                InteractiveColorWheel("Highlights", grading.highlightWheel, { w -> onUpdate { it.copy(highlightWheel = w) } })
            }
            CompactProSlider("Temperature", grading.temperature, -1f..1f, formatSignedPct(grading.temperature)) { v -> onUpdate { it.copy(temperature = v) } }
            CompactProSlider("Tint (Green / Magenta)", grading.tint, -1f..1f, formatSignedPct(grading.tint)) { v -> onUpdate { it.copy(tint = v) } }
            CompactProSlider("Highlights", grading.highlights, -1f..1f, formatSignedPct(grading.highlights)) { v -> onUpdate { it.copy(highlights = v) } }
            CompactProSlider("Shadows", grading.shadows, -1f..1f, formatSignedPct(grading.shadows)) { v -> onUpdate { it.copy(shadows = v) } }
            CompactProSlider("Matte Fade", grading.fade, 0f..1f, "${(grading.fade * 100).roundToInt()}%") { v -> onUpdate { it.copy(fade = v) } }
        }
        3 -> {
            AiRelightStudioPanel(
                relight = relight,
                onUpdateRelight = onUpdateRelight
            )
        }
        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FaceRetouchingNatural, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("35mm Cinema Texture & Portrait Retouch", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            }
            CompactProSlider("Cinema Film Grain (35mm)", grading.filmGrain, 0f..1f, "${(grading.filmGrain * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(filmGrain = v) }
            }
            CompactProSlider("Cinema Halation (Highlight Bloom)", grading.halation, 0f..1f, "${(grading.halation * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(halation = v) }
            }
            CompactProSlider("Skin Smooth (Soft Focus)", grading.beauty.skinSmooth, 0f..1f, "${(grading.beauty.skinSmooth * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(beauty = it.beauty.copy(skinSmooth = v)) }
            }
            CompactProSlider("Face Glow & Brighten", grading.beauty.faceGlow, 0f..1f, "${(grading.beauty.faceGlow * 100).roundToInt()}%") { v ->
                onUpdate { it.copy(beauty = it.beauty.copy(faceGlow = v)) }
            }
        }
    }
}

@Composable
private fun KeyframesDrawer(
    selectedClip: TimelineClip?,
    playheadMs: Long,
    transform: EvaluatedTransform,
    onAddOrUpdate: (Float?, Float?, Float?, Float?, Float?, KeyframeEasing?) -> Unit,
    onUpdateBezier: (Float, Float, Float, Float) -> Unit,
    onRemove: () -> Unit,
    onJump: (Boolean) -> Unit
) {
    val palette = LocalStudioPalette.current
    val activeKf = remember(selectedClip, playheadMs) {
        if (selectedClip == null) null
        else {
            val localMs = (playheadMs - selectedClip.startMs).coerceIn(0L, selectedClip.durationMs)
            selectedClip.keyframes.minByOrNull { abs(it.timeOffsetMs - localMs) }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Diamond, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Keyframes & Bezier (${selectedClip?.keyframes?.size ?: 0})", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onJump(false) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous keyframe", tint = palette.textSecondary)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WarmAmber.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, WarmAmber),
                modifier = Modifier
                    .clickable { onAddOrUpdate(null, null, null, null, null, null) }
                    .testTag("add_keyframe_button")
            ) {
                Text("+ Keyframe", style = MaterialTheme.typography.labelSmall, color = WarmAmber, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .testTag("remove_keyframe_button")
            ) {
                Text("Remove", style = MaterialTheme.typography.labelSmall, color = CoralRed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
            IconButton(onClick = { onJump(true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next keyframe", tint = palette.textSecondary)
            }
        }
    }

    BezierEasingGraphEditor(
        activeEasing = activeKf?.easing ?: KeyframeEasing.EASE_IN_OUT,
        bezierX1 = activeKf?.bezierX1 ?: 0.25f,
        bezierY1 = activeKf?.bezierY1 ?: 0.10f,
        bezierX2 = activeKf?.bezierX2 ?: 0.25f,
        bezierY2 = activeKf?.bezierY2 ?: 1.00f,
        onSelectEasing = { easing -> onAddOrUpdate(null, null, null, null, null, easing) },
        onUpdateBezier = { x1, y1, x2, y2 -> onUpdateBezier(x1, y1, x2, y2) }
    )

    CompactProSlider("Zoom Scale", transform.scale, 0.35f..2.6f, String.format(Locale.US, "%.2fx", transform.scale)) { v -> onAddOrUpdate(v, null, null, null, null, null) }
    CompactProSlider("Pan X", transform.offsetX, -0.6f..0.6f, formatSignedPct(transform.offsetX)) { v -> onAddOrUpdate(null, v, null, null, null, null) }
    CompactProSlider("Pan Y", transform.offsetY, -0.6f..0.6f, formatSignedPct(transform.offsetY)) { v -> onAddOrUpdate(null, null, v, null, null, null) }
    CompactProSlider("Rotation", transform.rotationDeg, -180f..180f, "${transform.rotationDeg.roundToInt()}°") { v -> onAddOrUpdate(null, null, null, v, null, null) }
    CompactProSlider("Opacity", transform.opacity, 0f..1f, "${(transform.opacity * 100).roundToInt()}%") { v -> onAddOrUpdate(null, null, null, null, v, null) }
}

@Composable
private fun SpeedCurveDrawer(
    selectedClip: TimelineClip?,
    motionBlur: Float,
    onUpdateSpeed: (Float) -> Unit,
    onApplyPreset: (SpeedCurvePreset) -> Unit,
    onUpdatePoint: (Int, Float) -> Unit,
    onUpdateMotionBlur: (Float) -> Unit,
    onToggleSmoothSlowMo: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var curveMode by remember { mutableStateOf(true) }
    val clip = selectedClip

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Velocity & Smooth Slow-Mo PRO", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(palette.background)
                .padding(2.dp)
        ) {
            listOf(false to "Normal", true to "Curve").forEach { (isCurve, title) ->
                val active = curveMode == isCurve
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) ElectricPurple else Color.Transparent)
                        .clickable { curveMode = isCurve }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("speed_mode_$title")
                ) {
                    Text(title, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.textSecondary)
                }
            }
        }
    }

    // 1-Tap Optical-Flow Smooth Slow-Mo Toggle
    val smoothActive = clip?.smoothSlowMo == true
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (smoothActive) NeonCyan.copy(alpha = 0.20f) else palette.surfaceElevated,
        border = BorderStroke(1.dp, if (smoothActive) NeonCyan else ProGold.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleSmoothSlowMo()
            }
            .testTag("speed_smooth_slowmo_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Optical-Flow Smooth Slow-Mo", tint = if (smoothActive) NeonCyan else ProGold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Optical-Flow Smooth Slow-Mo (0.05x–100x)",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = if (smoothActive) "ON ✓" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (smoothActive) NeonCyan else palette.textMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }

    CompactProSlider("Pro Velocity Motion Blur", motionBlur, 0f..1f, "${(motionBlur * 100).roundToInt()}%", onUpdateMotionBlur)

    if (clip == null) {
        Text("Select a video clip on the timeline to edit velocity.", style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
        return
    }

    if (!curveMode) {
        val logNorm = ((ln(clip.speed.coerceIn(0.05f, 100f).toDouble()) - ln(0.05)) / (ln(100.0) - ln(0.05))).toFloat().coerceIn(0f, 1f)
        val spdLabel = if (clip.speed >= 10f) String.format(Locale.US, "%.1fx", clip.speed) else String.format(Locale.US, "%.2fx", clip.speed)
        CompactProSlider("Extreme Speed (0.05x–100x)", logNorm, 0f..1f, spdLabel) { norm ->
            val mapped = (0.05f * 2000f.pow(norm)).coerceIn(0.05f, 100f)
            onUpdateSpeed(if (abs(mapped - 1f) < 0.06f) 1f else mapped)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(0.05f, 0.2f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f, 50.0f, 100.0f)) { presetSpd ->
                val active = abs(clip.speed - presetSpd) < (if (presetSpd >= 10f) 1.5f else 0.04f)
                val chipLabel = if (presetSpd >= 10f) "${presetSpd.roundToInt()}x" else "${presetSpd}x"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) NeonCyan.copy(alpha = 0.22f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clickable { onUpdateSpeed(presetSpd) }
                        .testTag("speed_quick_${presetSpd}")
                ) {
                    Text(chipLabel, style = MaterialTheme.typography.labelSmall, color = if (active) NeonCyan else palette.accentSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(SpeedCurvePreset.entries.filter { it != SpeedCurvePreset.CUSTOM }) { preset ->
                val active = clip.speedCurvePreset == preset
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clickable { onApplyPreset(preset) }
                        .testTag("speed_preset_${preset.name}")
                ) {
                    Text(preset.label, style = MaterialTheme.typography.labelSmall, color = if (active) NeonCyan else palette.textSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        SpeedCurveGraph(points = clip.speedCurvePoints, onUpdatePoint = onUpdatePoint)
    }
}

@Composable
private fun TextAnimationDrawer(
    activeTextStyle: TextClipStyle,
    isAutoTracked: Boolean,
    onAddTextClip: () -> Unit,
    onAddSticker: (String) -> Unit,
    onUpdateStyle: ((TextClipStyle) -> TextClipStyle) -> Unit,
    onAutoTrackSelectedOverlay: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    var animSubTab by remember { mutableIntStateOf(0) } // 0 = In, 1 = Out, 2 = Loop

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Text & Auto-Tracking", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isAutoTracked) NeonCyan.copy(alpha = 0.24f) else ProGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, if (isAutoTracked) NeonCyan else ProGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAutoTrackSelectedOverlay()
                    }
                    .testTag("auto_track_subject_button")
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = "Auto-track face or object",
                        tint = if (isAutoTracked) NeonCyan else ProGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (isAutoTracked) "Tracked ✓" else "AI Track",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAutoTracked) NeonCyan else ProGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricPurple.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier
                    .clickable(onClick = onAddTextClip)
                    .testTag("add_text_clip_button")
            ) {
                Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Add Text", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }
    }

    OutlinedTextField(
        value = activeTextStyle.text,
        onValueChange = { txt -> onUpdateStyle { it.copy(text = txt) } },
        singleLine = true,
        placeholder = { Text("Enter text (English / हिन्दी)…") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.accentSecondary,
            unfocusedBorderColor = palette.border,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("caption_text_input")
    )

    // Animated Stickers Quick Strip
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        item {
            Text("Stickers:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
        items(listOf("🔥", "✨", "⚡", "🎬", "❤️", "🚀", "🎵", "👑")) { emoji ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clickable { onAddSticker(emoji) }
                    .testTag("sticker_btn_$emoji")
            ) {
                Text(emoji, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
            }
        }
    }

    // In / Out / Loop Animation Selector Tabs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Text Motion", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(palette.background)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("In", "Out", "Loop").forEachIndexed { idx, label ->
                val active = animSubTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) ElectricPurple else Color.Transparent)
                        .clickable { animSubTab = idx }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("text_anim_subtab_$label")
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else palette.textSecondary)
                }
            }
        }
    }

    when (animSubTab) {
        0 -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TextAnimationType.entries) { anim ->
                val active = activeTextStyle.animationType == anim
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) NeonCyan.copy(alpha = 0.22f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                    modifier = Modifier
                        .clickable { onUpdateStyle { it.copy(animationType = anim) } }
                        .testTag("text_anim_${anim.name}")
                ) {
                    Text(anim.label, style = MaterialTheme.typography.labelSmall, color = if (active) NeonCyan else palette.textSecondary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
        }
        1 -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TextAnimOut.entries) { anim ->
                val active = activeTextStyle.outAnimation == anim
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) ProGold.copy(alpha = 0.22f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                    modifier = Modifier
                        .clickable { onUpdateStyle { it.copy(outAnimation = anim) } }
                        .testTag("text_out_${anim.name}")
                ) {
                    Text(anim.label, style = MaterialTheme.typography.labelSmall, color = if (active) ProGold else palette.textSecondary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
        }
        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TextAnimLoop.entries) { anim ->
                val active = activeTextStyle.loopAnimation == anim
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) MagentaSpark.copy(alpha = 0.22f) else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (active) MagentaSpark else palette.border),
                    modifier = Modifier
                        .clickable { onUpdateStyle { it.copy(loopAnimation = anim) } }
                        .testTag("text_loop_${anim.name}")
                ) {
                    Text(anim.label, style = MaterialTheme.typography.labelSmall, color = if (active) MagentaSpark else palette.textSecondary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
        }
    }

    var selectedFontCategory by remember { mutableStateOf("All") }
    val fontCategories = remember { listOf("All", "Cinema", "Reel", "Hindi", "Bold", "Serif", "Script", "Gaming") }
    val filteredFonts = remember(selectedFontCategory) {
        if (selectedFontCategory == "All") TextFontStyle.entries
        else TextFontStyle.entries.filter { it.category == selectedFontCategory }
    }

    Text("Pro Font Studio (${TextFontStyle.entries.size} Unlocked Fonts)", style = MaterialTheme.typography.labelSmall, color = ProGold)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(fontCategories) { cat ->
            val active = selectedFontCategory == cat
            val label = if (cat == "All") "All (${TextFontStyle.entries.size})" else cat
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (active) ProGold.copy(alpha = 0.22f) else palette.background,
                border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { selectedFontCategory = cat }
                    .testTag("font_category_$cat")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) ProGold else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(filteredFonts) { font ->
            val active = activeTextStyle.fontStyle == font
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) ElectricPurple.copy(alpha = 0.28f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                modifier = Modifier
                    .clickable { onUpdateStyle { it.copy(fontStyle = font) } }
                    .testTag("text_font_${font.name}")
            ) {
                Text(
                    text = font.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }

    Text("Pro Text Styling (Neon, 3D, Curve & Multi-Shadow)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(TextEffectStyle.entries) { fx ->
            val active = activeTextStyle.effectStyle == fx
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) MagentaSpark.copy(alpha = 0.22f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) MagentaSpark else palette.border),
                modifier = Modifier
                    .clickable { onUpdateStyle { it.copy(effectStyle = fx) } }
                    .testTag("text_effect_${fx.name}")
            ) {
                Text(fx.label, style = MaterialTheme.typography.labelSmall, color = if (active) palette.textPrimary else palette.textSecondary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(0xFF00F0FF, 0xFF9B5CFF, 0xFFFFD84D, 0xFFFF2E93, 0xFFA3FF12, 0xFFFFFFFF).forEach { hex ->
            val selected = activeTextStyle.accentColorHex == hex
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else palette.border, CircleShape)
                    .clickable { onUpdateStyle { it.copy(accentColorHex = hex) } }
            )
        }
    }

    CompactProSlider("Caption Size", activeTextStyle.fontSizeSp, 16f..54f, "${activeTextStyle.fontSizeSp.roundToInt()}sp") { v ->
        onUpdateStyle { it.copy(fontSizeSp = v) }
    }
    CompactProSlider("Anim Speed", activeTextStyle.animSpeed, 0.4f..2.5f, String.format(Locale.US, "%.1fx", activeTextStyle.animSpeed)) { v ->
        onUpdateStyle { it.copy(animSpeed = v) }
    }
    if (activeTextStyle.effectStyle == TextEffectStyle.CURVED_ARC) {
        CompactProSlider("Curve Arc", activeTextStyle.curveAmount, -1f..1f, formatSignedPct(activeTextStyle.curveAmount)) { v ->
            onUpdateStyle { it.copy(curveAmount = v) }
        }
    }
}

@Composable
private fun VisualFxDrawer(
    fx: VisualFxSettings,
    onSelectFx: (VisualFxType) -> Unit,
    onUpdateFx: ((VisualFxSettings) -> VisualFxSettings) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = MagentaSpark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Real-Time Canvas Visual FX (${VisualFxType.entries.size})", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VisualFxType.entries) { effect ->
            val active = fx.activeFx == effect
            val accent = Color(effect.accentHex)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) accent.copy(alpha = 0.26f) else palette.surfaceElevated,
                border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else palette.border),
                modifier = Modifier
                    .clickable { onSelectFx(effect) }
                    .testTag("visual_fx_${effect.name}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Brush.linearGradient(listOf(accent, Color.White))))
                    Spacer(Modifier.height(5.dp))
                    Text(effect.label, style = MaterialTheme.typography.labelSmall, color = if (active) palette.textPrimary else palette.textSecondary, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }

    CompactProSlider("FX Intensity", fx.intensity, 0f..1f, "${(fx.intensity * 100).roundToInt()}%") { v -> onUpdateFx { it.copy(intensity = v) } }
    CompactProSlider("FX Pulse Speed", fx.speed, 0.2f..2.5f, String.format(Locale.US, "%.1fx", fx.speed)) { v -> onUpdateFx { it.copy(speed = v) } }
    CompactProSlider("Velocity Motion Blur", fx.motionBlur, 0f..1f, "${(fx.motionBlur * 100).roundToInt()}%") { v -> onUpdateFx { it.copy(motionBlur = v) } }
}

private fun formatSignedPct(v: Float): String {
    val pct = (v * 100f).roundToInt()
    return if (pct > 0) "+$pct%" else "$pct%"
}
