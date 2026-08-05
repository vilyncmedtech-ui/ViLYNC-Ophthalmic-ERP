package com.vilync.ophthalmicerp.feature.backup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: BackupMetadataEntity): Long

    @Update
    suspend fun updateMetadata(metadata: BackupMetadataEntity)

    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC")
    fun getAllBackupMetadata(): Flow<List<BackupMetadataEntity>>

    @Query("SELECT * FROM backup_metadata WHERE id = :id LIMIT 1")
    suspend fun getBackupMetadataById(id: Long): BackupMetadataEntity?

    @Query("SELECT * FROM backup_metadata WHERE driveFileId = :driveFileId LIMIT 1")
    suspend fun getBackupMetadataByDriveId(driveFileId: String): BackupMetadataEntity?
}
