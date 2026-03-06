package com.example.kt4_8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.kt4_8.ui.theme.Kt4_8Theme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Kt4_8Theme {
                val viewModel: PhotoProcessorViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}