package com.vilync.ophthalmicerp.feature.purchase.returnentry

import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity


// =========================================================
// PURCHASE RETURN UI STATE
// =========================================================

data class PurchaseReturnUiState(

    // =====================================================
    // EDIT / CORRECTION MODE
    // =====================================================

    /**
     * Existing Purchase Return ID when this screen is opened
     * for correction.
     *
     * null = New Purchase Return
     * > 0  = Edit / Correct existing Purchase Return
     */
    val editingPurchaseReturnId: Long? = null,

    /**
     * True only while correcting an already-saved Purchase Return.
     */
    val isEditMode: Boolean = false,


    // =====================================================
    // ORIGINAL PURCHASE
    // =====================================================

    val originalPurchaseId: Long? = null,

    val originalPurchase: PurchaseEntity? = null,

    val originalInvoiceNumber: String = "",

    val originalInvoiceDate: String = "",


    // =====================================================
    // SUPPLIER
    // =====================================================

    val supplierId: Long? = null,

    val supplierName: String = "",


    // =====================================================
    // SUPPLIER CREDIT NOTE / RETURN DOCUMENT
    // =====================================================

    val creditNoteNumber: String = "",

    val creditNoteDate: String = "",


    // =====================================================
    // FINANCIAL YEAR
    // =====================================================

    /**
     * Example:
     *
     * FY 2026-27 = 2026
     */
    val financialYearStart: Int = 0,


    // =====================================================
    // ORIGINAL PURCHASE ITEMS
    // =====================================================

    val originalItems: List<PurchaseItemEntity> =
        emptyList(),


    // =====================================================
    // RETURN ITEMS
    // =====================================================

    val returnItems: List<PurchaseReturnItemUiState> =
        emptyList(),


    // =====================================================
    // FINANCIAL SUMMARY
    // =====================================================

    val subTotal: Double = 0.0,

    val gstAmount: Double = 0.0,

    val discountAmount: Double = 0.0,

    val roundOff: Double = 0.0,

    val totalAmount: Double = 0.0,


    // =====================================================
    // REMARKS
    // =====================================================

    val remarks: String = "",


    // =====================================================
    // SCREEN STATE
    // =====================================================

    val isLoading: Boolean = false,

    val isSaving: Boolean = false,

    val isSaved: Boolean = false,


    // =====================================================
    // VALIDATION / ERROR
    // =====================================================

    val errorMessage: String? = null,

    val successMessage: String? = null
)


// =========================================================
// PURCHASE RETURN ITEM UI STATE
// =========================================================

data class PurchaseReturnItemUiState(

    // =====================================================
    // ORIGINAL PURCHASE ITEM
    // =====================================================

    val originalPurchaseItemId: Long,

    val originalPurchaseItem: PurchaseItemEntity? = null,


    // =====================================================
    // PRODUCT
    // =====================================================

    val productId: Long,

    val productName: String,


    // =====================================================
    // ORIGINAL PURCHASE QUANTITY
    // =====================================================

    val purchasedQuantity: Int,


    // =====================================================
    // PREVIOUSLY RETURNED QUANTITY
    // =====================================================

    /**
     * Quantity returned through OTHER active Purchase Returns.
     *
     * In edit mode, the Purchase Return currently being corrected
     * is excluded from this number.
     */
    val alreadyReturnedQuantity: Int = 0,


    // =====================================================
    // MAXIMUM QUANTITY STILL RETURNABLE
    // =====================================================

    /**
     * In edit mode this includes the quantity belonging to the
     * current Purchase Return, because that return is excluded
     * from alreadyReturnedQuantity.
     */
    val remainingReturnableQuantity: Int =
        purchasedQuantity,


    // =====================================================
    // CURRENT RETURN QUANTITY
    // =====================================================

    val returnQuantity: Int = 0,


    // =====================================================
    // COMMERCIAL VALUES
    // =====================================================

    val rate: Double,

    val gstPercent: Double,


    // =====================================================
    // CALCULATED VALUES
    // =====================================================

    val taxableAmount: Double = 0.0,

    val gstAmount: Double = 0.0,

    val totalAmount: Double = 0.0,


    // =====================================================
    // BATCH / LOT
    // =====================================================

    val batchNumber: String = "",

    val lotNumber: String = "",


    // =====================================================
    // PHYSICAL IOL INFORMATION
    // =====================================================

    /**
     * All physical lenses from the original Purchase Item
     * which are eligible for this Purchase Return.
     *
     * In edit mode this also includes lenses already selected
     * by the Purchase Return currently being corrected.
     */
    val availableLenses: List<PurchaseLensEntity> =
        emptyList(),

    /**
     * Exact physical IOLs selected by the user for return.
     *
     * In edit mode this is pre-filled from the saved return.
     */
    val selectedLenses: List<PurchaseLensEntity> =
        emptyList(),


    // =====================================================
    // ITEM VALIDATION
    // =====================================================

    val errorMessage: String? = null
) {


    // =====================================================
    // DERIVED VALUES
    // =====================================================

    /**
     * True when the Purchase Item contains physical
     * serial-controlled IOL lenses.
     */
    val isSerialControlled: Boolean
        get() =
            availableLenses.isNotEmpty() ||
                    selectedLenses.isNotEmpty()


    /**
     * Number of physical IOLs currently selected.
     */
    val selectedLensCount: Int
        get() =
            selectedLenses.size


    /**
     * True when this item is actually part of
     * the current Purchase Return.
     */
    val isSelectedForReturn: Boolean
        get() =
            returnQuantity > 0


    /**
     * Quantity which can still be returned.
     */
    val canReturnMore: Boolean
        get() =
            remainingReturnableQuantity > 0
}


// =========================================================
// PURCHASE RETURN VALIDATION RESULT
// =========================================================

data class PurchaseReturnValidationResult(

    val isValid: Boolean,

    val message: String? = null
)
