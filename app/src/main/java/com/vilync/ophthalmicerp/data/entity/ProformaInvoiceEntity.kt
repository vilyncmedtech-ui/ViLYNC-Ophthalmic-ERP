package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proforma_invoices",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["proformaNumber"]),
        Index(value = ["normalizedProformaNumber", "financialYearStart"], unique = true),
        Index(value = ["financialYearStart"]),
        Index(value = ["status"])
    ]
)
data class ProformaInvoiceEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // CUSTOMER / BILL TO
    // =========================================================

    val customerId: Long,

    val customerName: String,

    val billToLegalName: String = "",

    val billToGstin: String = "",

    val billToAddress: String = "",

    val billToState: String = "",

    // =========================================================
    // SHIP TO
    // =========================================================

    val sameAsBillTo: Boolean = true,

    val shipToName: String = "",

    val shipToGstin: String = "",

    val shipToAddress: String = "",

    val shipToState: String = "",

    // =========================================================
    // PROFORMA DETAILS
    // =========================================================

    val proformaNumber: String,

    val normalizedProformaNumber: String,

    val proformaDate: String,

    val validUntilDate: String = "",

    val financialYearStart: Int,

    // =========================================================
    // PURCHASE ORDER REFERENCE
    // =========================================================

    val poNumber: String = "",

    val poDate: String = "",

    // =========================================================
    // GST
    // =========================================================

    val placeOfSupplyState: String = "",

    val gstSupplyType: String = "",

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

    val remarks: String = "",

    // =========================================================
    // DOCUMENT LIFECYCLE
    // =========================================================
    //
    // Proforma is non-posting:
    // it does NOT change InventoryUnitEntity.status.
    // =========================================================

    val status: String = "OPEN",

    val convertedSaleId: Long? = null,

    val convertedAt: Long? = null,

    val cancelledAt: Long? = null,

    val cancellationReason: String = "",

    // =========================================================
    // AUDIT
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)
