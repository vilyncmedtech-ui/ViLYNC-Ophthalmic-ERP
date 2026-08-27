package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "inventory_thresholds",
    primaryKeys = ["productId", "power"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"])
    ]
)
data class InventoryThresholdEntity(
    val productId: Long,
    val power: String, // Blank string for non-power products
    val minimumStock: Int,
    val reorderLevel: Int
)
