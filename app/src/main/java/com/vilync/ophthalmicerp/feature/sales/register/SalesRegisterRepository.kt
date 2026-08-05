package com.vilync.ophthalmicerp.feature.sales.register

import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SalesRegisterRepository(
    private val database: AppDatabase,
    private val salesRepository: SalesRepository? = null
) {
    fun getRegisterRows(type: SalesRegisterType): Flow<List<SalesRegisterRow>> {
        return when (type) {
            SalesRegisterType.INVOICE -> {
                database.salesDao().getAllSales().map { sales ->
                    sales.map { sale ->
                        SalesRegisterRow(
                            id = sale.id,
                            documentNumber = sale.invoiceNumber,
                            documentDate = sale.invoiceDate,
                            customerName = sale.customerName,
                            customerId = sale.customerId,
                            status = sale.status,
                            amount = sale.totalAmount,
                            secondaryInfo = sale.remarks,
                            financialYearStart = sale.financialYearStart
                        )
                    }
                }
            }
            SalesRegisterType.CHALLAN -> {
                database.challanDao().getAllChallans().map { challans ->
                    challans.map { challan ->
                        SalesRegisterRow(
                            id = challan.id,
                            documentNumber = challan.challanNumber,
                            documentDate = challan.challanDate,
                            customerName = challan.customerName,
                            customerId = challan.customerId,
                            status = challan.status,
                            amount = 0.0,
                            secondaryInfo = challan.remarks,
                            financialYearStart = challan.financialYearStart
                        )
                    }
                }
            }
            SalesRegisterType.CREDIT_NOTE -> {
                database.salesCreditNoteDao().getAllCreditNotes().map { notes ->
                    notes.map { note ->
                        SalesRegisterRow(
                            id = note.id,
                            documentNumber = note.creditNoteNumber,
                            documentDate = note.creditNoteDate,
                            customerName = note.customerName,
                            customerId = note.customerId,
                            status = note.status,
                            amount = note.totalAmount,
                            secondaryInfo = note.reason,
                            financialYearStart = note.financialYearStart
                        )
                    }
                }
            }
            SalesRegisterType.PROFORMA -> {
                database.proformaInvoiceDao().getAllProformaInvoices().map { proformas ->
                    proformas.map { proforma ->
                        SalesRegisterRow(
                            id = proforma.id,
                            documentNumber = proforma.proformaNumber,
                            documentDate = proforma.proformaDate,
                            customerName = proforma.customerName,
                            customerId = proforma.customerId,
                            status = proforma.status,
                            amount = proforma.totalAmount,
                            secondaryInfo = proforma.remarks,
                            financialYearStart = proforma.financialYearStart
                        )
                    }
                }
            }
            SalesRegisterType.SAMPLE_ISSUE -> {
                database.sampleIssueDao().getAllSampleIssues().map { samples ->
                    samples.map { sample ->
                        SalesRegisterRow(
                            id = sample.id,
                            documentNumber = sample.sampleIssueNumber,
                            documentDate = sample.sampleIssueDate,
                            customerName = sample.customerName,
                            customerId = sample.customerId,
                            status = sample.status,
                            amount = 0.0,
                            secondaryInfo = sample.remarks,
                            financialYearStart = sample.financialYearStart
                        )
                    }
                }
            }
        }
    }

    suspend fun cancelInvoice(saleId: Long, reason: String) {
        val repo = salesRepository ?: throw IllegalStateException("SalesRepository not provided.")
        repo.cancelSale(saleId, reason)
    }

    suspend fun deleteInvoice(saleId: Long) {
        val repo = salesRepository ?: throw IllegalStateException("SalesRepository not provided.")
        repo.softDeleteSale(saleId, "User requested delete from register")
    }
}
