package com.vilync.ophthalmicerp.feature.sales.register

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SalesRegisterViewModel(
    private val repository: SalesRegisterRepository,
    val registerType: SalesRegisterType,
    private val initialFinancialYear: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesRegisterUiState(financialYearFilter = initialFinancialYear))
    val uiState: StateFlow<SalesRegisterUiState> = _uiState.asStateFlow()

    private val _filterOptions = MutableStateFlow(SalesRegisterFilterOptions())
    val filterOptions: StateFlow<SalesRegisterFilterOptions> = _filterOptions.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        repository.getRegisterRows(registerType)
            .onEach { rows ->
                _uiState.update { state ->
                    val filtered = filterRows(rows, state.query, state.statusFilter)
                    state.copy(
                        isLoading = false,
                        rows = rows,
                        filteredRows = filtered,
                        count = filtered.size,
                        totalAmount = filtered.sumOf { it.amount ?: 0.0 },
                        postedCount = filtered.count { it.status.uppercase() == "POSTED" },
                        cancelledCount = filtered.count { it.status.uppercase() == "CANCELLED" }
                    )
                }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        // The flow observation handles the actual data update
    }

    private fun filterRows(rows: List<SalesRegisterRow>, query: String, status: String): List<SalesRegisterRow> {
        return rows.filter { row ->
            val matchesQuery = query.isBlank() || 
                row.documentNumber.contains(query, ignoreCase = true) ||
                row.customerName.contains(query, ignoreCase = true)
            
            val matchesStatus = status == "ALL" || row.status.uppercase() == status.uppercase()
            
            matchesQuery && matchesStatus
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterRows(state.rows, query, state.statusFilter)
            state.copy(
                query = query,
                filteredRows = filtered,
                count = filtered.size,
                totalAmount = filtered.sumOf { it.amount ?: 0.0 },
                postedCount = filtered.count { it.status.uppercase() == "POSTED" },
                cancelledCount = filtered.count { it.status.uppercase() == "CANCELLED" }
            )
        }
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { state ->
            val filtered = filterRows(state.rows, state.query, status)
            state.copy(
                statusFilter = status,
                filteredRows = filtered,
                count = filtered.size,
                totalAmount = filtered.sumOf { it.amount ?: 0.0 },
                postedCount = filtered.count { it.status.uppercase() == "POSTED" },
                cancelledCount = filtered.count { it.status.uppercase() == "CANCELLED" }
            )
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelection = state.selectedIds.toMutableSet()
            if (newSelection.contains(id)) newSelection.remove(id) else newSelection.add(id)
            state.copy(selectedIds = newSelection)
        }
    }

    fun setCancelDialog(row: SalesRegisterRow) {
        _uiState.update { it.copy(showCancelDialog = true, cancelRowId = row.id, cancelReason = "") }
    }

    fun updateCancelReason(reason: String) {
        _uiState.update { it.copy(cancelReason = reason) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false, cancelRowId = null) }
    }

    fun confirmCancel() {
        val rowId = _uiState.value.cancelRowId ?: return
        val reason = _uiState.value.cancelReason
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true) }
            try {
                if (registerType == SalesRegisterType.INVOICE) {
                    repository.cancelInvoice(rowId, reason)
                } else {
                    // TODO: Implement cancellation for other document types
                    throw UnsupportedOperationException("Cancellation for ${registerType.displayName} is not yet implemented.")
                }
                _uiState.update { it.copy(isActionRunning = false, showCancelDialog = false, actionMessage = "${registerType.shortName} cancelled") }
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(isActionRunning = false, actionMessage = "Error: ${e.message}") }
            }
        }
    }

    fun deleteInvoice(saleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true) }
            try {
                repository.deleteInvoice(saleId)
                _uiState.update { it.copy(isActionRunning = false, actionMessage = "Invoice moved to Deleted.") }
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(isActionRunning = false, actionMessage = "Error: ${e.message}") }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun update(block: (SalesRegisterUiState) -> SalesRegisterUiState) {
        _uiState.update(block)
    }

    fun exportPdfAndShare(context: Context) {
        SalesRegisterExportSuite.exportPdfAndShare(context, registerType.displayName, _uiState.value.filteredRows)
    }

    fun exportExcelAndShare(context: Context) {
        SalesRegisterExportSuite.exportExcelAndShare(context, registerType.displayName, _uiState.value.filteredRows)
    }

    fun printRegister(context: Context) {
        SalesRegisterExportSuite.print(context, registerType.displayName, _uiState.value.filteredRows)
    }
}

class SalesRegisterViewModelFactory(
    private val repository: SalesRegisterRepository,
    private val registerType: SalesRegisterType,
    private val currentFinancialYear: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalesRegisterViewModel(repository, registerType, currentFinancialYear) as T
    }
}
