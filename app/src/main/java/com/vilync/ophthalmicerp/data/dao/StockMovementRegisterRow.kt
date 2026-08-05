package com.vilync.ophthalmicerp.data.dao

import androidx.room.Embedded
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity

data class StockMovementRegisterRow(
    @Embedded val movement: StockMovementEntity,
    val productName: String?,
    val model: String?,
    val category: String?,
    val power: String?,
    val batchNumber: String?,
    val userName: String?
)
