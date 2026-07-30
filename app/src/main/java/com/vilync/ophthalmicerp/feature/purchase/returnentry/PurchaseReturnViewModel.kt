package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.round


class PurchaseReturnViewModel(

    private val purchaseReturnRepository: PurchaseReturnRepository,

    private val productRepository: ProductMasterRepository

) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            PurchaseReturnUiState()
        )


    val uiState: StateFlow<PurchaseReturnUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // LOAD ORIGINAL PURCHASE
    // =========================================================

    fun loadOriginalPurchase(
        purchaseId: Long
    ) {

        if (purchaseId <= 0L) {

            setError(
                "Invalid Purchase."
            )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                PurchaseReturnUiState(
                    isLoading = true
                )


            try {

                val purchase =
                    purchaseReturnRepository
                        .getOriginalPurchase(
                            purchaseId = purchaseId
                        )
                        ?: run {

                            _uiState.value =
                                PurchaseReturnUiState(
                                    errorMessage =
                                        "Purchase not found."
                                )

                            return@launch
                        }


                val purchaseItems =
                    purchaseReturnRepository
                        .getOriginalPurchaseItems(
                            purchaseId = purchaseId
                        )


                val returnItemStates =
                    purchaseItems.map { item ->


                        // =========================================
                        // PRODUCT MASTER
                        // =========================================

                        val product =
                            productRepository
                                .getProductById(
                                    productId =
                                        item.productId
                                )


                        val productName =
                            product
                                ?.productName
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Product #${item.productId}"


                        // =========================================
                        // ALREADY RETURNED QUANTITY
                        // =========================================

                        val alreadyReturned =
                            purchaseReturnRepository
                                .getAlreadyReturnedQuantity(
                                    originalPurchaseItemId =
                                        item.id
                                )


                        val remainingQuantity =
                            (
                                    item.quantity -
                                            alreadyReturned
                                    )
                                .coerceAtLeast(0)


                        // =========================================
                        // AVAILABLE PHYSICAL IOL SERIALS
                        // =========================================

                        val availableLenses =
                            purchaseReturnRepository
                                .getAvailablePurchaseLensesForReturn(
                                    purchaseItemId =
                                        item.id
                                )


                        // =========================================
                        // UI ITEM
                        // =========================================

                        PurchaseReturnItemUiState(

                            originalPurchaseItemId =
                                item.id,

                            originalPurchaseItem =
                                item,

                            productId =
                                item.productId,

                            productName =
                                productName,

                            purchasedQuantity =
                                item.quantity,

                            alreadyReturnedQuantity =
                                alreadyReturned,

                            remainingReturnableQuantity =
                                remainingQuantity,

                            returnQuantity =
                                0,

                            rate =
                                item.purchaseRate,

                            gstPercent =
                                item.gstPercent,

                            taxableAmount =
                                0.0,

                            gstAmount =
                                0.0,

                            totalAmount =
                                0.0,

                            batchNumber =
                                item.batchNumber,

                            lotNumber =
                                item.batchNumber,

                            availableLenses =
                                availableLenses,

                            selectedLenses =
                                emptyList(),

                            errorMessage =
                                null
                        )
                    }


                _uiState.value =
                    PurchaseReturnUiState(

                        editingPurchaseReturnId =
                            null,

                        isEditMode =
                            false,

                        originalPurchaseId =
                            purchase.id,

                        originalPurchase =
                            purchase,

                        originalInvoiceNumber =
                            purchase.invoiceNumber,

                        originalInvoiceDate =
                            purchase.invoiceDate,

                        supplierId =
                            purchase.supplierId,

                        supplierName =
                            purchase.supplierName,

                        creditNoteNumber =
                            "",

                        creditNoteDate =
                            "",

                        financialYearStart =
                            purchase.financialYearStart,

                        originalItems =
                            purchaseItems,

                        returnItems =
                            returnItemStates,

                        isLoading =
                            false
                    )


            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    PurchaseReturnUiState(

                        isLoading =
                            false,

                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load Purchase."
                    )
            }
        }
    }


    // =========================================================
    // LOAD EXISTING PURCHASE RETURN FOR EDIT / CORRECTION
    // =========================================================

    fun loadPurchaseReturnForEdit(
        purchaseReturnId: Long
    ) {

        if (purchaseReturnId <= 0L) {

            setError(
                "Invalid Purchase Return."
            )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                PurchaseReturnUiState(
                    editingPurchaseReturnId =
                        purchaseReturnId,
                    isEditMode =
                        true,
                    isLoading =
                        true
                )


            try {

                // =============================================
                // EXISTING RETURN HEADER
                // =============================================

                val existingReturn =
                    purchaseReturnRepository
                        .getPurchaseReturnById(
                            purchaseReturnId =
                                purchaseReturnId
                        )
                        ?: run {

                            _uiState.value =
                                PurchaseReturnUiState(
                                    editingPurchaseReturnId =
                                        purchaseReturnId,
                                    isEditMode =
                                        true,
                                    errorMessage =
                                        "Purchase Return not found."
                                )

                            return@launch
                        }


                if (
                    existingReturn.status ==
                    PurchaseReturnEntity.STATUS_CANCELLED
                ) {

                    _uiState.value =
                        PurchaseReturnUiState(
                            editingPurchaseReturnId =
                                purchaseReturnId,
                            isEditMode =
                                true,
                            errorMessage =
                                "Cancelled Purchase Return cannot be edited."
                        )

                    return@launch
                }


                // =============================================
                // ORIGINAL PURCHASE
                // =============================================

                val purchase =
                    purchaseReturnRepository
                        .getOriginalPurchase(
                            purchaseId =
                                existingReturn.originalPurchaseId
                        )
                        ?: throw IllegalStateException(
                            "Original Purchase not found."
                        )


                val purchaseItems =
                    purchaseReturnRepository
                        .getOriginalPurchaseItems(
                            purchaseId =
                                existingReturn.originalPurchaseId
                        )


                // =============================================
                // SAVED RETURN ITEMS
                // =============================================

                val savedReturnItems =
                    purchaseReturnRepository
                        .getPurchaseReturnItems(
                            purchaseReturnId =
                                purchaseReturnId
                        )
                        .first()


                val savedReturnItemByPurchaseItemId =
                    savedReturnItems.associateBy {
                        it.originalPurchaseItemId
                    }


                // =============================================
                // BUILD EDITABLE UI ITEMS
                // =============================================

                val returnItemStates =
                    purchaseItems.map { item ->

                        val product =
                            productRepository
                                .getProductById(
                                    productId =
                                        item.productId
                                )


                        val savedReturnItem =
                            savedReturnItemByPurchaseItemId[
                                item.id
                            ]


                        val productName =
                            savedReturnItem
                                ?.productName
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: product
                                    ?.productName
                                    ?.trim()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                ?: "Product #${item.productId}"


                        // =====================================
                        // OTHER RETURNS ONLY
                        // =====================================

                        val alreadyReturned =
                            purchaseReturnRepository
                                .getAlreadyReturnedQuantity(
                                    originalPurchaseItemId =
                                        item.id,
                                    excludePurchaseReturnId =
                                        purchaseReturnId
                                )


                        val remainingQuantity =
                            (
                                    item.quantity -
                                            alreadyReturned
                                    )
                                .coerceAtLeast(0)


                        // =====================================
                        // AVAILABLE LENSES, INCLUDING THIS
                        // RETURN'S OWN SAVED SERIALS
                        // =====================================

                        val availableLenses =
                            purchaseReturnRepository
                                .getAvailablePurchaseLensesForReturn(
                                    purchaseItemId =
                                        item.id,
                                    excludePurchaseReturnId =
                                        purchaseReturnId
                                )


                        val savedReturnLenses =
                            if (savedReturnItem != null) {

                                purchaseReturnRepository
                                    .getPurchaseReturnLenses(
                                        purchaseReturnItemId =
                                            savedReturnItem.id
                                    )
                                    .first()

                            } else {

                                emptyList()
                            }


                        val savedLensIds =
                            savedReturnLenses
                                .map {
                                    it.originalPurchaseLensId
                                }
                                .toSet()


                        val selectedLenses =
                            availableLenses
                                .filter {
                                    it.id in savedLensIds
                                }


                        if (
                            savedReturnLenses.isNotEmpty() &&
                            selectedLenses.size !=
                            savedReturnLenses.size
                        ) {

                            throw IllegalStateException(
                                "${productName}: One or more saved IOL serial numbers could not be loaded."
                            )
                        }


                        val returnQuantity =
                            savedReturnItem
                                ?.quantity
                                ?.coerceIn(
                                    minimumValue = 0,
                                    maximumValue =
                                        remainingQuantity
                                )
                                ?: 0


                        val baseItem =
                            PurchaseReturnItemUiState(

                                originalPurchaseItemId =
                                    item.id,

                                originalPurchaseItem =
                                    item,

                                productId =
                                    item.productId,

                                productName =
                                    productName,

                                purchasedQuantity =
                                    item.quantity,

                                alreadyReturnedQuantity =
                                    alreadyReturned,

                                remainingReturnableQuantity =
                                    remainingQuantity,

                                returnQuantity =
                                    returnQuantity,

                                rate =
                                    savedReturnItem
                                        ?.rate
                                        ?: item.purchaseRate,

                                gstPercent =
                                    savedReturnItem
                                        ?.gstPercent
                                        ?: item.gstPercent,

                                batchNumber =
                                    savedReturnItem
                                        ?.batchNumber
                                        ?: item.batchNumber,

                                lotNumber =
                                    savedReturnItem
                                        ?.lotNumber
                                        ?: item.batchNumber,

                                availableLenses =
                                    availableLenses,

                                selectedLenses =
                                    selectedLenses,

                                errorMessage =
                                    null
                            )


                        calculateItem(
                            baseItem
                        )
                    }


                // =============================================
                // EDIT STATE
                // =============================================

                _uiState.value =
                    PurchaseReturnUiState(

                        editingPurchaseReturnId =
                            existingReturn.id,

                        isEditMode =
                            true,

                        originalPurchaseId =
                            purchase.id,

                        originalPurchase =
                            purchase,

                        originalInvoiceNumber =
                            existingReturn.originalInvoiceNumber,

                        originalInvoiceDate =
                            purchase.invoiceDate,

                        supplierId =
                            existingReturn.supplierId,

                        supplierName =
                            existingReturn.supplierName,

                        creditNoteNumber =
                            existingReturn.creditNoteNumber,

                        creditNoteDate =
                            existingReturn.creditNoteDate,

                        financialYearStart =
                            existingReturn.financialYearStart,

                        originalItems =
                            purchaseItems,

                        returnItems =
                            returnItemStates,

                        discountAmount =
                            existingReturn.discountAmount,

                        roundOff =
                            existingReturn.roundOff,

                        remarks =
                            existingReturn.remarks,

                        isLoading =
                            false,

                        isSaving =
                            false,

                        isSaved =
                            false,

                        errorMessage =
                            null,

                        successMessage =
                            null
                    )


                recalculateGrandTotals()


            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    PurchaseReturnUiState(

                        editingPurchaseReturnId =
                            purchaseReturnId,

                        isEditMode =
                            true,

                        isLoading =
                            false,

                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load Purchase Return for editing."
                    )
            }
        }
    }


    // =========================================================
    // CREDIT NOTE NUMBER
    // =========================================================

    fun updateCreditNoteNumber(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(

                creditNoteNumber =
                    value,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // CREDIT NOTE DATE
    // =========================================================

    fun updateCreditNoteDate(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(

                creditNoteDate =
                    value,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // FINANCIAL YEAR
    // =========================================================

    fun updateFinancialYearStart(
        value: Int
    ) {

        _uiState.value =
            _uiState.value.copy(

                financialYearStart =
                    value,

                errorMessage =
                    null
            )
    }


    // =========================================================
    // REMARKS
    // =========================================================

    fun updateRemarks(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(

                remarks =
                    value,

                errorMessage =
                    null
            )
    }


    // =========================================================
    // DISCOUNT
    // =========================================================

    fun updateDiscountAmount(
        value: Double
    ) {

        val cleanValue =
            value.coerceAtLeast(
                0.0
            )


        _uiState.value =
            _uiState.value.copy(
                discountAmount =
                    cleanValue
            )


        recalculateGrandTotals()
    }


    // =========================================================
    // ROUND OFF
    // =========================================================

    fun updateRoundOff(
        value: Double
    ) {

        _uiState.value =
            _uiState.value.copy(
                roundOff =
                    value
            )


        recalculateGrandTotals()
    }


    // =========================================================
    // UPDATE RETURN QUANTITY
    // =========================================================

    fun updateReturnQuantity(
        originalPurchaseItemId: Long,
        quantity: Int
    ) {

        val state =
            _uiState.value


        val updatedItems =
            state.returnItems.map { item ->

                if (
                    item.originalPurchaseItemId !=
                    originalPurchaseItemId
                ) {

                    item

                } else {

                    val cleanQuantity =
                        quantity.coerceIn(
                            minimumValue = 0,
                            maximumValue =
                                item.remainingReturnableQuantity
                        )


                    // =========================================
                    // SERIAL-CONTROLLED ITEM
                    // =========================================
                    //
                    // If quantity is reduced below the number
                    // of selected serials, trim selected serials
                    // so state cannot become inconsistent.
                    // =========================================

                    val selectedLenses =
                        if (
                            item.selectedLenses.size >
                            cleanQuantity
                        ) {

                            item.selectedLenses
                                .take(
                                    cleanQuantity
                                )

                        } else {

                            item.selectedLenses
                        }


                    calculateItem(

                        item.copy(

                            returnQuantity =
                                cleanQuantity,

                            selectedLenses =
                                selectedLenses,

                            errorMessage =
                                null
                        )
                    )
                }
            }


        _uiState.value =
            state.copy(

                returnItems =
                    updatedItems,

                errorMessage =
                    null,

                successMessage =
                    null
            )


        recalculateGrandTotals()
    }


    // =========================================================
    // TOGGLE PHYSICAL IOL SERIAL
    // =========================================================

    fun toggleLensSelection(
        originalPurchaseItemId: Long,
        lens: PurchaseLensEntity
    ) {

        val state =
            _uiState.value


        val updatedItems =
            state.returnItems.map { item ->

                if (
                    item.originalPurchaseItemId !=
                    originalPurchaseItemId
                ) {

                    item

                } else {

                    // =========================================
                    // LENS MUST BELONG TO THIS PURCHASE ITEM
                    // =========================================

                    if (
                        lens.purchaseItemId !=
                        item.originalPurchaseItemId
                    ) {

                        item.copy(
                            errorMessage =
                                "Selected IOL does not belong to this Purchase Item."
                        )

                    } else {

                        val alreadySelected =
                            item.selectedLenses
                                .any {
                                    it.id == lens.id
                                }


                        val updatedSelectedLenses =
                            if (alreadySelected) {

                                item.selectedLenses
                                    .filterNot {
                                        it.id == lens.id
                                    }

                            } else {

                                if (
                                    item.selectedLenses.size >=
                                    item.remainingReturnableQuantity
                                ) {

                                    item.selectedLenses

                                } else {

                                    item.selectedLenses +
                                            lens
                                }
                            }


                        // =========================================
                        // SERIAL SELECTION DRIVES QUANTITY
                        // =========================================

                        val newQuantity =
                            updatedSelectedLenses.size


                        calculateItem(

                            item.copy(

                                selectedLenses =
                                    updatedSelectedLenses,

                                returnQuantity =
                                    newQuantity,

                                errorMessage =
                                    null
                            )
                        )
                    }
                }
            }


        _uiState.value =
            state.copy(

                returnItems =
                    updatedItems,

                errorMessage =
                    null,

                successMessage =
                    null
            )


        recalculateGrandTotals()
    }


    // =========================================================
    // CLEAR ONE RETURN ITEM
    // =========================================================

    fun clearReturnItem(
        originalPurchaseItemId: Long
    ) {

        val state =
            _uiState.value


        val updatedItems =
            state.returnItems.map { item ->

                if (
                    item.originalPurchaseItemId ==
                    originalPurchaseItemId
                ) {

                    calculateItem(

                        item.copy(

                            returnQuantity =
                                0,

                            selectedLenses =
                                emptyList(),

                            errorMessage =
                                null
                        )
                    )

                } else {

                    item
                }
            }


        _uiState.value =
            state.copy(
                returnItems =
                    updatedItems,
                errorMessage =
                    null
            )


        recalculateGrandTotals()
    }


    // =========================================================
    // ITEM CALCULATION
    // =========================================================

    private fun calculateItem(
        item: PurchaseReturnItemUiState
    ): PurchaseReturnItemUiState {

        if (item.returnQuantity <= 0) {

            return item.copy(
                taxableAmount =
                    0.0,
                gstAmount =
                    0.0,
                totalAmount =
                    0.0
            )
        }


        val originalItem =
            item.originalPurchaseItem


        // =====================================================
        // USE ORIGINAL PURCHASE COMMERCIAL PROPORTION
        // =====================================================
        //
        // This preserves the original line discount economics.
        //
        // Example:
        //
        // Original line taxable amount / original quantity
        // = taxable amount per returned unit.
        //
        // =====================================================

        val taxablePerUnit =
            if (
                originalItem != null &&
                originalItem.quantity > 0
            ) {

                originalItem.taxableAmount /
                        originalItem.quantity

            } else {

                item.rate
            }


        val taxableAmount =
            roundMoney(
                taxablePerUnit *
                        item.returnQuantity
            )


        val gstAmount =
            roundMoney(
                taxableAmount *
                        item.gstPercent /
                        100.0
            )


        val totalAmount =
            roundMoney(
                taxableAmount +
                        gstAmount
            )


        return item.copy(

            taxableAmount =
                taxableAmount,

            gstAmount =
                gstAmount,

            totalAmount =
                totalAmount
        )
    }


    // =========================================================
    // RECALCULATE GRAND TOTALS
    // =========================================================

    private fun recalculateGrandTotals() {

        val state =
            _uiState.value


        val selectedItems =
            state.returnItems
                .filter {
                    it.returnQuantity > 0
                }


        val subtotal =
            roundMoney(
                selectedItems.sumOf {
                    it.taxableAmount
                }
            )


        val gstAmount =
            roundMoney(
                selectedItems.sumOf {
                    it.gstAmount
                }
            )


        val totalAmount =
            roundMoney(
                subtotal +
                        gstAmount -
                        state.discountAmount +
                        state.roundOff
            )


        _uiState.value =
            state.copy(

                subTotal =
                    subtotal,

                gstAmount =
                    gstAmount,

                totalAmount =
                    totalAmount
            )
    }


    // =========================================================
    // VALIDATE PURCHASE RETURN
    // =========================================================

    private suspend fun validateBeforeSave():
            PurchaseReturnValidationResult {

        val state =
            _uiState.value


        // =====================================================
        // ORIGINAL PURCHASE
        // =====================================================

        val purchaseId =
            state.originalPurchaseId


        if (
            purchaseId == null ||
            purchaseId <= 0L
        ) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Please select the original Purchase."
            )
        }


        // =====================================================
        // SUPPLIER
        // =====================================================

        val supplierId =
            state.supplierId


        if (
            supplierId == null ||
            supplierId <= 0L
        ) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Valid Supplier is required."
            )
        }


        // =====================================================
        // CREDIT NOTE NUMBER
        // =====================================================

        val creditNoteNumber =
            state.creditNoteNumber.trim()


        if (creditNoteNumber.isBlank()) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Credit Note Number is required."
            )
        }


        // =====================================================
        // CREDIT NOTE DATE
        // =====================================================

        if (
            state.creditNoteDate
                .trim()
                .isBlank()
        ) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Credit Note Date is required."
            )
        }


        // =====================================================
        // FINANCIAL YEAR
        // =====================================================

        if (
            state.financialYearStart <= 0
        ) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Valid Financial Year is required."
            )
        }


        // =====================================================
        // DUPLICATE CREDIT NOTE
        // =====================================================

        val duplicateCreditNote =
            if (
                state.isEditMode &&
                state.editingPurchaseReturnId != null
            ) {

                purchaseReturnRepository
                    .creditNoteExists(

                        supplierId =
                            supplierId,

                        creditNoteNumber =
                            creditNoteNumber,

                        excludePurchaseReturnId =
                            state.editingPurchaseReturnId
                    )

            } else {

                purchaseReturnRepository
                    .creditNoteExists(

                        supplierId =
                            supplierId,

                        creditNoteNumber =
                            creditNoteNumber
                    )
            }


        if (duplicateCreditNote) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "This Credit Note Number already exists for the selected Supplier."
            )
        }


        // =====================================================
        // SELECTED RETURN ITEMS
        // =====================================================

        val selectedItems =
            state.returnItems
                .filter {
                    it.returnQuantity > 0
                }


        if (selectedItems.isEmpty()) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Select at least one item to return."
            )
        }


        // =====================================================
        // ITEM VALIDATION
        // =====================================================

        selectedItems.forEach { item ->


            if (
                item.returnQuantity >
                item.remainingReturnableQuantity
            ) {

                return PurchaseReturnValidationResult(
                    isValid = false,
                    message =
                        "${item.productName}: Return quantity exceeds available quantity."
                )
            }


            // ================================================
            // RECHECK DATABASE QUANTITY
            // ================================================
            //
            // Important because another return could have been
            // saved after this screen was loaded.
            // ================================================

            val latestRemainingQuantity =
                if (
                    state.isEditMode &&
                    state.editingPurchaseReturnId != null
                ) {

                    purchaseReturnRepository
                        .getRemainingReturnableQuantity(

                            originalPurchaseItemId =
                                item.originalPurchaseItemId,

                            originalPurchasedQuantity =
                                item.purchasedQuantity,

                            excludePurchaseReturnId =
                                state.editingPurchaseReturnId
                        )

                } else {

                    purchaseReturnRepository
                        .getRemainingReturnableQuantity(

                            originalPurchaseItemId =
                                item.originalPurchaseItemId,

                            originalPurchasedQuantity =
                                item.purchasedQuantity
                        )
                }


            if (
                item.returnQuantity >
                latestRemainingQuantity
            ) {

                return PurchaseReturnValidationResult(
                    isValid = false,
                    message =
                        "${item.productName}: Only $latestRemainingQuantity unit(s) are still returnable."
                )
            }


            // ================================================
            // SERIAL-CONTROLLED IOL
            // ================================================

            if (item.isSerialControlled) {

                if (
                    item.selectedLenses.size !=
                    item.returnQuantity
                ) {

                    return PurchaseReturnValidationResult(
                        isValid = false,
                        message =
                            "${item.productName}: Select exactly ${item.returnQuantity} IOL serial number(s)."
                    )
                }


                item.selectedLenses.forEach { lens ->

                    val alreadyReturned =
                        if (
                            state.isEditMode &&
                            state.editingPurchaseReturnId != null
                        ) {

                            purchaseReturnRepository
                                .isPurchaseLensAlreadyReturned(

                                    originalPurchaseLensId =
                                        lens.id,

                                    excludePurchaseReturnId =
                                        state.editingPurchaseReturnId
                                )

                        } else {

                            purchaseReturnRepository
                                .isPurchaseLensAlreadyReturned(
                                    originalPurchaseLensId =
                                        lens.id
                                )
                        }


                    if (alreadyReturned) {

                        return PurchaseReturnValidationResult(
                            isValid = false,
                            message =
                                "IOL Serial ${lens.serialNumber} has already been returned."
                        )
                    }
                }
            }
        }


        // =====================================================
        // TOTAL
        // =====================================================

        if (
            state.totalAmount < 0.0
        ) {

            return PurchaseReturnValidationResult(
                isValid = false,
                message =
                    "Purchase Return total cannot be negative."
            )
        }


        return PurchaseReturnValidationResult(
            isValid = true
        )
    }


    // =========================================================
    // SAVE PURCHASE RETURN
    // =========================================================

    fun savePurchaseReturn() {

        val currentState =
            _uiState.value


        if (
            currentState.isSaving ||
            currentState.isSaved
        ) {
            return
        }


        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(

                    isSaving =
                        true,

                    errorMessage =
                        null,

                    successMessage =
                        null
                )


            try {

                // =============================================
                // VALIDATION
                // =============================================

                val validation =
                    validateBeforeSave()


                if (!validation.isValid) {

                    _uiState.value =
                        _uiState.value.copy(

                            isSaving =
                                false,

                            errorMessage =
                                validation.message
                                    ?: "Unable to save Purchase Return."
                        )

                    return@launch
                }


                val state =
                    _uiState.value


                val purchaseId =
                    requireNotNull(
                        state.originalPurchaseId
                    )


                val supplierId =
                    requireNotNull(
                        state.supplierId
                    )


                // =============================================
                // HEADER
                // =============================================

                val purchaseReturn =
                    if (
                        state.isEditMode &&
                        state.editingPurchaseReturnId != null
                    ) {

                        val existingReturn =
                            purchaseReturnRepository
                                .getPurchaseReturnById(
                                    purchaseReturnId =
                                        state.editingPurchaseReturnId
                                )
                                ?: throw IllegalStateException(
                                    "Purchase Return not found."
                                )


                        if (
                            existingReturn.status ==
                            PurchaseReturnEntity.STATUS_CANCELLED
                        ) {

                            throw IllegalStateException(
                                "Cancelled Purchase Return cannot be edited."
                            )
                        }


                        existingReturn.copy(

                            originalPurchaseId =
                                purchaseId,

                            supplierId =
                                supplierId,

                            supplierName =
                                state.supplierName.trim(),

                            originalInvoiceNumber =
                                state.originalInvoiceNumber.trim(),

                            creditNoteNumber =
                                state.creditNoteNumber
                                    .trim()
                                    .uppercase(),

                            creditNoteDate =
                                state.creditNoteDate.trim(),

                            financialYearStart =
                                state.financialYearStart,

                            subTotal =
                                state.subTotal,

                            gstAmount =
                                state.gstAmount,

                            discountAmount =
                                state.discountAmount,

                            roundOff =
                                state.roundOff,

                            totalAmount =
                                state.totalAmount,

                            remarks =
                                state.remarks.trim(),

                            updatedAt =
                                System.currentTimeMillis()
                        )

                    } else {

                        PurchaseReturnEntity(

                            originalPurchaseId =
                                purchaseId,

                            supplierId =
                                supplierId,

                            supplierName =
                                state.supplierName.trim(),

                            originalInvoiceNumber =
                                state.originalInvoiceNumber.trim(),

                            creditNoteNumber =
                                state.creditNoteNumber
                                    .trim()
                                    .uppercase(),

                            creditNoteDate =
                                state.creditNoteDate.trim(),

                            financialYearStart =
                                state.financialYearStart,

                            subTotal =
                                state.subTotal,

                            gstAmount =
                                state.gstAmount,

                            discountAmount =
                                state.discountAmount,

                            roundOff =
                                state.roundOff,

                            totalAmount =
                                state.totalAmount,

                            remarks =
                                state.remarks.trim()
                        )
                    }


                // =============================================
                // ITEMS + PHYSICAL IOL SERIALS
                // =============================================

                val itemsWithLenses =
                    state.returnItems

                        .filter {
                            it.returnQuantity > 0
                        }

                        .map { item ->


                            val returnItem =
                                PurchaseReturnItemEntity(

                                    purchaseReturnId =
                                        0L,

                                    originalPurchaseItemId =
                                        item.originalPurchaseItemId,

                                    productId =
                                        item.productId,

                                    productName =
                                        item.productName,

                                    quantity =
                                        item.returnQuantity,

                                    rate =
                                        item.rate,

                                    gstPercent =
                                        item.gstPercent,

                                    taxableAmount =
                                        item.taxableAmount,

                                    gstAmount =
                                        item.gstAmount,

                                    totalAmount =
                                        item.totalAmount,

                                    batchNumber =
                                        item.batchNumber,

                                    lotNumber =
                                        item.lotNumber
                                )


                            val returnLenses =
                                item.selectedLenses
                                    .map { lens ->

                                        PurchaseReturnLensEntity(

                                            purchaseReturnItemId =
                                                0L,

                                            originalPurchaseLensId =
                                                lens.id,

                                            serialNumber =
                                                lens.serialNumber,

                                            expiryDate =
                                                lens.expiryDate
                                        )
                                    }


                            returnItem to
                                    returnLenses
                        }


                // =============================================
                // DATABASE TRANSACTION
                // =============================================

                val savedPurchaseReturnId =
                    if (
                        state.isEditMode &&
                        state.editingPurchaseReturnId != null
                    ) {

                        purchaseReturnRepository
                            .updateCompletePurchaseReturn(

                                purchaseReturn =
                                    purchaseReturn,

                                itemsWithLenses =
                                    itemsWithLenses
                            )


                        state.editingPurchaseReturnId

                    } else {

                        purchaseReturnRepository
                            .saveCompletePurchaseReturn(

                                purchaseReturn =
                                    purchaseReturn,

                                itemsWithLenses =
                                    itemsWithLenses
                            )
                    }


                if (
                    savedPurchaseReturnId <= 0L
                ) {

                    throw IllegalStateException(
                        if (state.isEditMode) {
                            "Purchase Return could not be updated."
                        } else {
                            "Purchase Return could not be saved."
                        }
                    )
                }


                // =============================================
                // SUCCESS
                // =============================================

                _uiState.value =
                    _uiState.value.copy(

                        isSaving =
                            false,

                        isSaved =
                            true,

                        errorMessage =
                            null,

                        successMessage =
                            if (state.isEditMode) {
                                "Purchase Return updated successfully."
                            } else {
                                "Purchase Return saved successfully."
                            }
                    )


            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    _uiState.value.copy(

                        isSaving =
                            false,

                        isSaved =
                            false,

                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: if (_uiState.value.isEditMode) {
                                    "Unable to update Purchase Return."
                                } else {
                                    "Unable to save Purchase Return."
                                }
                    )
            }
        }
    }


    // =========================================================
    // SAVE SUCCESS CONSUMED
    // =========================================================

    fun consumeSaveSuccess() {

        _uiState.value =
            _uiState.value.copy(

                isSaved =
                    false,

                successMessage =
                    null
            )
    }


    // =========================================================
    // CLEAR ERROR
    // =========================================================

    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage =
                    null
            )
    }


    // =========================================================
    // SET ERROR
    // =========================================================

    private fun setError(
        message: String
    ) {

        _uiState.value =
            _uiState.value.copy(

                isLoading =
                    false,

                isSaving =
                    false,

                errorMessage =
                    message
            )
    }


    // =========================================================
    // MONEY ROUNDING
    // =========================================================

    private fun roundMoney(
        value: Double
    ): Double {

        return round(
            value * 100.0
        ) / 100.0
    }
}