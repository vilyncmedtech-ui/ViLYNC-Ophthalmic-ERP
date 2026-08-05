package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import android.util.Log
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
 * - Google Drive service initialization.
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

    /**
     * Initializes the Drive client using the factory.
     */
    fun initializeDriveService(email: String) {
        driveService = GoogleDriveClientFactory.createDriveService(context, email)
        Log.d(TAG, "Drive service initialized for $email")
    }

    /**
     * Verifies that the Drive client is active and authorized.
     */
    suspend fun verifyDriveAccess(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val service = driveService ?: throw Exception("Drive service not initialized.")
            service.about().get().setFields("user").execute()
            Log.d(TAG, "Drive access verified.")
            Unit
        }
    }

    /**
     * Discovers or creates the dedicated ERP backup folder.
     */
    suspend fun findOrCreateBackupFolder(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val service = driveService ?: throw Exception("Drive service not initialized.")
            
            // 1. Try finding existing folder from settings
            val savedId = settingsManager.getGoogleDriveFolderId()
            if (savedId != null) {
                try {
                    val folder = service.files().get(savedId).execute()
                    if (folder != null && !folder.trashed) {
                        return@runCatching savedId
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Saved folder ID invalid or inaccessible. Searching...")
                }
            }

            // 2. Search for folder by name
            val query = "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
            val result = service.files().list().setQ(query).setSpaces("drive").execute()
            val existingFolder = result.files.firstOrNull()

            if (existingFolder != null) {
                settingsManager.saveGoogleDriveFolderId(existingFolder.id)
                return@runCatching existingFolder.id
            }

            // 3. Create new folder if not found
            val metadata = DriveFile().apply {
                setName(FOLDER_NAME)
                setMimeType(FOLDER_MIME_TYPE)
            }
            val newFolder = service.files().create(metadata).setFields("id").execute()
            settingsManager.saveGoogleDriveFolderId(newFolder.id)
            Log.d(TAG, "Created new ERP backup folder: ${newFolder.id}")
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
        Log.d(TAG, "uploadToDrive() - Start")
        Log.d(TAG, "Target Filename: $fileName")
        Log.d(TAG, "Local File: ${file.absolutePath} | Exists: ${file.exists()} | Size: ${file.length()}")
        Log.d(TAG, "Parent Folder ID: $folderId")

        if (fileName.isBlank()) {
            return@withContext Result.failure(Exception("Upload blocked: fileName is null or blank."))
        }

        if (!file.exists()) {
            return@withContext Result.failure(Exception("Upload blocked: local file not found at ${file.absolutePath}"))
        }

        runCatching {
            val service = driveService ?: throw Exception("Drive service not initialized.")

            val metadata = DriveFile()
            metadata.setName(fileName)
            metadata.setParents(listOf(folderId))
            metadata.setAppProperties(appProperties)

            Log.d(TAG, "Drive Metadata Name set to: ${metadata.name}")

            val content = FileContent("application/x-sqlite3", file)
            
            Log.d(TAG, "Executing Drive API create request...")
            val driveFile = service.files().create(metadata, content)
                .setFields("id")
                .execute()

            Log.d(TAG, "Upload SUCCESS. Drive ID: ${driveFile.id}")
            driveFile.id
        }.onFailure { e ->
            Log.e(TAG, "Upload FAILED at Drive API level: ${e.message}", e)
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
            val service = driveService ?: throw Exception("Drive service not initialized.")
            
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

            Log.d(TAG, "Uploaded file verified successfully.")
            Unit
        }
    }

    /**
     * Lists all backup files in the dedicated ERP folder on Google Drive.
     */
    suspend fun listCloudBackups(folderId: String): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val service = driveService ?: throw Exception("Drive service not initialized.")
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
            val service = driveService ?: throw Exception("Drive service not initialized.")
            if (destination.exists()) destination.delete()
            
            FileOutputStream(destination).use { output ->
                service.files().get(driveFileId).executeMediaAndDownloadTo(output)
            }
            Log.d(TAG, "File downloaded successfully: ${destination.absolutePath}")
            Unit
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
