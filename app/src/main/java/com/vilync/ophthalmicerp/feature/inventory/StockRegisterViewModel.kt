package com.vilync.ophthalmicerp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.repository.InventoryStockRepository
import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StockRegisterViewModel(
    private val repository: InventoryStockRepository
) : ViewModel() {

    // =========================================================
    // SEARCH
    // =========================================================

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery


    // =========================================================
    // SOURCE STOCK
    // =========================================================

    private val stockRegister =
        repository.getStockRegister()


    // =========================================================
    // FILTERED STOCK
    // =========================================================

    val stockRows: StateFlow<List<StockRegisterRow>> =
        combine(
            stockRegister,
            _searchQuery
        ) { rows, query ->

            val normalizedQuery =
                query.trim()

            // 1. Filter out rows where all four metrics are zero
            val activeRows = rows.filter { row ->
                row.purchasedQuantity > 0 ||
                        row.soldQuantity > 0 ||
                        row.purchaseReturnQuantity > 0 ||
                        row.availableQuantity > 0
            }

            // 2. Apply Search Filter
            if (normalizedQuery.isBlank()) {
                activeRows
            } else {
                activeRows.filter { row ->
                    row.productName.contains(
                        normalizedQuery,
                        ignoreCase = true
                    ) ||
                            row.model.contains(
                                normalizedQuery,
                                ignoreCase = true
                            ) ||
                            row.category.contains(
                                normalizedQuery,
                                ignoreCase = true
                            ) ||
                            row.power.contains(
                                normalizedQuery,
                                ignoreCase = true
                            )
                }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000
                ),
                initialValue = emptyList()
            )


    // =========================================================
    // SUMMARY
    // =========================================================

    /**
     * Total number of currently available physical /
     * quantity stock units represented by the filtered
     * Stock Register.
     */
    val totalAvailableUnits: StateFlow<Int> =
        stockRows
            .combine(
                MutableStateFlow(Unit)
            ) { rows, _ ->

                rows.sumOf { row ->
                    row.availableQuantity
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000
                ),
                initialValue = 0
            )


    /**
     * Number of Product + Power stock rows currently
     * visible after search filtering.
     */
    val totalStockRows: StateFlow<Int> =
        stockRows
            .combine(
                MutableStateFlow(Unit)
            ) { rows, _ ->

                rows.size
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000
                ),
                initialValue = 0
            )


    // =========================================================
    // SEARCH ACTION
    // =========================================================

    fun updateSearchQuery(
        query: String
    ) {

        _searchQuery.value =
            query
    }


    // =========================================================
    // CLEAR SEARCH
    // =========================================================

    fun clearSearch() {

        _searchQuery.value =
            ""
    }
}
