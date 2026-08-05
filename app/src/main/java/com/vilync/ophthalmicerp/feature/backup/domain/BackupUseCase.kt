package com.vilync.ophthalmicerp.feature.backup.domain

import android.content.Context
import com.vilync.ophthalmicerp.feature.backup.data.BackupMetadataEntity
import com.vilync.ophthalmicerp.feature.backup.data.BackupRepository
import com.vilync.ophthalmicerp.feature.backup.logic.BackupIntegrityVerifier
import com.vilync.ophthalmicerp.feature.backup.logic.GoogleDriveBackupService
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.feature.backup.logic.BackupSettingsManager
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupUseCase(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val integrityVerifier: BackupIntegrityVerifier,
    private val backupService: GoogleDriveBackupService,
    private val companyProfileRepository: CompanyProfileRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val settingsManager: BackupSettingsManager
) {

    companion object {
        private const val TAG = "BACKUP_USE_CASE"
        private const val DB_NAME = "vilync_ophthalmic_erp_database"
        private const val EMERGENCY_BACKUP_NAME = "emergency_rollback_backup.db"
        private const val DOWNLOAD_TEMP_NAME = "cloud_restore_temp.db"
    }

    fun initializeDriveService(email: String) {
        backupService.initializeDriveService(email)
    }

    suspend fun listBackups(): Result<List<BackupMetadataEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = backupService.findOrCreateBackupFolder().getOrThrow()
            val files = backupService.listCloudBackups(folderId).getOrThrow()
            files.map { driveFile ->
                val props = driveFile.appProperties
                BackupMetadataEntity(
                    driveFileId = driveFile.id,
                    timestamp = driveFile.createdTime.value,
                    fileSize = driveFile.getSize() ?: 0L,
                    erpVersion = props?.get("erpVersion") ?: "Unknown",
                    dbVersion = props?.get("dbVersion")?.toIntOrNull() ?: 0,
                    companyName = driveFile.name,
                    companyGst = props?.get("companyGst") ?: "N/A",
                    checksumSha256 = props?.get("checksumSha256") ?: "",
                    integrityResult = "UNKNOWN",
                    status = "COMPLETED"
                )
            }
        }
    }

    suspend fun prepareRestoreSummary(driveFileId: String): Result<BackupIntegrityVerifier.RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, DOWNLOAD_TEMP_NAME)
            backupService.downloadFromDrive(driveFileId, tempFile).getOrThrow()
            
            // Forensic Verification
            integrityVerifier.getRestoreSummary(tempFile).getOrThrow()
        }
    }

    /**
     * Safety-critical Atomic Restore Workflow with Automatic Rollback.
     */
    suspend fun executeRestore(metadata: BackupMetadataEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val driveFileId = metadata.driveFileId ?: return@withContext Result.failure(Exception("Drive File ID missing."))
        val googleAccount = settingsManager.getGoogleAccountEmail() ?: "Unknown"
        
        val liveDbFile = context.getDatabasePath(DB_NAME)
        val emergencyFile = File(context.filesDir, EMERGENCY_BACKUP_NAME)
        val downloadFile = File(context.cacheDir, DOWNLOAD_TEMP_NAME)
        
        runCatching {
            // 1. Audit Start
            auditTrailRepository.recordEvent(
                module = "RESTORE",
                action = "RESTORE_STARTED",
                description = "Restore initiated from account $googleAccount for backup timestamp ${metadata.timestamp}."
            )

            // 2. Pre-swap Forensic Validation
            if (!downloadFile.exists() || downloadFile.length() != metadata.fileSize) {
                backupService.downloadFromDrive(driveFileId, downloadFile).getOrThrow()
            }
            
            val summary = integrityVerifier.getRestoreSummary(downloadFile).getOrThrow()
            if (summary.dbVersion > 25) { // CURRENT_VERSION
                throw Exception("Backup version (${summary.dbVersion}) is newer than current ERP (25).")
            }

            // 3. Emergency Snapshot
            if (liveDbFile.exists()) {
                copyFile(liveDbFile, emergencyFile)
            }

            // 4. Atomic Swap
            DatabaseProvider.closeDatabase()
            copyFile(downloadFile, liveDbFile)

            // 5. Post-swap Validation
            integrityVerifier.validatePostRestore(liveDbFile).onFailure {
                Log.e(TAG, "Post-restore validation failed. Rolling back...")
                rollback(emergencyFile, liveDbFile)
                throw it
            }

            // 6. Audit Success
            auditTrailRepository.recordEvent(
                module = "RESTORE",
                action = "RESTORE_SUCCESS",
                description = "Database restored successfully. Account: $googleAccount, Rollback: NOT_REQUIRED"
            )

            // Cleanup
            if (emergencyFile.exists()) emergencyFile.delete()
            if (downloadFile.exists()) downloadFile.delete()
            
            Log.d(TAG, "Restore completed successfully.")
            Unit
        }.onFailure { e ->
            Log.e(TAG, "Restore failed: ${e.message}")
            val rollbackStatus = if (emergencyFile.exists()) "PENDING/ROLLING_BACK" else "NO_EMERGENCY_SNAPSHOT"
            auditTrailRepository.recordEvent(
                module = "RESTORE",
                action = "RESTORE_FAILURE",
                description = "Restore failed: ${e.message}. Account: $googleAccount, Rollback: $rollbackStatus"
            )
            Result.failure<Unit>(e)
        }
    }

    private fun rollback(emergencyFile: File, liveDbFile: File) {
        try {
            if (emergencyFile.exists()) {
                copyFile(emergencyFile, liveDbFile)
                settingsManager.setRollbackOccurred(true)
                Log.i(TAG, "Rollback successful.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Rollback failed!", e)
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { input ->
            FileOutputStream(dst).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun performBackupWorkflow(): Result<BackupMetadataEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val liveDbFile = context.getDatabasePath(DB_NAME)
            
            // 1. Integrity Verification
            val integrityReport = integrityVerifier.verify(liveDbFile)
            if (!integrityReport.isSuccess) {
                throw Exception("Backup aborted: ${integrityReport.message}")
            }

            // 2. Prepare Temporary Copy
            val tempFile = backupService.prepareTemporaryBackup(liveDbFile).getOrThrow()

            // 3. Metadata Generation
            val companyProfile = companyProfileRepository.getCompanyProfile()
            val checksum = backupService.generateChecksum(tempFile)
            
            val metadata = BackupMetadataEntity(
                erpVersion = "1.0.0", // To be pulled from BuildConfig in Phase 2
                dbVersion = 25,
                timestamp = System.currentTimeMillis(),
                companyName = companyProfile?.legalName?.takeIf { it.isNotBlank() } ?: "ViLYNC_ERP",
                companyGst = companyProfile?.gstin ?: "N/A",
                fileSize = tempFile.length(),
                checksumSha256 = checksum,
                integrityResult = "PASSED",
                status = "PENDING"
            )

            // 4. Persist Metadata locally
            val recordId = backupRepository.saveBackupRecord(metadata)
            
            metadata.copy(id = recordId)
        }
    }

    /**
     * Executes the full upload lifecycle: Pending -> Uploading -> Uploaded -> Verified -> Completed.
     */
    suspend fun uploadBackup(recordId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val record = backupRepository.getBackupById(recordId)
            ?: return@withContext Result.failure(Exception("Backup record not found: $recordId"))

        runCatching {
            // 1. STATE: UPLOADING
            backupRepository.updateBackupRecord(record.copy(status = "UPLOADING"))

            // 2. Folder Validation / Recovery
            val folderId = backupService.findOrCreateBackupFolder().getOrThrow()

            // 3. Prepare unique filename
            val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US)
                .format(java.util.Date(record.timestamp))
            val cleanCompanyName = record.companyName.replace(Regex("[^A-Za-z0-9]"), "_").takeIf { it.isNotBlank() } ?: "ViLYNC_ERP"
            val fileName = "ViLYNC_ERP_Backup_${cleanCompanyName}_$timestampStr.db"

            Log.d(TAG, "Generated Filename: $fileName")

            // 4. Construct App Properties (Forensic Metadata)
            val appProperties = mapOf(
                "erpVersion" to record.erpVersion,
                "dbVersion" to record.dbVersion.toString(),
                "checksumSha256" to record.checksumSha256,
                "timestamp" to record.timestamp.toString(),
                "companyGst" to record.companyGst
            )

            val tempFile = backupService.getTemporaryBackupFile()
            if (!tempFile.exists()) {
                throw Exception("Temporary backup file missing. Please re-run backup workflow.")
            }

            // 5. Execute Upload
            val driveFileId = backupService.uploadToDrive(
                file = tempFile,
                folderId = folderId,
                fileName = fileName,
                appProperties = appProperties
            ).getOrThrow()

            // 6. STATE: UPLOADED
            backupRepository.updateBackupRecord(record.copy(
                status = "UPLOADED",
                driveFileId = driveFileId
            ))

            // 7. Post-Upload Verification
            backupService.verifyUploadedFile(
                driveFileId = driveFileId,
                expectedSize = record.fileSize,
                expectedChecksum = record.checksumSha256
            ).getOrThrow()

            // 8. STATE: VERIFIED -> COMPLETED
            backupRepository.updateBackupRecord(record.copy(
                status = "COMPLETED",
                driveFileId = driveFileId
            ))
            
            Log.d(TAG, "Backup $recordId uploaded and verified successfully.")
            backupService.deleteTemporaryBackup()

        }.onFailure { e ->
            Log.e(TAG, "Upload failed for record $recordId", e)
            backupRepository.updateBackupRecord(record.copy(status = "FAILED"))
        }
    }

    suspend fun retryUpload(recordId: Long): Result<Unit> {
        return uploadBackup(recordId)
    }
}
