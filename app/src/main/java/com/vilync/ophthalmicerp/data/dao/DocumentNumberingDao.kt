package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vilync.ophthalmicerp.data.entity.DocumentNumberingEntity

@Dao
interface DocumentNumberingDao {

    @Query(
        """
        SELECT * FROM document_series 
        WHERE documentType = :documentType 
        AND financialYearStart = :financialYearStart 
        LIMIT 1
        """
    )
    suspend fun getSeries(documentType: String, financialYearStart: Int): DocumentNumberingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSeries(series: DocumentNumberingEntity)

    @Transaction
    suspend fun getNextSequenceNumber(documentType: String, financialYearStart: Int, defaultPrefix: String): Int {
        val current = getSeries(documentType, financialYearStart)
        val nextNumber = (current?.lastSequenceNumber ?: 0) + 1
        saveSeries(
            DocumentNumberingEntity(
                documentType = documentType,
                financialYearStart = financialYearStart,
                lastSequenceNumber = nextNumber,
                prefix = current?.prefix ?: defaultPrefix,
                padding = current?.padding ?: 4
            )
        )
        return nextNumber
    }
}
