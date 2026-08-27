package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.dao.OpeningStockDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockItemEntity
import kotlinx.coroutines.flow.Flow

class OpeningStockRepository(
    private val openingStockDao: OpeningStockDao,
    private val database: AppDatabase
) {

    suspend fun saveCompleteOpeningStock(
        openingStock: OpeningStockEntity,
        items: List<OpeningStockItemEntity>
    ): Long {
        return openingStockDao.saveCompleteOpeningStock(openingStock, items)
    }

    suspend fun updateOpeningStock(openingStock: OpeningStockEntity) {
        openingStockDao.updateOpeningStock(openingStock)
    }

    fun getAllOpeningStocks(): Flow<List<OpeningStockEntity>> {
        return openingStockDao.getAllOpeningStocks()
    }

    suspend fun getOpeningStockById(id: Long): OpeningStockEntity? {
        return openingStockDao.getOpeningStockById(id)
    }

    suspend fun getCancelledOpeningStocks(): List<OpeningStockEntity> {
        return openingStockDao.getCancelledOpeningStocks()
    }

    fun getOpeningStockItems(openingStockId: Long): Flow<List<OpeningStockItemEntity>> {
        return openingStockDao.getOpeningStockItems(openingStockId)
    }

    suspend fun getOpeningStockItemsList(openingStockId: Long): List<OpeningStockItemEntity> {
        return openingStockDao.getOpeningStockItemsList(openingStockId)
    }

    suspend fun cancelOpeningStock(id: Long, cancelledAt: Long) {
        openingStockDao.cancelOpeningStock(id, cancelledAt)
    }

    suspend fun <R> withTransaction(block: suspend () -> R): R {
        return database.withTransaction(block)
    }
}
