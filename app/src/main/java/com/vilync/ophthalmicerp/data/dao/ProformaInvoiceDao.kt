package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProformaInvoiceDao {

    // =========================================================
    // CREATE
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProformaInvoice(
        proforma: ProformaInvoiceEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProformaItems(
        items: List<ProformaInvoiceItemEntity>
    )

    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun updateProformaInvoice(
        proforma: ProformaInvoiceEntity
    )

    // =========================================================
    // DOCUMENT / REGISTER
    // =========================================================

    @Query(
        """
        SELECT * FROM proforma_invoices
        WHERE id = :proformaId
        LIMIT 1
        """
    )
    suspend fun getProformaById(
        proformaId: Long
    ): ProformaInvoiceEntity?

    @Query(
        """
        SELECT * FROM proforma_invoices
        ORDER BY id DESC
        """
    )
    fun getAllProformaInvoices(): Flow<List<ProformaInvoiceEntity>>

    @Query(
        """
        SELECT * FROM proforma_invoices
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getProformaInvoicesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<ProformaInvoiceEntity>>

    @Query(
        """
        SELECT * FROM proforma_invoices
        WHERE customerId = :customerId
        ORDER BY id DESC
        """
    )
    fun getProformaInvoicesForCustomer(
        customerId: Long
    ): Flow<List<ProformaInvoiceEntity>>

    @Query(
        """
        SELECT * FROM proforma_invoice_items
        WHERE proformaInvoiceId = :proformaId
        ORDER BY id ASC
        """
    )
    suspend fun getItemsByProformaId(
        proformaId: Long
    ): List<ProformaInvoiceItemEntity>

    // =========================================================
    // DUPLICATE NUMBER PROTECTION
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM proforma_invoices
            WHERE normalizedProformaNumber = :normalizedProformaNumber
              AND financialYearStart = :financialYearStart
        )
        """
    )
    suspend fun proformaNumberExists(
        normalizedProformaNumber: String,
        financialYearStart: Int
    ): Boolean

    // =========================================================
    // CONVERSION TO FINAL SALES INVOICE
    // =========================================================

    @Query(
        """
        UPDATE proforma_invoices
        SET status = 'CONVERTED',
            convertedSaleId = :saleId,
            convertedAt = :convertedAt,
            updatedAt = :convertedAt
        WHERE id = :proformaId
          AND status != 'CANCELLED'
          AND convertedSaleId IS NULL
        """
    )
    suspend fun markConvertedToSale(
        proformaId: Long,
        saleId: Long,
        convertedAt: Long
    ): Int
}
