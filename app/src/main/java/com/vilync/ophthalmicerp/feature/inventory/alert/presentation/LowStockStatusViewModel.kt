package com.vilync.ophthalmicerp.feature.inventory.alert.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.inventory.alert.LowStockAlertUseCase
import com.vilync.ophthalmicerp.feature.inventory.logic.InventorySnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LowStockStatusUiState(
    val items: List<LowStockAlertItem> = emptyList(),
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalAlerts: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: AlertFilter = AlertFilter.SHORTAGES_ONLY,
    val isLoading: Boolean = false
)

data class LowStockAlertItem(
    val snapshot: InventorySnapshot,
    val status: String
)

enum class AlertFilter {
    ALL,
    SHORTAGES_ONLY,
    OUT_OF_STOCK,
    LOW_STOCK
}

class LowStockStatusViewModel(
    private val useCase: LowStockAlertUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockStatusUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    fun loadStatus() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val snapshots = useCase.getSnapshots()
                val items = snapshots.map { 
                    LowStockAlertItem(it, useCase.classify(it))
                }
                
                val outOfStock = items.count { it.status == LowStockAlertUseCase.STATUS_OUT_OF_STOCK }
                val lowStock = items.count { it.status == LowStockAlertUseCase.STATUS_LOW_STOCK }
                
                _uiState.update { it.copy(
                    items = items,
                    outOfStockCount = outOfStock,
                    lowStockCount = lowStock,
                    totalAlerts = outOfStock + lowStock,
                    isLoading = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: AlertFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    val filteredItems = _uiState.map { state ->
        state.items.filter { item ->
            // 1. Status Filter
            val matchesFilter = when (state.selectedFilter) {
                AlertFilter.ALL -> true
                AlertFilter.SHORTAGES_ONLY -> item.status == LowStockAlertUseCase.STATUS_OUT_OF_STOCK || item.status == LowStockAlertUseCase.STATUS_LOW_STOCK
                AlertFilter.OUT_OF_STOCK -> item.status == LowStockAlertUseCase.STATUS_OUT_OF_STOCK
                AlertFilter.LOW_STOCK -> item.status == LowStockAlertUseCase.STATUS_LOW_STOCK
            }
            
            // 2. Search Filter
            val matchesSearch = state.searchQuery.isBlank() || 
                item.snapshot.productName.contains(state.searchQuery, ignoreCase = true) ||
                item.snapshot.brandName.contains(state.searchQuery, ignoreCase = true) ||
                item.snapshot.power.contains(state.searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }.sortedWith(compareBy<LowStockAlertItem> { 
            when(it.status) {
                LowStockAlertUseCase.STATUS_OUT_OF_STOCK -> 0
                LowStockAlertUseCase.STATUS_LOW_STOCK -> 1
                LowStockAlertUseCase.STATUS_REORDER -> 2
                else -> 3
            }
        }.thenBy { it.snapshot.productName })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class LowStockStatusViewModelFactory(
    private val useCase: LowStockAlertUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LowStockStatusViewModel(useCase) as T
    }
}
