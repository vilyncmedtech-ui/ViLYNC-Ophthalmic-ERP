package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.StockMovementDao
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

class StockMovementRepository(
    private val stockMovementDao: StockMovementDao
) {

    // Add a new movement/history record
    suspend fun insertMovement(
        movement: StockMovementEntity
    ): Long {
        return stockMovementDao.insertMovement(movement)
    }


    // Complete movement history of one inventory unit
    fun getMovementHistoryByUnit(
        inventoryUnitId: Long
    ): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementHistoryByUnit(
            inventoryUnitId
        )
    }


    // Complete history using Serial Number
    fun getMovementHistoryBySerialNumber(
        serialNumber: String
    ): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementHistoryBySerialNumber(
            serialNumber
        )
    }


    // Get movements according to movement type
    fun getMovementsByType(
        movementType: String
    ): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementsByType(
            movementType
        )
    }


    // Get complete stock movement history
    fun getAllMovements(): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getAllMovements()
    }
}