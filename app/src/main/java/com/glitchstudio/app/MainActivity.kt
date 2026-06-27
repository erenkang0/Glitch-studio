package com.glitchstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.glitchstudio.app.ui.EditorScreen
import com.glitchstudio.app.ui.EditorViewModel
import com.glitchstudio.app.ui.theme.GlitchColors
import com.glitchstudio.app.ui.theme.GlitchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GlitchTheme {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().background(GlitchColors.background)
                ) {
                    EditorScreen(viewModel)
                }
            }
        }
    }
}
