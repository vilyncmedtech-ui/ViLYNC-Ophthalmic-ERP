package com.vilync.ophthalmicerp.feature.inventory.alert

import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase
import com.vilync.ophthalmicerp.feature.inventory.logic.InventorySnapshot

class LowStockAlertUseCase(
    private val stockEngine: GetAvailableStockUseCase
) {
    companion object {
        const val STATUS_OUT_OF_STOCK = "OUT_OF_STOCK"
        const val STATUS_LOW_STOCK = "LOW_STOCK"
        const val STATUS_REORDER = "REORDER"
        const val STATUS_HEALTHY = "HEALTHY"
    }

    /**
     * Returns raw variant-wise snapshots for dedicated status screens.
     */
    suspend fun getSnapshots(): List<InventorySnapshot> {
        return stockEngine.execute()
    }

    /**
     * The canonical classification logic for the entire ERP.
     */
    fun classify(snapshot: InventorySnapshot): String {
        val qty = snapshot.availableQuantity
        val min = snapshot.minimumStock
        val reorder = snapshot.reorderLevel
        
        return when {
            qty <= 0 -> STATUS_OUT_OF_STOCK
            qty < min -> STATUS_LOW_STOCK
            qty < reorder -> STATUS_REORDER
            else -> STATUS_HEALTHY
        }
    }

    /**
     * Compatibility layer for the Universal Report Engine.
     */
    suspend fun getAlerts(filters: Map<String, Any?>): List<ReportRowData> {
        val snapshots = getSnapshots()
        
        return snapshots.map { snapshot ->
            val status = classify(snapshot)
            
            ReportRowData(
                // Use hash code for generic report ID, but dedicated screens should use (productId, power)
                id = (snapshot.productId.toString() + snapshot.power).hashCode().toLong(),
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
                STATUS_LOW_STOCK -> 1
                STATUS_REORDER -> 2
                else -> 3
            }
        }.thenBy { it.values["product"]?.toString() ?: "" })
    }

    fun calculateSummary(rows: List<ReportRowData>): Map<String, String> {
        return mapOf(
            "total" to rows.size.toString(),
            "outOfStock" to rows.count { it.values["status"] == STATUS_OUT_OF_STOCK }.toString(),
            "lowStock" to rows.count { it.values["status"] == STATUS_LOW_STOCK }.toString(),
            "reorder" to rows.count { it.values["status"] == STATUS_REORDER }.toString()
        )
    }
}
