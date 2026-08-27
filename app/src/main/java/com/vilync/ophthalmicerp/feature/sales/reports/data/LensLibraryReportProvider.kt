package com.vilync.ophthalmicerp.feature.sales.reports.data

import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Aggregates and reconciles physical library lenses using the authoritative settlementStatus.
 */
object LensLibraryReportProvider {

    suspend fun provideData(
        database: AppDatabase,
        filters: Map<String, Any?>
    ): List<ReportRowData> = withContext(Dispatchers.IO) {
        
        val customerId = filters["customer"]?.toString()?.split("|")?.firstOrNull()?.toLongOrNull() ?: 0L
        if (customerId == 0L) return@withContext emptyList()

        val startDate = filters["date_from"]?.toString() ?: "2026-04-01"
        val endDate = filters["date_to"]?.toString() ?: "2027-03-31"
        val productFilterId = filters["product"]?.toString()?.split("|")?.firstOrNull() ?: "0"
        val powerFilter = filters["power"]?.toString() ?: ""

        val rawData = database.challanDao().getLensLibraryReportData(customerId, startDate, endDate)

        // Filter and Group
        val processedData = rawData.filter { row ->
            val matchesProduct = productFilterId == "0" || row.productId.toString() == productFilterId
            val matchesPower = powerFilter.isBlank() || row.power.contains(powerFilter, ignoreCase = true)
            matchesProduct && matchesPower
        }

        // Group by Product + Power for the main table
        processedData.groupBy { it.productName to it.power }
            .map { (key, group) ->
                val (product, power) = key
                val issued = group.size
                val invoiced = group.count { it.settlementStatus == "INVOICED" }
                val balance = group.count { it.settlementStatus == "PENDING" }
                
                val serialDetails = group
                    .filter { it.settlementStatus == "PENDING" }
                    .joinToString("\n") { row ->
                        "${row.serialNumber} | Exp: ${row.expiryDate} | Ch: ${row.challanNumber} (${row.challanDate})"
                    }

                ReportRowData(
                    id = key.hashCode().toLong(),
                    values = mapOf(
                        "product" to product,
                        "power" to power,
                        "issued" to issued,
                        "invoiced" to invoiced,
                        "balance" to balance,
                        "serials" to serialDetails
                    )
                )
            }
    }

    fun calculateSummary(rows: List<ReportRowData>): Map<String, String> {
        val totalIssued = rows.sumOf { it.values["issued"]?.toString()?.toIntOrNull() ?: 0 }
        val totalInvoiced = rows.sumOf { it.values["invoiced"]?.toString()?.toIntOrNull() ?: 0 }
        val currentBalance = rows.sumOf { it.values["balance"]?.toString()?.toIntOrNull() ?: 0 }

        return mapOf(
            "total_issued" to totalIssued.toString(),
            "total_invoiced" to totalInvoiced.toString(),
            "library_balance" to currentBalance.toString()
        )
    }
}
