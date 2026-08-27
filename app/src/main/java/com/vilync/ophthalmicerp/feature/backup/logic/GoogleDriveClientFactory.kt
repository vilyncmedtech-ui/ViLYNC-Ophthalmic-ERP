package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

/**
 * Factory to create an authorized Google Drive client.
 */
object GoogleDriveClientFactory {

    private const val APP_NAME = "ViLYNC Ophthalmic ERP"
    private const val TIMEOUT_MS = 20000 // 20 Seconds

    /**
     * Creates a Drive service instance authorized for the given email.
     * Uses the minimum required scope: drive.file
     * 
     * @throws IllegalArgumentException if accountEmail is null or blank.
     */
    fun createDriveService(context: Context, accountEmail: String): Drive {
        if (accountEmail.isBlank()) {
            throw IllegalArgumentException("accountEmail must not be empty or null for Drive service creation.")
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccountName = accountEmail
        }

        val requestInitializer = HttpRequestInitializer { request ->
            credential.initialize(request)
            request.connectTimeout = TIMEOUT_MS
            request.readTimeout = TIMEOUT_MS
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer
        ).setApplicationName(APP_NAME).build()
    }
}
