package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["invoiceNumber"]),
        Index(value = ["financialYearStart"]),
        Index(value = ["status"])
    ]
)
data class SaleEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // BILL TO / CUSTOMER
    // =========================================================

    val customerId: Long,

    val customerName: String,

    // Invoice snapshot.
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
    // INVOICE DETAILS
    // =========================================================

    val invoiceNumber: String,

    val normalizedInvoiceNumber: String,

    val invoiceDate: String,

    val dueDate: String = "",

    val eWayNumber: String = "",

    /*
     * Kept internally for FY-wise registers and invoice
     * validation. It does not need to be shown on Sales UI.
     */
    val financialYearStart: Int,


    // =========================================================
    // PURCHASE ORDER REFERENCE
    // =========================================================

    val poNumber: String = "",

    /*
     * Optional.
     * Sales UI will use a Date Picker when PO Date is required.
     */
    val poDate: String = "",


    // =========================================================
    // GST / PLACE OF SUPPLY
    // =========================================================

    /*
     * State used for GST determination.
     */
    val placeOfSupplyState: String = "",

    /*
     * Expected values:
     *
     * INTRA_STATE
     * INTER_STATE
     */
    val gstSupplyType: String = "",


    // =========================================================
    // FINANCIAL SUMMARY
    // =========================================================

    val subTotal: Double,

    val discountAmount: Double,

    val taxableAmount: Double,

    /*
     * Total GST retained for accounting/reporting.
     *
     * INTRA_STATE:
     * gstAmount = cgstAmount + sgstAmount
     *
     * INTER_STATE:
     * gstAmount = igstAmount
     */
    val gstAmount: Double,

    val cgstAmount: Double = 0.0,

    val sgstAmount: Double = 0.0,

    val igstAmount: Double = 0.0,

    val adjustment: Double,

    val roundOff: Double,

    val totalAmount: Double,


    // =========================================================
    // REMARKS
    // =========================================================

    val remarks: String = "",


    // =========================================================
    // DOCUMENT LIFECYCLE
    // =========================================================

    val status: String = "POSTED",

    val cancelledAt: Long? = null,

    val cancellationReason: String = "",


    // =========================================================
    // AUDIT TIMESTAMPS
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)