package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_credit_note_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesCreditNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalSaleItemId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["creditNoteId"]),
        Index(value = ["originalSaleItemId"]),
        Index(value = ["productId"])
    ]
)
data class SalesCreditNoteItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val creditNoteId: Long,

    // Original invoice item when this credit is invoice-linked.
    // Nullable to support legitimate financial adjustments.
    val originalSaleItemId: Long? = null,

    // =========================================================
    // PRODUCT SNAPSHOT
    // =========================================================

    val productId: Long,

    val productName: String,

    val power: String = "",

    val quantity: Int,

    // =========================================================
    // PRICING
    // =========================================================

    val rate: Double,

    val discountPercent: Double = 0.0,

    val discountAmount: Double = 0.0,

    val taxableAmount: Double = 0.0,

    val gstPercent: Double = 0.0,

    val gstAmount: Double = 0.0,

    val totalAmount: Double = 0.0,

    // =========================================================
    // TRACEABILITY
    // =========================================================

    val batchNumber: String = "",

    val lotNumber: String = ""
)
