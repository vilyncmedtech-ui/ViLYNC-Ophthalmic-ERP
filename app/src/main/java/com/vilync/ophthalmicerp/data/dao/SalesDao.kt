package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SaleItemEntity
import com.vilync.ophthalmicerp.data.entity.SaleLensEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    // =========================================================
    // INSERT
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(
        sale: SaleEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItem(
        item: SaleItemEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleLenses(
        lenses: List<SaleLensEntity>
    )

    // =========================================================
    // UPDATE HEADER
    // =========================================================

    @Update
    suspend fun updateSale(
        sale: SaleEntity
    )

    // =========================================================
    // DUPLICATE INVOICE CHECK - NEW SALE
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sales
            WHERE customerId = :customerId
              AND normalizedInvoiceNumber =
                  UPPER(TRIM(:invoiceNumber))
              AND status != 'CANCELLED'
            LIMIT 1
        )
        """
    )
    suspend fun saleInvoiceExists(
        customerId: Long,
        invoiceNumber: String
    ): Boolean

    // =========================================================
    // DUPLICATE INVOICE CHECK - EDIT SALE
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sales
            WHERE customerId = :customerId
              AND normalizedInvoiceNumber =
                  UPPER(TRIM(:invoiceNumber))
              AND status != 'CANCELLED'
              AND id != :excludeSaleId
            LIMIT 1
        )
        """
    )
    suspend fun saleInvoiceExistsExcludingSale(
        customerId: Long,
        invoiceNumber: String,
        excludeSaleId: Long
    ): Boolean

    // =========================================================
    // SAVE COMPLETE SALE DOCUMENT
    // =========================================================

    /**
     * Persists only the Sales document hierarchy.
     *
     * Inventory status changes and Stock Movement entries are
     * intentionally handled by SalesRepository so that the final
     * Sale + Inventory operation can be coordinated atomically.
     */
    @Transaction
    suspend fun saveCompleteSaleDocument(
        sale: SaleEntity,
        itemsWithLenses:
        List<Pair<SaleItemEntity, List<SaleLensEntity>>>
    ): Long {

        val saleId = insertSale(
            sale = sale
        )

        itemsWithLenses.forEach { itemWithLenses ->

            val item = itemWithLenses.first
            val lenses = itemWithLenses.second

            val saleItemId = insertSaleItem(
                item.copy(
                    id = 0L,
                    saleId = saleId
                )
            )

            if (lenses.isNotEmpty()) {
                insertSaleLenses(
                    lenses.map { lens ->
                        lens.copy(
                            id = 0L,
                            saleItemId = saleItemId
                        )
                    }
                )
            }
        }

        return saleId
    }


    // =========================================================
    // EDIT SALE DOCUMENT HIERARCHY
    // =========================================================

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteSaleItemsForSale(
        saleId: Long
    )

    @Transaction
    suspend fun replaceCompleteSaleDocument(
        sale: SaleEntity,
        itemsWithLenses:
        List<Pair<SaleItemEntity, List<SaleLensEntity>>>
    ) {
        updateSale(sale)
        deleteSaleItemsForSale(sale.id)

        itemsWithLenses.forEach { itemWithLenses ->
            val item = itemWithLenses.first
            val lenses = itemWithLenses.second

            val saleItemId = insertSaleItem(
                item.copy(
                    id = 0L,
                    saleId = sale.id
                )
            )

            if (lenses.isNotEmpty()) {
                insertSaleLenses(
                    lenses.map { lens ->
                        lens.copy(
                            id = 0L,
                            saleItemId = saleItemId
                        )
                    }
                )
            }
        }
    }

    // =========================================================
    // SALES REGISTER
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        ORDER BY id DESC
        """
    )
    fun getAllSales(): Flow<List<SaleEntity>>

    // =========================================================
    // SALES REGISTER - FINANCIAL YEAR
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getSalesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SaleEntity>>

    // =========================================================
    // SALE BY ID
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        WHERE id = :saleId
        LIMIT 1
        """
    )
    suspend fun getSaleById(
        saleId: Long
    ): SaleEntity?

    // =========================================================
    // SALE ITEMS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sale_items
        WHERE saleId = :saleId
        ORDER BY id ASC
        """
    )
    fun getSaleItems(
        saleId: Long
    ): Flow<List<SaleItemEntity>>

    // =========================================================
    // SALE LENSES
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sale_lenses
        WHERE saleItemId = :saleItemId
        ORDER BY id ASC
        """
    )
    fun getSaleLenses(
        saleItemId: Long
    ): Flow<List<SaleLensEntity>>


    // =========================================================
    // SALE ITEMS / LENSES - TRANSACTION HELPERS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sale_items
        WHERE saleId = :saleId
        ORDER BY id ASC
        """
    )
    suspend fun getSaleItemsList(
        saleId: Long
    ): List<SaleItemEntity>

    @Query(
        """
        SELECT *
        FROM sale_lenses
        WHERE saleItemId = :saleItemId
        ORDER BY id ASC
        """
    )
    suspend fun getSaleLensesList(
        saleItemId: Long
    ): List<SaleLensEntity>

    // =========================================================
    // CANCEL SALE HEADER
    // =========================================================

    @Query(
        """
        UPDATE sales
        SET status = 'CANCELLED',
            cancelledAt = :cancelledAt,
            cancellationReason = :cancellationReason
        WHERE id = :saleId
        """
    )
    suspend fun markSaleCancelled(
        saleId: Long,
        cancelledAt: Long,
        cancellationReason: String
    )


    // =========================================================
    // SEARCH SALES REGISTER
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        WHERE invoiceNumber LIKE '%' || :query || '%'
           OR customerName LIKE '%' || :query || '%'
        ORDER BY id DESC
        """
    )
    fun searchSales(
        query: String
    ): Flow<List<SaleEntity>>

    // =========================================================
    // SALES BY CUSTOMER / HOSPITAL
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        WHERE customerId = :customerId
        ORDER BY id DESC
        """
    )
    fun getSalesByCustomer(
        customerId: Long
    ): Flow<List<SaleEntity>>
}