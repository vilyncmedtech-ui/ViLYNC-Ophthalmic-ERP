package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChallanDetailUiState(
    val isLoading: Boolean = true,
    val challan: ChallanEntity? = null,
    val items: List<ChallanItemEntity> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class ChallanDetailViewModel(
    private val challanId: Long,
    private val repository: ChallanRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallanDetailUiState())
    val uiState: StateFlow<ChallanDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ChallanDetailUiState(isLoading = true)
            runCatching {
                val challan = requireNotNull(repository.getChallanById(challanId)) { "Challan not found" }
                val items = repository.getItemsByChallanId(challanId)
                val company = companyProfileDao.getCompanyProfile()
                Triple(challan, items, company)
            }.onSuccess { (challan, items, company) ->
                _uiState.value = ChallanDetailUiState(isLoading = false, challan = challan, items = items, companyProfile = company)
            }.onFailure {
                _uiState.value = ChallanDetailUiState(isLoading = false, errorMessage = it.message)
            }
        }
    }
}

class ChallanDetailViewModelFactory(
    private val challanId: Long,
    private val repository: ChallanRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChallanDetailViewModel(challanId, repository, companyProfileDao) as T
    }
}
