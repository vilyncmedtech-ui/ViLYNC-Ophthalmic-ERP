package com.vilync.ophthalmicerp.feature.inventory.reconciliation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.feature.inventory.reconciliation.domain.StockReconciliationUseCase
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PhysicalSerialEntry(
    val serialNumber: String,
    val unitId: Long = 0L,
    val status: ValidationStatus = ValidationStatus.PENDING,
    val errorMessage: String? = null
)

enum class ValidationStatus {
    MATCHED,
    WRONG_PRODUCT,
    WRONG_POWER,
    ALREADY_CONSUMED,
    UNKNOWN,
    PENDING
}

data class StockReconciliationUiState(
    val isLoading: Boolean = false,
    val products: List<ProductMaster> = emptyList(),
    val selectedProduct: ProductMaster? = null,
    val powerInput: String = "",
    val normalizedPower: String = "",
    val systemSerials: List<InventoryUnitEntity> = emptyList(),
    val physicalEntries: List<PhysicalSerialEntry> = emptyList(),
    val serialInput: String = "",
    val remarks: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val showReview: Boolean = false,
    val isValidatingSerial: Boolean = false
)

class StockReconciliationViewModel(
    private val productRepository: ProductMasterRepository,
    private val useCase: StockReconciliationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockReconciliationUiState())
    val uiState: StateFlow<StockReconciliationUiState> = _uiState.asStateFlow()

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
                powerInput = "",
                normalizedPower = "",
                systemSerials = emptyList(),
                physicalEntries = emptyList(),
                serialInput = "",
                isSuccess = false,
                showReview = false
            ) 
        }
    }

    fun onPowerChanged(power: String) {
        val normalized = normalizePower(power)
        _uiState.update { 
            it.copy(
                powerInput = power, 
                normalizedPower = normalized,
                systemSerials = emptyList(),
                physicalEntries = emptyList(),
                serialInput = "",
                showReview = false
            ) 
        }
        if (normalized.isNotBlank()) {
            loadSystemStock()
        }
    }

    private fun loadSystemStock() {
        val product = _uiState.value.selectedProduct ?: return
        val power = _uiState.value.normalizedPower
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val units = useCase.getInStockUnits(product.id, power)
            _uiState.update { it.copy(systemSerials = units, isLoading = false) }
        }
    }

    fun onSerialInputChanged(input: String) {
        _uiState.update { it.copy(serialInput = input) }
    }

    fun addPhysicalSerial() {
        val rawInput = _uiState.value.serialInput.trim()
        if (rawInput.isBlank()) return

        // 1. Snapshot context immediately to prevent race conditions
        val snapshotProduct = _uiState.value.selectedProduct ?: return
        val snapshotPower = _uiState.value.normalizedPower

        _uiState.update { it.copy(isValidatingSerial = true) }

        viewModelScope.launch {
            try {
                // 2. Perform prefix-agnostic lookup (handles 252279 ↔ LMDE252279)
                val unit = useCase.getUnitBySerialAgnostic(rawInput)
                
                // 3. Determine consistent identity for the list entry
                val entrySerial = unit?.serialNumber ?: rawInput

                // 4. Duplicate Check: Block only if a valid match for this unit already exists
                val existingMatch = _uiState.value.physicalEntries.find { 
                    (unit != null && it.unitId == unit.id && it.status == ValidationStatus.MATCHED) ||
                    (unit == null && it.serialNumber == entrySerial)
                }
                if (existingMatch != null) {
                    _uiState.update { it.copy(errorMessage = "Serial already entered.", isValidatingSerial = false) }
                    return@launch
                }

                // 5. Validate against the captured snapshots
                val entry = validateSerial(entrySerial, unit, snapshotProduct.id, snapshotPower)
                
                _uiState.update { state ->
                    // Remove any previous error entries for this serial to allow "retry"
                    val filtered = state.physicalEntries.filterNot { it.serialNumber == entrySerial }
                    state.copy(
                        physicalEntries = filtered + entry,
                        serialInput = "",
                        errorMessage = null,
                        isValidatingSerial = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isValidatingSerial = false) }
            }
        }
    }

    private fun validateSerial(
        serial: String, 
        unit: InventoryUnitEntity?,
        contextProductId: Long,
        contextPower: String
    ): PhysicalSerialEntry {
        if (unit == null) return PhysicalSerialEntry(serial, status = ValidationStatus.UNKNOWN)
        
        if (unit.productId != contextProductId) {
            return PhysicalSerialEntry(serial, unit.id, ValidationStatus.WRONG_PRODUCT)
        }
        
        if (unit.power != contextPower) {
            // Power comparison now uses the snapshot context
            return PhysicalSerialEntry(serial, unit.id, ValidationStatus.WRONG_POWER)
        }
        
        if (unit.status != "IN_STOCK") {
            return PhysicalSerialEntry(serial, unit.id, ValidationStatus.ALREADY_CONSUMED, "Status: ${unit.status}")
        }
        
        return PhysicalSerialEntry(serial, unit.id, ValidationStatus.MATCHED)
    }

    fun removePhysicalSerial(serial: String) {
        _uiState.update { state ->
            state.copy(physicalEntries = state.physicalEntries.filterNot { it.serialNumber == serial })
        }
    }

    fun onRemarksChanged(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun toggleReview() {
        _uiState.update { it.copy(showReview = !it.showReview, errorMessage = null) }
    }

    fun submitReconciliation() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return

        if (state.isValidatingSerial) {
            _uiState.update { it.copy(errorMessage = "Please wait for serial validation to complete.") }
            return
        }
        
        val hasErrors = state.physicalEntries.any { it.status != ValidationStatus.MATCHED }
        if (hasErrors) {
            _uiState.update { it.copy(errorMessage = "Please resolve extra or invalid serials before confirming.") }
            return
        }

        val matchedIds = state.physicalEntries.map { it.unitId }.toSet()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                useCase.performReconciliation(
                    productId = product.id,
                    power = state.normalizedPower,
                    physicalSerialIds = matchedIds,
                    remarks = state.remarks
                )
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun normalizePower(input: String): String {
        val trimmed = input.trim().uppercase()
        if (trimmed.isEmpty()) return ""
        val base = if (trimmed.endsWith("D")) trimmed.dropLast(1).trim() else trimmed
        val numeric = base.toDoubleOrNull() ?: return trimmed
        val formatted = if (numeric % 1.0 == 0.0) numeric.toInt().toString() else numeric.toString()
        return "${formatted}D"
    }

    fun resetSuccess() {
        _uiState.update { StockReconciliationUiState(products = it.products) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
