package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

/**
 * Factory to create an authorized Google Drive client.
 */
object GoogleDriveClientFactory {

    private const val APP_NAME = "ViLYNC Ophthalmic ERP"

    /**
     * Creates a Drive service instance authorized for the given email.
     * Uses the minimum required scope: drive.file
     */
    fun createDriveService(context: Context, accountEmail: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccountName = accountEmail
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }
}
