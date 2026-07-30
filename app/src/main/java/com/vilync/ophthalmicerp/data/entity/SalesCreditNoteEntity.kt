package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_credit_notes",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["creditNoteNumber"]),
        Index(value = ["normalizedCreditNoteNumber", "financialYearStart"], unique = true),
        Index(value = ["financialYearStart"]),
        Index(value = ["originalSaleId"]),
        Index(value = ["status"])
    ]
)
data class SalesCreditNoteEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // CUSTOMER / BILL TO SNAPSHOT
    // =========================================================

    val customerId: Long,

    val customerName: String,

    val billToLegalName: String = "",

    val billToGstin: String = "",

    val billToAddress: String = "",

    val billToState: String = "",

    // =========================================================
    // CREDIT NOTE DETAILS
    // =========================================================

    val creditNoteNumber: String,

    val normalizedCreditNoteNumber: String,

    val creditNoteDate: String,

    val financialYearStart: Int,

    // =========================================================
    // ORIGINAL SALES INVOICE REFERENCE
    // =========================================================

    val originalSaleId: Long? = null,

    val originalInvoiceNumber: String = "",

    val originalInvoiceDate: String = "",

    // =========================================================
    // GST / PLACE OF SUPPLY
    // =========================================================

    val placeOfSupplyState: String = "",

    val gstSupplyType: String = "",

    // =========================================================
    // CREDIT NOTE TYPE
    // =========================================================
    //
    // Expected:
    // SALES_RETURN
    // FINANCIAL_ADJUSTMENT
    //
    // SALES_RETURN may return physical inventory.
    // FINANCIAL_ADJUSTMENT must not automatically alter stock.
    // =========================================================

    val creditNoteType: String = "SALES_RETURN",

    // =========================================================
    // FINANCIAL SUMMARY
    // =========================================================

    val subTotal: Double = 0.0,

    val discountAmount: Double = 0.0,

    val taxableAmount: Double = 0.0,

    val gstAmount: Double = 0.0,

    val cgstAmount: Double = 0.0,

    val sgstAmount: Double = 0.0,

    val igstAmount: Double = 0.0,

    val adjustment: Double = 0.0,

    val roundOff: Double = 0.0,

    val totalAmount: Double = 0.0,

    // =========================================================
    // REMARKS / REASON
    // =========================================================

    val reason: String = "",

    val remarks: String = "",

    // =========================================================
    // DOCUMENT LIFECYCLE
    // =========================================================

    val status: String = "POSTED",

    val cancelledAt: Long? = null,

    val cancellationReason: String = "",

    // =========================================================
    // AUDIT
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)
