package com.vilync.ophthalmicerp.feature.purchase.presentation

import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem

data class PurchaseUiState(
    val isEditMode: Boolean = false,
    val editingPurchaseId: Long? = null,
    val isLoadingPurchaseForEdit: Boolean = false,

    val supplierId: Long? = null,
    val supplierName: String = "",
    val supplierAddress: String = "",
    val supplierCity: String = "",
    val supplierDistrict: String = "",
    val supplierState: String = "",
    val supplierPinCode: String = "",
    val supplierGstin: String = "",
    val supplierCreditDays: String = "",

    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val receivedDate: String = "",
    val purchaseType: String = "Invoice",
    val paymentType: String = "Credit",
    val creditDays: String = "",
    val reference: String = "",

    val isCheckingInvoiceDuplicate: Boolean = false,
    val isInvoiceDuplicate: Boolean = false,
    val invoiceDuplicateMessage: String? = null,

    val items: List<PurchaseItem> = emptyList(),

    val grossAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val adjustmentAmount: Double = 0.0,
    val roundOffAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,

    val isLoadingSuppliers: Boolean = false,
    val isHeaderConfirmed: Boolean = false,
    val isSaving: Boolean = false,
    val status: String = "POSTED",

    /*
     * True once the current version has been saved. It is reset as soon
     * as the user changes purchase data or its items.
     */
    val isSaved: Boolean = false,

    /*
     * Used by navigation to decide whether an exit confirmation is needed.
     */
    val isDirty: Boolean = false,

    /*
     * One-time event for a success message. Do not use this for button state.
     */
    val isSavedSuccessfully: Boolean = false,

    val errorMessage: String? = null
)

