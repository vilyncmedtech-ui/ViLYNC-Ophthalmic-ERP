package com.vilync.ophthalmicerp.feature.backup.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.feature.backup.data.BackupRepository
import com.vilync.ophthalmicerp.feature.backup.domain.BackupUseCase
import com.vilync.ophthalmicerp.feature.backup.logic.*
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository

/**
 * Background Worker for executing automatic nightly backups.
 */
class NightlyBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NIGHTLY_BACKUP_WORKER"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Automatic nightly backup triggered")

        val settingsManager = BackupSettingsManager(applicationContext)
        val email = settingsManager.getGoogleAccountEmail()

        if (email == null) {
            Log.w(TAG, "No Google account persisted. Skipping background backup.")
            return Result.success() // Handled gracefully
        }

        return try {
            val database = DatabaseProvider.getDatabase(applicationContext)
            val backupRepository = BackupRepository(database)
            val integrityVerifier = BackupIntegrityVerifier(applicationContext)
            val backupService = GoogleDriveBackupService(applicationContext, settingsManager)
            val companyProfileRepository = CompanyProfileRepository(database.companyProfileDao())

            val backupUseCase = BackupUseCase(
                context = applicationContext,
                backupRepository = backupRepository,
                integrityVerifier = integrityVerifier,
                backupService = backupService,
                companyProfileRepository = companyProfileRepository,
                auditTrailRepository = AuditTrailRepository(database.auditTrailDao()),
                settingsManager = settingsManager
            )

            // 1. Initialize Drive Client
            val initResult = backupUseCase.initializeDriveService(email)
            if (initResult.isFailure) {
                Log.e(TAG, "Failed to initialize Drive service: ${initResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

            // 2. Perform Workflow (Integrity + Temp Copy + Metadata)
            val workflowResult = backupUseCase.performBackupWorkflow()
            if (workflowResult.isFailure) {
                val error = workflowResult.exceptionOrNull()?.message
                Log.e(TAG, "Background backup workflow failed: $error")
                return Result.retry()
            }

            val record = workflowResult.getOrThrow()
            
            // 3. Execute Upload
            val uploadResult = backupUseCase.uploadBackup(record.id)
            if (uploadResult.isSuccess) {
                Log.d(TAG, "Automatic backup completed and verified successfully.")
                Result.success()
            } else {
                val error = uploadResult.exceptionOrNull()?.message
                Log.e(TAG, "Automatic upload failed: $error")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in background backup", e)
            Result.failure()
        }
    }
}
