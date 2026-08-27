package com.vilync.ophthalmicerp.feature.sales.invoicehub.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.sales.detail.SalesInvoiceDetailExportSuite
import com.vilync.ophthalmicerp.feature.sales.detail.SalesInvoiceDetailLine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InvoiceHubUiState(
    val isLoading: Boolean = false,
    val invoices: List<SaleEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val partySearch: String = "",
    val dateFrom: String = "", // dd-MM-yyyy
    val dateTo: String = "",   // dd-MM-yyyy
    val statusFilter: String = "POSTED",
    val selectedParty: PartyMaster? = null,
    val partySuggestions: List<PartyMaster> = emptyList(),
    val errorMessage: String? = null,
    val isExporting: Boolean = false
)

class InvoiceHubViewModel(
    private val repository: SalesRepository,
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceHubUiState())
    val uiState: StateFlow<InvoiceHubUiState> = _uiState.asStateFlow()

    private var allParties: List<PartyMaster> = emptyList()

    init {
        // Set default dates: current month
        val calendar = Calendar.getInstance()
        val toDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val fromDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)
        
        _uiState.update { it.copy(dateFrom = fromDate, dateTo = toDate) }
        
        observeParties()
        loadInvoices()
    }

    private fun observeParties() {
        partyRepository.getAllActiveParties()
            .onEach { parties ->
                allParties = parties
            }
            .launchIn(viewModelScope)
    }

    fun loadInvoices() {
        val state = _uiState.value
        val start = convertToSqlDate(state.dateFrom)
        val end = convertToSqlDate(state.dateTo)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val invoices = repository.getFilteredSalesForHub(
                    query = state.partySearch,
                    status = state.statusFilter,
                    startDate = start,
                    endDate = end,
                    customerId = state.selectedParty?.id
                )
                _uiState.update { it.copy(isLoading = false, invoices = invoices) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load invoices") }
            }
        }
    }

    private fun convertToSqlDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else ""
        } catch (e: Exception) { "" }
    }

    fun updatePartySearch(query: String) {
        _uiState.update { state ->
            val suggestions = if (query.isBlank()) emptyList()
            else allParties.filter { 
                it.partyName.contains(query, ignoreCase = true) ||
                it.legalName.contains(query, ignoreCase = true)
            }.take(5)
            
            state.copy(partySearch = query, partySuggestions = suggestions)
        }
        loadInvoices()
    }

    fun selectParty(party: PartyMaster?) {
        _uiState.update { it.copy(selectedParty = party, partySearch = "", partySuggestions = emptyList()) }
        loadInvoices()
    }

    fun updateDateFrom(date: String) {
        _uiState.update { it.copy(dateFrom = date) }
        loadInvoices()
    }

    fun updateDateTo(date: String) {
        _uiState.update { it.copy(dateTo = date) }
        loadInvoices()
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        loadInvoices()
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSet = state.selectedIds.toMutableSet()
            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
            state.copy(selectedIds = newSet)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.invoices.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun exportSelected(context: Context) {
        val selectedIds = _uiState.value.selectedIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val profile = companyProfileDao.getCompanyProfile()
                val selectedInvoices = _uiState.value.invoices.filter { selectedIds.contains(it.id) }
                
                val invoiceData = selectedInvoices.map { sale ->
                    val items = repository.getSaleItems(sale.id).first()
                    val lines = items.map { item ->
                        val savedLenses = repository.getSaleLenses(item.id).first()
                        val productMaster = productRepository.getProductById(item.productId)
                        
                        SalesInvoiceDetailLine(
                            item = item,
                            lenses = savedLenses,
                            hsnFromMaster = productMaster?.hsnCode ?: item.hsnCode,
                            productModel = productMaster?.model ?: ""
                        )
                    }
                    sale to lines
                }

                SalesInvoiceDetailExportSuite.exportBulkPdfAndShare(context, invoiceData, profile)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
}

class InvoiceHubViewModelFactory(
    private val repository: SalesRepository,
    private val productRepository: ProductRepository,
    private val partyRepository: PartyRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InvoiceHubViewModel(repository, productRepository, partyRepository, companyProfileDao) as T
    }
}
