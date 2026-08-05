package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreditNoteDetailUiState(
    val isLoading: Boolean = true,
    val creditNote: SalesCreditNoteEntity? = null,
    val items: List<SalesCreditNoteItemEntity> = emptyList(),
    val lenses: List<SalesCreditNoteLensEntity> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class CreditNoteDetailViewModel(
    private val creditNoteId: Long,
    private val repository: SalesCreditNoteRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditNoteDetailUiState())
    val uiState: StateFlow<CreditNoteDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = CreditNoteDetailUiState(isLoading = true)
            runCatching {
                val creditNote = requireNotNull(repository.getCreditNoteById(creditNoteId)) { "Credit Note not found" }
                val items = repository.getItemsByCreditNoteId(creditNoteId)
                val lenses = repository.getLensesByCreditNoteId(creditNoteId)
                val company = companyProfileDao.getCompanyProfile()
                _uiState.value = CreditNoteDetailUiState(
                    isLoading = false,
                    creditNote = creditNote,
                    items = items,
                    lenses = lenses,
                    companyProfile = company
                )
            }.onFailure {
                _uiState.value = CreditNoteDetailUiState(isLoading = false, errorMessage = it.message)
            }
        }
    }
}

class CreditNoteDetailViewModelFactory(
    private val creditNoteId: Long,
    private val repository: SalesCreditNoteRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CreditNoteDetailViewModel(creditNoteId, repository, companyProfileDao) as T
    }
}
