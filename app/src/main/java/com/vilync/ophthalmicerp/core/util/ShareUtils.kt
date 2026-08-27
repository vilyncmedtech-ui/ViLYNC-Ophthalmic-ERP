package com.vilync.ophthalmicerp.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    /**
     * Shares a file using the Android Native Share Sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Document") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        shareUri(context, uri, mimeType, title)
    }

    /**
     * Shares a content Uri using the Android Native Share Sheet.
     */
    fun shareUri(context: Context, uri: Uri, mimeType: String, title: String = "Share Document") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
