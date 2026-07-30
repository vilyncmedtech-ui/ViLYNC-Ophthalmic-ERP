package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnSerialSearchRow
import kotlinx.coroutines.flow.Flow


@Dao
interface PurchaseReturnDao {


    // =========================================================
    // INSERT PURCHASE RETURN HEADER
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPurchaseReturn(
        purchaseReturn: PurchaseReturnEntity
    ): Long

    @Update
    suspend fun updatePurchaseReturn(purchaseReturn: PurchaseReturnEntity)

    @Query("""
        UPDATE purchase_returns
        SET status = 'CANCELLED',
            cancelledAt = :cancelledAt,
            cancellationReason = :reason,
            updatedAt = :cancelledAt
        WHERE id = :purchaseReturnId AND status = 'POSTED'
    """)
    suspend fun cancelPostedPurchaseReturn(
        purchaseReturnId: Long,
        reason: String,
        cancelledAt: Long
    ): Int


    // =========================================================
    // INSERT PURCHASE RETURN ITEM
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPurchaseReturnItem(
        item: PurchaseReturnItemEntity
    ): Long


    // =========================================================
    // INSERT PURCHASE RETURN LENS
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPurchaseReturnLens(
        lens: PurchaseReturnLensEntity
    ): Long


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPurchaseReturnLenses(

        lenses: List<PurchaseReturnLensEntity>
    )


    // =========================================================
    // DELETE PURCHASE RETURN CHILD ROWS FOR EDIT
    // =========================================================

    @Query(
        """
        DELETE FROM purchase_return_lenses
        WHERE purchaseReturnItemId IN (
            SELECT id
            FROM purchase_return_items
            WHERE purchaseReturnId = :purchaseReturnId
        )
        """
    )
    suspend fun deletePurchaseReturnLensesForReturn(
        purchaseReturnId: Long
    )

    @Query(
        """
        DELETE FROM purchase_return_items
        WHERE purchaseReturnId = :purchaseReturnId
        """
    )
    suspend fun deletePurchaseReturnItemsForReturn(
        purchaseReturnId: Long
    )


    // =========================================================
    // SAVE COMPLETE PURCHASE RETURN
    // =========================================================

