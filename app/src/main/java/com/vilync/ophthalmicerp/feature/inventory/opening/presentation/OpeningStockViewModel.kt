package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearManager
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockItemEntity
import com.vilync.ophthalmicerp.data.repository.OpeningStockRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.feature.inventory.opening.domain.OpeningStockUseCase
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
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
    private val productMasterRepository: ProductMasterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpeningStockUiState())
    val uiState: StateFlow<OpeningStockUiState> = _uiState.asStateFlow()

    init {
        loadRegister()
    }

    fun loadRegister() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllOpeningStocks().collect { stocks ->
                _uiState.update { it.copy(isLoading = false, openingStocks = stocks) }
            }
        }
    }

    fun startNewEntry() {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val fy = FinancialYearManager.activeFinancialYear.value.startYear
        _uiState.update { 
            OpeningStockUiState(
                entryDate = today,
                status = "DRAFT",
                isEditMode = false
            ) 
        }
    }

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            val header = repository.getOpeningStockById(id)
            if (header != null) {
                val items = repository.getOpeningStockItems(id).first().map { 
                    val product = productMasterRepository.getProductById(it.productId)
                    OpeningStockUiItem(
                        productId = it.productId,
                        productName = it.productName,
                        model = it.model,
                        power = it.power,
                        batchNumber = it.batchNumber,
                        expiryDate = it.expiryDate,
                        quantity = it.quantity,
                        unitCost = it.unitCost,
                        totalCost = it.totalCost,
                        trackingType = product?.serialNumberRequired?.let { if (it) "SERIAL" else "QUANTITY" } ?: "QUANTITY"
                    )
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        id = header.id,
                        entryNumber = header.entryNumber,
                        entryDate = header.entryDate,
                        remarks = header.remarks,
                        status = header.status,
                        items = items,
                        isPosted = header.status == "POSTED",
                        isCancelled = header.status == "CANCELLED"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Entry not found") }
            }
        }
    }

    fun updateEntryNumber(value: String) {
        _uiState.update { it.copy(entryNumber = value, isDirty = true) }
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
                useCase.saveDraft(header, items)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true, isDirty = false) }
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
        if (state.entryNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Entry Number is required") }
            return false
        }
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "At least one item is required") }
            return false
        }
        
        // Serial uniqueness in document
        val serials = state.items.filter { it.trackingType == "SERIAL" }.map { it.batchNumber }
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
        expiryDate = uiItem.expiryDate,
        quantity = uiItem.quantity,
        unitCost = uiItem.unitCost,
        totalCost = uiItem.totalCost
    )

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
