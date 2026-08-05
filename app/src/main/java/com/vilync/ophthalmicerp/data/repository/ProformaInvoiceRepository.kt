package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.dao.ProformaInvoiceDao
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import kotlinx.coroutines.flow.Flow

class ProformaInvoiceRepository(
    private val proformaDao: ProformaInvoiceDao,
    private val database: AppDatabase,
    private val numberingRepository: DocumentNumberingRepository
) {

    // =========================================================
    // SAVE COMPLETE PROFORMA INVOICE
    // =========================================================

    suspend fun saveCompleteProforma(
        proforma: ProformaInvoiceEntity,
        items: List<ProformaInvoiceItemEntity>
    ): Long {
        return database.withTransaction {
            val finalNumber = numberingRepository.getNextDocumentNumber(
                DocumentType.PROFORMA,
                proforma.financialYearStart
            )

            val proformaId = proformaDao.insertProformaInvoice(
                proforma.copy(
                    proformaNumber = finalNumber,
                    normalizedProformaNumber = finalNumber.uppercase()
                )
            )

            if (items.isNotEmpty()) {
                proformaDao.insertProformaItems(
                    items.map { it.copy(proformaInvoiceId = proformaId) }
                )
            }
            proformaId
        }
    }

    // =========================================================
    // UPDATE COMPLETE PROFORMA INVOICE
    // =========================================================

    suspend fun updateCompleteProforma(
        proforma: ProformaInvoiceEntity,
        items: List<ProformaInvoiceItemEntity>
    ) {
        database.withTransaction {
            proformaDao.updateProformaInvoice(proforma)
            proformaDao.deleteItemsByProformaId(proforma.id)
            if (items.isNotEmpty()) {
                proformaDao.insertProformaItems(
                    items.map { it.copy(proformaInvoiceId = proforma.id) }
                )
            }
        }
    }

    suspend fun getProformaWithItems(proformaId: Long): Pair<ProformaInvoiceEntity, List<ProformaInvoiceItemEntity>>? {
        val proforma = proformaDao.getProformaById(proformaId) ?: return null
        val items = proformaDao.getItemsByProformaId(proformaId)
        return proforma to items
    }

    suspend fun insertProformaInvoice(
        proforma: ProformaInvoiceEntity
    ): Long {
        return proformaDao.insertProformaInvoice(proforma)
    }

    suspend fun insertProformaItems(
        items: List<ProformaInvoiceItemEntity>
    ) {
        if (items.isEmpty()) return
        proformaDao.insertProformaItems(items)
    }

    suspend fun updateProformaInvoice(
        proforma: ProformaInvoiceEntity
    ) {
        proformaDao.updateProformaInvoice(proforma)
    }

    suspend fun getProformaById(
        proformaId: Long
    ): ProformaInvoiceEntity? {
        return proformaDao.getProformaById(proformaId)
    }

    fun getAllProformaInvoices(): Flow<List<ProformaInvoiceEntity>> {
        return proformaDao.getAllProformaInvoices()
    }

    fun getProformaInvoicesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<ProformaInvoiceEntity>> {
        return proformaDao.getProformaInvoicesByFinancialYear(
            financialYearStart
        )
    }

    fun getProformaInvoicesForCustomer(
        customerId: Long
    ): Flow<List<ProformaInvoiceEntity>> {
        return proformaDao.getProformaInvoicesForCustomer(customerId)
    }

    suspend fun getItemsByProformaId(
        proformaId: Long
    ): List<ProformaInvoiceItemEntity> {
        return proformaDao.getItemsByProformaId(proformaId)
    }

    suspend fun proformaNumberExists(
        normalizedProformaNumber: String,
        financialYearStart: Int
    ): Boolean {
        return proformaDao.proformaNumberExists(
            normalizedProformaNumber =
                normalizedProformaNumber.trim(),
            financialYearStart =
                financialYearStart
        )
    }

    suspend fun markConvertedToSale(
        proformaId: Long,
        saleId: Long,
        convertedAt: Long
    ): Int {
        return proformaDao.markConvertedToSale(
            proformaId = proformaId,
            saleId = saleId,
            convertedAt = convertedAt
        )
    }
}
