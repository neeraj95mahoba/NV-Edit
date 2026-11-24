package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ProjectSummary
import com.example.media.MediaFrameEngine
import com.example.model.AspectRatioPreset
import com.example.model.EditorToolTab
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.network.NetworkStatus
import com.example.ui.components.AiLabMode
import com.example.ui.components.AspectRatioGlyph
import com.example.ui.components.DeleteProjectConfirmDialog
import com.example.ui.components.HeroFilmCutIcon
import com.example.ui.components.HomeAiToolType
import com.example.ui.components.NetworkResilienceBanner
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.NvEditBrandEmblem
import com.example.ui.components.PrivacyAndStoreInfoDialog
import com.example.ui.components.RenameProjectDialog
import com.example.ui.components.StudioSettingsSheetDialog
import com.example.ui.components.StudioToolGlyph
import com.example.ui.components.springPressClickable
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioThemeMode
import com.example.ui.theme.TrackAudioAccent
import com.example.ui.theme.WarmAmber
import java.util.Locale

private enum class ProjectSortMode(val label: String) {
    RECENT("Recent"),
    LONGEST("Longest"),
    TITLE("A–Z");

    fun next(): ProjectSortMode = entries[(ordinal + 1) % entries.size]
}

@Composable
fun ProjectsTab(
    projects: List<ProjectSummary>,
    frameEngine: MediaFrameEngine,
    themeMode: StudioThemeMode = StudioThemeMode.OLED_DARK,
    networkStatus: NetworkStatus = NetworkStatus(),
    onToggleLowDataMode: () -> Unit = {},
    onSelectThemeMode: (StudioThemeMode) -> Unit = {},
    onOpenAiLab: (AiLabMode) -> Unit = {},
    onOpenQuickTool: (EditorToolTab) -> Unit = {},
    onJumpToAiLabTab: () -> Unit = {},
    onCreateProject: (
        title: String,
        aspectRatio: AspectRatioPreset,
        resolution: ExportResolution,
        fps: ExportFps,
        initialMediaUris: List<Uri>
    ) -> Unit,
    onOpenProject: (String) -> Unit,
    onExportProject: (String) -> Unit = {},
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val haptics = LocalHapticFeedback.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAspectFilter by remember { mutableStateOf<AspectRatioPreset?>(null) }
    var sortMode by remember { mutableStateOf(ProjectSortMode.RECENT) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var renamingProject by remember { mutableStateOf<ProjectSummary?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectSummary?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val filteredProjects by remember(projects, selectedAspectFilter, sortMode, searchQuery) {
        derivedStateOf {
            val base = projects.filter { project ->
                val matchesAspect = selectedAspectFilter == null || project.aspectRatio == selectedAspectFilter
                val matchesSearch = searchQuery.isBlank() ||
                    project.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    project.aspectRatio.label.contains(searchQuery.trim(), ignoreCase = true)
                matchesAspect && matchesSearch
            }
            when (sortMode) {
                ProjectSortMode.RECENT -> base.sortedByDescending { it.updatedAtMs }
                ProjectSortMode.LONGEST -> base.sortedByDescending { it.durationMs }
                ProjectSortMode.TITLE -> base.sortedBy { it.title.lowercase(Locale.US) }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("projects_dashboard_grid"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Minimal Flagship Header (Wordmark + Vector Emblem + PRO 4K Glass Pill + Search + Profile/Settings)
        item(span = { GridItemSpan(maxLineSpan) }) {
            StudioFlagshipHeader(
                searchVisible = searchVisible,
                hasNetworkAlert = networkStatus.isOffline || networkStatus.useLiteOptimization,
                onToggleSearch = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                },
                onOpenSettings = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showSettingsDialog = true
                }
            )
        }

        // 1b. Optional Expandable Search Bar
        if (searchVisible) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = searchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search drafts by name or aspect ratio…",
                                color = palette.textMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search),
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accentSecondary,
                            unfocusedBorderColor = palette.border,
                            focusedContainerColor = palette.surface,
                            unfocusedContainerColor = palette.surface,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("projects_search_input")
                    )
                }
            }
        }

        // 1c. Live Low-Network / Offline Resilience Banner (auto-shown only when offline or Lite Mode active)
        if (networkStatus.isOffline || networkStatus.useLiteOptimization) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NetworkResilienceBanner(
                    networkStatus = networkStatus,
                    onToggleLowDataMode = onToggleLowDataMode
                )
            }
        }

        // 2. Hero "+ New Project" Studio Action Card with Ambient Glow, Shimmer Border & Integrated Aspect Chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroNewProjectStudioCard(
                onClickNewProject = { showCreateDialog = true },
                onQuickStartAspect = { preset ->
                    val defaultName = when (preset) {
                        AspectRatioPreset.PORTRAIT_9_16 -> "Reel Edit"
                        AspectRatioPreset.LANDSCAPE_16_9 -> "YouTube Cut"
                        AspectRatioPreset.SQUARE_1_1 -> "Square Edit"
                        AspectRatioPreset.SOCIAL_4_5 -> "Portrait Feed"
                    }
                    onCreateProject(
                        "$defaultName ${projects.size + 1}",
                        preset,
                        ExportResolution.RES_4K,
                        ExportFps.FPS_60,
                        emptyList()
                    )
                }
            )
        }

        // 3. AI Studio Tools Strip (Horizontal Scrollable Frosted-Glass Pro Tiles)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Studio Tools",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "See all →",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onJumpToAiLabTab)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("home_see_all_ai_tools")
                    )
                }

                AiStudioToolsStrip(
                    onToolClick = { tool ->
                        when (tool) {
                            HomeAiToolType.TEXT_TO_VIDEO -> onOpenAiLab(AiLabMode.TEXT_TO_VIDEO)
                            HomeAiToolType.TEXT_TO_IMAGE -> onOpenAiLab(AiLabMode.TEXT_TO_IMAGE)
                            HomeAiToolType.AUTO_CAPTIONS -> onOpenQuickTool(EditorToolTab.CAPTIONS)
                            HomeAiToolType.BG_REMOVER -> onOpenQuickTool(EditorToolTab.CUTOUT)
                            HomeAiToolType.BEAT_SYNC -> onOpenQuickTool(EditorToolTab.AUDIO)
                            HomeAiToolType.AI_ENHANCER -> onOpenQuickTool(EditorToolTab.COLOR)
                        }
                    }
                )
            }
        }

        // 4. Saved Projects Section Header + Aspect Filter Chips + Sort Pill
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.projects_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = palette.surfaceElevated,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Text(
                                text = "${filteredProjects.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sortMode = sortMode.next()
                            }
                            .testTag("sort_projects_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.cd_sort_projects),
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = sortMode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                AspectFilterBar(
                    projects = projects,
                    selectedAspect = selectedAspectFilter,
                    onSelectAspect = { selectedAspectFilter = it }
                )
            }
        }

        // 5. Saved Projects Grid or Flagship Empty State
        if (filteredProjects.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProjectsEmptyStateCard(
                    hasAnyProjects = projects.isNotEmpty(),
                    onCreateNew = { showCreateDialog = true },
                    onClearFilters = {
                        selectedAspectFilter = null
                        searchQuery = ""
                    }
                )
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                ProjectDraftGridCard(
                    project = project,
                    frameEngine = frameEngine,
                    onOpen = { onOpenProject(project.id) },
                    onExport = { onExportProject(project.id) },
                    onRename = { renamingProject = project },
                    onDuplicate = { onDuplicateProject(project.id) },
                    onDelete = { deletingProject = project }
                )
            }
        }
    }

    if (showCreateDialog) {
        NewProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, ratio, res, fps, uris ->
                showCreateDialog = false
                onCreateProject(title, ratio, res, fps, uris)
            }
        )
    }

    renamingProject?.let { target ->
        RenameProjectDialog(
            currentTitle = target.title,
            onDismiss = { renamingProject = null },
            onConfirm = { newTitle ->
                renamingProject = null
                onRenameProject(target.id, newTitle)
            }
        )
    }

    deletingProject?.let { target ->
        DeleteProjectConfirmDialog(
            projectTitle = target.title,
            onDismiss = { deletingProject = null },
            onConfirmDelete = {
                deletingProject = null
                onDeleteProject(target.id)
            }
        )
    }

    if (showSettingsDialog) {
        StudioSettingsSheetDialog(
            themeMode = themeMode,
            networkStatus = networkStatus,
            onSelectTheme = onSelectThemeMode,
            onToggleLowDataMode = onToggleLowDataMode,
            onOpenPrivacyInfo = { showPrivacyDialog = true },
            onOpenAiLab = onJumpToAiLabTab,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyAndStoreInfoDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@Composable
private fun StudioFlagshipHeader(
    searchVisible: Boolean,
    hasNetworkAlert: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NvEditBrandEmblem(size = 34.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
            // Subtle Frosted-Glass "PRO 4K" Pill Badge
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = palette.surfaceElevated.copy(alpha = 0.9f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            ElectricPurple.copy(alpha = 0.55f),
                            NeonCyan.copy(alpha = 0.55f)
                        )
                    )
                )
            ) {
                Text(
                    text = stringResource(R.string.pro_4k_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (searchVisible) palette.accentPrimary.copy(alpha = 0.22f) else palette.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (searchVisible) palette.accentSecondary.copy(alpha = 0.65f) else palette.border
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleSearch)
                    .testTag("toggle_search_projects_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search_projects),
                        tint = if (searchVisible) palette.accentSecondary else palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box {
                Surface(
                    shape = CircleShape,
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings)
                        .testTag("home_studio_settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.cd_studio_settings),
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (hasNetworkAlert) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WarmAmber)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroNewProjectStudioCard(
    onClickNewProject: () -> Unit,
    onQuickStartAspect: (AspectRatioPreset) -> Unit
) {
    val palette = LocalStudioPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "heroShimmerBorder")
    val shimmerShift by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroBorderOffset"
    )

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Soft ambient studio obsidian-cyan-violet aura behind the hero card
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f, size.height * 0.35f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.65f),
                        radius = size.width * 0.58f
                    )
                )
            }
    ) {
        // Subtle Animated Shimmer Border Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 0.08f),
                            (shimmerShift - 0.18f).coerceIn(0f, 1f) to ElectricPurple.copy(alpha = 0.35f),
                            shimmerShift.coerceIn(0f, 1f) to NeonCyan.copy(alpha = 0.85f),
                            (shimmerShift + 0.18f).coerceIn(0f, 1f) to ElectricPurple.copy(alpha = 0.35f),
                            1.0f to Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(1.2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.8.dp))
                    .background(
                        if (palette.isLight) {
                            Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F4FF)))
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF161823),
                                    Color(0xFF11121A),
                                    Color(0xFF101924)
                                )
                            )
                        }
                    )
            ) {
                // Main Tactile "+ New Project" Tap Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springPressClickable(
                            pressedScale = 0.98f,
                            hapticType = HapticFeedbackType.LongPress,
                            onClick = onClickNewProject
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .testTag("new_project_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        HeroFilmCutIcon(size = 48.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = stringResource(R.string.action_plus_new_project),
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "4K 60fps • Multi-Track • Chroma Key • No Watermark",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonStudioGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_new_project),
                            tint = OledBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Hairline Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(palette.borderSubtle)
                )

                // Quick-Launch Aspect Ratio Chips Integrated Neatly Inside the Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AspectRatioPreset.entries.forEach { preset ->
                        val chipLabel = when (preset) {
                            AspectRatioPreset.PORTRAIT_9_16 -> "9:16 Reels"
                            AspectRatioPreset.LANDSCAPE_16_9 -> "16:9 YouTube"
                            AspectRatioPreset.SQUARE_1_1 -> "1:1"
                            AspectRatioPreset.SOCIAL_4_5 -> "4:5"
                        }
                        val accent = aspectAccentColor(preset)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .springPressClickable(pressedScale = 0.94f) {
                                    onQuickStartAspect(preset)
                                }
                                .testTag("quick_create_${preset.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AspectRatioGlyph(
                                    aspectRatio = preset,
                                    color = accent,
                                    boxSize = 12.dp
                                )
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiStudioToolsStrip(
    onToolClick: (HomeAiToolType) -> Unit
) {
    val palette = LocalStudioPalette.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_ai_studio_tools_strip")
    ) {
        items(HomeAiToolType.entries, key = { it.name }) { tool ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.glassBorderBrush),
                modifier = Modifier
                    .width(94.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .springPressClickable(pressedScale = 0.93f) { onToolClick(tool) }
                    .testTag("home_ai_tool_${tool.name}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        tool.primaryColor.copy(alpha = 0.16f),
                                        tool.secondaryColor.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = tool.primaryColor.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StudioToolGlyph(type = tool, size = 24.dp)
                    }

                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectFilterBar(
    projects: List<ProjectSummary>,
    selectedAspect: AspectRatioPreset?,
    onSelectAspect: (AspectRatioPreset?) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPillChip(
            label = "All (${projects.size})",
            selected = selectedAspect == null,
            accentColor = palette.accentSecondary,
            aspectGlyph = null,
            onClick = { onSelectAspect(null) },
            testTag = "filter_ratio_ALL"
        )
        AspectRatioPreset.entries.forEach { preset ->
            val count = projects.count { it.aspectRatio == preset }
            FilterPillChip(
                label = if (count > 0) "${preset.label} ($count)" else preset.label,
                selected = selectedAspect == preset,
                accentColor = aspectAccentColor(preset),
                aspectGlyph = preset,
                onClick = { onSelectAspect(if (selectedAspect == preset) null else preset) },
                testTag = "filter_ratio_${preset.name}"
            )
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    aspectGlyph: AspectRatioPreset?,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else palette.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 1.dp,
            color = if (selected) accentColor.copy(alpha = 0.75f) else palette.border
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (aspectGlyph != null) {
                AspectRatioGlyph(
                    aspectRatio = aspectGlyph,
                    color = if (selected) accentColor else palette.textSecondary,
                    boxSize = 12.dp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accentColor else palette.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProjectDraftGridCard(
    project: ProjectSummary,
    frameEngine: MediaFrameEngine,
    onOpen: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalStudioPalette.current
    var menuExpanded by remember { mutableStateOf(false) }
    val accent = aspectAccentColor(project.aspectRatio)

    val thumbnailBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = project.thumbnailUri,
        key2 = project.thumbnailDrawableKey
    ) {
        value = frameEngine.loadThumbnailBitmap(
            uriString = project.thumbnailUri,
            drawableKey = project.thumbnailDrawableKey,
            maxDim = 420
        )
    }

    val previewRatio = when (project.aspectRatio) {
        AspectRatioPreset.LANDSCAPE_16_9 -> 1.28f
        AspectRatioPreset.PORTRAIT_9_16 -> 0.92f
        else -> 1.06f
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.glassBorderBrush),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .springPressClickable(pressedScale = 0.97f, onClick = onOpen)
            .testTag("project_card_${project.id}")
    ) {
        Column {
            // Proportional Sleek Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewRatio)
                    .background(palette.surfaceHigh)
            ) {
                val bmp = thumbnailBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_project_thumb),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF131520), Color(0xFF1A172B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MovieCreation,
                                contentDescription = stringResource(R.string.cd_project_thumb),
                                tint = palette.textMuted,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Blank Draft",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                        }
                    }
                }

                // Subtle Cinematic Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.42f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.84f)
                                )
                            )
                        )
                )

                // Top-Left: Aspect Ratio Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OledBlack.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .testTag("project_aspect_tag_${project.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AspectRatioGlyph(
                            aspectRatio = project.aspectRatio,
                            color = accent,
                            boxSize = 10.dp
                        )
                        Text(
                            text = project.aspectRatio.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Top-Right: Resolution / FPS Tag (e.g. 4K · 60fps)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OledBlack.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, ProGold.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = project.resolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ProGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Bottom-Left: Duration Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OledBlack.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .testTag("project_duration_tag_${project.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatShortDuration(project.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom-Right: Glass Play Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(OledBlack.copy(alpha = 0.75f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Metadata Footer (Title, File Size, Clips, Relative Time + 3-Dot Menu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${formatFileSizeMb(project.estimatedSizeMb)} • ${formatRelativeTime(project.updatedAtMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("project_menu_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.cd_project_options),
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = palette.surfaceElevated
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open in Editor", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = palette.accentSecondary)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export 4K", color = ProGold) },
                            leadingIcon = {
                                Icon(Icons.Filled.IosShare, contentDescription = null, tint = ProGold)
                            },
                            onClick = {
                                menuExpanded = false
                                onExport()
                            },
                            modifier = Modifier.testTag("export_project_${project.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.textSecondary)
                            },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                            modifier = Modifier.testTag("duplicate_project_${project.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.textSecondary)
                            },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                            modifier = Modifier.testTag("rename_project_${project.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = CoralRed) },
                            leadingIcon = {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = CoralRed)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            modifier = Modifier.testTag("delete_project_${project.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsEmptyStateCard(
    hasAnyProjects: Boolean,
    onCreateNew: () -> Unit,
    onClearFilters: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.glassBorderBrush),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricPurple.copy(alpha = 0.20f), NeonCyan.copy(alpha = 0.14f))
                        )
                    )
                    .border(1.dp, palette.glassBorderBrush, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (hasAnyProjects) "No matching projects" else "Your studio canvas awaits",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (hasAnyProjects) {
                    "Clear your aspect ratio filter or search text to view all saved drafts."
                } else {
                    "Tap + New Project above or pick a 9:16 / 16:9 preset to launch the 4K 60fps timeline."
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = if (hasAnyProjects) onClearFilters else onCreateNew,
                modifier = Modifier.testTag("projects_empty_action_button")
            ) {
                Text(
                    text = if (hasAnyProjects) "Reset Filters" else "+ Create First Project",
                    color = palette.accentSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun aspectAccentColor(preset: AspectRatioPreset): Color = when (preset) {
    AspectRatioPreset.PORTRAIT_9_16 -> NeonCyan
    AspectRatioPreset.LANDSCAPE_16_9 -> ElectricPurple
    AspectRatioPreset.SQUARE_1_1 -> WarmAmber
    AspectRatioPreset.SOCIAL_4_5 -> TrackAudioAccent
}

fun formatShortDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

private fun formatFileSizeMb(sizeMb: Float): String {
    return if (sizeMb >= 100f) {
        String.format(Locale.US, "%.0f MB", sizeMb)
    } else {
        String.format(Locale.US, "%.1f MB", sizeMb)
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val diffMin = ((System.currentTimeMillis() - timestampMs) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMin < 1 -> "Just now"
        diffMin < 60 -> "${diffMin}m ago"
        diffMin < 1440 -> "${diffMin / 60}h ago"
        else -> "${diffMin / 1440}d ago"
    }
}
