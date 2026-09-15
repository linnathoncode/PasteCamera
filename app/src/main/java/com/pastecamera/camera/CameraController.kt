package com.pastecamera.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraController(private val context: Context) {
    private val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    private var camera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    fun bind(owner: LifecycleOwner, previewView: PreviewView) {
        lifecycleOwner = owner
        this.previewView = previewView
        bindCamera()
    }

    fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera()
    }

    private fun bindCamera() {
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            if (!provider.hasCamera(selector)) {
                lensFacing = CameraSelector.LENS_FACING_BACK
            }
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                owner,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun capture(file: File, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSuccess()
                override fun onError(exception: ImageCaptureException) = onError(exception)
            }
        )
    }

    fun setFlash(mode: Int) {
        imageCapture.flashMode = mode
    }

    fun focusAndMeter(x: Float, y: Float, previewView: PreviewView) {
        val point = previewView.meteringPointFactory.createPoint(x, y)
        camera?.cameraControl?.startFocusAndMetering(
            androidx.camera.core.FocusMeteringAction.Builder(point).build()
        )
    }

    fun setZoom(linearZoom: Float) {
        camera?.cameraControl?.setLinearZoom(linearZoom.coerceIn(0f, 1f))
    }

    fun adjustZoom(scaleFactor: Float) {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        setZoom(zoomState.linearZoom * scaleFactor)
    }
}
