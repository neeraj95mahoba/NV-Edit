package com.example.ui.components

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.R
import com.example.media.StudioAudioRecorder
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.TrackAudioAccent
import kotlinx.coroutines.delay
import java.util.Locale

data class StudioCapturePermissionState(
    val hasCameraPermission: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val showRationaleDialog: Boolean = false,
    val rationaleNeedsCamera: Boolean = false,
    val rationaleNeedsAudio: Boolean = false,
    val openSettingsSuggested: Boolean = false
)

interface StudioCaptureController {
    val permissionState: StudioCapturePermissionState

    fun requestPermissions(
        requireCamera: Boolean,
        requireAudio: Boolean,
        onGranted: () -> Unit
    )

    fun launchRawVideoCapture(onVideoCaptured: (Uri) -> Unit)

    fun openAppPermissionSettings()

    fun dismissPermissionDialog()
}

private object NoOpStudioCaptureController : StudioCaptureController {
    override val permissionState: StudioCapturePermissionState = StudioCapturePermissionState()
    override fun requestPermissions(requireCamera: Boolean, requireAudio: Boolean, onGranted: () -> Unit) {}
    override fun launchRawVideoCapture(onVideoCaptured: (Uri) -> Unit) {}
    override fun openAppPermissionSettings() {}
    override fun dismissPermissionDialog() {}
}

val LocalStudioCaptureController = compositionLocalOf<StudioCaptureController> {
    NoOpStudioCaptureController
}

@Composable
fun StudioPermissionRationaleDialog(
    state: StudioCapturePermissionState,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.showRationaleDialog) return
    val palette = LocalStudioPalette.current

    val messageRes = when {
        state.openSettingsSuggested -> R.string.perm_dialog_denied_settings
        state.rationaleNeedsCamera && state.rationaleNeedsAudio -> R.string.perm_dialog_rationale_both
        state.rationaleNeedsCamera -> R.string.perm_dialog_rationale_camera
        else -> R.string.perm_dialog_rationale_audio
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.rationaleNeedsCamera) Icons.Filled.Videocam else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.perm_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionStatusChip("Camera", state.hasCameraPermission)
                    PermissionStatusChip("Microphone", state.hasAudioPermission)
                }
            }
        },
        confirmButton = {
            if (state.openSettingsSuggested) {
                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = OledBlack),
                    modifier = Modifier.testTag("perm_dialog_open_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.perm_action_settings), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = OledBlack),
                    modifier = Modifier.testTag("perm_dialog_grant_button")
                ) {
                    Text(stringResource(R.string.perm_action_grant), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("perm_dialog_dismiss_button")
            ) {
                Text(stringResource(R.string.perm_action_not_now), color = palette.textSecondary)
            }
        }
    )
}

@Composable
private fun PermissionStatusChip(label: String, granted: Boolean) {
    val accent = if (granted) AcidLime else CoralRed
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Text(
            text = if (granted) "$label ✓" else "$label Needed",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

/**
 * Interactive raw microphone audio recorder modal that captures real hardware AAC audio
 * directly into the NV Edit timeline with a live VU level meter.
 */
@Composable
fun StudioVoiceRecorderDialog(
    onAudioRecorded: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val context = LocalContext.current
    val captureController = LocalStudioCaptureController.current
    val recorder = remember { StudioAudioRecorder(context.applicationContext) }

    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val meterBars = remember { mutableStateListOf<Float>().apply { repeat(20) { add(0.08f) } } }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancelAndDiscard()
        }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        val startMs = System.currentTimeMillis()
        while (isRecording) {
            elapsedMs = System.currentTimeMillis() - startMs
            val amp = recorder.pollAmplitudeNormalized()
            if (meterBars.size >= 20) meterBars.removeAt(0)
            meterBars.add(amp)
            delay(80L)
        }
    }

    AlertDialog(
        onDismissRequest = {
            recorder.cancelAndDiscard()
            isRecording = false
            onDismiss()
        },
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) CoralRed.copy(alpha = 0.22f) else TrackAudioAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (isRecording) CoralRed else TrackAudioAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.voice_recorder_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val mins = (elapsedMs / 60_000L)
                val secs = (elapsedMs % 60_000L) / 1000L
                val tenths = (elapsedMs % 1000L) / 100L
                Text(
                    text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isRecording) CoralRed else NeonCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("voice_recorder_timer")
                )

                // Live 20-bar hardware microphone amplitude VU meter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OledBlack)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    meterBars.forEachIndexed { idx, amp ->
                        val color = when {
                            !isRecording -> palette.textMuted.copy(alpha = 0.35f)
                            idx > 15 && amp > 0.7f -> CoralRed
                            idx > 9 -> NeonCyan
                            else -> ElectricPurple
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((amp.coerceIn(0.08f, 1f) * 40f).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralRed
                    )
                } else {
                    Text(
                        text = if (isRecording) "Recording 44.1 kHz AAC raw audio…" else "Tap Start to capture raw microphone voiceover directly to your Audio track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            if (!isRecording) {
                Button(
                    onClick = {
                        errorMessage = null
                        captureController.requestPermissions(
                            requireCamera = false,
                            requireAudio = true
                        ) {
                            val result = recorder.startRecording()
                            result.fold(
                                onSuccess = {
                                    elapsedMs = 0L
                                    isRecording = true
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Unable to start microphone recording on this device."
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White),
                    modifier = Modifier.testTag("voice_recorder_start_button")
                ) {
                    Icon(Icons.Filled.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.voice_recorder_start), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        isRecording = false
                        val file = recorder.stopAndSave()
                        if (file != null) {
                            onAudioRecorded(Uri.fromFile(file))
                            onDismiss()
                        } else {
                            errorMessage = "Recording too short — hold for at least 1 second."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = OledBlack),
                    modifier = Modifier.testTag("voice_recorder_stop_button")
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.voice_recorder_stop_add), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    recorder.cancelAndDiscard()
                    isRecording = false
                    onDismiss()
                },
                modifier = Modifier.testTag("voice_recorder_cancel_button")
            ) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    )
}