    /**
     * Saves the complete Purchase Return / Supplier Credit Note
     * inside one Room transaction.
     *
     * Structure:
     *
     * PurchaseReturn
     *      ↓
     * PurchaseReturnItem
     *      ↓
     * PurchaseReturnLens
     *
     * If any insert fails, Room rolls back the complete
     * transaction.
     */
    @Transaction
    suspend fun saveCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses:
        List<
                Pair<
                        PurchaseReturnItemEntity,
                        List<PurchaseReturnLensEntity>
                        >
                >
    ): Long {

        val purchaseReturnId =
            insertPurchaseReturn(
                purchaseReturn
            )


        itemsWithLenses.forEach { itemWithLenses ->

            val item =
                itemWithLenses.first

            val lenses =
                itemWithLenses.second


            val purchaseReturnItemId =
                insertPurchaseReturnItem(

                    item.copy(
                        id = 0L,
                        purchaseReturnId =
                            purchaseReturnId
                    )
                )



            if (lenses.isNotEmpty()) {

                insertPurchaseReturnLenses(

                    lenses.map { lens ->

                        lens.copy(
                            id = 0L,
                            purchaseReturnItemId =
                                purchaseReturnItemId
                        )
                    }
                )
            }
        }


        return purchaseReturnId
    }


    // =========================================================
    // UPDATE COMPLETE PURCHASE RETURN
    // =========================================================

    /**
     * Corrects an existing Purchase Return inside one Room transaction.
     *
     * The parent header keeps the same ID. Existing child lenses are
     * deleted first, then existing child items, and finally the corrected
     * items/lenses are inserted again.
     *
     * Deleting lenses before items is intentional because
     * originalPurchaseLensId is UNIQUE in purchase_return_lenses.
     *
     * If any operation fails, Room rolls back the complete correction.
     */
    @Transaction
    suspend fun updateCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses:
        List<
                Pair<
                        PurchaseReturnItemEntity,
                        List<PurchaseReturnLensEntity>
                        >
                >
    ) {
        require(purchaseReturn.id > 0L) {
            "Purchase Return ID is required for update."
        }

        deletePurchaseReturnLensesForReturn(
            purchaseReturnId = purchaseReturn.id
        )

        deletePurchaseReturnItemsForReturn(
            purchaseReturnId = purchaseReturn.id
        )

        updatePurchaseReturn(
            purchaseReturn = purchaseReturn
        )

        itemsWithLenses.forEach { itemWithLenses ->

            val item = itemWithLenses.first
            val lenses = itemWithLenses.second

            val purchaseReturnItemId =
                insertPurchaseReturnItem(
                    item.copy(
                        id = 0L,
                        purchaseReturnId = purchaseReturn.id
                    )
                )

            if (lenses.isNotEmpty()) {
                insertPurchaseReturnLenses(
                    lenses.map { lens ->
                        lens.copy(
                            id = 0L,
                            purchaseReturnItemId = purchaseReturnItemId
                        )
                    }
                )
            }
        }
    }


    // =========================================================
    // PURCHASE RETURN REGISTER
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_returns
        ORDER BY id DESC
        """
    )
    fun getAllPurchaseReturns():
            Flow<List<PurchaseReturnEntity>>


    // =========================================================
    // PURCHASE RETURN REGISTER - FINANCIAL YEAR
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_returns
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getPurchaseReturnsByFinancialYear(
        financialYearStart: Int
    ): Flow<List<PurchaseReturnEntity>>


    // =========================================================
    // PURCHASE RETURN BY ID
    // =========================================================

    @Query(
        """

        SELECT *
        FROM purchase_returns
        WHERE id = :purchaseReturnId
        LIMIT 1
        """
    )
    suspend fun getPurchaseReturnById(
        purchaseReturnId: Long
    ): PurchaseReturnEntity?


    // =========================================================
    // PURCHASE RETURN ITEMS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_return_items
        WHERE purchaseReturnId = :purchaseReturnId
        ORDER BY id ASC
        """
    )
    fun getPurchaseReturnItems(
        purchaseReturnId: Long
    ): Flow<List<PurchaseReturnItemEntity>>


    // =========================================================
    // PURCHASE RETURN LENSES
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_return_lenses
        WHERE purchaseReturnItemId = :purchaseReturnItemId
        ORDER BY id ASC
        """
    )
    fun getPurchaseReturnLenses(
        purchaseReturnItemId: Long
    ): Flow<List<PurchaseReturnLensEntity>>


    // =========================================================
    // ORIGINAL PURCHASE
    // =========================================================

    /**
     * Loads the exact Purchase against which the
     * Purchase Return is being created.
     */
    @Query(
        """
        SELECT *
        FROM purchases
        WHERE id = :purchaseId
        LIMIT 1
        """

    )
    suspend fun getOriginalPurchase(
        purchaseId: Long
    ): PurchaseEntity?


    // =========================================================
    // ORIGINAL PURCHASE ITEMS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_items
        WHERE purchaseId = :purchaseId
        ORDER BY id ASC
        """
    )
    suspend fun getOriginalPurchaseItems(
        purchaseId: Long
    ): List<PurchaseItemEntity>


    // =========================================================
    // ORIGINAL PURCHASE ITEM
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_items
        WHERE id = :purchaseItemId
        LIMIT 1
        """
    )
    suspend fun getOriginalPurchaseItemById(
        purchaseItemId: Long
    ): PurchaseItemEntity?


    // =========================================================
    // ORIGINAL PHYSICAL IOL LENSES
    // =========================================================

    /**
     * Returns all physical IOL lenses originally
     * purchased under a Purchase Item.
     */
    @Query(
        """
        SELECT *
        FROM purchase_lenses
        WHERE purchaseItemId = :purchaseItemId
        ORDER BY id ASC
        """
    )
    suspend fun getOriginalPurchaseLenses(
        purchaseItemId: Long
    ): List<PurchaseLensEntity>



    // =========================================================
    // AVAILABLE PHYSICAL IOL LENSES FOR RETURN
    // =========================================================

    /**
     * Returns only those physical lenses which have NOT
     * already been included in a previous Purchase Return.
     *
     * This is important for the Return UI.
     */
    @Query(
        """
        SELECT pl.*
        FROM purchase_lenses pl
        WHERE pl.purchaseItemId = :purchaseItemId
          AND NOT EXISTS (
              SELECT 1
              FROM purchase_return_lenses prl
              INNER JOIN purchase_return_items pri
                  ON pri.id = prl.purchaseReturnItemId
              INNER JOIN purchase_returns pr
                  ON pr.id = pri.purchaseReturnId
              WHERE prl.originalPurchaseLensId = pl.id
                AND pr.status != 'CANCELLED'
          )
        ORDER BY pl.id ASC
        """
    )
    suspend fun getAvailablePurchaseLensesForReturn(
        purchaseItemId: Long
    ): List<PurchaseLensEntity>


    /**
     * Edit-mode variant.
     *
     * Lenses belonging to the Purchase Return currently being corrected
     * remain selectable, while lenses used by other active returns stay
     * unavailable.
     */
    @Query(
        """
        SELECT pl.*
        FROM purchase_lenses pl
        WHERE pl.purchaseItemId = :purchaseItemId
          AND NOT EXISTS (
              SELECT 1
              FROM purchase_return_lenses prl
              INNER JOIN purchase_return_items pri
                  ON pri.id = prl.purchaseReturnItemId
              INNER JOIN purchase_returns pr
                  ON pr.id = pri.purchaseReturnId
              WHERE prl.originalPurchaseLensId = pl.id
                AND pr.status != 'CANCELLED'
                AND pr.id != :excludePurchaseReturnId
          )
        ORDER BY pl.id ASC
        """
    )
    suspend fun getAvailablePurchaseLensesForReturnExcludingReturn(
        purchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): List<PurchaseLensEntity>


    // =========================================================
    // PHYSICAL IOL ALREADY RETURNED CHECK
    // =========================================================

    /**
     * Application-level check.
     *
     * Database also has a UNIQUE index on
     * originalPurchaseLensId, therefore even if two operations
     * somehow try to return the same physical lens,
     * the database remains the final protection layer.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_return_lenses prl
            INNER JOIN purchase_return_items pri
                ON pri.id = prl.purchaseReturnItemId
            INNER JOIN purchase_returns pr
                ON pr.id = pri.purchaseReturnId
            WHERE prl.originalPurchaseLensId = :originalPurchaseLensId
              AND pr.status != 'CANCELLED'
            LIMIT 1
        )
        """
    )
    suspend fun isPurchaseLensAlreadyReturned(
        originalPurchaseLensId: Long
    ): Boolean


    /**
     * Edit-mode duplicate check.
     *
     * Ignores the Purchase Return currently being corrected.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_return_lenses prl
            INNER JOIN purchase_return_items pri
                ON pri.id = prl.purchaseReturnItemId
            INNER JOIN purchase_returns pr
                ON pr.id = pri.purchaseReturnId
            WHERE prl.originalPurchaseLensId = :originalPurchaseLensId
              AND pr.status != 'CANCELLED'
              AND pr.id != :excludePurchaseReturnId
            LIMIT 1
        )
        """
    )
    suspend fun isPurchaseLensAlreadyReturnedExcludingReturn(
        originalPurchaseLensId: Long,
        excludePurchaseReturnId: Long
    ): Boolean


    // =========================================================
    // RETURNED QUANTITY FOR ORIGINAL PURCHASE ITEM
    // =========================================================


    /**
     * Total quantity already returned against one
     * original Purchase Item.
     *
     * Used for non-serial-controlled products and for
     * overall return quantity validation.
     */
    @Query(
        """
        SELECT COALESCE(
            SUM(quantity),
            0
        )
        FROM purchase_return_items
        WHERE originalPurchaseItemId = :originalPurchaseItemId
          AND purchaseReturnId IN (
              SELECT id FROM purchase_returns
              WHERE status != 'CANCELLED'
          )
        """
    )
    suspend fun getAlreadyReturnedQuantity(
        originalPurchaseItemId: Long
    ): Int


    /**
     * Edit-mode quantity calculation.
     *
     * Excludes the Purchase Return currently being corrected so its own
     * old quantity does not reduce the quantity available to itself.
     */
    @Query(
        """
        SELECT COALESCE(
            SUM(pri.quantity),
            0
        )
        FROM purchase_return_items pri
        INNER JOIN purchase_returns pr
            ON pr.id = pri.purchaseReturnId
        WHERE pri.originalPurchaseItemId = :originalPurchaseItemId
          AND pr.status != 'CANCELLED'
          AND pr.id != :excludePurchaseReturnId
        """
    )
    suspend fun getAlreadyReturnedQuantityExcludingReturn(
        originalPurchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): Int


    // =========================================================
    // CREDIT NOTE DUPLICATE CHECK
    // =========================================================

    /**
     * Checks duplicate Supplier + Credit Note Number.
     *
     * Comparison ignores leading/trailing spaces and case.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_returns
            WHERE supplierId = :supplierId
              AND UPPER(TRIM(creditNoteNumber)) =
                  UPPER(TRIM(:creditNoteNumber))
            LIMIT 1
        )
        """
    )
    suspend fun creditNoteExists(
        supplierId: Long,
        creditNoteNumber: String
    ): Boolean


    /**
     * Edit-mode Debit Note duplicate check.
     *
     * Allows the current return to retain its own Debit Note number while
     * still blocking the same Supplier + Debit Note number on another row.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_returns
            WHERE supplierId = :supplierId
              AND UPPER(TRIM(creditNoteNumber)) =
                  UPPER(TRIM(:creditNoteNumber))
              AND id != :excludePurchaseReturnId
            LIMIT 1
        )
        """
    )
    suspend fun creditNoteExistsExcludingReturn(
        supplierId: Long,
        creditNoteNumber: String,
        excludePurchaseReturnId: Long
    ): Boolean


    // =========================================================
    // PURCHASE RETURNS AGAINST ORIGINAL PURCHASE
    // =========================================================

    /**
     * One original Purchase may have multiple partial returns.
     */
    @Query(
        """
        SELECT *
        FROM purchase_returns

        WHERE originalPurchaseId = :originalPurchaseId
        ORDER BY id DESC
        """
    )
    fun getReturnsForPurchase(
        originalPurchaseId: Long
    ): Flow<List<PurchaseReturnEntity>>


    // =========================================================
    // PURCHASE RETURN BY SUPPLIER
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_returns
        WHERE supplierId = :supplierId
        ORDER BY id DESC
        """
    )
    fun getPurchaseReturnsBySupplier(
        supplierId: Long
    ): Flow<List<PurchaseReturnEntity>>


    // =========================================================
    // SEARCH CREDIT NOTE
    // =========================================================

    @Query(
        """
        SELECT *
        FROM purchase_returns
        WHERE creditNoteNumber
              LIKE '%' || :query || '%'
           OR originalInvoiceNumber
              LIKE '%' || :query || '%'
           OR supplierName
              LIKE '%' || :query || '%'
        ORDER BY id DESC
        """
    )
    fun searchPurchaseReturns(
        query: String
    ): Flow<List<PurchaseReturnEntity>>
    // =========================================================
    // AVAILABLE IOL SERIAL SEARCH FOR PURCHASE RETURN
    // =========================================================

    @Query(
        """
        SELECT
            pl.id AS purchaseLensId,
            pl.serialNumber AS serialNumber,
            pl.expiryDate AS expiryDate,
            pi.id AS purchaseItemId,
            p.id AS purchaseId,
            p.invoiceNumber AS invoiceNumber,
            p.invoiceDate AS invoiceDate,
            p.supplierName AS supplierName,
            pi.productId AS productId,
            pi.power AS power,
            0 AS alreadyReturned
        FROM purchase_lenses pl
        INNER JOIN purchase_items pi
            ON pi.id = pl.purchaseItemId
        INNER JOIN purchases p
            ON p.id = pi.purchaseId
        WHERE UPPER(pl.serialNumber)
              LIKE '%' || UPPER(:query) || '%'
          AND NOT EXISTS (
              SELECT 1
              FROM purchase_return_lenses prl
              INNER JOIN purchase_return_items pri
                  ON pri.id = prl.purchaseReturnItemId
              INNER JOIN purchase_returns pr
                  ON pr.id = pri.purchaseReturnId
              WHERE prl.originalPurchaseLensId = pl.id
                AND pr.status != 'CANCELLED'
          )
        ORDER BY pl.id DESC
        LIMIT 50
        """
    )
    fun searchAvailableIolSerials(
        query: String
    ): Flow<List<PurchaseReturnSerialSearchRow>>

}
