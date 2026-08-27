package com.vilync.ophthalmicerp.feature.financialstatements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.financialstatements.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FinancialStatementsUiState(
    val isLoading: Boolean = false,
    val pendingDateFrom: String = "",
    val pendingDateTo: String = "",
    val appliedDateFrom: String = "",
    val appliedDateTo: String = "",
    val financialYearStart: Int = 2026,
    
    val selectedTab: Int = 0,
    
    val trialBalance: TrialBalanceReport? = null,
    val profitLoss: ProfitLossReport? = null,
    val balanceSheet: BalanceSheetReport? = null,
    
    val errorMessage: String? = null
)

class FinancialStatementsViewModel(
    private val useCase: FinancialStatementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialStatementsUiState())
    val uiState: StateFlow<FinancialStatementsUiState> = _uiState.asStateFlow()

    init {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val startOfFY = "01-04-${if (Calendar.getInstance().get(Calendar.MONTH) >= 3) Calendar.getInstance().get(Calendar.YEAR) else Calendar.getInstance().get(Calendar.YEAR) - 1}"
        
        _uiState.update { it.copy(
            pendingDateFrom = startOfFY, 
            pendingDateTo = today,
            appliedDateFrom = startOfFY,
            appliedDateTo = today
        ) }
        loadAllReports()
    }

    fun updatePendingFrom(date: String) {
        _uiState.update { it.copy(pendingDateFrom = date) }
    }

    fun updatePendingTo(date: String) {
        _uiState.update { it.copy(pendingDateTo = date) }
    }

    fun updateSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun applyFilters() {
        val state = _uiState.value
        
        // Date Validation
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        try {
            val from = sdf.parse(state.pendingDateFrom)
            val to = sdf.parse(state.pendingDateTo)
            if (from != null && to != null && from.after(to)) {
                _uiState.update { it.copy(errorMessage = "From Date cannot be later than To Date") }
                return
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Invalid date format") }
            return
        }

        _uiState.update { it.copy(
            appliedDateFrom = state.pendingDateFrom,
            appliedDateTo = state.pendingDateTo
        ) }
        loadAllReports()
    }

    fun loadAllReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val state = _uiState.value
                val tb = useCase.getTrialBalance(state.appliedDateFrom, state.appliedDateTo)
                val pl = useCase.getProfitLoss(state.appliedDateFrom, state.appliedDateTo)
                // Balance Sheet uses To Date as the cumulative as-of date.
                val bs = useCase.getBalanceSheet(state.appliedDateTo)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    trialBalance = tb,
                    profitLoss = pl,
                    balanceSheet = bs
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Error loading reports") }
            }
        }
    }
}

class FinancialStatementsViewModelFactory(
    private val useCase: FinancialStatementUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinancialStatementsViewModel(useCase) as T
    }
}
