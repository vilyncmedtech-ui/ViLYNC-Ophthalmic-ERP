package com.vilync.ophthalmicerp.feature.inventory.movement

import androidx.lifecycle.ViewModel
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.data.dao.StockMovementRegisterRow
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.master.party.data.PartyEntity
import java.util.Locale

class InventoryMovementViewModel(
    private val useCase: InventoryMovementUseCase,
    private val products: List<ProductEntity>,
    private val parties: List<PartyEntity>
) : ViewModel() {

    suspend fun provideReportData(filters: Map<String, Any?>): List<ReportRowData> {
        val start = filters["date_from"]?.toString() ?: "2026-07-01"
        val end = filters["date_to"]?.toString() ?: "2026-07-31"
        val moveType = filters["movement_type"]?.toString() ?: "All"
        val serial = filters["serial"]?.toString()?.takeIf { it.isNotBlank() }
        val powerFilter = filters["power"]?.toString()?.takeIf { it != "All" }
        
        val selectedProductName = filters["product"]?.toString() ?: "All Products"
        val prodId = if (selectedProductName == "All Products") null else {
            products.find { it.productName.replace(Regex("^\\d+\\s+"), "").trim() == selectedProductName }?.id
        }

        val custIdStr = filters["customer"]?.toString() ?: "0"
        val vendorIdStr = filters["vendor"]?.toString() ?: "0"
        
        val partyNameFilter = when {
            custIdStr != "0" -> parties.find { it.id.toString() == custIdStr }?.partyName
            vendorIdStr != "0" -> parties.find { it.id.toString() == vendorIdStr }?.partyName
            else -> null
        }

        // 1. Fetch raw data in CHRONOLOGICAL order (SQL ORDER BY Date ASC)
        val rawMovements = useCase.getMovements(
            startDate = start,
            endDate = end,
            movementType = moveType,
            partyName = partyNameFilter,
            serialNumber = serial,
            productId = prodId,
            power = powerFilter
        )

        // 2. Calculate Running Balances in memory
        val rowsWithBalance = calculateBalances(rawMovements)

        // 3. Return reverse chronological for UI (Newest first)
        return rowsWithBalance.reversed()
    }

    private fun calculateBalances(raw: List<StockMovementRegisterRow>): List<ReportRowData> {
        var balance = 0
        return raw.map { row ->
            val isStockIn = isMovementIn(row)
            val isStockOut = isMovementOut(row)
            
            if (isStockIn) balance++
            if (isStockOut) balance--

            ReportRowData(
                id = row.movement.id,
                values = mapOf(
                    "date" to row.movement.movementDate,
                    "type" to row.movement.movementType.replace("_", " "),
                    "product" to (row.productName ?: "-"),
                    "model" to (row.model ?: "-"),
                    "power" to (row.power ?: "-"),
                    "serial" to row.movement.serialNumber,
                    "batch" to (row.batchNumber ?: "-"),
                    "party" to row.movement.partyName,
                    "refNo" to row.movement.referenceNumber,
                    "qtyIn" to if (isStockIn) "1" else "0",
                    "qtyOut" to if (isStockOut) "1" else "0",
                    "balance" to balance.toString(),
                    "user" to (row.userName ?: "SYSTEM"),
                    "status" to row.movement.toStatus
                )
            )
        }
    }

    private fun isMovementIn(row: StockMovementRegisterRow): Boolean {
        val type = row.movement.movementType.uppercase(Locale.getDefault())
        return type == "PURCHASE_RECEIVED" || type == "RETURNED" || type == "SALES_RETURN" || type == "SALE_EDIT_RETURN"
    }

    private fun isMovementOut(row: StockMovementRegisterRow): Boolean {
        val type = row.movement.movementType.uppercase(Locale.getDefault())
        return type == "SOLD" || type == "CHALLAN_ISSUED" || type == "SAMPLE_ISSUED" || type == "DEMO_ISSUED" || type == "SALE_EDIT_SOLD"
    }
}
