package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCameraMotion
import com.example.ai.AiImageStyle
import com.example.ai.AiTaskState
import com.example.ai.AiVideoLabService
import com.example.ai.ViralKitResult
import com.example.data.ProjectSummary
import com.example.data.StudioDatabase
import com.example.data.StudioRepository
import com.example.media.BeatSyncAnalyzer
import com.example.media.ExportProgressState
import com.example.media.MediaFrameEngine
import com.example.media.Mp4Exporter
import com.example.media.StudioAudioEngine
import com.example.media.VideoCanvasRenderer
import com.example.model.AspectRatioPreset
import com.example.model.AudioEnhanceSettings
import com.example.model.AutoCaptionStyle
import com.example.model.CanvasBackgroundSettings
import com.example.model.CanvasBgMode
import com.example.model.ClipTransition
import com.example.model.ColorGrading
import com.example.model.CreatorInsightsData
import com.example.model.CutoutSettings
import com.example.model.EditorToolTab
import com.example.model.ExportBitrate
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.Keyframe
import com.example.model.KeyframeEasing
import com.example.model.LutPreset
import com.example.model.MusicVisualizerStyle
import com.example.model.SafeZonePreset
import com.example.model.SpeedCurvePreset
import com.example.model.StoryboardScene
import com.example.model.StudioCatalog
import com.example.model.TextAnimLoop
import com.example.model.TextAnimOut
import com.example.model.TextAnimationType
import com.example.model.TextClipStyle
import com.example.model.TextEffectStyle
import com.example.model.TextFontStyle
import com.example.model.TimelineClip
import com.example.model.TrackType
import com.example.model.TransitionType
import com.example.model.TrendingAudioSuggestion
import com.example.model.VideoAnimCombo
import com.example.model.VideoAnimIn
import com.example.model.VideoAnimOut
import com.example.model.VideoProjectDocument
import com.example.model.VideoTemplate
import com.example.model.VisualFxSettings
import com.example.model.VisualFxType
import com.example.model.VolumeKeyframe
import com.example.model.defaultCurvePoints
import com.example.network.NetworkMonitor
import com.example.network.NetworkStatus
import com.example.ui.theme.StudioThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

enum class HomeTab(val label: String) {
    PROJECTS("Projects"),
    TEMPLATES("Templates"),
    AI_LAB("AI Lab"),
    INSIGHTS("Insights")
}

