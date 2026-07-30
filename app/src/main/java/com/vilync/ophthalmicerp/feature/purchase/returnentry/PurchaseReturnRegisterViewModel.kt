package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnSerialSearchRow
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class PurchaseReturnRegisterViewModel(
    private val repository: PurchaseReturnRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    val searchQuery: StateFlow<String> = query

    val results: StateFlow<List<PurchaseReturnSerialSearchRow>> =
        query
            .debounce(250)
            .distinctUntilChanged()
            .flatMapLatest { value ->
                if (value.trim().length < 2) {
                    flowOf(emptyList())
                } else {
                    repository.searchAvailableIolSerials(value.trim())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun updateSearchQuery(value: String) {
        query.value = value
    }
}
