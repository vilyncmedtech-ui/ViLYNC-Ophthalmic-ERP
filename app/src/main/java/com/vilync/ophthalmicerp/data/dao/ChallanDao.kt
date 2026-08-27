package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.feature.sales.reports.data.LensLibraryRawRow
import kotlinx.coroutines.flow.Flow


@Dao
interface ChallanDao {

    // =========================================================
    // CREATE CHALLAN
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertChallan(
        challan: ChallanEntity
    ): Long


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertChallanItems(
        items: List<ChallanItemEntity>
    )


    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun updateChallan(
        challan: ChallanEntity
    )


    @Update
    suspend fun updateChallanItem(
        item: ChallanItemEntity
    )


    // =========================================================
    // CHALLAN BY ID
    // =========================================================

    @Query(
        """
        SELECT * FROM challans
        WHERE id = :challanId
        LIMIT 1
        """
    )
    suspend fun getChallanById(
        challanId: Long
    ): ChallanEntity?


    // =========================================================
    // ITEMS BY CHALLAN
    // =========================================================

    @Query(
        """
        SELECT * FROM challan_items
        WHERE challanId = :challanId
        ORDER BY id ASC
        """
    )
    suspend fun getItemsByChallanId(
        challanId: Long
    ): List<ChallanItemEntity>


    // =========================================================
    // PENDING ITEMS BY CHALLAN
    // =========================================================

    @Query(
        """
        SELECT * FROM challan_items
        WHERE challanId = :challanId
          AND settlementStatus = 'PENDING'
        ORDER BY productName COLLATE NOCASE ASC,
                 power COLLATE NOCASE ASC,
                 serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun getPendingItemsByChallanId(
        challanId: Long
    ): List<ChallanItemEntity>


    // =========================================================
    // CUSTOMER PENDING CHALLANS
    // =========================================================

    @Query(
        """
        SELECT DISTINCT c.*
        FROM challans c
        INNER JOIN challan_items ci
            ON ci.challanId = c.id
        WHERE c.customerId = :customerId
          AND c.status != 'CANCELLED'
          AND ci.settlementStatus = 'PENDING'
        ORDER BY c.id DESC
        """
    )
    fun getPendingChallansForCustomer(
        customerId: Long
    ): Flow<List<ChallanEntity>>


    // =========================================================
    // CUSTOMER PENDING CHALLAN ITEMS
    // =========================================================

    @Query(
        """
        SELECT ci.*
        FROM challan_items ci
        INNER JOIN challans c
            ON c.id = ci.challanId
        WHERE c.customerId = :customerId
          AND c.status != 'CANCELLED'
          AND ci.settlementStatus = 'PENDING'
        ORDER BY c.id DESC,
                 ci.productName COLLATE NOCASE ASC,
                 ci.power COLLATE NOCASE ASC,
                 ci.serialNumber COLLATE NOCASE ASC
        """
    )
    fun getPendingItemsForCustomer(
        customerId: Long
    ): Flow<List<ChallanItemEntity>>


    // =========================================================
    // EXACT SERIAL SEARCH
    // =========================================================

    @Query(
        """
        SELECT ci.*
        FROM challan_items ci
        INNER JOIN challans c
            ON c.id = ci.challanId
        WHERE c.customerId = :customerId
          AND c.status != 'CANCELLED'
          AND ci.settlementStatus = 'PENDING'
          AND UPPER(TRIM(ci.serialNumber)) =
              UPPER(TRIM(:serialNumber))
        LIMIT 1
        """
    )
    suspend fun findPendingExactSerialForCustomer(
        customerId: Long,
        serialNumber: String
    ): ChallanItemEntity?


    // =========================================================
    // SMART SUFFIX SERIAL SEARCH
    // =========================================================

    @Query(
        """
        SELECT ci.*
        FROM challan_items ci
        INNER JOIN challans c
            ON c.id = ci.challanId
        WHERE c.customerId = :customerId
          AND c.status != 'CANCELLED'
          AND ci.settlementStatus = 'PENDING'
          AND UPPER(TRIM(ci.serialNumber))
              LIKE '%' || UPPER(TRIM(:serialSuffix))
        ORDER BY ci.serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun findPendingSerialSuffixForCustomer(
        customerId: Long,
        serialSuffix: String
    ): List<ChallanItemEntity>


    // =========================================================
    // ITEM BY INVENTORY UNIT
    // =========================================================

    @Query(
        """
        SELECT * FROM challan_items
        WHERE inventoryUnitId = :inventoryUnitId
        LIMIT 1
        """
    )
    suspend fun getItemByInventoryUnitId(
        inventoryUnitId: Long
    ): ChallanItemEntity?


    // =========================================================
    // MARK PHYSICAL CHALLAN LENS AS INVOICED
    // =========================================================

    @Query(
        """
        UPDATE challan_items
        SET settlementStatus = 'INVOICED',
            saleId = :saleId,
            settledAt = :settledAt,
            updatedAt = :settledAt
        WHERE id = :challanItemId
          AND settlementStatus = 'PENDING'
        """
    )
    suspend fun markItemInvoiced(
        challanItemId: Long,
        saleId: Long,
        settledAt: Long
    ): Int


    // =========================================================
    // PENDING ITEM COUNT
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM challan_items
        WHERE challanId = :challanId
          AND settlementStatus = 'PENDING'
        """
    )
    suspend fun getPendingItemCount(
        challanId: Long
    ): Int


    // =========================================================
    // TOTAL ITEM COUNT
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM challan_items
        WHERE challanId = :challanId
        """
    )
    suspend fun getTotalItemCount(
        challanId: Long
    ): Int