data class EditorUiState(
    val document: VideoProjectDocument? = null,
    val selectedClipId: String? = null,
    val activeTransitionClipId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isFullscreenPreview: Boolean = false,
    val isComparingOriginal: Boolean = false,
    val isAnalyzingBeats: Boolean = false,
    val safeZonePreset: SafeZonePreset = SafeZonePreset.OFF,
    val timelineZoom: Float = 1.0f,
    val activeToolTab: EditorToolTab = EditorToolTab.COLOR,
    val isToolDrawerOpen: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isAutoSaving: Boolean = false,
    val lastSavedTimeMs: Long = 0L,
    val frameRevision: Int = 0,
    val openExportOnLaunch: Boolean = false,
    val toastMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("nv_edit_prefs", Context.MODE_PRIVATE)
    private val database = StudioDatabase.getInstance(application)
    private val repository = StudioRepository(database.projectDao(), database.storyboardDao())

    val frameEngine = MediaFrameEngine(application)
    val canvasRenderer = VideoCanvasRenderer(application)
    val audioEngine = StudioAudioEngine(application)
    private val aiService = AiVideoLabService(application)
    private val beatSyncAnalyzer = BeatSyncAnalyzer(application)
    private val exporter = Mp4Exporter(application, canvasRenderer, frameEngine)
    private val networkMonitor = NetworkMonitor(
        context = application,
        initialManualLowData = prefs.getBoolean("low_data_mode", false)
    )
    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

    val projects: StateFlow<List<ProjectSummary>> = repository.projectsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val storyboards: StateFlow<List<StoryboardScene>> = repository.storyboardsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val spectrumLevels: StateFlow<List<Float>> = audioEngine.spectrumLevels

    private val _themeMode = MutableStateFlow(
        runCatching {
            StudioThemeMode.valueOf(prefs.getString("studio_theme_mode", StudioThemeMode.OLED_DARK.name) ?: StudioThemeMode.OLED_DARK.name)
        }.getOrDefault(StudioThemeMode.OLED_DARK)
    )
    val themeMode: StateFlow<StudioThemeMode> = _themeMode.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeTab.PROJECTS)
    val selectedHomeTab: StateFlow<HomeTab> = _selectedHomeTab.asStateFlow()

    private val _insightsPeriod = MutableStateFlow("7D")
    val insightsPeriod: StateFlow<String> = _insightsPeriod.asStateFlow()

    private val _insightsData = MutableStateFlow(StudioCatalog.insightsForPeriod("7D"))
    val insightsData: StateFlow<CreatorInsightsData> = _insightsData.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _exportState = MutableStateFlow(ExportProgressState())
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    private val _aiTaskState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val aiTaskState: StateFlow<AiTaskState<String>> = _aiTaskState.asStateFlow()

    private val _viralKitState = MutableStateFlow<AiTaskState<ViralKitResult>>(AiTaskState.Idle)
    val viralKitState: StateFlow<AiTaskState<ViralKitResult>> = _viralKitState.asStateFlow()

    private val undoStack = ArrayDeque<VideoProjectDocument>()
    private val redoStack = ArrayDeque<VideoProjectDocument>()
    private var playbackJob: Job? = null
    private var autoSaveJob: Job? = null
    private var exportJob: Job? = null
    private var aiJob: Job? = null
    private var viralKitJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun cycleThemeMode() {
        setThemeMode(_themeMode.value.next())
    }

    fun toggleLowDataMode() {
        val next = !networkStatus.value.manualLowDataMode
        prefs.edit().putBoolean("low_data_mode", next).apply()
        networkMonitor.setManualLowDataMode(next)
        val msg = if (next) {
            "Low-Network Lite Mode ON (Fast Gemini Lite + Auto-Retry)"
        } else {
            "Standard High-Speed AI Mode active"
        }
        _editorState.update { it.copy(toastMessage = msg) }
    }

    fun setThemeMode(mode: StudioThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("studio_theme_mode", mode.name).apply()
    }

    fun cycleSafeZonePreset() {
        _editorState.update { it.copy(safeZonePreset = it.safeZonePreset.next()) }
    }

    fun selectHomeTab(tab: HomeTab) {
        _selectedHomeTab.value = tab
    }

    fun selectInsightsPeriod(period: String) {
        _insightsPeriod.value = period
        _insightsData.value = StudioCatalog.insightsForPeriod(period)
    }

    // ---- Project Lifecycle ----

    fun createNewProject(
        title: String,
        aspectRatio: AspectRatioPreset,
        resolution: ExportResolution = ExportResolution.RES_4K,
        fps: ExportFps = ExportFps.FPS_60,
        initialMediaUris: List<Uri> = emptyList()
    ) {
        val cleanTitle = title.trim().ifBlank { "NV Edit ${(10..99).random()}" }
        viewModelScope.launch {
            val importedClips = mutableListOf<TimelineClip>()
            var videoCursorMs = 0L
            var audioCursorMs = 0L
            var firstVisualUri: String? = null

            kotlinx.coroutines.withContext(Dispatchers.IO) {
                initialMediaUris.forEach { rawUri ->
                    runCatching {
                        getApplication<Application>().contentResolver.takePersistableUriPermission(
                            rawUri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val (localUri, originalName) = frameEngine.persistPickedMediaLocally(rawUri)
                    val info = frameEngine.inspectMedia(
                        uri = localUri,
                        fallbackVideo = true,
                        fallbackAudio = false,
                        knownDisplayName = originalName
                    )
                    if (info.isVideo) {
                        frameEngine.prefetchVideoFrame(info.uriString, 0L)
                    }
                    if (info.isAudio) {
                        importedClips += TimelineClip(
                            trackType = TrackType.AUDIO_MUSIC,
                            label = info.displayName,
                            startMs = audioCursorMs,
                            durationMs = info.durationMs,
                            sourceDurationMs = info.durationMs,
                            mediaUri = info.uriString,
                            isVideoMedia = false,
                            drawableKey = null,
                            accentColorHex = 0xFF10E896,
                            waveformPeaks = info.waveformPeaks
                        )
                        audioCursorMs += info.durationMs
                    } else {
                        if (firstVisualUri == null) firstVisualUri = info.uriString
                        importedClips += TimelineClip(
                            trackType = TrackType.MAIN_VIDEO,
                            label = info.displayName,
                            startMs = videoCursorMs,
                            durationMs = info.durationMs,
                            sourceDurationMs = info.durationMs,
                            mediaUri = info.uriString,
                            isVideoMedia = info.isVideo,
                            drawableKey = null,
                            accentColorHex = 0xFF9B5CFF,
                            waveformPeaks = info.waveformPeaks
                        )
                        videoCursorMs += info.durationMs
                    }
                }
            }

            val doc = VideoProjectDocument(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                aspectRatio = aspectRatio,
                resolution = resolution,
                fps = fps,
                bitrate = ExportBitrate.ULTRA_PRO,
                thumbnailDrawableKey = null,
                thumbnailUri = firstVisualUri,
                clips = importedClips
            )
            repository.saveProject(doc)
            openDocumentInEditor(
                doc = doc,
                initialToolTab = if (importedClips.isEmpty()) EditorToolTab.MEDIA else EditorToolTab.COLOR
            )
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle)
        }
    }

    fun openProjectById(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.loadProject(projectId) ?: return@launch
            openDocumentInEditor(loaded)
        }
    }

    fun openProjectForExport(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.loadProject(projectId) ?: return@launch
            openDocumentInEditor(loaded, openExportOnLaunch = true)
        }
    }

    fun consumeOpenExportOnLaunch() {
        _editorState.update { it.copy(openExportOnLaunch = false) }
    }

    fun openQuickToolInEditor(toolTab: EditorToolTab) {
        viewModelScope.launch {
            val latest = projects.value.maxByOrNull { it.updatedAtMs }
            if (latest != null) {
                val loaded = repository.loadProject(latest.id)
                if (loaded != null) {
                    openDocumentInEditor(loaded, initialToolTab = toolTab)
                    return@launch
                }
            }
            val doc = VideoProjectDocument(
                id = UUID.randomUUID().toString(),
                title = "Studio ${toolTab.label} Edit",
                aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                resolution = ExportResolution.RES_4K,
                fps = ExportFps.FPS_60,
                bitrate = ExportBitrate.ULTRA_PRO,
                thumbnailDrawableKey = null,
                thumbnailUri = null,
                clips = emptyList()
            )
            repository.saveProject(doc)
            openDocumentInEditor(doc, initialToolTab = toolTab)
        }
    }

    fun useTemplate(template: VideoTemplate) {
        val doc = template.createDocument()
        viewModelScope.launch {
            repository.saveProject(doc)
            openDocumentInEditor(doc)
        }
    }

    fun useTrendingAudioInNewProject(audio: TrendingAudioSuggestion) {
        val doc = VideoProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "${audio.title.take(18)} Edit",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            resolution = ExportResolution.RES_4K,
            fps = ExportFps.FPS_60,
            thumbnailDrawableKey = "tpl_velocity",
            colorGrading = ColorGrading(lutPreset = LutPreset.TOKYO_NEON, contrast = 0.14f),
            clips = listOf(
                TimelineClip(
                    trackType = TrackType.MAIN_VIDEO,
                    label = "Beat Visual",
                    startMs = 0L,
                    durationMs = audio.durationMs,
                    drawableKey = "tpl_velocity",
                    accentColorHex = audio.accentHex,
                    speedCurvePreset = SpeedCurvePreset.MONTAGE,
                    speedCurvePoints = defaultCurvePoints(SpeedCurvePreset.MONTAGE)
                ),
                TimelineClip(
                    trackType = TrackType.TEXT_STICKER,
                    label = audio.title,
                    startMs = 400L,
                    durationMs = 4500L,
                    accentColorHex = 0xFFFF9F1C,
                    textStyle = TextClipStyle(
                        text = audio.title.uppercase(),
                        accentColorHex = audio.accentHex
                    )
                ),
                TimelineClip(
                    trackType = TrackType.AUDIO_MUSIC,
                    label = "${audio.title} (${audio.bpm} BPM)",
                    startMs = 0L,
                    durationMs = audio.durationMs,
                    accentColorHex = 0xFF10E896,
                    waveformPeaks = StudioCatalog.generateWaveform(seed = audio.bpm)
                )
            )
        )
        viewModelScope.launch {
            repository.saveProject(doc)
            openDocumentInEditor(doc)
        }
    }

    fun convertStoryboardToProject() {
        val scenes = storyboards.value
        if (scenes.isEmpty()) return
        val drawables = listOf("tpl_beat_sync", "tpl_velocity", "tpl_cinematic_travel", "tpl_retro_vhs")
        var cursorMs = 0L
        val clips = mutableListOf<TimelineClip>()
        scenes.forEachIndexed { idx, scene ->
            val durMs = (scene.durationSec.coerceIn(2, 20) * 1000L)
            clips += TimelineClip(
                trackType = TrackType.MAIN_VIDEO,
                label = "${idx + 1}. ${scene.title.take(18)}",
                startMs = cursorMs,
                durationMs = durMs,
                drawableKey = drawables[idx % drawables.size],
                accentColorHex = if (idx % 2 == 0) 0xFF9B5CFF else 0xFF00D4FF,
                transitionIn = if (idx > 0) ClipTransition(TransitionType.WHIP_PAN_LEFT, 550L) else ClipTransition()
            )
            clips += TimelineClip(
                trackType = TrackType.TEXT_STICKER,
                label = scene.cameraAngle,
                startMs = cursorMs + 250L,
                durationMs = (durMs - 500L).coerceAtLeast(1200L),
                accentColorHex = 0xFFFF9F1C,
                textStyle = TextClipStyle(text = scene.title.uppercase(), fontSizeSp = 26f)
            )
            cursorMs += durMs
        }
        clips += TimelineClip(
            trackType = TrackType.AUDIO_MUSIC,
            label = "Storyboard Score 124 BPM",
            startMs = 0L,
            durationMs = cursorMs.coerceAtLeast(4000L),
            accentColorHex = 0xFF10E896,
            waveformPeaks = StudioCatalog.generateWaveform(seed = 91)
        )
        val doc = VideoProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "Storyboard Sequence",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            resolution = ExportResolution.RES_4K,
            fps = ExportFps.FPS_60,
            thumbnailDrawableKey = drawables.first(),
            clips = clips
        )
        viewModelScope.launch {
            repository.saveProject(doc)
            openDocumentInEditor(doc)
        }
    }

    fun duplicateProject(id: String) {
        viewModelScope.launch { repository.duplicateProject(id) }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch { repository.deleteProject(id) }
    }

    // ---- Storyboard CRUD ----

    fun saveStoryboardScene(scene: StoryboardScene) {
        val currentCount = storyboards.value.size
        val ordered = if (storyboards.value.none { it.id == scene.id }) {
            scene.copy(orderIndex = currentCount)
        } else scene
        viewModelScope.launch { repository.upsertStoryboard(ordered) }
    }

    fun toggleStoryboardFilmed(scene: StoryboardScene) {
        viewModelScope.launch { repository.upsertStoryboard(scene.copy(isFilmed = !scene.isFilmed)) }
    }

    fun moveStoryboardScene(index: Int, delta: Int) {
        val list = storyboards.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        viewModelScope.launch { repository.saveStoryboardOrder(list) }
    }

    fun deleteStoryboardScene(id: String) {
        viewModelScope.launch { repository.deleteStoryboard(id) }
    }

    // ---- Editor Lifecycle & Undo/Redo + Auto-Save ----

    private fun openDocumentInEditor(
        doc: VideoProjectDocument,
        initialToolTab: EditorToolTab = if (doc.clips.isEmpty()) EditorToolTab.MEDIA else EditorToolTab.COLOR,
        openExportOnLaunch: Boolean = false
    ) {
        stopPlayback()
        undoStack.clear()
        redoStack.clear()
        _exportState.value = ExportProgressState()
        val mainClips = doc.clips.filter { it.trackType == TrackType.MAIN_VIDEO }.sortedBy { it.startMs }
        _editorState.value = EditorUiState(
            document = doc,
            selectedClipId = mainClips.firstOrNull()?.id ?: doc.clips.firstOrNull()?.id,
            activeTransitionClipId = mainClips.getOrNull(1)?.id,
            playheadMs = 0L,
            isPlaying = false,
            activeToolTab = initialToolTab,
            isToolDrawerOpen = true,
            canUndo = false,
            canRedo = false,
            lastSavedTimeMs = doc.updatedAtMs,
            openExportOnLaunch = openExportOnLaunch
        )
        prefetchFramesForCurrentPlayhead()
    }

    fun closeEditor() {
        stopPlayback()
        val doc = _editorState.value.document
        if (doc != null) {
            viewModelScope.launch { repository.saveProject(doc) }
        }
        _editorState.update { it.copy(document = null, isPlaying = false, isFullscreenPreview = false) }
    }

    private fun mutateDocument(
        recordUndo: Boolean = true,
        newSelectedClipId: String? = _editorState.value.selectedClipId,
        toast: String? = null,
        transform: (VideoProjectDocument) -> VideoProjectDocument
    ) {
        val current = _editorState.value.document ?: return
        val updated = transform(current)
        if (updated == current) return

        if (recordUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
        }

        val clampedPlayhead = _editorState.value.playheadMs.coerceIn(0L, updated.totalDurationMs)
        val validSelection = if (updated.clips.any { it.id == newSelectedClipId }) {
            newSelectedClipId
        } else {
            updated.clips.firstOrNull()?.id
        }

        _editorState.update {
            it.copy(
                document = updated,
                selectedClipId = validSelection,
                playheadMs = clampedPlayhead,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isAutoSaving = true,
                toastMessage = toast ?: it.toastMessage
            )
        }
        audioEngine.updateSettings(updated.audioEnhance, clampedPlayhead)
        scheduleAutoSave(updated)
    }

    private fun scheduleAutoSave(doc: VideoProjectDocument) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350L)
            repository.saveProject(doc)
            _editorState.update {
                it.copy(isAutoSaving = false, lastSavedTimeMs = System.currentTimeMillis())
            }
        }
    }

    fun undo() {
        val current = _editorState.value.document ?: return
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _editorState.update {
            it.copy(
                document = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isAutoSaving = true
            )
        }
        scheduleAutoSave(prev)
    }

    fun redo() {
        val current = _editorState.value.document ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _editorState.update {
            it.copy(
                document = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isAutoSaving = true
            )
        }
        scheduleAutoSave(next)
    }

    fun consumeToast() {
        _editorState.update { it.copy(toastMessage = null) }
    }

    fun setComparingOriginal(comparing: Boolean) {
        _editorState.update { it.copy(isComparingOriginal = comparing) }
    }

    // ---- Playback & Scrubbing ----

    fun togglePlayback() {
        if (_editorState.value.isPlaying) stopPlayback() else startPlayback()
    }

    private fun startPlayback() {
        val doc = _editorState.value.document ?: return
        if (doc.totalDurationMs <= 0L) {
            _editorState.update { it.copy(toastMessage = "Import a video, photo, or audio file first") }
            return
        }
        var startPos = _editorState.value.playheadMs
        if (startPos >= doc.totalDurationMs - 40L) startPos = 0L

        _editorState.update { it.copy(isPlaying = true, playheadMs = startPos) }
        audioEngine.startPlayback(doc, startPos)

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTickMs = System.currentTimeMillis()
            while (isActive && _editorState.value.isPlaying) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTickMs).coerceIn(1L, 50L)
                lastTickMs = now

                val state = _editorState.value
                val currentDoc = state.document ?: break
                val total = currentDoc.totalDurationMs
                var nextPos = state.playheadMs + dt
                if (nextPos >= total) {
                    nextPos = 0L
                    audioEngine.startPlayback(currentDoc, 0L)
                }

                _editorState.update { it.copy(playheadMs = nextPos) }
                audioEngine.syncPlayhead(currentDoc, nextPos, isPlaying = true)
                prefetchFramesForCurrentPlayhead()
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopPlayback()
        _editorState.update { it.copy(isPlaying = false) }
    }

    fun seekTo(ms: Long) {
        val doc = _editorState.value.document ?: return
        val clamped = ms.coerceIn(0L, doc.totalDurationMs)
        _editorState.update { it.copy(playheadMs = clamped) }
        audioEngine.syncPlayhead(doc, clamped, _editorState.value.isPlaying)
        prefetchFramesForCurrentPlayhead()
    }

    fun toggleFullscreenPreview() {
        _editorState.update { it.copy(isFullscreenPreview = !it.isFullscreenPreview) }
    }

    fun setTimelineZoom(zoom: Float) {
        _editorState.update { it.copy(timelineZoom = zoom.coerceIn(0.5f, 4.0f)) }
    }

    fun selectToolTab(tab: EditorToolTab) {
        _editorState.update { state ->
            if (state.activeToolTab == tab && state.isToolDrawerOpen) {
                state.copy(isToolDrawerOpen = false)
            } else {
                state.copy(activeToolTab = tab, isToolDrawerOpen = true)
            }
        }
    }

    fun selectClip(clipId: String?) {
        _editorState.update { it.copy(selectedClipId = clipId) }
    }

    fun openTransitionPickerForClip(clipId: String) {
        val doc = _editorState.value.document ?: return
        val clip = doc.clips.firstOrNull { it.id == clipId } ?: return
        _editorState.update {
            it.copy(
                activeTransitionClipId = clipId,
                selectedClipId = clipId,
                activeToolTab = EditorToolTab.TRANSITION,
                isToolDrawerOpen = true,
                playheadMs = (clip.startMs + (clip.transitionIn.durationMs / 2L)).coerceIn(0L, doc.totalDurationMs)
            )
        }
    }

    private fun prefetchFramesForCurrentPlayhead() {
        val state = _editorState.value
        val doc = state.document ?: return
        val playhead = state.playheadMs
        val videoClips = doc.clips.filter {
            (it.trackType == TrackType.MAIN_VIDEO || it.trackType == TrackType.OVERLAY_PIP) &&
                it.isVideoMedia && !it.mediaUri.isNullOrBlank() &&
                playhead in it.startMs..it.endMs
        }
        if (videoClips.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            videoClips.forEach { clip ->
                val uri = clip.mediaUri ?: return@forEach
                val localMs = (playhead - clip.startMs).coerceAtLeast(0L)
                val srcMs = canvasRenderer.evaluateSourceTimeMs(clip, localMs)
                frameEngine.prefetchVideoFrame(uri, srcMs)
            }
            _editorState.update { it.copy(frameRevision = it.frameRevision + 1) }
        }
    }

    // ---- Top Bar Project Settings ----

    fun updateResolution(res: ExportResolution) {
        mutateDocument { it.copy(resolution = res) }
    }

    fun updateFps(fps: ExportFps) {
        mutateDocument { it.copy(fps = fps) }
    }

    fun updateBitrate(bitrate: ExportBitrate) {
        mutateDocument { it.copy(bitrate = bitrate) }
    }

    // ---- Real Local Media Import (Zero Dummy/Stock Substitutions) ----

    fun importMultipleLocalMedia(
        uris: List<Uri>,
        preferredVisualTrack: TrackType = TrackType.MAIN_VIDEO
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentDoc = _editorState.value.document ?: return@launch
            var mainCursorMs = currentDoc.clips
                .filter { it.trackType == TrackType.MAIN_VIDEO }
                .maxOfOrNull { it.endMs } ?: 0L
            var pipCursorMs = _editorState.value.playheadMs
            var audioCursorMs = currentDoc.clips
                .filter { it.trackType == TrackType.AUDIO_MUSIC }
                .maxOfOrNull { it.endMs } ?: 0L

            val addedClips = mutableListOf<TimelineClip>()
            var firstVisualUri: String? = null

            uris.forEach { rawUri ->
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        rawUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val (localUri, originalName) = frameEngine.persistPickedMediaLocally(rawUri)
                val info = frameEngine.inspectMedia(
                    uri = localUri,
                    fallbackVideo = true,
                    fallbackAudio = false,
                    knownDisplayName = originalName
                )
                if (info.isVideo) {
                    frameEngine.prefetchVideoFrame(info.uriString, 0L)
                }
                if (info.isAudio) {
                    val clip = TimelineClip(
                        trackType = TrackType.AUDIO_MUSIC,
                        label = info.displayName,
                        startMs = audioCursorMs,
                        durationMs = info.durationMs,
                        sourceDurationMs = info.durationMs,
                        mediaUri = info.uriString,
                        isVideoMedia = false,
                        drawableKey = null,
                        accentColorHex = 0xFF10E896,
                        waveformPeaks = info.waveformPeaks
                    )
                    addedClips += clip
                    audioCursorMs += clip.durationMs
                } else {
                    if (firstVisualUri == null) firstVisualUri = info.uriString
                    val startMs = if (preferredVisualTrack == TrackType.MAIN_VIDEO) mainCursorMs else pipCursorMs
                    val clip = TimelineClip(
                        trackType = preferredVisualTrack,
                        label = info.displayName,
                        startMs = startMs,
                        durationMs = info.durationMs,
                        sourceDurationMs = info.durationMs,
                        mediaUri = info.uriString,
                        isVideoMedia = info.isVideo,
                        drawableKey = null,
                        accentColorHex = if (preferredVisualTrack == TrackType.MAIN_VIDEO) 0xFF9B5CFF else 0xFF00D4FF,
                        waveformPeaks = info.waveformPeaks
                    )
                    addedClips += clip
                    if (preferredVisualTrack == TrackType.MAIN_VIDEO) {
                        mainCursorMs += clip.durationMs
                    } else {
                        pipCursorMs += clip.durationMs
                    }
                }
            }

            if (addedClips.isEmpty()) return@launch
            val toastText = if (addedClips.size == 1) {
                "Imported ${addedClips.first().label}"
            } else {
                "Imported ${addedClips.size} media files"
            }

            mutateDocument(
                newSelectedClipId = addedClips.first().id,
                toast = toastText
            ) { doc ->
                doc.copy(
                    thumbnailUri = doc.thumbnailUri ?: firstVisualUri,
                    clips = doc.clips + addedClips
                )
            }
            _editorState.update { it.copy(frameRevision = it.frameRevision + 1) }
        }
    }

    fun importLocalVisualMedia(uri: Uri, targetTrack: TrackType = TrackType.MAIN_VIDEO) {
        importMultipleLocalMedia(listOf(uri), preferredVisualTrack = targetTrack)
    }

    fun importLocalAudioMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentDoc = _editorState.value.document ?: return@launch
            var cursorMs = if (currentDoc.clips.none { it.trackType == TrackType.AUDIO_MUSIC }) {
                0L
            } else {
                _editorState.value.playheadMs
            }
            val added = mutableListOf<TimelineClip>()
            uris.forEach { rawUri ->
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        rawUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val (localUri, originalName) = frameEngine.persistPickedMediaLocally(rawUri)
                val info = frameEngine.inspectMedia(
                    uri = localUri,
                    fallbackVideo = false,
                    fallbackAudio = true,
                    knownDisplayName = originalName
                )
                val clip = TimelineClip(
                    trackType = TrackType.AUDIO_MUSIC,
                    label = info.displayName,
                    startMs = cursorMs,
                    durationMs = info.durationMs,
                    sourceDurationMs = info.durationMs,
                    mediaUri = info.uriString,
                    isVideoMedia = false,
                    drawableKey = null,
                    accentColorHex = 0xFF10E896,
                    waveformPeaks = info.waveformPeaks
                )
                added += clip
                cursorMs += clip.durationMs
            }
            if (added.isEmpty()) return@launch
            mutateDocument(
                newSelectedClipId = added.first().id,
                toast = if (added.size == 1) "Added audio: ${added.first().label}" else "Added ${added.size} audio files"
            ) { doc ->
                doc.copy(clips = doc.clips + added)
            }
        }
    }

    fun setCutoutCustomBackground(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val (localUri, _) = frameEngine.persistPickedMediaLocally(uri)
            mutateDocument(toast = "Applied custom cutout background") { doc ->
                doc.copy(
                    cutout = doc.cutout.copy(
                        bgType = com.example.model.CutoutBackgroundType.CUSTOM_MEDIA,
                        bgCustomUri = localUri.toString(),
                        bgCustomDrawableKey = null
                    )
                )
            }
            _editorState.update { it.copy(frameRevision = it.frameRevision + 1) }
        }
    }

    // ---- Timeline Clip Actions ----

    fun splitSelectedClipAtPlayhead() {
        val state = _editorState.value
        val doc = state.document ?: return
        val clip = doc.clips.firstOrNull { it.id == state.selectedClipId } ?: return
        val cutMs = state.playheadMs
        if (cutMs <= clip.startMs + 300L || cutMs >= clip.endMs - 300L) {
            _editorState.update { it.copy(toastMessage = "Move playhead inside the selected clip to split") }
            return
        }
        val firstDur = cutMs - clip.startMs
        val secondDur = clip.durationMs - firstDur
        val firstHalf = clip.copy(
            durationMs = firstDur,
            keyframes = clip.keyframes.filter { it.timeOffsetMs <= firstDur }
        )
        val secondHalf = clip.copy(
            id = UUID.randomUUID().toString(),
            label = "${clip.label} (2)",
            startMs = cutMs,
            durationMs = secondDur,
            sourceStartMs = clip.sourceStartMs + firstDur,
            transitionIn = ClipTransition(TransitionType.FLASH_WHITE, 350L),
            keyframes = clip.keyframes
                .filter { it.timeOffsetMs >= firstDur }
                .map { it.copy(id = UUID.randomUUID().toString(), timeOffsetMs = it.timeOffsetMs - firstDur) }
        )
        mutateDocument(newSelectedClipId = secondHalf.id, toast = "Clip split at playhead") { current ->
            val replaced = current.clips.flatMap { if (it.id == clip.id) listOf(firstHalf, secondHalf) else listOf(it) }
            current.copy(clips = replaced)
        }
    }

    fun duplicateSelectedClip() {
        val state = _editorState.value
        val doc = state.document ?: return
        val clip = doc.clips.firstOrNull { it.id == state.selectedClipId } ?: return
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            label = "${clip.label} Copy",
            startMs = clip.endMs + 100L,
            keyframes = clip.keyframes.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        mutateDocument(newSelectedClipId = copy.id, toast = "Duplicated clip") { current ->
            current.copy(clips = current.clips + copy)
        }
    }

    fun deleteSelectedClip() {
        val clipId = _editorState.value.selectedClipId ?: return
        mutateDocument(toast = "Deleted clip") { current ->
            current.copy(clips = current.clips.filterNot { it.id == clipId })
        }
    }

    fun trimClip(clipId: String, deltaStartMs: Long, deltaDurationMs: Long) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(
                clips = doc.clips.map { clip ->
                    if (clip.id != clipId) clip
                    else {
                        val newStart = (clip.startMs + deltaStartMs).coerceAtLeast(0L)
                        val actualDeltaStart = newStart - clip.startMs
                        val newDur = (clip.durationMs + deltaDurationMs - actualDeltaStart).coerceIn(500L, 90_000L)
                        clip.copy(
                            startMs = newStart,
                            durationMs = newDur,
                            sourceStartMs = (clip.sourceStartMs + actualDeltaStart).coerceAtLeast(0L)
                        )
                    }
                }
            )
        }
    }

    fun moveClipStart(clipId: String, deltaMs: Long) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(
                clips = doc.clips.map { clip ->
                    if (clip.id != clipId) clip
                    else clip.copy(startMs = (clip.startMs + deltaMs).coerceAtLeast(0L))
                }
            )
        }
    }

    // ---- VIP 1: AI Auto-Captions & Dynamic Subtitles ----

    fun generateAutoCaptions(rawScript: String, style: AutoCaptionStyle, wordsPerCaption: Int = 3) {
        val doc = _editorState.value.document ?: return
        val words = rawScript.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            _editorState.update { it.copy(toastMessage = "Paste a script or dictate speech first") }
            return
        }

        val chunks = words.chunked(wordsPerCaption.coerceIn(1, 5)).map { it.joinToString(" ") }
        val totalMs = doc.totalDurationMs.coerceAtLeast(chunks.size * 1000L)
        val sliceMs = (totalMs / chunks.size).coerceAtLeast(650L)

        val newCaptionClips = chunks.mapIndexed { idx, phrase ->
            val styled = when (style) {
                AutoCaptionStyle.HORMOZI -> TextClipStyle(
                    text = phrase.uppercase(),
                    fontStyle = TextFontStyle.BEBAS_NEUE,
                    effectStyle = TextEffectStyle.OUTLINE_BOLD,
                    animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                    textColorHex = 0xFFFFFFFF,
                    accentColorHex = if (idx % 2 == 0) 0xFFFFD84D else 0xFFA3FF12,
                    fontSizeSp = 38f
                )
                AutoCaptionStyle.NEON_POP -> TextClipStyle(
                    text = phrase.uppercase(),
                    fontStyle = TextFontStyle.SYNE,
                    effectStyle = TextEffectStyle.NEON_GLOW,
                    animationType = TextAnimationType.POP,
                    textColorHex = 0xFFFFFFFF,
                    accentColorHex = if (idx % 2 == 0) 0xFF00F0FF else 0xFFFF2E93,
                    fontSizeSp = 32f
                )
                AutoCaptionStyle.KARAOKE -> TextClipStyle(
                    text = phrase,
                    fontStyle = TextFontStyle.SPACE_GROTESK,
                    effectStyle = TextEffectStyle.DROP_SHADOW,
                    animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                    textColorHex = 0xFFFFFFFF,
                    accentColorHex = 0xFF00F0FF,
                    fontSizeSp = 30f
                )
                AutoCaptionStyle.MINIMALIST_BOLD -> TextClipStyle(
                    text = phrase,
                    fontStyle = TextFontStyle.PLUS_JAKARTA,
                    effectStyle = TextEffectStyle.PILL_HIGHLIGHT,
                    animationType = TextAnimationType.FADE_UP,
                    textColorHex = 0xFF0A0A0A,
                    accentColorHex = 0xFFF6F6FA,
                    fontSizeSp = 25f
                )
            }
            TimelineClip(
                trackType = TrackType.TEXT_STICKER,
                label = phrase.take(18),
                startMs = idx * sliceMs,
                durationMs = (sliceMs - 40L).coerceAtLeast(500L),
                accentColorHex = style.badgeHex,
                textStyle = styled
            )
        }

        mutateDocument(
            newSelectedClipId = newCaptionClips.firstOrNull()?.id,
            toast = "Generated ${newCaptionClips.size} ${style.label} captions!"
        ) { current ->
            val nonText = current.clips.filterNot { it.trackType == TrackType.TEXT_STICKER }
            current.copy(clips = nonText + newCaptionClips)
        }
        seekTo(0L)
    }

    // ---- VIP 2: Smart Cutout & Chroma Key ----

    fun updateCutout(transform: (CutoutSettings) -> CutoutSettings) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(cutout = transform(doc.cutout))
        }
    }

    // ---- VIP 3: Auto Beat-Sync & Velocity Motion Blur ----

    fun detectAudioBeats() {
        val doc = _editorState.value.document ?: return
        _editorState.update { it.copy(isAnalyzingBeats = true) }
        viewModelScope.launch {
            val beats = beatSyncAnalyzer.detectBeats(doc)
            _editorState.update { it.copy(isAnalyzingBeats = false) }
            mutateDocument(toast = "Detected ${beats.size} bass beat markers!") { current ->
                current.copy(beatMarkersMs = beats)
            }
        }
    }

    fun snapAndCutMainClipsToBeats() {
        val doc = _editorState.value.document ?: return
        val beats = doc.beatMarkersMs.sorted().filter { it > 350L && it < doc.totalDurationMs - 350L }
        if (beats.isEmpty()) {
            detectAudioBeats()
            return
        }
        val mainClips = doc.clips.filter { it.trackType == TrackType.MAIN_VIDEO }.sortedBy { it.startMs }
        if (mainClips.isEmpty()) return

        // Cut main clips along every 2nd beat (or every beat if sparse) for clean rhythmic cuts
        val stride = if (beats.size > 10) 2 else 1
        val cutPoints = beats.filterIndexed { idx, _ -> idx % stride == 0 }
        val drawables = listOf("tpl_beat_sync", "tpl_velocity", "tpl_cinematic_travel", "tpl_retro_vhs")
        val proTrans = listOf(TransitionType.FLASH_WHITE, TransitionType.GLITCH_ZOOM, TransitionType.WHIP_PAN_LEFT, TransitionType.ZOOM_IN_BLUR)

        val boundaries = (listOf(0L) + cutPoints + listOf(doc.totalDurationMs)).distinct().sorted()
        val syncedMain = mutableListOf<TimelineClip>()
        for (i in 0 until boundaries.lastIndex) {
            val segStart = boundaries[i]
            val segEnd = boundaries[i + 1]
            val segDur = (segEnd - segStart).coerceAtLeast(450L)
            val sourceClip = mainClips.firstOrNull { segStart in it.startMs..it.endMs } ?: mainClips[i % mainClips.size]
            syncedMain += sourceClip.copy(
                id = UUID.randomUUID().toString(),
                label = "Beat ${i + 1}",
                startMs = segStart,
                durationMs = segDur,
                sourceStartMs = (sourceClip.sourceStartMs + (segStart - sourceClip.startMs).coerceAtLeast(0L)) % sourceClip.sourceDurationMs.coerceAtLeast(1000L),
                drawableKey = sourceClip.drawableKey ?: drawables[i % drawables.size],
                transitionIn = if (i > 0) ClipTransition(proTrans[i % proTrans.size], 320L) else ClipTransition(),
                keyframes = listOf(
                    Keyframe(timeOffsetMs = 0L, scale = 1.18f),
                    Keyframe(timeOffsetMs = (segDur * 0.45f).toLong(), scale = 1.02f),
                    Keyframe(timeOffsetMs = segDur, scale = 1.08f)
                )
            )
        }

        mutateDocument(
            newSelectedClipId = syncedMain.firstOrNull()?.id,
            toast = "Snapped & cut ${syncedMain.size} clips to rhythm!"
        ) { current ->
            val others = current.clips.filterNot { it.trackType == TrackType.MAIN_VIDEO }
            current.copy(clips = syncedMain + others)
        }
    }

    fun updateMotionBlur(intensity: Float) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(visualFx = doc.visualFx.copy(motionBlur = intensity.coerceIn(0f, 1f)))
        }
    }

    // ---- VIP 4: 32 Pro Transitions ----

    fun updateClipTransition(type: TransitionType? = null, durationMs: Long? = null) {
        val state = _editorState.value
        val doc = state.document ?: return
        val mainClips = doc.clips.filter { it.trackType == TrackType.MAIN_VIDEO }.sortedBy { it.startMs }
        val targetId = state.activeTransitionClipId?.takeIf { id -> mainClips.any { it.id == id } }
            ?: mainClips.getOrNull(1)?.id
            ?: mainClips.firstOrNull()?.id
            ?: return

        mutateDocument(recordUndo = type != null) { current ->
            current.copy(
                clips = current.clips.map { clip ->
                    if (clip.id != targetId) clip
                    else clip.copy(
                        transitionIn = clip.transitionIn.copy(
                            type = type ?: clip.transitionIn.type,
                            durationMs = (durationMs ?: clip.transitionIn.durationMs).coerceIn(200L, 2000L)
                        )
                    )
                }
            )
        }
        val updatedClip = _editorState.value.document?.clips?.firstOrNull { it.id == targetId }
        if (updatedClip != null && type != null && type != TransitionType.NONE) {
            seekTo((updatedClip.startMs + updatedClip.transitionIn.durationMs / 2L).coerceIn(0L, doc.totalDurationMs))
        }
    }

    // ---- VIP 5: Multi-Layer PIP Blend Modes & Shape Masking ----

    fun updateActivePipClip(transform: (TimelineClip) -> TimelineClip) {
        val state = _editorState.value
        val doc = state.document ?: return
        val existingPip = doc.clips.firstOrNull { it.id == state.selectedClipId && it.trackType == TrackType.OVERLAY_PIP }
            ?: doc.clips.firstOrNull { it.trackType == TrackType.OVERLAY_PIP }

        if (existingPip == null) {
            val freshPip = transform(
                TimelineClip(
                    trackType = TrackType.OVERLAY_PIP,
                    label = "PIP Layer",
                    startMs = state.playheadMs.coerceAtMost((doc.totalDurationMs - 2000L).coerceAtLeast(0L)),
                    durationMs = 4500L,
                    drawableKey = "tpl_beat_sync",
                    accentColorHex = 0xFF00D4FF
                )
            )
            mutateDocument(newSelectedClipId = freshPip.id, toast = "Added PIP Overlay layer") { d ->
                d.copy(clips = d.clips + freshPip)
            }
            return
        }

        mutateDocument(recordUndo = false, newSelectedClipId = existingPip.id) { d ->
            d.copy(clips = d.clips.map { if (it.id == existingPip.id) transform(it) else it })
        }
    }

    // ---- Audio Enhance ----

    fun updateAudioEnhance(transform: (AudioEnhanceSettings) -> AudioEnhanceSettings) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(audioEnhance = transform(doc.audioEnhance))
        }
    }

    fun toggleAudioMute() {
        val currentMuted = _editorState.value.document?.audioEnhance?.isMuted ?: false
        mutateDocument { doc ->
            doc.copy(audioEnhance = doc.audioEnhance.copy(isMuted = !currentMuted))
        }
        val updatedDoc = _editorState.value.document ?: return
        if (!currentMuted) audioEngine.stopPlayback()
        else if (_editorState.value.isPlaying) audioEngine.startPlayback(updatedDoc, _editorState.value.playheadMs)
    }

    fun addVolumeKeyframeAtPlayhead(volume: Float) {
        val playhead = _editorState.value.playheadMs
        mutateDocument(toast = "Volume keyframe added") { doc ->
            val existing = doc.audioEnhance.volumeKeyframes.filterNot { abs(it.timeMs - playhead) < 150L }
            val next = (existing + VolumeKeyframe(timeMs = playhead, volume = volume.coerceIn(0f, 1.5f))).sortedBy { it.timeMs }
            doc.copy(audioEnhance = doc.audioEnhance.copy(volumeKeyframes = next))
        }
    }

    fun clearVolumeKeyframes() {
        mutateDocument(toast = "Volume keyframes cleared") { doc ->
            doc.copy(audioEnhance = doc.audioEnhance.copy(volumeKeyframes = emptyList()))
        }
    }

    // ---- Color Grading, 3-Way Color Wheels & Beauty Retouch ----

    fun selectLutPreset(lut: LutPreset) {
        mutateDocument { doc -> doc.copy(colorGrading = doc.colorGrading.copy(lutPreset = lut)) }
    }

    fun updateColorGrading(transform: (ColorGrading) -> ColorGrading) {
        mutateDocument(recordUndo = false) { doc -> doc.copy(colorGrading = transform(doc.colorGrading)) }
    }

    fun resetColorGrading() {
        mutateDocument(toast = "Color grade & retouch reset") { doc -> doc.copy(colorGrading = ColorGrading()) }
    }

    // ---- Keyframes ----

    fun addOrUpdateKeyframeAtPlayhead(
        scale: Float? = null,
        offsetX: Float? = null,
        offsetY: Float? = null,
        rotationDeg: Float? = null,
        opacity: Float? = null,
        easing: KeyframeEasing? = null
    ) {
        val state = _editorState.value
        val doc = state.document ?: return
        val clip = doc.clips.firstOrNull { it.id == state.selectedClipId }
            ?: doc.clips.firstOrNull { it.trackType == TrackType.MAIN_VIDEO }
            ?: return
        val localMs = (state.playheadMs - clip.startMs).coerceIn(0L, clip.durationMs)
        val evaluated = canvasRenderer.evaluateKeyframes(clip.keyframes, localMs)
        val existing = clip.keyframes.firstOrNull { abs(it.timeOffsetMs - localMs) <= 140L }
        val updatedKf = Keyframe(
            id = existing?.id ?: UUID.randomUUID().toString(),
            timeOffsetMs = existing?.timeOffsetMs ?: localMs,
            scale = (scale ?: existing?.scale ?: evaluated.scale).coerceIn(0.3f, 3.0f),
            offsetX = (offsetX ?: existing?.offsetX ?: evaluated.offsetX).coerceIn(-0.8f, 0.8f),
            offsetY = (offsetY ?: existing?.offsetY ?: evaluated.offsetY).coerceIn(-0.8f, 0.8f),
            rotationDeg = (rotationDeg ?: existing?.rotationDeg ?: evaluated.rotationDeg).coerceIn(-180f, 180f),
            opacity = (opacity ?: existing?.opacity ?: evaluated.opacity).coerceIn(0f, 1f),
            easing = easing ?: existing?.easing ?: KeyframeEasing.EASE_IN_OUT
        )
        mutateDocument(recordUndo = existing == null, newSelectedClipId = clip.id) { d ->
            d.copy(
                clips = d.clips.map { c ->
                    if (c.id != clip.id) c
                    else c.copy(keyframes = (c.keyframes.filterNot { it.id == updatedKf.id } + updatedKf).sortedBy { it.timeOffsetMs })
                }
            )
        }
    }

    fun applyClipMultiTouchGesture(
        zoomFactor: Float,
        panDeltaXNorm: Float,
        panDeltaYNorm: Float
    ) {
        val state = _editorState.value
        val doc = state.document ?: return
        val selected = doc.clips.firstOrNull { it.id == state.selectedClipId }
        val activeMain = doc.clips.firstOrNull {
            it.trackType == TrackType.MAIN_VIDEO && state.playheadMs in it.startMs..it.endMs
        } ?: doc.clips.firstOrNull { it.trackType == TrackType.MAIN_VIDEO }

        val target = when {
            selected != null && (selected.trackType == TrackType.OVERLAY_PIP || selected.trackType == TrackType.MAIN_VIDEO) -> selected
            else -> activeMain
        } ?: return

        if (target.trackType == TrackType.OVERLAY_PIP) {
            updateActivePipClip { pip ->
                pip.copy(
                    pipScale = (pip.pipScale * zoomFactor).coerceIn(0.18f, 1.4f),
                    pipOffsetX = (pip.pipOffsetX + panDeltaXNorm).coerceIn(-0.6f, 0.6f),
                    pipOffsetY = (pip.pipOffsetY + panDeltaYNorm).coerceIn(-0.6f, 0.6f)
                )
            }
            return
        }

        val localMs = (state.playheadMs - target.startMs).coerceIn(0L, target.durationMs)
        val evaluated = canvasRenderer.evaluateKeyframes(target.keyframes, localMs)
        val newScale = (evaluated.scale * zoomFactor).coerceIn(0.35f, 3.0f)
        val newOffsetX = (evaluated.offsetX + panDeltaXNorm).coerceIn(-0.8f, 0.8f)
        val newOffsetY = (evaluated.offsetY + panDeltaYNorm).coerceIn(-0.8f, 0.8f)
        addOrUpdateKeyframeAtPlayhead(
            scale = newScale,
            offsetX = newOffsetX,
            offsetY = newOffsetY
        )
    }

    fun resetActiveClipTransform() {
        val state = _editorState.value
        val doc = state.document ?: return
        val selected = doc.clips.firstOrNull { it.id == state.selectedClipId }
        if (selected?.trackType == TrackType.OVERLAY_PIP) {
            updateActivePipClip { it.copy(pipScale = 0.42f, pipOffsetX = 0.22f, pipOffsetY = -0.22f) }
        } else {
            addOrUpdateKeyframeAtPlayhead(scale = 1.0f, offsetX = 0f, offsetY = 0f, rotationDeg = 0f)
        }
    }

    fun removeKeyframeAtPlayhead() {
        val state = _editorState.value
        val doc = state.document ?: return
        val clip = doc.clips.firstOrNull { it.id == state.selectedClipId } ?: return
        val localMs = (state.playheadMs - clip.startMs).coerceIn(0L, clip.durationMs)
        val target = clip.keyframes.minByOrNull { abs(it.timeOffsetMs - localMs) } ?: return
        if (abs(target.timeOffsetMs - localMs) > 250L) return
        mutateDocument(toast = "Keyframe removed") { d ->
            d.copy(clips = d.clips.map { c -> if (c.id != clip.id) c else c.copy(keyframes = c.keyframes.filterNot { it.id == target.id }) })
        }
    }

    fun jumpToAdjacentKeyframe(next: Boolean) {
        val state = _editorState.value
        val doc = state.document ?: return
        val clip = doc.clips.firstOrNull { it.id == state.selectedClipId } ?: return
        val localMs = (state.playheadMs - clip.startMs).coerceIn(0L, clip.durationMs)
        val sorted = clip.keyframes.sortedBy { it.timeOffsetMs }
        val target = (if (next) sorted.firstOrNull { it.timeOffsetMs > localMs + 60L }
        else sorted.lastOrNull { it.timeOffsetMs < localMs - 60L }) ?: return
        seekTo(clip.startMs + target.timeOffsetMs)
    }

    // ---- Speed Curves ----

    fun updateSelectedClipSpeed(speed: Float) {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(recordUndo = false, newSelectedClipId = clipId) { doc ->
            doc.copy(clips = doc.clips.map { c -> if (c.id != clipId) c else c.copy(speed = speed.coerceIn(0.1f, 10f), speedCurvePreset = SpeedCurvePreset.NONE) })
        }
    }

    fun applySpeedCurvePreset(preset: SpeedCurvePreset) {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(newSelectedClipId = clipId, toast = "Speed curve: ${preset.label}") { doc ->
            doc.copy(clips = doc.clips.map { c -> if (c.id != clipId) c else c.copy(speedCurvePreset = preset, speedCurvePoints = defaultCurvePoints(preset)) })
        }
    }

    fun updateSpeedCurvePoint(pointIndex: Int, newSpeedMultiplier: Float) {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(recordUndo = false, newSelectedClipId = clipId) { doc ->
            doc.copy(
                clips = doc.clips.map { c ->
                    if (c.id != clipId) c
                    else {
                        val pts = c.speedCurvePoints.toMutableList()
                        if (pointIndex in pts.indices) {
                            pts[pointIndex] = pts[pointIndex].copy(speedMultiplier = newSpeedMultiplier.coerceIn(0.1f, 10f))
                        }
                        c.copy(speedCurvePreset = SpeedCurvePreset.CUSTOM, speedCurvePoints = pts)
                    }
                }
            )
        }
    }

    private fun resolveTargetVideoClipId(): String? {
        val state = _editorState.value
        val doc = state.document ?: return null
        val selected = doc.clips.firstOrNull { it.id == state.selectedClipId }
        if (selected != null && (selected.trackType == TrackType.MAIN_VIDEO || selected.trackType == TrackType.OVERLAY_PIP)) {
            return selected.id
        }
        return doc.clips.firstOrNull { it.trackType == TrackType.MAIN_VIDEO }?.id
    }

    // ---- Text Animation & Visual FX ----

    fun addTextClipAtPlayhead(initialText: String = "NEW CAPTION") {
        val playhead = _editorState.value.playheadMs
        val newClip = TimelineClip(
            trackType = TrackType.TEXT_STICKER,
            label = initialText.take(18),
            startMs = playhead,
            durationMs = 3500L,
            accentColorHex = 0xFFFF9F1C,
            textStyle = TextClipStyle(text = initialText)
        )
        mutateDocument(newSelectedClipId = newClip.id, toast = "Added text overlay") { doc ->
            doc.copy(clips = doc.clips + newClip)
        }
    }

    fun updateSelectedTextStyle(transform: (TextClipStyle) -> TextClipStyle) {
        val state = _editorState.value
        val doc = state.document ?: return
        val existingText = doc.clips.firstOrNull { it.id == state.selectedClipId && it.trackType == TrackType.TEXT_STICKER }
            ?: doc.clips.firstOrNull { it.trackType == TrackType.TEXT_STICKER }
        if (existingText == null) {
            val style = transform(TextClipStyle())
            val newClip = TimelineClip(
                trackType = TrackType.TEXT_STICKER,
                label = style.text.take(18).ifBlank { "Caption" },
                startMs = state.playheadMs,
                durationMs = 4000L,
                accentColorHex = 0xFFFF9F1C,
                textStyle = style
            )
            mutateDocument(newSelectedClipId = newClip.id) { d -> d.copy(clips = d.clips + newClip) }
            return
        }
        mutateDocument(recordUndo = false, newSelectedClipId = existingText.id) { d ->
            d.copy(
                clips = d.clips.map { c ->
                    if (c.id != existingText.id) c
                    else {
                        val updated = transform(c.textStyle ?: TextClipStyle())
                        c.copy(label = updated.text.take(18).ifBlank { "Caption" }, textStyle = updated)
                    }
                }
            )
        }
    }

    fun selectVisualFx(fxType: VisualFxType) {
        mutateDocument { doc -> doc.copy(visualFx = doc.visualFx.copy(activeFx = fxType)) }
    }

    fun updateVisualFxSettings(transform: (VisualFxSettings) -> VisualFxSettings) {
        mutateDocument(recordUndo = false) { doc -> doc.copy(visualFx = transform(doc.visualFx)) }
    }

    fun addStickerAtPlayhead(emoji: String) {
        val playhead = _editorState.value.playheadMs
        val newClip = TimelineClip(
            trackType = TrackType.TEXT_STICKER,
            label = "Sticker $emoji",
            startMs = playhead,
            durationMs = 3000L,
            accentColorHex = 0xFFFFD84D,
            textStyle = TextClipStyle(
                text = emoji,
                fontStyle = TextFontStyle.SPACE_GROTESK,
                effectStyle = TextEffectStyle.CLEAN,
                animationType = TextAnimationType.BOUNCE,
                outAnimation = TextAnimOut.SHRINK_POP,
                loopAnimation = TextAnimLoop.FLOATING_HOVER,
                fontSizeSp = 48f,
                isSticker = true
            )
        )
        mutateDocument(newSelectedClipId = newClip.id, toast = "Added animated sticker $emoji") { doc ->
            doc.copy(clips = doc.clips + newClip)
        }
    }

    // ---- Essential Clip Ops: Freeze Frame, Reverse, Rotate/Flip/Crop, Video Animations ----

    fun insertFreezeFrameAtPlayhead() {
        val state = _editorState.value
        val doc = state.document ?: return
        val activeMain = doc.clips.firstOrNull {
            it.id == state.selectedClipId && (it.trackType == TrackType.MAIN_VIDEO || it.trackType == TrackType.OVERLAY_PIP)
        } ?: doc.clips.firstOrNull {
            it.trackType == TrackType.MAIN_VIDEO && state.playheadMs in it.startMs..it.endMs
        } ?: doc.clips.firstOrNull { it.trackType == TrackType.MAIN_VIDEO }

        if (activeMain == null) {
            _editorState.update { it.copy(toastMessage = "Add a video or image clip first to freeze a frame") }
            return
        }

        val localMs = (state.playheadMs - activeMain.startMs).coerceIn(0L, activeMain.durationMs)
        val frozenSrcMs = canvasRenderer.evaluateSourceTimeMs(activeMain, localMs)
        val freezeDur = 3000L
        val insertStart = activeMain.endMs
        val freezeClip = activeMain.copy(
            id = UUID.randomUUID().toString(),
            label = "${activeMain.label.take(12)} Freeze",
            startMs = insertStart,
            durationMs = freezeDur,
            isFreezeFrame = true,
            freezeSourceMs = frozenSrcMs,
            isReversed = false,
            keyframes = emptyList(),
            transitionIn = ClipTransition(TransitionType.FLASH_WHITE, 280L)
        )
        mutateDocument(newSelectedClipId = freezeClip.id, toast = "Inserted 3s Freeze Frame!") { current ->
            val shifted = current.clips.map { c ->
                if (c.trackType == activeMain.trackType && c.startMs >= insertStart) {
                    c.copy(startMs = c.startMs + freezeDur)
                } else c
            }
            current.copy(clips = shifted + freezeClip)
        }
        seekTo(insertStart + 200L)
    }

    fun toggleReverseSelectedClip() {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(newSelectedClipId = clipId, toast = "Toggled Reverse playback") { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == clipId) it.copy(isReversed = !it.isReversed) else it })
        }
    }

    fun rotateSelectedClip90() {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(newSelectedClipId = clipId, toast = "Rotated clip 90°") { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == clipId) it.copy(rotationQuarterTurns = (it.rotationQuarterTurns + 1) % 4) else it })
        }
    }

    fun toggleFlipHorizontalSelectedClip() {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(newSelectedClipId = clipId, toast = "Mirrored clip horizontally") { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == clipId) it.copy(flipHorizontal = !it.flipHorizontal) else it })
        }
    }

    fun toggleFlipVerticalSelectedClip() {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(newSelectedClipId = clipId, toast = "Flipped clip vertically") { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == clipId) it.copy(flipVertical = !it.flipVertical) else it })
        }
    }

    fun updateSelectedClipCropZoom(cropZoom: Float) {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(recordUndo = false, newSelectedClipId = clipId) { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == clipId) it.copy(cropZoom = cropZoom.coerceIn(0.6f, 2.5f)) else it })
        }
    }

    fun updateSelectedClipAnimation(
        animIn: VideoAnimIn? = null,
        animOut: VideoAnimOut? = null,
        animCombo: VideoAnimCombo? = null,
        durationMs: Long? = null
    ) {
        val clipId = resolveTargetVideoClipId() ?: return
        mutateDocument(recordUndo = durationMs == null, newSelectedClipId = clipId) { doc ->
            doc.copy(
                clips = doc.clips.map { c ->
                    if (c.id != clipId) c
                    else c.copy(
                        animIn = animIn ?: c.animIn,
                        animOut = animOut ?: c.animOut,
                        animCombo = animCombo ?: c.animCombo,
                        animDurationMs = (durationMs ?: c.animDurationMs).coerceIn(200L, 3000L)
                    )
                }
            )
        }
    }

    // ---- Canvas Background & Music Visualizer ----

    fun updateCanvasBackground(transform: (CanvasBackgroundSettings) -> CanvasBackgroundSettings) {
        mutateDocument(recordUndo = false) { doc ->
            doc.copy(canvasBackground = transform(doc.canvasBackground))
        }
    }

    fun setCanvasBackgroundCustomImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val (localUri, _) = frameEngine.persistPickedMediaLocally(uri)
            mutateDocument(toast = "Applied custom canvas background") { doc ->
                doc.copy(
                    canvasBackground = doc.canvasBackground.copy(
                        mode = CanvasBgMode.CUSTOM_IMAGE,
                        customImageUri = localUri.toString()
                    )
                )
            }
            _editorState.update { it.copy(frameRevision = it.frameRevision + 1) }
        }
    }

    fun updateMusicVisualizer(style: MusicVisualizerStyle) {
        mutateDocument(toast = if (style == MusicVisualizerStyle.NONE) "Visualizer off" else "Music Visualizer: ${style.label}") { doc ->
            doc.copy(musicVisualizer = style)
        }
    }

    // ---- All-in-One AI Video Lab (Real Firebase AI Logic - Gemini) ----

    fun resetAiTaskState() {
        aiJob?.cancel()
        _aiTaskState.value = AiTaskState.Idle
    }

    fun resetViralKitState() {
        viralKitJob?.cancel()
        _viralKitState.value = AiTaskState.Idle
    }

    private suspend fun ensureProjectAndInsertClips(
        projectTitle: String,
        aspectRatio: AspectRatioPreset,
        newClips: List<TimelineClip>,
        firstThumbUri: String? = null,
        visualizer: MusicVisualizerStyle? = null,
        visualFx: VisualFxType? = null,
        beatMarkers: List<Long>? = null,
        toastMsg: String
    ) {
        val currentDoc = _editorState.value.document
        if (currentDoc != null) {
            val mainEnd = currentDoc.clips.filter { it.trackType == TrackType.MAIN_VIDEO }.maxOfOrNull { it.endMs } ?: 0L
            val shiftedClips = newClips.map { c ->
                if (c.trackType == TrackType.MAIN_VIDEO || c.trackType == TrackType.TEXT_STICKER) {
                    c.copy(startMs = c.startMs + mainEnd)
                } else c
            }
            mutateDocument(
                newSelectedClipId = shiftedClips.firstOrNull()?.id,
                toast = toastMsg
            ) { doc ->
                doc.copy(
                    thumbnailUri = doc.thumbnailUri ?: firstThumbUri,
                    musicVisualizer = visualizer ?: doc.musicVisualizer,
                    visualFx = if (visualFx != null) doc.visualFx.copy(activeFx = visualFx) else doc.visualFx,
                    beatMarkersMs = beatMarkers ?: doc.beatMarkersMs,
                    clips = doc.clips + shiftedClips
                )
            }
            seekTo(mainEnd)
            _editorState.update { it.copy(frameRevision = it.frameRevision + 1) }
        } else {
            val doc = VideoProjectDocument(
                id = UUID.randomUUID().toString(),
                title = projectTitle.take(28).ifBlank { "AI Studio Reel" },
                aspectRatio = aspectRatio,
                resolution = ExportResolution.RES_4K,
                fps = ExportFps.FPS_60,
                bitrate = ExportBitrate.ULTRA_PRO,
                thumbnailUri = firstThumbUri,
                musicVisualizer = visualizer ?: MusicVisualizerStyle.NONE,
                visualFx = if (visualFx != null) VisualFxSettings(activeFx = visualFx, intensity = 0.55f) else VisualFxSettings(),
                beatMarkersMs = beatMarkers ?: emptyList(),
                clips = newClips
            )
            repository.saveProject(doc)
            openDocumentInEditor(doc, initialToolTab = EditorToolTab.ANIMATE)
            _editorState.update { it.copy(toastMessage = toastMsg, frameRevision = it.frameRevision + 1) }
        }
    }

    fun runAiTextToImage(
        prompt: String,
        style: AiImageStyle,
        aspectRatio: AspectRatioPreset
    ) {
        if (prompt.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Enter an image description first.")
            return
        }
        networkMonitor.refresh()
        if (networkStatus.value.isOffline) {
            _aiTaskState.value = AiTaskState.Error("Offline: Cloud AI needs internet. Photo Animate, Paste Lyrics & full 4K video editing work 100% offline!")
            return
        }
        val useLite = networkStatus.value.useLiteOptimization
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val modeNote = if (useLite) "Lite Low-Net Mode" else "Standard"
            _aiTaskState.value = AiTaskState.Loading("Generating ${style.label} artwork ($modeNote)…")
            val result = aiService.generateAiImageToLocalFile(
                prompt = prompt,
                style = style,
                aspectLabel = aspectRatio.label,
                lowDataMode = useLite,
                onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
            )
            result.fold(
                onSuccess = { uri ->
                    val clip = TimelineClip(
                        trackType = TrackType.MAIN_VIDEO,
                        label = "AI: ${prompt.take(16)}",
                        startMs = 0L,
                        durationMs = 4500L,
                        sourceDurationMs = 4500L,
                        mediaUri = uri.toString(),
                        isVideoMedia = false,
                        accentColorHex = 0xFF9B5CFF,
                        animIn = VideoAnimIn.ZOOM_IN,
                        animDurationMs = 1200L
                    )
                    ensureProjectAndInsertClips(
                        projectTitle = "AI ${style.label}",
                        aspectRatio = aspectRatio,
                        newClips = listOf(clip),
                        firstThumbUri = uri.toString(),
                        toastMsg = "AI Image added to timeline!"
                    )
                    _aiTaskState.value = AiTaskState.Success("Added ${style.label} AI visual directly to your timeline.")
                },
                onFailure = { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "AI image generation failed.")
                }
            )
        }
    }

    fun runAiTextToVideo(
        prompt: String,
        style: AiImageStyle,
        motion: AiCameraMotion,
        aspectRatio: AspectRatioPreset
    ) {
        if (prompt.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Describe your scene prompt first.")
            return
        }
        networkMonitor.refresh()
        if (networkStatus.value.isOffline) {
            _aiTaskState.value = AiTaskState.Error("Offline: Text→Video needs internet. Try Photo Animate to turn your gallery photos into motion videos offline!")
            return
        }
        val useLite = networkStatus.value.useLiteOptimization
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val modeNote = if (useLite) "Low-Net Lite" else "Gemini"
            _aiTaskState.value = AiTaskState.Loading("Generating AI scene & ${motion.label} motion ($modeNote)…")
            val result = aiService.generateAiImageToLocalFile(
                prompt = prompt,
                style = style,
                aspectLabel = aspectRatio.label,
                lowDataMode = useLite,
                onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
            )
            result.fold(
                onSuccess = { uri ->
                    val durMs = 6000L
                    val keyframes = buildCameraMotionKeyframes(motion, durMs)
                    val fx = if (motion == AiCameraMotion.PARTICLE_FLOW) VisualFxType.SPARKLE else null
                    val clip = TimelineClip(
                        trackType = TrackType.MAIN_VIDEO,
                        label = "AI ${motion.label}",
                        startMs = 0L,
                        durationMs = durMs,
                        sourceDurationMs = durMs,
                        mediaUri = uri.toString(),
                        isVideoMedia = false,
                        accentColorHex = 0xFF00F0FF,
                        keyframes = keyframes,
                        animIn = VideoAnimIn.FADE_IN,
                        animCombo = if (motion == AiCameraMotion.PARALLAX_3D) VideoAnimCombo.PENDULUM_1 else VideoAnimCombo.NONE
                    )
                    ensureProjectAndInsertClips(
                        projectTitle = "AI ${motion.label} Clip",
                        aspectRatio = aspectRatio,
                        newClips = listOf(clip),
                        firstThumbUri = uri.toString(),
                        visualFx = fx,
                        toastMsg = "AI Motion Video added to timeline!"
                    )
                    _aiTaskState.value = AiTaskState.Success("Created 6s animated ${motion.label} clip on timeline.")
                },
                onFailure = { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "AI Text-to-Video failed.")
                }
            )
        }
    }

    fun runAiScriptToVideo(
        scriptOrTopic: String,
        language: String,
        generateAiVisualCover: Boolean,
        aspectRatio: AspectRatioPreset
    ) {
        if (scriptOrTopic.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Enter a topic or script first.")
            return
        }
        networkMonitor.refresh()
        if (networkStatus.value.isOffline) {
            _aiTaskState.value = AiTaskState.Error("Offline: Connect internet for AI Script-to-Video. All local timeline editing & 4K export work offline!")
            return
        }
        val useLite = networkStatus.value.useLiteOptimization
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val engineTag = if (useLite) "Gemini Lite (Low-Net)" else "Gemini"
            _aiTaskState.value = AiTaskState.Loading("Writing $language storyboard scenes with $engineTag…")
            val planResult = aiService.generateScriptToVideoPlan(
                scriptOrTopic = scriptOrTopic,
                language = language,
                lowDataMode = useLite,
                onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
            )
            val plan = planResult.getOrElse { err ->
                _aiTaskState.value = AiTaskState.Error(err.message ?: "Script generation failed.")
                return@launch
            }

            var generatedVisualUri: String? = null
            if (generateAiVisualCover) {
                val firstPrompt = plan.scenes.firstOrNull()?.visualPrompt?.ifBlank { scriptOrTopic } ?: scriptOrTopic
                _aiTaskState.value = AiTaskState.Loading("Generating AI hero scene visual…")
                val imgRes = aiService.generateAiImageToLocalFile(
                    prompt = firstPrompt,
                    style = AiImageStyle.CINEMATIC_8K,
                    aspectLabel = aspectRatio.label,
                    lowDataMode = useLite,
                    onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
                )
                generatedVisualUri = imgRes.getOrNull()?.toString()
            }

            val transitions = listOf(
                TransitionType.WHIP_PAN_LEFT,
                TransitionType.ZOOM_IN_BLUR,
                TransitionType.FLASH_WHITE,
                TransitionType.GLITCH_ZOOM
            )
            val motions = listOf(
                AiCameraMotion.SLOW_ZOOM,
                AiCameraMotion.DRONE_PAN,
                AiCameraMotion.PARALLAX_3D,
                AiCameraMotion.PARTICLE_FLOW
            )
            var cursorMs = 0L
            val clips = mutableListOf<TimelineClip>()

            plan.scenes.forEachIndexed { idx, scene ->
                val durMs = (scene.durationSec.coerceIn(2, 6) * 1000L)
                clips += TimelineClip(
                    trackType = TrackType.MAIN_VIDEO,
                    label = "${idx + 1}. ${scene.title.take(14)}",
                    startMs = cursorMs,
                    durationMs = durMs,
                    sourceDurationMs = durMs,
                    mediaUri = generatedVisualUri,
                    drawableKey = if (generatedVisualUri == null) null else null,
                    isVideoMedia = false,
                    accentColorHex = if (idx % 2 == 0) 0xFF9B5CFF else 0xFF00F0FF,
                    keyframes = buildCameraMotionKeyframes(motions[idx % motions.size], durMs),
                    transitionIn = if (idx > 0) ClipTransition(transitions[idx % transitions.size], 420L) else ClipTransition()
                )
                clips += TimelineClip(
                    trackType = TrackType.TEXT_STICKER,
                    label = scene.caption.take(18),
                    startMs = cursorMs + 120L,
                    durationMs = (durMs - 220L).coerceAtLeast(1000L),
                    accentColorHex = 0xFFFFD84D,
                    textStyle = TextClipStyle(
                        text = scene.caption,
                        fontStyle = TextFontStyle.BEBAS_NEUE,
                        effectStyle = TextEffectStyle.OUTLINE_BOLD,
                        animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                        outAnimation = TextAnimOut.FADE_OUT,
                        textColorHex = 0xFFFFFFFF,
                        accentColorHex = if (idx % 2 == 0) 0xFFFFD84D else 0xFF00F0FF,
                        fontSizeSp = 32f
                    )
                )
                cursorMs += durMs
            }

            ensureProjectAndInsertClips(
                projectTitle = plan.projectTitle,
                aspectRatio = aspectRatio,
                newClips = clips,
                firstThumbUri = generatedVisualUri,
                toastMsg = "Built ${plan.scenes.size}-scene $language video on timeline!"
            )
            _aiTaskState.value = AiTaskState.Success("Generated ${plan.scenes.size} scenes & timed captions on timeline.")
        }
    }

    fun runImageToVideoAnimator(
        photoUri: Uri,
        motion: AiCameraMotion,
        aspectRatio: AspectRatioPreset
    ) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Preparing ${motion.label} animation from your photo…")
            val (localUri, name) = withContext(Dispatchers.IO) {
                frameEngine.persistPickedMediaLocally(photoUri)
            }
            val durMs = 5500L
            val keyframes = buildCameraMotionKeyframes(motion, durMs)
            val fx = when (motion) {
                AiCameraMotion.PARTICLE_FLOW -> VisualFxType.SPARKLE
                AiCameraMotion.PARALLAX_3D -> VisualFxType.DREAMY_BLOOM
                else -> null
            }
            val clip = TimelineClip(
                trackType = TrackType.MAIN_VIDEO,
                label = "${(name ?: "Photo").take(10)} (${motion.label})",
                startMs = 0L,
                durationMs = durMs,
                sourceDurationMs = durMs,
                mediaUri = localUri.toString(),
                isVideoMedia = false,
                accentColorHex = 0xFF00D4FF,
                keyframes = keyframes,
                animCombo = if (motion == AiCameraMotion.PARALLAX_3D) VideoAnimCombo.WAVE_SLIDE else VideoAnimCombo.NONE
            )
            ensureProjectAndInsertClips(
                projectTitle = "Photo Motion ${motion.label}",
                aspectRatio = aspectRatio,
                newClips = listOf(clip),
                firstThumbUri = localUri.toString(),
                visualFx = fx,
                toastMsg = "Animated photo with ${motion.label}!"
            )
            _aiTaskState.value = AiTaskState.Success("Animated your gallery photo with ${motion.label}.")
        }
    }

    fun runAiLyricsVisualizer(
        themeOrLyrics: String,
        useAiToWrite: Boolean,
        language: String,
        visualizerStyle: MusicVisualizerStyle,
        aspectRatio: AspectRatioPreset
    ) {
        if (themeOrLyrics.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Enter a song mood or paste lyrics first.")
            return
        }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val title: String
            val lines: List<String>
            if (useAiToWrite) {
                networkMonitor.refresh()
                if (networkStatus.value.isOffline) {
                    _aiTaskState.value = AiTaskState.Error("Offline: Switch off 'AI Compose' to paste your own lyrics and build a Music Visualizer 100% offline!")
                    return@launch
                }
                val useLite = networkStatus.value.useLiteOptimization
                _aiTaskState.value = AiTaskState.Loading(
                    if (useLite) "Composing $language karaoke lyrics (Gemini Lite)…" else "Composing $language karaoke lyrics with Gemini…"
                )
                val res = aiService.generateMusicLyrics(
                    themeOrMood = themeOrLyrics,
                    language = language,
                    lowDataMode = useLite,
                    onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
                )
                val plan = res.getOrElse { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "Lyrics generation failed.")
                    return@launch
                }
                title = plan.songTitle
                lines = plan.lines.filter { it.isNotBlank() }
            } else {
                title = "Lyrics Visualizer"
                lines = themeOrLyrics.lines().map { it.trim() }.filter { it.isNotBlank() }
            }

            if (lines.isEmpty()) {
                _aiTaskState.value = AiTaskState.Error("No lyric lines found.")
                return@launch
            }

            val lineDur = 2400L
            val totalDur = lines.size * lineDur
            val clips = mutableListOf<TimelineClip>()
            clips += TimelineClip(
                trackType = TrackType.AUDIO_MUSIC,
                label = "$title (124 BPM)",
                startMs = 0L,
                durationMs = totalDur,
                sourceDurationMs = totalDur,
                accentColorHex = 0xFF10E896,
                waveformPeaks = StudioCatalog.generateWaveform(seed = 124)
            )
            lines.forEachIndexed { idx, line ->
                clips += TimelineClip(
                    trackType = TrackType.TEXT_STICKER,
                    label = line.take(16),
                    startMs = idx * lineDur,
                    durationMs = lineDur - 80L,
                    accentColorHex = 0xFF00F0FF,
                    textStyle = TextClipStyle(
                        text = line,
                        fontStyle = TextFontStyle.SYNE,
                        effectStyle = TextEffectStyle.NEON_GLOW,
                        animationType = TextAnimationType.KARAOKE_HIGHLIGHT,
                        outAnimation = TextAnimOut.FADE_OUT,
                        loopAnimation = TextAnimLoop.HEARTBEAT_PULSE,
                        textColorHex = 0xFFFFFFFF,
                        accentColorHex = if (idx % 2 == 0) 0xFF00F0FF else 0xFFFF2E93,
                        fontSizeSp = 30f
                    )
                )
            }
            val beats = (0L until totalDur step 600L).toList()
            ensureProjectAndInsertClips(
                projectTitle = title,
                aspectRatio = aspectRatio,
                newClips = clips,
                visualizer = visualizerStyle.takeIf { it != MusicVisualizerStyle.NONE } ?: MusicVisualizerStyle.NEON_BARS,
                beatMarkers = beats,
                toastMsg = "Created ${lines.size}-line Music Visualizer!"
            )
            _aiTaskState.value = AiTaskState.Success("Synced ${lines.size} karaoke lines with ${visualizerStyle.label}.")
        }
    }

    fun runAiTalkingPhotoOrPromo(
        brief: String,
        isTalkingPhoto: Boolean,
        pickedPhotoUri: Uri?,
        language: String,
        aspectRatio: AspectRatioPreset
    ) {
        if (brief.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Enter your talking script or product promo brief.")
            return
        }
        networkMonitor.refresh()
        if (networkStatus.value.isOffline) {
            _aiTaskState.value = AiTaskState.Error("Offline: Connect internet for AI Promo/Avatar scripting. Photo Animate & full editing work offline!")
            return
        }
        val useLite = networkStatus.value.useLiteOptimization
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val modeLabel = if (isTalkingPhoto) "Talking Photo Avatar" else "High-Converting Promo Ad"
            _aiTaskState.value = AiTaskState.Loading(
                if (useLite) "Directing $modeLabel ($language • Lite)…" else "Directing $modeLabel ($language) with Gemini…"
            )
            val planResult = aiService.generatePromoOrTalkingPlan(
                productOrTopic = brief,
                modeLabel = modeLabel,
                language = language,
                lowDataMode = useLite,
                onRetryStatus = { msg -> _aiTaskState.value = AiTaskState.Loading(msg) }
            )
            val plan = planResult.getOrElse { err ->
                _aiTaskState.value = AiTaskState.Error(err.message ?: "Promo script generation failed.")
                return@launch
            }

            val localPhotoUri = if (pickedPhotoUri != null) {
                withContext(Dispatchers.IO) { frameEngine.persistPickedMediaLocally(pickedPhotoUri).first.toString() }
            } else null

            var cursorMs = 0L
            val clips = mutableListOf<TimelineClip>()
            plan.scenes.forEachIndexed { idx, scene ->
                val durMs = (scene.durationSec.coerceIn(2, 5) * 1000L)
                if (localPhotoUri != null) {
                    clips += TimelineClip(
                        trackType = TrackType.MAIN_VIDEO,
                        label = if (isTalkingPhoto) "Avatar ${idx + 1}" else "Ad Scene ${idx + 1}",
                        startMs = cursorMs,
                        durationMs = durMs,
                        sourceDurationMs = durMs,
                        mediaUri = localPhotoUri,
                        isVideoMedia = false,
                        accentColorHex = 0xFFFF9F1C,
                        keyframes = listOf(
                            Keyframe(timeOffsetMs = 0L, scale = 1.04f),
                            Keyframe(timeOffsetMs = durMs / 2L, scale = 1.12f, offsetY = if (isTalkingPhoto) -0.02f else 0f),
                            Keyframe(timeOffsetMs = durMs, scale = 1.06f)
                        ),
                        animCombo = if (isTalkingPhoto) VideoAnimCombo.BOUNCE_1 else VideoAnimCombo.BEAT_FLASH_ZOOM
                    )
                }
                clips += TimelineClip(
                    trackType = TrackType.TEXT_STICKER,
                    label = scene.headline.take(18),
                    startMs = cursorMs + 80L,
                    durationMs = (durMs - 140L).coerceAtLeast(1000L),
                    accentColorHex = 0xFFFFD84D,
                    textStyle = TextClipStyle(
                        text = "${scene.headline}\n${scene.subtext}".trim(),
                        fontStyle = TextFontStyle.BEBAS_NEUE,
                        effectStyle = TextEffectStyle.PILL_HIGHLIGHT,
                        animationType = TextAnimationType.POP,
                        outAnimation = TextAnimOut.SHRINK_POP,
                        textColorHex = 0xFF0A0A0A,
                        accentColorHex = if (idx % 2 == 0) 0xFFFFD84D else 0xFF00F0FF,
                        fontSizeSp = 26f
                    )
                )
                cursorMs += durMs
            }

            ensureProjectAndInsertClips(
                projectTitle = plan.campaignTitle,
                aspectRatio = aspectRatio,
                newClips = clips,
                firstThumbUri = localPhotoUri,
                toastMsg = "Created ${plan.scenes.size}-scene $modeLabel!"
            )
            _aiTaskState.value = AiTaskState.Success("Generated ${plan.scenes.size} $modeLabel scenes on timeline.")
        }
    }

    fun generateExportViralKit(language: String = "English") {
        val doc = _editorState.value.document ?: return
        networkMonitor.refresh()
        if (networkStatus.value.isOffline) {
            _viralKitState.value = AiTaskState.Error("Offline: You can still render & save your 4K MP4 offline! Connect internet for AI Titles & Hashtags.")
            return
        }
        val useLite = networkStatus.value.useLiteOptimization
        viralKitJob?.cancel()
        viralKitJob = viewModelScope.launch {
            _viralKitState.value = AiTaskState.Loading(
                if (useLite) "Generating Viral Kit (Gemini Lite Low-Net)…" else "Generating Viral Titles, Captions & Hashtags with Gemini…"
            )
            val captions = doc.clips
                .filter { it.trackType == TrackType.TEXT_STICKER }
                .mapNotNull { it.textStyle?.text }
                .filter { it.isNotBlank() }
            val res = aiService.generateViralKit(
                projectTitle = doc.title,
                aspectLabel = doc.aspectRatio.label,
                captionTexts = captions,
                language = language,
                lowDataMode = useLite,
                onRetryStatus = { msg -> _viralKitState.value = AiTaskState.Loading(msg) }
            )
            res.fold(
                onSuccess = { kit -> _viralKitState.value = AiTaskState.Success(kit) },
                onFailure = { err -> _viralKitState.value = AiTaskState.Error(err.message ?: "Viral Kit generation failed.") }
            )
        }
    }

    private fun buildCameraMotionKeyframes(motion: AiCameraMotion, durMs: Long): List<Keyframe> = when (motion) {
        AiCameraMotion.SLOW_ZOOM -> listOf(
            Keyframe(timeOffsetMs = 0L, scale = 1.0f, easing = KeyframeEasing.EASE_IN_OUT),
            Keyframe(timeOffsetMs = durMs, scale = 1.32f, easing = KeyframeEasing.EASE_IN_OUT)
        )
        AiCameraMotion.DRONE_PAN -> listOf(
            Keyframe(timeOffsetMs = 0L, scale = 1.24f, offsetX = -0.16f, easing = KeyframeEasing.EASE_IN_OUT),
            Keyframe(timeOffsetMs = durMs, scale = 1.24f, offsetX = 0.16f, easing = KeyframeEasing.EASE_IN_OUT)
        )
        AiCameraMotion.PARALLAX_3D -> listOf(
            Keyframe(timeOffsetMs = 0L, scale = 1.12f, offsetX = -0.08f, offsetY = 0.04f, rotationDeg = -2.5f),
            Keyframe(timeOffsetMs = durMs / 2L, scale = 1.26f, offsetX = 0.02f, offsetY = -0.04f, rotationDeg = 1.8f),
            Keyframe(timeOffsetMs = durMs, scale = 1.18f, offsetX = 0.10f, offsetY = 0.02f, rotationDeg = -1.2f)
        )
        AiCameraMotion.PARTICLE_FLOW -> listOf(
            Keyframe(timeOffsetMs = 0L, scale = 1.06f, offsetY = 0.05f),
            Keyframe(timeOffsetMs = durMs, scale = 1.28f, offsetY = -0.05f)
        )
    }

    // ---- Real Watermark-Free 4K MP4 Export ----

    fun startMp4Export() {
        val doc = _editorState.value.document ?: return
        stopPlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            exporter.exportProject(doc) { progress -> _exportState.value = progress }
        }
    }

    fun saveExportedFileToUri(destinationUri: Uri, onFinished: (Boolean) -> Unit) {
        val filePath = _exportState.value.exportedFilePath
        if (filePath.isNullOrBlank()) {
            onFinished(false)
            return
        }
        viewModelScope.launch {
            val ok = exporter.copyExportToUri(filePath, destinationUri)
            onFinished(ok)
        }
    }

    fun cancelOrResetExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportProgressState()
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        aiJob?.cancel()
        viralKitJob?.cancel()
        networkMonitor.stop()
        audioEngine.release()
    }
}
