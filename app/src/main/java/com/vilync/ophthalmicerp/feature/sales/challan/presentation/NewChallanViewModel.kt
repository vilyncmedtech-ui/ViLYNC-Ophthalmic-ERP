package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NewChallanViewModel(
    private val challanRepository: ChallanRepository,
    private val inventoryRepository: InventoryRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductMasterRepository,
    private val numberingRepository: DocumentNumberingRepository,
    private val editChallanId: Long? = null
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            NewChallanUiState(
                challanDate = today(),
                financialYearStart = financialYearStart(Date())
            )
        )

    val uiState: StateFlow<NewChallanUiState> =
        _uiState.asStateFlow()

    init {
        loadCustomers()
        
        if (editChallanId != null && editChallanId > 0L) {
            loadChallanForEdit(editChallanId)
        } else {
            refreshChallanNumberPreview(_uiState.value.financialYearStart)
        }
    }

    private fun loadChallanForEdit(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val challan = challanRepository.getChallanById(id)
                    ?: error("Challan not found.")
                
                val items = challanRepository.getItemsByChallanId(id)
                
                val allParties = _uiState.value.customers.ifEmpty {
                    partyRepository.getAllActiveParties().firstOrNull() ?: emptyList()
                }
                
                val customer = allParties.firstOrNull { it.id == challan.customerId }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isEditMode = true,
                        editingChallanId = id,
                        selectedCustomer = customer,
                        challanNumber = challan.challanNumber,
                        challanDate = challan.challanDate,
                        financialYearStart = challan.financialYearStart,
                        asLibrary = challan.asLibrary,
                        remarks = challan.remarks,
                        items = items.map { item ->
                            NewChallanItemUi(
                                inventoryUnitId = item.inventoryUnitId,
                                productId = item.productId,
                                productName = item.productName,
                                serialNumber = item.serialNumber,
                                power = item.power,
                                batchNumber = item.batchNumber,
                                expiryDate = item.expiryDate,
                                rate = item.rate,
                                gstPercent = item.gstPercent
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load Challan.") }
            }
        }
    }

    private fun refreshChallanNumberPreview(fyStart: Int) {
        viewModelScope.launch {
            try {
                val preview = numberingRepository.peekNextDocumentNumber(DocumentType.CHALLAN, fyStart)
                _uiState.value = _uiState.value.copy(
                    challanNumber = preview
                )
            } catch (e: Exception) {
                // Silently fail preview
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            partyRepository
                .getAllActiveParties()
                .collect { parties ->

                    val customers =
                        parties.filter {
                            it.isActive &&
                                    (
                                            it.partyType == PartyType.CUSTOMER ||
                                                    it.partyType == PartyType.BOTH
                                            )
                        }

                    _uiState.value =
                        _uiState.value.copy(
                            customers = customers
                        )
                }
        }
    }

    fun selectCustomer(
        customer: PartyMaster
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedCustomer = customer,
                errorMessage = null,
                successMessage = null
            )
    }

    fun updateChallanNumber(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                challanNumber = value.uppercase(Locale.getDefault()),
                errorMessage = null,
                successMessage = null
            )
    }

    fun updateChallanDate(
        value: String
    ) {
        val fyStart = financialYearStartFromDisplayDate(value)
        _uiState.value =
            _uiState.value.copy(
                challanDate = value,
                financialYearStart = fyStart,
                errorMessage = null,
                successMessage = null
            )
        refreshChallanNumberPreview(fyStart)
    }

    fun updateRemarks(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                remarks = value,
                errorMessage = null
            )
    }


    // =========================================================
    // LENS LIBRARY
    // =========================================================

    fun updateAsLibrary(
        value: Boolean
    ) {
        _uiState.value =
            _uiState.value.copy(
                asLibrary = value,
                errorMessage = null
            )
    }

    fun updateSerialQuery(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                serialQuery = value.uppercase(Locale.getDefault()),
                serialMatches = emptyList(),
                errorMessage = null
            )
    }

    fun updateRate(
        value: String
    ) {
        if (
            value.isBlank() ||
            value.matches(Regex("^\\d*\\.?\\d*$"))
        ) {
            _uiState.value =
                _uiState.value.copy(
                    rate = value,
                    errorMessage = null
                )
        }
    }

    fun updateGstPercent(
        value: String
    ) {
        if (
            value.isBlank() ||
            value.matches(Regex("^\\d*\\.?\\d*$"))
        ) {
            _uiState.value =
                _uiState.value.copy(
                    gstPercent = value,
                    errorMessage = null
                )
        }
    }

    fun searchSerial() {

        val query =
            _uiState.value.serialQuery.trim()

        if (query.isBlank()) {
            showError("Enter Serial Number / Unique ID.")
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSearchingSerial = true,
                    serialMatches = emptyList(),
                    errorMessage = null
                )

            try {

                val matches =
                    inventoryRepository
                        .smartSearchInStockSerial(query)
                        .filterNot { unit ->
                            _uiState.value.items.any {
                                it.inventoryUnitId == unit.id
                            }
                        }

                when {
                    matches.isEmpty() -> {
                        showError(
                            "No available IN_STOCK unit found for $query."
                        )
                    }

                    matches.size == 1 -> {
                        addInventoryUnit(matches.first())
                    }

                    else -> {
                        _uiState.value =
                            _uiState.value.copy(
                                serialMatches = matches,
                                isSearchingSerial = false
                            )
                    }
                }

            } catch (exception: Exception) {

                showError(
                    exception.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Unable to search inventory."
                )
            }
        }
    }

    fun chooseSerialMatch(
        unit: InventoryUnitEntity
    ) {
        viewModelScope.launch {
            addInventoryUnit(unit)
        }
    }

    private suspend fun addInventoryUnit(
        unit: InventoryUnitEntity
    ) {

        if (
            _uiState.value.items.any {
                it.inventoryUnitId == unit.id
            }
        ) {
            showError(
                "Serial ${unit.serialNumber} is already added."
            )
            return
        }

        val product =
            productRepository.getProductById(
                unit.productId
            )

        if (product == null) {
            showError(
                "Product Master not found for serial ${unit.serialNumber}."
            )
            return
        }

        val enteredRate =
            _uiState.value.rate
                .trim()
                .toDoubleOrNull()
                ?: product.retailPrice

        val enteredGst =
            _uiState.value.gstPercent
                .trim()
                .toDoubleOrNull()
                ?: product.gstPercent

        val item =
            NewChallanItemUi(
                inventoryUnitId = unit.id,
                productId = unit.productId,
                productName = product.productName,
                serialNumber = unit.serialNumber,
                power = unit.power,
                batchNumber = unit.batchNumber,
                expiryDate = unit.expiryDate,
                rate = enteredRate,
                gstPercent = enteredGst
            )

        _uiState.value =
            _uiState.value.copy(
                items = _uiState.value.items + item,
                serialQuery = "",
                serialMatches = emptyList(),
                rate = "",
                gstPercent = "",
                isSearchingSerial = false,
                errorMessage = null
            )
    }

    fun removeItem(
        inventoryUnitId: Long
    ) {
        _uiState.value =
            _uiState.value.copy(
                items =
                    _uiState.value.items.filterNot {
                        it.inventoryUnitId == inventoryUnitId
                    },
                errorMessage = null
            )
    }

    fun clearMessages() {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = null,
                successMessage = null
            )
    }

    fun saveChallan() {

        val state =
            _uiState.value

        if (state.isSaving) return

        val customer =
            state.selectedCustomer

        if (customer == null) {
            showError("Please select Customer / Hospital.")
            return
        }

        val challanNumber =
            state.challanNumber.trim()

        if (challanNumber.isBlank()) {
            showError("Challan Number is required.")
            return
        }

        if (state.challanDate.isBlank()) {
            showError("Challan Date is required.")
            return
        }

        if (state.financialYearStart <= 0) {
            showError("Valid Financial Year is required.")
            return
        }

        if (state.items.isEmpty()) {
            showError("Please add at least one physical item.")
            return
        }

        viewModelScope.launch {

            _uiState.value =
                state.copy(
                    isSaving = true,
                    errorMessage = null,
                    successMessage = null
                )

            try {

                val normalizedNumber =
                    normalizeDocumentNumber(
                        challanNumber
                    )

                if (!state.isEditMode) {
                    val duplicate =
                        challanRepository
                            .challanNumberExists(
                                normalizedChallanNumber =
                                    normalizedNumber,
                                financialYearStart =
                                    state.financialYearStart
                            )

                    require(!duplicate) {
                        "Challan Number $challanNumber already exists in this Financial Year."
                    }
                }

                val now =
                    System.currentTimeMillis()

                val challan =
                    ChallanEntity(
                        id = if (state.isEditMode) state.editingChallanId ?: 0L else 0L,
                        customerId = customer.id,
                        customerName = customer.partyName.trim(),
                        challanNumber = challanNumber,
                        normalizedChallanNumber = normalizedNumber,
                        challanDate = state.challanDate.trim(),
                        financialYearStart = state.financialYearStart,
                        asLibrary = state.asLibrary,
                        remarks = state.remarks.trim(),
                        status = "OPEN",
                        createdAt = now,
                        updatedAt = now
                    )

                val items =
                    state.items.map { item ->
                        ChallanItemEntity(
                            challanId = if (state.isEditMode) state.editingChallanId ?: 0L else 0L,
                            inventoryUnitId =
                                item.inventoryUnitId,
                            productId =
                                item.productId,
                            productName =
                                item.productName.trim(),
                            serialNumber =
                                item.serialNumber.trim(),
                            power =
                                item.power.trim(),
                            batchNumber =
                                item.batchNumber.trim(),
                            expiryDate =
                                item.expiryDate.trim(),
                            rate =
                                item.rate,
                            gstPercent =
                                item.gstPercent,
                            settlementStatus =
                                "PENDING",
                            saleId =
                                null,
                            settledAt =
                                null,
                            createdAt =
                                now,
                            updatedAt =
                                now
                        )
                    }

                val challanId =
                    if (state.isEditMode && state.editingChallanId != null) {
                        challanRepository.updateCompleteChallan(
                            challanId = state.editingChallanId,
                            challan = challan,
                            items = items
                        )
                    } else {
                        challanRepository.saveCompleteNewChallan(
                            challan = challan,
                            items = items
                        )
                    }

                _uiState.value =
                    NewChallanUiState(
                        customers =
                            _uiState.value.customers,
                        challanDate =
                            today(),
                        financialYearStart =
                            financialYearStart(Date()),
                        savedChallanId =
                            challanId,
                        successMessage =
                            if (state.isEditMode) "Challan $challanNumber updated successfully."
                            else "Challan $challanNumber saved successfully."
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        errorMessage =
                            exception.message
                                ?.takeIf { it.isNotBlank() }
                                ?: "Unable to save Challan."
                    )
            }
        }
    }

    private fun showError(
        message: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                isSearchingSerial = false,
                isSaving = false,
                errorMessage = message,
                successMessage = null
            )
    }

    private fun normalizeDocumentNumber(
        value: String
    ): String {
        return value
            .trim()
            .uppercase(Locale.getDefault())
            .replace(Regex("\\s+"), "")
    }

    private fun today(): String {
        return SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    private fun financialYearStart(
        date: Date
    ): Int {

        val calendar =
            Calendar.getInstance()

        calendar.time = date

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH) + 1

        return if (month >= 4) {
            year
        } else {
            year - 1
        }
    }

    private fun financialYearStartFromDisplayDate(
        value: String
    ): Int {

        return runCatching {

            val parsed =
                SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }.parse(value)
                    ?: return@runCatching 0

            financialYearStart(parsed)

        }.getOrDefault(0)
    }
}
