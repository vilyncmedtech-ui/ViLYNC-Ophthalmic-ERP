package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreditNoteGroupedRow(
    val item: SalesCreditNoteItemEntity,
    val lenses: List<SalesCreditNoteLensEntity>,
    val hsn: String = ""
)

data class CreditNoteDetailUiState(
    val isLoading: Boolean = true,
    val creditNote: SalesCreditNoteEntity? = null,
    val lines: List<CreditNoteGroupedRow> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class CreditNoteDetailViewModel(
    private val creditNoteId: Long,
    private val repository: SalesCreditNoteRepository,
    private val productRepository: ProductRepository,
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
                val rawItems = repository.getItemsByCreditNoteId(creditNoteId)
                val allLenses = repository.getLensesByCreditNoteId(creditNoteId)
                val company = companyProfileDao.getCompanyProfile()

                // =====================================================
                // GROUPING BY 6-FIELD KEY (MATCHING TAX INVOICE)
                // =====================================================
                val groupedLines = rawItems.groupBy { item ->
                    val product = productRepository.getProductById(item.productId)
                    val hsn = product?.hsnCode ?: ""
                    "${item.productName}|${item.power}|${item.rate}|${item.discountPercent}|${item.gstPercent}|$hsn"
                }.values.map { group ->
                    val first = group.first()
                    val totalQty = group.sumOf { it.quantity }
                    val totalTaxable = group.sumOf { it.taxableAmount }
                    val totalGst = group.sumOf { it.gstAmount }
                    val totalAmt = group.sumOf { it.totalAmount }
                    
                    val groupItemIds = group.map { it.id }.toSet()
                    val groupLenses = allLenses.filter { it.creditNoteItemId in groupItemIds }
                    
                    val product = productRepository.getProductById(first.productId)
                    
                    CreditNoteGroupedRow(
                        item = first.copy(
                            quantity = totalQty,
                            taxableAmount = totalTaxable,
                            gstAmount = totalGst,
                            totalAmount = totalAmt
                        ),
                        lenses = groupLenses,
                        hsn = product?.hsnCode ?: ""
                    )
                }

                _uiState.value = CreditNoteDetailUiState(
                    isLoading = false,
                    creditNote = creditNote,
                    lines = groupedLines,
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
    private val productRepository: ProductRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CreditNoteDetailViewModel(creditNoteId, repository, productRepository, companyProfileDao) as T
    }
}
