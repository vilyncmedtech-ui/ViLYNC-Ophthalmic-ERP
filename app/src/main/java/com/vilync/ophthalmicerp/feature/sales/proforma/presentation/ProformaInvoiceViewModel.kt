package com.vilync.ophthalmicerp.feature.sales.proforma.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import com.vilync.ophthalmicerp.data.repository.ProformaInvoiceRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

data class ProformaInvoiceUiState(
    val proformaId: Long = 0,
    val proformaNumber: String = "",
    val proformaDate: String = "",
    val validUntilDate: String = "",
    val financialYearStart: Int = 0,
    val selectedCustomer: PartyMaster? = null,
    val customers: List<PartyMaster> = emptyList(),
    val products: List<com.vilync.ophthalmicerp.data.entity.ProductEntity> = emptyList(),
    val items: List<ProformaInvoiceItemUi> = emptyList(),
    val subTotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val gstSupplyType: String = "INTRA_STATE",
    val remarks: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedProformaId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ProformaInvoiceItemUi(
    val productId: Long,
    val productName: String,
    val power: String = "",
    val quantity: Int,
    val rate: Double,
    val discountPercent: Double = 0.0,
    val gstPercent: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

class ProformaInvoiceViewModel(
    private val repository: ProformaInvoiceRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val companyProfileRepository: CompanyProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProformaInvoiceUiState(proformaDate = today(), validUntilDate = in30Days()))
    val uiState: StateFlow<ProformaInvoiceUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
        loadProducts()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            partyRepository.getAllActiveParties().collect { parties ->
                val customers = parties.filter { it.partyType == PartyType.CUSTOMER || it.partyType == PartyType.BOTH }
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllActiveProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun loadProforma(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val pair = repository.getProformaWithItems(id) ?: throw Exception("Proforma not found")
                val (proforma, items) = pair
                
                val customer = partyRepository.getPartyById(proforma.customerId)
                
                _uiState.update { 
                    it.copy(
                        proformaId = proforma.id,
                        proformaNumber = proforma.proformaNumber,
                        proformaDate = proforma.proformaDate,
                        validUntilDate = proforma.validUntilDate,
                        financialYearStart = proforma.financialYearStart,
                        selectedCustomer = customer,
                        items = items.map { entity -> 
                            ProformaInvoiceItemUi(
                                productId = entity.productId,
                                productName = entity.productName,
                                power = entity.power,
                                quantity = entity.quantity,
                                rate = entity.rate,
                                discountPercent = entity.discountPercent,
                                gstPercent = entity.gstPercent,
                                taxableAmount = entity.taxableAmount,
                                gstAmount = entity.gstAmount,
                                totalAmount = entity.totalAmount
                            )
                        },
                        subTotal = proforma.subTotal,
                        taxableAmount = proforma.taxableAmount,
                        gstAmount = proforma.gstAmount,
                        cgstAmount = proforma.cgstAmount,
                        sgstAmount = proforma.sgstAmount,
                        igstAmount = proforma.igstAmount,
                        totalAmount = proforma.totalAmount,
                        remarks = proforma.remarks,
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
        refreshTaxBreakup()
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(proformaDate = date, financialYearStart = financialYearStart(date)) }
        refreshTaxBreakup()
    }

    fun updateValidUntil(date: String) {
        _uiState.update { it.copy(validUntilDate = date) }
    }

    fun updateRemarks(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun addProduct(product: com.vilync.ophthalmicerp.data.entity.ProductEntity, qty: Int, rate: Double) {
        val taxable = qty * rate
        val gst = taxable * (product.gstPercent / 100.0)
        val item = ProformaInvoiceItemUi(
            productId = product.id,
            productName = product.productName,
            power = "",
            quantity = qty,
            rate = rate,
            gstPercent = product.gstPercent,
            taxableAmount = money(taxable),
            gstAmount = money(gst),
            totalAmount = money(taxable + gst)
        )
        val updated = _uiState.value.items + item
        calculateSummary(updated)
    }

    fun removeItem(index: Int) {
        val updated = _uiState.value.items.toMutableList().apply { removeAt(index) }
        calculateSummary(updated)
    }

    private fun calculateSummary(items: List<ProformaInvoiceItemUi>) {
        val taxable = items.sumOf { it.taxableAmount }
        val gst = items.sumOf { it.gstAmount }
        val total = items.sumOf { it.totalAmount }
        
        _uiState.update { it.copy(
            items = items,
            taxableAmount = money(taxable),
            gstAmount = money(gst),
            totalAmount = money(total)
        ) }
        refreshTaxBreakup()
    }

    private fun refreshTaxBreakup() {
        viewModelScope.launch {
            val state = _uiState.value
            val company = companyProfileRepository.getCompanyProfile()
            val customer = state.selectedCustomer
            
            val isInterState = company != null && customer != null && 
                company.state.trim().lowercase() != customer.state.trim().lowercase()
                
            val gst = state.gstAmount
            if (isInterState) {
                _uiState.update { it.copy(igstAmount = gst, cgstAmount = 0.0, sgstAmount = 0.0, gstSupplyType = "INTER_STATE") }
            } else {
                _uiState.update { it.copy(cgstAmount = money(gst / 2.0), sgstAmount = money(gst / 2.0), igstAmount = 0.0, gstSupplyType = "INTRA_STATE") }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.selectedCustomer == null || state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select customer and add items.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = System.currentTimeMillis()
                val proforma = ProformaInvoiceEntity(
                    id = state.proformaId,
                    customerId = state.selectedCustomer.id,
                    customerName = state.selectedCustomer.partyName,
                    proformaNumber = state.proformaNumber,
                    normalizedProformaNumber = state.proformaNumber.uppercase(),
                    proformaDate = state.proformaDate,
                    validUntilDate = state.validUntilDate,
                    financialYearStart = state.financialYearStart,
                    taxableAmount = state.taxableAmount,
                    gstAmount = state.gstAmount,
                    cgstAmount = state.cgstAmount,
                    sgstAmount = state.sgstAmount,
                    igstAmount = state.igstAmount,
                    totalAmount = state.totalAmount,
                    remarks = state.remarks,
                    gstSupplyType = state.gstSupplyType,
                    status = "OPEN",
                    createdAt = if (state.isEditMode) now else now, // Should preserve original in real app
                    updatedAt = now
                )
                
                val items = state.items.map {
                    ProformaInvoiceItemEntity(
                        proformaInvoiceId = state.proformaId,
                        productId = it.productId,
                        productName = it.productName,
                        power = it.power,
                        quantity = it.quantity,
                        rate = it.rate,
                        discountPercent = it.discountPercent,
                        gstPercent = it.gstPercent,
                        taxableAmount = it.taxableAmount,
                        gstAmount = it.gstAmount,
                        totalAmount = it.totalAmount
                    )
                }

                val proformaId = if (state.isEditMode) {
                    repository.updateCompleteProforma(proforma, items)
                    state.proformaId
                } else {
                    repository.saveCompleteProforma(proforma, items)
                }
                
                _uiState.update { it.copy(isSaving = false, isSaved = true, savedProformaId = proformaId, successMessage = "Proforma Invoice saved successfully.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save Proforma.") }
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

    private fun money(v: Double) = round(v * 100.0) / 100.0

    private fun today() = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    private fun in30Days(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 30)
        return SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time)
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
