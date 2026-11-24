package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiCameraMotion
import com.example.ai.AiImageStyle
import com.example.ai.AiTaskState
import com.example.model.AspectRatioPreset
import com.example.model.MusicVisualizerStyle
import com.example.network.NetworkStatus
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MagentaSpark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonStudioGradient
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient

enum class AiLabMode(val label: String, val shortLabel: String) {
    TEXT_TO_IMAGE("Text → Image", "Text→Img"),
    TEXT_TO_VIDEO("Text → Video", "Text→Vid"),
    SCRIPT_TO_VIDEO("Script → Video", "Script"),
    PHOTO_ANIMATE("Photo Animate", "Photo"),
    LYRICS_VISUALIZER("Lyrics & Music", "Lyrics"),
    TALKING_PROMO("Talking / Ad", "Ad/Talk")
}

@Composable
fun AiLabQuickDrawer(
    onOpenAiLabMode: (AiLabMode) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("All-in-One AI Video Lab (Unlocked)", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ProGold.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, ProGold)
        ) {
            Text(
                text = "FREE",
                style = MaterialTheme.typography.labelSmall,
                color = ProGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }

    Text(
        text = "Generate real AI art, animated scenes, Hindi/English script reels, lyric visualizers & promo ads straight onto your timeline.",
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary
    )

    val cards = listOf(
        Triple(AiLabMode.TEXT_TO_IMAGE, "7 Art Styles", ElectricPurple),
        Triple(AiLabMode.TEXT_TO_VIDEO, "AI + Camera", NeonCyan),
        Triple(AiLabMode.SCRIPT_TO_VIDEO, "Hindi / Eng", ProGold),
        Triple(AiLabMode.PHOTO_ANIMATE, "3D Parallax", MagentaSpark),
        Triple(AiLabMode.LYRICS_VISUALIZER, "Audio Waves", AcidLime),
        Triple(AiLabMode.TALKING_PROMO, "Avatar & Ads", Color(0xFFFF8C42))
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cards) { (mode, subtitle, accent) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .width(116.dp)
                    .clickable { onOpenAiLabMode(mode) }
                    .testTag("ai_lab_quick_${mode.name}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = aiModeIcon(mode),
                        contentDescription = mode.label,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AiVideoLabDialog(
    initialMode: AiLabMode = AiLabMode.TEXT_TO_IMAGE,
    defaultAspectRatio: AspectRatioPreset = AspectRatioPreset.PORTRAIT_9_16,
    aiTaskState: AiTaskState<String>,
    networkStatus: NetworkStatus = NetworkStatus(),
    onToggleLowDataMode: () -> Unit = {},
    onResetTaskState: () -> Unit,
    onRunTextToImage: (prompt: String, style: AiImageStyle, ratio: AspectRatioPreset) -> Unit,
    onRunTextToVideo: (prompt: String, style: AiImageStyle, motion: AiCameraMotion, ratio: AspectRatioPreset) -> Unit,
    onRunScriptToVideo: (script: String, language: String, withAiVisual: Boolean, ratio: AspectRatioPreset) -> Unit,
    onRunPhotoAnimate: (photoUri: Uri, motion: AiCameraMotion, ratio: AspectRatioPreset) -> Unit,
    onRunLyricsVisualizer: (text: String, useAi: Boolean, language: String, style: MusicVisualizerStyle, ratio: AspectRatioPreset) -> Unit,
    onRunTalkingOrPromo: (brief: String, isTalking: Boolean, photoUri: Uri?, language: String, ratio: AspectRatioPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    var activeMode by remember(initialMode) { mutableStateOf(initialMode) }
    var aspectRatio by remember(defaultAspectRatio) { mutableStateOf(defaultAspectRatio) }
    var language by remember { mutableStateOf("English") }

    // Text-to-Image / Text-to-Video inputs
    var visualPrompt by remember { mutableStateOf("Futuristic neon rainy Tokyo street with glowing reflections") }
    var imageStyle by remember { mutableStateOf(AiImageStyle.CINEMATIC_8K) }
    var cameraMotion by remember { mutableStateOf(AiCameraMotion.SLOW_ZOOM) }

    // Script-to-Video inputs
    var scriptInput by remember { mutableStateOf("3 morning habits that double your creative focus") }
    var generateAiCoverVisual by remember { mutableStateOf(true) }

    // Photo Animator & Talking/Promo inputs
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var lyricsInput by remember { mutableStateOf("Late night highway neon city lights dreams") }
    var useAiForLyrics by remember { mutableStateOf(true) }
    var visualizerStyle by remember { mutableStateOf(MusicVisualizerStyle.NEON_BARS) }
    var promoBrief by remember { mutableStateOf("Flash launch sale for wireless studio headphones with 50% off") }
    var isTalkingPhotoMode by remember { mutableStateOf(false) }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedPhotoUri = uri
    }

    val isLoading = aiTaskState is AiTaskState.Loading

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .heightIn(max = 660.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .background(NeonStudioGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OledBlack, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Video Lab", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text("100% Unlocked • Direct to Timeline", style = MaterialTheme.typography.labelSmall, color = ProGold)
                        }
                    }
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier.testTag("ai_lab_close_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close AI Lab", tint = palette.textSecondary)
                    }
                }

                NetworkResilienceBanner(
                    networkStatus = networkStatus,
                    onToggleLowDataMode = onToggleLowDataMode
                )

                // 6 Mode Tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AiLabMode.entries) { mode ->
                        val active = mode == activeMode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accentPrimary.copy(alpha = 0.25f) else palette.surfaceElevated,
                            border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                            modifier = Modifier
                                .clickable(enabled = !isLoading) {
                                    activeMode = mode
                                    onResetTaskState()
                                }
                                .testTag("ai_lab_tab_${mode.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = aiModeIcon(mode),
                                    contentDescription = null,
                                    tint = if (active) palette.accentSecondary else palette.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = mode.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) palette.textPrimary else palette.textSecondary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Aspect Ratio Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Format:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        AspectRatioPreset.entries.forEach { preset ->
                            val selected = aspectRatio == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) palette.accentSecondary.copy(alpha = 0.2f) else palette.surfaceElevated,
                                border = BorderStroke(1.dp, if (selected) palette.accentSecondary else palette.border),
                                modifier = Modifier.clickable(enabled = !isLoading) { aspectRatio = preset }
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) palette.accentSecondary else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    when (activeMode) {
                        AiLabMode.TEXT_TO_IMAGE, AiLabMode.TEXT_TO_VIDEO -> {
                            OutlinedTextField(
                                value = visualPrompt,
                                onValueChange = { visualPrompt = it },
                                label = { Text("Scene Prompt") },
                                placeholder = { Text("Describe the visual scene you want to create…") },
                                maxLines = 3,
                                colors = aiTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("ai_visual_prompt_input")
                            )

                            Text("Art Style (7 Presets)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AiImageStyle.entries) { style ->
                                    val active = style == imageStyle
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) palette.accentPrimary.copy(alpha = 0.26f) else palette.surfaceElevated,
                                        border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                                        modifier = Modifier.clickable(enabled = !isLoading) { imageStyle = style }
                                    ) {
                                        Text(
                                            text = style.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) palette.textPrimary else palette.textSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (activeMode == AiLabMode.TEXT_TO_VIDEO) {
                                Text("Camera Motion Preset", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(AiCameraMotion.entries) { motion ->
                                        val active = motion == cameraMotion
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (active) ProGold.copy(alpha = 0.22f) else palette.surfaceElevated,
                                            border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                                            modifier = Modifier.clickable(enabled = !isLoading) { cameraMotion = motion }
                                        ) {
                                            Text(
                                                text = motion.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (active) ProGold else palette.textSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AiLabMode.SCRIPT_TO_VIDEO -> {
                            LanguageToggleRow(language = language, onSelect = { language = it })
                            OutlinedTextField(
                                value = scriptInput,
                                onValueChange = { scriptInput = it },
                                label = { Text("Video Topic or Script ($language)") },
                                placeholder = { Text("Enter a reel idea or paste your script…") },
                                maxLines = 3,
                                colors = aiTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("ai_script_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Generate AI Hero Visual", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                    Text("Creates a custom AI scene backdrop with camera pans", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                Switch(
                                    checked = generateAiCoverVisual,
                                    onCheckedChange = { generateAiCoverVisual = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = OledBlack, checkedTrackColor = NeonCyan)
                                )
                            }
                        }

                        AiLabMode.PHOTO_ANIMATE -> {
                            OutlinedButton(
                                onClick = {
                                    singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                border = BorderStroke(1.dp, palette.accentSecondary),
                                modifier = Modifier.fillMaxWidth().testTag("ai_pick_photo_button")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = palette.accentSecondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (pickedPhotoUri != null) "Photo Selected — Tap to Change" else "Select Photo from Gallery",
                                    color = palette.textPrimary
                                )
                            }
                            Text("Motion Style", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AiCameraMotion.entries) { motion ->
                                    val active = motion == cameraMotion
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) palette.accentSecondary.copy(alpha = 0.22f) else palette.surfaceElevated,
                                        border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                                        modifier = Modifier.clickable { cameraMotion = motion }
                                    ) {
                                        Text(
                                            text = motion.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) palette.accentSecondary else palette.textSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AiLabMode.LYRICS_VISUALIZER -> {
                            LanguageToggleRow(language = language, onSelect = { language = it })
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Write Karaoke Lyrics with AI", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                Switch(
                                    checked = useAiForLyrics,
                                    onCheckedChange = { useAiForLyrics = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = OledBlack, checkedTrackColor = NeonCyan)
                                )
                            }
                            OutlinedTextField(
                                value = lyricsInput,
                                onValueChange = { lyricsInput = it },
                                label = { Text(if (useAiForLyrics) "Song Mood / Vibe" else "Paste Lyrics (1 line per cue)") },
                                maxLines = 4,
                                colors = aiTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("ai_lyrics_input")
                            )
                            Text("Music Visualizer Overlay", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MusicVisualizerStyle.entries.filter { it != MusicVisualizerStyle.NONE }.forEach { viz ->
                                    val active = viz == visualizerStyle
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) AcidLime.copy(alpha = 0.22f) else palette.surfaceElevated,
                                        border = BorderStroke(1.dp, if (active) AcidLime else palette.border),
                                        modifier = Modifier.weight(1f).clickable { visualizerStyle = viz }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                            Text(viz.label, style = MaterialTheme.typography.labelSmall, color = if (active) AcidLime else palette.textSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        AiLabMode.TALKING_PROMO -> {
                            LanguageToggleRow(language = language, onSelect = { language = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(false to "Promo Ad Maker", true to "Talking Photo").forEach { (isTalk, lbl) ->
                                    val active = isTalkingPhotoMode == isTalk
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (active) ProGold.copy(alpha = 0.22f) else palette.surfaceElevated,
                                        border = BorderStroke(1.dp, if (active) ProGold else palette.border),
                                        modifier = Modifier.weight(1f).clickable { isTalkingPhotoMode = isTalk }
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (active) ProGold else palette.textSecondary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = palette.accentSecondary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (pickedPhotoUri != null) "Avatar/Product Photo Ready (Tap to Change)" else "Optional: Pick Portrait / Product Photo",
                                    color = palette.textPrimary
                                )
                            }
                            OutlinedTextField(
                                value = promoBrief,
                                onValueChange = { promoBrief = it },
                                label = { Text(if (isTalkingPhotoMode) "Talking Script / Topic" else "Product / Offer Brief") },
                                maxLines = 3,
                                colors = aiTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("ai_promo_input")
                            )
                        }
                    }

                    // Live AI Task Status Banner (Loading / Success / Real Error)
                    when (aiTaskState) {
                        is AiTaskState.Loading -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.accentPrimary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, palette.accentSecondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = palette.accentSecondary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(aiTaskState.status, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                }
                            }
                        }
                        is AiTaskState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AcidLime.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, AcidLime),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AcidLime, modifier = Modifier.size(18.dp))
                                    Text(aiTaskState.data, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                }
                            }
                        }
                        is AiTaskState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CoralRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CoralRed, modifier = Modifier.size(18.dp))
                                    Text(aiTaskState.message, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                }
                            }
                        }
                        AiTaskState.Idle -> Unit
                    }
                }

                // Primary Action Button
                val buttonLabel = when (activeMode) {
                    AiLabMode.TEXT_TO_IMAGE -> "Generate AI Image → Timeline"
                    AiLabMode.TEXT_TO_VIDEO -> "Generate AI Video → Timeline"
                    AiLabMode.SCRIPT_TO_VIDEO -> "Build AI Script Video → Timeline"
                    AiLabMode.PHOTO_ANIMATE -> "Animate Photo → Timeline"
                    AiLabMode.LYRICS_VISUALIZER -> "Create Lyrics Visualizer → Timeline"
                    AiLabMode.TALKING_PROMO -> if (isTalkingPhotoMode) "Create Talking Photo → Timeline" else "Generate Promo Ad → Timeline"
                }

                Button(
                    enabled = !isLoading,
                    onClick = {
                        when (activeMode) {
                            AiLabMode.TEXT_TO_IMAGE -> onRunTextToImage(visualPrompt, imageStyle, aspectRatio)
                            AiLabMode.TEXT_TO_VIDEO -> onRunTextToVideo(visualPrompt, imageStyle, cameraMotion, aspectRatio)
                            AiLabMode.SCRIPT_TO_VIDEO -> onRunScriptToVideo(scriptInput, language, generateAiCoverVisual, aspectRatio)
                            AiLabMode.PHOTO_ANIMATE -> {
                                val uri = pickedPhotoUri
                                if (uri != null) {
                                    onRunPhotoAnimate(uri, cameraMotion, aspectRatio)
                                } else {
                                    singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            }
                            AiLabMode.LYRICS_VISUALIZER -> onRunLyricsVisualizer(lyricsInput, useAiForLyrics, language, visualizerStyle, aspectRatio)
                            AiLabMode.TALKING_PROMO -> onRunTalkingOrPromo(promoBrief, isTalkingPhotoMode, pickedPhotoUri, language, aspectRatio)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = OledBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_lab_generate_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(buttonLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LanguageToggleRow(
    language: String,
    onSelect: (String) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Language:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        listOf("English", "Hindi").forEach { lang ->
            val active = language == lang
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) palette.accentPrimary.copy(alpha = 0.25f) else palette.surfaceElevated,
                border = BorderStroke(1.dp, if (active) palette.accentSecondary else palette.border),
                modifier = Modifier.clickable { onSelect(lang) }
            ) {
                Text(
                    text = if (lang == "Hindi") "हिन्दी (Hindi)" else "English",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.accentSecondary else palette.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun aiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalStudioPalette.current.accentSecondary,
    unfocusedBorderColor = LocalStudioPalette.current.border,
    focusedTextColor = LocalStudioPalette.current.textPrimary,
    unfocusedTextColor = LocalStudioPalette.current.textPrimary,
    focusedLabelColor = LocalStudioPalette.current.accentSecondary,
    unfocusedLabelColor = LocalStudioPalette.current.textSecondary
)

private fun aiModeIcon(mode: AiLabMode): ImageVector = when (mode) {
    AiLabMode.TEXT_TO_IMAGE -> Icons.Filled.Image
    AiLabMode.TEXT_TO_VIDEO -> Icons.Filled.MovieFilter
    AiLabMode.SCRIPT_TO_VIDEO -> Icons.Filled.Description
    AiLabMode.PHOTO_ANIMATE -> Icons.Filled.MotionPhotosOn
    AiLabMode.LYRICS_VISUALIZER -> Icons.Filled.MusicNote
    AiLabMode.TALKING_PROMO -> Icons.Filled.Campaign
}
