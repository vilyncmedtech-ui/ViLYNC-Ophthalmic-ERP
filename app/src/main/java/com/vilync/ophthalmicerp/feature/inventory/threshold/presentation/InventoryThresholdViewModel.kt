package com.vilync.ophthalmicerp.feature.inventory.threshold.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.dao.ProductDao
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.data.entity.InventoryThresholdEntity
import com.vilync.ophthalmicerp.feature.inventory.threshold.data.InventoryThresholdRepository
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryThresholdUiState(
    val brands: List<String> = emptyList(),
    val selectedBrand: String? = null,
    val products: List<ProductEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val powerThresholds: List<PowerThresholdItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

data class PowerThresholdItem(
    val power: String,
    val currentStock: Int,
    val minimumStock: String,
    val reorderLevel: String,
    val isModified: Boolean = false
)

class InventoryThresholdViewModel(
    private val productDao: ProductDao,
    private val thresholdRepository: InventoryThresholdRepository,
    private val stockUseCase: GetAvailableStockUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryThresholdUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadBrands()
    }

    private fun loadBrands() {
        viewModelScope.launch {
            productDao.getManufacturerSuggestions()
                .collect { brands ->
                    _uiState.update { it.copy(brands = brands) }
                }
        }
    }

    fun selectBrand(brand: String) {
        _uiState.update { it.copy(selectedBrand = brand, selectedProduct = null, powerThresholds = emptyList()) }
        loadProductsForBrand(brand)
    }

    private fun loadProductsForBrand(brand: String) {
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { allProducts ->
                val filtered = allProducts.filter { it.brandName == brand }
                _uiState.update { it.copy(products = filtered) }
            }
        }
    }

    fun selectProduct(product: ProductEntity) {
        _uiState.update { it.copy(selectedProduct = product, isLoading = true) }
        loadThresholdsForProduct(product)
    }

    private fun loadThresholdsForProduct(product: ProductEntity) {
        viewModelScope.launch {
            val stockSnapshots = stockUseCase.execute().filter { it.productId == product.id }
            val existingThresholds = thresholdRepository.getThresholdsForProduct(product.id).first()
                .associateBy { it.power }

            val items = stockSnapshots.map { snapshot ->
                val threshold = existingThresholds[snapshot.power]
                PowerThresholdItem(
                    power = snapshot.power,
                    currentStock = snapshot.availableQuantity,
                    minimumStock = (threshold?.minimumStock ?: product.minimumStock).toString(),
                    reorderLevel = (threshold?.reorderLevel ?: product.reorderLevel).toString()
                )
            }.sortedBy { it.power }

            _uiState.update { it.copy(powerThresholds = items, isLoading = false) }
        }
    }

    fun updateThreshold(power: String, min: String, reorder: String) {
        _uiState.update { state ->
            val updatedItems = state.powerThresholds.map { 
                if (it.power == power) it.copy(minimumStock = min, reorderLevel = reorder, isModified = true)
                else it
            }
            state.copy(powerThresholds = updatedItems)
        }
    }

    fun saveChanges() {
        val product = _uiState.value.selectedProduct ?: return
        val modifiedItems = _uiState.value.powerThresholds.filter { it.isModified }
        if (modifiedItems.isEmpty()) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                modifiedItems.forEach { item ->
                    val entity = InventoryThresholdEntity(
                        productId = product.id,
                        power = item.power,
                        minimumStock = item.minimumStock.toIntOrNull() ?: 0,
                        reorderLevel = item.reorderLevel.toIntOrNull() ?: 0
                    )
                    thresholdRepository.upsertThreshold(entity)
                }
                _uiState.update { it.copy(isSaving = false, message = "Thresholds saved successfully") }
                // Reset modification flags
                loadThresholdsForProduct(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "Failed to save: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

class InventoryThresholdViewModelFactory(
    private val productDao: ProductDao,
    private val thresholdRepository: InventoryThresholdRepository,
    private val stockUseCase: GetAvailableStockUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InventoryThresholdViewModel(productDao, thresholdRepository, stockUseCase) as T
    }
}
