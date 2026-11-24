package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.screens.EditorWorkspaceScreen
import com.example.ui.screens.HomeShell
import com.example.ui.theme.CutPulseTheme
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.StudioThemeMode
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val studioViewModel: StudioViewModel = viewModel()
            val themeMode by studioViewModel.themeMode.collectAsStateWithLifecycle()
            val editorState by studioViewModel.editorState.collectAsStateWithLifecycle()
            val inEditor = editorState.document != null

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
            }
        }
    }
}
