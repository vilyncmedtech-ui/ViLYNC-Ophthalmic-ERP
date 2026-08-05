package com.vilync.ophthalmicerp.feature.backup.data

import com.vilync.ophthalmicerp.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow

class BackupRepository(private val database: AppDatabase) {

    private val dao = database.backupMetadataDao()

    suspend fun saveBackupRecord(metadata: BackupMetadataEntity): Long {
        return dao.insertMetadata(metadata)
    }

    suspend fun updateBackupRecord(metadata: BackupMetadataEntity) {
        dao.updateMetadata(metadata)
    }

    fun getBackupHistory(): Flow<List<BackupMetadataEntity>> {
        return dao.getAllBackupMetadata()
    }

    suspend fun getBackupById(id: Long): BackupMetadataEntity? {
        return dao.getBackupMetadataById(id)
    }
}
