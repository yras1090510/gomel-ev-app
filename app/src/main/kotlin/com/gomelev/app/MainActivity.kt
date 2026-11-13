package com.gomelev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.gomelev.app.ui.GomelEvRoot
import com.gomelev.app.ui.MainViewModel
import com.gomelev.app.ui.theme.GomelEvTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val foldingFeature by produceState<FoldingFeature?>(initialValue = null, this) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info ->
                        value = info.displayFeatures
                            .filterIsInstance<FoldingFeature>()
                            .firstOrNull()
                    }
            }

            GomelEvTheme {
                GomelEvRoot(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    foldingFeature = foldingFeature
                )
            }
        }
    }
}
