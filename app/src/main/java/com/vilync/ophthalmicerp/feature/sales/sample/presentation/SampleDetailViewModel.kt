package com.vilync.ophthalmicerp.feature.sales.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import com.vilync.ophthalmicerp.data.repository.SampleIssueRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleDetailUiState(
    val isLoading: Boolean = true,
    val isActionRunning: Boolean = false,
    val sample: SampleIssueEntity? = null,
    val items: List<SampleIssueItemEntity> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class SampleDetailViewModel(
    private val sampleId: Long,
    private val repository: SampleIssueRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleDetailUiState())
    val uiState: StateFlow<SampleDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun returnToStock() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true, errorMessage = null) }
            runCatching {
                repository.returnCompleteSampleToStock(sampleId)
            }.onSuccess {
                _uiState.update { it.copy(isActionRunning = false) }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(isActionRunning = false, errorMessage = e.message) }
            }
        }
    }

    fun markAsEvaluated() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionRunning = true, errorMessage = null) }
            runCatching {
                repository.markCompleteSampleEvaluated(sampleId)
            }.onSuccess {
                _uiState.update { it.copy(isActionRunning = false) }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(isActionRunning = false, errorMessage = e.message) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = SampleDetailUiState(isLoading = true)
            runCatching {
                val sample = requireNotNull(repository.getSampleWithItems(sampleId)) { "Sample Note not found" }
                val company = companyProfileDao.getCompanyProfile()
                sample to company
            }.onSuccess { (pair, company) ->
                _uiState.update { it.copy(isLoading = false, sample = pair.first, items = pair.second, companyProfile = company) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}

class SampleDetailViewModelFactory(
    private val sampleId: Long,
    private val repository: SampleIssueRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SampleDetailViewModel(sampleId, repository, companyProfileDao) as T
    }
}
