package com.vilync.ophthalmicerp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",

    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // SALE REFERENCE
    // =========================================================

    val saleId: Long,

    // =========================================================
    // PRODUCT
    // =========================================================

    // Stable Product Master reference.
    val productId: Long,

    // Product name snapshot preserved for invoice history.
    val productName: String,

    // Historical HSN snapshot preserved with the sales line.
    @ColumnInfo(defaultValue = "''")
    val hsnCode: String = "",

    // IOL power.
    // Blank for products where power is not applicable.
    val power: String = "",

    // =========================================================
    // QUANTITY
    // =========================================================

    val quantity: Int,

    // =========================================================
    // PRICING
    // =========================================================

    // Selling rate before discount / GST.
    val rate: Double,

    // Discount percentage applied to this item.
    val discountPercent: Double,

    // Calculated discount amount.
    val discountAmount: Double,

    // Amount after discount and before GST.
    val taxableAmount: Double,

    // GST percentage applicable to this item.
    val gstPercent: Double,

    // Calculated GST amount.
    val gstAmount: Double,

    // Final item total including GST.
    val totalAmount: Double,

    // =========================================================
    // TRACEABILITY
    // =========================================================

    // Batch / lot snapshot where applicable.
    val batchNumber: String = "",

    val lotNumber: String = ""
)