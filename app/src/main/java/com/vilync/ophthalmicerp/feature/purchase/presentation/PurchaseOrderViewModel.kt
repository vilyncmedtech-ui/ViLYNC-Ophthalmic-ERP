package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PurchaseOrderViewModel(
    private val partyRepository: PartyRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductMasterRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val numberingRepository: DocumentNumberingRepository,
    private val initialProductId: Long = 0L,
    private val initialPower: String = "",
    private val initialQty: Int = 0
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrderUiState())
    val uiState: StateFlow<PurchaseOrderUiState> = _uiState.asStateFlow()

    private val _vendors = MutableStateFlow<List<PartyMaster>>(emptyList())
    val vendors: StateFlow<List<PartyMaster>> = _vendors.asStateFlow()

    init {
        loadVendors()
        _uiState.update { it.copy(
            poDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))
        )}
        if (initialProductId > 0L) {
            prefillFromShortage(initialProductId, initialPower, initialQty)
        }
    }

    private fun loadVendors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuppliers = true) }
            try {
                partyRepository.getAllActiveParties().collect { parties ->
                    val vendorParties = parties.filter { isPurchaseVendor(it.partyType) }
                    _vendors.value = vendorParties.sortedBy { it.partyName.lowercase() }
                    _uiState.update { it.copy(isLoadingSuppliers = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoadingSuppliers = false,
                    errorMessage = e.message ?: "Unable to load vendors."
                )}
            }
        }
    }

    private fun isPurchaseVendor(partyType: PartyType): Boolean {
        val typeName = partyType.displayName.trim().lowercase()
        return typeName == "vendor" || (typeName.contains("customer") && typeName.contains("vendor"))
    }

    fun selectVendor(party: PartyMaster) {
        _uiState.update { it.copy(
            supplierId = party.id,
            supplierName = party.partyName,
            supplierAddress = buildSupplierAddress(party),
            supplierCity = party.city,
            supplierDistrict = party.district,
            supplierState = party.state,
            supplierPinCode = party.pinCode,
            supplierGstin = party.gstin,
            supplierCreditDays = party.creditDays.toString(),
            errorMessage = null
        )}
        markDirty()
    }

    fun clearVendor() {
        _uiState.update { it.copy(
            supplierId = null,
            supplierName = "",
            supplierAddress = "",
            supplierCity = "",
            supplierDistrict = "",
            supplierState = "",
            supplierPinCode = "",
            supplierGstin = "",
            supplierCreditDays = "",
            errorMessage = null
        )}
        markDirty()
    }

    fun updatePoDate(value: String) {
        _uiState.update { it.copy(poDate = value, errorMessage = null) }
        markDirty()
    }

    fun updateReference(value: String) {
        _uiState.update { it.copy(reference = value, errorMessage = null) }
        markDirty()
    }

    fun confirmHeader(): Boolean {
        val state = _uiState.value
        if (state.supplierId == null || state.supplierName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select Vendor.") }
            return false
        }
        if (state.poDate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select Order Date.") }
            return false
        }
        _uiState.update { it.copy(isHeaderConfirmed = true, errorMessage = null) }
        return true
    }

    fun editHeader() {
        _uiState.update { it.copy(isHeaderConfirmed = false) }
    }

    fun addItem(item: PurchaseItem) {
        _uiState.update { it.copy(items = it.items + item, errorMessage = null) }
        recalculateTotals()
        markDirty()
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            val list = state.items.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                state.copy(items = list)
            } else state
        }
        recalculateTotals()
        markDirty()
    }

    fun updateItem(index: Int, item: PurchaseItem) {
        _uiState.update { state ->
            val list = state.items.toMutableList()
            if (index in list.indices) {
                list[index] = item
                state.copy(items = list)
            } else state
        }
        recalculateTotals()
        markDirty()
    }

    private fun recalculateTotals() {
        _uiState.update { state ->
            var gross = 0.0
            var disc = 0.0
            var taxable = 0.0
            var tax = 0.0
            state.items.forEach {
                val g = it.quantity * it.purchaseRate
                val d = g * (it.discountPercent / 100.0)
                val t = g - d
                val tx = t * (it.gstPercent / 100.0)
                gross += g
                disc += d
                taxable += t
                tax += tx
            }
            val total = taxable + tax + state.adjustmentAmount
            val rounded = kotlin.math.round(total)
            state.copy(
                grossAmount = gross,
                discountAmount = disc,
                taxableAmount = taxable,
                taxAmount = tax,
                roundOffAmount = rounded - total,
                netAmount = rounded
            )
        }
    }

    fun prefillFromShortage(productId: Long, power: String, qty: Int) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch
            val vendors = partyRepository.getAllActiveParties().first()
            val matchingVendor = vendors.find { 
                it.partyName.trim().equals(product.brand.trim(), ignoreCase = true) && isPurchaseVendor(it.partyType)
            }
            
            matchingVendor?.let { selectVendor(it) }
            
            val item = PurchaseItem(
                productId = product.id,
                productName = product.productName,
                model = product.model,
                category = product.category.name,
                hsnCode = product.hsnCode,
                power = power,
                quantity = qty.coerceAtLeast(1),
                purchaseRate = product.purchasePrice,
                gstPercent = product.gstPercent
            )
            addItem(item)
        }
    }

    fun saveOrder() {
        val state = _uiState.value
        if (state.isSaving) return
        
        // Validations
        if (state.supplierId == null) {
            _uiState.update { it.copy(errorMessage = "Please select Vendor.") }
            return
        }
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least one Product.") }
            return
        }

        val date = try {
            LocalDate.parse(state.poDate, DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT))
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Invalid Order Date.") }
            return
        }

        val fy = FinancialYearManager.financialYearForDate(date)
        if (fy.startYear != FinancialYearManager.activeFinancialYear.value.startYear) {
            _uiState.update { it.copy(errorMessage = "Order Date must be within the active Financial Year (${FinancialYearManager.activeFinancialYear.value.displayName}).") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val finalPoNumber = if (state.isEditMode) state.poNumber else {
                    numberingRepository.getNextDocumentNumber(DocumentType.PURCHASE_ORDER, fy.startYear)
                }

                val purchaseEntity = PurchaseEntity(
                    id = state.editingPoId ?: 0L,
                    supplierId = state.supplierId,
                    supplierName = state.supplierName,
                    invoiceNumber = finalPoNumber,
                    normalizedInvoiceNumber = finalPoNumber,
                    invoiceDate = state.poDate,
                    receivedDate = "", // No received date for PO
                    financialYearStart = fy.startYear,
                    purchaseType = "",
                    paymentType = "",
                    creditDays = state.supplierCreditDays.toIntOrNull() ?: 0,
                    reference = state.reference,
                    subtotal = state.grossAmount,
                    discountAmount = state.discountAmount,
                    taxableAmount = state.taxableAmount,
                    cgstAmount = 0.0,
                    sgstAmount = 0.0,
                    igstAmount = state.taxAmount,
                    grandTotal = state.netAmount,
                    status = "ORDER"
                )

                val itemsWithLenses = state.items.map { item ->
                    val gross = item.quantity * item.purchaseRate
                    val disc = gross * (item.discountPercent / 100.0)
                    val taxable = gross - disc
                    val gst = taxable * (item.gstPercent / 100.0)
                    
                    PurchaseItemEntity(
                        purchaseId = 0L,
                        productId = item.productId,
                        power = item.power,
                        quantity = item.quantity,
                        purchaseRate = item.purchaseRate,
                        discountPercent = item.discountPercent,
                        gstPercent = item.gstPercent,
                        grossAmount = gross,
                        discountAmount = disc,
                        taxableAmount = taxable,
                        gstAmount = gst,
                        lineTotal = taxable + gst
                    ) to emptyList<com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity>()
                }

                val savedId = if (state.isEditMode) {
                    purchaseRepository.updateCompletePurchase(purchaseEntity, itemsWithLenses)
                    state.editingPoId!!
                } else {
                    purchaseRepository.saveCompletePurchase(purchaseEntity, itemsWithLenses)
                }

                auditTrailRepository.recordEvent(
                    module = "PURCHASE",
                    action = if (state.isEditMode) "UPDATE" else "CREATE",
                    recordId = savedId,
                    description = "Purchase Order ${if (state.isEditMode) "updated" else "created"} â€¢ $finalPoNumber â€¢ ${state.supplierName}"
                )

                _uiState.update { it.copy(
                    isSaving = false,
                    isSaved = true,
                    isSavedSuccessfully = true,
                    poNumber = finalPoNumber,
                    isDirty = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save Purchase Order.") }
            }
        }
    }

    private fun markDirty() {
        _uiState.update { it.copy(isDirty = true, isSavedSuccessfully = false) }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(isSavedSuccessfully = false) } }

    private fun buildSupplierAddress(party: PartyMaster): String {
        return listOf(party.addressLine1, party.addressLine2).map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
    }
}
