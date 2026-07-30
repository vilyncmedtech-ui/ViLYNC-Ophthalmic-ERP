package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_lenses",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["purchaseItemId"]),
        Index(value = ["serialNumber"], unique = true),
        Index(value = ["expiryDate"])
    ]
)
data class PurchaseLensEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Parent PurchaseItemEntity reference
    val purchaseItemId: Long,

    // Unique physical IOL serial number
    val serialNumber: String,

    // Expiry in MMYY format
    // Example: 1229 = December 2029
    val expiryDate: String
)
