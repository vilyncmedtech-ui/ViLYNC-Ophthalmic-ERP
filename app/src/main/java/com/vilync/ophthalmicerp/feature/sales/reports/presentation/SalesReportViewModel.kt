package com.vilync.ophthalmicerp.feature.sales.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.sales.reports.data.SalesReportUiState
import com.vilync.ophthalmicerp.feature.sales.reports.domain.SalesReportType
import com.vilync.ophthalmicerp.feature.sales.reports.domain.SalesReportUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalesReportViewModel(
    private val salesReportUseCase: SalesReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesReportUiState())
    val uiState: StateFlow<SalesReportUiState> = _uiState.asStateFlow()

    init {
        // Initialize with current month range
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val end = sdf.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = sdf.format(calendar.time)
        
        _uiState.update { it.copy(startDate = start, endDate = end) }
        loadReport()
    }

    fun loadReport() {
        val state = _uiState.value
        if (state.startDate.isBlank() || state.endDate.isBlank()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                when (state.selectedReportType) {
                    SalesReportType.SUMMARY -> {
                        val data = salesReportUseCase.getProductSummary(state.startDate, state.endDate)
                        _uiState.update { it.copy(
                            isLoading = false,
                            productSummary = data,
                            totalCount = data.sumOf { it.qty },
                            totalAmount = data.sumOf { it.amount },
                            reportTitle = "Product-wise Sales Summary"
                        )}
                    }
                    SalesReportType.DETAIL -> {
                        val data = salesReportUseCase.getSalesDetails(state.startDate, state.endDate)
                        _uiState.update { it.copy(
                            isLoading = false,
                            salesList = data,
                            totalCount = data.size,
                            totalAmount = data.sumOf { it.amount },
                            reportTitle = "Date-wise Sales Detail"
                        )}
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Report type not implemented") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load report") }
            }
        }
    }

    fun updateReportType(type: SalesReportType) {
        _uiState.update { it.copy(selectedReportType = type) }
        loadReport()
    }

    fun updateDateRange(start: String, end: String) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
    }
}

class SalesReportViewModelFactory(
    private val useCase: SalesReportUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalesReportViewModel(useCase) as T
    }
}
