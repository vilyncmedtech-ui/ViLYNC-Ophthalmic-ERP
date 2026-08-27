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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PurchaseViewModel(
    private val partyRepository: PartyRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductMasterRepository? = null,
    private val auditTrailRepository: AuditTrailRepository,
    private val numberingRepository: com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository? = null,
    private val initialProductId: Long = 0L,
    private val initialPower: String = "",
    private val initialQty: Int = 0,
    private val initialStatus: String = "POSTED"
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

    private fun prefillFromShortage() {
        viewModelScope.launch {
            val product = productRepository?.getProductById(initialProductId) ?: return@launch
            val allVendors = partyRepository.getAllActiveParties().first()
            
            val matchingVendor = allVendors.find { 
                it.partyName.trim().equals(product.brand.trim(), ignoreCase = true) &&
                isPurchaseVendor(it.partyType)
            }

            _uiState.update { state ->
                var updatedState = state.copy(
                    status = initialStatus,
                    invoiceNumber = if (initialStatus == "ORDER") "Auto-generated on Save" else "",
                    invoiceDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    receivedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                )
                
                matchingVendor?.let {
                    updatedState = updatedState.copy(
                        supplierId = it.id,
                        supplierName = it.partyName,
                        supplierGstin = it.gstin,
                        supplierState = it.state,
                        supplierAddress = buildSupplierAddress(it),
                        supplierCity = it.city,
                        supplierDistrict = it.district,
                        supplierPinCode = it.pinCode,
                        supplierCreditDays = it.creditDays.toString(),
                        creditDays = it.creditDays.toString()
                    )
                }

                val item = PurchaseItem(
                    productId = product.id,
                    productName = product.productName,
                    model = product.model,
                    category = product.category.name,
                    hsnCode = product.hsnCode,
                    power = initialPower,
                    quantity = initialQty.coerceAtLeast(1),
                    purchaseRate = product.purchasePrice,
                    gstPercent = product.gstPercent
                )
                
                updatedState.copy(items = listOf(item)).let { finalState ->
                    // Calculate totals
                    val gross = item.quantity * item.purchaseRate
                    val discount = 0.0
                    val taxable = gross - discount
                    val tax = taxable * (item.gstPercent / 100.0)
                    val total = taxable + tax
                    
                    finalState.copy(
                        grossAmount = gross,
                        taxableAmount = taxable,
                        taxAmount = tax,
                        netAmount = total,
                        dueAmount = total,
                        isDirty = true
                    )
                }
            }
        }
    }


    // =========================================================
    // LOAD VENDORS FROM PARTY MASTER
    // =========================================================

    private fun loadVendors() {

        viewModelScope.launch {

            _uiState.update { it.copy(
                isLoadingSuppliers = true
            )}

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

                        _uiState.update { it.copy(
                            isLoadingSuppliers = false
                        )}
                    }

            } catch (exception: Exception) {

                _uiState.update { it.copy(
                    isLoadingSuppliers = false,
                    errorMessage =
                        exception.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Unable to load vendors."
                )}
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

            _uiState.update { it.copy(
                errorMessage =
                    "Please select a Vendor or Customer & Vendor party."
            )}

            return
        }


        val fullAddress =
            buildSupplierAddress(
                party = party
            )


        val creditDaysStr =
            party.creditDays
                .toString()
                .takeIf {
                    it != "0"
                }
                .orEmpty()


        _uiState.update { it.copy(

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
                creditDaysStr,

            creditDays =
                creditDaysStr,

            errorMessage =
                null
        )}

        checkInvoiceDuplicateDebounced()


        markPurchaseChanged()
    }


    // =========================================================
    // CLEAR SELECTED VENDOR
    // =========================================================

    fun clearVendor() {

        _uiState.update { it.copy(

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
        )}


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

        _uiState.update { it.copy(
            supplierName = value,
            supplierId = null,
            errorMessage = null
        )}


        markPurchaseChanged()
    }


    // =========================================================
    // INVOICE NUMBER
    // =========================================================

    fun updateInvoiceNumber(
        value: String
    ) {

        _uiState.update { it.copy(
            invoiceNumber = value,
            isInvoiceDuplicate = false,
            isCheckingInvoiceDuplicate = false,
            invoiceDuplicateMessage = null,
            errorMessage = null,
            isSavedSuccessfully = false
        )}

        checkInvoiceDuplicateDebounced()


        markPurchaseChanged()
    }


    // =========================================================
    // INVOICE DATE
    // =========================================================

    fun updateInvoiceDate(
        value: String
    ) {

        _uiState.update { it.copy(
            invoiceDate = value,
            errorMessage = null
        )}


        markPurchaseChanged()
    }


    // =========================================================
    // RECEIVED DATE
    // =========================================================

    fun updateReceivedDate(
        value: String
    ) {

        _uiState.update { it.copy(
            receivedDate = value,
            errorMessage = null
        )}


        markPurchaseChanged()
    }


    // =========================================================
    // PURCHASE TYPE
    // =========================================================

    fun updatePurchaseType(
        value: String
    ) {

        _uiState.update { it.copy(
            purchaseType = value,
            errorMessage = null
        )}


        markPurchaseChanged()
    }


    // =========================================================
    // PAYMENT TYPE
    // =========================================================

    fun updatePaymentType(
        value: String
    ) {

        _uiState.update { it.copy(
            paymentType = value,
            errorMessage = null
        )}


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

        _uiState.update { it.copy(
            creditDays = cleanValue,
            errorMessage = null
        )}


        markPurchaseChanged()
    }


    // =========================================================
    // REFERENCE / REMARKS
    // =========================================================

    fun updateReference(
        value: String
    ) {

        _uiState.update { it.copy(
            reference = value,
            errorMessage = null
        )}


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

            _uiState.update { it.copy(
                isInvoiceDuplicate = false,
                isCheckingInvoiceDuplicate = false,
                invoiceDuplicateMessage = null
            )}

            return
        }

        invoiceDuplicateCheckJob =
            viewModelScope.launch {

                _uiState.update { it.copy(
                    isCheckingInvoiceDuplicate = true,
                    isInvoiceDuplicate = false,
                    invoiceDuplicateMessage = null
                )}

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

                    _uiState.update { latestState ->
                        if (
                            latestState.supplierId != supplierId ||
                            latestState.invoiceNumber.trim() != invoiceNumber
                        ) {
                            latestState
                        } else {
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
                        }
                    }

                } catch (_: Exception) {

                    _uiState.update { it.copy(
                        isCheckingInvoiceDuplicate = false
                    )}
                }
            }
    }


    fun confirmPurchaseHeader(): Boolean {

        if (_uiState.value.isCheckingInvoiceDuplicate) {

            _uiState.update { it.copy(
                errorMessage =
                    "Please wait while Invoice No. is being checked."
            )}

            return false
        }

        if (_uiState.value.isInvoiceDuplicate) {

            _uiState.update { it.copy(
                errorMessage =
                    it.invoiceDuplicateMessage
                        ?: "Purchase Invoice No. already exists for this vendor."
            )}

            return false
        }


        val currentState =
            _uiState.value

        if (
            currentState.supplierId == null ||
            currentState.supplierName.isBlank()
        ) {

            _uiState.update { it.copy(
                isHeaderConfirmed = false,
                errorMessage = "Please select Vendor."
            )}

            return false
        }

        if (
            currentState.invoiceNumber
                .trim()
                .isBlank()
        ) {

            _uiState.update { it.copy(
                isHeaderConfirmed = false,
                errorMessage = "Please enter Supplier Invoice Number."
            )}

            return false
        }

        if (
            currentState.invoiceDate
                .trim()
                .isBlank()
        ) {

            _uiState.update { it.copy(
                isHeaderConfirmed = false,
                errorMessage = "Please select Invoice Date."
            )}

            return false
        }

        _uiState.update { it.copy(
            isHeaderConfirmed = true,
            errorMessage = null
        )}

        return true
    }


    // =========================================================
    // EDIT PURCHASE HEADER
    // =========================================================

    fun editPurchaseHeader() {

        _uiState.update { it.copy(
            isHeaderConfirmed = false,
            errorMessage = null
        )}
    }


    // =========================================================
    // ADJUSTMENT
    // =========================================================

    fun updateAdjustmentAmount(
        value: Double
    ) {

        _uiState.update { it.copy(
            adjustmentAmount = value
        )}

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

        _uiState.update { it.copy(
            paidAmount = safeValue
        )}

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

        _uiState.update { it.copy(
            items = it.items + item,
            errorMessage = null
        )}


        recalculateTotals()


        markPurchaseChanged()
    }


    // =========================================================
    // REMOVE PURCHASE ITEM
    // =========================================================

    fun removeItem(
        index: Int
    ) {

        _uiState.update { state ->
            val currentItems = state.items.toMutableList()
            if (index in currentItems.indices) {
                currentItems.removeAt(index)
                state.copy(
                    items = currentItems,
                    errorMessage = null
                )
            } else {
                state
            }
        }


        recalculateTotals()


        markPurchaseChanged()
    }


    // =========================================================
    // UPDATE PURCHASE ITEM
    // =========================================================

    fun updateItem(
        index: Int,
        item: PurchaseItem
    ): Boolean {

        var success = false
        _uiState.update { state ->
            val currentItems = state.items.toMutableList()
            if (index in currentItems.indices) {
                currentItems[index] = item
                success = true
                state.copy(
                    items = currentItems,
                    errorMessage = null
                )
            } else {
                state.copy(
                    errorMessage = "Unable to update Purchase Item."
                )
            }
        }

        if (success) {
            recalculateTotals()
            markPurchaseChanged()
        }

        return success
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

            _uiState.update { it.copy(
                errorMessage =
                    "Duplicate Serial Number found in current Purchase."
            )}

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

            _uiState.update { it.copy(
                errorMessage =
                    "Unable to update Purchase Item."
            )}

            return false
        }


        if (
            hasDuplicateSerialNumbers(
                newItem = item,
                excludeIndex = index
            )
        ) {

            _uiState.update { it.copy(
                errorMessage =
                    "Duplicate Serial Number found in current Purchase."
            )}

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

        _uiState.update { state ->
            var grossAmount = 0.0
            var discountAmount = 0.0
            var taxableAmount = 0.0
            var taxAmount = 0.0

            state.items.forEach { item ->
                val itemGross = item.quantity * item.purchaseRate
                val itemDiscount = itemGross * (item.discountPercent / 100.0)
                val itemTaxable = itemGross - itemDiscount
                val itemTax = itemTaxable * (item.gstPercent / 100.0)

                grossAmount += itemGross
                discountAmount += itemDiscount
                taxableAmount += itemTaxable
                taxAmount += itemTax
            }

            val amountBeforeRoundOff = taxableAmount + taxAmount + state.adjustmentAmount
            val roundedNetAmount = kotlin.math.round(amountBeforeRoundOff)
            val roundOffAmount = roundedNetAmount - amountBeforeRoundOff
            val dueAmount = (roundedNetAmount - state.paidAmount).coerceAtLeast(0.0)

            state.copy(
                grossAmount = grossAmount,
                discountAmount = discountAmount,
                taxableAmount = taxableAmount,
                taxAmount = taxAmount,
                roundOffAmount = roundOffAmount,
                netAmount = roundedNetAmount,
                dueAmount = dueAmount
            )
        }
    }


    // =========================================================
    // LOAD EXISTING PURCHASE FOR EDIT
    // =========================================================

    fun loadPurchaseForEdit(
        purchaseId: Long
    ) {

        if (purchaseId <= 0L) {
            _uiState.update { it.copy(
                errorMessage = "Invalid Purchase ID."
            )}
            return
        }

        // =====================================================
        // SESSION PROTECTION GUARD
        // =====================================================
        //
        // If we are already in edit mode for this specific
        // purchase, and it's not currently loading, we skip
        // the reload. This prevents the state from being
        // reset to database values when returning from
        // the Add Product flow.
        // =====================================================

        val currentState = _uiState.value

        if (
            currentState.isEditMode &&
            currentState.editingPurchaseId == purchaseId &&
            !currentState.isLoadingPurchaseForEdit &&
            currentState.items.isNotEmpty()
        ) {
            return
        }


        val repository =
            productRepository

        if (repository == null) {
            _uiState.update { it.copy(
                errorMessage =
                    "Product repository is not connected for Purchase Edit."
            )}
            return
        }

        viewModelScope.launch {

            _uiState.update { 
                it.copy(
                    isEditMode = true,
                    editingPurchaseId = purchaseId,
                    isLoadingPurchaseForEdit = true,
                    errorMessage = null
                )
            }

            try {

                val purchase =
                    purchaseRepository
                        .getPurchaseById(
                            purchaseId
                        )

                if (purchase == null) {

                    _uiState.update { 
                        it.copy(
                            isEditMode = true,
                            editingPurchaseId = purchaseId,
                            isLoadingPurchaseForEdit = false,
                            errorMessage = "Purchase invoice not found."
                        )
                    }

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

                _uiState.update { lastState ->
                    lastState.copy(
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
                        isSavedSuccessfully = false,
                        status = purchase.status // IMPORTANT: Load status from DB
                    )
                }

                /*
                 * Recalculate from item rows so Purchase Entry summary is
                 * internally consistent with the current calculation rules.
                 */
                recalculateTotals()

                checkInvoiceDuplicateDebounced()

            } catch (exception: Exception) {

                _uiState.update { it.copy(
                    isLoadingPurchaseForEdit = false,
                    errorMessage =
                        exception.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Unable to load Purchase for editing."
                )}
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

        _uiState.update { PurchaseUiState() }

        loadVendors()
    }


    // =========================================================
    // SAVE PURCHASE
    // =========================================================

    fun savePurchase() {
        android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: savePurchase() ENTERED")

        val saveState = _uiState.value

        if (saveState.isSaving) {
            android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Early return - Already saving")
            return
        }
        if (saveState.isSaved && !saveState.isDirty) {
            android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Early return - Already saved and not dirty")
            return
        }

        if (saveState.isCheckingInvoiceDuplicate) {
            android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Early return - Duplicate check in progress")
            _uiState.update { it.copy(
                errorMessage = "Please wait while Invoice No. is being checked."
            )}

            return
        }

        if (_uiState.value.isInvoiceDuplicate) {
            android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Early return - Invoice is duplicate")
            _uiState.update { it.copy(
                errorMessage = it.invoiceDuplicateMessage ?: "Purchase Invoice No. already exists for this vendor."
            )}

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
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Invalid Vendor (id=$supplierId, name=${currentState.supplierName})")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage = "Please select Vendor."
            )}

            return
        }

        val normalizedInvoiceNumber =
            currentState.invoiceNumber
                .trim()
                .uppercase()

        if (normalizedInvoiceNumber.isBlank()) {
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Invoice Number blank")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage = "Please enter Supplier Invoice Number."
            )}

            return
        }

        if (currentState.invoiceDate.trim().isBlank()) {
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Invoice Date blank")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage = "Please select Invoice Date."
            )}

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
            } catch (e: Exception) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Date Parse Error: ${e.message}")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Invalid Invoice Date. Please use DD-MM-YYYY."
                )}
                return
            }

        val transactionFinancialYear =
            FinancialYearManager.financialYearForDate(
                invoiceLocalDate
            )

        val activeFinancialYear =
            FinancialYearManager.activeFinancialYear.value
        
        android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: FY check - Transaction FY: ${transactionFinancialYear.displayName}, Active FY: ${activeFinancialYear.displayName}")

        if (

            transactionFinancialYear.startYear !=
            activeFinancialYear.startYear
        ) {
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - FY Mismatch")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage =
                    "Invoice Date belongs to FY " +
                            transactionFinancialYear.displayName +
                            ". Please change Working Financial Year to " +
                            transactionFinancialYear.displayName +
                            " from Settings before saving this purchase."
            )}
            return
        }

        val financialYearStart =
            transactionFinancialYear.startYear


        if (currentState.items.isEmpty()) {
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Items list empty")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage = "Please add at least one Product."
            )}

            return
        }

        // Final safety validation before persistence.
        currentState.items.forEachIndexed { index, item ->

            if (item.productId <= 0L) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Item at $index has invalid Product ID")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Invalid Product found in Purchase."
                )}

                return
            }

            if (item.quantity <= 0) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Item at $index has invalid quantity (${item.quantity})")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Product quantity must be greater than zero."
                )}

                return
            }

            if (item.purchaseRate < 0.0) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Item at $index has negative rate")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Purchase rate cannot be negative."
                )}

                return
            }

            if (
                item.discountPercent < 0.0 ||
                item.discountPercent > 100.0
            ) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Item at $index has invalid discount")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "Discount must be between 0 and 100."
                )}

                return
            }

            if (
                item.gstPercent < 0.0 ||
                item.gstPercent > 100.0
            ) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Item at $index has invalid GST")
                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = "GST must be between 0 and 100."
                )}

                return
            }

            val isIol =
                item.category.equals(
                    "IOL",
                    ignoreCase = true
                )

                if (isIol) {
                    if (item.lensDetails.size != item.quantity) {
                        android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - IOL Item at $index: Lens Detail count (${item.lensDetails.size}) != quantity (${item.quantity})")
                        _uiState.update { it.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage = "IOL Quantity and Lens Detail count must be equal."
                        )}

                        return
                    }

                    if (
                        item.lensDetails.any {
                            it.serialNumber.trim().isBlank()
                        }
                    ) {
                        android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - IOL Item at $index has blank serial")
                        _uiState.update { it.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage = "Serial Number cannot be blank."
                        )}

                        return
                    }

                    if (
                        item.lensDetails.any {
                            it.expiryDate.trim().isBlank()
                        }
                    ) {
                        android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - IOL Item at $index has blank expiry")
                        _uiState.update { it.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage = "Expiry cannot be blank."
                        )}

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
            android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Validation FAIL - Cross-item serial duplication detected")
            _uiState.update { it.copy(
                isSaving = false,
                isSavedSuccessfully = false,
                errorMessage = "Duplicate Serial Number found in current Purchase."
            )}

            return
        }

        android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: All UI validations PASSED. Starting launch block.")

        _uiState.update { it.copy(
            isSaving = true,
            isSavedSuccessfully = false,
            errorMessage = null
        )}

        viewModelScope.launch {

            try {
                val finalInvoiceNumber = normalizedInvoiceNumber

                val editPurchaseId =
                    currentState.editingPurchaseId

                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Database check - Duplicate Invoice check. id=$supplierId, invoice=$normalizedInvoiceNumber")
                val duplicateExists =
                    if (
                        currentState.isEditMode &&
                        editPurchaseId != null &&
                        editPurchaseId > 0L
                    ) {
                        purchaseRepository
                            .purchaseInvoiceExistsExcludingPurchase(
                                supplierId = supplierId,
                                invoiceNumber = normalizedInvoiceNumber,
                                excludePurchaseId = editPurchaseId
                            )
                    } else {
                        purchaseRepository
                            .purchaseInvoiceExists(
                                supplierId = supplierId,
                                invoiceNumber = normalizedInvoiceNumber
                            )
                    }

                if (duplicateExists) {
                    android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Database FAIL - Invoice already exists")
                    _uiState.update { it.copy(
                        isSaving = false,
                        isSavedSuccessfully = false,
                        errorMessage = "Purchase Invoice No. already exists for this vendor."
                    )}

                    return@launch
                }

                val stateForSave =
                    _uiState.value

                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Creating PurchaseEntity and items list")
                val purchaseEntity =
                    PurchaseEntity(
                        id = if (stateForSave.isEditMode) stateForSave.editingPurchaseId ?: 0L else 0L,
                        supplierId = supplierId,
                        supplierName = stateForSave.supplierName.trim(),
                        invoiceNumber = finalInvoiceNumber,
                        normalizedInvoiceNumber = finalInvoiceNumber,
                        invoiceDate = stateForSave.invoiceDate.trim(),
                        receivedDate = stateForSave.receivedDate.trim(),
                        financialYearStart = financialYearStart,
                        purchaseType = stateForSave.purchaseType.trim(),
                        paymentType = stateForSave.paymentType.trim(),
                        creditDays = stateForSave.creditDays.trim().toIntOrNull() ?: 0,
                        reference = stateForSave.reference.trim(),
                        subtotal = stateForSave.grossAmount,
                        discountAmount = stateForSave.discountAmount,
                        taxableAmount = stateForSave.taxableAmount,
                        cgstAmount = 0.0,
                        sgstAmount = 0.0,
                        igstAmount = stateForSave.taxAmount,
                        grandTotal = stateForSave.netAmount,
                        status = stateForSave.status
                    )

                val itemsWithLenses =
                    stateForSave.items.map { item ->
                        val grossAmount = item.quantity * item.purchaseRate
                        val discountAmount = grossAmount * (item.discountPercent / 100.0)
                        val taxableAmount = grossAmount - discountAmount
                        val gstAmount = taxableAmount * (item.gstPercent / 100.0)
                        val lineTotal = taxableAmount + gstAmount

                        val itemEntity = PurchaseItemEntity(
                            purchaseId = 0L,
                            productId = item.productId,
                            power = item.power.trim(),
                            quantity = item.quantity,
                            purchaseRate = item.purchaseRate,
                            discountPercent = item.discountPercent,
                            gstPercent = item.gstPercent,
                            batchNumber = item.batchNumber.trim(),
                            expiryDate = "",
                            grossAmount = grossAmount,
                            discountAmount = discountAmount,
                            taxableAmount = taxableAmount,
                            gstAmount = gstAmount,
                            lineTotal = lineTotal
                        )

                        val lensEntities = item.lensDetails.map { lens ->
                            PurchaseLensEntity(
                                purchaseItemId = 0L,
                                serialNumber = lens.serialNumber.trim().uppercase(),
                                expiryDate = lens.expiryDate.trim()
                            )
                        }

                        itemEntity to lensEntities
                    }

                val serialsToValidate =
                    stateForSave.items
                        .flatMap { item ->
                            item.lensDetails.map { lens ->
                                lens.serialNumber.trim().uppercase()
                            }
                        }
                        .filter { it.isNotBlank() }

                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Checking ${serialsToValidate.size} serials for existing duplicates in DB")
                for (serialNumber in serialsToValidate) {

                    val serialExists =
                        if (
                            stateForSave.isEditMode &&
                            stateForSave.editingPurchaseId != null &&
                            stateForSave.editingPurchaseId > 0L
                        ) {
                            purchaseRepository.purchaseLensSerialExistsExcludingPurchase(
                                serialNumber = serialNumber,
                                excludePurchaseId = stateForSave.editingPurchaseId
                            )
                        } else {
                            purchaseRepository.purchaseLensSerialExists(serialNumber)
                        }

                    if (serialExists) {
                        android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Database FAIL - Serial $serialNumber already exists")
                        _uiState.update { it.copy(
                            isSaving = false,
                            isSavedSuccessfully = false,
                            errorMessage = "Serial Number $serialNumber already exists in another Purchase."
                        )}

                        return@launch
                    }
                }

                val isExistingPurchase = stateForSave.isEditMode && stateForSave.editingPurchaseId != null && stateForSave.editingPurchaseId > 0L

                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Calling repository.saveCompletePurchase. isEdit=$isExistingPurchase")
                val savedPurchaseId =
                    if (isExistingPurchase) {
                        val existingPurchaseId = stateForSave.editingPurchaseId!!
                        purchaseRepository.updateCompletePurchase(purchase = purchaseEntity, itemsWithLenses = itemsWithLenses)
                        existingPurchaseId
                    } else {
                        purchaseRepository.saveCompletePurchase(purchase = purchaseEntity, itemsWithLenses = itemsWithLenses)
                    }

                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Repository call SUCCESS. Generated ID=$savedPurchaseId")

                try {
                    android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Recording Audit Trail event")
                    auditTrailRepository.recordEvent(
                        module = "PURCHASE",
                        action = if (isExistingPurchase) "UPDATE" else "CREATE",
                        recordId = savedPurchaseId,
                        description = if (isExistingPurchase) {
                            "Purchase updated â€¢ Invoice $finalInvoiceNumber â€¢ ${stateForSave.supplierName.trim()} â€¢ FY ${transactionFinancialYear.displayName}"
                        } else {
                            "Purchase created â€¢ Invoice $finalInvoiceNumber â€¢ ${stateForSave.supplierName.trim()} â€¢ FY ${transactionFinancialYear.displayName}"
                        }
                    )
                    android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Audit Trail recorded SUCCESS")

                } catch (e: Exception) {
                    android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: Audit Trail recorded FAIL - Non-critical: ${e.message}", e)
                }


                _uiState.update { it.copy(
                    isSaving = false,
                    isSaved = true,
                    isDirty = false,
                    isSavedSuccessfully = true,
                    errorMessage = null
                )}
                android.util.Log.d("PURCHASE_SAVE_TRACE", "VM: Final UI state updated. isSavedSuccessfully=true")

            } catch (exception: Exception) {
                android.util.Log.e("PURCHASE_SAVE_TRACE", "VM: CATCH block entered. Exception: ${exception.javaClass.simpleName}: ${exception.message}", exception)

                val message = exception.message?.takeIf { it.isNotBlank() } ?: "Unable to save Purchase."
                val friendlyMessage = if (message.contains("UNIQUE constraint failed", ignoreCase = true)) {
                    "Purchase Invoice No. or Serial Number already exists."
                } else {
                    message
                }

                _uiState.update { it.copy(
                    isSaving = false,
                    isSavedSuccessfully = false,
                    errorMessage = friendlyMessage
                )}
            }
        }
    }


    // =========================================================
    // SAVE / DIRTY STATE
    // =========================================================

    private fun markPurchaseChanged() {

        _uiState.update { currentState ->
            if (currentState.isLoadingPurchaseForEdit || currentState.isSaving) {
                currentState
            } else {
                currentState.copy(
                    isSaved = false,
                    isDirty = true,
                    isSavedSuccessfully = false
                )
            }
        }
    }

    /*
     * Save success is a one-time event for the confirmation message.
     * The persistent isSaved flag controls the disabled SAVED button.
     */
    fun clearSaveSuccess() {

        _uiState.update { it.copy(
            isSavedSuccessfully = false
        )}
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

        _uiState.update { it.copy(
            errorMessage = null
        )}
    }
}

