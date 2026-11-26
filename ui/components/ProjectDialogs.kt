package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ads.LocalStudioAdController
import com.example.model.AspectRatioPreset
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackAudioAccent

@Composable
fun AspectRatioGlyph(
    aspectRatio: AspectRatioPreset,
    color: Color,
    boxSize: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(boxSize)) {
        val maxW = size.width * 0.86f
        val maxH = size.height * 0.86f
        val ratio = aspectRatio.ratio
        val (w, h) = if (ratio >= 1f) {
            maxW to (maxW / ratio).coerceAtLeast(size.height * 0.42f)
        } else {
            (maxH * ratio).coerceAtLeast(size.width * 0.42f) to maxH
        }
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

@Composable
fun NewProjectDialog(
    initialResolution: ExportResolution = ExportResolution.RES_4K,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        aspectRatio: AspectRatioPreset,
        resolution: ExportResolution,
        fps: ExportFps,
        initialMediaUris: List<Uri>
    ) -> Unit
) {
    val adController = LocalStudioAdController.current
    var title by remember { mutableStateOf("My Studio Reel") }
    var selectedAspect by remember { mutableStateOf(AspectRatioPreset.PORTRAIT_9_16) }
    var selectedResolution by remember(initialResolution) { mutableStateOf(initialResolution) }
    var selectedFps by remember { mutableStateOf(ExportFps.FPS_60) }
    var pickedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedMediaUris = uris
            adController.showMediaImportInterstitialOncePerSession("New Project Media Import")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricPurple.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MovieCreation,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "New Studio Project",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "PRO UNLOCKED • Zero Watermark",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProGold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                // Canvas Aspect Ratio selector with proportional frame glyphs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Canvas Aspect Ratio",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            val active = preset == selectedAspect
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) ElectricPurple.copy(alpha = 0.26f) else StudioSurface,
                                border = BorderStroke(
                                    width = if (active) 1.4.dp else 1.dp,
                                    color = if (active) NeonCyan else StudioBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedAspect = preset }
                                    .testTag("aspect_choice_${preset.name}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    AspectRatioGlyph(
                                        aspectRatio = preset,
                                        color = if (active) NeonCyan else TextSecondary,
                                        boxSize = 18.dp
                                    )
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) NeonCyan else TextSecondary,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Resolution & FPS quick selectors
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Output Spec",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExportFps.entries.forEach { fpsOpt ->
                                val active = fpsOpt == selectedFps
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) NeonCyan.copy(alpha = 0.18f) else StudioSurface,
                                    border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedFps = fpsOpt }
                                        .testTag("new_project_fps_${fpsOpt.fps}")
                                ) {
                                    Text(
                                        text = fpsOpt.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) NeonCyan else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportResolution.entries.forEach { res ->
                            val active = res == selectedResolution
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) ElectricPurple.copy(alpha = 0.24f) else StudioSurface,
                                border = BorderStroke(1.dp, if (active) ElectricPurple else StudioBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedResolution = res }
                                    .testTag("new_project_res_${res.name}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) TextPrimary else TextSecondary,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Zero-permission Multi-File Gallery Import (Real Videos & Photos)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (pickedMediaUris.isNotEmpty()) TrackAudioAccent.copy(alpha = 0.12f) else StudioSurfaceHigh,
                    border = BorderStroke(
                        1.dp,
                        if (pickedMediaUris.isNotEmpty()) TrackAudioAccent.copy(alpha = 0.55f) else StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .testTag("new_project_pick_media_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (pickedMediaUris.isNotEmpty()) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (pickedMediaUris.isNotEmpty()) TrackAudioAccent else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (pickedMediaUris.isNotEmpty()) {
                                        "${pickedMediaUris.size} Gallery Clip${if (pickedMediaUris.size > 1) "s" else ""} Selected"
                                    } else {
                                        "Select Videos / Photos from Gallery"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (pickedMediaUris.isNotEmpty()) {
                                        "Tap to replace selection"
                                    } else {
                                        "Multi-select supported • or open a blank timeline"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (pickedMediaUris.isNotEmpty()) {
                            IconButton(
                                onClick = { pickedMediaUris = emptyList() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear selected media",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(title, selectedAspect, selectedResolution, selectedFps, pickedMediaUris)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Start Editing", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun RenameProjectDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draftTitle by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Rename Draft",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = draftTitle,
                onValueChange = { draftTitle = it },
                label = { Text("Project Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = StudioBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = NeonCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_project_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(draftTitle) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = OledBlack),
                modifier = Modifier.testTag("confirm_rename_project_button")
            ) {
                Text("Save Name", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeleteProjectConfirmDialog(
    projectTitle: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = CoralRed,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Delete Draft?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "“$projectTitle” and its timeline edits will be permanently removed from this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_delete_project_button")
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Draft", color = TextSecondary)
            }
        }
    )
}
