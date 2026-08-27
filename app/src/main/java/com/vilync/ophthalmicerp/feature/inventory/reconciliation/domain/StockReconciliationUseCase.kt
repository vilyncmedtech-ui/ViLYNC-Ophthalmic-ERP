package com.vilync.ophthalmicerp.feature.inventory.reconciliation.domain

import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.feature.inventory.movement.MovementType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockReconciliationUseCase(
    private val inventoryRepository: InventoryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val auditTrailRepository: AuditTrailRepository
) {

    /**
     * Performs a physical reconciliation for a specific Product + Power.
     *
     * @param productId The ID of the product being reconciled.
     * @param power The normalized power string (e.g., "19D").
     * @param physicalSerialIds The set of Inventory Unit IDs that were physically confirmed.
     * @param remarks User-provided notes for the reconciliation.
     */
    suspend fun performReconciliation(
        productId: Long,
        power: String,
        physicalSerialIds: Set<Long>,
        remarks: String
    ) {
        inventoryRepository.withTransaction {
            // 1. Fetch CURRENT System Stock for this Product + Power
            val systemUnits = inventoryRepository.getAvailableUnitsByProductAndPower(productId, power)
            val systemUnitIds = systemUnits.map { it.id }.toSet()

            // 2. Identify Shortage: Exist in System but not in Physical Count
            val missingUnitIds = systemUnitIds - physicalSerialIds
            
            if (missingUnitIds.isEmpty() && systemUnitIds.size == physicalSerialIds.size) {
                // Zero Difference Reconciliation
                recordReconciliationAudit(productId, power, systemUnitIds.size, physicalSerialIds.size, 0, "Zero Difference: All serials matched. $remarks")
                return@withTransaction
            }

            val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val reference = "RECON-" + System.currentTimeMillis()

            // 3. Resolve Shortage: Move missing units to SHORTAGE status
            missingUnitIds.forEach { unitId ->
                val unit = systemUnits.find { it.id == unitId } 
                    ?: throw IllegalStateException("Unit $unitId not found in system state.")
                
                // Final revalidation of individual unit
                if (unit.status != "IN_STOCK") {
                    throw IllegalStateException("Serial ${unit.serialNumber} is no longer IN_STOCK (Stale data). Refresh required.")
                }

                inventoryRepository.updateStatus(unitId, "SHORTAGE")

                stockMovementRepository.insertMovement(
                    StockMovementEntity(
                        inventoryUnitId = unitId,
                        serialNumber = unit.serialNumber,
                        movementType = MovementType.STOCK_ADJUSTMENT_OUT.name,
                        fromStatus = "IN_STOCK",
                        toStatus = "SHORTAGE",
                        partyName = "INTERNAL",
                        referenceNumber = reference,
                        movementDate = today,
                        remarks = "Reconciliation Shortage. $remarks"
                    )
                )
            }

            // 4. Audit the overall reconciliation event
            recordReconciliationAudit(
                productId, 
                power, 
                systemUnitIds.size, 
                physicalSerialIds.size, 
                physicalSerialIds.size - systemUnitIds.size,
                "Stock Reconciled. Missing: ${missingUnitIds.size}. $remarks"
            )
        }
    }

    private suspend fun recordReconciliationAudit(
        productId: Long,
        power: String,
        systemQty: Int,
        physicalQty: Int,
        diff: Int,
        description: String
    ) {
        auditTrailRepository.recordEvent(
            module = "INVENTORY",
            action = "STOCK_RECONCILIATION",
            recordId = productId,
            referenceNumber = power,
            description = "Audit: Sys=$systemQty, Phys=$physicalQty, Diff=$diff. $description"
        )
    }

    suspend fun getInStockUnits(productId: Long, power: String) =
        inventoryRepository.getAvailableUnitsByProductAndPower(productId, power)

    suspend fun getUnitBySerial(serial: String) =
        inventoryRepository.getBySerialNumber(serial)

    suspend fun getUnitBySerialAgnostic(serial: String) =
        inventoryRepository.getUnitBySerialAgnostic(serial)
}
