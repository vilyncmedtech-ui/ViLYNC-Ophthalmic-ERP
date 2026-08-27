package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleIssueDao {

    // =========================================================
    // CREATE
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSampleIssue(
        sampleIssue: SampleIssueEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSampleIssueItems(
        items: List<SampleIssueItemEntity>
    )

    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun updateSampleIssue(
        sampleIssue: SampleIssueEntity
    )

    // =========================================================
    // DOCUMENT / REGISTER
    // =========================================================

    @Query(
        """
        SELECT * FROM sample_issues
        WHERE id = :sampleIssueId
        LIMIT 1
        """
    )
    suspend fun getSampleIssueById(
        sampleIssueId: Long
    ): SampleIssueEntity?

    @Query(
        """
        SELECT * FROM sample_issues
        ORDER BY id DESC
        """
    )
    fun getAllSampleIssues(): Flow<List<SampleIssueEntity>>

    @Query(
        """
        SELECT * FROM sample_issues
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getSampleIssuesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SampleIssueEntity>>

    @Query(
        """
        SELECT * FROM sample_issues
        WHERE customerId = :customerId
        ORDER BY id DESC
        """
    )
    fun getSampleIssuesForCustomer(
        customerId: Long
    ): Flow<List<SampleIssueEntity>>

    @Query(
        """
        SELECT * FROM sample_issue_items
        WHERE sampleIssueId = :sampleIssueId
        ORDER BY productName COLLATE NOCASE ASC,
                 power COLLATE NOCASE ASC,
                 serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun getItemsBySampleIssueId(
        sampleIssueId: Long
    ): List<SampleIssueItemEntity>

    // =========================================================
    // ACTIVE SAMPLE PROTECTION
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sample_issue_items i
            INNER JOIN sample_issues s
                ON s.id = i.sampleIssueId
            WHERE i.inventoryUnitId = :inventoryUnitId
              AND i.settlementStatus = 'ISSUED'
              AND s.status != 'CANCELLED'
        )
        """
    )
    suspend fun inventoryUnitHasActiveSampleIssue(
        inventoryUnitId: Long
    ): Boolean

    // =========================================================
    // ITEM LIFECYCLE
    // =========================================================

    @Query(
        """
        UPDATE sample_issue_items
        SET settlementStatus = 'RETURNED',
            returnedAt = :returnedAt,
            updatedAt = :returnedAt
        WHERE id = :sampleIssueItemId
          AND settlementStatus = 'ISSUED'
        """
    )
    suspend fun markItemReturned(
        sampleIssueItemId: Long,
        returnedAt: Long
    ): Int

    @Query(
        """
        UPDATE sample_issue_items
        SET settlementStatus = 'CONSUMED',
            consumedAt = :consumedAt,
            updatedAt = :consumedAt
        WHERE id = :sampleIssueItemId
          AND settlementStatus = 'ISSUED'
        """
    )
    suspend fun markItemConsumed(
        sampleIssueItemId: Long,
        consumedAt: Long
    ): Int

    // =========================================================
    // COUNTS / HEADER STATUS
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM sample_issue_items
        WHERE sampleIssueId = :sampleIssueId
        """
    )
    suspend fun getTotalItemCount(
        sampleIssueId: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM sample_issue_items
        WHERE sampleIssueId = :sampleIssueId
          AND settlementStatus = 'ISSUED'
        """
    )
    suspend fun getIssuedItemCount(
        sampleIssueId: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM sample_issue_items
        WHERE sampleIssueId = :sampleIssueId
          AND settlementStatus = 'EVALUATED'
        """
    )
    suspend fun getEvaluatedItemCount(
        sampleIssueId: Long
    ): Int

    @Query(
        """
        UPDATE sample_issues
        SET status = :status,
            updatedAt = :updatedAt
        WHERE id = :sampleIssueId
        """
    )
    suspend fun updateSampleIssueStatus(
        sampleIssueId: Long,
        status: String,
        updatedAt: Long
    )

    @Query("SELECT COUNT(*) FROM sample_issues WHERE status = 'ISSUED'")
    fun observePendingSampleCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM sample_issues 
        WHERE (sampleIssueNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')
          AND status != 'CANCELLED'
        ORDER BY id DESC LIMIT 20
        """
    )
    suspend fun searchSamples(query: String): List<SampleIssueEntity>


    @Query("DELETE FROM sample_issue_items WHERE sampleIssueId = :sampleIssueId")
    suspend fun deleteItemsBySampleIssueId(sampleIssueId: Long)

    @Query("SELECT * FROM sample_issue_items WHERE sampleIssueId = :sampleIssueId")
    suspend fun getItemsBySampleIssueIdList(sampleIssueId: Long): List<SampleIssueItemEntity>

    // =========================================================
    // DUPLICATE NUMBER PROTECTION
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sample_issues
            WHERE normalizedSampleIssueNumber = :normalizedSampleIssueNumber
              AND financialYearStart = :financialYearStart
        )
        """
    )
    suspend fun sampleIssueNumberExists(
        normalizedSampleIssueNumber: String,
        financialYearStart: Int
    ): Boolean
    @Query(
        """
        UPDATE sample_issue_items
        SET settlementStatus = :status,
            updatedAt = :updatedAt
        WHERE id = :sampleIssueItemId
        """
    )
    suspend fun updateSampleIssueItemSettlementStatus(
        sampleIssueItemId: Long,
        status: String,
        updatedAt: Long
    )
    @Query(
        """
        SELECT i.*
        FROM sample_issue_items i
        INNER JOIN sample_issues s ON s.id = i.sampleIssueId
        WHERE s.customerId = :customerId
          AND i.settlementStatus = 'EVALUATED'
          AND s.status != 'CANCELLED'
          AND UPPER(TRIM(i.serialNumber)) LIKE '%' || UPPER(TRIM(:query))
        ORDER BY i.serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun findEvaluatedSampleItemsForCustomer(
        customerId: Long,
        query: String
    ): List<SampleIssueItemEntity>
}
