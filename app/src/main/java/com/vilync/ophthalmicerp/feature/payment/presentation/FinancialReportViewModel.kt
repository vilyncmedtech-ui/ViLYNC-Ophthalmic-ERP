package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.reports.domain.*
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportUiState
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.payment.logic.FinancialReportingUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialReportViewModel(
    val schema: ReportSchema,
    private val reportingUseCase: FinancialReportingUseCase,
    private val partyRepository: PartyRepository,
    private val accountRepository: AccountRepository,
    private val initialPartyId: Long = 0L
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversalReportUiState(
        filters = schema.filters.associate { it.id to it.defaultValue },
        visibleColumnIds = schema.columns.filter { it.isDefaultVisible }.map { it.id }.toSet()
    ))
    val uiState: StateFlow<UniversalReportUiState> = _uiState.asStateFlow()

    private var partyMap = mapOf<String, Long>()
    private var accountMap = mapOf<String, Long>()

    init {
        loadDynamicOptions()
        // If initialPartyId is provided, we don't call loadData() here yet, 
        // we'll wait for party list to load and then set the filter.
        if (initialPartyId == 0L) {
            loadData()
        }
    }

    private fun loadDynamicOptions() {
        viewModelScope.launch {
            if (schema.id == "customer_ledger" || schema.id == "supplier_ledger") {
                partyRepository.getAllActiveParties().collect { parties ->
                    val filtered = if (schema.id == "customer_ledger") {
                        parties.filter { it.partyType.name == "CUSTOMER" || it.partyType.name == "BOTH" }
                    } else {
                        parties.filter { it.partyType.name == "SUPPLIER" || it.partyType.name == "BOTH" }
                    }
                    partyMap = filtered.associate { it.partyName to it.id }
                    setDynamicOptions("party_id", filtered.map { it.partyName })
                    
                    if (initialPartyId > 0L) {
                        val partyName = filtered.find { it.id == initialPartyId }?.partyName
                        if (partyName != null) {
                            updateFilter("party_id", partyName)
                        } else {
                            loadData()
                        }
                    }
                }
            } else if (schema.id == "bank_book") {
                accountRepository.getAccountsByType("BANK").collect { accounts ->
                    accountMap = accounts.associate { it.name to it.id }
                    setDynamicOptions("account_id", accounts.map { it.name })
                }
            }
        }
    }

    private fun setDynamicOptions(id: String, options: List<String>) {
        val updatedOptions = _uiState.value.dynamicOptions.toMutableMap()
        updatedOptions[id] = options
        _uiState.update { it.copy(dynamicOptions = updatedOptions) }
    }

    fun updateFilter(id: String, value: Any?) {
        val updatedFilters = _uiState.value.filters.toMutableMap()
        updatedFilters[id] = value
        _uiState.update { it.copy(filters = updatedFilters) }
        loadData()
    }

    fun resetFilters() {
        val defaultFilters = schema.filters.associate { it.id to it.defaultValue }
        loadData(defaultFilters)
    }

    fun loadData(overrideFilters: Map<String, Any?>? = null) {
        val currentFilters = overrideFilters ?: _uiState.value.filters
        _uiState.update { it.copy(isLoading = true, errorMessage = null, filters = currentFilters) }
        
        viewModelScope.launch {
            try {
                val startDate = currentFilters["date_from"]?.toString() ?: ""
                val endDate = currentFilters["date_to"]?.toString() ?: ""
                
                val statement = when (schema.id) {
                    "customer_ledger" -> {
                        val partyName = currentFilters["party_id"]?.toString() ?: ""
                        val partyId = partyMap[partyName] ?: 0L
                        reportingUseCase.getCustomerLedger(partyId, startDate, endDate)
                    }
                    "supplier_ledger" -> {
                        val partyName = currentFilters["party_id"]?.toString() ?: ""
                        val partyId = partyMap[partyName] ?: 0L
                        reportingUseCase.getSupplierLedger(partyId, startDate, endDate)
                    }
                    "cash_book" -> {
                        // Assuming "Main Cash" or similar. For now, let's find the first CASH account.
                        val cashAccounts = accountRepository.getAccountsByType("CASH").first()
                        val accountId = cashAccounts.firstOrNull()?.id ?: 0L
                        reportingUseCase.getAccountBook(accountId, startDate, endDate)
                    }
                    "bank_book" -> {
                        val accountName = currentFilters["account_id"]?.toString() ?: ""
                        val accountId = accountMap[accountName] ?: 0L
                        reportingUseCase.getAccountBook(accountId, startDate, endDate)
                    }
                    else -> throw IllegalArgumentException("Unknown report schema")
                }

                val reportRows = statement.rows.mapIndexed { index, row ->
                    ReportRowData(
                        id = index.toLong(),
                        values = mapOf(
                            "date" to row.date,
                            "refNo" to row.refNo,
                            "particulars" to row.particulars,
                            "debit" to row.debit,
                            "credit" to row.credit,
                            "balance" to row.balance
                        )
                    )
                }

                val summaries = mapOf(
                    "opening" to String.format(java.util.Locale.US, "%.2f", statement.openingBalance),
                    "debit" to String.format(java.util.Locale.US, "%.2f", statement.totalDebit),
                    "credit" to String.format(java.util.Locale.US, "%.2f", statement.totalCredit),
                    "closing" to String.format(java.util.Locale.US, "%.2f", statement.closingBalance)
                )

                _uiState.update { state ->
                    val filtered = applyClientSideFilters(reportRows, state.searchQuery, state.sortColumnId, state.sortDirection)
                    state.copy(
                        isLoading = false,
                        rows = reportRows,
                        filteredRows = filtered,
                        paginatedRows = filtered, // No pagination for now to keep it simple as per ledger requirements
                        summaries = summaries,
                        totalRecords = filtered.size,
                        currentPage = 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load report") }
            }
        }
    }

    private fun applyClientSideFilters(
        rows: List<ReportRowData>,
        searchQuery: String,
        sortColumnId: String?,
        sortDirection: SortDirection
    ): List<ReportRowData> {
        var result = rows
        if (searchQuery.isNotBlank()) {
            result = result.filter { row ->
                row.values.values.any { it?.toString()?.contains(searchQuery, ignoreCase = true) == true }
            }
        }
        if (sortColumnId != null) {
            result = if (sortDirection == SortDirection.ASC) {
                result.sortedBy { it.values[sortColumnId]?.toString() ?: "" }
            } else {
                result.sortedByDescending { it.values[sortColumnId]?.toString() ?: "" }
            }
        }
        return result
    }
}

class FinancialReportViewModelFactory(
    private val schema: ReportSchema,
    private val reportingUseCase: FinancialReportingUseCase,
    private val partyRepository: PartyRepository,
    private val accountRepository: AccountRepository,
    private val initialPartyId: Long = 0L
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinancialReportViewModel(schema, reportingUseCase, partyRepository, accountRepository, initialPartyId) as T
    }
}
