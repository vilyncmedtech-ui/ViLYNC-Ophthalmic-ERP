package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChallanGroupedRow(
    val item: ChallanItemEntity,
    val items: List<ChallanItemEntity>, // All items in this group
    val hsn: String = ""
)

data class ChallanDetailUiState(
    val isLoading: Boolean = true,
    val challan: ChallanEntity? = null,
    val lines: List<ChallanGroupedRow> = emptyList(),
    val customer: PartyMaster? = null,
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class ChallanDetailViewModel(
    private val challanId: Long,
    private val repository: ChallanRepository,
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository,
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
                val rawItems = repository.getItemsByChallanId(challanId)
                val company = companyProfileDao.getCompanyProfile()
                val customer = partyRepository.getPartyById(challan.customerId)

                // =====================================================
                // GROUPING BY 6-FIELD KEY (CONSISTENT WITH TAX INVOICE)
                // =====================================================
                val groupedLines = rawItems.groupBy { item ->
                    val product = productRepository.getProductById(item.productId)
                    val hsn = product?.hsnCode ?: ""
                    // Discount% is 0.0 for Challan
                    "${item.productName}|${item.power}|${item.rate}|0.0|${item.gstPercent}|$hsn"
                }.values.map { group ->
                    val first = group.first()
                    val product = productRepository.getProductById(first.productId)
                    ChallanGroupedRow(
                        item = first, // Snapshot of first item as base
                        items = group,
                        hsn = product?.hsnCode ?: ""
                    )
                }

                Triple(challan, groupedLines, company to customer)
            }.onSuccess { (challan, lines, pair) ->
                _uiState.value = ChallanDetailUiState(
                    isLoading = false, 
                    challan = challan, 
                    lines = lines, 
                    companyProfile = pair.first,
                    customer = pair.second
                )
            }.onFailure {
                _uiState.value = ChallanDetailUiState(isLoading = false, errorMessage = it.message)
            }
        }
    }
}

class ChallanDetailViewModelFactory(
    private val challanId: Long,
    private val repository: ChallanRepository,
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChallanDetailViewModel(challanId, repository, productRepository, partyRepository, companyProfileDao) as T
    }
}
