package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddPurchaseOrderItemViewModel(
    private val productRepository: ProductMasterRepository,
    private val stockUseCase: com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPurchaseOrderItemUiState())
    val uiState: StateFlow<AddPurchaseOrderItemUiState> = _uiState.asStateFlow()

    private val _productSuggestions = MutableStateFlow<List<ProductMaster>>(emptyList())
    val productSuggestions: StateFlow<List<ProductMaster>> = _productSuggestions.asStateFlow()

    private var productSearchJob: Job? = null

    fun updateProductName(value: String) {
        _uiState.update { it.copy(productId = 0L, productName = value, errorMessage = null, variants = emptyList(), availablePowers = emptyList()) }
        searchProducts(value)
    }

    private fun searchProducts(query: String) {
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            val flow = if (query.trim().isBlank()) {
                productRepository.getAllActiveProducts()
            } else {
                productRepository.searchProducts(query)
            }
            flow.collect { products ->
                _productSuggestions.value = products.filter { it.isActive }.take(20)
            }
        }
    }

    fun selectProduct(product: ProductMaster) {
        _uiState.update { it.copy(
            productId = product.id,
            productName = product.productName,
            model = product.model,
            category = product.category.name,
            hsnCode = product.hsnCode,
            gstPercent = product.gstPercent,
            purchaseRate = product.purchasePrice,
            errorMessage = null,
            variants = if (it.variants.isEmpty()) listOf(OrderItemVariant()) else it.variants
        )}
        _productSuggestions.value = emptyList()
        loadAvailablePowers(product.id)
        recalculate()
    }

    private fun loadAvailablePowers(productId: Long) {
        viewModelScope.launch {
            try {
                val snapshots = stockUseCase?.execute() ?: emptyList()
                val powers = snapshots
                    .filter { it.productId == productId && it.power.isNotBlank() }
                    .map { it.power }
                    .distinct()
                    .sorted()
                _uiState.update { it.copy(availablePowers = powers) }
            } catch (_: Exception) {}
        }
    }

    fun addVariant() {
        _uiState.update { it.copy(variants = it.variants + OrderItemVariant()) }
        recalculate()
    }

    fun updateVariant(id: String, power: String, qty: Int) {
        _uiState.update { state ->
            val list = state.variants.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                // Check if power already exists in ANOTHER row to merge
                val existingIndex = list.indexOfFirst { it.id != id && it.power.isNotBlank() && it.power.equals(power.trim(), ignoreCase = true) }
                
                if (existingIndex != -1 && power.isNotBlank()) {
                    // Merge into existing
                    val existing = list[existingIndex]
                    list[existingIndex] = existing.copy(quantity = existing.quantity + qty)
                    list.removeAt(index)
                } else {
                    // Just update
                    list[index] = list[index].copy(power = power.uppercase().trim(), quantity = qty.coerceAtLeast(1))
                }
                state.copy(variants = list)
            } else state
        }
        recalculate()
    }

    fun removeVariant(id: String) {
        _uiState.update { it.copy(variants = it.variants.filter { v -> v.id != id }) }
        recalculate()
    }

    fun updatePurchaseRate(value: Double) {
        _uiState.update { it.copy(purchaseRate = value.coerceAtLeast(0.0), errorMessage = null) }
        recalculate()
    }

    fun updateGstPercent(value: Double) {
        _uiState.update { it.copy(gstPercent = value.coerceIn(0.0, 100.0), errorMessage = null) }
        recalculate()
    }

    private fun recalculate() {
        _uiState.update { state ->
            val totalQty = state.variants.sumOf { it.quantity }
            val gross = totalQty * state.purchaseRate
            val taxable = gross
            val tax = taxable * (state.gstPercent / 100.0)
            state.copy(
                totalQuantity = totalQty,
                grossAmount = gross,
                taxableAmount = taxable,
                taxAmount = tax,
                overallTotal = taxable + tax
            )
        }
    }

    fun validate(): Boolean {
        val state = _uiState.value
        if (state.productName.isBlank() || state.productId <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please select a Product.") }
            return false
        }
        if (state.variants.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least one Power/Qty row.") }
            return false
        }
        if (state.category.equals("IOL", ignoreCase = true)) {
            // Format and Validate all variants
            val updatedVariants = state.variants.map { v ->
                val p = v.power.uppercase().replace("D", "").trim()
                if (p.isBlank()) return@map v
                val numeric = p.toDoubleOrNull() ?: return@map v
                val formatted = if (numeric % 1.0 == 0.0) numeric.toInt().toString() else numeric.toString()
                v.copy(power = "${formatted}D")
            }
            
            if (updatedVariants.any { it.power.isBlank() || !it.power.endsWith("D") }) {
                _uiState.update { it.copy(errorMessage = "All IOL rows must have a valid Power (e.g. 20D).") }
                return false
            }
            
            _uiState.update { it.copy(variants = updatedVariants) }
        }
        return true
    }

    fun loadItem(item: PurchaseItem) {
        // Edit mode support for a single item (existing logic)
        _uiState.update { AddPurchaseOrderItemUiState(
            productId = item.productId,
            productName = item.productName,
            model = item.model,
            category = item.category,
            hsnCode = item.hsnCode,
            variants = listOf(OrderItemVariant(power = item.power, quantity = item.quantity)),
            purchaseRate = item.purchaseRate,
            discountPercent = item.discountPercent,
            gstPercent = item.gstPercent,
            isEditMode = true
        )}
        loadAvailablePowers(item.productId)
        recalculate()
    }

    fun prefillVariant(power: String, qty: Int) {
        _uiState.update { it.copy(
            variants = listOf(OrderItemVariant(power = power, quantity = qty))
        )}
        recalculate()
    }

    fun getPurchaseItems(): List<PurchaseItem> {
        val state = _uiState.value
        return state.variants.map { v ->
            PurchaseItem(
                productId = state.productId,
                productName = state.productName,
                model = state.model,
                category = state.category,
                hsnCode = state.hsnCode,
                power = v.power,
                quantity = v.quantity,
                purchaseRate = state.purchaseRate,
                discountPercent = state.discountPercent,
                gstPercent = state.gstPercent
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
