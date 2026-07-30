package com.vilync.ophthalmicerp.feature.gst.data

import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.gst.model.GstSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GstSummaryRepository(
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository
) {
    fun observeSummary(financialYearStart: Int): Flow<GstSummary> {
        require(financialYearStart > 0) { "Valid Financial Year start is required." }

        return combine(
            salesRepository.getSalesByFinancialYear(financialYearStart),
            purchaseRepository.getPurchasesByFinancialYear(financialYearStart)
        ) { sales, purchases ->

            val postedSales = sales.filter {
                it.status.trim().equals("POSTED", ignoreCase = true)
            }

            GstSummary(
                financialYearStart = financialYearStart,
                outputTaxableValue = money(postedSales.sumOf { it.taxableAmount }),
                outputCgst = money(postedSales.sumOf { it.cgstAmount }),
                outputSgst = money(postedSales.sumOf { it.sgstAmount }),
                outputIgst = money(postedSales.sumOf { it.igstAmount }),
                inputTaxableValue = money(purchases.sumOf { it.taxableAmount }),

                // Purchase tax heads are read exactly as persisted.
                // Historical rows are never recalculated, reclassified or overwritten here.
                inputCgstPersisted = money(purchases.sumOf { it.cgstAmount }),
                inputSgstPersisted = money(purchases.sumOf { it.sgstAmount }),
                inputIgstPersisted = money(purchases.sumOf { it.igstAmount }),

                postedSalesCount = postedSales.size,
                purchaseCount = purchases.size
            )
        }
    }

    private fun money(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}
