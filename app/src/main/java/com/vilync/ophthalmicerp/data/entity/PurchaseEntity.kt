package com.vilync.ophthalmicerp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["invoiceNumber"]),
        Index(value = ["supplierName"]),
        Index(value = ["invoiceDate"]),
        Index(value = ["supplierId"]),

        // Fast Financial Year filtering.
        Index(value = ["financialYearStart"]),

        Index(
            value = [
                "supplierId",
                "normalizedInvoiceNumber"
            ],
            unique = true
        ),
        Index(value = ["status"])
    ]
)
data class PurchaseEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // =========================================================
    // SUPPLIER
    // =========================================================

    // Stable Party Master reference.
    //
    // New purchases MUST always have supplierId > 0.
    // 0L is retained only for old/unmatched legacy rows.
    val supplierId: Long = 0L,

    // Supplier / Vendor display-name snapshot.
    val supplierName: String,

    // GST identity snapshot preserved with the purchase document.
    @ColumnInfo(defaultValue = "''")
    val supplierGstin: String = "",

    // Supplier state snapshot used for GST supply determination.
    @ColumnInfo(defaultValue = "''")
    val supplierState: String = "",

    // Expected values for new/updated v20 transactions:
    // INTRA_STATE / INTER_STATE
    @ColumnInfo(defaultValue = "''")
    val gstSupplyType: String = "",


    // =========================================================
    // INVOICE
    // =========================================================

    // Original supplier invoice number shown to the user.
    //
    // New save flow stores this trimmed + uppercase.
    val invoiceNumber: String,

    // Canonical duplicate-check key.
    //
    // Business rule:
    //
    // supplierId + normalizedInvoiceNumber = UNIQUE
    //
    // New save flow stores:
    //
    // invoiceNumber.trim().uppercase()
    //
    // Legacy duplicate/unmatched rows may contain an internal
    // migration-only marker so historical records are preserved.
    val normalizedInvoiceNumber: String,

    // Date printed on supplier invoice.
    val invoiceDate: String,

    // Date on which goods were actually received.
    val receivedDate: String,


    // =========================================================
    // FINANCIAL YEAR
    // =========================================================

    /**
     * Indian Financial Year start year.
     *
     * This is a lightweight database key used for fast
     * Financial Year filtering.
     *
     * Examples:
     *
     * Invoice Date: 31-03-2026
     * financialYearStart = 2025
     * Display FY = 2025-26
     *
     * Invoice Date: 01-04-2026
     * financialYearStart = 2026
     * Display FY = 2026-27
     *
     * IMPORTANT:
     *
     * This value must be derived from the actual invoice /
     * accounting transaction date.
     *
     * It must NOT simply copy whichever Working FY happens
     * to be selected in Settings.
     *
     * This ensures historical corrections remain
     * accounting-correct.
     */
    val financialYearStart: Int = 0,


    // =========================================================
    // PURCHASE TYPE / PAYMENT
    // =========================================================

    // Example:
    // Invoice / Challan
    val purchaseType: String = "Invoice",

    // Example:
    // Credit / Cash / Bank
    val paymentType: String = "Credit",

    // Example:
    // 30 / 45 etc.
    val creditDays: Int = 0,

    // Optional reference or remarks.
    val reference: String = "",


    // =========================================================
    // FINANCIAL VALUES
    // =========================================================

    val subtotal: Double = 0.0,

    val discountAmount: Double = 0.0,

    val taxableAmount: Double = 0.0,

    val cgstAmount: Double = 0.0,

    val sgstAmount: Double = 0.0,

    val igstAmount: Double = 0.0,

    val grandTotal: Double = 0.0,

    @ColumnInfo(defaultValue = "'POSTED'")
    val status: String = "POSTED"
)