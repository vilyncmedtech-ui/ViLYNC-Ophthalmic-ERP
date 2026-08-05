package com.vilync.ophthalmicerp.feature.inventory.alert

import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase
import com.vilync.ophthalmicerp.feature.inventory.logic.InventorySnapshot

class LowStockAlertUseCase(
    private val stockEngine: GetAvailableStockUseCase
) {
    companion object {
        const val STATUS_OUT_OF_STOCK = "OUT_OF_STOCK"
        const val STATUS_CRITICAL = "CRITICAL"
        const val STATUS_LOW = "LOW"
        const val STATUS_HEALTHY = "HEALTHY"
    }

    suspend fun getAlerts(filters: Map<String, Any?>): List<ReportRowData> {
        val snapshots = stockEngine.execute()
        
        return snapshots.map { snapshot ->
            val status = classify(snapshot)
            
            ReportRowData(
                id = snapshot.productId,
                values = mapOf(
                    "product" to snapshot.productName,
                    "company" to snapshot.brandName,
                    "model" to snapshot.model,
                    "category" to snapshot.category,
                    "power" to snapshot.power,
                    "available" to snapshot.availableQuantity,
                    "minStock" to snapshot.minimumStock,
                    "reorderLevel" to snapshot.reorderLevel,
                    "status" to status
                )
            )
        }.filter { row ->
            val categoryFilter = filters["category"]?.toString() ?: "All Categories"
            val companyFilter = filters["company"]?.toString() ?: "All Companies"
            val statusFilter = filters["status"]?.toString() ?: "All"
            
            val matchesCategory = categoryFilter == "All Categories" || row.values["category"] == categoryFilter
            val matchesCompany = companyFilter == "All Companies" || row.values["company"] == companyFilter
            val matchesStatus = statusFilter == "All" || row.values["status"] == statusFilter
            
            matchesCategory && matchesCompany && matchesStatus
        }.sortedWith(compareBy<ReportRowData> { 
            when(it.values["status"]) {
                STATUS_OUT_OF_STOCK -> 0
                STATUS_CRITICAL -> 1
                STATUS_LOW -> 2
                else -> 3
            }
        }.thenBy { it.values["product"]?.toString() ?: "" })
    }

    private fun classify(snapshot: InventorySnapshot): String {
        val qty = snapshot.availableQuantity
        val min = snapshot.minimumStock
        val reorder = snapshot.reorderLevel
        
        return when {
            qty == 0 -> STATUS_OUT_OF_STOCK
            qty <= min -> STATUS_CRITICAL
            qty <= reorder -> STATUS_LOW
            else -> STATUS_HEALTHY
        }
    }

    fun calculateSummary(rows: List<ReportRowData>): Map<String, String> {
        return mapOf(
            "total" to rows.size.toString(),
            "outOfStock" to rows.count { it.values["status"] == STATUS_OUT_OF_STOCK }.toString(),
            "critical" to rows.count { it.values["status"] == STATUS_CRITICAL }.toString(),
            "low" to rows.count { it.values["status"] == STATUS_LOW }.toString()
        )
    }
}
