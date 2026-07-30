package com.vilync.ophthalmicerp.feature.gst.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.gst.data.GstSummaryRepository
import com.vilync.ophthalmicerp.feature.gst.model.GstSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GstDashboardUiState(
    val financialYearStart: Int = 0,
    val isLoading: Boolean = false,
    val summary: GstSummary? = null,
    val errorMessage: String? = null
)

class GstDashboardViewModel(
    private val repository: GstSummaryRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            GstDashboardUiState()
        )

    val uiState:
            StateFlow<GstDashboardUiState> =
        _uiState.asStateFlow()

    private var summaryJob: Job? = null

    fun loadFinancialYear(
        financialYearStart: Int
    ) {

        if (financialYearStart <= 0) {

            summaryJob?.cancel()

            _uiState.value =
                GstDashboardUiState(
                    financialYearStart =
                        financialYearStart,

                    errorMessage =
                        "Valid Financial Year is required."
                )

            return
        }

        if (
            _uiState.value.financialYearStart ==
            financialYearStart &&
            summaryJob?.isActive == true
        ) {
            return
        }

        summaryJob?.cancel()

        _uiState.value =
            GstDashboardUiState(
                financialYearStart =
                    financialYearStart,

                isLoading = true
            )

        summaryJob =
            viewModelScope.launch {

                try {

                    repository
                        .observeSummary(
                            financialYearStart
                        )
                        .collect { summary ->

                            _uiState.value =
                                GstDashboardUiState(
                                    financialYearStart =
                                        financialYearStart,

                                    summary =
                                        summary
                                )
                        }

                } catch (
                    exception: Exception
                ) {

                    _uiState.value =
                        GstDashboardUiState(
                            financialYearStart =
                                financialYearStart,

                            errorMessage =
                                exception.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Unable to load GST summary."
                        )
                }
            }
    }
}

class GstDashboardViewModelFactory(
    private val repository:
    GstSummaryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                GstDashboardViewModel::class.java
            )
        ) {

            return GstDashboardViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: " +
                    modelClass.name
        )
    }
}