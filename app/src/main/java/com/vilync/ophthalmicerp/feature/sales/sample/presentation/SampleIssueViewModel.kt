package com.vilync.ophthalmicerp.feature.sales.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.repository.SampleIssueRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.core.util.SerialFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SampleIssueUiState(
    val sampleId: Long = 0,
    val sampleNumber: String = "",
    val issueDate: String = "",
    val expectedReturnDate: String = "",
    val financialYearStart: Int = 0,
    val selectedCustomer: PartyMaster? = null,
    val customers: List<PartyMaster> = emptyList(),
    val sampleType: String = "Evaluation",
    val items: List<SampleIssueItemUi> = emptyList(),
    val remarks: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedSampleId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Serial Search
    val serialQuery: String = "",
    val isSearchingSerial: Boolean = false,
    val serialMatches: List<InventoryUnitEntity> = emptyList()
)

data class SampleIssueItemUi(
    val inventoryUnitId: Long,
    val productId: Long,
    val productName: String,
    val power: String,
    val serialNumber: String,
    val batchNumber: String,
    val expiryDate: String
)

class SampleIssueViewModel(
    private val repository: SampleIssueRepository,
    private val partyRepository: PartyRepository,
    private val inventoryRepository: InventoryRepository,
    private val productRepository: com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleIssueUiState(issueDate = today(), expectedReturnDate = in15Days()))
    val uiState: StateFlow<SampleIssueUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            partyRepository.getAllActiveParties().collect { parties ->
                val customers = parties.filter { it.partyType == PartyType.CUSTOMER || it.partyType == PartyType.BOTH }
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun loadSample(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val pair = repository.getSampleWithItems(id) ?: throw Exception("Sample Note not found")
                val (sample, items) = pair
                
                val customer = partyRepository.getPartyById(sample.customerId)
                
                _uiState.update { 
                    it.copy(
                        sampleId = sample.id,
                        sampleNumber = sample.sampleIssueNumber,
                        issueDate = sample.sampleIssueDate,
                        expectedReturnDate = sample.expectedReturnDate,
                        financialYearStart = sample.financialYearStart,
                        selectedCustomer = customer,
                        sampleType = sample.sampleType,
                        items = items.map { entity -> 
                            SampleIssueItemUi(
                                inventoryUnitId = entity.inventoryUnitId,
                                productId = entity.productId,
                                productName = entity.productName,
                                power = entity.power,
                                serialNumber = entity.serialNumber,
                                batchNumber = entity.batchNumber,
                                expiryDate = entity.expiryDate
                            )
                        },
                        remarks = sample.remarks,
                        isEditMode = true,
                        isSaved = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectCustomer(customer: PartyMaster) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(issueDate = date, financialYearStart = financialYearStart(date)) }
    }

    fun updateExpectedReturnDate(date: String) {
        _uiState.update { it.copy(expectedReturnDate = date) }
    }

    fun updateSampleType(type: String) {
        _uiState.update { it.copy(sampleType = type) }
    }

    fun updateRemarks(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun updateSerialQuery(query: String) {
        _uiState.update { it.copy(serialQuery = query) }
    }

    fun searchSerial() {
        val query = _uiState.value.serialQuery.trim()
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSerial = true) }
            try {
                // Numeric-only search with automatic formatting
                val numericPart = query.filter { it.isDigit() }
                val formattedQuery = if (numericPart.isNotEmpty()) {
                    // Try formatting with standard prefix if not already formatted
                    if (!query.any { it.isLetter() }) {
                         SerialFormatter.format("LMDE", numericPart)
                    } else query
                } else query

                val matches = inventoryRepository.smartSearchInStockSerial(formattedQuery)
                if (matches.size == 1) {
                    addItem(matches.first())
                } else {
                    _uiState.update { it.copy(serialMatches = matches, isSearchingSerial = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSerial = false, errorMessage = e.message) }
            }
        }
    }

    fun addItem(unit: InventoryUnitEntity) {
        if (_uiState.value.items.any { it.inventoryUnitId == unit.id }) return
        
        viewModelScope.launch {
            val product = productRepository.getProductById(unit.productId)
            val item = SampleIssueItemUi(
                inventoryUnitId = unit.id,
                productId = unit.productId,
                productName = product?.productName ?: "Unknown Product",
                power = unit.power,
                serialNumber = unit.serialNumber,
                batchNumber = unit.batchNumber,
                expiryDate = unit.expiryDate
            )
            _uiState.update { it.copy(
                items = it.items + item,
                serialQuery = "",
                serialMatches = emptyList(),
                isSearchingSerial = false
            ) }
        }
    }

    fun removeItem(index: Int) {
        val updated = _uiState.value.items.toMutableList().apply { removeAt(index) }
        _uiState.update { it.copy(items = updated) }
    }

    fun save() {
        val state = _uiState.value
        if (state.selectedCustomer == null || state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select customer and add serials.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = System.currentTimeMillis()
                val sampleIssue = SampleIssueEntity(
                    id = state.sampleId,
                    customerId = state.selectedCustomer.id,
                    customerName = state.selectedCustomer.partyName,
                    sampleIssueNumber = state.sampleNumber,
                    normalizedSampleIssueNumber = state.sampleNumber.uppercase(),
                    sampleIssueDate = state.issueDate,
                    financialYearStart = state.financialYearStart,
                    sampleType = state.sampleType,
                    expectedReturnDate = state.expectedReturnDate,
                    remarks = state.remarks,
                    status = "ISSUED",
                    createdAt = now,
                    updatedAt = now
                )
                
                val items = state.items.map {
                    SampleIssueItemEntity(
                        sampleIssueId = state.sampleId,
                        inventoryUnitId = it.inventoryUnitId,
                        productId = it.productId,
                        productName = it.productName,
                        power = it.power,
                        serialNumber = it.serialNumber,
                        batchNumber = it.batchNumber,
                        expiryDate = it.expiryDate,
                        settlementStatus = "ISSUED",
                        createdAt = now,
                        updatedAt = now
                    )
                }

                val sampleId = if (state.isEditMode) {
                    repository.updateCompleteSampleIssue(sampleIssue, items)
                    state.sampleId
                } else {
                    repository.saveCompleteSampleIssue(sampleIssue, items)
                }
                
                _uiState.update { it.copy(isSaving = false, isSaved = true, savedSampleId = sampleId, successMessage = "Sample Distribution Note saved successfully.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save Sample Issue.") }
            }
        }
    }

    private fun financialYearStart(date: String): Int {
        return runCatching {
            val d = SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date)
            val cal = Calendar.getInstance().apply { time = d }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            if (month >= 4) year else year - 1
        }.getOrDefault(Calendar.getInstance().get(Calendar.YEAR))
    }

    private fun today() = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    private fun in15Days(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 15)
        return SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time)
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
