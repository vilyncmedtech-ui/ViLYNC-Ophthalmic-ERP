package com.vilync.ophthalmicerp.feature.backup.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Production-grade metadata record for tracking ERP backups.
 */
@Entity(tableName = "backup_metadata")
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val erpVersion: String,
    val dbVersion: Int,
    val timestamp: Long,
    
    val companyName: String,
    val companyGst: String,
    
    val fileSize: Long,
    val checksumSha256: String,
    
    val integrityResult: String, // e.g. "PASSED", "FAILED"
    
    val driveFileId: String? = null,
    val status: String // PENDING, UPLOADING, UPLOADED, VERIFIED, COMPLETED, FAILED
)
