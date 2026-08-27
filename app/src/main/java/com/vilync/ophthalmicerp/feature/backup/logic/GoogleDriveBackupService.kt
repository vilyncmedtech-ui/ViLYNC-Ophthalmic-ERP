package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Production foundation for Google Drive Backup interaction.
 * 
 * Responsibilities:
 * - Atomic database duplication.
 * - Payload checksum generation.
 * - Google Drive service initialization with Account Visibility Bridge.
 * - ERP folder management in Drive.
 */
class GoogleDriveBackupService(
    private val context: Context,
    private val settingsManager: BackupSettingsManager
) {

    companion object {
        private const val TAG = "GDRIVE_BACKUP"
        private const val TEMP_BACKUP_NAME = "gdrive_backup_temp.db"
        private const val FOLDER_NAME = "ViLYNC ERP Backup"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    }

    private var driveService: Drive? = null
    private var initializedEmail: String? = null

    /**
     * Initializes the Drive client using the factory after ensuring account visibility.
     * 
     * Idempotent: Does nothing if already initialized for the same email.
     */
    suspend fun initializeDriveService(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (email.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot initialize Drive service with blank email."))
        }

        // 1. Ensure Account Visibility via Bridge (MUST be on background thread)
        try {
            // This is the CRITICAL fix for "name must not be empty: null"
            // It whitelists the app in AccountManager for this specific email.
            GoogleAuthUtil.requestGoogleAccountsAccess(context)
            Log.i(TAG, "Account visibility bridge verified for $email")
        } catch (e: Exception) {
            // Log and rethrow if it's a UserRecoverableAuthException so ViewModel can handle it
            if (e.javaClass.name.contains("UserRecoverableAuthException")) {
                Log.w(TAG, "Account visibility requires user interaction for $email")
                return@withContext Result.failure(e)
            }
            Log.e(TAG, "Account visibility bridge failed for $email: ${e.message}")
            // We do NOT proceed if the bridge fails with a non-recoverable error
            return@withContext Result.failure(e)
        }

        if (driveService != null && initializedEmail == email) {
            Log.d(TAG, "Drive service already initialized for $email. Skipping.")
            return@withContext Result.success(Unit)
        }

        try {
            driveService = GoogleDriveClientFactory.createDriveService(context, email)
            initializedEmail = email
            Log.i(TAG, "Drive service instance created for $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Drive service for $email", e)
            initializedEmail = null
            driveService = null
            Result.failure(e)
        }
    }

    /**
     * Resets the Drive service state, typically used during sign-out or account change.
     */
    fun resetService() {
        driveService = null
        initializedEmail = null
        Log.d(TAG, "Drive service state reset.")
    }

    /**
     * Internal guard to ensure the service is ready for cloud operations.
     */
    private fun getReadyService(): Drive {
        val service = driveService
        if (service == null || initializedEmail.isNullOrBlank()) {
            throw IllegalStateException("Google Drive service not initialized. Call initializeDriveService first.")
        }
        return service
    }

    /**
     * Verifies that the Drive client is active and authorized.
     */
    suspend fun verifyDriveAccess(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val service = getReadyService()
            service.about().get().setFields("user").execute()
            Log.d(TAG, "Drive access verified for $initializedEmail")
            Unit
        }
    }

    /**
     * Discovers or creates the dedicated ERP backup folder.
     */
    suspend fun findOrCreateBackupFolder(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val service = getReadyService()
            
            // 1. Try finding existing folder from settings
            val savedId = settingsManager.getGoogleDriveFolderId()
            if (savedId != null) {
                try {
                    val folder = service.files().get(savedId).execute()
                    if (folder != null && !folder.trashed) {
                        return@runCatching savedId
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Saved folder ID invalid or inaccessible ($savedId). Searching...")
                }
            }

            // 2. Search for folder by name
            val query = "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
            val result = service.files().list().setQ(query).setSpaces("drive").execute()
            val existingFolder = result.files?.firstOrNull()

            if (existingFolder != null) {
                settingsManager.saveGoogleDriveFolderId(existingFolder.id)
                Log.d(TAG, "Found existing ERP backup folder: ${existingFolder.id}")
                return@runCatching existingFolder.id
            }

            // 3. Create new folder if not found
            val metadata = DriveFile().apply {
                setName(FOLDER_NAME)
                setMimeType(FOLDER_MIME_TYPE)
            }
            val newFolder = service.files().create(metadata).setFields("id").execute()
            settingsManager.saveGoogleDriveFolderId(newFolder.id)
            Log.i(TAG, "Created new ERP backup folder: ${newFolder.id}")
            newFolder.id
        }
    }

    /**
     * Creates an atomic copy of the live database for backup processing.
     */
    fun prepareTemporaryBackup(liveDbFile: File): Result<File> = runCatching {
        val tempFile = File(context.cacheDir, TEMP_BACKUP_NAME)
        if (tempFile.exists()) tempFile.delete()
        
        FileInputStream(liveDbFile).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }

    /**
     * Generates SHA-256 for data integrity tracking.
     */
    fun generateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Uploads the temporary backup file to the specified Google Drive folder.
     */
    suspend fun uploadToDrive(
        file: File,
        folderId: String,
        fileName: String,
        appProperties: Map<String, String>
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadToDrive() initiated for $fileName")

        if (fileName.isBlank()) {
            return@withContext Result.failure(Exception("Upload blocked: fileName is null or blank."))
        }

        if (!file.exists()) {
            return@withContext Result.failure(Exception("Upload blocked: local file not found at ${file.absolutePath}"))
        }

        runCatching {
            val service = getReadyService()

            val metadata = DriveFile()
            metadata.setName(fileName)
            metadata.setParents(listOf(folderId))
            metadata.setAppProperties(appProperties)

            val content = FileContent("application/x-sqlite3", file)
            
            val driveFile = service.files().create(metadata, content)
                .setFields("id")
                .execute()

            Log.i(TAG, "Upload SUCCESS. Drive ID: ${driveFile.id}")
            driveFile.id
        }.onFailure { e ->
            Log.e(TAG, "Upload FAILED for $fileName: ${e.message}", e)
        }
    }

    /**
     * Verifies the uploaded file by comparing size, checksum and metadata.
     */
    suspend fun verifyUploadedFile(
        driveFileId: String,
        expectedSize: Long,
        expectedChecksum: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val service = getReadyService()
            
            val driveFile = service.files().get(driveFileId)
                .setFields("id, size, appProperties")
                .execute()

            val actualSize = driveFile.getSize() ?: 0L
            val actualChecksum = driveFile.appProperties?.get("checksumSha256")

            if (actualSize != expectedSize) {
                throw Exception("Verification failed: Size mismatch (Expected: $expectedSize, Actual: $actualSize)")
            }

            if (actualChecksum != expectedChecksum) {
                throw Exception("Verification failed: Checksum mismatch")
            }

            Log.d(TAG, "Uploaded file verification PASSED for $driveFileId")
            Unit
        }
    }

    /**
     * Lists all backup files in the dedicated ERP folder on Google Drive.
     */
    suspend fun listCloudBackups(folderId: String): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val service = getReadyService()
            val query = "'$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, appProperties, createdTime)")
                .execute()
            result.files ?: emptyList()
        }
    }

    /**
     * Downloads a backup file from Google Drive to a local destination.
     */
    suspend fun downloadFromDrive(driveFileId: String, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val service = getReadyService()
            if (destination.exists()) destination.delete()
            
            Log.d(TAG, "DOWNLOAD_START: File $driveFileId to ${destination.absolutePath}")
            FileOutputStream(destination).use { output ->
                service.files().get(driveFileId).executeMediaAndDownloadTo(output)
            }
            Log.i(TAG, "DOWNLOAD_COMPLETE: Local size ${destination.length()}")
            Unit
        }.onFailure { e ->
            Log.e(TAG, "DOWNLOAD_FAILED for $driveFileId: ${e.message}", e)
        }
    }

    fun getTemporaryBackupFile(): File {
        return File(context.cacheDir, TEMP_BACKUP_NAME)
    }

    fun deleteTemporaryBackup() {
        val tempFile = getTemporaryBackupFile()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
