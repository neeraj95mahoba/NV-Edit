package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiLabMode
import com.example.ui.components.AiVideoLabDialog
import com.example.ui.theme.FloatingNavGlass
import com.example.ui.theme.LocalStudioPalette
import com.example.viewmodel.HomeTab
import com.example.viewmodel.StudioViewModel

@Composable
fun HomeShell(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val activeTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val storyboards by viewModel.storyboards.collectAsStateWithLifecycle()
    val insightsPeriod by viewModel.insightsPeriod.collectAsStateWithLifecycle()
    val insightsData by viewModel.insightsData.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()

    var aiLabLaunchMode by remember { mutableStateOf<AiLabMode?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val useRail = maxWidth >= 660.dp

        if (useRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                NavigationRail(
                    containerColor = palette.surface,
                    contentColor = palette.textPrimary
                ) {
                    HomeTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectHomeTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tabIcon(tab, selected),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = palette.accentSecondary,
                                selectedTextColor = palette.accentSecondary,
                                indicatorColor = palette.accentPrimary.copy(alpha = 0.24f),
                                unselectedIconColor = palette.textMuted,
                                unselectedTextColor = palette.textMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1160.dp)
                    ) {
                        HomeTabBody(
                            activeTab = activeTab,
                            themeMode = themeMode,
                            networkStatus = networkStatus,
                            projects = projects,
                            storyboards = storyboards,
                            insightsPeriod = insightsPeriod,
                            insightsData = insightsData,
                            viewModel = viewModel,
                            onOpenAiLab = { mode ->
                                viewModel.resetAiTaskState()
                                aiLabLaunchMode = mode
                            }
                        )
                    }
                }
            }
        } else {
            // Compact / Handheld Layout with Floating Glass Pill Bottom Navigation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .statusBarsPadding()
            ) {
                HomeTabBody(
                    activeTab = activeTab,
                    themeMode = themeMode,
                    networkStatus = networkStatus,
                    projects = projects,
                    storyboards = storyboards,
                    insightsPeriod = insightsPeriod,
                    insightsData = insightsData,
                    viewModel = viewModel,
                    onOpenAiLab = { mode ->
                        viewModel.resetAiTaskState()
                        aiLabLaunchMode = mode
                    }
                )

                // Soft Bottom Scrim behind Floating Pill for Legibility over Scrolling Cards
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.background.copy(alpha = 0.85f),
                                    palette.background
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingGlassPillNavBar(
                        activeTab = activeTab,
                        onSelectTab = { viewModel.selectHomeTab(it) }
                    )
                }
            }
        }
    }

    aiLabLaunchMode?.let { mode ->
        AiVideoLabDialog(
            initialMode = mode,
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
            onDismiss = { aiLabLaunchMode = null }
        )
    }
}

@Composable
private fun FloatingGlassPillNavBar(
    activeTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit
) {
    val palette = LocalStudioPalette.current
    val haptic = LocalHapticFeedback.current
    val pillShape = RoundedCornerShape(28.dp)

    Surface(
        shape = pillShape,
        color = if (palette.isLight) palette.glassSurface else FloatingNavGlass,
        border = BorderStroke(1.dp, palette.glassBorderBrush),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .shadow(
                elevation = 18.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.65f),
                spotColor = palette.accentPrimary.copy(alpha = 0.25f)
            )
            .testTag("floating_bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val pillBg by animateColorAsState(
                    targetValue = if (selected) palette.accentPrimary.copy(alpha = 0.22f) else Color.Transparent,
                    label = "navPillBg_${tab.name}"
                )
                val pillBorderColor by animateColorAsState(
                    targetValue = if (selected) palette.accentSecondary.copy(alpha = 0.45f) else Color.Transparent,
                    label = "navPillBorder_${tab.name}"
                )
                val contentTint by animateColorAsState(
                    targetValue = if (selected) palette.accentSecondary else palette.textMuted,
                    label = "navPillTint_${tab.name}"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navPillScale_${tab.name}"
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = pillBg,
                    border = if (selected) BorderStroke(1.dp, pillBorderColor) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(tab)
                            }
                        }
                        .testTag("nav_tab_${tab.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = tabIcon(tab, selected),
                            contentDescription = tab.label,
                            tint = contentTint,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) palette.textPrimary else palette.textMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabBody(
    activeTab: HomeTab,
    themeMode: com.example.ui.theme.StudioThemeMode,
    networkStatus: com.example.network.NetworkStatus,
    projects: List<com.example.data.ProjectSummary>,
    storyboards: List<com.example.model.StoryboardScene>,
    insightsPeriod: String,
    insightsData: com.example.model.CreatorInsightsData,
    viewModel: StudioViewModel,
    onOpenAiLab: (AiLabMode) -> Unit
) {
    when (activeTab) {
        HomeTab.PROJECTS -> ProjectsTab(
            projects = projects,
            frameEngine = viewModel.frameEngine,
            themeMode = themeMode,
            networkStatus = networkStatus,
            onToggleLowDataMode = { viewModel.toggleLowDataMode() },
            onSelectThemeMode = { viewModel.setThemeMode(it) },
            onOpenAiLab = onOpenAiLab,
            onOpenQuickTool = { toolTab -> viewModel.openQuickToolInEditor(toolTab) },
            onJumpToAiLabTab = { viewModel.selectHomeTab(HomeTab.AI_LAB) },
            onCreateProject = { title, ratio, res, fps, uris ->
                viewModel.createNewProject(
                    title = title,
                    aspectRatio = ratio,
                    resolution = res,
                    fps = fps,
                    initialMediaUris = uris
                )
            },
            onOpenProject = { viewModel.openProjectById(it) },
            onExportProject = { viewModel.openProjectForExport(it) },
            onRenameProject = { id, newTitle -> viewModel.renameProject(id, newTitle) },
            onDuplicateProject = { viewModel.duplicateProject(it) },
            onDeleteProject = { viewModel.deleteProject(it) }
        )
        HomeTab.TEMPLATES -> TemplatesTab(
            onUseTemplate = { viewModel.useTemplate(it) }
        )
        HomeTab.AI_LAB -> AiLabTab(
            networkStatus = networkStatus,
            scenes = storyboards,
            onToggleLowDataMode = { viewModel.toggleLowDataMode() },
            onOpenAiLab = onOpenAiLab,
            onSaveScene = { viewModel.saveStoryboardScene(it) },
            onToggleFilmed = { viewModel.toggleStoryboardFilmed(it) },
            onMoveScene = { idx, delta -> viewModel.moveStoryboardScene(idx, delta) },
            onDeleteScene = { viewModel.deleteStoryboardScene(it) },
            onConvertToTimeline = { viewModel.convertStoryboardToProject() }
        )
        HomeTab.INSIGHTS -> InsightsTab(
            selectedPeriod = insightsPeriod,
            insights = insightsData,
            projectCount = projects.size,
            totalLibraryDurationMs = projects.sumOf { it.durationMs },
            onSelectPeriod = { viewModel.selectInsightsPeriod(it) },
            onUseTrendingAudio = { viewModel.useTrendingAudioInNewProject(it) }
        )
    }
}

private fun tabIcon(tab: HomeTab, selected: Boolean) = when (tab) {
    HomeTab.PROJECTS -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    HomeTab.TEMPLATES -> if (selected) Icons.Filled.MovieFilter else Icons.Outlined.MovieFilter
    HomeTab.AI_LAB -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    HomeTab.INSIGHTS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
}
