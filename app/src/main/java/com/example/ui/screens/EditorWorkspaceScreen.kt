package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.media.EvaluatedTransform
import com.example.model.EditorToolTab
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.TrackType
import com.example.ui.components.AiLabMode
import com.example.ui.components.AiVideoLabDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.InteractivePreviewCanvas
import com.example.ui.components.NetworkStatusPill
import com.example.ui.components.ProToolDrawerContent
import com.example.ui.components.TimelinePanel
import com.example.ui.components.springPressClickable
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private enum class PendingVisualPick { MAIN, PIP, CUTOUT_BG, CANVAS_BG }

@Composable
fun EditorWorkspaceScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val spectrumLevels by viewModel.spectrumLevels.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()
    val viralKitState by viewModel.viralKitState.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val document = editorState.document ?: return

    val haptic = LocalHapticFeedback.current
    val uiScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportDialog by remember { mutableStateOf(false) }
    var showResMenu by remember { mutableStateOf(false) }
    var pendingPick by remember { mutableStateOf(PendingVisualPick.MAIN) }
    var aiLabDialogMode by remember { mutableStateOf<AiLabMode?>(null) }
    var captionDraftScript by remember {
        mutableStateOf("Stop scrolling watch this cinematic transition glow in 4K 60fps")
    }

    // Multi-file zero-permission Android Photo Picker for Videos & Photos (Main & PIP)
    val multiVisualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            when (pendingPick) {
                PendingVisualPick.MAIN -> viewModel.importMultipleLocalMedia(uris, TrackType.MAIN_VIDEO)
                PendingVisualPick.PIP -> viewModel.importMultipleLocalMedia(uris, TrackType.OVERLAY_PIP)
                PendingVisualPick.CUTOUT_BG -> viewModel.setCutoutCustomBackground(uris.first())
                PendingVisualPick.CANVAS_BG -> viewModel.setCanvasBackgroundCustomImage(uris.first())
            }
        }
    }

    // Single-file picker for Cutout or Canvas custom background image
    val singleImageBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (pendingPick == PendingVisualPick.CANVAS_BG) {
                viewModel.setCanvasBackgroundCustomImage(uri)
            } else {
                viewModel.setCutoutCustomBackground(uri)
            }
        }
    }

    // Multi-file SAF Document Picker for Local Audio
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importLocalAudioMedia(uris)
        }
    }

    // Multi-file SAF Document Picker for any Video, Image, or Audio from phone storage
    val allMediaDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleLocalMedia(uris, TrackType.MAIN_VIDEO)
        }
    }

    // Built-in Android Speech Recognition Intent for Auto-Captions dictation (English / Hindi)
    val speechDictationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                captionDraftScript = recognized
                uiScope.launch { snackbarHostState.showSnackbar("Transcribed: \"$recognized\"") }
            }
        }
    }

    BackHandler {
        if (editorState.isFullscreenPreview) viewModel.toggleFullscreenPreview()
        else viewModel.closeEditor()
    }

    LaunchedEffect(editorState.toastMessage) {
        val msg = editorState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(editorState.openExportOnLaunch) {
        if (editorState.openExportOnLaunch) {
            viewModel.resetViralKitState()
            showExportDialog = true
            viewModel.consumeOpenExportOnLaunch()
        }
    }

    val selectedClip = remember(document.clips, editorState.selectedClipId) {
        document.clips.firstOrNull { it.id == editorState.selectedClipId }
            ?: document.clips.firstOrNull { it.trackType == TrackType.MAIN_VIDEO }
    }

    val evaluatedTransform = remember(selectedClip, editorState.playheadMs) {
        if (selectedClip != null) {
            val localMs = (editorState.playheadMs - selectedClip.startMs).coerceIn(0L, selectedClip.durationMs)
            viewModel.canvasRenderer.evaluateKeyframes(selectedClip.keyframes, localMs)
        } else EvaluatedTransform()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Spacebar && !event.isCtrlPressed -> {
                        viewModel.togglePlayback()
                        true
                    }
                    event.key == Key.DirectionLeft -> {
                        viewModel.seekTo((editorState.playheadMs - 1000L).coerceAtLeast(0L))
                        true
                    }
                    event.key == Key.DirectionRight -> {
                        viewModel.seekTo(editorState.playheadMs + 1000L)
                        true
                    }
                    event.key == Key.Z && event.isCtrlPressed && event.isShiftPressed -> {
                        if (editorState.canRedo) viewModel.redo()
                        true
                    }
                    event.key == Key.Z && event.isCtrlPressed -> {
                        if (editorState.canUndo) viewModel.undo()
                        true
                    }
                    event.key == Key.Y && event.isCtrlPressed -> {
                        if (editorState.canRedo) viewModel.redo()
                        true
                    }
                    event.key == Key.S && event.isCtrlPressed -> {
                        viewModel.splitSelectedClipAtPlayhead()
                        true
                    }
                    event.key == Key.M && event.isCtrlPressed -> {
                        viewModel.toggleAudioMute()
                        true
                    }
                    event.key == Key.F && event.isCtrlPressed -> {
                        viewModel.toggleFullscreenPreview()
                        true
                    }
                    else -> false
                }
            }
    ) {
        val isWideStudioLayout = maxWidth >= 760.dp

        val renderToolDrawer: @Composable () -> Unit = {
            ProToolDrawerContent(
                activeTab = editorState.activeToolTab,
                document = document,
                selectedClip = selectedClip,
                activeTransitionClipId = editorState.activeTransitionClipId,
                evaluatedTransform = evaluatedTransform,
                spectrumLevels = spectrumLevels,
                isAnalyzingBeats = editorState.isAnalyzingBeats,
                captionDraftScript = captionDraftScript,
                onCaptionDraftScriptChange = { captionDraftScript = it },
                onLaunchVoiceDictation = { localeTag ->
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, if (localeTag == "hi-IN") "अपने रील कैप्शन बोलें…" else "Speak your Reel captions…")
                    }
                    val launched = runCatching { speechDictationLauncher.launch(intent) }.isSuccess
                    if (!launched) {
                        uiScope.launch {
                            snackbarHostState.showSnackbar("Voice service unavailable on this device — paste English or Hindi script below!")
                        }
                    }
                },
                onGenerateAutoCaptions = { script, style, words ->
                    viewModel.generateAutoCaptions(script, style, words)
                },
                onUpdateCutout = { viewModel.updateCutout(it) },
                onPickCutoutCustomBg = {
                    pendingPick = PendingVisualPick.CUTOUT_BG
                    singleImageBgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onDetectBeats = { viewModel.detectAudioBeats() },
                onSnapVideoToBeats = { viewModel.snapAndCutMainClipsToBeats() },
                onUpdateMotionBlur = { viewModel.updateMotionBlur(it) },
                onUpdateTransition = { type, dur -> viewModel.updateClipTransition(type, dur) },
                onUpdatePipClip = { viewModel.updateActivePipClip(it) },
                onImportMainVisual = {
                    pendingPick = PendingVisualPick.MAIN
                    multiVisualMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onImportPipVisual = {
                    pendingPick = PendingVisualPick.PIP
                    multiVisualMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onBrowseAllFiles = {
                    allMediaDocumentPicker.launch(arrayOf("video/*", "image/*", "audio/*"))
                },
                onUpdateAudioEnhance = { viewModel.updateAudioEnhance(it) },
                onAddVolumeKeyframe = { viewModel.addVolumeKeyframeAtPlayhead(it) },
                onClearVolumeKeyframes = { viewModel.clearVolumeKeyframes() },
                onSelectLut = { viewModel.selectLutPreset(it) },
                onUpdateColorGrading = { viewModel.updateColorGrading(it) },
                onResetColorGrading = { viewModel.resetColorGrading() },
                onAddOrUpdateKeyframe = { s, x, y, r, o, e -> viewModel.addOrUpdateKeyframeAtPlayhead(s, x, y, r, o, e) },
                onRemoveKeyframe = { viewModel.removeKeyframeAtPlayhead() },
                onJumpKeyframe = { viewModel.jumpToAdjacentKeyframe(it) },
                onUpdateSpeed = { viewModel.updateSelectedClipSpeed(it) },
                onApplySpeedPreset = { viewModel.applySpeedCurvePreset(it) },
                onUpdateSpeedCurvePoint = { idx, spd -> viewModel.updateSpeedCurvePoint(idx, spd) },
                onAddTextClip = { viewModel.addTextClipAtPlayhead() },
                onAddSticker = { emoji -> viewModel.addStickerAtPlayhead(emoji) },
                onUpdateTextStyle = { viewModel.updateSelectedTextStyle(it) },
                onSelectVisualFx = { viewModel.selectVisualFx(it) },
                onUpdateVisualFx = { viewModel.updateVisualFxSettings(it) },
                onOpenAiLabMode = { mode ->
                    viewModel.resetAiTaskState()
                    aiLabDialogMode = mode
                },
                onUpdateClipAnimation = { aIn, aOut, aCombo, dur ->
                    viewModel.updateSelectedClipAnimation(aIn, aOut, aCombo, dur)
                },
                onFreezeFrame = { viewModel.insertFreezeFrameAtPlayhead() },
                onToggleReverse = { viewModel.toggleReverseSelectedClip() },
                onRotate90 = { viewModel.rotateSelectedClip90() },
                onToggleFlipH = { viewModel.toggleFlipHorizontalSelectedClip() },
                onToggleFlipV = { viewModel.toggleFlipVerticalSelectedClip() },
                onUpdateCropZoom = { viewModel.updateSelectedClipCropZoom(it) },
                onUpdateCanvasBg = { viewModel.updateCanvasBackground(it) },
                onPickCustomCanvasBg = {
                    pendingPick = PendingVisualPick.CANVAS_BG
                    singleImageBgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onUpdateMusicVisualizer = { viewModel.updateMusicVisualizer(it) }
            )
        }

        val renderToolStrip: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.glassSurface)
                    .border(width = 0.7.dp, color = palette.border)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct Quick-Split Action on the Bottom Tool Ribbon (CapCut / VN style)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .springPressClickable(
                            pressedScale = 0.90f,
                            hapticType = HapticFeedbackType.LongPress
                        ) {
                            viewModel.splitSelectedClipAtPlayhead()
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("ribbon_quick_split_button"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = stringResource(R.string.cd_split_clip),
                        tint = NeonCyan,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Split",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                EditorToolTab.entries.forEach { tab ->
                    val active = (isWideStudioLayout || editorState.isToolDrawerOpen) && editorState.activeToolTab == tab
                    val icon = when (tab) {
                        EditorToolTab.AI_LAB -> Icons.Filled.AutoAwesome
                        EditorToolTab.MEDIA -> Icons.Filled.AddPhotoAlternate
                        EditorToolTab.ANIMATE -> Icons.Filled.Animation
                        EditorToolTab.CANVAS -> Icons.Filled.AspectRatio
                        EditorToolTab.CAPTIONS -> Icons.Filled.Subtitles
                        EditorToolTab.CUTOUT -> Icons.Filled.Colorize
                        EditorToolTab.TRANSITION -> Icons.Filled.SwapHoriz
                        EditorToolTab.PIP_MASK -> Icons.Filled.Layers
                        EditorToolTab.COLOR -> Icons.Filled.Palette
                        EditorToolTab.SPEED -> Icons.Filled.Speed
                        EditorToolTab.AUDIO -> Icons.Filled.GraphicEq
                        EditorToolTab.KEYFRAME -> Icons.Filled.Diamond
                        EditorToolTab.TEXT -> Icons.Filled.TextFields
                        EditorToolTab.FX -> Icons.Filled.AutoFixHigh
                    }
                    val displayLabel = when (tab) {
                        EditorToolTab.AI_LAB -> "AI Tools"
                        EditorToolTab.COLOR -> "Filter"
                        EditorToolTab.FX -> "Effects"
                        EditorToolTab.CUTOUT -> "Chroma"
                        else -> tab.label
                    }
                    val isProHighlight = tab in setOf(
                        EditorToolTab.AI_LAB,
                        EditorToolTab.CAPTIONS,
                        EditorToolTab.CUTOUT
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) palette.accentPrimary.copy(alpha = 0.24f) else Color.Transparent)
                            .border(
                                width = if (active) 1.dp else 0.dp,
                                color = if (active) palette.accentSecondary.copy(alpha = 0.65f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .springPressClickable(pressedScale = 0.90f) {
                                viewModel.selectToolTab(tab)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("editor_tool_tab_${tab.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = displayLabel,
                            tint = when {
                                active -> palette.accentSecondary
                                isProHighlight -> ProGold
                                else -> palette.textSecondary
                            },
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) palette.textPrimary else palette.textSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Flagship Studio Top Bar with PRO UNLOCKED Gold Badge + Theme Toggle
            if (!editorState.isFullscreenPreview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.closeEditor() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("editor_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = palette.textPrimary)
                        }

                        // Crisp Resolution / FPS Glass Pill (e.g. "4K • 60fps")
                        Box {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = palette.surfaceElevated,
                                border = BorderStroke(1.dp, palette.glassBorderBrush),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { showResMenu = true }
                                    .testTag("editor_resolution_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = ProGold, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${document.resolution.label} • ${document.fps.fps}fps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showResMenu,
                                onDismissRequest = { showResMenu = false },
                                containerColor = palette.surfaceElevated
                            ) {
                                ExportResolution.entries.forEach { res ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${res.detailLabel} (Unlocked)",
                                                color = if (document.resolution == res) ProGold else palette.textPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateResolution(res)
                                            showResMenu = false
                                        }
                                    )
                                }
                                ExportFps.entries.forEach { fps ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Frame Rate: ${fps.label}",
                                                color = if (document.fps == fps) palette.accentSecondary else palette.textPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateFps(fps)
                                            showResMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = stringResource(R.string.editor_auto_saved),
                            tint = if (editorState.isAutoSaving) palette.accentSecondary else AcidLime.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWideStudioLayout || networkStatus.isOffline || networkStatus.useLiteOptimization) {
                            NetworkStatusPill(
                                networkStatus = networkStatus,
                                onToggleLowDataMode = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleLowDataMode()
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = editorState.canUndo,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_undo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.cd_undo),
                                tint = if (editorState.canUndo) palette.textPrimary else palette.textMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = editorState.canRedo,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_redo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = stringResource(R.string.cd_redo),
                                tint = if (editorState.canRedo) palette.textPrimary else palette.textMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))

                        // Bold Glowing Export CTA Pill Button
                        Box(
                            modifier = Modifier
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                NeonCyan.copy(alpha = 0.42f),
                                                ElectricPurple.copy(alpha = 0.22f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width * 0.72f
                                        )
                                    )
                                }
                                .height(35.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(NeonStudioGradient)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                                .springPressClickable(
                                    pressedScale = 0.94f,
                                    hapticType = HapticFeedbackType.LongPress
                                ) {
                                    viewModel.resetViralKitState()
                                    showExportDialog = true
                                }
                                .padding(horizontal = 14.dp)
                                .testTag("editor_export_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelLarge,
                                color = OledBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Adaptive Workspace Body: Side-by-Side Two-Pane on Tablets/Laptops/PCs, Vertical Stack on Phones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Center Live 60fps Multi-Touch Preview Canvas + Safe-Zone Guides
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(palette.background)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                InteractivePreviewCanvas(
                    document = document,
                    playheadMs = editorState.playheadMs,
                    frameRevision = editorState.frameRevision,
                    comparingOriginal = editorState.isComparingOriginal,
                    canvasRenderer = viewModel.canvasRenderer,
                    frameEngine = viewModel.frameEngine,
                    onTogglePlayback = { viewModel.togglePlayback() },
                    onSetComparingOriginal = { viewModel.setComparingOriginal(it) },
                    onImportEmptyCanvasMedia = {
                        pendingPick = PendingVisualPick.MAIN
                        multiVisualMediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onClipMultiTouchTransform = { zoomDelta, dxNorm, dyNorm ->
                        viewModel.applyClipMultiTouchGesture(zoomDelta, dxNorm, dyNorm)
                    },
                    onResetClipTransform = { viewModel.resetActiveClipTransform() },
                    isPlaying = editorState.isPlaying,
                    isFullscreen = editorState.isFullscreenPreview,
                    onToggleFullscreen = { viewModel.toggleFullscreenPreview() },
                    safeZonePreset = editorState.safeZonePreset,
                    onCycleSafeZone = { viewModel.cycleSafeZonePreset() }
                )
            }

            // 3. Transport Bar + Timecode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatTimecode(editorState.playheadMs)} / ${formatTimecode(document.totalDurationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.testTag("editor_timecode_text")
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.seekTo((editorState.playheadMs - 1000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Rewind 1 second", tint = palette.textSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonStudioGradient)
                            .clickable { viewModel.togglePlayback() }
                            .testTag("editor_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (editorState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = OledBlack,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.seekTo(editorState.playheadMs + 1000L) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Forward 1 second", tint = palette.textSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleAudioMute() },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_mute_button")
                    ) {
                        Icon(
                            imageVector = if (document.audioEnhance.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_mute_audio),
                            tint = if (document.audioEnhance.isMuted) palette.textMuted else palette.accentSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFullscreenPreview() },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (editorState.isFullscreenPreview) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = stringResource(R.string.cd_fullscreen),
                            tint = palette.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

                        // 4. Multi-Track Timeline (Full-Width in both Compact & Wide Studio)
                        if (!editorState.isFullscreenPreview) {
                            TimelinePanel(
                                document = document,
                                frameEngine = viewModel.frameEngine,
                                selectedClipId = editorState.selectedClipId,
                                playheadMs = editorState.playheadMs,
                                zoom = editorState.timelineZoom,
                                onSeek = { viewModel.seekTo(it) },
                                onSelectClip = { viewModel.selectClip(it) },
                                onSplitClip = { viewModel.splitSelectedClipAtPlayhead() },
                                onDuplicateClip = { viewModel.duplicateSelectedClip() },
                                onDeleteClip = { viewModel.deleteSelectedClip() },
                                onDetectBeats = { viewModel.detectAudioBeats() },
                                onClickTransitionNode = { clipId -> viewModel.openTransitionPickerForClip(clipId) },
                                onTrimClip = { id, dStart, dDur -> viewModel.trimClip(id, dStart, dDur) },
                                onMoveClip = { id, dMs -> viewModel.moveClipStart(id, dMs) },
                                onZoomChange = { viewModel.setTimelineZoom(it) },
                                onImportVisual = {
                                    pendingPick = PendingVisualPick.MAIN
                                    multiVisualMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                },
                                onImportAudio = {
                                    audioPicker.launch(arrayOf("audio/*"))
                                }
                            )

                            if (!isWideStudioLayout) {
                                AnimatedVisibility(visible = editorState.isToolDrawerOpen) {
                                    renderToolDrawer()
                                }
                                renderToolStrip()
                            }
                        }
                    }

                // Pro Desktop / Tablet / Foldable Right Inspector Pane
                if (isWideStudioLayout && !editorState.isFullscreenPreview) {
                    Column(
                        modifier = Modifier
                            .width(356.dp)
                            .fillMaxHeight()
                            .background(palette.surface)
                            .border(width = 0.5.dp, color = palette.border)
                    ) {
                        renderToolStrip()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            renderToolDrawer()
                        }
                        Surface(
                            color = palette.surfaceElevated,
                            border = BorderStroke(0.5.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Shortcuts: Space Play • ←/→ Seek • Ctrl+S Split • Ctrl+Z Undo",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )
    }

    if (showExportDialog) {
        ExportDialog(
            document = document,
            exportState = exportState,
            viralKitState = viralKitState,
            networkStatus = networkStatus,
            onToggleLowDataMode = { viewModel.toggleLowDataMode() },
            onGenerateViralKit = { lang -> viewModel.generateExportViralKit(lang) },
            onSelectResolution = { viewModel.updateResolution(it) },
            onSelectFps = { viewModel.updateFps(it) },
            onSelectBitrate = { viewModel.updateBitrate(it) },
            onStartExport = { viewModel.startMp4Export() },
            onSaveToDeviceUri = { uri, onDone -> viewModel.saveExportedFileToUri(uri, onDone) },
            onDismiss = {
                viewModel.cancelOrResetExport()
                showExportDialog = false
            }
        )
    }

    aiLabDialogMode?.let { mode ->
        AiVideoLabDialog(
            initialMode = mode,
            defaultAspectRatio = document.aspectRatio,
            aiTaskState = aiTaskState,
            networkStatus = networkStatus,
            onToggleLowDataMode = { viewModel.toggleLowDataMode() },
            onResetTaskState = { viewModel.resetAiTaskState() },
            onRunTextToImage = { p, s, r -> viewModel.runAiTextToImage(p, s, r) },
            onRunTextToVideo = { p, s, m, r -> viewModel.runAiTextToVideo(p, s, m, r) },
            onRunScriptToVideo = { sc, lang, vis, r -> viewModel.runAiScriptToVideo(sc, lang, vis, r) },
            onRunPhotoAnimate = { uri, m, r -> viewModel.runImageToVideoAnimator(uri, m, r) },
            onRunLyricsVisualizer = { txt, ai, lang, viz, r -> viewModel.runAiLyricsVisualizer(txt, ai, lang, viz, r) },
            onRunTalkingOrPromo = { brief, talk, uri, lang, r -> viewModel.runAiTalkingPhotoOrPromo(brief, talk, uri, lang, r) },
            onDismiss = { aiLabDialogMode = null }
        )
    }
}

private fun formatTimecode(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val mins = (clamped / 60_000L)
    val secs = (clamped % 60_000L) / 1000L
    val centis = (clamped % 1000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, centis)
}
