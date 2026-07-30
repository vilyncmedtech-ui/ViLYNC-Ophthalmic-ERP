package com.vilync.ophthalmicerp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_items",
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"]),
        Index(value = ["power"])
    ]
)
data class PurchaseItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Purchase invoice reference
    val purchaseId: Long,

    // Product Master reference
    val productId: Long,

    // Historical HSN snapshot preserved with the purchase line.
    @ColumnInfo(defaultValue = "''")
    val hsnCode: String = "",

    // IOL Power - Example: 20.0D
    // Empty for products where power is not applicable
    val power: String = "",

    // Quantity purchased
    val quantity: Int,

    // Purchase rate per unit
    val purchaseRate: Double,

    // Discount percentage on this line
    val discountPercent: Double = 0.0,

    // GST percentage
    val gstPercent: Double = 0.0,

    // Batch / Lot Number
    val batchNumber: String = "",

    // Expiry Date
    val expiryDate: String = "",

    // Amount before discount/GST
    val grossAmount: Double = 0.0,

    // Discount amount
    val discountAmount: Double = 0.0,

    // Taxable amount after discount
    val taxableAmount: Double = 0.0,

    // Total GST amount for this line
    val gstAmount: Double = 0.0,

    // Final line total
    val lineTotal: Double = 0.0
)