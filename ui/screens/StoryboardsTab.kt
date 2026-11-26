package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.StoryboardScene
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import java.util.UUID
import kotlin.math.roundToInt

private val cameraAngles = listOf("Wide Shot", "Close-Up", "Low Angle", "Drone Aerial", "POV", "Tracking", "Dutch Tilt")
private val lightingPresets = listOf("Neon Rim", "Golden Hour", "Street Neon", "Backlit Mist", "Studio Key", "Natural Soft")

@Composable
fun StoryboardsTab(
    scenes: List<StoryboardScene>,
    onSaveScene: (StoryboardScene) -> Unit,
    onToggleFilmed: (StoryboardScene) -> Unit,
    onMoveScene: (index: Int, delta: Int) -> Unit,
    onDeleteScene: (String) -> Unit,
    onConvertToTimeline: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var editingScene by remember { mutableStateOf<StoryboardScene?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val totalSec = remember(scenes) { scenes.sumOf { it.durationSec } }
    val filmedCount = remember(scenes) { scenes.count { it.isFilmed } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (embedded) 4.dp else 16.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!embedded) {
                        Text(
                            text = "Shot Storyboard",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = "${scenes.size} scenes • ${totalSec}s runtime • $filmedCount/${scenes.size} filmed",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonStudioGradient)
                        .clickable {
                            isCreatingNew = true
                            editingScene = StoryboardScene(
                                id = UUID.randomUUID().toString(),
                                orderIndex = scenes.size,
                                title = "",
                                scriptNotes = "",
                                durationSec = 4,
                                cameraAngle = "Wide Shot",
                                lightingTag = "Neon Rim"
                            )
                        }
                        .padding(horizontal = 14.dp)
                        .testTag("add_storyboard_scene_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add storyboard scene",
                            tint = OledBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Add Shot",
                            style = MaterialTheme.typography.labelLarge,
                            color = OledBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Convert Storyboard to Timeline Action Banner
        if (scenes.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConvertToTimeline)
                        .testTag("convert_storyboard_to_timeline")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MovieFilter,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Send Shot List to Editor",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Auto-build a ${totalSec}s multi-track timeline from these scenes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "BUILD",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        itemsIndexed(scenes, key = { _, item -> item.id }) { index, scene ->
            StoryboardSceneCard(
                index = index,
                totalCount = scenes.size,
                scene = scene,
                onToggleFilmed = { onToggleFilmed(scene) },
                onEdit = {
                    isCreatingNew = false
                    editingScene = scene
                },
                onMoveUp = { onMoveScene(index, -1) },
                onMoveDown = { onMoveScene(index, 1) },
                onDelete = { onDeleteScene(scene.id) }
            )
        }
    }

    editingScene?.let { draft ->
        StoryboardEditorDialog(
            initial = draft,
            isNew = isCreatingNew,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                onSaveScene(updated)
                editingScene = null
            }
        )
    }
}

@Composable
private fun StoryboardSceneCard(
    index: Int,
    totalCount: Int,
    scene: StoryboardScene,
    onToggleFilmed: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (scene.isFilmed) AcidLime.copy(alpha = 0.45f) else StudioBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storyboard_card_${scene.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ElectricPurple.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", index + 1),
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = scene.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onToggleFilmed,
                    modifier = Modifier.testTag("toggle_filmed_${scene.id}")
                ) {
                    Icon(
                        imageVector = if (scene.isFilmed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (scene.isFilmed) "Marked as filmed" else "Mark as filmed",
                        tint = if (scene.isFilmed) AcidLime else TextMuted
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = scene.scriptNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = scene.cameraAngle,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarmAmber.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${scene.durationSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSurfaceElevated
                    ) {
                        Text(
                            text = scene.lightingTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move scene up",
                        tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = index < totalCount - 1, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move scene down",
                        tint = if (index < totalCount - 1) TextSecondary else TextMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit scene",
                        tint = NeonCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete scene",
                        tint = CoralRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryboardEditorDialog(
    initial: StoryboardScene,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (StoryboardScene) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.scriptNotes) }
    var durationSec by remember { mutableFloatStateOf(initial.durationSec.toFloat()) }
    var angle by remember { mutableStateOf(initial.cameraAngle) }
    var lighting by remember { mutableStateOf(initial.lightingTag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        titleContentColor = TextPrimary,
        title = {
            Text(if (isNew) "New Shot Card" else "Edit Shot Card", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Scene Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Script & Action Notes") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = "Shot Duration: ${durationSec.roundToInt()}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    Slider(
                        value = durationSec,
                        onValueChange = { durationSec = it },
                        valueRange = 1f..20f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricPurple)
                    )
                }
                Text("Camera Angle", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cameraAngles) { item ->
                        val active = item == angle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) ElectricPurple.copy(alpha = 0.3f) else StudioSurface,
                            border = BorderStroke(1.dp, if (active) NeonCyan else StudioBorder),
                            modifier = Modifier.clickable { angle = item }
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Text("Lighting Mood", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(lightingPresets) { item ->
                        val active = item == lighting
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) WarmAmber.copy(alpha = 0.25f) else StudioSurface,
                            border = BorderStroke(1.dp, if (active) WarmAmber else StudioBorder),
                            modifier = Modifier.clickable { lighting = item }
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) WarmAmber else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim().ifBlank { "Untitled Scene" },
                            scriptNotes = notes.trim().ifBlank { "Capture smooth camera motion and match beat cut." },
                            durationSec = durationSec.roundToInt(),
                            cameraAngle = angle,
                            lightingTag = lighting
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                modifier = Modifier.testTag("save_storyboard_button")
            ) {
                Text("Save Shot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
