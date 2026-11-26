package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.LocalStudioAdController
import com.example.model.PhotoExportFormat
import com.example.model.PhotoExportResolution
import com.example.model.PhotoSpecCalculator
import com.example.model.PhotoStudioUiState
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import java.util.Locale

private val PHOTO_DPI_OPTIONS = listOf(72, 150, 300, 600)

@Composable
fun PhotoStudioExportDialog(
    uiState: PhotoStudioUiState,
    onSelectResolution: (PhotoExportResolution) -> Unit,
    onSelectFormat: (PhotoExportFormat) -> Unit,
    onSelectDpi: (Int) -> Unit,
    onStartExport: () -> Unit,
    onSaveToDeviceUri: (Uri, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val context = LocalContext.current
    val adController = LocalStudioAdController.current
    val adState by adController.state.collectAsStateWithLifecycle()
    val adSequence = adState.rewardedSequence
    val exportState = uiState.exportState

    var downloadedFeedback by remember(exportState.exportedFilePath) { mutableStateOf<String?>(null) }
    var interstitialTriggeredUri by remember { mutableStateOf<Uri?>(null) }

    // 1080p–4K exports start immediately with 0 pre-export ads and show 1 quick Interstitial when finished.
    // 8K (1 Ad), 16K (2 Ads), 32K (3 Ads) unlock sequentially before export and show only the Native Ad card on finish.
    LaunchedEffect(exportState.exportedShareUri) {
        val finishedUri = exportState.exportedShareUri
        if (finishedUri != null && interstitialTriggeredUri != finishedUri) {
            interstitialTriggeredUri = finishedUri
            if (uiState.selectedExportResolution.requiredRewardedAds == 0) {
                adController.showInterstitialAd(contextTitle = "Photo Export Complete")
            }
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(uiState.selectedExportFormat.mimeType)
    ) { destUri ->
        if (destUri != null) {
            onSaveToDeviceUri(destUri) { ok ->
                downloadedFeedback = if (ok) {
                    "Saved ${uiState.selectedExportResolution.label} ${uiState.selectedExportFormat.label} to storage!"
                } else {
                    "Could not write file to selected location."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!exportState.isExporting && !adSequence.isRunning) onDismiss()
        },
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ProGoldGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = OledBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "32K Photo Export",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProGoldGradient)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = OledBlack, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("ZERO WATERMARK", style = MaterialTheme.typography.labelSmall, color = OledBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val estimate = remember(
                    uiState.sourceWidthPx,
                    uiState.sourceHeightPx,
                    uiState.selectedExportResolution,
                    uiState.selectedExportFormat,
                    uiState.selectedExportDpi
                ) {
                    PhotoSpecCalculator.estimateExport(
                        sourceW = uiState.sourceWidthPx,
                        sourceH = uiState.sourceHeightPx,
                        resolution = uiState.selectedExportResolution,
                        format = uiState.selectedExportFormat,
                        dpi = uiState.selectedExportDpi
                    )
                }

                // Real-Time Ultra-HD Spec & Tiled Print Estimate Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_export_estimate_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ultra-Print Spec (${estimate.megapixelsLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AcidLime.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = uiState.selectedExportFormat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AcidLime,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = estimate.estimatedSizeLabel,
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${estimate.targetWidthPx}×${estimate.targetHeightPx}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Print: ${estimate.printInchesLabel} • Tiled: ${estimate.tileGridLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // Live Sequential Rewarded Ad Progress Card
                if (adSequence.isRunning) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ElectricPurple.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, ProGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_export_rewarded_progress")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = ProGold,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unlocking ${adSequence.resolutionLabel} (${adSequence.completedAds}/${adSequence.totalAds} Ads)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ProGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = adSequence.statusMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (step in 1..adSequence.totalAds.coerceAtLeast(1)) {
                                    val done = step <= adSequence.completedAds
                                    val activeStep = step == adSequence.currentAdNumber
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    done -> AcidLime
                                                    activeStep -> ProGold
                                                    else -> Color.White.copy(alpha = 0.16f)
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                if (!exportState.isExporting && exportState.exportedShareUri == null) {
                    Text(
                        text = stringResource(R.string.ad_photo_resolution_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(PhotoExportResolution.entries) { res ->
                            val active = uiState.selectedExportResolution == res
                            val isUltraRes = res.requiredRewardedAds > 0
                            val adBadge = res.adBadgeLabel
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) ElectricPurple.copy(alpha = 0.34f) else palette.surface,
                                border = BorderStroke(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = when {
                                        active -> ProGold
                                        isUltraRes -> ProGold.copy(alpha = 0.45f)
                                        else -> palette.border
                                    }
                                ),
                                modifier = Modifier
                                    .clickable(enabled = !adSequence.isRunning) { onSelectResolution(res) }
                                    .testTag("photo_export_res_${res.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active || isUltraRes) ProGold else palette.textPrimary,
                                        fontWeight = if (active || isUltraRes) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (adBadge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (active) ProGold else ProGold.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, ProGold.copy(alpha = 0.65f)),
                                            modifier = Modifier.testTag("photo_export_res_ad_badge_${res.name}")
                                        ) {
                                            Text(
                                                text = adBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (active) OledBlack else ProGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("File Format (Lossless & Ultra HDR)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PhotoExportFormat.entries.forEach { fmt ->
                            val active = uiState.selectedExportFormat == fmt
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (active) palette.accentSecondary.copy(alpha = 0.22f) else palette.surface,
                                border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectFormat(fmt) }
                                    .testTag("photo_export_fmt_${fmt.name}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = fmt.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) palette.accentSecondary else palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = fmt.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text("Print Density (DPI)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PHOTO_DPI_OPTIONS.forEach { dpi ->
                            val active = uiState.selectedExportDpi == dpi
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (active) ElectricPurple.copy(alpha = 0.30f) else palette.surface,
                                border = BorderStroke(1.dp, if (active) NeonCyan else palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectDpi(dpi) }
                                    .testTag("photo_export_dpi_$dpi")
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$dpi DPI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) NeonCyan else palette.textSecondary,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Tiled Rendering Progress
                if (exportState.isExporting) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exportState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(exportState.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { exportState.progress.coerceIn(0f, 1f) },
                                color = palette.accentSecondary,
                                trackColor = palette.surfaceHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                // Export Complete Card + Native Ad Card
                if (exportState.exportedShareUri != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AcidLime.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AcidLime, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = exportState.statusMessage,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AcidLime,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Saved to %s • %.2f MB • %d×%d master",
                                    exportState.savedGalleryPath ?: "Pictures/NVEdit",
                                    exportState.exportedFileSizeBytes / (1024f * 1024f),
                                    exportState.encodedWidthPx,
                                    exportState.encodedHeightPx
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { saveDocumentLauncher.launch(exportState.exportedFileName) },
                                    border = BorderStroke(1.dp, NeonCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("photo_export_save_as_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save As", color = NeonCyan)
                                }
                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = exportState.format.mimeType
                                            putExtra(Intent.EXTRA_STREAM, exportState.exportedShareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Ultra-HD Photo"))
                                        }.onFailure {
                                            Toast.makeText(context, "Unable to open Share sheet.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("photo_export_share_button")
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                            downloadedFeedback?.let { msg ->
                                Text(msg, style = MaterialTheme.typography.labelSmall, color = AcidLime)
                            }
                        }
                    }

                    if (adState.config.nativeCardOnExportComplete) {
                        NativeMediumRectangleAdCard(
                            placementTag = "photo_export_complete",
                            compactForDialog = true
                        )
                    }
                }

                exportState.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralRed.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CoralRed)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CoralRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = CoralRed)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!exportState.isExporting && exportState.exportedShareUri == null) {
                val res = uiState.selectedExportResolution
                val neededAds = res.requiredRewardedAds
                Button(
                    onClick = {
                        if (neededAds > 0) {
                            adController.showResolutionRewardedSequence(
                                resolutionLabel = "${res.label} Photo",
                                totalAds = neededAds,
                                onAllAdsCompleted = { onStartExport() },
                                onCancelled = {
                                    Toast.makeText(
                                        context,
                                        "Watch all $neededAds Rewarded Ads to unlock ${res.label} photo export.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        } else {
                            onStartExport()
                        }
                    },
                    enabled = !adSequence.isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (neededAds > 0) ProGold else palette.accentPrimary,
                        contentColor = if (neededAds > 0) OledBlack else Color.White
                    ),
                    modifier = Modifier.testTag("photo_export_start_button")
                ) {
                    if (adSequence.isRunning) {
                        CircularProgressIndicator(color = OledBlack, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ad ${adSequence.currentAdNumber} of ${adSequence.totalAds}…")
                    } else if (neededAds > 0) {
                        Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(res.exportCtaLabel, fontWeight = FontWeight.Bold)
                    } else {
                        Text(res.exportCtaLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (adSequence.isRunning) adController.cancelRewardedSequence()
                    onDismiss()
                },
                enabled = !exportState.isExporting
            ) {
                Text(if (exportState.exportedShareUri != null) "Done" else "Cancel", color = palette.textSecondary)
            }
        }
    )
}
