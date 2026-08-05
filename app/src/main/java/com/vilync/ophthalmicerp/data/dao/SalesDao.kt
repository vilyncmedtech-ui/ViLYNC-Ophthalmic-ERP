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
        WHERE status != 'DELETED'
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
          AND status != 'DELETED'
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
        WHERE (invoiceNumber LIKE '%' || :query || '%'
           OR customerName LIKE '%' || :query || '%')
          AND status != 'DELETED'
        ORDER BY id DESC
        """
    )
    fun searchSales(
        query: String
    ): Flow<List<SaleEntity>>

    @Query(
        """
        SELECT *
        FROM sales
        WHERE (invoiceNumber LIKE '%' || :query || '%'
           OR customerName LIKE '%' || :query || '%')
          AND status != 'DELETED'
        ORDER BY id DESC LIMIT 20
        """
    )
    suspend fun searchSalesList(
        query: String
    ): List<SaleEntity>

    // =========================================================
    // SALES BY CUSTOMER / HOSPITAL
    // =========================================================

    @Query(
        """
        SELECT *
        FROM sales
        WHERE customerId = :customerId
          AND status != 'DELETED'
        ORDER BY id DESC
        """
    )
    fun getSalesByCustomer(
        customerId: Long
    ): Flow<List<SaleEntity>>

    // =========================================================
    // FINANCIAL AGGREGATION
    // =========================================================

    @Query("SELECT SUM(totalAmount) FROM sales WHERE customerId = :customerId AND status = 'POSTED'")
    suspend fun getTotalSaleAmountForCustomer(customerId: Long): Double?

    // =========================================================
    // PERIOD REPORTING QUERIES
    // =========================================================

    @Query("""
        SELECT SUM(totalAmount) FROM sales 
        WHERE customerId = :customerId AND status = 'POSTED'
          AND (substr(invoiceDate, 7, 4) || '-' || substr(invoiceDate, 4, 2) || '-' || substr(invoiceDate, 1, 2)) < :startDate
    """)
    suspend fun getOpeningSalesTotal(customerId: Long, startDate: String): Double?

    @Query("""
        SELECT * FROM sales 
        WHERE customerId = :customerId AND status = 'POSTED'
          AND (substr(invoiceDate, 7, 4) || '-' || substr(invoiceDate, 4, 2) || '-' || substr(invoiceDate, 1, 2)) BETWEEN :startDate AND :endDate
    """)
    suspend fun getSalesForPeriod(customerId: Long, startDate: String, endDate: String): List<SaleEntity>

    // =========================================================
    // REPORTING QUERIES
    // =========================================================

    @Query(
        """
        WITH all_transactions AS (
            -- Sales Invoices
            SELECT 
                si.productName, 
                si.quantity as qty, 
                si.totalAmount as amount,
                'Sales Invoice' as docType,
                s.invoiceDate as docDate,
                s.customerId as partyId,
                si.productId as prodId,
                s.status as docStatus
            FROM sale_items si
            INNER JOIN sales s ON si.saleId = s.id
            
            UNION ALL
            
            -- Sales Challans
            SELECT 
                ci.productName, 
                1 as qty, 
                ci.rate * (1 + ci.gstPercent / 100.0) as amount,
                'Sales Challan' as docType,
                c.challanDate as docDate,
                c.customerId as partyId,
                ci.productId as prodId,
                c.status as docStatus
            FROM challan_items ci
            INNER JOIN challans c ON ci.challanId = c.id
            
            UNION ALL
            
            -- Credit Notes
            SELECT 
                cni.productName, 
                -cni.quantity as qty, 
                -cni.totalAmount as amount,
                'Credit Note' as docType,
                cn.creditNoteDate as docDate,
                cn.customerId as partyId,
                cni.productId as prodId,
                cn.status as docStatus
            FROM sales_credit_note_items cni
            INNER JOIN sales_credit_notes cn ON cni.creditNoteId = cn.id
            
            UNION ALL
            
            -- Debit Notes (Purchase Returns)
            SELECT 
                pri.productName, 
                -pri.quantity as qty, 
                -pri.totalAmount as amount,
                'Debit Note' as docType,
                pr.creditNoteDate as docDate,
                pr.supplierId as partyId,
                pri.productId as prodId,
                pr.status as docStatus
            FROM purchase_return_items pri
            INNER JOIN purchase_returns pr ON pri.purchaseReturnId = pr.id
            
            UNION ALL
            
            -- Sample Distribution
            SELECT 
                sii.productName, 
                1 as qty, 
                sii.referenceRate as amount,
                'Sample Distribution' as docType,
                si_h.sampleIssueDate as docDate,
                si_h.customerId as partyId,
                sii.productId as prodId,
                si_h.status as docStatus
            FROM sample_issue_items sii
            INNER JOIN sample_issues si_h ON sii.sampleIssueId = si_h.id
            
            UNION ALL
            
            -- Proforma Invoice
            SELECT 
                pi.productName, 
                pi.quantity as qty, 
                pi.totalAmount as amount,
                'Proforma Invoice' as docType,
                p.proformaDate as docDate,
                p.customerId as partyId,
                pi.productId as prodId,
                p.status as docStatus
            FROM proforma_invoice_items pi
            INNER JOIN proforma_invoices p ON pi.proformaInvoiceId = p.id
        )
        SELECT productName, SUM(qty) as qty, SUM(amount) as amount
        FROM all_transactions
        WHERE (substr(docDate, 7, 4) || '-' || substr(docDate, 4, 2) || '-' || substr(docDate, 1, 2)) 
              BETWEEN :startDate AND :endDate
          AND docStatus != 'CANCELLED'
          AND (IFNULL(:customerId, 0) == 0 OR partyId = :customerId)
          AND (IFNULL(:productId, 0) == 0 OR prodId = :productId)
          AND (
                :transactionType = 'All' 
                OR docType = :transactionType
                OR (:transactionType = 'Sales and Pending Challan Both' AND docType IN ('Sales Invoice', 'Sales Challan'))
              )
        GROUP BY productName
        """
    )
    suspend fun getOptimizedProductWiseSummary(
        startDate: String,
        endDate: String,
        customerId: Long?,
        productId: Long?,
        transactionType: String?
    ): List<com.vilync.ophthalmicerp.feature.sales.reports.data.ProductWiseSummary>

    @Query(
        """
        SELECT 
            sales.invoiceDate as date, 
            sales.invoiceNumber as invoiceNo, 
            sales.customerName as customer,
            sales.billToGstin as customerGstin,
            sale_items.productName as product,
            sale_items.power as power,
            CASE 
                WHEN sale_lenses.serialNumber IS NOT NULL 
                THEN COALESCE(products.serialPrefix, '') || sale_lenses.serialNumber 
                ELSE '' 
            END as serial, 
            1 as qty,
            sale_items.gstPercent as gst_percent,
            CASE WHEN sales.gstSupplyType = 'INTRA_STATE' THEN (sale_items.gstAmount / sale_items.quantity) / 2.0 ELSE 0.0 END as cgst,
            CASE WHEN sales.gstSupplyType = 'INTRA_STATE' THEN (sale_items.gstAmount / sale_items.quantity) / 2.0 ELSE 0.0 END as sgst,
            CASE WHEN sales.gstSupplyType = 'INTER_STATE' THEN (sale_items.gstAmount / sale_items.quantity) ELSE 0.0 END as igst,
            (sale_items.totalAmount / sale_items.quantity) as amount, 
            sales.status,
            COALESCE(
                NULLIF(sale_lenses.purchasePriceSnapshot, 0.0),
                NULLIF(trace_pi.purchaseRate, 0.0),
                products.purchasePrice
            ) as purchasePrice,
            CASE
                WHEN sale_lenses.purchasePriceSnapshot > 0 THEN 'SNAPSHOT'
                WHEN trace_pi.purchaseRate > 0 THEN 'TRACE'
                ELSE 'MASTER_FALLBACK'
            END as costResolutionSource
        FROM sale_items
        INNER JOIN sales ON sale_items.saleId = sales.id
        LEFT JOIN sale_lenses ON sale_lenses.saleItemId = sale_items.id
        LEFT JOIN products ON sale_items.productId = products.id
        LEFT JOIN inventory_units ON sale_lenses.inventoryUnitId = inventory_units.id
        LEFT JOIN purchase_items AS trace_pi ON (
            (inventory_units.purchaseItemId IS NOT NULL AND trace_pi.id = inventory_units.purchaseItemId)
            OR
            (inventory_units.purchaseItemId IS NULL AND 
             inventory_units.purchaseInvoiceNumber IS NOT NULL AND 
             trace_pi.purchaseId = (SELECT id FROM purchases WHERE invoiceNumber = inventory_units.purchaseInvoiceNumber LIMIT 1) AND
             trace_pi.productId = sale_items.productId AND
             trace_pi.power = sale_items.power)
        )
        WHERE (substr(sales.invoiceDate, 7, 4) || '-' || substr(sales.invoiceDate, 4, 2) || '-' || substr(sales.invoiceDate, 1, 2)) 
              BETWEEN :startDate AND :endDate
        ORDER BY (substr(sales.invoiceDate, 7, 4) || '-' || substr(sales.invoiceDate, 4, 2) || '-' || substr(sales.invoiceDate, 1, 2)) ASC
        """
    )
    suspend fun getDateWiseSales(
        startDate: String,
        endDate: String
    ): List<com.vilync.ophthalmicerp.feature.sales.reports.data.SalesTransactionDetail>
}
