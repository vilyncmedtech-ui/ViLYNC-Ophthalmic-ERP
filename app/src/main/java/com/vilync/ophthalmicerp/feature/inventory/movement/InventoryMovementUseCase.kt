package com.vilync.ophthalmicerp.feature.inventory.movement

import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.data.dao.StockMovementRegisterRow

class InventoryMovementUseCase(
    private val repository: StockMovementRepository
) {
    suspend fun getMovements(
        startDate: String,
        endDate: String,
        movementType: String = "All",
        partyName: String? = null,
        serialNumber: String? = null,
        productId: Long? = null,
        power: String? = null
    ): List<StockMovementRegisterRow> {
        return repository.getMovementRegister(
            startDate, endDate, movementType, partyName, serialNumber, productId, power
        )
    }
}
