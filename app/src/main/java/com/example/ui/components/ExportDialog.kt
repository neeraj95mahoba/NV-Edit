package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiTaskState
import com.example.ai.ViralKitResult
import com.example.media.ExportProgressState
import com.example.model.ExportBitrate
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.VideoProjectDocument
import com.example.network.NetworkStatus
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    document: VideoProjectDocument,
    exportState: ExportProgressState,
    viralKitState: AiTaskState<ViralKitResult> = AiTaskState.Idle,
    networkStatus: NetworkStatus = NetworkStatus(),
    onToggleLowDataMode: () -> Unit = {},
    onGenerateViralKit: (language: String) -> Unit = {},
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectFps: (ExportFps) -> Unit,
    onSelectBitrate: (ExportBitrate) -> Unit,
    onStartExport: () -> Unit,
    onSaveToDeviceUri: (Uri, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val context = LocalContext.current
    var downloadedFeedback by remember(exportState.exportedFilePath) { mutableStateOf<String?>(null) }
    var viralLanguage by remember { mutableStateOf("English") }
    var copiedBanner by remember { mutableStateOf<String?>(null) }

    fun copyToSystemClipboard(label: String, text: String, bannerMsg: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedBanner = bannerMsg
        Toast.makeText(context, bannerMsg, Toast.LENGTH_SHORT).show()
    }

    // Native Android "Save As / Download" launcher -> saves real MP4 directly to Downloads or any folder
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { destUri ->
        if (destUri != null) {
            onSaveToDeviceUri(destUri) { ok ->
                downloadedFeedback = if (ok) {
                    "Downloaded to device storage successfully!"
                } else {
                    "Could not write file to selected location."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!exportState.isExporting) onDismiss()
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
                            imageVector = Icons.Filled.MovieCreation,
                            contentDescription = null,
                            tint = OledBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Export MP4", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
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
                        Text("PRO UNLOCKED", style = MaterialTheme.typography.labelSmall, color = OledBlack, fontWeight = FontWeight.Bold)
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ProGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ProGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Real H.264 MP4 • Offline Ready", style = MaterialTheme.typography.labelMedium, color = ProGold)
                        Text(document.aspectRatio.label, style = MaterialTheme.typography.labelSmall, color = palette.accentSecondary)
                    }
                }

                if (!exportState.isExporting && exportState.exportedShareUri == null) {
                    Text("Resolution (All Unlocked)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportResolution.entries.forEach { res ->
                            val active = document.resolution == res
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) ElectricPurple.copy(alpha = 0.30f) else palette.surface,
                                border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectResolution(res) }
                                    .testTag("export_res_${res.name}")
                            ) {
                                Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) ProGold else palette.textPrimary,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Frame Rate", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ExportFps.entries.forEach { fps ->
                                    val active = document.fps == fps
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) palette.accentSecondary.copy(alpha = 0.22f) else palette.surface,
                                        border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectFps(fps) }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(fps.label, style = MaterialTheme.typography.labelSmall, color = if (active) palette.accentSecondary else palette.textSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Bitrate", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ExportBitrate.entries.forEach { br ->
                                    val active = document.bitrate == br
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) ProGold.copy(alpha = 0.22f) else palette.surface,
                                        border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectBitrate(br) }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(br.label, style = MaterialTheme.typography.labelSmall, color = if (active) ProGold else palette.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                when {
                    exportState.isExporting -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exportState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${(exportState.progress * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ProGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { exportState.progress.coerceIn(0f, 1f) },
                                color = ProGold,
                                trackColor = palette.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    exportState.exportedShareUri != null -> {
                        val sizeMb = exportState.exportedFileSizeBytes / (1024f * 1024f)
                        val durSec = exportState.exportedDurationMs / 1000f
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AcidLime.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AcidLime)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Saved to Gallery!",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = exportState.savedGalleryPath ?: exportState.exportedFileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.2f MB • %d×%d • %.1fs MP4",
                                            sizeMb.coerceAtLeast(0.01f),
                                            exportState.exportedWidth,
                                            exportState.exportedHeight,
                                            durSec
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AcidLime
                                    )
                                }
                            }
                        }

                        if (!downloadedFeedback.isNullOrBlank()) {
                            Text(
                                text = downloadedFeedback!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentSecondary
                            )
                        }

                        Button(
                            onClick = {
                                saveDocumentLauncher.launch(exportState.exportedFileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProGold,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_video_file_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download Video File (.mp4)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val viralKit = (viralKitState as? AiTaskState.Success)?.data
                                val captionExtra = viralKit?.let { kit ->
                                    buildString {
                                        kit.youtubeTitles.firstOrNull()?.let { appendLine(it) }
                                        kit.captions.firstOrNull()?.let { appendLine(it) }
                                        if (kit.hashtags.isNotEmpty()) append(kit.hashtags.joinToString(" "))
                                    }.trim()
                                }.orEmpty()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, exportState.exportedShareUri)
                                    putExtra(Intent.EXTRA_SUBJECT, exportState.exportedFileName)
                                    if (captionExtra.isNotBlank()) {
                                        putExtra(Intent.EXTRA_TEXT, captionExtra)
                                    }
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Publish / Share NV Edit Video")
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_exported_video_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Publish / Share to Gallery & Social", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val playUri = exportState.savedGalleryUri ?: exportState.exportedShareUri
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(playUri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(viewIntent, "Play Exported MP4"))
                                }
                            },
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("play_exported_video_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open in Video Player", color = palette.textPrimary)
                        }
                    }
                    exportState.errorMessage != null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralRed.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CoralRed)
                                Spacer(Modifier.width(10.dp))
                                Text(exportState.errorMessage, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            }
                        }
                    }
                }

                // Built-in AI Viral Kit (YouTube Titles, IG/FB Captions & 12 Trending Hashtags)
                HorizontalDivider(color = palette.border)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Built-in Viral Kit", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("English", "Hindi").forEach { lang ->
                                    val selected = viralLanguage == lang
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (selected) palette.accentPrimary.copy(alpha = 0.25f) else palette.surfaceElevated,
                                        border = BorderStroke(1.dp, if (selected) palette.accentSecondary else palette.border),
                                        modifier = Modifier.clickable { viralLanguage = lang }
                                    ) {
                                        Text(
                                            text = if (lang == "Hindi") "हिन्दी" else "EN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) palette.accentSecondary else palette.textSecondary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        NetworkResilienceBanner(
                            networkStatus = networkStatus,
                            onToggleLowDataMode = onToggleLowDataMode
                        )

                        if (copiedBanner != null) {
                            Text(copiedBanner!!, style = MaterialTheme.typography.labelSmall, color = AcidLime)
                        }

                        when (viralKitState) {
                            AiTaskState.Idle -> {
                                OutlinedButton(
                                    onClick = { onGenerateViralKit(viralLanguage) },
                                    border = BorderStroke(1.dp, palette.accentSecondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_generate_viral_kit_button")
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accentSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generate Titles, Captions & Hashtags", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                }
                            }
                            is AiTaskState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = palette.accentSecondary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(viralKitState.status, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                            is AiTaskState.Error -> {
                                Text(viralKitState.message, style = MaterialTheme.typography.labelSmall, color = CoralRed)
                                TextButton(onClick = { onGenerateViralKit(viralLanguage) }) {
                                    Text("Retry Viral Kit", color = palette.accentSecondary)
                                }
                            }
                            is AiTaskState.Success -> {
                                val kit = viralKitState.data
                                if (kit.youtubeTitles.isNotEmpty()) {
                                    Text("YouTube / Shorts Titles", style = MaterialTheme.typography.labelSmall, color = ProGold, fontWeight = FontWeight.Bold)
                                    kit.youtubeTitles.forEach { title ->
                                        ViralCopyRow(text = title, onCopy = {
                                            copyToSystemClipboard("NV Edit Title", title, "Copied title to clipboard!")
                                        })
                                    }
                                }
                                if (kit.captions.isNotEmpty()) {
                                    Text("IG / FB Captions", style = MaterialTheme.typography.labelSmall, color = palette.accentSecondary, fontWeight = FontWeight.Bold)
                                    kit.captions.forEach { cap ->
                                        ViralCopyRow(text = cap, onCopy = {
                                            copyToSystemClipboard("NV Edit Caption", cap, "Copied caption to clipboard!")
                                        })
                                    }
                                }
                                if (kit.hashtags.isNotEmpty()) {
                                    val tagBundle = kit.hashtags.joinToString(" ")
                                    Text("Trending Hashtags (${kit.hashtags.size})", style = MaterialTheme.typography.labelSmall, color = AcidLime, fontWeight = FontWeight.Bold)
                                    ViralCopyRow(text = tagBundle, onCopy = {
                                        copyToSystemClipboard("NV Edit Hashtags", tagBundle, "Copied ${kit.hashtags.size} hashtags!")
                                    })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        val fullPost = buildString {
                                            kit.youtubeTitles.firstOrNull()?.let { appendLine(it) }
                                            kit.captions.firstOrNull()?.let { appendLine(); appendLine(it) }
                                            if (kit.hashtags.isNotEmpty()) {
                                                appendLine()
                                                append(kit.hashtags.joinToString(" "))
                                            }
                                        }.trim()
                                        if (fullPost.isNotBlank()) {
                                            copyToSystemClipboard("NV Edit Viral Post", fullPost, "Copied full post to clipboard!")
                                        }
                                    }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = ProGold, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy Full Post", style = MaterialTheme.typography.labelSmall, color = ProGold, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = { onGenerateViralKit(viralLanguage) }) {
                                        Text("Regenerate Kit", style = MaterialTheme.typography.labelSmall, color = palette.accentSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                exportState.isExporting -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = CoralRed)
                    }
                }
                exportState.exportedShareUri != null -> {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = palette.accentSecondary, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = OledBlack),
                        modifier = Modifier.testTag("start_render_export_button")
                    ) {
                        Text("Render & Save ${document.resolution.label} MP4", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!exportState.isExporting && exportState.exportedShareUri == null) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = palette.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun ViralCopyRow(
    text: String,
    onCopy: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy text",
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
