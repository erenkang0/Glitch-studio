package com.glitchstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.glitchstudio.app.ui.EditorScreen
import com.glitchstudio.app.ui.EditorViewModel
import com.glitchstudio.app.ui.HomeScreen
import com.glitchstudio.app.ui.theme.GlitchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlitchTheme {
                val state by viewModel.state.collectAsState()
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri -> uri?.let { viewModel.loadImage(it) } }

                val launchPicker: () -> Unit = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                Surface(Modifier.fillMaxSize(), color = GlitchTheme.colors.backdrop) {
                    if (state.sourceLoaded) {
                        EditorScreen(
                            viewModel = viewModel,
                            onPickNewImage = launchPicker,
                            onBack = { viewModel.clearImage() },
                        )
                    } else {
                        HomeScreen(onPickImage = launchPicker, isLoading = state.isLoading)
                    }
                }
            }
        }
    }
}
