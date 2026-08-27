package com.vilync.ophthalmicerp.data.dao

import androidx.room.*
import com.vilync.ophthalmicerp.data.entity.InventoryThresholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryThresholdDao {

    @Upsert
    suspend fun upsertThreshold(threshold: InventoryThresholdEntity)

    @Query("SELECT * FROM inventory_thresholds WHERE productId = :productId")
    fun getThresholdsForProduct(productId: Long): Flow<List<InventoryThresholdEntity>>

    @Query("SELECT * FROM inventory_thresholds WHERE productId = :productId AND power = :power LIMIT 1")
    suspend fun getThreshold(productId: Long, power: String): InventoryThresholdEntity?

    @Query("SELECT * FROM inventory_thresholds")
    suspend fun getAllThresholds(): List<InventoryThresholdEntity>

    @Delete
    suspend fun deleteThreshold(threshold: InventoryThresholdEntity)
}
