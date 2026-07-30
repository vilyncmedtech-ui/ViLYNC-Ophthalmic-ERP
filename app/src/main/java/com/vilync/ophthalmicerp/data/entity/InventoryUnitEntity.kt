package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_units",
    indices = [
        Index(value = ["serialNumber"], unique = true),
        Index(value = ["productId"]),
        Index(value = ["status"])
    ]
)
data class InventoryUnitEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Product Master reference
    val productId: Long,

    // Example: 20.0D, 20.5D etc.
    val power: String = "",

    // Unique serial number of individual lens/unit
    val serialNumber: String,

    // Manufacturer batch / lot
    val batchNumber: String = "",

    // Expiry date
    val expiryDate: String = "",

    // Date on which stock was received
    val receivedDate: String = "",

    // Supplier from whom this unit was received
    val supplierName: String = "",

    // Supplier purchase invoice reference
    val purchaseInvoiceNumber: String = "",

    // Current inventory status
    val status: String = "IN_STOCK"
)