package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opening_stock_items",
    foreignKeys = [
        ForeignKey(
            entity = OpeningStockEntity::class,
            parentColumns = ["id"],
            childColumns = ["openingStockId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["openingStockId"]),
        Index(value = ["productId"]),
        Index(value = ["power"])
    ]
)
data class OpeningStockItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val openingStockId: Long,

    val productId: Long,

    // Snapshots
    val productName: String,
    val model: String = "",

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",

    val quantity: Int,

    val unitCost: Double = 0.0,

    val totalCost: Double = 0.0
)
