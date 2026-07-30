package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_returns",
    indices = [
        Index(value = ["originalPurchaseId"]),
        Index(value = ["supplierId"]),
        Index(value = ["creditNoteNumber"]),
        Index(value = ["financialYearStart"]),
        Index(value = ["status"]),
        Index(value = ["reversedByPurchaseReturnId"])
    ]
)
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val originalPurchaseId: Long,
    val supplierId: Long,
    val supplierName: String,
    val originalInvoiceNumber: String,

    // Internal legacy field name retained for database compatibility.
    // User-facing label in the ERP is: Debit Note Number.
    val creditNoteNumber: String,
    val creditNoteDate: String,

    val financialYearStart: Int,
    val subTotal: Double,
    val gstAmount: Double,
    val discountAmount: Double = 0.0,
    val roundOff: Double = 0.0,
    val totalAmount: Double,
    val remarks: String = "",

    // DRAFT can be edited. POSTED is accounting-final. CANCELLED is retained
    // for audit history and its IOL serials become returnable again.
    val status: String = STATUS_POSTED,
    val cancelledAt: Long? = null,
    val cancellationReason: String = "",
    val reversedByPurchaseReturnId: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_POSTED = "POSTED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
