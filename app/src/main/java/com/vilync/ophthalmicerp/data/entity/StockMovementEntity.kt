package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["inventoryUnitId"]),
        Index(value = ["serialNumber"]),
        Index(value = ["movementType"])
    ]
)
data class StockMovementEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Inventory unit reference
    val inventoryUnitId: Long,

    // Serial number kept for quick tracking/search
    val serialNumber: String,

    // Example:
    // PURCHASE_RECEIVED
    // SAMPLE_ISSUED
    // DEMO_ISSUED
    // APPROVAL_ISSUED
    // RETURNED
    // EVALUATED
    // SOLD
    val movementType: String,

    // Previous status before this movement
    val fromStatus: String = "",

    // New status after this movement
    val toStatus: String,

    // Doctor / Hospital / Customer name
    val partyName: String = "",

    // Optional reference:
    // Invoice / Challan / Sample Issue reference etc.
    val referenceNumber: String = "",

    // Movement date
    val movementDate: String,

    // Optional remarks
    val remarks: String = ""
)