package com.vilync.ophthalmicerp.feature.gst.data

import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.gst.model.GstDocumentRow
import com.vilync.ophthalmicerp.feature.gst.model.GstHsnRow
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import kotlinx.coroutines.flow.first
import kotlin.math.round

class GstReportingRepository(
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository,
    private val salesCreditNoteRepository: SalesCreditNoteRepository
) {
    suspend fun load(financialYearStart: Int): GstReportSnapshot {
        require(financialYearStart > 0) { "Valid Financial Year is required." }

        val sales = salesRepository
            .getSalesByFinancialYear(financialYearStart)
            .first()
            .filter { it.status.trim().equals("POSTED", ignoreCase = true) }

        val purchases = purchaseRepository
            .getPurchasesByFinancialYear(financialYearStart)
            .first()

        val creditNotes = salesCreditNoteRepository
            .getCreditNotesByFinancialYear(financialYearStart)
            .first()
            .filter { it.status.trim().equals("POSTED", ignoreCase = true) }

        val saleRows = sales.map {
            GstDocumentRow(
                id = it.id,
                documentNumber = it.invoiceNumber,
                documentDate = it.invoiceDate,
                partyName = it.customerName,
                gstin = it.billToGstin.trim(),
                taxableAmount = money(it.taxableAmount),
                cgstAmount = money(it.cgstAmount),
                sgstAmount = money(it.sgstAmount),
                igstAmount = money(it.igstAmount),
                totalAmount = money(it.totalAmount),
                status = it.status
            )
        }

        val purchaseRows = purchases.map {
            GstDocumentRow(
                id = it.id,
                documentNumber = it.invoiceNumber,
                documentDate = it.invoiceDate,
                partyName = it.supplierName,
                gstin = it.supplierGstin.trim(),
                taxableAmount = money(it.taxableAmount),
                cgstAmount = money(it.cgstAmount),
                sgstAmount = money(it.sgstAmount),
                igstAmount = money(it.igstAmount),
                totalAmount = money(it.grandTotal)
            )
        }

        val creditRows = creditNotes.map {
            GstDocumentRow(
                id = it.id,
                documentNumber = it.creditNoteNumber,
                documentDate = it.creditNoteDate,
                partyName = it.customerName,
                gstin = it.billToGstin.trim(),
                taxableAmount = money(it.taxableAmount),
                cgstAmount = money(it.cgstAmount),
                sgstAmount = money(it.sgstAmount),
                igstAmount = money(it.igstAmount),
                totalAmount = money(it.totalAmount),
                status = it.status
            )
        }

        val hsnAccumulator = linkedMapOf<String, MutableHsn>()

        sales.forEach { sale ->
            salesRepository.getSaleItems(sale.id).first().forEach { item ->
                val key = item.hsnCode.trim().ifBlank { "UNSPECIFIED" }
                val row = hsnAccumulator.getOrPut(key) {
                    MutableHsn(
                        hsnCode = key,
                        description = item.productName.trim()
                    )
                }
                row.quantity += item.quantity
                row.taxableAmount += item.taxableAmount
                row.gstAmount += item.gstAmount
            }
        }

        purchases.forEach { purchase ->
            purchaseRepository.getPurchaseItems(purchase.id).first().forEach { item ->
                val key = item.hsnCode.trim().ifBlank { "UNSPECIFIED" }
                val row = hsnAccumulator.getOrPut(key) {
                    MutableHsn(
                        hsnCode = key,
                        description = "Purchase HSN"
                    )
                }
                row.quantity += item.quantity
                row.taxableAmount += item.taxableAmount
                row.gstAmount += item.gstAmount
            }
        }

        return GstReportSnapshot(
            financialYearStart = financialYearStart,
            sales = saleRows,
            purchases = purchaseRows,
            creditNotes = creditRows,
            hsnRows = hsnAccumulator.values.map {
                GstHsnRow(
                    hsnCode = it.hsnCode,
                    description = it.description,
                    quantity = it.quantity,
                    taxableAmount = money(it.taxableAmount),
                    gstAmount = money(it.gstAmount)
                )
            }.sortedBy { it.hsnCode }
        )
    }

    private data class MutableHsn(
        val hsnCode: String,
        val description: String,
        var quantity: Int = 0,
        var taxableAmount: Double = 0.0,
        var gstAmount: Double = 0.0
    )

    private fun money(value: Double): Double =
        round(value * 100.0) / 100.0
}
