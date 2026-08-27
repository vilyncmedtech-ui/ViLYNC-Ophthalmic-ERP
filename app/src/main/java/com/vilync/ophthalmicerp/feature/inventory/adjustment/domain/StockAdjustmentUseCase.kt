package com.vilync.ophthalmicerp.feature.inventory.adjustment.domain

import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.feature.inventory.movement.MovementType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockAdjustmentUseCase(
    private val inventoryRepository: InventoryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val auditTrailRepository: AuditTrailRepository
) {

    suspend fun adjustStock(
        productId: Long,
        power: String,
        inventoryUnitId: Long,
        reason: String,
        remarks: String
    ) {
        inventoryRepository.withTransaction {
            val unit = inventoryRepository.getById(inventoryUnitId) 
                ?: throw IllegalStateException("Physical inventory unit not found.")
            
            // Revalidation: Single source of truth check
            if (unit.productId != productId) {
                throw IllegalStateException("Selected serial does not belong to the selected product.")
            }
            if (unit.power != power) {
                throw IllegalStateException("Selected serial does not match the selected power.")
            }
            if (unit.status != "IN_STOCK") {
                throw IllegalStateException("Serial Number ${unit.serialNumber} is not available for adjustment (Current status: ${unit.status}).")
            }

            // Update physical status
            inventoryRepository.updateStatus(inventoryUnitId, reason.uppercase())

            val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            // Record movement
            stockMovementRepository.insertMovement(
                StockMovementEntity(
                    inventoryUnitId = inventoryUnitId,
                    serialNumber = unit.serialNumber,
                    movementType = MovementType.STOCK_ADJUSTMENT_OUT.name,
                    fromStatus = "IN_STOCK",
                    toStatus = reason.uppercase(),
                    partyName = "INTERNAL",
                    referenceNumber = "ADJ-" + System.currentTimeMillis(),
                    movementDate = today,
                    remarks = "Stock Adjustment: $reason. $remarks"
                )
            )

            // Audit Trail
            auditTrailRepository.recordEvent(
                module = "INVENTORY",
                action = "STOCK_ADJUSTMENT",
                recordId = inventoryUnitId,
                referenceNumber = unit.serialNumber,
                description = "Stock adjusted due to $reason. Serial: ${unit.serialNumber}. Remarks: $remarks"
            )
        }
    }
    
    suspend fun getAvailableUnits(productId: Long, power: String) =
        inventoryRepository.getAvailableUnitsByProductAndPower(productId, power)
}
