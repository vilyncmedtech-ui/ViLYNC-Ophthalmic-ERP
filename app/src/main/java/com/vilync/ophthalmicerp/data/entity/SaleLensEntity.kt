package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_lenses",

    foreignKeys = [
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleItemId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = InventoryUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryUnitId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],

    indices = [
        Index(value = ["saleItemId"]),
        Index(value = ["inventoryUnitId"]),
        Index(value = ["serialNumber"])
    ]
)
data class SaleLensEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // SALE ITEM REFERENCE
    // =========================================================

    val saleItemId: Long,

    // =========================================================
    // PHYSICAL INVENTORY UNIT
    // =========================================================

    // Exact physical unit selected from Inventory.
    val inventoryUnitId: Long,

    // Serial snapshot retained for invoice/report/search.
    val serialNumber: String,

    // =========================================================
    // LENS TRACEABILITY SNAPSHOTS
    // =========================================================

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = ""
)