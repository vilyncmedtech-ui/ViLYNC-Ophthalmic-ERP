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
import kotlinx.coroutines.flow.Flow


@Dao
interface PurchaseDao {

    // =========================================================
    // INSERT
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(
        purchase: PurchaseEntity
    ): Long


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItem(
        item: PurchaseItemEntity
    ): Long


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItems(
        items: List<PurchaseItemEntity>
    )


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseLens(
        lens: PurchaseLensEntity
    ): Long


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseLenses(
        lenses: List<PurchaseLensEntity>
    )


    // =========================================================
    // UPDATE PURCHASE
    // =========================================================

    /**
     * Updates the existing Purchase parent row.
     *
     * The PurchaseEntity must retain its original database ID.
     */
    @Update
    suspend fun updatePurchase(
        purchase: PurchaseEntity
    )


    // =========================================================
    // PURCHASE LENSES
    // =========================================================

    @Query(
        """
        SELECT * FROM purchase_lenses
        WHERE purchaseItemId = :purchaseItemId
        ORDER BY id ASC
        """
    )
    fun getPurchaseLenses(
        purchaseItemId: Long
    ): Flow<List<PurchaseLensEntity>>


    // =========================================================
    // DUPLICATE INVOICE CHECK - NEW PURCHASE
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchases
            WHERE supplierId = :supplierId
              AND normalizedInvoiceNumber =
                  UPPER(TRIM(:invoiceNumber))
            LIMIT 1
        )
        """
    )
    suspend fun purchaseInvoiceExists(
        supplierId: Long,
        invoiceNumber: String
    ): Boolean


    // =========================================================
    // DUPLICATE INVOICE CHECK - EDIT PURCHASE
    // =========================================================

    /**
     * During Purchase editing, the current Purchase row must
     * not be treated as its own duplicate.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchases
            WHERE supplierId = :supplierId
              AND normalizedInvoiceNumber =
                  UPPER(TRIM(:invoiceNumber))
              AND id != :excludePurchaseId
            LIMIT 1
        )
        """
    )
    suspend fun purchaseInvoiceExistsExcludingPurchase(
        supplierId: Long,
        invoiceNumber: String,
        excludePurchaseId: Long
    ): Boolean


    // =========================================================
    // DUPLICATE SERIAL CHECK - NEW PURCHASE
    // =========================================================

    /**
     * Real-time saved IOL serial duplicate check.
     *
     * Case and surrounding spaces are ignored.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_lenses
            WHERE UPPER(TRIM(serialNumber)) =
                  UPPER(TRIM(:serialNumber))
            LIMIT 1
        )
        """
    )
    suspend fun purchaseLensSerialExists(
        serialNumber: String
    ): Boolean


    // =========================================================
    // DUPLICATE SERIAL CHECK - EDIT PURCHASE
    // =========================================================

    /**
     * Checks whether a serial exists in another Purchase.
     *
     * Lenses belonging to the Purchase currently being edited
     * are excluded.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM purchase_lenses AS lens
            INNER JOIN purchase_items AS item
                ON item.id = lens.purchaseItemId
            WHERE UPPER(TRIM(lens.serialNumber)) =
                  UPPER(TRIM(:serialNumber))
              AND item.purchaseId != :excludePurchaseId
            LIMIT 1
        )
        """
    )
    suspend fun purchaseLensSerialExistsExcludingPurchase(
        serialNumber: String,
        excludePurchaseId: Long
    ): Boolean


    // =========================================================
    // SAVE COMPLETE NEW PURCHASE
    // =========================================================

    @Transaction
    suspend fun saveCompletePurchase(
        purchase: PurchaseEntity,
        itemsWithLenses:
        List<Pair<PurchaseItemEntity, List<PurchaseLensEntity>>>
    ): Long {

        val purchaseId =
            insertPurchase(
                purchase
            )

        itemsWithLenses.forEach { itemWithLenses ->

            val item =
                itemWithLenses.first

            val lenses =
                itemWithLenses.second


            val purchaseItemId =
                insertPurchaseItem(
                    item.copy(
                        purchaseId = purchaseId
                    )
                )


            if (lenses.isNotEmpty()) {

                insertPurchaseLenses(

                    lenses.map { lens ->

                        lens.copy(
                            purchaseItemId =
                                purchaseItemId
                        )
                    }
                )
            }
        }


        return purchaseId
    }


    // =========================================================
    // DELETE EXISTING CHILD LENSES FOR PURCHASE
    // =========================================================

    /**
     * Used only inside complete Purchase update.
     *
     * Deletes physical lens rows belonging to Purchase items.
     * Parent Purchase is NOT deleted.
     */
    @Query(
        """
        DELETE FROM purchase_lenses
        WHERE purchaseItemId IN (
            SELECT id
            FROM purchase_items
            WHERE purchaseId = :purchaseId
        )
        """
    )
    suspend fun deletePurchaseLensesForPurchase(
        purchaseId: Long
    )


    // =========================================================
    // DELETE EXISTING CHILD ITEMS FOR PURCHASE
    // =========================================================

    /**
     * Used only inside complete Purchase update.
     *
     * Parent Purchase row remains intact.
     */
    @Query(
        """
        DELETE FROM purchase_items
        WHERE purchaseId = :purchaseId
        """
    )
    suspend fun deletePurchaseItemsForPurchase(
        purchaseId: Long
    )


    // =========================================================
    // DELETE PARENT PURCHASE
    // =========================================================

    /**
     * Deletes only the parent Purchase row.
     *
     * This method must be called only after the Purchase child
     * lenses/items have been removed and after higher-level
     * dependency protection has approved the delete.
     */
    @Query(
        """
        DELETE FROM purchases
        WHERE id = :purchaseId
        """
    )
    suspend fun deletePurchaseById(
        purchaseId: Long
    )


    // =========================================================
    // DELETE COMPLETE PURCHASE
    // =========================================================

    /**
     * Physically deletes one complete Purchase.
     *
     * IMPORTANT:
     *
     * Higher-level Repository/ViewModel code must perform all
     * downstream dependency checks BEFORE calling this method.
     *
     * Current protection will include Purchase Return usage.
     * Future Sales Invoice / Sales Challan dependency checks
     * must also be completed before this transaction is called.
     *
     * Everything runs inside one Room transaction. If any step
     * fails, Room rolls back the complete delete.
     */
    @Transaction
    suspend fun deleteCompletePurchase(
        purchaseId: Long
    ) {

        require(
            purchaseId > 0L
        ) {
            "Valid Purchase ID is required for delete."
        }

        deletePurchaseLensesForPurchase(
            purchaseId = purchaseId
        )

        deletePurchaseItemsForPurchase(
            purchaseId = purchaseId
        )

        deletePurchaseById(
            purchaseId = purchaseId
        )
    }


    // =========================================================
    // UPDATE COMPLETE PURCHASE
    // =========================================================

    /**
     * Safely updates an existing complete Purchase.
     *
     * IMPORTANT:
     *
     * 1. Parent Purchase keeps the SAME database ID.
     *
     * 2. Existing child lenses/items are replaced with the
     *    corrected version.
     *
     * 3. Everything runs inside one Room transaction.
     *
     * 4. If any operation fails, Room rolls back the complete
     *    transaction.
     */
    @Transaction
    suspend fun updateCompletePurchase(
        purchase: PurchaseEntity,
        itemsWithLenses:
        List<Pair<PurchaseItemEntity, List<PurchaseLensEntity>>>
    ) {

        require(
            purchase.id > 0L
        ) {
            "Existing Purchase ID is required for update."
        }


        // -----------------------------------------------------
        // UPDATE PARENT PURCHASE
        // -----------------------------------------------------

        updatePurchase(
            purchase
        )


        // -----------------------------------------------------
        // REMOVE OLD PHYSICAL LENS CHILDREN
        // -----------------------------------------------------

        deletePurchaseLensesForPurchase(
            purchaseId = purchase.id
        )


        // -----------------------------------------------------
        // REMOVE OLD PURCHASE ITEMS
        // -----------------------------------------------------

        deletePurchaseItemsForPurchase(
            purchaseId = purchase.id
        )


        // -----------------------------------------------------
        // INSERT CORRECTED ITEMS + PHYSICAL LENSES
        // -----------------------------------------------------

        itemsWithLenses.forEach { itemWithLenses ->

            val item =
                itemWithLenses.first

            val lenses =
                itemWithLenses.second


            val purchaseItemId =
                insertPurchaseItem(

                    item.copy(
                        id = 0L,
                        purchaseId = purchase.id
                    )
                )


            if (lenses.isNotEmpty()) {

                insertPurchaseLenses(

                    lenses.map { lens ->

                        lens.copy(
                            id = 0L,
                            purchaseItemId =
                                purchaseItemId
                        )
                    }
                )
            }
        }
    }


    // =========================================================
    // PURCHASE REGISTER
    // =========================================================

    @Query(
        """
        SELECT * FROM purchases
        ORDER BY id DESC
        """
    )
    fun getAllPurchases():
            Flow<List<PurchaseEntity>>


    // =========================================================
    // PURCHASE REGISTER - FINANCIAL YEAR
    // =========================================================

    /**
     * Returns only Purchases belonging to the requested
     * Indian Financial Year start year.
     *
     * Example:
     *
     * financialYearStart = 2025
     * means FY 2025-26.
     *
     * Filtering is performed directly by SQLite / Room so the
     * complete Purchase table does not need to be loaded first.
     */
    @Query(
        """
        SELECT * FROM purchases
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getPurchasesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<PurchaseEntity>>


    // =========================================================
    // PURCHASE BY ID
    // =========================================================

    @Query(
        """
        SELECT * FROM purchases
        WHERE id = :purchaseId
        LIMIT 1
        """
    )
    suspend fun getPurchaseById(
        purchaseId: Long
    ): PurchaseEntity?


    // =========================================================
    // PURCHASE ITEMS
    // =========================================================

    @Query(
        """
        SELECT * FROM purchase_items
        WHERE purchaseId = :purchaseId
        ORDER BY id ASC
        """
    )
    fun getPurchaseItems(
        purchaseId: Long
    ): Flow<List<PurchaseItemEntity>>


    // =========================================================
    // SEARCH BY INVOICE NUMBER
    // =========================================================

    @Query(
        """
        SELECT * FROM purchases
        WHERE invoiceNumber LIKE '%' || :query || '%'
        ORDER BY id DESC
        """
    )
    fun searchByInvoiceNumber(
        query: String
    ): Flow<List<PurchaseEntity>>


    @Query(
        """
        SELECT * FROM purchases
        WHERE (invoiceNumber LIKE '%' || :query || '%' OR supplierName LIKE '%' || :query || '%')
        ORDER BY id DESC LIMIT 20
        """
    )
    suspend fun searchPurchasesList(
        query: String
    ): List<PurchaseEntity>


    // =========================================================
    // PURCHASES BY SUPPLIER
    // =========================================================

    @Query(
        """
        SELECT * FROM purchases
        WHERE supplierName = :supplierName
        ORDER BY id DESC
        """
    )
    fun getPurchasesBySupplier(
        supplierName: String
    ): Flow<List<PurchaseEntity>>

    // =========================================================
    // FINANCIAL AGGREGATION
    // =========================================================

    @Query("SELECT SUM(grandTotal) FROM purchases WHERE supplierId = :supplierId")
    suspend fun getTotalPurchaseAmountForSupplier(supplierId: Long): Double?

    // =========================================================
    // PERIOD REPORTING QUERIES
    // =========================================================

    @Query("""
        SELECT SUM(grandTotal) FROM purchases 
        WHERE supplierId = :supplierId
          AND (substr(invoiceDate, 7, 4) || '-' || substr(invoiceDate, 4, 2) || '-' || substr(invoiceDate, 1, 2)) < :startDate
    """)
    suspend fun getOpeningPurchasesTotal(supplierId: Long, startDate: String): Double?

    @Query("""
        SELECT * FROM purchases 
        WHERE supplierId = :supplierId
          AND (substr(invoiceDate, 7, 4) || '-' || substr(invoiceDate, 4, 2) || '-' || substr(invoiceDate, 1, 2)) BETWEEN :startDate AND :endDate
    """)
    suspend fun getPurchasesForPeriod(supplierId: Long, startDate: String, endDate: String): List<PurchaseEntity>
}
