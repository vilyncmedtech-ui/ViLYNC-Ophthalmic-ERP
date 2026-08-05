package com.vilync.ophthalmicerp.feature.sales.proforma.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import com.vilync.ophthalmicerp.data.repository.ProformaInvoiceRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProformaDetailUiState(
    val isLoading: Boolean = true,
    val proforma: ProformaInvoiceEntity? = null,
    val items: List<ProformaInvoiceItemEntity> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class ProformaDetailViewModel(
    private val proformaId: Long,
    private val repository: ProformaInvoiceRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProformaDetailUiState())
    val uiState: StateFlow<ProformaDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ProformaDetailUiState(isLoading = true)
            runCatching {
                val proforma = requireNotNull(repository.getProformaWithItems(proformaId)) { "Proforma not found" }
                val company = companyProfileDao.getCompanyProfile()
                proforma to company
            }.onSuccess { (pair, company) ->
                _uiState.value = ProformaDetailUiState(isLoading = false, proforma = pair.first, items = pair.second, companyProfile = company)
            }.onFailure {
                _uiState.value = ProformaDetailUiState(isLoading = false, errorMessage = it.message)
            }
        }
    }
}

class ProformaDetailViewModelFactory(
    private val proformaId: Long,
    private val repository: ProformaInvoiceRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProformaDetailViewModel(proformaId, repository, companyProfileDao) as T
    }
}
