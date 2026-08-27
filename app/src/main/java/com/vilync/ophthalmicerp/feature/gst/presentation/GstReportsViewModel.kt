package com.vilync.ophthalmicerp.feature.gst.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.gst.data.GstReportingRepository
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FilingPeriod {
    FINANCIAL_YEAR,
    MONTHLY,
    QUARTERLY
}

enum class Gstr1InvoiceFilter {
    ALL,
    B2B,
    B2C
}

data class GstReportsUiState(
    val isLoading: Boolean = false,
    val snapshot: GstReportSnapshot? = null,
    val errorMessage: String? = null,
    val selectedPeriod: FilingPeriod = FilingPeriod.FINANCIAL_YEAR,
    val selectedMonth: Int = 3, // April (0-indexed Calendar.APRIL = 3)
    val selectedQuarter: Int = 1, // Q1
    val gstr1Filter: Gstr1InvoiceFilter = Gstr1InvoiceFilter.ALL
)

class GstReportsViewModel(
    private val repository: GstReportingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GstReportsUiState())
    val uiState: StateFlow<GstReportsUiState> = _uiState.asStateFlow()

    private var currentFyStart: Int = 0

    fun updatePeriod(period: FilingPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        load(currentFyStart)
    }

    fun updateMonth(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        load(currentFyStart)
    }

    fun updateQuarter(quarter: Int) {
        _uiState.value = _uiState.value.copy(selectedQuarter = quarter)
        load(currentFyStart)
    }

    fun updateGstr1Filter(filter: Gstr1InvoiceFilter) {
        _uiState.value = _uiState.value.copy(gstr1Filter = filter)
    }

    fun load(financialYearStart: Int) {
        currentFyStart = financialYearStart
        if (financialYearStart <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Valid Financial Year is required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val (startDate, endDate) = calculateDateRange(financialYearStart)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snapshot = repository.load(financialYearStart, startDate, endDate)
                )

            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snapshot = null,
                    errorMessage = exception.message ?: "Unable to load GST report."
                )
            }
        }
    }

    private fun calculateDateRange(fyStart: Int): Pair<String?, String?> {
        val state = _uiState.value
        return when (state.selectedPeriod) {
            FilingPeriod.FINANCIAL_YEAR -> {
                val start = "$fyStart-04-01"
                val end = "${fyStart + 1}-03-31"
                start to end
            }
            FilingPeriod.MONTHLY -> {
                val calendar = Calendar.getInstance()
                val month = state.selectedMonth
                val year = if (month >= Calendar.APRIL) fyStart else fyStart + 1
                
                calendar.set(year, month, 1)
                val start = formatDate(calendar)
                
                calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = formatDate(calendar)
                
                start to end
            }
            FilingPeriod.QUARTERLY -> {
                when (state.selectedQuarter) {
                    1 -> "$fyStart-04-01" to "$fyStart-06-30"
                    2 -> "$fyStart-07-01" to "$fyStart-09-30"
                    3 -> "$fyStart-10-01" to "$fyStart-12-31"
                    4 -> "${fyStart + 1}-01-01" to "${fyStart + 1}-03-31"
                    else -> null to null
                }
            }
        }
    }

    private fun formatDate(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", y, m, d)
    }
}

class GstReportsViewModelFactory(
    private val repository:
    GstReportingRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                GstReportsViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return GstReportsViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: " +
                    modelClass.name
        )
    }
}