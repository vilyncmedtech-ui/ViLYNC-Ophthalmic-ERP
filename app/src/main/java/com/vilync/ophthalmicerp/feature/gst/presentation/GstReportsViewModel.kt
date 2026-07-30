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

data class GstReportsUiState(
    val isLoading: Boolean = false,
    val snapshot: GstReportSnapshot? = null,
    val errorMessage: String? = null
)

class GstReportsViewModel(
    private val repository:
    GstReportingRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            GstReportsUiState()
        )

    val uiState:
            StateFlow<GstReportsUiState> =
        _uiState.asStateFlow()

    fun load(
        financialYearStart: Int
    ) {

        if (financialYearStart <= 0) {

            _uiState.value =
                GstReportsUiState(
                    errorMessage =
                        "Valid Financial Year is required."
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

            try {

                _uiState.value =
                    GstReportsUiState(
                        isLoading = false,

                        snapshot =
                            repository.load(
                                financialYearStart
                            )
                    )

            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    GstReportsUiState(
                        isLoading = false,

                        snapshot = null,

                        errorMessage =
                            exception.message
                                ?: "Unable to load GST report."
                    )
            }
        }
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