package com.vilync.ophthalmicerp.feature.settings.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.AuditTrailEntity
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


// =============================================================
// AUDIT TRAIL UI STATE
// =============================================================

data class AuditTrailUiState(

    val records: List<AuditTrailEntity> = emptyList(),

    val searchQuery: String = "",

    val isLoading: Boolean = true,

    val errorMessage: String? = null
)


// =============================================================
// AUDIT TRAIL VIEWMODEL
// =============================================================

class AuditTrailViewModel(

    private val auditTrailRepository: AuditTrailRepository

) : ViewModel() {


    private val _uiState =
        MutableStateFlow(
            AuditTrailUiState()
        )


    val uiState: StateFlow<AuditTrailUiState> =
        _uiState.asStateFlow()


    init {

        observeAuditTrail(
            query = ""
        )
    }


    // =========================================================
    // SEARCH
    // =========================================================

    fun updateSearchQuery(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = value
            )


        observeAuditTrail(
            query = value
        )
    }


    // =========================================================
    // OBSERVE AUDIT TRAIL
    // =========================================================

    private var auditObserverJob:
            kotlinx.coroutines.Job? = null


    private fun observeAuditTrail(
        query: String
    ) {

        auditObserverJob?.cancel()


        auditObserverJob =
            viewModelScope.launch {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        errorMessage = null
                    )


                try {

                    auditTrailRepository
                        .searchAuditTrail(
                            query = query
                        )
                        .collectLatest { auditRecords ->

                            _uiState.value =
                                _uiState.value.copy(
                                    records = auditRecords,
                                    isLoading = false,
                                    errorMessage = null
                                )
                        }

                } catch (_: Exception) {

                    _uiState.value =
                        _uiState.value.copy(
                            records = emptyList(),
                            isLoading = false,
                            errorMessage =
                                "Unable to load audit trail."
                        )
                }
            }
    }


    // =========================================================
    // CLEAR SEARCH
    // =========================================================

    fun clearSearch() {

        if (
            _uiState.value.searchQuery.isBlank()
        ) {
            return
        }


        _uiState.value =
            _uiState.value.copy(
                searchQuery = ""
            )


        observeAuditTrail(
            query = ""
        )
    }
}


// =============================================================
// AUDIT TRAIL VIEWMODEL FACTORY
// =============================================================

class AuditTrailViewModelFactory(

    private val auditTrailRepository: AuditTrailRepository

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                AuditTrailViewModel::class.java
            )
        ) {

            return AuditTrailViewModel(
                auditTrailRepository =
                    auditTrailRepository
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}