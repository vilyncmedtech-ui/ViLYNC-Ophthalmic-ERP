package com.vilync.ophthalmicerp.feature.inventory.ageing

import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class InventoryAgeingUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository
) {
    companion object {
        const val RISK_FRESH = "Fresh"
        const val RISK_SENSITIVE = "Sensitive"
        const val RISK_ON_RISK = "On Risk"
        const val RISK_HIGH_RISK = "High Risk"
        const val RISK_EXPIRED = "Expired"

        fun getExpiryRisk(daysLeft: Long): String {
            return when {
                daysLeft > 365 -> RISK_FRESH
                daysLeft >= 181 -> RISK_SENSITIVE
                daysLeft >= 91 -> RISK_ON_RISK
                daysLeft >= 1 -> RISK_HIGH_RISK
                else -> RISK_EXPIRED
            }
        }

        /**
         * Parses MMYY string and returns the last day of that month.
         * Example: 1229 -> 2029-12-31
         */
        fun parseExpiryDate(mmyy: String): LocalDate? {
            val clean = mmyy.filter { it.isDigit() }
            if (clean.length != 4) return null
            
            return try {
                val formatter = DateTimeFormatter.ofPattern("MMyy")
                val ym = YearMonth.parse(clean, formatter)
                ym.atEndOfMonth()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getAgeingReport(filters: Map<String, Any?>): List<ReportRowData> {
        val products = productRepository.getAllActiveProducts().first()
        val productMap = products.associateBy { it.id }
        
        val inStockUnits = inventoryRepository.getInStockUnits().first()
        
        val productFilterId = filters["product"]?.toString() ?: "0"
        val categoryFilter = filters["category"]?.toString() ?: "All Categories"
        val powerFilter = filters["power"]?.toString() ?: "All"
        val batchFilter = filters["batch"]?.toString() ?: ""
        val vendorFilterId = filters["vendor"]?.toString() ?: "0"
        val riskFilter = filters["expiry_risk"]?.toString() ?: "All"

        // Resolve vendor name for filtering
        val selectedVendorName = if (vendorFilterId != "0") {
            partyRepository.getPartyById(vendorFilterId.toLongOrNull() ?: 0L)?.partyName
        } else null

        val today = LocalDate.now()
        val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        return inStockUnits.mapNotNull { unit ->
            val product = productMap[unit.productId] ?: return@mapNotNull null
            
            // Apply Filters Early (Product, Category, Power, Batch, Vendor)
            val matchesProduct = productFilterId == "0" || unit.productId.toString() == productFilterId
            val matchesCategory = categoryFilter == "All Categories" || product.category == categoryFilter
            val matchesPower = powerFilter == "All" || unit.power == powerFilter
            val matchesBatch = batchFilter.isBlank() || unit.batchNumber.contains(batchFilter, ignoreCase = true)
            
            val matchesVendor = selectedVendorName == null || 
                    unit.supplierName.equals(selectedVendorName, ignoreCase = true)
            
            if (!matchesProduct || !matchesCategory || !matchesPower || !matchesBatch || !matchesVendor) return@mapNotNull null

            // Expiry Calculation
            val expiryDate = parseExpiryDate(unit.expiryDate)
            
            // Handle missing/invalid expiry safely: display N/A, skip risk filtering unless "All"
            val daysLeft: Long?
            val riskStatus: String
            val displayExpiry: String

            if (expiryDate != null) {
                daysLeft = ChronoUnit.DAYS.between(today, expiryDate)
                riskStatus = getExpiryRisk(daysLeft)
                displayExpiry = expiryDate.format(displayFormatter)
            } else {
                daysLeft = null
                riskStatus = "N/A"
                displayExpiry = unit.expiryDate.ifBlank { "N/A" }
            }
            
            // Expiry Risk Filter
            if (riskFilter != "All" && riskStatus != riskFilter) return@mapNotNull null

            ReportRowData(
                id = unit.id,
                values = mapOf(
                    "product" to product.productName,
                    "model" to product.model,
                    "category" to product.category,
                    "power" to unit.power,
                    "batch" to unit.batchNumber,
                    "serial" to unit.serialNumber,
                    "expiryDate" to displayExpiry,
                    "daysLeft" to (daysLeft?.toString() ?: "—"),
                    "expiryStatus" to riskStatus,
                    "status" to unit.status,
                    "vendor" to unit.supplierName,
                    "location" to "Main Store"
                )
            )
        }.sortedBy { it.values["daysLeft"]?.toString()?.toLongOrNull() ?: Long.MAX_VALUE }
    }
}
