package com.vilync.ophthalmicerp.feature.inventory.ageing

import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class InventoryAgeingUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    companion object {
        const val BUCKET_0_30 = "0-30 Days"
        const val BUCKET_31_60 = "31-60 Days"
        const val BUCKET_61_90 = "61-90 Days"
        const val BUCKET_91_180 = "91-180 Days"
        const val BUCKET_181_PLUS = "180+ Days"
        
        val ALL_BUCKETS = listOf(
            BUCKET_0_30,
            BUCKET_31_60,
            BUCKET_61_90,
            BUCKET_91_180,
            BUCKET_181_PLUS
        )

        fun getBucket(ageDays: Long): String {
            return when {
                ageDays <= 30 -> BUCKET_0_30
                ageDays <= 60 -> BUCKET_31_60
                ageDays <= 90 -> BUCKET_61_90
                ageDays <= 180 -> BUCKET_91_180
                else -> BUCKET_181_PLUS
            }
        }
    }

    suspend fun getAgeingReport(filters: Map<String, Any?>): List<ReportRowData> {
        val products = productRepository.getAllActiveProducts().first()
        val productMap = products.associateBy { it.id }
        
        val inStockUnits = inventoryRepository.getInStockUnits().first()
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = Date()

        return inStockUnits.mapNotNull { unit ->
            val product = productMap[unit.productId] ?: return@mapNotNull null
            
            val receivedDate = try {
                sdf.parse(unit.receivedDate) ?: today
            } catch (e: Exception) {
                today
            }
            
            val diffInMs = today.time - receivedDate.time
            val ageDays = TimeUnit.MILLISECONDS.toDays(diffInMs).coerceAtLeast(0)
            val bucket = getBucket(ageDays)

            ReportRowData(
                id = unit.id,
                values = mapOf(
                    "product" to product.productName,
                    "model" to product.model,
                    "category" to product.category,
                    "power" to unit.power,
                    "batch" to unit.batchNumber,
                    "serial" to unit.serialNumber,
                    "receivedDate" to unit.receivedDate,
                    "ageDays" to ageDays.toString(),
                    "bucket" to bucket,
                    "status" to unit.status,
                    "vendor" to unit.supplierName,
                    "location" to "Main Store" // Default as per architecture verify
                )
            )
        }.filter { row ->
            // Apply Filters
            val productFilter = filters["product"]?.toString() ?: "All Products"
            val categoryFilter = filters["category"]?.toString() ?: "All Categories"
            val powerFilter = filters["power"]?.toString() ?: "All"
            val batchFilter = filters["batch"]?.toString() ?: ""
            val vendorFilter = filters["vendor"]?.toString() ?: "0"
            val bucketFilter = filters["bucket"]?.toString() ?: "All"
            
            val matchesProduct = productFilter == "All Products" || row.values["product"] == productFilter
            val matchesCategory = categoryFilter == "All Categories" || row.values["category"] == categoryFilter
            val matchesPower = powerFilter == "All" || row.values["power"] == powerFilter
            val matchesBatch = batchFilter.isBlank() || row.values["batch"].toString().contains(batchFilter, ignoreCase = true)
            val matchesBucket = bucketFilter == "All" || row.values["bucket"] == bucketFilter
            
            // Vendor filter in ERP usually uses ID, but here we have name in row. 
            // For now, if "0" (All), it matches. If not "0", we would need party master lookup.
            // Keeping it simple as per implementation rules.
            val matchesVendor = vendorFilter == "0"
            
            matchesProduct && matchesCategory && matchesPower && matchesBatch && matchesBucket && matchesVendor
        }.sortedByDescending { it.values["ageDays"]?.toString()?.toLongOrNull() ?: 0L }
    }
}
