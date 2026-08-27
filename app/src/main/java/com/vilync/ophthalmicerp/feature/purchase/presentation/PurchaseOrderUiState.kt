package com.vilync.ophthalmicerp.feature.purchase.presentation

import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem

data class PurchaseOrderUiState(
    val isLoadingSuppliers: Boolean = false,
    val isLoadingPurchaseForEdit: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDirty: Boolean = false,
    val isSavedSuccessfully: Boolean = false,
    val errorMessage: String? = null,

    // Header State
    val supplierId: Long? = null,
    val supplierName: String = "",
    val supplierAddress: String = "",
    val supplierCity: String = "",
    val supplierDistrict: String = "",
    val supplierState: String = "",
    val supplierPinCode: String = "",
    val supplierGstin: String = "",
    val supplierCreditDays: String = "",

    val poNumber: String = "Auto-generated on Save",
    val poDate: String = "", // DD-MM-YYYY
    val reference: String = "",
    
    val isHeaderConfirmed: Boolean = false,
    val isCheckingPoDuplicate: Boolean = false,
    val isPoDuplicate: Boolean = false,
    val poDuplicateMessage: String? = null,

    // Items State
    val items: List<PurchaseItem> = emptyList(),

    // Totals
    val grossAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val adjustmentAmount: Double = 0.0,
    val roundOffAmount: Double = 0.0,
    val netAmount: Double = 0.0,

    val isEditMode: Boolean = false,
    val editingPoId: Long? = null,
    val status: String = "ORDER"
)