    // =========================================================
    // UPDATE CHALLAN STATUS
    // =========================================================

    @Query(
        """
        UPDATE challans
        SET status = :status,
            updatedAt = :updatedAt
        WHERE id = :challanId
        """
    )
    suspend fun updateChallanStatus(
        challanId: Long,
        status: String,
        updatedAt: Long
    )


    // =========================================================
    // ALL CHALLANS (REGISTER)
    // =========================================================

    @Query("SELECT * FROM challans ORDER BY id DESC")
    fun getAllChallans(): Flow<List<ChallanEntity>>


    @Query("SELECT COUNT(*) FROM challans WHERE status IN ('OPEN', 'PARTIALLY_SETTLED')")
    fun observeOpenChallanCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM challans 
        WHERE (challanNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')
          AND status != 'CANCELLED'
        ORDER BY id DESC LIMIT 20
        """
    )
    suspend fun searchChallans(query: String): List<ChallanEntity>


    // =========================================================
    // DUPLICATE CHALLAN NUMBER PROTECTION
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM challans
            WHERE normalizedChallanNumber =
                  :normalizedChallanNumber
              AND financialYearStart =
                  :financialYearStart
        )
        """
    )
    suspend fun challanNumberExists(
        normalizedChallanNumber: String,
        financialYearStart: Int
    ): Boolean

    // =========================================================
    // LENS LIBRARY STATUS REPORT QUERY
    // =========================================================

    @Query(
        """
        SELECT 
            ci.productId,
            ci.productName,
            ci.power,
            ci.settlementStatus,
            ci.serialNumber,
            ci.expiryDate,
            c.id as challanId,
            c.challanNumber,
            c.challanDate,
            ci.saleId,
            s.invoiceNumber,
            s.invoiceDate
        FROM challan_items ci
        INNER JOIN challans c ON ci.challanId = c.id
        LEFT JOIN sales s ON ci.saleId = s.id
        WHERE c.customerId = :customerId
          AND c.asLibrary = 1
          AND c.status != 'CANCELLED'
          AND (substr(c.challanDate, 7, 4) || '-' || substr(c.challanDate, 4, 2) || '-' || substr(c.challanDate, 1, 2)) 
              BETWEEN :startDate AND :endDate
        ORDER BY ci.productName ASC, ci.power ASC, c.challanDate ASC
        """
    )
    suspend fun getLensLibraryReportData(
        customerId: Long,
        startDate: String,
        endDate: String
    ): List<LensLibraryRawRow>


    // =========================================================
    // PARTIES WITH LIBRARY CHALLANS
    // =========================================================

    @Query(
        """
        SELECT DISTINCT p.*
        FROM parties p
        INNER JOIN challans c ON c.customerId = p.id
        WHERE c.asLibrary = 1
          AND c.status != 'CANCELLED'
          AND p.isActive = 1
        ORDER BY p.partyName COLLATE NOCASE ASC
        """
    )
    fun getPartiesWithLibraryChallans(): Flow<List<com.vilync.ophthalmicerp.master.party.data.PartyEntity>>
    @Query("DELETE FROM challan_items WHERE challanId = :challanId AND settlementStatus = 'PENDING'")
    suspend fun deletePendingItemsByChallanId(challanId: Long)
}
