package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearManager
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockItemEntity
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType
import com.vilync.ophthalmicerp.data.repository.OpeningStockRepository
import com.vilync.ophthalmicerp.feature.inventory.opening.domain.OpeningStockUseCase
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.core.util.SerialFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OpeningStockViewModel(
    private val repository: OpeningStockRepository,
    private val useCase: OpeningStockUseCase,
    private val productMasterRepository: ProductMasterRepository,
    private val numberingRepository: DocumentNumberingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpeningStockUiState())
    val uiState: StateFlow<OpeningStockUiState> = _uiState.asStateFlow()

    init {
        loadRegister()
    }

    fun loadRegister() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegisterLoading = true) }
            
            // Reconcile any ghost stock before loading the list.
            useCase.reconcileGhostStock()

            repository.getAllOpeningStocks().collect { stocks ->
                _uiState.update { it.copy(isRegisterLoading = false, openingStocks = stocks) }
            }
        }
    }

    fun startNewEntry() {
        // Guard: If already initialized for a New Entry session, skip.
        if (_uiState.value.entryNumber.isNotBlank() && _uiState.value.id == 0L) return
        
        // Guard: Prevent concurrent entry initialization.
        if (_uiState.value.isEntryLoading) return

        viewModelScope.launch {
            val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            
            _uiState.update { it.copy(isEntryLoading = true) }
            
            try {
                // New logic: Permanent VMOS sequence is NOT consumed during NEW/DRAFT initialization.
                val tempDraftNumber = "DRAFT-" + System.currentTimeMillis()
                
                _uiState.update { 
                    it.copy(
                        id = 0L, // Explicitly reset database ID for NEW entry
                        entryNumber = tempDraftNumber,
                        entryDate = today,
                        status = "DRAFT",
                        isEditMode = false,
                        isEntryLoading = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEntryLoading = false, errorMessage = "Failed to initialize NEW entry: ${e.message}") }
            }
        }
    }

    fun loadEntry(id: Long) {
        // Guard: If already loading or loaded this specific record, skip.
        if (_uiState.value.id == id && _uiState.value.entryNumber.isNotBlank()) return
        if (_uiState.value.isEntryLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEntryLoading = true, isEditMode = true) }
            val header = repository.getOpeningStockById(id)
            if (header != null) {
                val entities = repository.getOpeningStockItems(id).first()
                val uiItems = mutableListOf<OpeningStockUiItem>()
                
                for (entity in entities) {
                    val product = productMasterRepository.getProductById(entity.productId)
                    val isSerial = product?.serialNumberRequired == true
                    
                    var uiSerialNumber = entity.serialNumber
                    if (isSerial && product != null && product.serialPrefix.isNotBlank()) {
                        // Ensure legacy/backfilled numeric serials are formatted for display
                        if (!uiSerialNumber.startsWith(product.serialPrefix, ignoreCase = true)) {
                            val source = if (entity.rawSerial.isNotBlank()) entity.rawSerial else uiSerialNumber
                            uiSerialNumber = SerialFormatter.format(product.serialPrefix, source)
                        }
                    }

                    uiItems.add(
                        OpeningStockUiItem(
                            productId = entity.productId,
                            productName = entity.productName,
                            model = entity.model,
                            power = entity.power,
                            batchNumber = entity.batchNumber,
                            serialNumber = uiSerialNumber,
                            rawSerial = entity.rawSerial,
                            expiryDate = entity.expiryDate,
                            quantity = entity.quantity,
                            unitCost = entity.unitCost,
                            gstPercent = entity.gstPercent,
                            totalCost = entity.totalCost,
                            trackingType = if (isSerial) "SERIAL" else "QUANTITY"
                        )
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isEntryLoading = false,
                        id = header.id,
                        entryNumber = header.entryNumber,
                        entryDate = header.entryDate,
                        remarks = header.remarks,
                        status = header.status,
                        items = uiItems,
                        isPosted = header.status == "POSTED",
                        isCancelled = header.status == "CANCELLED"
                    )
                }
            } else {
                _uiState.update { it.copy(isEntryLoading = false, errorMessage = "Entry not found") }
            }
        }
    }

    /**
     * Entry Number is now system-generated and read-only.
     * Manual updates are no longer accepted.
     */
    @Suppress("UNUSED_PARAMETER")
    fun updateEntryNumber(value: String) {
        // No-op to enforce read-only status in state
    }

    fun updateEntryDate(value: String) {
        _uiState.update { it.copy(entryDate = value, isDirty = true) }
    }

    fun updateRemarks(value: String) {
        _uiState.update { it.copy(remarks = value, isDirty = true) }
    }

    fun addItem(item: OpeningStockUiItem) {
        val currentItems = _uiState.value.items.toMutableList()
        currentItems.add(item)
        _uiState.update { it.copy(items = currentItems, isDirty = true) }
    }

    fun removeItem(index: Int) {
        val currentItems = _uiState.value.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _uiState.update { it.copy(items = currentItems, isDirty = true) }
        }
    }

    fun saveDraft() {
        if (!validate()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val header = createEntity()
                val items = _uiState.value.items.map { createItemEntity(it) }
                val savedId = useCase.saveDraft(header, items)
                _uiState.update { it.copy(
                    id = savedId,
                    isSaving = false, 
                    saveSuccess = true, 
                    isDirty = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun post() {
        if (!validate()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // First save complete
                val header = createEntity()
                val items = _uiState.value.items.map { createItemEntity(it) }
                val id = useCase.saveDraft(header, items)
                
                // Then post
                useCase.postOpeningStock(id)
                loadEntry(id) // Reload to get POSTED status
                _uiState.update { it.copy(isSaving = false, isPosted = true, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun cancel(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                useCase.cancelOpeningStock(_uiState.value.id, reason)
                loadEntry(_uiState.value.id)
                _uiState.update { it.copy(isSaving = false, isCancelled = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        if (state.entryNumber.isBlank() && !state.isEntryLoading) {
            _uiState.update { it.copy(errorMessage = "System Error: Entry Number is missing.") }
            return false
        }
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least one product.") }
            return false
        }
        
        // Serial uniqueness in document
        val serials = state.items.filter { it.trackingType == "SERIAL" }.map { it.serialNumber.uppercase() }
        if (serials.size != serials.distinct().size) {
            _uiState.update { it.copy(errorMessage = "Duplicate Serial Numbers found in document") }
            return false
        }
        
        return true
    }

    private fun createEntity() = OpeningStockEntity(
        id = _uiState.value.id,
        entryNumber = _uiState.value.entryNumber.trim().uppercase(),
        normalizedEntryNumber = _uiState.value.entryNumber.trim().uppercase(),
        entryDate = _uiState.value.entryDate,
        financialYearStart = FinancialYearManager.activeFinancialYear.value.startYear,
        remarks = _uiState.value.remarks,
        status = _uiState.value.status
    )

    private fun createItemEntity(uiItem: OpeningStockUiItem) = OpeningStockItemEntity(
        openingStockId = _uiState.value.id,
        productId = uiItem.productId,
        productName = uiItem.productName,
        model = uiItem.model,
        power = uiItem.power,
        batchNumber = uiItem.batchNumber,
        serialNumber = uiItem.serialNumber,
        rawSerial = uiItem.rawSerial,
        expiryDate = uiItem.expiryDate,
        quantity = uiItem.quantity,
        unitCost = uiItem.unitCost,
        gstPercent = uiItem.gstPercent,
        totalCost = uiItem.totalCost
    )

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
