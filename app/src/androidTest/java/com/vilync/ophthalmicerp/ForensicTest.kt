package com.vilync.ophthalmicerp

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.feature.backup.data.BackupRepository
import com.vilync.ophthalmicerp.feature.backup.domain.BackupUseCase
import com.vilync.ophthalmicerp.feature.backup.logic.*
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForensicTest {

    @Test
    fun performInvestigation() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsManager = BackupSettingsManager(context)
        val email = "vilyncmedtech@gmail.com"

        val database = DatabaseProvider.getDatabase(context)
        val backupRepository = BackupRepository(database)
        val integrityVerifier = BackupIntegrityVerifier(context)
        val backupService = GoogleDriveBackupService(context, settingsManager)
        val companyProfileRepository = CompanyProfileRepository(database.companyProfileDao())

        val backupUseCase = BackupUseCase(
            context = context,
            backupRepository = backupRepository,
            integrityVerifier = integrityVerifier,
            backupService = backupService,
            companyProfileRepository = companyProfileRepository,
            auditTrailRepository = AuditTrailRepository(database.auditTrailDao()),
            settingsManager = settingsManager
        )

        Log.i("FORENSIC", "Investigation Started")

        // 1. Initialize
        backupUseCase.initializeDriveService(email).getOrThrow()

        // 2. List Backups
        val backups = backupUseCase.listBackups().getOrThrow()
        val targetBackup = backups.find { it.companyName.contains("20260810_0719") }
        
        if (targetBackup != null) {
            Log.i("FORENSIC", "Target Backup Found. ID: ${targetBackup.driveFileId}")
            
            // 3. Prepare Restore Summary
            val summaryResult = backupUseCase.prepareRestoreSummary(targetBackup.driveFileId!!)
            if (summaryResult.isSuccess) {
                val summary = summaryResult.getOrThrow()
                Log.i("FORENSIC", "SUMMARY_DATA: $summary")
            } else {
                Log.e("FORENSIC", "Failed to get summary: ${summaryResult.exceptionOrNull()?.message}")
            }
        } else {
            Log.e("FORENSIC", "Target backup not found in list. Available backups: ${backups.map { it.companyName }}")
        }
        
        Log.i("FORENSIC", "Investigation Finished")
        }
    }
}
