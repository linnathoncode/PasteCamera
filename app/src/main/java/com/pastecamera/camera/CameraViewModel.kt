package com.pastecamera.camera

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import com.pastecamera.clipboard.ImageClipboard
import com.pastecamera.storage.ImageSaver
import com.pastecamera.storage.TemporaryImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class CameraUiState(
    val lastCapture: Uri? = null,
    val isCapturing: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val isFrontCamera: Boolean = false,
    val message: String? = null,
    val showPreview: Boolean = false
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TemporaryImageStore(application)
    private val clipboard = ImageClipboard(application)
    private val saver = ImageSaver(application)
    val controller = CameraController(application)
    private var lastFile: File? = null
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    init {
        store.cleanup()
    }

    fun capture() {
        if (_state.value.isCapturing) return
        val file = store.createCaptureFile()
        _state.value = _state.value.copy(isCapturing = true, message = null)
        controller.capture(file, onSuccess = {
            val uri = store.contentUri(file)
            clipboard.copy(uri)
            lastFile = file
            store.rememberCurrent(file)
            store.cleanup()
            _state.value = _state.value.copy(lastCapture = uri, isCapturing = false, message = "Copied to clipboard")
        }, onError = { error ->
            file.delete()
            _state.value = _state.value.copy(isCapturing = false, message = error.message ?: "Could not take photo")
        })
    }

    fun cycleFlash() {
        val next = when (_state.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        controller.setFlash(next)
        _state.value = _state.value.copy(flashMode = next)
    }

    fun flipCamera() {
        controller.toggleCamera()
        _state.value = _state.value.copy(isFrontCamera = !_state.value.isFrontCamera)
    }

    fun openPreview() { if (_state.value.lastCapture != null) _state.value = _state.value.copy(showPreview = true) }
    fun closePreview() { _state.value = _state.value.copy(showPreview = false) }
    fun copyAgain() { _state.value.lastCapture?.let { clipboard.copy(it); _state.value = _state.value.copy(message = "Copied to clipboard") } }
    fun save() { _state.value.lastCapture?.let { uri -> _state.value = _state.value.copy(message = if (saver.saveToGallery(uri).isSuccess) "Saved to Pictures/Paste Camera" else "Could not save image") } }
    fun share() {
        _state.value.lastCapture?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            getApplication<Application>().startActivity(Intent.createChooser(intent, "Share image").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    fun delete() { lastFile?.let(store::delete); lastFile = null; _state.value = CameraUiState(message = "Temporary image deleted") }
}
