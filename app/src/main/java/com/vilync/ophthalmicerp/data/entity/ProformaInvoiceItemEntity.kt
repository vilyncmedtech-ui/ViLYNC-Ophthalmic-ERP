package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proforma_invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = ProformaInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["proformaInvoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["proformaInvoiceId"]),
        Index(value = ["productId"])
    ]
)
data class ProformaInvoiceItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val proformaInvoiceId: Long,

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

    val batchNumber: String = "",

    val lotNumber: String = ""
)
