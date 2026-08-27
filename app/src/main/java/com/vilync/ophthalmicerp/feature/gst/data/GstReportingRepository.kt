package com.vilync.ophthalmicerp.feature.gst.data

import com.vilync.ophthalmicerp.data.repository.ProductRepository
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
    private val salesCreditNoteRepository: SalesCreditNoteRepository,
    private val productRepository: ProductRepository
) {
    suspend fun load(
        financialYearStart: Int,
        startDate: String? = null,
        endDate: String? = null
    ): GstReportSnapshot {
        require(financialYearStart > 0) { "Valid Financial Year is required." }

        var salesFlow = salesRepository.getSalesByFinancialYear(financialYearStart).first()
        var purchasesFlow = purchaseRepository.getPurchasesByFinancialYear(financialYearStart).first()
        var creditNotesFlow = salesCreditNoteRepository.getCreditNotesByFinancialYear(financialYearStart).first()

        // Apply Date Filtering if provided
        if (startDate != null && endDate != null) {
            salesFlow = salesFlow.filter { isDateInRange(it.invoiceDate, startDate, endDate) }
            purchasesFlow = purchasesFlow.filter { isDateInRange(it.invoiceDate, startDate, endDate) }
            creditNotesFlow = creditNotesFlow.filter { isDateInRange(it.creditNoteDate, startDate, endDate) }
        }

        val sales = salesFlow.filter { it.status.trim().equals("POSTED", ignoreCase = true) }
        val purchases = purchasesFlow
        val creditNotes = creditNotesFlow.filter { it.status.trim().equals("POSTED", ignoreCase = true) }

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

        // Helper to resolve HSN with Master fallback
        val productHsnCache = mutableMapOf<Long, String>()
        suspend fun resolveHsn(productId: Long, itemHsn: String): String {
            val trimmed = itemHsn.trim()
            if (trimmed.isNotBlank()) return trimmed
            
            return productHsnCache.getOrPut(productId) {
                productRepository.getProductById(productId)?.hsnCode?.trim() ?: ""
            }.ifBlank { "UNSPECIFIED" }
        }

        sales.forEach { sale ->
            salesRepository.getSaleItems(sale.id).first().forEach { item ->
                val hsn = resolveHsn(item.productId, item.hsnCode)
                val rate = item.gstPercent
                val key = "${hsn}_${rate}"
                
                val row = hsnAccumulator.getOrPut(key) {
                    MutableHsn(
                        hsnCode = hsn,
                        gstPercent = rate,
                        description = item.productName.trim()
                    )
                }
                row.quantity += item.quantity
                row.taxableAmount += item.taxableAmount
                row.gstAmount += item.gstAmount
            }
        }

        // Deduct Credit Notes (Sales Returns) from HSN Summary
        creditNotes.forEach { cn ->
            salesCreditNoteRepository.getItemsByCreditNoteId(cn.id).forEach { item ->
                val hsn = resolveHsn(item.productId, "") 
                val rate = item.gstPercent
                val key = "${hsn}_${rate}"
                
                val row = hsnAccumulator.getOrPut(key) {
                    MutableHsn(
                        hsnCode = hsn,
                        gstPercent = rate,
                        description = item.productName.trim()
                    )
                }
                row.quantity -= item.quantity
                row.taxableAmount -= item.taxableAmount
                row.gstAmount -= item.gstAmount
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
                    gstPercent = it.gstPercent,
                    gstAmount = money(it.gstAmount),
                    totalAmount = money(it.taxableAmount + it.gstAmount)
                )
            }.sortedWith(compareBy({ it.hsnCode }, { it.gstPercent }))
        )
    }

    private fun isDateInRange(dateStr: String, start: String, end: String): Boolean {
        // dateStr is dd-MM-yyyy, start/end are yyyy-MM-dd
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return false
            val normalized = "${parts[2]}-${parts[1]}-${parts[0]}"
            return normalized >= start && normalized <= end
        } catch (e: Exception) {
            return false
        }
    }

    private data class MutableHsn(
        val hsnCode: String,
        val gstPercent: Double,
        val description: String,
        var quantity: Int = 0,
        var taxableAmount: Double = 0.0,
        var gstAmount: Double = 0.0
    )

    private fun money(value: Double): Double =
        round(value * 100.0) / 100.0
}
