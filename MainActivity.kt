package com.example

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AndroidAdsBridge
import com.example.ads.LocalStudioAdController
import com.example.ads.StudioAdManager
import com.example.ai.configureAppCheck
import com.example.ui.components.LocalStudioCaptureController
import com.example.ui.components.NvEditSplashOverlay
import com.example.ui.components.StudioCaptureController
import com.example.ui.components.StudioCapturePermissionState
import com.example.ui.components.StudioFullscreenAdPreviewDialog
import com.example.ui.components.StudioPermissionRationaleDialog
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalStudioStrings
import com.example.ui.i18n.studioStringsFor
import com.example.ui.screens.EditorWorkspaceScreen
import com.example.ui.screens.HomeShell
import com.example.ui.theme.CutPulseTheme
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.StudioThemeMode
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var studioAdManager: StudioAdManager
    lateinit var androidAdsBridge: AndroidAdsBridge
        private set

    private val capturePermissionState = MutableStateFlow(StudioCapturePermissionState())

    private var pendingRequiredPermissions: List<String> = emptyList()
    private var pendingPermissionGrantedAction: (() -> Unit)? = null
    private var pendingVideoOutputFile: File? = null
    private var pendingVideoOutputUri: Uri? = null
    private var pendingVideoCapturedCallback: ((Uri) -> Unit)? = null

    private val requestCapturePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val camGranted = results[Manifest.permission.CAMERA] ?: hasPermission(Manifest.permission.CAMERA)
        val micGranted = results[Manifest.permission.RECORD_AUDIO] ?: hasPermission(Manifest.permission.RECORD_AUDIO)
        val required = pendingRequiredPermissions
        val allRequiredGranted = required.all { perm ->
            when (perm) {
                Manifest.permission.CAMERA -> camGranted
                Manifest.permission.RECORD_AUDIO -> micGranted
                else -> hasPermission(perm)
            }
        }

        if (allRequiredGranted) {
            capturePermissionState.update {
                it.copy(
                    hasCameraPermission = camGranted,
                    hasAudioPermission = micGranted,
                    showRationaleDialog = false,
                    openSettingsSuggested = false
                )
            }
            val action = pendingPermissionGrantedAction
            pendingPermissionGrantedAction = null
            pendingRequiredPermissions = emptyList()
            action?.invoke()
        } else {
            val missing = required.filter { !hasPermission(it) }
            val permanentlyDenied = missing.any { perm ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)
            }
            capturePermissionState.update {
                it.copy(
                    hasCameraPermission = camGranted,
                    hasAudioPermission = micGranted,
                    showRationaleDialog = true,
                    rationaleNeedsCamera = Manifest.permission.CAMERA in required,
                    rationaleNeedsAudio = Manifest.permission.RECORD_AUDIO in required,
                    openSettingsSuggested = permanentlyDenied
                )
            }
        }
    }

    private val captureRawVideoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { saved ->
        val file = pendingVideoOutputFile
        val uri = pendingVideoOutputUri
        val callback = pendingVideoCapturedCallback
        pendingVideoOutputFile = null
        pendingVideoOutputUri = null
        pendingVideoCapturedCallback = null

        if (saved && file != null && file.exists() && file.length() > 0L) {
            callback?.invoke(Uri.fromFile(file))
        } else if (saved && uri != null) {
            callback?.invoke(uri)
        } else {
            runCatching { file?.delete() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        studioAdManager = StudioAdManager(this).also { it.initialize() }
        androidAdsBridge = AndroidAdsBridge(studioAdManager)
        refreshCapturePermissionState()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val studioViewModel: StudioViewModel = viewModel()
            val themeMode by studioViewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by studioViewModel.appLanguage.collectAsStateWithLifecycle()
            val editorState by studioViewModel.editorState.collectAsStateWithLifecycle()
            val permState by capturePermissionState.collectAsStateWithLifecycle()
            val adUiState by studioAdManager.state.collectAsStateWithLifecycle()
            val inEditor = editorState.document != null
            val studioStrings = remember(appLanguage) { studioStringsFor(appLanguage) }
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1500L)
                showSplash = false
            }

            val captureController = remember(permState) {
                object : StudioCaptureController {
                    override val permissionState: StudioCapturePermissionState
                        get() = permState

                    override fun requestPermissions(
                        requireCamera: Boolean,
                        requireAudio: Boolean,
                        onGranted: () -> Unit
                    ) {
                        requestStudioPermissions(
                            requireCamera = requireCamera,
                            requireAudio = requireAudio,
                            onGranted = onGranted
                        )
                    }

                    override fun launchRawVideoCapture(onVideoCaptured: (Uri) -> Unit) {
                        startRawCameraVideoCapture(onVideoCaptured)
                    }

                    override fun openAppPermissionSettings() {
                        dismissCapturePermissionDialog()
                        openSystemAppSettings()
                    }

                    override fun dismissPermissionDialog() {
                        dismissCapturePermissionDialog()
                    }
                }
            }

            LaunchedEffect(themeMode) {
                val barStyle = if (themeMode == StudioThemeMode.STUDIO_LIGHT) {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            CutPulseTheme(themeMode = themeMode) {
                val palette = LocalStudioPalette.current
                CompositionLocalProvider(
                    LocalStudioCaptureController provides captureController,
                    LocalStudioAdController provides studioAdManager,
                    LocalAppLanguage provides appLanguage,
                    LocalStudioStrings provides studioStrings
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.background)
                    ) {
                        Crossfade(
                            targetState = inEditor,
                            label = "studio_workspace_transition",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.background)
                        ) { editing ->
                            if (editing) {
                                EditorWorkspaceScreen(viewModel = studioViewModel)
                            } else {
                                HomeShell(viewModel = studioViewModel)
                            }
                        }

                        StudioPermissionRationaleDialog(
                            state = permState,
                            onGrantClick = {
                                val toRequest = pendingRequiredPermissions.ifEmpty {
                                    buildList {
                                        if (permState.rationaleNeedsCamera) add(Manifest.permission.CAMERA)
                                        if (permState.rationaleNeedsAudio) add(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                capturePermissionState.update { it.copy(showRationaleDialog = false) }
                                if (toRequest.isNotEmpty()) {
                                    requestCapturePermissionsLauncher.launch(toRequest.toTypedArray())
                                }
                            },
                            onOpenSettingsClick = {
                                dismissCapturePermissionDialog()
                                openSystemAppSettings()
                            },
                            onDismiss = { dismissCapturePermissionDialog() }
                        )

                        StudioFullscreenAdPreviewDialog(
                            request = adUiState.activePreviewAd,
                            onComplete = { rewardGranted ->
                                studioAdManager.completeFullscreenPreviewAd(rewardGranted)
                            }
                        )

                        NvEditSplashOverlay(visible = showSplash)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCapturePermissionState()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun refreshCapturePermissionState() {
        capturePermissionState.update {
            it.copy(
                hasCameraPermission = hasPermission(Manifest.permission.CAMERA),
                hasAudioPermission = hasPermission(Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    private fun requestStudioPermissions(
        requireCamera: Boolean,
        requireAudio: Boolean,
        onGranted: () -> Unit
    ) {
        val needed = buildList {
            if (requireCamera && !hasPermission(Manifest.permission.CAMERA)) {
                add(Manifest.permission.CAMERA)
            }
            if (requireAudio && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }

        refreshCapturePermissionState()
        if (needed.isEmpty()) {
            onGranted()
            return
        }

        pendingRequiredPermissions = needed
        pendingPermissionGrantedAction = onGranted

        val needsRationale = needed.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }
        if (needsRationale) {
            capturePermissionState.update {
                it.copy(
                    showRationaleDialog = true,
                    rationaleNeedsCamera = Manifest.permission.CAMERA in needed,
                    rationaleNeedsAudio = Manifest.permission.RECORD_AUDIO in needed,
                    openSettingsSuggested = false
                )
            }
        } else {
            requestCapturePermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startRawCameraVideoCapture(onVideoCaptured: (Uri) -> Unit) {
        requestStudioPermissions(
            requireCamera = true,
            requireAudio = true
        ) {
            val recordingsDir = File(filesDir, "recordings").apply { mkdirs() }
            val outFile = File(recordingsDir, "NV_RAW_CAM_${System.currentTimeMillis()}.mp4")
            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                outFile
            )
            pendingVideoOutputFile = outFile
            pendingVideoOutputUri = contentUri
            pendingVideoCapturedCallback = onVideoCaptured
            try {
                captureRawVideoLauncher.launch(contentUri)
            } catch (_: ActivityNotFoundException) {
                runCatching { outFile.delete() }
                pendingVideoOutputFile = null
                pendingVideoOutputUri = null
                pendingVideoCapturedCallback = null
                Toast.makeText(
                    this,
                    "No camera capture app available on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun dismissCapturePermissionDialog() {
        pendingPermissionGrantedAction = null
        pendingRequiredPermissions = emptyList()
        capturePermissionState.update {
            it.copy(showRationaleDialog = false, openSettingsSuggested = false)
        }
    }

    private fun openSystemAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }
}
