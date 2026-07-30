package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity

class InventoryTransactionRepository(
    private val database: AppDatabase
) {

    suspend fun changeInventoryStatusWithMovement(
        inventoryUnitId: Long,
        serialNumber: String,
        fromStatus: String,
        toStatus: String,
        movementType: String,
        partyName: String = "",
        referenceNumber: String = "",
        movementDate: String,
        remarks: String = ""
    ) {

        database.withTransaction {

            // Step 1:
            // Change current inventory status
            database.inventoryDao().updateStatus(
                unitId = inventoryUnitId,
                newStatus = toStatus
            )

            // Step 2:
            // Save permanent movement history
            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    inventoryUnitId = inventoryUnitId,
                    serialNumber = serialNumber,
                    movementType = movementType,
                    fromStatus = fromStatus,
                    toStatus = toStatus,
                    partyName = partyName,
                    referenceNumber = referenceNumber,
                    movementDate = movementDate,
                    remarks = remarks
                )
            )
        }
    }
}