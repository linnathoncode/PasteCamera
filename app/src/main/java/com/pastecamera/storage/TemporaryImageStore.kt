package com.pastecamera.storage

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class TemporaryImageStore(private val context: Context) {
    private val capturesDirectory = File(context.cacheDir, "captures")
    private val preferences = context.getSharedPreferences("captures", Context.MODE_PRIVATE)

    fun createCaptureFile(): File {
        capturesDirectory.mkdirs()
        return File(capturesDirectory, "capture_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    }

    fun contentUri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun rememberCurrent(file: File) {
        preferences.edit().putString(CURRENT_CAPTURE, file.name).apply()
    }

    fun delete(file: File) {
        file.delete()
        if (preferences.getString(CURRENT_CAPTURE, null) == file.name) {
            preferences.edit().remove(CURRENT_CAPTURE).apply()
        }
    }

    fun cleanup() {
        val currentName = preferences.getString(CURRENT_CAPTURE, null)
        val files = capturesDirectory.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        val expiry = System.currentTimeMillis() - MAX_AGE_MILLIS
        files.forEachIndexed { index, file ->
            if (file.name != currentName && (file.lastModified() < expiry || index >= MAX_CAPTURES)) {
                file.delete()
            }
        }
    }

    companion object {
        private const val CURRENT_CAPTURE = "current_capture"
        private const val MAX_CAPTURES = 20
        private const val MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L
    }
}
