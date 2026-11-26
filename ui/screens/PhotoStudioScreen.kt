package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.LocalStudioAdController
import com.example.model.PhotoStudioToolTab
import com.example.ui.components.PhotoPreviewSplitCanvas
import com.example.ui.components.PhotoStudioExportDialog
import com.example.ui.components.PhotoStudioToolPanelContent
import com.example.ui.i18n.LocalStudioStrings
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay

@Composable
fun PhotoStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val strings = LocalStudioStrings.current
    val context = LocalContext.current
    val adController = LocalStudioAdController.current
    val adState by adController.state.collectAsStateWithLifecycle()
    val uiState by viewModel.photoStudioState.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker -> triggers once-per-session Interstitial Ad
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            adController.showMediaImportInterstitialOncePerSession("Photo Studio Import") {
                viewModel.importPhotoToStudio(pickedUri)
            }
        }
    }

    LaunchedEffect(uiState.statusBanner) {
        if (uiState.statusBanner != null) {
            delay(3200L)
            viewModel.clearPhotoBanner()
        }
    }

    fun handleSelectMultiplier(multiplier: com.example.model.PhotoUpscaleMultiplier) {
        if (multiplier in uiState.unlockedMultipliers || multiplier.requiredRewardedAds == 0) {
            viewModel.applyUnlockedPhotoMultiplier(multiplier)
        } else {
            adController.showRewardedAdSequence(
                actionLabel = "${multiplier.label} AI Super-Resolution",
                totalAds = multiplier.requiredRewardedAds,
                onAllAdsCompleted = {
                    viewModel.applyUnlockedPhotoMultiplier(multiplier)
                },
                onCancelled = {
                    Toast.makeText(
                        context,
                        "Watch ${multiplier.requiredRewardedAds} Rewarded Ad(s) to unlock ${multiplier.label} Ultra-HD.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("photo_studio_screen")
    ) {
        // Top Studio Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = strings.photoStudioTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = ElectricPurple.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "DSLR • 32K",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${uiState.sourceDisplayName} • ${strings.zeroWatermark}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("photo_studio_import_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Import Photo",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.pickPhoto,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.resetPhotoStudioAll() },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("photo_studio_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset Photo Studio Edits",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonStudioGradient)
                        .clickable {
                            viewModel.resetPhotoExportState()
                            showExportDialog = true
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                        .testTag("photo_studio_export_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.IosShare,
                            contentDescription = "Export Ultra-HD Photo",
                            tint = OledBlack,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = strings.export32k,
                            style = MaterialTheme.typography.labelSmall,
                            color = OledBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Transient feedback pill
        AnimatedVisibility(visible = uiState.statusBanner != null) {
            uiState.statusBanner?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricPurple.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clickable { viewModel.clearPhotoBanner() }
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Adaptive Body: Side-by-side on Tablets / Foldables, Stacked on Phones
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWide = maxWidth >= 700.dp

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PhotoPreviewSplitCanvas(
                        uiState = uiState,
                        photoEngine = viewModel.photoEngine,
                        onTapCanvas = { x, y -> viewModel.onPhotoCanvasTap(x, y) },
                        onBrushStroke = { x, y -> viewModel.addPhotoBrushStroke(x, y) },
                        onUpdateSplitPosition = { viewModel.updatePhotoSplitPosition(it) },
                        onToggleSplitCompare = { viewModel.togglePhotoSplitCompare() },
                        onSelectInteractionTool = { viewModel.setPhotoInteractionTool(it) },
                        onClearBrushAndHeals = { viewModel.clearPhotoBrushAndHeals() },
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        PhotoStudioTabBar(
                            activeTab = uiState.activeToolTab,
                            onSelectTab = { viewModel.setPhotoStudioToolTab(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp)
                        ) {
                            PhotoStudioToolPanelContent(
                                uiState = uiState,
                                isAdSequenceRunning = adState.rewardedSequence.isRunning,
                                onSelectMultiplier = ::handleSelectMultiplier,
                                onApplyDslrProLook = { viewModel.applyAiDslrAutoEnhance() },
                                onApplyAutoRawTone = { viewModel.applyAiAutoRawTone() },
                                onUpdateSettings = { viewModel.updatePhotoSettings(it) },
                                onUpdateColorGrading = { viewModel.updatePhotoColorGrading(it) },
                                onSelectInteractionTool = { viewModel.setPhotoInteractionTool(it) },
                                onClearBrushAndHeals = { viewModel.clearPhotoBrushAndHeals() }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    PhotoPreviewSplitCanvas(
                        uiState = uiState,
                        photoEngine = viewModel.photoEngine,
                        onTapCanvas = { x, y -> viewModel.onPhotoCanvasTap(x, y) },
                        onBrushStroke = { x, y -> viewModel.addPhotoBrushStroke(x, y) },
                        onUpdateSplitPosition = { viewModel.updatePhotoSplitPosition(it) },
                        onToggleSplitCompare = { viewModel.togglePhotoSplitCompare() },
                        onSelectInteractionTool = { viewModel.setPhotoInteractionTool(it) },
                        onClearBrushAndHeals = { viewModel.clearPhotoBrushAndHeals() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(265.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    PhotoStudioTabBar(
                        activeTab = uiState.activeToolTab,
                        onSelectTab = { viewModel.setPhotoStudioToolTab(it) }
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 104.dp)
                    ) {
                        PhotoStudioToolPanelContent(
                            uiState = uiState,
                            isAdSequenceRunning = adState.rewardedSequence.isRunning,
                            onSelectMultiplier = ::handleSelectMultiplier,
                            onApplyDslrProLook = { viewModel.applyAiDslrAutoEnhance() },
                            onApplyAutoRawTone = { viewModel.applyAiAutoRawTone() },
                            onUpdateSettings = { viewModel.updatePhotoSettings(it) },
                            onUpdateColorGrading = { viewModel.updatePhotoColorGrading(it) },
                            onSelectInteractionTool = { viewModel.setPhotoInteractionTool(it) },
                            onClearBrushAndHeals = { viewModel.clearPhotoBrushAndHeals() }
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        PhotoStudioExportDialog(
            uiState = uiState,
            onSelectResolution = { viewModel.selectPhotoExportResolution(it) },
            onSelectFormat = { viewModel.selectPhotoExportFormat(it) },
            onSelectDpi = { viewModel.selectPhotoExportDpi(it) },
            onStartExport = { viewModel.startUltraPhotoExport() },
            onSaveToDeviceUri = { uri, cb -> viewModel.savePhotoExportToUri(uri, cb) },
            onDismiss = {
                viewModel.resetPhotoExportState()
                showExportDialog = false
            }
        )
    }
}

@Composable
private fun PhotoStudioTabBar(
    activeTab: PhotoStudioToolTab,
    onSelectTab: (PhotoStudioToolTab) -> Unit
) {
    val palette = LocalStudioPalette.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(PhotoStudioToolTab.entries) { tab ->
            val selected = tab == activeTab
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) ElectricPurple.copy(alpha = 0.28f) else palette.surface,
                border = BorderStroke(
                    width = 1.dp,
                    brush = if (selected) {
                        Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan))
                    } else {
                        Brush.horizontalGradient(listOf(palette.border, palette.border))
                    }
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectTab(tab) }
                    .testTag("photo_tool_tab_${tab.name}")
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) NeonCyan else palette.textSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
