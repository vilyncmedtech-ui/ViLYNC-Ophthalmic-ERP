package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.repository.SerialStockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch


class SerialStockViewModel(

    private val repository: SerialStockRepository

) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            SerialStockUiState()
        )


    val uiState: StateFlow<SerialStockUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // ACTIVE STOCK OBSERVATION JOB
    // =========================================================

    private var stockJob: Job? = null


    // =========================================================
    // INITIAL LOAD
    // =========================================================

    init {

        loadStock()
    }


    // =========================================================
    // SEARCH QUERY CHANGED
    // =========================================================

    fun onSearchQueryChanged(
        query: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = query
            )

        loadStock()
    }


    // =========================================================
    // STATUS FILTER CHANGED
    // =========================================================

    fun onStatusSelected(
        status: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedStatus =
                    status
                        .trim()
                        .uppercase()
            )

        loadStock()
    }


    // =========================================================
    // REFRESH
    // =========================================================

    fun refresh() {

        loadStock()
    }


    // =========================================================
    // LOAD SERIAL STOCK
    // =========================================================

    private fun loadStock() {

        // Stop observing the previous query/filter Flow.
        stockJob?.cancel()


        stockJob =
            viewModelScope.launch {

                val currentState =
                    _uiState.value


                val query =
                    currentState
                        .searchQuery
                        .trim()


                val status =
                    currentState
                        .selectedStatus
                        .trim()
                        .uppercase()


                // =================================================
                // SELECT CORRECT DATABASE FLOW
                // =================================================

                val source =

                    when {

                        // -----------------------------------------
                        // SEARCH ACTIVE
                        // -----------------------------------------

                        query.isNotBlank() -> {

                            repository
                                .searchSerialStock(
                                    query = query
                                )
                        }


                        // -----------------------------------------
                        // STATUS FILTER ACTIVE
                        // -----------------------------------------

                        status.isNotBlank() &&
                                status != "ALL" -> {

                            repository
                                .getSerialStockByStatus(
                                    status = status
                                )
                        }


                        // -----------------------------------------
                        // COMPLETE REGISTER
                        // -----------------------------------------

                        else -> {

                            repository
                                .getSerialStockRegister()
                        }
                    }


                // =================================================
                // OBSERVE ROOM FLOW
                // =================================================

                source
                    .onStart {

                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = true,
                                errorMessage = null
                            )
                    }
                    .catch { throwable ->

                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage =
                                    throwable.message
                                        ?: "Unable to load Serial Stock."
                            )
                    }
                    .collect { rows ->


                        // =========================================
                        // SEARCH + STATUS TOGETHER
                        // =========================================
                        //
                        // DAO search handles text search.
                        //
                        // If user also selected a status,
                        // apply that status to search results.
                        // =========================================

                        val finalRows =

                            if (
                                query.isNotBlank() &&
                                status.isNotBlank() &&
                                status != "ALL"
                            ) {

                                rows.filter { row ->

                                    row.status.equals(
                                        other = status,
                                        ignoreCase = true
                                    )
                                }

                            } else {

                                rows
                            }


                        // =========================================
                        // UPDATE UI
                        // =========================================

                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                serialStock = finalRows,
                                errorMessage = null
                            )
                    }
            }
    }


    // =========================================================
    // CLEAR SEARCH
    // =========================================================

    fun clearSearch() {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = ""
            )

        loadStock()
    }


    // =========================================================
    // RESET FILTERS
    // =========================================================

    fun resetFilters() {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = "",
                selectedStatus = "ALL"
            )

        loadStock()
    }
}