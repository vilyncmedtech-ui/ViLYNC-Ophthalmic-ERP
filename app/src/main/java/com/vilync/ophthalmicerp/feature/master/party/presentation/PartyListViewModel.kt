package com.vilync.ophthalmicerp.feature.master.party.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PartyListViewModel(
    private val repository: com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListUiState()
        )

    val uiState: StateFlow<com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // CURRENT SOURCE LIST
    // =========================================================
    //
    // Repository currently exposes active parties.
    // Filtering by Party Type is performed locally.
    // =========================================================

    private var sourceParties:
            List<com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster> =
        emptyList()


    // =========================================================
    // OBSERVATION JOB
    // =========================================================

    private var partiesJob: Job? =
        null


    // =========================================================
    // INITIAL LOAD
    // =========================================================

    init {

        loadParties()
    }


    // =========================================================
    // LOAD ACTIVE PARTIES
    // =========================================================

    fun loadParties() {

        partiesJob?.cancel()


        _uiState.value =
            _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )


        partiesJob =
            viewModelScope.launch {

                try {

                    repository
                        .getAllActiveParties()
                        .collect { parties ->

                            sourceParties =
                                parties

                            applyFilters()

                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = null
                                )
                        }


                } catch (exception: Exception) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,

                            errorMessage =
                                exception.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Unable to load parties."
                        )
                }
            }
    }


    // =========================================================
    // UPDATE SEARCH QUERY
    // =========================================================

    fun updateSearchQuery(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = value,
                errorMessage = null
            )


        searchParties(
            value
        )
    }


    // =========================================================
    // SEARCH PARTIES
    // =========================================================

    private fun searchParties(
        query: String
    ) {

        partiesJob?.cancel()


        if (query.isBlank()) {

            loadParties()

            return
        }


        _uiState.value =
            _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )


        partiesJob =
            viewModelScope.launch {

                try {

                    repository
                        .searchParties(
                            query.trim()
                        )
                        .collect { parties ->

                            /*
                             * Current Party List is intended
                             * to show active parties only.
                             *
                             * This additional check also keeps
                             * behaviour consistent if DAO search
                             * later returns inactive records.
                             */

                            sourceParties =
                                parties.filter {
                                    it.isActive
                                }


                            applyFilters()


                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = null
                                )
                        }


                } catch (exception: Exception) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,

                            errorMessage =
                                exception.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Unable to search parties."
                        )
                }
            }
    }


    // =========================================================
    // PARTY TYPE FILTER
    // =========================================================

    fun updatePartyTypeFilter(
        partyType: com.vilync.ophthalmicerp.feature.master.party.model.PartyType?
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedPartyType =
                    partyType
            )


        applyFilters()
    }


    // =========================================================
    // ACTIVE STATUS FILTER
    // =========================================================
    //
    // Infrastructure is retained in UiState.
    //
    // For now repository supplies active parties only.
    // Full Active / Inactive / All support will be connected
    // after extending PartyDao + PartyRepository.
    // =========================================================

    fun updateActiveStatusFilter(
        status: Boolean?
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedActiveStatus =
                    status
            )


        applyFilters()
    }


    // =========================================================
    // APPLY LOCAL FILTERS
    // =========================================================

    private fun applyFilters() {

        val currentState =
            _uiState.value


        var filteredParties =
            sourceParties


        // -----------------------------------------------------
        // PARTY TYPE
        // -----------------------------------------------------

        currentState.selectedPartyType
            ?.let { selectedType ->

                filteredParties =
                    filteredParties.filter {
                            party ->

                        when (selectedType) {

                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER ->

                                party.partyType ==
                                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER ||
                                        party.partyType ==
                                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH


                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.VENDOR ->

                                party.partyType ==
                                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.VENDOR ||
                                        party.partyType ==
                                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH


                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH ->

                                party.partyType ==
                                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH
                        }
                    }
            }


        // -----------------------------------------------------
        // ACTIVE STATUS
        // -----------------------------------------------------

        currentState.selectedActiveStatus
            ?.let { selectedStatus ->

                filteredParties =
                    filteredParties.filter {
                            party ->

                        party.isActive ==
                                selectedStatus
                    }
            }


        // -----------------------------------------------------
        // SORT
        // -----------------------------------------------------

        filteredParties =
            filteredParties.sortedBy {
                it.partyName.lowercase()
            }


        _uiState.value =
            currentState.copy(
                parties =
                    filteredParties
            )
    }


    // =========================================================
    // REFRESH
    // =========================================================

    fun refresh() {

        val searchQuery =
            _uiState.value
                .searchQuery


        if (searchQuery.isBlank()) {

            loadParties()

        } else {

            searchParties(
                searchQuery
            )
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


        loadParties()
    }


    // =========================================================
    // CLEAR FILTERS
    // =========================================================

    fun clearFilters() {

        _uiState.value =
            _uiState.value.copy(
                selectedPartyType = null,
                selectedActiveStatus = null
            )


        applyFilters()
    }


    // =========================================================
    // CLEAR ERROR
    // =========================================================

    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }
}