package com.pastecamera.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

class ImageClipboard(private val context: Context) {
    fun copy(uri: Uri) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "Paste Camera image", uri))
    }
}
