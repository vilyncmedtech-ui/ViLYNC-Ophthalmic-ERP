package com.vilync.ophthalmicerp.feature.inventory.threshold.data

import com.vilync.ophthalmicerp.data.dao.InventoryThresholdDao
import com.vilync.ophthalmicerp.data.entity.InventoryThresholdEntity
import kotlinx.coroutines.flow.Flow

class InventoryThresholdRepository(
    private val thresholdDao: InventoryThresholdDao
) {
    suspend fun upsertThreshold(threshold: InventoryThresholdEntity) = 
        thresholdDao.upsertThreshold(threshold)

    fun getThresholdsForProduct(productId: Long): Flow<List<InventoryThresholdEntity>> = 
        thresholdDao.getThresholdsForProduct(productId)

    suspend fun getThreshold(productId: Long, power: String): InventoryThresholdEntity? = 
        thresholdDao.getThreshold(productId, power)

    suspend fun deleteThreshold(threshold: InventoryThresholdEntity) = 
        thresholdDao.deleteThreshold(threshold)
}
