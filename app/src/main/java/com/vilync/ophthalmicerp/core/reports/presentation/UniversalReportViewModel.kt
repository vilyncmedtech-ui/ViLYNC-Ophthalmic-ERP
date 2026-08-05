package com.vilync.ophthalmicerp.core.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.reports.domain.SortDirection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UniversalReportUiState(
    val isLoading: Boolean = false,
    val rows: List<ReportRowData> = emptyList(),
    val filteredRows: List<ReportRowData> = emptyList(),
    val paginatedRows: List<ReportRowData> = emptyList(),
    val summaries: Map<String, String> = emptyMap(),
    val filters: Map<String, Any?> = emptyMap(),
    val dynamicOptions: Map<String, List<String>> = emptyMap(),
    val visibleColumnIds: Set<String> = emptySet(),
    val sortColumnId: String? = null,
    val sortDirection: SortDirection = SortDirection.DESC,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val rowsPerPage: Int = 25,
    val totalRecords: Int = 0,
    val errorMessage: String? = null
)

class UniversalReportViewModel(
    val schema: ReportSchema,
    private val dataProvider: suspend (Map<String, Any?>) -> List<ReportRowData>,
    private val summaryCalculator: (List<ReportRowData>) -> Map<String, String>
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversalReportUiState(
        filters = schema.filters.associate { it.id to it.defaultValue },
        visibleColumnIds = schema.columns.filter { it.isDefaultVisible }.map { it.id }.toSet()
    ))
    val uiState: StateFlow<UniversalReportUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(overrideFilters: Map<String, Any?>? = null) {
        val currentFilters = overrideFilters ?: _uiState.value.filters
        _uiState.update { it.copy(isLoading = true, errorMessage = null, filters = currentFilters) }
        viewModelScope.launch {
            try {
                val data = dataProvider(currentFilters)
                val calculatedSummaries = summaryCalculator(data)
                _uiState.update { state ->
                    val filtered = applyClientSideFilters(data, state.searchQuery, state.sortColumnId, state.sortDirection)
                    state.copy(
                        isLoading = false,
                        rows = data,
                        filteredRows = filtered,
                        paginatedRows = applyPagination(filtered, 1, state.rowsPerPage),
                        summaries = calculatedSummaries,
                        totalRecords = filtered.size,
                        currentPage = 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load report") }
            }
        }
    }

    private fun applyPagination(rows: List<ReportRowData>, page: Int, rowsPerPage: Int): List<ReportRowData> {
        val start = (page - 1) * rowsPerPage
        val end = (start + rowsPerPage).coerceAtMost(rows.size)
        return if (start < rows.size) rows.subList(start, end) else emptyList()
    }

    private fun applyClientSideFilters(
        rows: List<ReportRowData>,
        searchQuery: String,
        sortColumnId: String?,
        sortDirection: SortDirection
    ): List<ReportRowData> {
        var result = rows
        
        // Universal Search
        if (searchQuery.isNotBlank()) {
            result = result.filter { row ->
                row.values.values.any { it?.toString()?.contains(searchQuery, ignoreCase = true) == true }
            }
        }

        // Client-side Sorting
        if (sortColumnId != null) {
            result = if (sortDirection == SortDirection.ASC) {
                result.sortedBy { it.values[sortColumnId]?.toString() ?: "" }
            } else {
                result.sortedByDescending { it.values[sortColumnId]?.toString() ?: "" }
            }
        }

        return result
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyClientSideFilters(state.rows, query, state.sortColumnId, state.sortDirection)
            state.copy(
                searchQuery = query,
                filteredRows = filtered,
                paginatedRows = applyPagination(filtered, 1, state.rowsPerPage),
                totalRecords = filtered.size,
                currentPage = 1
            )
        }
    }

    fun updateSorting(columnId: String) {
        _uiState.update { state ->
            val newDirection = if (state.sortColumnId == columnId && state.sortDirection == SortDirection.ASC) {
                SortDirection.DESC
            } else {
                SortDirection.ASC
            }
            val filtered = applyClientSideFilters(state.rows, state.searchQuery, columnId, newDirection)
            state.copy(
                sortColumnId = columnId,
                sortDirection = newDirection,
                filteredRows = filtered,
                paginatedRows = applyPagination(filtered, 1, state.rowsPerPage),
                currentPage = 1
            )
        }
    }

    fun updatePagination(page: Int, rowsPerPage: Int) {
        _uiState.update { state ->
            state.copy(
                currentPage = page,
                rowsPerPage = rowsPerPage,
                paginatedRows = applyPagination(state.filteredRows, page, rowsPerPage)
            )
        }
    }

    fun toggleColumnVisibility(columnId: String) {
        _uiState.update { 
            val newVisible = it.visibleColumnIds.toMutableSet()
            if (newVisible.contains(columnId)) newVisible.remove(columnId) else newVisible.add(columnId)
            it.copy(visibleColumnIds = newVisible)
        }
    }

    fun updateFilter(id: String, value: Any?) {
        val updatedFilters = _uiState.value.filters.toMutableMap()
        updatedFilters[id] = value
        _uiState.update { it.copy(filters = updatedFilters) }
        // For some filters (like search), we might want to wait, but for now we reload
        loadData()
    }

    fun setDynamicOptions(id: String, options: List<String>) {
        val updatedOptions = _uiState.value.dynamicOptions.toMutableMap()
        updatedOptions[id] = options
        _uiState.update { it.copy(dynamicOptions = updatedOptions) }
    }

    fun resetFilters() {
        val defaultFilters = schema.filters.associate { it.id to it.defaultValue }
        loadData(defaultFilters)
    }
}
