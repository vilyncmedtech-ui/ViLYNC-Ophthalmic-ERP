package com.vilync.ophthalmicerp.feature.inventory.adjustment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.feature.inventory.adjustment.domain.StockAdjustmentUseCase
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StockAdjustmentUiState(
    val isLoading: Boolean = false,
    val products: List<ProductMaster> = emptyList(),
    val selectedProduct: ProductMaster? = null,
    val selectedPower: String = "",
    val availablePowers: List<String> = emptyList(),
    val availableSerials: List<InventoryUnitEntity> = emptyList(),
    val filteredSerials: List<InventoryUnitEntity> = emptyList(),
    val serialSearchQuery: String = "",
    val selectedSerialId: Long = 0L,
    val reason: String = "",
    val quantity: String = "1",
    val remarks: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class StockAdjustmentViewModel(
    private val productRepository: ProductMasterRepository,
    private val useCase: StockAdjustmentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAdjustmentUiState())
    val uiState: StateFlow<StockAdjustmentUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllActiveProducts().collect { list ->
                _uiState.update { it.copy(products = list) }
            }
        }
    }

    fun onProductSelected(product: ProductMaster) {
        _uiState.update { 
            it.copy(
                selectedProduct = if (product.id == -1L) null else product,
                selectedPower = "",
                availablePowers = emptyList(),
                availableSerials = emptyList(),
                filteredSerials = emptyList(),
                serialSearchQuery = "",
                selectedSerialId = 0L,
                isSuccess = false
            ) 
        }
    }

    fun onPowerChanged(power: String) {
        _uiState.update { 
            it.copy(
                selectedPower = power, 
                selectedSerialId = 0L,
                serialSearchQuery = "",
                filteredSerials = emptyList()
            ) 
        }
        loadSerials(normalizePower(power))
    }

    private fun loadSerials(normalizedPower: String) {
        val product = _uiState.value.selectedProduct ?: return
        
        if (product.serialNumberRequired && normalizedPower.isNotBlank()) {
            viewModelScope.launch {
                val units = useCase.getAvailableUnits(product.id, normalizedPower)
                _uiState.update { it.copy(availableSerials = units, filteredSerials = units) }
            }
        }
    }

    private fun normalizePower(input: String): String {
        val trimmed = input.trim().uppercase()
        if (trimmed.isEmpty()) return ""

        // Remove "D" if present at the end
        val base = if (trimmed.endsWith("D")) {
            trimmed.substring(0, trimmed.length - 1).trim()
        } else {
            trimmed
        }

        val numeric = base.toDoubleOrNull() ?: return trimmed // Fallback to raw if not numeric

        val formatted = if (numeric % 1.0 == 0.0) {
            numeric.toInt().toString()
        } else {
            numeric.toString()
        }

        return "${formatted}D"
    }

    fun onSerialSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.availableSerials
            } else {
                state.availableSerials.filter { 
                    it.serialNumber.contains(query, ignoreCase = true) 
                }
            }
            state.copy(
                serialSearchQuery = query,
                filteredSerials = filtered,
                selectedSerialId = 0L // Reset selection while searching
            )
        }
    }

    fun onSerialSelected(unit: InventoryUnitEntity) {
        _uiState.update { 
            it.copy(
                selectedSerialId = unit.id,
                serialSearchQuery = unit.serialNumber,
                filteredSerials = emptyList() // Hide suggestions
            ) 
        }
    }

    fun onReasonChanged(reason: String) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onQuantityChanged(qty: String) {
        if (qty.isEmpty() || qty.all { it.isDigit() }) {
            _uiState.update { it.copy(quantity = qty) }
        }
    }

    fun onRemarksChanged(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun submitAdjustment() {
        val state = _uiState.value
        
        if (state.selectedProduct == null) {
            _uiState.update { it.copy(errorMessage = "Please select a product") }
            return
        }
        
        if (state.reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a reason") }
            return
        }

        if (state.selectedProduct.serialNumberRequired && state.selectedSerialId == 0L) {
            _uiState.update { it.copy(errorMessage = "Please select a serial number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                useCase.adjustStock(
                    productId = state.selectedProduct.id,
                    power = normalizePower(state.selectedPower),
                    inventoryUnitId = state.selectedSerialId,
                    reason = state.reason,
                    remarks = state.remarks
                )
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun resetSuccess() {
        _uiState.update { StockAdjustmentUiState(products = it.products) }
    }
}
