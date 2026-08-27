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

    suspend fun initializeDriveService(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Account email is missing for Drive initialization."))
        }
        val result = backupService.initializeDriveService(email)
        Log.d(TAG, "initializeDriveService($email) result: ${result.isSuccess}")
        return result
    }

    /**
     * Resets the cloud service session.
     */
    fun resetSession() {
        backupService.resetService()
    }

    suspend fun listBackups(): Result<List<BackupMetadataEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "listBackups() initiated")
            val folderId = backupService.findOrCreateBackupFolder().getOrThrow()
            val files = backupService.listCloudBackups(folderId).getOrThrow()
            files.map { driveFile ->
                val props = driveFile.appProperties
                Log.d(TAG, "Processing Drive File: ${driveFile.name} | Created: ${driveFile.createdTime}")
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
        Log.i(TAG, "PREPARE_RESTORE_SUMMARY: Start for $driveFileId")
        runCatching {
            val tempFile = File(context.cacheDir, DOWNLOAD_TEMP_NAME)
            
            // 1. Download
            Log.d(TAG, "PREPARE_RESTORE_SUMMARY: Stage 1 - Downloading...")
            backupService.downloadFromDrive(driveFileId, tempFile).getOrThrow()
            
            Log.i(TAG, "PREPARE_RESTORE_SUMMARY: Stage 2 - Downloaded. Size: ${tempFile.length()} bytes")
            if (tempFile.length() == 0L) {
                throw Exception("Downloaded backup file is empty (0 bytes).")
            }

            // 2. Forensic Verification
            Log.d(TAG, "PREPARE_RESTORE_SUMMARY: Stage 3 - Reading Summary...")
            val result = integrityVerifier.getRestoreSummary(tempFile).getOrThrow()
            Log.i(TAG, "PREPARE_RESTORE_SUMMARY: Completed. Data: $result")
            result
        }.onFailure { e ->
            Log.e(TAG, "PREPARE_RESTORE_SUMMARY: FAILED", e)
        }
    }

    suspend fun executeRestore(metadata: BackupMetadataEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val driveFileId = metadata.driveFileId ?: return@withContext Result.failure(Exception("Drive File ID missing."))
        val googleAccount = settingsManager.getGoogleAccountEmail() ?: "Unknown"
        
        val currentJob = coroutineContext[kotlinx.coroutines.Job]
        Log.i(TAG, "RESTORE_STAGE_1_START: Initiating restore for account $googleAccount | Job=$currentJob")

        val liveDbFile = context.getDatabasePath(DB_NAME)
        val emergencyFile = File(context.filesDir, EMERGENCY_BACKUP_NAME)
        val downloadFile = File(context.cacheDir, DOWNLOAD_TEMP_NAME)
        
        var physicalSuccessReached = false
        try {
            // 1. Audit Start
            auditTrailRepository.recordEvent(
                module = "RESTORE",
                action = "RESTORE_STARTED",
                description = "Restore initiated from account $googleAccount for backup timestamp ${metadata.timestamp}."
            )

            // 2. Pre-swap Forensic Validation
            Log.d(TAG, "RESTORE_STAGE_2_DOWNLOAD_START: Target size ${metadata.fileSize} | ID=$driveFileId")
            if (!downloadFile.exists() || downloadFile.length() != metadata.fileSize) {
                backupService.downloadFromDrive(driveFileId, downloadFile).getOrThrow()
            }
            Log.i(TAG, "RESTORE_STAGE_2_DOWNLOAD_COMPLETE: Local size ${downloadFile.length()}")
            
            Log.d(TAG, "RESTORE_STAGE_3_FILE_VALIDATION: Checking integrity and version...")
            val summary = integrityVerifier.getRestoreSummary(downloadFile).getOrThrow()
            if (summary.dbVersion > 28) { // CURRENT_VERSION (Aligned with AppDatabase v28)
                Log.e(TAG, "Restore Blocked: Backup version (${summary.dbVersion}) is newer than current ERP (28).")
                throw Exception("Backup version (${summary.dbVersion}) is newer than current ERP (28).")
            }
            Log.i(TAG, "RESTORE_STAGE_4_FORENSIC_VALID: Compatible version ${summary.dbVersion} | ID confirmed=$driveFileId")

            // 3. Emergency Snapshot
            Log.d(TAG, "RESTORE_STAGE_5_ROLLBACK_INIT: Protecting current database...")
            if (liveDbFile.exists()) {
                copyFile(liveDbFile, emergencyFile)
            }
            Log.i(TAG, "RESTORE_STAGE_6_ROLLBACK_CREATED")

            // 4. Atomic Swap
            Log.d(TAG, "RESTORE_STAGE_7_DATABASE_CLOSE: Locking system... JobState=${currentJob?.isActive}")
            // USE NonCancellable ONLY for the minimal critical database/file-swap section
            withContext(kotlinx.coroutines.NonCancellable) {
                Log.d(TAG, "RESTORE_STAGE_7_EXEC: Closing database...")
                DatabaseProvider.closeDatabase()
                
                Log.d(TAG, "RESTORE_TARGET_VERIFIED: DriveID=$driveFileId")
                Log.d(TAG, "RESTORE_STAGE_8_ATOMIC_SWAP: Replacing physical file...")
                copyFile(downloadFile, liveDbFile)
            }

            // 5. Post-swap Validation
            Log.d(TAG, "RESTORE_STAGE_9_VERIFICATION: Validating new database handle...")
            integrityVerifier.validatePostRestore(liveDbFile).onFailure {
                Log.e(TAG, "Post-restore validation failed. ROLLING BACK...")
                rollback(emergencyFile, liveDbFile)
                throw it
            }

            // PHYSICAL SUCCESS FLAG: Once we pass Stage 9, we consider the restore successful 
            // even if a lifecycle cancellation signal is received during final cleanup.
            physicalSuccessReached = true
            Log.i(TAG, "RESTORE_STAGE_10_COMPLETE: Physical swap and verification SUCCESS.")

            // Cleanup
            if (emergencyFile.exists()) emergencyFile.delete()
            if (downloadFile.exists()) downloadFile.delete()
            
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException && physicalSuccessReached) {
                Log.i(TAG, "RESTORE_FINAL_CLEANUP_CANCELLED: But physical swap was already verified. Returning Success.")
                return@withContext Result.success(Unit)
            }
            
            Log.e(TAG, "RESTORE FAILURE at Stage: ${e.message} | JobState=${currentJob?.isActive}", e)
            val rollbackStatus = if (emergencyFile.exists()) "PENDING/ROLLING_BACK" else "NO_EMERGENCY_SNAPSHOT"
            
            // Note: This write will likely fail if the DB is closed, but it's kept for audit continuity if possible.
            try {
                auditTrailRepository.recordEvent(
                    module = "RESTORE",
                    action = "RESTORE_FAILURE",
                    description = "Restore failed: ${e.message}. Account: $googleAccount, Rollback: $rollbackStatus"
                )
            } catch (auditError: Exception) {
                Log.w(TAG, "Could not record failure audit: ${auditError.message}")
            }
            
            Result.failure(e)
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
        Log.i(TAG, "BACKUP_STAGE_1_START: Initiating local workflow")
        runCatching {
            val liveDbFile = context.getDatabasePath(DB_NAME)
            
            // 1. Integrity Verification
            Log.d(TAG, "BACKUP_STAGE_2_INTEGRITY: Verifying live database...")
            val integrityReport = integrityVerifier.verify(liveDbFile)
            if (!integrityReport.isSuccess) {
                throw Exception("Backup aborted: ${integrityReport.message}")
            }

            // 2. FORCE WAL CHECKPOINT (Ensure all data is flushed from -wal to .db)
            Log.i(TAG, "BACKUP_STAGE_3_CHECKPOINT: Forcing WAL flush...")
            val checkpointStart = System.currentTimeMillis()
            DatabaseProvider.checkpoint(context)
            Log.d(TAG, "BACKUP_STAGE_3_COMPLETE: Checkpoint took ${System.currentTimeMillis() - checkpointStart}ms")

            // 3. Prepare Temporary Copy
            Log.d(TAG, "BACKUP_STAGE_4_SNAPSHOT: Copying database file...")
            val tempFile = backupService.prepareTemporaryBackup(liveDbFile).getOrThrow()
            Log.i(TAG, "BACKUP_STAGE_4_COMPLETE: Snapshot created. Size=${tempFile.length()} bytes")

            // 4. Metadata Generation
            Log.d(TAG, "BACKUP_STAGE_5_METADATA: Generating checksum and metadata...")
            val companyProfile = companyProfileRepository.getCompanyProfile()
            val checksum = backupService.generateChecksum(tempFile)
            
            val metadata = BackupMetadataEntity(
                erpVersion = "1.0.0", // To be pulled from BuildConfig in Phase 2
                dbVersion = 28,
                timestamp = System.currentTimeMillis(),
                companyName = companyProfile?.legalName?.takeIf { it.isNotBlank() } ?: "ViLYNC_ERP",
                companyGst = companyProfile?.gstin ?: "N/A",
                fileSize = tempFile.length(),
                checksumSha256 = checksum,
                integrityResult = "PASSED",
                status = "PENDING"
            )

            // 5. Persist Metadata locally
            val recordId = backupRepository.saveBackupRecord(metadata)
            Log.i(TAG, "BACKUP_STAGE_6_FINISH: Local record $recordId created.")
            
            metadata.copy(id = recordId)
        }.onFailure { e ->
            Log.e(TAG, "BACKUP_WORKFLOW_FAILED", e)
        }
    }

    /**
     * Executes the full upload lifecycle: Pending -> Uploading -> Uploaded -> Verified -> Completed.
     */
    suspend fun uploadBackup(recordId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "UPLOAD_STAGE_1_START: Initiating cloud upload for record $recordId")
        val record = backupRepository.getBackupById(recordId)
            ?: return@withContext Result.failure(Exception("Backup record not found: $recordId"))

        runCatching {
            // 1. STATE: UPLOADING
            backupRepository.updateBackupRecord(record.copy(status = "UPLOADING"))

            // 2. Folder Validation / Recovery
            Log.d(TAG, "UPLOAD_STAGE_2_FOLDER: Resolving target folder...")
            val folderId = backupService.findOrCreateBackupFolder().getOrThrow()

            // 3. Prepare unique filename
            val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US)
                .format(java.util.Date(record.timestamp))
            val cleanCompanyName = record.companyName.replace(Regex("[^A-Za-z0-9]"), "_").takeIf { it.isNotBlank() } ?: "ViLYNC_ERP"
            val fileName = "ViLYNC_ERP_Backup_${cleanCompanyName}_$timestampStr.db"

            Log.d(TAG, "UPLOAD_STAGE_3_METADATA: Target filename $fileName")

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
            Log.i(TAG, "UPLOAD_STAGE_4_TRANSFER: Starting byte stream to Google Drive...")
            val uploadStart = System.currentTimeMillis()
            val driveFileId = backupService.uploadToDrive(
                file = tempFile,
                folderId = folderId,
                fileName = fileName,
                appProperties = appProperties
            ).getOrThrow()
            Log.d(TAG, "UPLOAD_STAGE_4_COMPLETE: Transfer took ${System.currentTimeMillis() - uploadStart}ms")

            // 6. STATE: UPLOADED
            backupRepository.updateBackupRecord(record.copy(
                status = "UPLOADED",
                driveFileId = driveFileId
            ))

            // 7. Post-Upload Verification
            Log.d(TAG, "UPLOAD_STAGE_5_VERIFY: verifying remote integrity...")
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
            
            Log.i(TAG, "UPLOAD_STAGE_6_FINISH: Backup successfully verified and completed.")
            backupService.deleteTemporaryBackup()

        }.onFailure { e ->
            Log.e(TAG, "UPLOAD_FAILED for record $recordId", e)
            backupRepository.updateBackupRecord(record.copy(status = "FAILED"))
        }
    }

    suspend fun retryUpload(recordId: Long): Result<Unit> {
        return uploadBackup(recordId)
    }
}
