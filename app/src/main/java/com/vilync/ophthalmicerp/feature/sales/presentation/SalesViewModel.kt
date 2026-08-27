package com.vilync.ophthalmicerp.feature.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SaleItemEntity
import com.vilync.ophthalmicerp.data.entity.SaleLensEntity
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.data.repository.SampleIssueRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round


class SalesViewModel(

    private val salesRepository: SalesRepository,

    private val partyRepository: PartyRepository,

    private val productRepository: ProductRepository,

    private val inventoryRepository: InventoryRepository,

    private val challanRepository: ChallanRepository,

    private val sampleIssueRepository: SampleIssueRepository,

    private val numberingRepository: DocumentNumberingRepository,
    private val editSaleId: Long? = null

) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            SalesUiState(
                isLoading = true
            )
        )

    val uiState: StateFlow<SalesUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // LOCAL ITEM ID
    // =========================================================

    private var nextLocalItemId: Long = 1L

    private var challanObservationJob: Job? = null
    private var editInvoiceLoaded: Boolean = false


    // =========================================================
    // INIT
    // =========================================================

    init {
        observeMasterAndInventoryData()
        
        // Initial Invoice Number Preview
        if (editSaleId == null) {
            val today = java.util.Calendar.getInstance()
            val fyStart = numberingRepository.getFinancialYearStart(today)
            updateFinancialYearStart(fyStart)
        }
    }


    // =========================================================
    // MASTER + INVENTORY DATA
    // =========================================================

    private fun observeMasterAndInventoryData() {

        viewModelScope.launch {

            combine(
                partyRepository.getAllActiveParties(),
                productRepository.getAllActiveProducts(),
                inventoryRepository.getAllInventoryUnits()
            ) { parties, products, inventoryUnits ->

                Triple(
                    parties,
                    products,
                    inventoryUnits
                )

            }
                .catch { exception ->

                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                exception.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Unable to load Sales data."
                        )
                    }
                }
                .collectLatest {
                        (
                            parties,
                            products,
                            inventoryUnits
                        ) ->

                    val eligibleCustomers =
                        parties.filter { party ->

                            party.isActive &&
                                    (
                                            party.partyType ==
                                                    PartyType.CUSTOMER ||

                                                    party.partyType ==
                                                    PartyType.BOTH
                                            )
                        }

                    _uiState.update { 
                        it.copy(
                            customers = eligibleCustomers,
                            products = products,
                            inStockUnits = inventoryUnits,
                            isLoading = false
                        )
                    }

                    if (
                        editSaleId != null &&
                        editSaleId > 0L &&
                        !editInvoiceLoaded
                    ) {
                        loadSaleForEdit(editSaleId)
                    }
                }
        }
    }


    // =========================================================
    // CUSTOMER
    // =========================================================

    fun selectCustomer(
        customer: PartyMaster
    ) {
        _uiState.update { state ->
            state.copy(
                selectedCustomer =
                    customer,

                customerSearchQuery =
                    customer.partyName,

                billToLegalName =
                    customer.legalName
                        .trim()
                        .ifBlank { customer.partyName.trim() },

                billToGstin =
                    customer.gstin.trim(),

                billToAddress =
                    buildPartyAddress(customer),

                billToState =
                    customer.state.trim(),

                sameAsBillTo =
                    true,

                shipToName =
                    customer.partyName.trim(),

                shipToGstin =
                    customer.gstin.trim(),

                shipToAddress =
                    buildPartyAddress(customer),

                shipToState =
                    customer.state.trim(),

                placeOfSupplyState =
                    customer.state.trim(),

                gstSupplyType =
                    "",

                selectedChallanItemIds = emptySet(),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
        }
        observePendingChallansForCustomer(customer.id)
    }


    fun updateCustomerSearchQuery(
        value: String
    ) {
        _uiState.update { state ->
            state.copy(
                customerSearchQuery = value,
                selectedCustomer = if (state.selectedCustomer?.partyName?.equals(value, ignoreCase = true) == true) {
                    state.selectedCustomer
                } else {
                    null
                },
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
        }
        if (_uiState.value.selectedCustomer == null) stopAndClearChallanState()
    }


    fun clearSelectedCustomer() {

        _uiState.value =
            _uiState.value.copy(
                selectedCustomer =
                    null,

                customerSearchQuery =
                    "",

                billToLegalName =
                    "",

                billToGstin =
                    "",

                billToAddress =
                    "",

                billToState =
                    "",

                sameAsBillTo =
                    true,

                shipToName =
                    "",

                shipToGstin =
                    "",

                shipToAddress =
                    "",

                shipToState =
                    "",

                placeOfSupplyState =
                    "",

                gstSupplyType =
                    "",

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
        stopAndClearChallanState()
    }


    // =========================================================
    // BILL TO / SHIP TO / GST SUPPLY
    // =========================================================

    fun updateSameAsBillTo(
        value: Boolean
    ) {

        val state = _uiState.value

        _uiState.value =
            if (value) {
                state.copy(
                    sameAsBillTo = true,
                    shipToName = state.selectedCustomer?.partyName?.trim().orEmpty(),
                    shipToGstin = state.billToGstin,
                    shipToAddress = state.billToAddress,
                    shipToState = state.billToState,
                    placeOfSupplyState = state.billToState,
                    isDirty = true,
                    isSaved = false,
                    isSavedSuccessfully = false,
                    errorMessage = null,
                    successMessage = null
                )
            } else {
                state.copy(
                    sameAsBillTo = false,
                    isDirty = true,
                    isSaved = false,
                    isSavedSuccessfully = false,
                    errorMessage = null,
                    successMessage = null
                )
            }
    }


    fun updateShipToName(value: String) {
        _uiState.value =
            _uiState.value.copy(
                sameAsBillTo = false,
                shipToName = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
    }


    fun updateShipToGstin(value: String) {
        _uiState.value =
            _uiState.value.copy(
                sameAsBillTo = false,
                shipToGstin = value.trim().uppercase(),
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
    }


    fun updateShipToAddress(value: String) {
        _uiState.value =
            _uiState.value.copy(
                sameAsBillTo = false,
                shipToAddress = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
    }


    fun updateShipToState(value: String) {

        val state = _uiState.value

        _uiState.value =
            state.copy(
                sameAsBillTo = false,
                shipToState = value,
                placeOfSupplyState = value.trim(),
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )

        refreshTaxBreakup()
    }


    fun updatePlaceOfSupplyState(value: String) {
        _uiState.value =
            _uiState.value.copy(
                placeOfSupplyState = value.trim(),
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )

        refreshTaxBreakup()
    }


    // =========================================================
    // PURCHASE ORDER
    // =========================================================

    fun updatePoNumber(value: String) {
        _uiState.value =
            _uiState.value.copy(
                poNumber = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
    }


    fun updatePoDate(value: String) {
        _uiState.value =
            _uiState.value.copy(
                poDate = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
    }


    // =========================================================
    // INVOICE DETAILS
    // =========================================================

    fun updateInvoiceNumber(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                invoiceNumber =
                    value,

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    fun updateInvoiceDate(
        value: String
    ) {
        _uiState.update { 
            it.copy(
                invoiceDate = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }


    fun updateFinancialYearStart(
        value: Int
    ) {
        _uiState.update { state ->
            state.copy(
                financialYearStart = value,
                isDirty = true,
                isSaved = false,
                isSavedSuccessfully = false,
                errorMessage = null,
                successMessage = null
            )
        }
        
        if (!_uiState.value.isEditMode) {
            refreshInvoiceNumberPreview(value)
        }
    }

    private fun refreshInvoiceNumberPreview(fyStart: Int) {
        viewModelScope.launch {
            try {
                val preview = numberingRepository.peekNextDocumentNumber(DocumentType.INVOICE, fyStart)
                _uiState.update { it.copy(nextInvoiceNumberPreview = preview) }
            } catch (e: Exception) {
                // Silently fail preview
            }
        }
    }


    fun updateRemarks(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                remarks =
                    value,

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // CHALLAN SETTLEMENT - LOAD / SEARCH / SELECT
    // =========================================================

    private fun observePendingChallansForCustomer(customerId: Long) {
        challanObservationJob?.cancel()
        _uiState.value = _uiState.value.copy(pendingChallans = emptyList(), pendingChallanItems = emptyList(), selectedChallanItemIds = emptySet(), challanSerialQuery = "", challanSerialMatches = emptyList(), isChallanLoading = true, isChallanSerialSearching = false, showChallanSerialMatchSelection = false)
        challanObservationJob = viewModelScope.launch {
            combine(challanRepository.getPendingChallansForCustomer(customerId), challanRepository.getPendingItemsForCustomer(customerId)) { challans, items -> challans to items }
                .catch { e -> _uiState.value = _uiState.value.copy(isChallanLoading = false, errorMessage = e.message?.takeIf { it.isNotBlank() } ?: "Unable to load pending Challans.") }
                .collectLatest { (challans, items) ->
                    val liveIds = items.map { it.id }.toSet()
                    _uiState.value = _uiState.value.copy(pendingChallans = challans, pendingChallanItems = items, selectedChallanItemIds = _uiState.value.selectedChallanItemIds.filter { it in liveIds }.toSet(), isChallanLoading = false)
                }
        }
    }

    private fun stopAndClearChallanState() {
        challanObservationJob?.cancel(); challanObservationJob = null
        _uiState.value = _uiState.value.copy(pendingChallans = emptyList(), pendingChallanItems = emptyList(), selectedChallanItemIds = emptySet(), challanSerialQuery = "", challanSerialMatches = emptyList(), isChallanLoading = false, isChallanSerialSearching = false, showChallanSerialMatchSelection = false)
    }

    fun updateChallanSerialQuery(value: String) {
        _uiState.value = _uiState.value.copy(challanSerialQuery = value.trimStart(), challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, errorMessage = null)
    }

    fun searchPendingChallanSerial() {
        val state = _uiState.value
        val customerId = state.selectedCustomer?.id
        if (customerId == null || customerId <= 0L) { showError("Please select Bill To customer first."); return }
        val query = state.challanSerialQuery.trim()
        if (query.isBlank()) { showError("Enter Serial / Unique ID to search pending Challans."); return }
        if (state.isChallanSerialSearching) return
        _uiState.value = state.copy(isChallanSerialSearching = true, challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, errorMessage = null)
        viewModelScope.launch {
            try {
                val matches = challanRepository.smartSearchPendingSerialForCustomer(customerId, query)
                when (matches.size) {
                    0 -> _uiState.value = _uiState.value.copy(isChallanSerialSearching = false, errorMessage = "No pending Challan lens found for this customer.")
                    1 -> { selectChallanItemForInvoice(matches.first().id); _uiState.value = _uiState.value.copy(isChallanSerialSearching = false, challanSerialQuery = matches.first().serialNumber, challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, errorMessage = null) }
                    else -> _uiState.value = _uiState.value.copy(isChallanSerialSearching = false, challanSerialMatches = matches, showChallanSerialMatchSelection = true, errorMessage = null)
                }
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(isChallanSerialSearching = false, challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, errorMessage = e.message?.takeIf { it.isNotBlank() } ?: "Unable to search pending Challan serial.") }
        }
    }

    fun chooseChallanSerialMatch(challanItemId: Long) {
        val item = _uiState.value.challanSerialMatches.firstOrNull { it.id == challanItemId } ?: return
        selectChallanItemForInvoice(item.id)
        _uiState.value = _uiState.value.copy(challanSerialQuery = item.serialNumber, challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, isChallanSerialSearching = false, errorMessage = null)
    }

    fun dismissChallanSerialMatches() { _uiState.value = _uiState.value.copy(challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, isChallanSerialSearching = false) }

    fun toggleChallanItemForInvoice(challanItemId: Long) {
        val state = _uiState.value; val item = state.pendingChallanItems.firstOrNull { it.id == challanItemId } ?: return
        if (!item.settlementStatus.equals("PENDING", true)) { showError("This Challan lens is no longer pending."); return }
        val updated = state.selectedChallanItemIds.toMutableSet(); if (!updated.add(challanItemId)) updated.remove(challanItemId)
        _uiState.value = state.copy(selectedChallanItemIds = updated, isDirty = true, isSaved = false, isSavedSuccessfully = false, errorMessage = null, successMessage = null)
    }

    fun selectChallanItemForInvoice(challanItemId: Long) {
        val state = _uiState.value; val item = state.pendingChallanItems.firstOrNull { it.id == challanItemId } ?: return
        if (!item.settlementStatus.equals("PENDING", true)) { showError("This Challan lens is no longer pending."); return }
        _uiState.value = state.copy(selectedChallanItemIds = state.selectedChallanItemIds + challanItemId, isDirty = true, isSaved = false, isSavedSuccessfully = false, errorMessage = null, successMessage = null)
    }

    fun clearSelectedChallanItems() { _uiState.value = _uiState.value.copy(selectedChallanItemIds = emptySet(), challanSerialMatches = emptyList(), showChallanSerialMatchSelection = false, isDirty = true, isSaved = false, isSavedSuccessfully = false, errorMessage = null, successMessage = null) }

    fun addSelectedChallanItemsToInvoice() {
        val state = _uiState.value
        val selectedItems = state.selectedChallanItems
        if (selectedItems.isEmpty()) return

        val newItems = selectedItems.map { ci ->
            SalesEntryItem(
                localId = nextLocalItemId++,
                productId = ci.productId,
                productName = ci.productName,
                power = ci.power,
                quantity = 1,
                rate = ci.rate,
                discountPercent = 0.0,
                discountAmount = 0.0,
                taxableAmount = ci.rate,
                gstPercent = ci.gstPercent,
                gstAmount = ci.rate * (ci.gstPercent / 100.0),
                totalAmount = ci.rate * (1 + ci.gstPercent / 100.0),
                selectedUnits = listOf(
                    SalesSelectedInventoryUnit(
                        inventoryUnitId = ci.inventoryUnitId,
                        serialNumber = ci.serialNumber,
                        power = ci.power,
                        batchNumber = ci.batchNumber,
                        expiryDate = ci.expiryDate,
                        sourceChallanItemId = ci.id
                    )
                )
            )
        }

        updateItemsAndSummary(state.items + newItems, clearCurrentEntry = false)
        _uiState.update { it.copy(selectedChallanItemIds = emptySet()) }
    }


    // =========================================================
    // PRODUCT SELECTION
    // =========================================================

    fun selectProduct(
        productId: Long
    ) {

        val product =
            _uiState.value
                .products
                .firstOrNull {
                    it.id == productId
                }
                ?: return

        _uiState.value =
            _uiState.value.copy(
                selectedProduct =
                    product,

                selectedPower =
                    "",

                selectedInventoryUnitIds =
                    emptySet(),

                rate =
                    decimalForInput(
                        product.retailPrice
                    ),

                discountPercent =
                    "",

                gstPercent =
                    decimalForInput(
                        product.gstPercent
                    ),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    fun clearSelectedProduct() {

        resetCurrentEntry(
            markDirty = true
        )
    }


    // =========================================================
    // POWER
    // =========================================================

    fun selectPower(
        power: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedPower =
                    power.trim(),

                selectedInventoryUnitIds =
                    emptySet(),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // SERIAL SELECTION
    // =========================================================

    /**
     * Smart Sales entry by physical Serial / Unique ID.
     *
     * The serial is resolved only from the live IN_STOCK inventory
     * already observed by this ViewModel. Once found, Product, Power,
     * default Retail Rate, GST and the physical Inventory Unit are
     * selected together as one consistent entry state.
     */
    fun updateSmartSerialQuery(
        query: String
    ) {
        _uiState.value = _uiState.value.copy(
            smartSerialQuery = query,
            smartSerialMatches = emptyList(),
            showSmartSerialMatchSelection = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun selectSerialNumber(
        serialNumber: String
    ) {
        val normalizedSerial = serialNumber.trim()
        if (normalizedSerial.isBlank()) {
            showError("Please enter Serial / Unique ID.")
            return
        }

        _uiState.value = _uiState.value.copy(
            smartSerialQuery = normalizedSerial,
            smartSerialMatches = emptyList(),
            showSmartSerialMatchSelection = false,
            isSmartSerialSearching = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                // 1. Search in live stock
                val stockMatches = inventoryRepository.smartSearchInStockSerial(
                    serialQuery = normalizedSerial
                )

                // 2. Search in evaluated samples for this customer
                val customerId = _uiState.value.selectedCustomer?.id ?: 0L
                val sampleMatches = if (customerId > 0L) {
                    sampleIssueRepository.findEvaluatedSampleItemsForCustomer(customerId, normalizedSerial)
                } else emptyList<com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity>()

                val state = _uiState.value
                val inventoryIdsAlreadyAdded = state.items
                    .asSequence()
                    .flatMap { it.selectedUnits.asSequence() }
                    .map { it.inventoryUnitId }
                    .toSet()

                // Filter out already added items from stock matches
                val availableStockMatches = stockMatches.filter {
                    it.id !in inventoryIdsAlreadyAdded
                }
                
                // Map sample items to a common format or handle them separately
                // Actually, the UI expects InventoryUnitEntity for smartSerialMatches
                // So I should fetch the InventoryUnitEntity for these sample matches.
                val availableSampleUnits = mutableListOf<com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity>()
                sampleMatches.forEach { item ->
                    if (item.inventoryUnitId !in inventoryIdsAlreadyAdded) {
                        inventoryRepository.getById(item.inventoryUnitId)?.let {
                            availableSampleUnits.add(it)
                        }
                    }
                }

                val allAvailableMatches = (availableStockMatches + availableSampleUnits).distinctBy { it.id }

                when {
                    allAvailableMatches.isEmpty() -> {
                        val matchedButAlreadyAdded = (stockMatches.isNotEmpty() && stockMatches.all { it.id in inventoryIdsAlreadyAdded }) ||
                                                     (sampleMatches.isNotEmpty() && sampleMatches.all { it.inventoryUnitId in inventoryIdsAlreadyAdded })
                        
                        _uiState.value = _uiState.value.copy(
                            isSmartSerialSearching = false,
                            smartSerialMatches = emptyList(),
                            showSmartSerialMatchSelection = false,
                            errorMessage = if (matchedButAlreadyAdded) {
                                "Matching Serial / Unique ID is already added to this invoice."
                            } else {
                                "No matching in-stock or evaluated sample Serial / Unique ID found."
                            },
                            successMessage = null
                        )
                    }
                    allAvailableMatches.size == 1 -> {
                        val unit = allAvailableMatches.first()
                        val sampleItem = sampleMatches.find { it.inventoryUnitId == unit.id }
                        applySmartSerialSelection(unit, sampleItem?.id)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isSmartSerialSearching = false,
                            smartSerialMatches = allAvailableMatches,
                            showSmartSerialMatchSelection = true,
                            errorMessage = null,
                            successMessage = null
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSmartSerialSearching = false,
                    smartSerialMatches = emptyList(),
                    showSmartSerialMatchSelection = false,
                    errorMessage = exception.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Unable to search Serial / Unique ID.",
                    successMessage = null
                )
            }
        }
    }

    fun selectSmartSerialMatch(
        inventoryUnitId: Long
    ) {
        val inventoryUnit = _uiState.value.smartSerialMatches
            .firstOrNull { it.id == inventoryUnitId }
            ?: return
            
        viewModelScope.launch {
            val customerId = _uiState.value.selectedCustomer?.id ?: 0L
            val sampleItems = if (customerId > 0L) {
                sampleIssueRepository.findEvaluatedSampleItemsForCustomer(customerId, inventoryUnit.serialNumber)
            } else emptyList<com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity>()
            
            val sampleItem = sampleItems.find { it.inventoryUnitId == inventoryUnitId }
            applySmartSerialSelection(inventoryUnit, sampleItem?.id)
        }
    }

    fun dismissSmartSerialMatches() {
        _uiState.value = _uiState.value.copy(
            smartSerialMatches = emptyList(),
            showSmartSerialMatchSelection = false,
            isSmartSerialSearching = false
        )
    }

    private fun applySmartSerialSelection(
        inventoryUnit: com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity,
        sourceSampleIssueItemId: Long? = null
    ) {
        val state = _uiState.value
        val alreadyAdded = state.items
            .asSequence()
            .flatMap { it.selectedUnits.asSequence() }
            .any { it.inventoryUnitId == inventoryUnit.id }

        if (alreadyAdded) {
            _uiState.value = state.copy(
                isSmartSerialSearching = false,
                smartSerialMatches = emptyList(),
                showSmartSerialMatchSelection = false,
                errorMessage = "This Serial / Unique ID is already added to the invoice.",
                successMessage = null
            )
            return
        }

        val product = state.products.firstOrNull {
            it.id == inventoryUnit.productId
        }
        if (product == null) {
            _uiState.value = state.copy(
                isSmartSerialSearching = false,
                smartSerialMatches = emptyList(),
                showSmartSerialMatchSelection = false,
                errorMessage = "Product Master not found for this Serial / Unique ID.",
                successMessage = null
            )
            return
        }

        _uiState.value = state.copy(
            smartSerialQuery = inventoryUnit.serialNumber.trim(),
            smartSerialMatches = emptyList(),
            isSmartSerialSearching = false,
            showSmartSerialMatchSelection = false,
            selectedProduct = product,
            selectedPower = inventoryUnit.power.trim(),
            selectedInventoryUnitIds = setOf(inventoryUnit.id),
            rate = decimalForInput(product.retailPrice),
            discountPercent = "",
            gstPercent = decimalForInput(product.gstPercent),
            isDirty = true,
            isSaved = false,
            isSavedSuccessfully = false,
            errorMessage = null,
            successMessage = null
        )
        
        // Temporarily store the source sample issue item id if any
        // We'll need this when adding the item to the list.
        currentSourceSampleIssueItemId = sourceSampleIssueItemId
    }
    
    private var currentSourceSampleIssueItemId: Long? = null


    fun toggleInventoryUnit(
        inventoryUnitId: Long
    ) {

        val state =
            _uiState.value

        val alreadySelected =
            inventoryUnitId in
                    state.selectedInventoryUnitIds


        if (!alreadySelected) {

            val unit =
                state.availableSerialUnits
                    .firstOrNull {
                        it.id == inventoryUnitId
                    }
                    ?: return


            if (
                state.selectedProduct == null ||
                unit.productId != state.selectedProduct.id
            ) {
                return
            }
        }


        val updatedSelection =
            if (alreadySelected) {

                state.selectedInventoryUnitIds -
                        inventoryUnitId

            } else {

                state.selectedInventoryUnitIds +
                        inventoryUnitId
            }


        _uiState.value =
            state.copy(
                selectedInventoryUnitIds =
                    updatedSelection,

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    fun clearSelectedSerials() {

        _uiState.value =
            _uiState.value.copy(
                selectedInventoryUnitIds =
                    emptySet(),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // RATE / DISCOUNT / GST
    // =========================================================

    fun updateRate(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                rate =
                    sanitizeDecimalInput(
                        value
                    ),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    fun updateDiscountPercent(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                discountPercent =
                    sanitizeDecimalInput(
                        value
                    ),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    fun updateGstPercent(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                gstPercent =
                    sanitizeDecimalInput(
                        value
                    ),

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // ADD CURRENT ITEM
    // =========================================================

    fun addCurrentItem() {

        val state =
            _uiState.value

        val product =
            state.selectedProduct
                ?: run {

                    showError(
                        "Please select a Product."
                    )

                    return
                }


        val selectedUnits =
            state.selectedSerialUnits


        if (selectedUnits.isEmpty()) {

            showError(
                "Please select at least one available Serial Number."
            )

            return
        }


        if (
            product.powerApplicable &&
            state.selectedPower.isBlank()
        ) {

            showError(
                "Please select Power for ${product.productName}."
            )

            return
        }


        if (product.powerApplicable) {

            val invalidPowerExists =
                selectedUnits.any { unit ->

                    !unit.power
                        .trim()
                        .equals(
                            other =
                                state.selectedPower.trim(),

                            ignoreCase =
                                true
                        )
                }


            if (invalidPowerExists) {

                showError(
                    "Selected Serial Number does not match the selected Power."
                )

                return
            }
        }


        val rate =
            state.rate
                .toDoubleOrNull()


        if (
            rate == null ||
            rate < 0.0
        ) {

            showError(
                "Please enter a valid Rate."
            )

            return
        }


        val discountPercent =
            state.discountPercent
                .toDoubleOrNull()
                ?: 0.0


        if (
            discountPercent < 0.0 ||
            discountPercent > 100.0
        ) {

            showError(
                "Discount must be between 0 and 100."
            )

            return
        }


        val gstPercent =
            state.gstPercent
                .toDoubleOrNull()
                ?: 0.0


        if (
            gstPercent < 0.0 ||
            gstPercent > 100.0
        ) {

            showError(
                "GST must be between 0 and 100."
            )

            return
        }


        val inventoryIdsAlreadyAdded =
            state.items
                .flatMap {
                    it.selectedUnits
                }
                .map {
                    it.inventoryUnitId
                }
                .toSet()


        val duplicateUnit =
            selectedUnits
                .firstOrNull {
                    it.id in inventoryIdsAlreadyAdded
                }


        if (duplicateUnit != null) {

            showError(
                "Serial Number ${duplicateUnit.serialNumber} is already added to this Sale."
            )

            return
        }


        val quantity =
            selectedUnits.size


        val grossAmount =
            money(
                rate * quantity
            )


        val discountAmount =
            money(
                grossAmount *
                        discountPercent /
                        100.0
            )


        val taxableAmount =
            money(
                grossAmount -
                        discountAmount
            )


        val gstAmount =
            money(
                taxableAmount *
                        gstPercent /
                        100.0
            )


        val totalAmount =
            money(
                taxableAmount +
                        gstAmount
            )


        val selectedInventorySnapshots =
            selectedUnits.map { unit ->

                SalesSelectedInventoryUnit(

                    inventoryUnitId =
                        unit.id,

                    serialNumber =
                        unit.serialNumber.trim(),

                    power =
                        unit.power.trim(),

                    batchNumber =
                        unit.batchNumber.trim(),

                    expiryDate =
                        unit.expiryDate.trim(),

                    sourceSampleIssueItemId =
                        currentSourceSampleIssueItemId
                )
            }


        val newItem =
            SalesEntryItem(

                localId =
                    nextLocalItemId++,

                productId =
                    product.id,

                productName =
                    product.productName.trim(),

                power =
                    state.selectedPower.trim(),

                quantity =
                    quantity,

                rate =
                    money(rate),

                discountPercent =
                    discountPercent,

                discountAmount =
                    discountAmount,

                taxableAmount =
                    taxableAmount,

                gstPercent =
                    gstPercent,

                gstAmount =
                    gstAmount,

                totalAmount =
                    totalAmount,

                selectedUnits =
                    selectedInventorySnapshots
            )


        updateItemsAndSummary(
            items =
                state.items +
                        newItem,

            clearCurrentEntry =
                true
        )
        
        currentSourceSampleIssueItemId = null
    }


    // =========================================================
    // REMOVE ITEM
    // =========================================================

    fun removeItem(
        localId: Long
    ) {

        val state =
            _uiState.value


        val updatedItems =
            state.items.filterNot {
                it.localId == localId
            }


        if (
            updatedItems.size ==
            state.items.size
        ) {
            return
        }


        updateItemsAndSummary(
            items =
                updatedItems,

            clearCurrentEntry =
                false
        )
    }


    // =========================================================
    // EDIT ITEM
    // =========================================================

    fun editItem(
        localId: Long
    ) {

        val state =
            _uiState.value


        val item =
            state.items
                .firstOrNull {
                    it.localId == localId
                }
                ?: return


        val product =
            state.products
                .firstOrNull {
                    it.id == item.productId
                }
                ?: run {

                    showError(
                        "Product Master record is no longer available."
                    )

                    return
                }


        val inventoryIds =
            item.selectedUnits
                .map {
                    it.inventoryUnitId
                }
                .toSet()


        val updatedItems =
            state.items.filterNot {
                it.localId == localId
            }


        val summary =
            calculateBillSummary(
                updatedItems
            )


        _uiState.value =
            state.copy(
                selectedProduct =
                    product,

                selectedPower =
                    item.power,

                selectedInventoryUnitIds =
                    inventoryIds,

                rate =
                    decimalForInput(
                        item.rate
                    ),

                discountPercent =
                    decimalForInput(
                        item.discountPercent
                    ),

                gstPercent =
                    decimalForInput(
                        item.gstPercent
                    ),

                items =
                    updatedItems,

                subTotal =
                    summary.subTotal,

                discountAmount =
                    summary.discountAmount,

                taxableAmount =
                    summary.taxableAmount,

                gstAmount =
                    summary.gstAmount,

                cgstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).cgstAmount,

                sgstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).sgstAmount,

                igstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).igstAmount,

                adjustment =
                    summary.adjustment,

                roundOff =
                    summary.roundOff,

                totalAmount =
                    summary.totalAmount,

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // CLEAR CURRENT ENTRY
    // =========================================================

    fun clearCurrentEntry() {

        resetCurrentEntry(
            markDirty = true
        )
    }



    // =========================================================
    // LOAD EXISTING SALES INVOICE FOR EDIT
    // =========================================================

    private fun loadSaleForEdit(
        saleId: Long
    ) {

        if (editInvoiceLoaded) {
            return
        }

        editInvoiceLoaded = true

        viewModelScope.launch {

            try {

                val sale =
                    salesRepository.getSaleById(saleId)
                        ?: throw IllegalStateException(
                            "Sales Invoice could not be found."
                        )

                require(
                    sale.status.trim().equals(
                        other = "POSTED",
                        ignoreCase = true
                    )
                ) {
                    "Only a POSTED Sales Invoice can be edited."
                }

                val saleItems =
                    salesRepository.getSaleItemsListForEdit(
                        saleId = saleId
                    )

                val customers =
                    _uiState.value.customers

                val customer =
                    customers.firstOrNull {
                        it.id == sale.customerId
                    } ?: throw IllegalStateException(
                        "Customer / Hospital Master record for this invoice is no longer available."
                    )

                var localId = 1L

                val entryItems =
                    saleItems.map { item ->

                        val lenses =
                            salesRepository.getSaleLensesListForEdit(
                                saleItemId = item.id
                            )

                        SalesEntryItem(
                            localId = localId++,
                            productId = item.productId,
                            productName = item.productName,
                            power = item.power,
                            quantity = item.quantity,
                            rate = item.rate,
                            discountPercent = item.discountPercent,
                            discountAmount = item.discountAmount,
                            taxableAmount = item.taxableAmount,
                            gstPercent = item.gstPercent,
                            gstAmount = item.gstAmount,
                            totalAmount = item.totalAmount,
                            selectedUnits =
                                lenses.map { lens ->
                                    SalesSelectedInventoryUnit(
                                        inventoryUnitId = lens.inventoryUnitId,
                                        serialNumber = lens.serialNumber,
                                        power = lens.power,
                                        batchNumber = lens.batchNumber,
                                        expiryDate = lens.expiryDate
                                    )
                                }
                        )
                    }

                nextLocalItemId = localId

                _uiState.value =
                    _uiState.value.copy(
                        editingSaleId = sale.id,
                        isEditMode = true,
                        selectedCustomer = customer,
                        customerSearchQuery = customer.partyName,
                        billToLegalName = sale.billToLegalName,
                        billToGstin = sale.billToGstin,
                        billToAddress = sale.billToAddress,
                        billToState = sale.billToState,
                        sameAsBillTo = sale.sameAsBillTo,
                        shipToName = sale.shipToName,
                        shipToGstin = sale.shipToGstin,
                        shipToAddress = sale.shipToAddress,
                        shipToState = sale.shipToState,
                        invoiceNumber = sale.invoiceNumber,
                        invoiceDate = sale.invoiceDate,
                        financialYearStart = sale.financialYearStart,
                        poNumber = sale.poNumber,
                        poDate = sale.poDate,
                        placeOfSupplyState = sale.placeOfSupplyState,
                        gstSupplyType = sale.gstSupplyType,
                        remarks = sale.remarks,
                        selectedProduct = null,
                        selectedPower = "",
                        selectedInventoryUnitIds = emptySet(),
                        rate = "",
                        discountPercent = "",
                        gstPercent = "",
                        items = entryItems,
                        subTotal = sale.subTotal,
                        discountAmount = sale.discountAmount,
                        taxableAmount = sale.taxableAmount,
                        gstAmount = sale.gstAmount,
                        cgstAmount = sale.cgstAmount,
                        sgstAmount = sale.sgstAmount,
                        igstAmount = sale.igstAmount,
                        adjustment = sale.adjustment,
                        roundOff = sale.roundOff,
                        totalAmount = sale.totalAmount,
                        isLoading = false,
                        isSaving = false,
                        isDirty = false,
                        isSaved = true,
                        isSavedSuccessfully = false,
                        errorMessage = null,
                        successMessage = null
                    )

            } catch (exception: Exception) {

                editInvoiceLoaded = false

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            exception.message
                                ?.takeIf { it.isNotBlank() }
                                ?: "Unable to load Sales Invoice for editing."
                    )
            }
        }
    }


    // =========================================================
    // SAVE COMPLETE SALE
    // =========================================================

    fun saveSale() {

        val state =
            _uiState.value


        if (state.isSaving) {
            return
        }


        // =====================================================
        // CUSTOMER
        // =====================================================

        val customer =
            state.selectedCustomer


        if (customer == null) {

            showError(
                "Please select a Customer / Hospital."
            )

            return
        }


        if (
            customer.partyType != PartyType.CUSTOMER &&
            customer.partyType != PartyType.BOTH
        ) {

            showError(
                "Selected Party is not eligible for Sales."
            )

            return
        }


        // =====================================================
        // INVOICE
        // =====================================================

        val invoiceNumber =
            state.invoiceNumber
                .trim()


        if (invoiceNumber.isBlank() && state.isEditMode) {

            showError(
                "Sales Invoice Number is required."
            )

            return
        }


        val invoiceDate =
            state.invoiceDate
                .trim()


        if (invoiceDate.isBlank()) {

            showError(
                "Sales Invoice Date is required."
            )

            return
        }


        if (state.financialYearStart <= 0) {

            showError(
                "Valid Financial Year is required."
            )

            return
        }


        // =====================================================
        // CURRENT UNSAVED PRODUCT ENTRY
        // =====================================================

        if (
            state.selectedProduct != null ||
            state.selectedInventoryUnitIds.isNotEmpty() ||
            state.rate.isNotBlank()
        ) {

            showError(
                "Current Product entry is not added. Please Add Item or clear the current entry before saving."
            )

            return
        }


        // =====================================================
        // ITEMS
        // =====================================================

        if (state.items.isEmpty()) {

            showError(
                "Please add at least one Sales item."
            )

            return
        }


        val invalidItem =
            state.items
                .firstOrNull { item ->

                    item.productId <= 0L ||
                            item.quantity <= 0 ||
                            item.selectedUnits.isEmpty() ||
                            item.selectedUnits.size !=
                            item.quantity
                }


        if (invalidItem != null) {

            showError(
                "Invalid Sales item found: ${invalidItem.productName}."
            )

            return
        }


        // =====================================================
        // DUPLICATE SERIAL SAFETY
        // =====================================================

        val allInventoryUnitIds =
            state.items
                .flatMap {
                    it.selectedUnits
                }
                .map {
                    it.inventoryUnitId
                }


        if (
            allInventoryUnitIds.size !=
            allInventoryUnitIds.distinct().size
        ) {

            showError(
                "The same physical Serial Number cannot be used more than once in one Sales Invoice."
            )

            return
        }


        // =====================================================
        // START SAVE
        // =====================================================

        _uiState.value =
            state.copy(
                isSaving =
                    true,

                errorMessage =
                    null,

                successMessage =
                    null,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false
            )


        viewModelScope.launch {

            try {

                // =============================================
                // FINAL DUPLICATE INVOICE CHECK
                // =============================================

                val duplicateInvoiceExists =
                    if (
                        state.isEditMode &&
                        state.editingSaleId != null
                    ) {
                        salesRepository
                            .saleInvoiceExistsExcludingSale(
                                customerId = customer.id,
                                invoiceNumber = invoiceNumber,
                                excludeSaleId = state.editingSaleId
                            )
                    } else {
                        salesRepository
                            .saleInvoiceExists(
                                customerId = customer.id,
                                invoiceNumber = invoiceNumber
                            )
                    }


                if (duplicateInvoiceExists) {

                    _uiState.value =
                        _uiState.value.copy(
                            isSaving =
                                false,

                            errorMessage =
                                "Sales Invoice Number $invoiceNumber already exists for ${customer.partyName}."
                        )

                    return@launch
                }


                // =============================================
                // SALE HEADER
                // =============================================

                val now =
                    System.currentTimeMillis()


                val sale =
                    SaleEntity(

                        customerId =
                            customer.id,

                        customerName =
                            customer.partyName.trim(),

                        billToLegalName =
                            state.billToLegalName.trim(),

                        billToGstin =
                            state.billToGstin.trim(),

                        billToAddress =
                            state.billToAddress.trim(),

                        billToState =
                            state.billToState.trim(),

                        sameAsBillTo =
                            state.sameAsBillTo,

                        shipToName =
                            state.shipToName.trim(),

                        shipToGstin =
                            state.shipToGstin.trim(),

                        shipToAddress =
                            state.shipToAddress.trim(),

                        shipToState =
                            state.shipToState.trim(),

                        invoiceNumber =
                            invoiceNumber,

                        normalizedInvoiceNumber =
                            invoiceNumber.uppercase(),

                        invoiceDate =
                            invoiceDate,

                        financialYearStart =
                            state.financialYearStart,

                        poNumber =
                            state.poNumber.trim(),

                        poDate =
                            state.poDate.trim(),

                        placeOfSupplyState =
                            state.placeOfSupplyState.trim(),

                        gstSupplyType =
                            resolveGstSupplyType(state),

                        subTotal =
                            money(
                                state.subTotal
                            ),

                        discountAmount =
                            money(
                                state.discountAmount
                            ),

                        taxableAmount =
                            money(
                                state.taxableAmount
                            ),

                        gstAmount =
                            money(
                                state.gstAmount
                            ),

                        cgstAmount =
                            money(
                                state.cgstAmount
                            ),

                        sgstAmount =
                            money(
                                state.sgstAmount
                            ),

                        igstAmount =
                            money(
                                state.igstAmount
                            ),

                        adjustment =
                            money(
                                state.adjustment
                            ),

                        roundOff =
                            money(
                                state.roundOff
                            ),

                        totalAmount =
                            money(
                                state.totalAmount
                            ),

                        remarks =
                            state.remarks.trim(),

                        status =
                            "POSTED",

                        cancelledAt =
                            null,

                        cancellationReason =
                            "",

                        createdAt =
                            now,

                        updatedAt =
                            now
                    )


                // =============================================
                // SALE ITEMS + PHYSICAL LENSES
                // =============================================

                val itemsWithLenses =
                    state.items.map { item ->

                        val batchNumbers =
                            item.selectedUnits
                                .map {
                                    it.batchNumber.trim()
                                }
                                .filter {
                                    it.isNotBlank()
                                }
                                .distinct()


                        val saleItem =
                            SaleItemEntity(

                                saleId =
                                    0L,

                                productId =
                                    item.productId,

                                productName =
                                    item.productName.trim(),

                                power =
                                    item.power.trim(),

                                quantity =
                                    item.quantity,

                                rate =
                                    money(
                                        item.rate
                                    ),

                                discountPercent =
                                    item.discountPercent,

                                discountAmount =
                                    money(
                                        item.discountAmount
                                    ),

                                taxableAmount =
                                    money(
                                        item.taxableAmount
                                    ),

                                gstPercent =
                                    item.gstPercent,

                                gstAmount =
                                    money(
                                        item.gstAmount
                                    ),

                                totalAmount =
                                    money(
                                        item.totalAmount
                                    ),

                                batchNumber =
                                    if (
                                        batchNumbers.size == 1
                                    ) {
                                        batchNumbers.first()
                                    } else {
                                        ""
                                    },

                                lotNumber =
                                    ""
                            )


                        val saleLenses =
                            item.selectedUnits.map { unit ->

                                SaleLensEntity(

                                    saleItemId =
                                        0L,

                                    inventoryUnitId =
                                        unit.inventoryUnitId,

                                    serialNumber =
                                        unit.serialNumber.trim(),

                                    power =
                                        unit.power.trim(),

                                    batchNumber =
                                        unit.batchNumber.trim(),

                                    expiryDate =
                                        unit.expiryDate.trim(),

                                    sourceChallanItemId =
                                        unit.sourceChallanItemId
                                )
                            }


                        saleItem to
                                saleLenses
                    }


                // =============================================
                // ATOMIC SALES POSTING
                // =============================================
                //
                // SalesRepository will:
                //
                // - revalidate live Inventory Units
                // - verify IN_STOCK
                // - verify Product / Power / Serial
                // - save Sale document
                // - mark Inventory Units SOLD
                // - create SOLD Stock Movements
                //
                // all inside one Room transaction.
                // =============================================

                val saleId =
                    if (
                        state.isEditMode &&
                        state.editingSaleId != null
                    ) {
                        salesRepository
                            .updateCompleteSale(
                                saleId = state.editingSaleId,
                                sale = sale,
                                itemsWithLenses = itemsWithLenses
                            )
                    } else {
                        salesRepository
                            .saveCompleteSale(
                                sale = sale,
                                itemsWithLenses = itemsWithLenses
                            )
                    }


                // =============================================
                // SUCCESS
                // =============================================

                if (saleId <= 0L) {

                    throw IllegalStateException(
                        "Sales Invoice could not be saved."
                    )
                }


                if (
                    state.isEditMode &&
                    state.editingSaleId != null
                ) {
                    _uiState.value =
                        _uiState.value.copy(
                            editingSaleId = saleId,
                            isEditMode = true,
                            isSaving = false,
                            isDirty = false,
                            isSaved = true,
                            isSavedSuccessfully = true,
                            errorMessage = null,
                            successMessage =
                                "Sales Invoice $invoiceNumber updated successfully."
                        )
                } else {

                    nextLocalItemId =
                        1L

                    _uiState.value =
                        _uiState.value.copy(
                            selectedCustomer = null,
                            customerSearchQuery = "",
                            billToLegalName = "",
                            billToGstin = "",
                            billToAddress = "",
                            billToState = "",
                            sameAsBillTo = true,
                            shipToName = "",
                            shipToGstin = "",
                            shipToAddress = "",
                            shipToState = "",
                            poNumber = "",
                            poDate = "",
                            placeOfSupplyState = "",
                            gstSupplyType = "",
                            invoiceNumber = "",
                            invoiceDate = "",
                            remarks = "",
                            selectedProduct = null,
                            selectedPower = "",
                            selectedInventoryUnitIds = emptySet(),
                            rate = "",
                            discountPercent = "",
                            gstPercent = "",
                            items = emptyList(),
                            subTotal = 0.0,
                            discountAmount = 0.0,
                            taxableAmount = 0.0,
                            gstAmount = 0.0,
                            cgstAmount = 0.0,
                            sgstAmount = 0.0,
                            igstAmount = 0.0,
                            adjustment = 0.0,
                            roundOff = 0.0,
                            totalAmount = 0.0,
                            isSaving = false,
                            isDirty = false,
                            isSaved = true,
                            isSavedSuccessfully = true,
                            savedSaleId = saleId,
                            errorMessage = null,
                            successMessage =
                                "Sales Invoice $invoiceNumber saved successfully."
                        )
                }


            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,

                        isSaved =
                            false,

                        isSavedSuccessfully =
                            false,

                        successMessage =
                            null,

                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to save Sales Invoice."
                    )
            }
        }
    }


    // =========================================================
    // UPDATE ITEMS + SUMMARY
    // =========================================================

    private fun updateItemsAndSummary(
        items: List<SalesEntryItem>,
        clearCurrentEntry: Boolean
    ) {

        val state =
            _uiState.value


        val summary =
            calculateBillSummary(
                items
            )


        _uiState.value =
            state.copy(
                selectedProduct =
                    if (clearCurrentEntry) {
                        null
                    } else {
                        state.selectedProduct
                    },

                selectedPower =
                    if (clearCurrentEntry) {
                        ""
                    } else {
                        state.selectedPower
                    },

                selectedInventoryUnitIds =
                    if (clearCurrentEntry) {
                        emptySet()
                    } else {
                        state.selectedInventoryUnitIds
                    },

                rate =
                    if (clearCurrentEntry) {
                        ""
                    } else {
                        state.rate
                    },

                discountPercent =
                    if (clearCurrentEntry) {
                        ""
                    } else {
                        state.discountPercent
                    },

                gstPercent =
                    if (clearCurrentEntry) {
                        ""
                    } else {
                        state.gstPercent
                    },

                items =
                    items,

                subTotal =
                    summary.subTotal,

                discountAmount =
                    summary.discountAmount,

                taxableAmount =
                    summary.taxableAmount,

                gstAmount =
                    summary.gstAmount,

                cgstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).cgstAmount,

                sgstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).sgstAmount,

                igstAmount =
                    taxBreakup(
                        gstAmount = summary.gstAmount,
                        supplyType = resolveGstSupplyType(state)
                    ).igstAmount,

                adjustment =
                    summary.adjustment,

                roundOff =
                    summary.roundOff,

                totalAmount =
                    summary.totalAmount,

                isDirty =
                    true,

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // BILL SUMMARY
    // =========================================================

    private fun calculateBillSummary(
        items: List<SalesEntryItem>
    ): SalesBillSummary {

        val subTotal =
            money(
                items.sumOf { item ->

                    item.rate *
                            item.quantity
                }
            )


        val discountAmount =
            money(
                items.sumOf {
                    it.discountAmount
                }
            )


        val taxableAmount =
            money(
                items.sumOf {
                    it.taxableAmount
                }
            )


        val gstAmount =
            money(
                items.sumOf {
                    it.gstAmount
                }
            )


        val adjustment =
            0.0


        val amountBeforeRoundOff =
            money(
                taxableAmount +
                        gstAmount +
                        adjustment
            )


        val roundedTotal =
            round(
                amountBeforeRoundOff
            )


        val roundOff =
            money(
                roundedTotal -
                        amountBeforeRoundOff
            )


        val totalAmount =
            money(
                amountBeforeRoundOff +
                        roundOff
            )


        return SalesBillSummary(

            subTotal =
                subTotal,

            discountAmount =
                discountAmount,

            taxableAmount =
                taxableAmount,

            gstAmount =
                gstAmount,

            adjustment =
                adjustment,

            roundOff =
                roundOff,

            totalAmount =
                totalAmount
        )
    }


    // =========================================================
    // RESET CURRENT ENTRY
    // =========================================================

    private fun resetCurrentEntry(
        markDirty: Boolean
    ) {

        val state =
            _uiState.value


        _uiState.value =
            state.copy(
                selectedProduct =
                    null,

                selectedPower =
                    "",

                selectedInventoryUnitIds =
                    emptySet(),

                rate =
                    "",

                discountPercent =
                    "",

                gstPercent =
                    "",

                isDirty =
                    if (markDirty) {
                        true
                    } else {
                        state.isDirty
                    },

                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                errorMessage =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // GST SUPPLY / TAX BREAKUP
    // =========================================================
    //
    // The company/home state is not present in the current
    // SalesViewModel source. Therefore the ViewModel preserves
    // explicit gstSupplyType when provided by the UI.
    //
    // If no explicit type is available, the safe default is
    // INTRA_STATE. This can later be connected to Company Master
    // state without changing the Sales database schema.
    // =========================================================

    private fun resolveGstSupplyType(
        state: SalesUiState
    ): String {

        val explicit =
            state.gstSupplyType
                .trim()
                .uppercase()

        return when (explicit) {
            "INTER_STATE" -> "INTER_STATE"
            "INTRA_STATE" -> "INTRA_STATE"
            else -> "INTRA_STATE"
        }
    }


    private fun taxBreakup(
        gstAmount: Double,
        supplyType: String
    ): SalesTaxBreakup {

        return if (
            supplyType.equals(
                other = "INTER_STATE",
                ignoreCase = true
            )
        ) {

            SalesTaxBreakup(
                cgstAmount = 0.0,
                sgstAmount = 0.0,
                igstAmount = money(gstAmount)
            )

        } else {

            val cgst =
                money(
                    gstAmount / 2.0
                )

            val sgst =
                money(
                    gstAmount - cgst
                )

            SalesTaxBreakup(
                cgstAmount = cgst,
                sgstAmount = sgst,
                igstAmount = 0.0
            )
        }
    }


    private fun refreshTaxBreakup() {

        val state =
            _uiState.value

        val supplyType =
            resolveGstSupplyType(state)

        val breakup =
            taxBreakup(
                gstAmount = state.gstAmount,
                supplyType = supplyType
            )

        _uiState.value =
            state.copy(
                gstSupplyType = supplyType,
                cgstAmount = breakup.cgstAmount,
                sgstAmount = breakup.sgstAmount,
                igstAmount = breakup.igstAmount
            )
    }


    private fun buildPartyAddress(
        party: com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
    ): String {

        return listOf(
            party.addressLine1.trim(),
            party.addressLine2.trim(),
            party.city.trim(),
            party.district.trim(),
            party.state.trim(),
            party.pinCode.trim()
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                separator = ", "
            )
    }


    // = :::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // SAVE SUCCESS CONSUMED
    // = :::::::::::::::::::::::::::::::::::::::::::::::::::::::

    fun consumeSaveSuccess() {

        _uiState.value =
            _uiState.value.copy(
                isSaved =
                    false,

                isSavedSuccessfully =
                    false,

                savedSaleId =
                    null,

                successMessage =
                    null
            )
    }


    // =========================================================
    // CLEAR ERROR
    // =========================================================


    private fun showError(
        message: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                errorMessage =
                    message,

                successMessage =
                    null
            )
    }


    // =========================================================
    // DECIMAL INPUT
    // =========================================================

    private fun sanitizeDecimalInput(
        value: String
    ): String {

        val trimmed =
            value.trim()


        if (trimmed.isBlank()) {
            return ""
        }


        val filtered =
            trimmed.filter {
                it.isDigit() ||
                        it == '.'
            }


        val firstDecimalIndex =
            filtered.indexOf('.')


        if (firstDecimalIndex < 0) {
            return filtered
        }


        val integerPart =
            filtered.substring(
                startIndex = 0,
                endIndex = firstDecimalIndex
            )


        val decimalPart =
            filtered.substring(
                startIndex =
                    firstDecimalIndex + 1
            )
                .replace(
                    ".",
                    ""
                )


        return "$integerPart.$decimalPart"
    }


    // =========================================================
    // DISPLAY DECIMAL
    // =========================================================

    private fun decimalForInput(
        value: Double
    ): String {

        if (value == 0.0) {
            return "0"
        }


        return if (
            value % 1.0 == 0.0
        ) {

            value
                .toLong()
                .toString()

        } else {

            value.toString()
        }
    }


    // =========================================================
    // MONEY PRECISION
    // =========================================================

    private fun money(
        value: Double
    ): Double {

        return round(
            value * 100.0
        ) / 100.0
    }


    // =========================================================
    // INTERNAL BILL SUMMARY
    // =========================================================

    private data class SalesBillSummary(

        val subTotal: Double,

        val discountAmount: Double,

        val taxableAmount: Double,

        val gstAmount: Double,

        val adjustment: Double,

        val roundOff: Double,

        val totalAmount: Double
    )

    private data class SalesTaxBreakup(
        val cgstAmount: Double,
        val sgstAmount: Double,
        val igstAmount: Double
    )

}