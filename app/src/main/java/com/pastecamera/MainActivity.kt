package com.pastecamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pastecamera.camera.CameraViewModel
import com.pastecamera.ui.CameraScreen
import com.pastecamera.ui.theme.PasteCameraTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PasteCameraTheme { CameraScreen(viewModel) } }
    }
}
