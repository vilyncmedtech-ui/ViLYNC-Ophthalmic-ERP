package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import com.vilync.ophthalmicerp.data.dao.StockMovementRegisterRow


@Dao
interface StockMovementDao {

    // =========================================================
    // INSERT STOCK MOVEMENT
    // =========================================================

    /**
     * Adds one permanent stock movement / history entry.
     *
     * Examples:
     *
     * PURCHASE_RECEIVED
     * SAMPLE
     * DEMO
     * APPROVAL
     * SOLD
     * RETURNED
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(
        movement: StockMovementEntity
    ): Long


    // =========================================================
    // MOVEMENT HISTORY BY INVENTORY UNIT
    // =========================================================

    /**
     * Complete movement history of one physical inventory unit.
     */
    @Query(
        """
        SELECT *
        FROM stock_movements
        WHERE inventoryUnitId = :inventoryUnitId
        ORDER BY id DESC
        """
    )
    fun getMovementHistoryByUnit(
        inventoryUnitId: Long
    ): Flow<List<StockMovementEntity>>


    // =========================================================
    // MOVEMENT HISTORY BY SERIAL NUMBER
    // =========================================================

    /**
     * Complete movement history using Serial Number.
     */
    @Query(
        """
        SELECT *
        FROM stock_movements
        WHERE serialNumber = :serialNumber
        ORDER BY id DESC
        """
    )
    fun getMovementHistoryBySerialNumber(
        serialNumber: String
    ): Flow<List<StockMovementEntity>>


    // =========================================================
    // MOVEMENTS BY TYPE
    // =========================================================

    /**
     * Returns all movements of a particular movement type.
     */
    @Query(
        """
        SELECT *
        FROM stock_movements
        WHERE movementType = :movementType
        ORDER BY id DESC
        """
    )
    fun getMovementsByType(
        movementType: String
    ): Flow<List<StockMovementEntity>>


    // =========================================================
    // ALL STOCK MOVEMENTS
    // =========================================================

    /**
     * Returns complete stock movement history.
     */
    @Query(
        """
        SELECT *
        FROM stock_movements
        ORDER BY id DESC
        """
    )
    fun getAllMovements():
            Flow<List<StockMovementEntity>>


    // =========================================================
    // PURCHASE EDIT SAFETY
    // =========================================================

    /**
     * Counts downstream movements of an Inventory Unit.
     *
     * PURCHASE_RECEIVED is the original stock receipt and is
     * therefore NOT treated as a downstream movement.
     *
     * If this method returns more than 0, the physical serial
     * has already participated in another stock transaction
     * and must not be removed through Purchase Edit.
     *
     * Examples of protected downstream movements:
     *
     * SAMPLE
     * DEMO
     * APPROVAL
     * SOLD
     * RETURNED
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM stock_movements
        WHERE inventoryUnitId = :inventoryUnitId
          AND movementType != 'PURCHASE_RECEIVED'
        """
    )
    suspend fun countDownstreamMovements(
        inventoryUnitId: Long
    ): Int


    // =========================================================
    // DELETE ORIGINAL PURCHASE RECEIPT MOVEMENT
    // =========================================================

    /**
     * Deletes the original PURCHASE_RECEIVED movement belonging
     * to an Inventory Unit.
     *
     * IMPORTANT:
     *
     * This method must only be called during a safe Purchase Edit
     * after confirming:
     *
     * 1. Inventory Unit has no downstream movements.
     * 2. Inventory Unit is still IN_STOCK.
     * 3. Serial is being removed from the original Purchase.
     *
     * The Repository is responsible for these safety checks.
     */
    @Query(
        """
        DELETE FROM stock_movements
        WHERE inventoryUnitId = :inventoryUnitId
          AND movementType = 'PURCHASE_RECEIVED'
        """
    )
    suspend fun deletePurchaseReceivedMovement(
        inventoryUnitId: Long
    )

    // =========================================================
    // MASTER MOVEMENT REGISTER QUERY
    // =========================================================

    @Query(
        """
        SELECT 
            sm.*,
            p.productName as productName,
            p.model as model,
            p.category as category,
            iu.power as power,
            iu.batchNumber as batchNumber,
            (SELECT userDisplayName FROM audit_trail 
             WHERE referenceNumber = sm.referenceNumber 
             LIMIT 1) as userName
        FROM stock_movements sm
        LEFT JOIN inventory_units iu ON sm.inventoryUnitId = iu.id
        LEFT JOIN products p ON iu.productId = p.id
        WHERE (substr(sm.movementDate, 7, 4) || '-' || substr(sm.movementDate, 4, 2) || '-' || substr(sm.movementDate, 1, 2)) 
              BETWEEN :startDate AND :endDate
          AND (:movementType = 'All' OR sm.movementType = :movementType)
          AND (:partyName IS NULL OR sm.partyName LIKE '%' || :partyName || '%')
          AND (:serialNumber IS NULL OR sm.serialNumber LIKE '%' || :serialNumber || '%')
          AND (:productId IS NULL OR iu.productId = :productId)
          AND (:power IS NULL OR iu.power = :power)
        ORDER BY (substr(sm.movementDate, 7, 4) || '-' || substr(sm.movementDate, 4, 2) || '-' || substr(sm.movementDate, 1, 2)) ASC, 
                 sm.id ASC
        """
    )
    suspend fun getMovementRegisterRows(
        startDate: String,
        endDate: String,
        movementType: String,
        partyName: String?,
        serialNumber: String?,
        productId: Long?,
        power: String?
    ): List<StockMovementRegisterRow>
}
