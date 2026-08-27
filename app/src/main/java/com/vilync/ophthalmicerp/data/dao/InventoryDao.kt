package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface InventoryDao {

    // =========================================================
    // CREATE INVENTORY UNIT
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertInventoryUnit(
        unit: InventoryUnitEntity
    ): Long


    // =========================================================
    // UPDATE INVENTORY UNIT
    // =========================================================

    @Update
    suspend fun updateInventoryUnit(
        unit: InventoryUnitEntity
    )


    // =========================================================
    // ALL INVENTORY UNITS
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        ORDER BY id DESC
        """
    )
    fun getAllInventoryUnits():
            Flow<List<InventoryUnitEntity>>


    // =========================================================
    // EXACT SERIAL SEARCH
    // =========================================================
    //
    // Existing behaviour is preserved.
    //
    // Example:
    //
    // LMDE232123 -> LMDE232123
    //
    // Comparison is now trim/case insensitive.
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE UPPER(TRIM(serialNumber)) =
              UPPER(TRIM(:serialNumber))
        LIMIT 1
        """
    )
    suspend fun getBySerialNumber(
        serialNumber: String
    ): InventoryUnitEntity?


    // =========================================================
    // SMART SERIAL SUFFIX SEARCH — ERP WIDE
    // =========================================================
    //
    // Example:
    //
    // Search:
    // 232123
    //
    // Possible results:
    //
    // LMDE232123
    // LMMS232123
    //
    // List is intentionally returned.
    //
    // Repository / UI will decide:
    //
    // 0 result  -> Not found
    // 1 result  -> Auto select
    // 2+ result -> Show selection list
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE UPPER(TRIM(serialNumber))
              LIKE '%' || UPPER(TRIM(:serialSuffix))
        ORDER BY serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun findBySerialSuffix(
        serialSuffix: String
    ): List<InventoryUnitEntity>


    // =========================================================
    // SMART SERIAL SUFFIX SEARCH BY STATUS
    // =========================================================
    //
    // Allows the same central search architecture to be reused
    // in different ERP workflows.
    //
    // Examples:
    //
    // Sales:
    // status = IN_STOCK
    //
    // Challan-related inventory workflow:
    // status = ON_CHALLAN
    //
    // Future Returns:
    // appropriate status can be supplied.
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE status = :status
          AND UPPER(TRIM(serialNumber))
              LIKE '%' || UPPER(TRIM(:serialSuffix))
        ORDER BY serialNumber COLLATE NOCASE ASC
        """
    )
    suspend fun findBySerialSuffixAndStatus(
        serialSuffix: String,
        status: String
    ): List<InventoryUnitEntity>


    // =========================================================
    // EXACT SERIAL SEARCH BY STATUS
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE status = :status
          AND UPPER(TRIM(serialNumber)) =
              UPPER(TRIM(:serialNumber))
        LIMIT 1
        """
    )
    suspend fun findExactSerialByStatus(
        serialNumber: String,
        status: String
    ): InventoryUnitEntity?


    // =========================================================
    // INVENTORY UNIT BY ID
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE id = :unitId
        LIMIT 1
        """
    )
    suspend fun getById(
        unitId: Long
    ): InventoryUnitEntity?


    // =========================================================
    // UNITS BY PRODUCT
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE productId = :productId
        ORDER BY power ASC
        """
    )
    fun getUnitsByProduct(
        productId: Long
    ): Flow<List<InventoryUnitEntity>>


    // =========================================================
    // CURRENT AVAILABLE STOCK
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE status = 'IN_STOCK'
        ORDER BY productId ASC,
                 power ASC
        """
    )
    fun getInStockUnits():
            Flow<List<InventoryUnitEntity>>


    @Query(
        """
        SELECT DISTINCT power FROM inventory_units
        WHERE status = 'IN_STOCK' AND power != ''
        ORDER BY power ASC
        """
    )
    suspend fun getDistinctPowers(): List<String>


    @Query(
        """
        SELECT * FROM inventory_units
        WHERE productId = :productId AND power = :power AND status = 'IN_STOCK'
        ORDER BY serialNumber ASC
        """
    )
    suspend fun getAvailableUnitsByProductAndPower(
        productId: Long,
        power: String
    ): List<InventoryUnitEntity>


    // =========================================================
    // UNITS BY STATUS
    // =========================================================

    @Query(
        """
        SELECT * FROM inventory_units
        WHERE status = :status
        ORDER BY id DESC
        """
    )
    fun getUnitsByStatus(
        status: String
    ): Flow<List<InventoryUnitEntity>>


    // =========================================================
    // UPDATE INVENTORY STATUS
    // =========================================================

    @Query(
        """
        UPDATE inventory_units
        SET status = :newStatus
        WHERE id = :unitId
        """
    )
    suspend fun updateStatus(
        unitId: Long,
        newStatus: String
    )


    // =========================================================
    // SERIAL NUMBER EXISTS
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM inventory_units
            WHERE UPPER(TRIM(serialNumber)) =
                  UPPER(TRIM(:serialNumber))
        )
        """
    )
    suspend fun serialNumberExists(
        serialNumber: String
    ): Boolean


    // =========================================================
    // SAFE INVENTORY UNIT DELETE
    // =========================================================

    /**
     * Deletes one physical inventory unit.
     *
     * Repository must first verify:
     *
     * 1. Unit is still IN_STOCK.
     * 2. Unit has no downstream stock movements.
     * 3. Serial was removed from the original Purchase.
     */
    @Query(
        """
        DELETE FROM inventory_units
        WHERE id = :unitId
        """
    )
    suspend fun deleteInventoryUnit(
        unitId: Long
    )
}