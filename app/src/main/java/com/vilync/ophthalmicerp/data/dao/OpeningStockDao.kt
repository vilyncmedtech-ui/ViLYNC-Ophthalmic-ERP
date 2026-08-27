package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningStockDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOpeningStock(openingStock: OpeningStockEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOpeningStockItems(items: List<OpeningStockItemEntity>)

    @Update
    suspend fun updateOpeningStock(openingStock: OpeningStockEntity)

    @Transaction
    suspend fun saveCompleteOpeningStock(
        openingStock: OpeningStockEntity,
        items: List<OpeningStockItemEntity>
    ): Long {
        val id = if (openingStock.id > 0) {
            updateOpeningStock(openingStock)
            deleteOpeningStockItems(openingStock.id)
            openingStock.id
        } else {
            insertOpeningStock(openingStock)
        }
        insertOpeningStockItems(items.map { it.copy(openingStockId = id) })
        return id
    }

    @Query("SELECT * FROM opening_stocks ORDER BY id DESC")
    fun getAllOpeningStocks(): Flow<List<OpeningStockEntity>>

    @Query("SELECT * FROM opening_stocks WHERE id = :id LIMIT 1")
    suspend fun getOpeningStockById(id: Long): OpeningStockEntity?

    @Query("SELECT * FROM opening_stocks WHERE status = 'CANCELLED'")
    suspend fun getCancelledOpeningStocks(): List<OpeningStockEntity>

    @Query("SELECT * FROM opening_stock_items WHERE openingStockId = :openingStockId ORDER BY id ASC")
    fun getOpeningStockItems(openingStockId: Long): Flow<List<OpeningStockItemEntity>>

    @Query("SELECT * FROM opening_stock_items WHERE openingStockId = :openingStockId ORDER BY id ASC")
    suspend fun getOpeningStockItemsList(openingStockId: Long): List<OpeningStockItemEntity>

    @Query("DELETE FROM opening_stock_items WHERE openingStockId = :openingStockId")
    suspend fun deleteOpeningStockItems(openingStockId: Long)

    @Query("UPDATE opening_stocks SET status = 'CANCELLED', updatedAt = :cancelledAt WHERE id = :id")
    suspend fun cancelOpeningStock(id: Long, cancelledAt: Long)
}
