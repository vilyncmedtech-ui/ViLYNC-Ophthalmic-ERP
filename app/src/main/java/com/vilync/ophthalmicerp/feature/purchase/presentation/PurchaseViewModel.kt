package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
import com.vilync.ophthalmicerp.feature.purchase.model.IolLensDetail
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PurchaseViewModel(
    private val partyRepository: PartyRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductMasterRepository? = null,
    private val auditTrailRepository: AuditTrailRepository
) : ViewModel() {


    // =========================================================
    // PURCHASE UI STATE
    // =========================================================

    private var invoiceDuplicateCheckJob: Job? = null

    private val _uiState =
        MutableStateFlow(
            PurchaseUiState()
        )

    val uiState: StateFlow<PurchaseUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // VENDOR LIST
    // =========================================================

    private val _vendors =
        MutableStateFlow<List<PartyMaster>>(
            emptyList()
        )

    val vendors: StateFlow<List<PartyMaster>> =
        _vendors.asStateFlow()


    init {

        loadVendors()
    }


    // =========================================================
    // LOAD VENDORS FROM PARTY MASTER
    // =========================================================

    private fun loadVendors() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoadingSuppliers = true
                )

            try {

                partyRepository
                    .getAllActiveParties()
                    .collect { parties ->

                        val vendorParties =
                            parties.filter { party ->

                                isPurchaseVendor(
                                    party.partyType
                                )
                            }

                        _vendors.value =
                            vendorParties.sortedBy {
                                it.partyName.lowercase()
                            }

                        _uiState.value =
                            _uiState.value.copy(
                                isLoadingSuppliers = false
                            )
                    }

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingSuppliers = false,
                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load vendors."
                    )
            }
        }
    }


    // =========================================================
    // CHECK WHETHER PARTY CAN BE USED AS VENDOR
    // =========================================================

    private fun isPurchaseVendor(
        partyType: PartyType
    ): Boolean {

        /*
         * We deliberately use displayName here instead of
         * assuming the exact enum constant used for the
         * Customer & Vendor option.
         */

        val typeName =
            partyType.displayName
                .trim()
                .lowercase()

        return typeName == "vendor" ||
                (
                        typeName.contains("customer") &&
                                typeName.contains("vendor")
                        )
    }


    // =========================================================
    // SELECT VENDOR
    // =========================================================

    fun selectVendor(
        party: PartyMaster
    ) {

        if (

            !isPurchaseVendor(
                party.partyType
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Please select a Vendor or Customer & Vendor party."
                )

            return
        }


        val fullAddress =
            buildSupplierAddress(
                party = party
            )


        val creditDays =
            party.creditDays
                .toString()
                .takeIf {
                    it != "0"
                }
                .orEmpty()


        _uiState.value =
            _uiState.value.copy(

                supplierId =
                    party.id,

                supplierName =
                    party.partyName,

                supplierAddress =
                    fullAddress,

                supplierCity =
                    party.city,

                supplierDistrict =
                    party.district,

                supplierState =
                    party.state,

                supplierPinCode =
                    party.pinCode,

                supplierGstin =
                    party.gstin,

                supplierCreditDays =
                    creditDays,

                creditDays =
                    creditDays,

                errorMessage =
                    null
            )

        checkInvoiceDuplicateDebounced()


        markPurchaseChanged()
    }


    // =========================================================
    // CLEAR SELECTED VENDOR
    // =========================================================

    fun clearVendor() {

        _uiState.value =
            _uiState.value.copy(

                supplierId = null,

                supplierName = "",

                supplierAddress = "",

                supplierCity = "",

                supplierDistrict = "",

                supplierState = "",

                supplierPinCode = "",

                supplierGstin = "",

                supplierCreditDays = "",

                creditDays = "",

                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // SUPPLIER NAME
    // =========================================================
    //
    // Retained for compatibility with the existing screen.
    // Once the searchable Vendor selector is connected, the
    // supplier should normally be selected from Party Master.
    // =========================================================

    fun updateSupplierName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                supplierName = value,
                supplierId = null,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // INVOICE NUMBER
    // =========================================================

    fun updateInvoiceNumber(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                invoiceNumber = value,
                isInvoiceDuplicate = false,
                isCheckingInvoiceDuplicate = false,
                invoiceDuplicateMessage = null,
                errorMessage = null,
                isSavedSuccessfully = false
            )

        checkInvoiceDuplicateDebounced()


        markPurchaseChanged()
    }


    // =========================================================
    // INVOICE DATE
    // =========================================================

    fun updateInvoiceDate(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                invoiceDate = value,

                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // RECEIVED DATE
    // =========================================================

    fun updateReceivedDate(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                receivedDate = value,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // PURCHASE TYPE
    // =========================================================

    fun updatePurchaseType(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                purchaseType = value,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // PAYMENT TYPE
    // =========================================================

    fun updatePaymentType(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                paymentType = value,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // CREDIT DAYS
    // =========================================================

    fun updateCreditDays(
        value: String
    ) {

        val cleanValue =
            value.filter {
                it.isDigit()
            }

        _uiState.value =
            _uiState.value.copy(
                creditDays = cleanValue,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // REFERENCE / REMARKS
    // =========================================================

    fun updateReference(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                reference = value,
                errorMessage = null
            )


        markPurchaseChanged()
    }


    // =========================================================
    // CONFIRM PURCHASE HEADER
    // =========================================================

    private fun checkInvoiceDuplicateDebounced() {

        invoiceDuplicateCheckJob?.cancel()

        val supplierId =
            _uiState.value.supplierId

        val invoiceNumber =
            _uiState.value.invoiceNumber.trim()

        if (
            supplierId == null ||
            supplierId <= 0L ||
            invoiceNumber.isBlank()
        ) {

            _uiState.value =
                _uiState.value.copy(
                    isInvoiceDuplicate = false,
                    isCheckingInvoiceDuplicate = false,
                    invoiceDuplicateMessage = null
                )

            return
        }

        invoiceDuplicateCheckJob =
            viewModelScope.launch {

                _uiState.value =
                    _uiState.value.copy(
                        isCheckingInvoiceDuplicate = true,
                        isInvoiceDuplicate = false,
                        invoiceDuplicateMessage = null
                    )

                delay(350)

                try {

                    val editPurchaseId =
                        _uiState.value.editingPurchaseId

                    val exists =
                        if (
                            _uiState.value.isEditMode &&
                            editPurchaseId != null &&
                            editPurchaseId > 0L
                        ) {
                            purchaseRepository
                                .purchaseInvoiceExistsExcludingPurchase(
                                    supplierId = supplierId,
                                    invoiceNumber = invoiceNumber,
                                    excludePurchaseId = editPurchaseId
                                )
                        } else {
                            purchaseRepository
                                .purchaseInvoiceExists(
                                    supplierId = supplierId,
                                    invoiceNumber = invoiceNumber
                                )
                        }

                    val latestState =
                        _uiState.value


                    if (
                        latestState.supplierId != supplierId ||
                        latestState.invoiceNumber.trim() != invoiceNumber
                    ) {
                        return@launch
                    }

                    _uiState.value =
                        latestState.copy(
                            isCheckingInvoiceDuplicate = false,
                            isInvoiceDuplicate = exists,
                            invoiceDuplicateMessage =
                                if (exists) {
                                    "Purchase Invoice No. already exists for this vendor."
                                } else {
                                    null
                                }
                        )

                } catch (_: Exception) {

                    _uiState.value =
                        _uiState.value.copy(
                            isCheckingInvoiceDuplicate = false
                        )
                }
            }
    }


    fun confirmPurchaseHeader(): Boolean {

        if (_uiState.value.isCheckingInvoiceDuplicate) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Please wait while Invoice No. is being checked."
                )

            return false
        }

        if (_uiState.value.isInvoiceDuplicate) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        _uiState.value.invoiceDuplicateMessage
                            ?: "Purchase Invoice No. already exists for this vendor."
                )

            return false
        }


        val currentState =
            _uiState.value

        if (
            currentState.supplierId == null ||
            currentState.supplierName.isBlank()
        ) {

            _uiState.value =
                currentState.copy(
                    isHeaderConfirmed = false,
                    errorMessage = "Please select Vendor."
                )

            return false
        }

        if (
            currentState.invoiceNumber
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                currentState.copy(
                    isHeaderConfirmed = false,
                    errorMessage = "Please enter Supplier Invoice Number."
                )

            return false
        }

        if (
            currentState.invoiceDate
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                currentState.copy(
                    isHeaderConfirmed = false,
                    errorMessage = "Please select Invoice Date."
                )

            return false
        }

        _uiState.value =
            currentState.copy(
                isHeaderConfirmed = true,
                errorMessage = null
            )

        return true
    }


    // =========================================================
    // EDIT PURCHASE HEADER
    // =========================================================

    fun editPurchaseHeader() {

        _uiState.value =
            _uiState.value.copy(
                isHeaderConfirmed = false,
                errorMessage = null
            )
    }


    // =========================================================
    // ADJUSTMENT
    // =========================================================

    fun updateAdjustmentAmount(
        value: Double
    ) {

        _uiState.value =
            _uiState.value.copy(
                adjustmentAmount = value
            )

        recalculateTotals()


        markPurchaseChanged()
    }


    // =========================================================
    // PAID AMOUNT
    // =========================================================
    //
    // Retained temporarily during structural migration.
    // Paid Amount / Due Amount will be removed in the dedicated
    // Purchase accounting cleanup step.
    // =========================================================

    fun updatePaidAmount(
        value: Double
    ) {

        val safeValue =
            if (value < 0.0) {
                0.0

            } else {
                value
            }

        _uiState.value =
            _uiState.value.copy(
                paidAmount = safeValue
            )

        recalculateTotals()


        markPurchaseChanged()
    }


    // =========================================================
    // ADD PURCHASE ITEM
    // =========================================================

    fun addItem(
        item: PurchaseItem
    ) {

        /*
         * Same Product + Same Power can be added multiple times.
         *
         * Physical IOL identity is controlled through its
         * unique Serial Number.
         */

        val updatedItems =
            _uiState.value.items +
                    item


        _uiState.value =
            _uiState.value.copy(
                items = updatedItems,
                errorMessage = null
            )


        recalculateTotals()


        markPurchaseChanged()
    }


    // =========================================================
    // REMOVE PURCHASE ITEM
    // =========================================================

    fun removeItem(
        index: Int
    ) {

        val currentItems =
            _uiState.value.items
                .toMutableList()


        if (
            index in currentItems.indices
        ) {

            currentItems.removeAt(
                index
            )


            _uiState.value =
                _uiState.value.copy(
                    items = currentItems,
                    errorMessage = null
                )


            recalculateTotals()
        }


        markPurchaseChanged()
    }


    // =========================================================
    // UPDATE PURCHASE ITEM
    // =========================================================

    fun updateItem(
        index: Int,
        item: PurchaseItem
    ): Boolean {

        val currentItems =
            _uiState.value.items
                .toMutableList()

        if (index !in currentItems.indices) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Unable to update Purchase Item."
                )

            return false
        }

        currentItems[index] =
            item

        _uiState.value =
            _uiState.value.copy(
                items = currentItems,
                errorMessage = null
            )

        recalculateTotals()

        markPurchaseChanged()

        return true
    }


    // =========================================================
    // DUPLICATE SERIAL CHECK
    // =========================================================

    fun hasDuplicateSerialNumbers(
        newItem: PurchaseItem,
        excludeIndex: Int? = null
    ): Boolean {

        /*
         * During Add:
         * excludeIndex = null
         *
         * During Edit:
         * excludeIndex = index of the item being edited.
         *
         * This prevents an existing item's own serial numbers
         * from being treated as duplicates while updating it.
         */

        val existingSerialNumbers =
            _uiState.value.items
                .filterIndexed { index, _ ->
                    index != excludeIndex
                }
                .flatMap { item ->

                    item.lensDetails.map { lensDetail ->
                        lensDetail.serialNumber
                    }
                }
                .map {
                    it
                        .trim()
                        .uppercase()
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()


        val newSerialNumbers =

            newItem.lensDetails
                .map {
                    it.serialNumber
                        .trim()
                        .uppercase()
                }
                .filter {
                    it.isNotBlank()
                }


        // Duplicate against another Purchase Item
        if (
            newSerialNumbers.any {
                it in existingSerialNumbers
            }
        ) {

            return true
        }


        // Duplicate inside the edited/new item itself
        return newSerialNumbers.size !=
                newSerialNumbers
                    .toSet()
                    .size
    }


    // =========================================================
    // ADD ITEM SAFELY
    // =========================================================

    fun addItemSafely(
        item: PurchaseItem
    ): Boolean {

        if (
            hasDuplicateSerialNumbers(
                newItem = item
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Duplicate Serial Number found in current Purchase."
                )

            return false
        }


        addItem(
            item
        )

        return true
    }


    // =========================================================
    // UPDATE ITEM SAFELY
    // =========================================================

    fun updateItemSafely(
        index: Int,
        item: PurchaseItem
    ): Boolean {

        if (
            index !in _uiState.value.items.indices
        ) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Unable to update Purchase Item."
                )

            return false
        }


        if (
            hasDuplicateSerialNumbers(
                newItem = item,
                excludeIndex = index
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Duplicate Serial Number found in current Purchase."
                )

            return false
        }


        return updateItem(
            index = index,
            item = item
        )
    }


    // =========================================================
    // RECALCULATE PURCHASE TOTALS
    // =========================================================

    private fun recalculateTotals() {

        val currentState =
            _uiState.value


        var grossAmount =
            0.0

        var discountAmount =
            0.0

        var taxableAmount =
            0.0

        var taxAmount =
            0.0


        currentState.items.forEach { item ->

            val itemGross =
                item.quantity *
                        item.purchaseRate


            val itemDiscount =
                itemGross *
                        (
                                item.discountPercent /
                                        100.0
                                )


            val itemTaxable =
                itemGross -
                        itemDiscount


            val itemTax =
                itemTaxable *
                        (
                                item.gstPercent /
                                        100.0
                                )



            grossAmount +=
                itemGross

            discountAmount +=
                itemDiscount

            taxableAmount +=
                itemTaxable

            taxAmount +=
                itemTax
        }


        val amountBeforeRoundOff =
            taxableAmount +
                    taxAmount +
                    currentState.adjustmentAmount


        val roundedNetAmount =
            kotlin.math.round(
                amountBeforeRoundOff
            )


        val roundOffAmount =
            roundedNetAmount -
                    amountBeforeRoundOff


        val dueAmount =
            (
                    roundedNetAmount -
                            currentState.paidAmount
                    )
                .coerceAtLeast(
                    0.0
                )


        _uiState.value =
            currentState.copy(

                grossAmount =
                    grossAmount,

                discountAmount =
                    discountAmount,

                taxableAmount =
                    taxableAmount,

                taxAmount =
                    taxAmount,

                roundOffAmount =
                    roundOffAmount,

                netAmount =
                    roundedNetAmount,

                dueAmount =
                    dueAmount
            )
    }


    // =========================================================
    // LOAD EXISTING PURCHASE FOR EDIT
    // =========================================================

    fun loadPurchaseForEdit(
        purchaseId: Long
    ) {

        if (purchaseId <= 0L) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage = "Invalid Purchase ID."
                )
            return
        }

        val repository =
            productRepository

        if (repository == null) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Product repository is not connected for Purchase Edit."
                )
            return
        }

        viewModelScope.launch {

            _uiState.value =
                PurchaseUiState(
                    isEditMode = true,
                    editingPurchaseId = purchaseId,
                    isLoadingPurchaseForEdit = true
                )

            try {

                val purchase =
                    purchaseRepository
                        .getPurchaseById(
                            purchaseId
                        )

                if (purchase == null) {

                    _uiState.value =
                        PurchaseUiState(
                            isEditMode = true,
                            editingPurchaseId = purchaseId,
                            isLoadingPurchaseForEdit = false,
                            errorMessage =
                                "Purchase invoice not found."
                        )

                    return@launch
                }

                val purchaseItems =
                    purchaseRepository
                        .getPurchaseItems(
                            purchaseId
                        )
                        .first()

                val uiItems =
                    purchaseItems.map { itemEntity ->

                        val product =
                            repository
                                .getProductById(
                                    itemEntity.productId
                                )

                        val lenses =
                            purchaseRepository
                                .getPurchaseLenses(
                                    itemEntity.id
                                )
                                .first()

                        PurchaseItem(
                            productId =
                                itemEntity.productId,

                            productName =
                                product?.productName
                                    ?: "Unknown Product",

                            model =
                                product?.model

                                    ?: "",

                            category =
                                product?.category
                                    ?.name
                                    ?: "",

                            hsnCode =
                                product?.hsnCode
                                    ?: "",

                            power =
                                itemEntity.power,

                            quantity =
                                itemEntity.quantity,

                            purchaseRate =
                                itemEntity.purchaseRate,

                            discountPercent =
                                itemEntity.discountPercent,

                            gstPercent =
                                itemEntity.gstPercent,

                            batchNumber =
                                itemEntity.batchNumber,

                            lensDetails =
                                lenses.map { lens ->
                                    IolLensDetail(
                                        serialNumber =
                                            lens.serialNumber,
                                        expiryDate =
                                            lens.expiryDate
                                    )
                                }
                        )
                    }

                val parties =
                    partyRepository
                        .getAllActiveParties()
                        .first()

                val vendor =
                    parties.firstOrNull { party ->
                        party.id == purchase.supplierId
                    }

                val totalTax =
                    purchase.cgstAmount +
                            purchase.sgstAmount +
                            purchase.igstAmount

                /*
                 * PurchaseEntity currently does not persist Adjustment and
                 * Round Off separately. The difference below reconstructs
                 * the saved final adjustment effect so reopening an old
                 * Purchase does not silently change its grand total.
                 */
                val reconstructedAdjustment =
                    purchase.grandTotal -
                            (
                                    purchase.taxableAmount +
                                            totalTax
                                    )

                val vendorCreditDays =
                    vendor?.creditDays
                        ?.toString()
                        ?.takeIf {
                            it != "0"
                        }
                        .orEmpty()

                _uiState.value =
                    PurchaseUiState(
                        isEditMode = true,
                        editingPurchaseId = purchase.id,
                        isLoadingPurchaseForEdit = false,

                        supplierId = purchase.supplierId,
                        supplierName = purchase.supplierName,

                        supplierAddress =
                            vendor?.let {
                                buildSupplierAddress(it)
                            }.orEmpty(),

                        supplierCity =
                            vendor?.city.orEmpty(),

                        supplierDistrict =
                            vendor?.district.orEmpty(),

                        supplierState =
                            vendor?.state.orEmpty(),

                        supplierPinCode =
                            vendor?.pinCode.orEmpty(),

                        supplierGstin =
                            vendor?.gstin.orEmpty(),

                        supplierCreditDays =
                            vendorCreditDays,

                        invoiceNumber =
                            purchase.invoiceNumber,

                        invoiceDate =
                            purchase.invoiceDate,

                        receivedDate =
                            purchase.receivedDate,

                        purchaseType =
                            purchase.purchaseType,

                        paymentType =
                            purchase.paymentType,

                        creditDays =
                            purchase.creditDays
                                .toString()
                                .takeIf {
                                    it != "0"
                                }
                                .orEmpty(),

                        reference =
                            purchase.reference,

                        items =
                            uiItems,

                        grossAmount =
                            purchase.subtotal,

                        discountAmount =
                            purchase.discountAmount,

                        taxableAmount =
                            purchase.taxableAmount,

                        taxAmount =
                            totalTax,

                        adjustmentAmount =
                            reconstructedAdjustment,

                        netAmount =
                            purchase.grandTotal,

                        isHeaderConfirmed = true,

                        isSaved = true,
                        isDirty = false,
                        errorMessage = null,
                        isSavedSuccessfully = false

                    )

                /*
                 * Recalculate from item rows so Purchase Entry summary is
                 * internally consistent with the current calculation rules.
                 */
                recalculateTotals()

                checkInvoiceDuplicateDebounced()

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingPurchaseForEdit = false,
                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load Purchase for editing."
                    )
            }
        }
    }


    // =========================================================
    // START NEW PURCHASE
    // =========================================================
    //
    // Call only when the user intentionally starts a NEW
    // purchase from the Dashboard.
    //
    // Do NOT call this when returning from Add/Edit Product,
    // otherwise the current unsaved purchase would be cleared.
    // =========================================================

    fun startNewPurchase() {

        _uiState.value =
            PurchaseUiState()

        loadVendors()
    }


    // =========================================================
    // SAVE PURCHASE
    // =========================================================

    fun savePurchase() {

        val saveState = _uiState.value

        if (saveState.isSaving || (saveState.isSaved && !saveState.isDirty)) {
            return
        }

        if (saveState.isCheckingInvoiceDuplicate) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Please wait while Invoice No. is being checked."
                )

            return
        }

        if (_uiState.value.isInvoiceDuplicate) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        _uiState.value.invoiceDuplicateMessage
                            ?: "Purchase Invoice No. already exists for this vendor."
                )

            return
        }


        val currentState =
            _uiState.value

        val supplierId =
            currentState.supplierId

        if (
            supplierId == null ||
            supplierId <= 0L ||
            currentState.supplierName.isBlank()
        ) {

            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Please select Vendor."
                )

            return
        }

        val normalizedInvoiceNumber =
            currentState.invoiceNumber
                .trim()
                .uppercase()

        if (normalizedInvoiceNumber.isBlank()) {

            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Please enter Supplier Invoice Number."
                )

            return
        }

        if (currentState.invoiceDate.trim().isBlank()) {

            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Please select Invoice Date."
                )

            return
        }

        // =====================================================
        // FINANCIAL YEAR VALIDATION
        // =====================================================

        val invoiceLocalDate =
            try {
                LocalDate.parse(
                    currentState.invoiceDate.trim(),
                    DateTimeFormatter
                        .ofPattern("dd-MM-uuuu")
                        .withResolverStyle(ResolverStyle.STRICT)
                )
            } catch (_: Exception) {
                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage =
                            "Invalid Invoice Date. Please use DD-MM-YYYY."
                    )
                return
            }

        val transactionFinancialYear =
            FinancialYearManager.financialYearForDate(
                invoiceLocalDate
            )

        val activeFinancialYear =
            FinancialYearManager.activeFinancialYear.value

        if (

            transactionFinancialYear.startYear !=
            activeFinancialYear.startYear
        ) {
            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage =
                        "Invoice Date belongs to FY " +
                                transactionFinancialYear.displayName +
                                ". Please change Working Financial Year to " +
                                transactionFinancialYear.displayName +
                                " from Settings before saving this purchase."
                )
            return
        }

        val financialYearStart =
            transactionFinancialYear.startYear


        if (currentState.items.isEmpty()) {

            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Please add at least one Product."
                )

            return
        }

        // Final safety validation before persistence.
        currentState.items.forEach { item ->

            if (item.productId <= 0L) {

                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "Invalid Product found in Purchase."
                    )

                return
            }

            if (item.quantity <= 0) {

                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "Product quantity must be greater than zero."
                    )

                return
            }

            if (item.purchaseRate < 0.0) {

                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "Purchase rate cannot be negative."
                    )

                return
            }

            if (
                item.discountPercent < 0.0 ||
                item.discountPercent > 100.0
            ) {

                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "Discount must be between 0 and 100."
                    )

                return
            }

            if (
                item.gstPercent < 0.0 ||
                item.gstPercent > 100.0
            ) {

                _uiState.value =
                    currentState.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "GST must be between 0 and 100."
                    )

                return
            }

            val isIol =
                item.category.equals(
                    "IOL",
                    ignoreCase = true
                )

            if (isIol) {

                if (item.lensDetails.size != item.quantity) {

                    _uiState.value =
                        currentState.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage =
                                "IOL Quantity and Lens Detail count must be equal."
                        )

                    return
                }

                if (
                    item.lensDetails.any {
                        it.serialNumber.trim().isBlank()
                    }
                ) {

                    _uiState.value =
                        currentState.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage =
                                "Serial Number cannot be blank."
                        )

                    return
                }

                if (
                    item.lensDetails.any {
                        it.expiryDate.trim().isBlank()
                    }
                ) {

                    _uiState.value =
                        currentState.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage =
                                "Expiry cannot be blank."
                        )

                    return
                }
            }
        }

        // Cross-item serial duplication check.

        val allSerialNumbers =
            currentState.items
                .flatMap { item ->
                    item.lensDetails.map { lens ->
                        lens.serialNumber
                            .trim()
                            .uppercase()
                    }
                }
                .filter {
                    it.isNotBlank()
                }

        if (
            allSerialNumbers.size !=
            allSerialNumbers.toSet().size
        ) {

            _uiState.value =
                currentState.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage =
                        "Duplicate Serial Number found in current Purchase."
                )

            return
        }

        _uiState.value =
            currentState.copy(
                isSaving = true,
                isSavedSuccessfully = false,
                errorMessage = null
            )

        viewModelScope.launch {

            try {

                val editPurchaseId =
                    currentState.editingPurchaseId

                val duplicateExists =
                    if (
                        currentState.isEditMode &&
                        editPurchaseId != null &&
                        editPurchaseId > 0L
                    ) {
                        purchaseRepository
                            .purchaseInvoiceExistsExcludingPurchase(
                                supplierId = supplierId,
                                invoiceNumber =
                                    normalizedInvoiceNumber,
                                excludePurchaseId =
                                    editPurchaseId
                            )
                    } else {
                        purchaseRepository
                            .purchaseInvoiceExists(
                                supplierId = supplierId,
                                invoiceNumber =
                                    normalizedInvoiceNumber
                            )
                    }

                if (duplicateExists) {

                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage =
                                "Purchase Invoice No. already exists for this vendor."
                        )

                    return@launch
                }

                val stateForSave =
                    _uiState.value

                val purchaseEntity =
                    PurchaseEntity(
                        id =
                            if (stateForSave.isEditMode) {
                                stateForSave.editingPurchaseId
                                    ?: 0L
                            } else {
                                0L
                            },

                        supplierId = supplierId,
                        supplierName =
                            stateForSave.supplierName.trim(),
                        invoiceNumber =
                            normalizedInvoiceNumber,
                        normalizedInvoiceNumber =
                            normalizedInvoiceNumber,
                        invoiceDate =
                            stateForSave.invoiceDate.trim(),
                        receivedDate =
                            stateForSave.receivedDate.trim(),

                        financialYearStart =
                            financialYearStart,

                        purchaseType =
                            stateForSave.purchaseType.trim(),
                        paymentType =
                            stateForSave.paymentType.trim(),
                        creditDays =
                            stateForSave.creditDays
                                .trim()
                                .toIntOrNull()
                                ?: 0,
                        reference =
                            stateForSave.reference.trim(),
                        subtotal =
                            stateForSave.grossAmount,
                        discountAmount =
                            stateForSave.discountAmount,
                        taxableAmount =
                            stateForSave.taxableAmount,

                        // Current Purchase UI exposes total GST,
                        // not separate CGST / SGST / IGST breakup.
                        // Preserve the total tax in IGST temporarily
                        // rather than silently losing the tax amount.
                        cgstAmount = 0.0,
                        sgstAmount = 0.0,
                        igstAmount =
                            stateForSave.taxAmount,

                        grandTotal =
                            stateForSave.netAmount
                    )

                val itemsWithLenses =
                    stateForSave.items.map { item ->

                        val grossAmount =
                            item.quantity *
                                    item.purchaseRate

                        val discountAmount =
                            grossAmount *
                                    (
                                            item.discountPercent /
                                                    100.0
                                            )

                        val taxableAmount =
                            grossAmount -
                                    discountAmount

                        val gstAmount =
                            taxableAmount *
                                    (
                                            item.gstPercent /

                                                    100.0
                                            )

                        val lineTotal =
                            taxableAmount +
                                    gstAmount

                        val itemEntity =
                            PurchaseItemEntity(
                                // Replaced inside DAO transaction.
                                purchaseId = 0L,

                                productId =
                                    item.productId,

                                power =
                                    item.power.trim(),

                                quantity =
                                    item.quantity,

                                purchaseRate =
                                    item.purchaseRate,

                                discountPercent =
                                    item.discountPercent,

                                gstPercent =
                                    item.gstPercent,

                                batchNumber =
                                    item.batchNumber.trim(),

                                // Legacy compatibility field.
                                // Physical IOL expiry is persisted
                                // per lens in purchase_lenses.
                                expiryDate = "",

                                grossAmount =
                                    grossAmount,

                                discountAmount =
                                    discountAmount,

                                taxableAmount =
                                    taxableAmount,

                                gstAmount =
                                    gstAmount,

                                lineTotal =
                                    lineTotal
                            )

                        val lensEntities =
                            item.lensDetails.map { lens ->

                                PurchaseLensEntity(
                                    // Replaced inside DAO transaction.
                                    purchaseItemId = 0L,

                                    serialNumber =
                                        lens.serialNumber
                                            .trim()
                                            .uppercase(),

                                    expiryDate =
                                        lens.expiryDate
                                            .trim()
                                )
                            }

                        itemEntity to
                                lensEntities
                    }

                // -------------------------------------------------
                // SAVED SERIAL DUPLICATE VALIDATION
                // -------------------------------------------------

                val serialsToValidate =
                    stateForSave.items
                        .flatMap { item ->
                            item.lensDetails.map { lens ->
                                lens.serialNumber
                                    .trim()
                                    .uppercase()
                            }
                        }
                        .filter {
                            it.isNotBlank()
                        }

                for (serialNumber in serialsToValidate) {

                    val serialExists =
                        if (
                            stateForSave.isEditMode &&
                            stateForSave.editingPurchaseId != null &&
                            stateForSave.editingPurchaseId > 0L
                        ) {
                            purchaseRepository
                                .purchaseLensSerialExistsExcludingPurchase(
                                    serialNumber = serialNumber,
                                    excludePurchaseId =
                                        stateForSave.editingPurchaseId
                                )
                        } else {
                            purchaseRepository
                                .purchaseLensSerialExists(
                                    serialNumber
                                )
                        }

                    if (serialExists) {

                        _uiState.value =
                            _uiState.value.copy(
                                isSaving = false,
                                isSavedSuccessfully = false,
                                errorMessage =
                                    "Serial Number $serialNumber already exists in another Purchase."
                            )

                        return@launch
                    }
                }

                // -------------------------------------------------
                // INSERT NEW OR UPDATE EXISTING PURCHASE
                // -------------------------------------------------
                //
                // IMPORTANT:
                // Audit is written only AFTER the Purchase database
                // operation succeeds. Audit failure itself must never
                // convert an already-saved Purchase into a UI failure.
                // -------------------------------------------------

                val isExistingPurchase =
                    stateForSave.isEditMode &&
                            stateForSave.editingPurchaseId != null &&
                            stateForSave.editingPurchaseId > 0L

                val savedPurchaseId =
                    if (isExistingPurchase) {

                        val existingPurchaseId =
                            stateForSave.editingPurchaseId!!

                        purchaseRepository
                            .updateCompletePurchase(
                                purchase =
                                    purchaseEntity,

                                itemsWithLenses =
                                    itemsWithLenses
                            )

                        existingPurchaseId


                    } else {

                        purchaseRepository
                            .saveCompletePurchase(
                                purchase =
                                    purchaseEntity,

                                itemsWithLenses =
                                    itemsWithLenses
                            )
                    }


                // -------------------------------------------------
                // PURCHASE AUDIT TRAIL
                // -------------------------------------------------
                //
                // The Purchase is already safely committed at this
                // point. Therefore audit logging is deliberately kept
                // in its own try/catch block.
                // -------------------------------------------------

                try {

                    auditTrailRepository.recordEvent(
                        module = "PURCHASE",
                        action =
                            if (isExistingPurchase) {
                                "UPDATE"
                            } else {
                                "CREATE"
                            },
                        recordId =
                            savedPurchaseId,
                        description =
                            if (isExistingPurchase) {
                                "Purchase updated â€¢ Invoice $normalizedInvoiceNumber â€¢ ${stateForSave.supplierName.trim()} â€¢ FY ${transactionFinancialYear.displayName}"
                            } else {
                                "Purchase created â€¢ Invoice $normalizedInvoiceNumber â€¢ ${stateForSave.supplierName.trim()} â€¢ FY ${transactionFinancialYear.displayName}"
                            }
                    )

                } catch (_: Exception) {

                    /*
                     * Audit failure must never make a successfully
                     * saved/updated Purchase appear to have failed.
                     */
                }


                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        isSaved = true,
                        isDirty = false,
                        isSavedSuccessfully = true,
                        errorMessage = null
                    )

            } catch (exception: Exception) {

                val message =
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to save Purchase."

                val friendlyMessage =
                    if (
                        message.contains(
                            "UNIQUE constraint failed",
                            ignoreCase = true
                        )
                    ) {
                        "Purchase Invoice No. or Serial Number already exists."
                    } else {
                        message
                    }

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage =
                            friendlyMessage
                    )
            }
        }
    }


    // =========================================================
    // SAVE / DIRTY STATE
    // =========================================================

    private fun markPurchaseChanged() {

        val currentState = _uiState.value

        if (currentState.isLoadingPurchaseForEdit || currentState.isSaving) {
            return
        }

        _uiState.value =
            currentState.copy(
                isSaved = false,
                isDirty = true,
                isSavedSuccessfully = false
            )
    }

    /*
     * Save success is a one-time event for the confirmation message.
     * The persistent isSaved flag controls the disabled SAVED button.
     */
    fun clearSaveSuccess() {

        _uiState.value =
            _uiState.value.copy(
                isSavedSuccessfully = false
            )
    }


    // =========================================================
    // BUILD SUPPLIER ADDRESS
    // =========================================================

    private fun buildSupplierAddress(
        party: PartyMaster
    ): String {

        return listOf(

            party.addressLine1,

            party.addressLine2

        )
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                separator = ", "
            )
    }


    // =========================================================
    // CLEAR ERROR
    // =========================================================

    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }
}

