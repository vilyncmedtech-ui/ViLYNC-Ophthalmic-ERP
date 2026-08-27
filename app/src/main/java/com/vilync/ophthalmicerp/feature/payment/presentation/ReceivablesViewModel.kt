package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.payment.domain.PartyReceivableSummary
import com.vilync.ophthalmicerp.feature.payment.logic.FinancialReportingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceivablesUiState(
    val isLoading: Boolean = false,
    val summaries: List<PartyReceivableSummary> = emptyList(),
    val filteredSummaries: List<PartyReceivableSummary> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val totalDue: Double = 0.0,
    val totalReceived: Double = 0.0,
    val totalAdjustments: Double = 0.0,
    val totalOutstanding: Double = 0.0
)

class ReceivablesViewModel(
    private val reportingUseCase: FinancialReportingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceivablesUiState())
    val uiState: StateFlow<ReceivablesUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val data = reportingUseCase.getReceivablesSummary()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        summaries = data,
                        totalDue = data.sumOf { it.totalDue },
                        totalReceived = data.sumOf { it.totalReceived },
                        totalAdjustments = data.sumOf { it.totalAdjustments },
                        totalOutstanding = data.sumOf { it.balanceOutstanding }
                    )
                }
                applyFilter()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load summary") }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val filtered = if (state.searchQuery.isBlank()) {
            state.summaries
        } else {
            state.summaries.filter { it.partyName.contains(state.searchQuery, ignoreCase = true) }
        }
        _uiState.update { it.copy(filteredSummaries = filtered) }
    }
}

class ReceivablesViewModelFactory(
    private val reportingUseCase: FinancialReportingUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReceivablesViewModel(reportingUseCase) as T
    }
}
