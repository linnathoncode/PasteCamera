package com.pastecamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pastecamera.camera.CameraViewModel
import com.pastecamera.R
import kotlinx.coroutines.delay

private val Overlay = Color(0xD9161920)
private val Ink = Color(0xFFF7F8FA)
private val MutedInk = Color(0xFFB9C0CB)
private val Signal = Color(0xFFFFB000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var thumbnailVisible by remember { mutableStateOf(false) }
    var showEmptyRecent by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.lastCapture) {
        if (state.lastCapture == null) {
            thumbnailVisible = false
        } else {
            thumbnailVisible = true
            delay(4_000)
            thumbnailVisible = false
        }
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (!hasPermission) {
            PermissionScreen { permissionLauncher.launch(Manifest.permission.CAMERA) }
            return@Surface
        }
        Box(Modifier.fillMaxSize()) {
            CameraPreview(viewModel, Modifier.fillMaxSize())
            CameraControls(
                flashEnabled = state.flashMode != ImageCapture.FLASH_MODE_OFF,
                flashAuto = state.flashMode == ImageCapture.FLASH_MODE_AUTO,
                lastCapture = state.lastCapture.takeIf { thumbnailVisible },
                isCapturing = state.isCapturing,
                onFlash = viewModel::cycleFlash,
                onFlip = viewModel::flipCamera,
                onPreview = {
                    if (state.lastCapture != null) viewModel.openPreview() else showEmptyRecent = true
                },
                onCapture = viewModel::capture
            )
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 104.dp))
        }
        val lastCapture = state.lastCapture
        if (state.showPreview && lastCapture != null) {
            CaptureSheet(lastCapture, viewModel::copyAgain, viewModel::save, viewModel::share, viewModel::delete, viewModel::closePreview)
        }
        if (showEmptyRecent) {
            EmptyRecentSheet { showEmptyRecent = false }
        }
    }
}

@Composable
private fun CameraPreview(viewModel: CameraViewModel, modifier: Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(modifier = modifier, factory = { context ->
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            viewModel.controller.bind(lifecycleOwner, this)
            val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    viewModel.controller.adjustZoom(detector.scaleFactor)
                    return true
                }
            })
            setOnTouchListener { _, event ->
                scaleDetector.onTouchEvent(event)
                if (event.action == android.view.MotionEvent.ACTION_UP && !scaleDetector.isInProgress) {
                    viewModel.controller.focusAndMeter(event.x, event.y, this)
                }
                true
            }
        }
    })
}

@Composable
private fun CameraControls(flashEnabled: Boolean, flashAuto: Boolean, lastCapture: Uri?, isCapturing: Boolean, onFlash: () -> Unit, onFlip: () -> Unit, onPreview: () -> Unit, onCapture: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(start = 16.dp, top = 38.dp, end = 16.dp, bottom = 38.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LatestCaptureControl(lastCapture, onPreview)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FlashControl(flashEnabled, flashAuto, onFlash)
                ShutterButton(isCapturing, onCapture)
                IconControl(R.drawable.ic_recycle, "Flip camera", Ink, onFlip)
            }
        }
    }
}

@Composable
private fun FlashControl(flashEnabled: Boolean, flashAuto: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(58.dp)) {
        IconControl(R.drawable.ic_flash, "Change flash mode", if (flashEnabled) Signal else Ink, onClick)
        if (flashAuto) {
            Text(
                "A",
                color = Ink,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun IconControl(icon: Int, description: String, tint: Color, onClick: () -> Unit) {
    Surface(color = Overlay, shape = CircleShape, modifier = Modifier.size(58.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(icon), contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun LatestCaptureControl(uri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (uri == null) "Open recent captures" else "Open recent capture" },
        contentAlignment = Alignment.Center
    ) {
        if (uri == null) {
            Text("\u22EE", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { it.setImageURI(uri) }
            )
        }
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(80.dp).border(3.dp, Ink, CircleShape).padding(6.dp).clip(CircleShape)
            .background(if (isCapturing) MutedInk else Ink).clickable(enabled = !isCapturing, onClick = onClick)
            .semantics { contentDescription = "Capture photo" },
        contentAlignment = Alignment.Center
    ) {
        Text(if (isCapturing) "..." else "CAPTURE", color = Color(0xFF12151B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Paste Camera needs camera access.", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Captured photos remain temporary until you choose Save.", color = MutedInk)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = Color(0xFF211800))) { Text("Allow camera") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureSheet(uri: Uri, onCopy: () -> Unit, onSave: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF171A21), contentColor = Ink) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()) {
            UriImage(uri, Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.height(16.dp))
            Text("Copied to clipboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("This photo is temporary. Save it only if you want it in your gallery.", color = MutedInk)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCopy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = Color(0xFF211800))) { Text("Copy again") }
                OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("Share") }
                OutlinedButton(onClick = { onDelete(); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Delete") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyRecentSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF171A21), contentColor = Ink) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)) {
            Text("No recent capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Take a photo and its temporary preview will appear here.", color = MutedInk)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } }, update = { it.setImageURI(uri) })
}
