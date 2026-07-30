package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class NewCreditNoteViewModel(
    private val salesRepository: SalesRepository,
    private val creditNoteRepository: SalesCreditNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NewCreditNoteUiState(creditNoteDate = today())
    )
    val uiState: StateFlow<NewCreditNoteUiState> = _uiState.asStateFlow()

    private var allInvoices: List<SaleEntity> = emptyList()

    init {
        viewModelScope.launch {
            allInvoices = salesRepository.getAllSales().first()
                .filter { !it.status.equals("CANCELLED", true) }
            applyInvoiceFilter()
        }
    }

    fun updateInvoiceQuery(value: String) {
        _uiState.value = _uiState.value.copy(invoiceQuery = value)
        applyInvoiceFilter()
    }

    private fun applyInvoiceFilter() {
        val q = _uiState.value.invoiceQuery.trim()
        val rows = if (q.isBlank()) allInvoices else allInvoices.filter {
            it.invoiceNumber.contains(q, true) || it.customerName.contains(q, true) || it.invoiceDate.contains(q, true)
        }
        _uiState.value = _uiState.value.copy(invoices = rows)
    }

    fun selectInvoice(sale: SaleEntity) {
        _uiState.value = _uiState.value.copy(
            selectedInvoice = sale,
            invoiceQuery = "${sale.invoiceNumber} • ${sale.customerName}",
            invoices = emptyList(),
            invoiceLines = emptyList(),
            isLoadingInvoice = true,
            errorMessage = null
        )
        viewModelScope.launch {
            runCatching {
                val items = salesRepository.getSaleItems(sale.id).first()
                items.flatMap { item ->
                    val lenses = salesRepository.getSaleLenses(item.id).first()
                    val quantityDivisor = if (item.quantity > 0) item.quantity.toDouble() else 1.0
                    lenses.map { lens ->
                        CreditNoteInvoiceLineUi(
                            saleItemId = item.id,
                            saleLensId = lens.id,
                            inventoryUnitId = lens.inventoryUnitId,
                            productId = item.productId,
                            productName = item.productName,
                            serialNumber = lens.serialNumber,
                            power = lens.power.ifBlank { item.power },
                            batchNumber = lens.batchNumber.ifBlank { item.batchNumber },
                            expiryDate = lens.expiryDate,
                            rate = item.rate,
                            discountPercent = item.discountPercent,
                            gstPercent = item.gstPercent,
                            taxableAmount = money(item.taxableAmount / quantityDivisor),
                            gstAmount = money(item.gstAmount / quantityDivisor),
                            totalAmount = money(item.totalAmount / quantityDivisor)
                        )
                    }
                }
            }.onSuccess { lines ->
                _uiState.value = _uiState.value.copy(invoiceLines = lines, isLoadingInvoice = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoadingInvoice = false,
                    errorMessage = it.message ?: "Unable to load invoice details."
                )
            }
        }
    }

    fun toggleLine(saleLensId: Long) {
        _uiState.value = _uiState.value.copy(
            invoiceLines = _uiState.value.invoiceLines.map {
                if (it.saleLensId == saleLensId) it.copy(selected = !it.selected) else it
            }
        )
    }

    fun updateCreditNoteNumber(value: String) { _uiState.value = _uiState.value.copy(creditNoteNumber = value) }
    fun updateCreditNoteDate(value: String) { _uiState.value = _uiState.value.copy(creditNoteDate = value) }
    fun updateReason(value: String) { _uiState.value = _uiState.value.copy(reason = value) }
    fun updateRemarks(value: String) { _uiState.value = _uiState.value.copy(remarks = value) }
    fun updateAdjustmentAmount(value: String) { _uiState.value = _uiState.value.copy(adjustmentAmount = value.filter { it.isDigit() || it == '.' }) }

    fun setCreditNoteType(type: String) {
        val normalized = if (type == "FINANCIAL_ADJUSTMENT") "FINANCIAL_ADJUSTMENT" else "SALES_RETURN"
        _uiState.value = _uiState.value.copy(
            creditNoteType = normalized,
            invoiceLines = if (normalized == "FINANCIAL_ADJUSTMENT") {
                _uiState.value.invoiceLines.map { it.copy(selected = false) }
            } else _uiState.value.invoiceLines,
            errorMessage = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun save() {
        val state = _uiState.value
        val sale = state.selectedInvoice
        if (sale == null) {
            _uiState.value = state.copy(errorMessage = "Select the original Sales Invoice.")
            return
        }
        if (state.creditNoteNumber.trim().isBlank()) {
            _uiState.value = state.copy(errorMessage = "Credit Note Number is required.")
            return
        }
        if (state.creditNoteDate.trim().isBlank()) {
            _uiState.value = state.copy(errorMessage = "Credit Note Date is required.")
            return
        }

        val selected = state.invoiceLines.filter { it.selected }
        val adjustment = state.adjustmentAmount.toDoubleOrNull() ?: 0.0
        if (state.creditNoteType == "SALES_RETURN" && selected.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Select at least one sold serial to return.")
            return
        }
        if (state.creditNoteType == "FINANCIAL_ADJUSTMENT" && adjustment <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Enter a Financial Adjustment amount greater than zero.")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val itemsWithLenses = if (state.creditNoteType == "SALES_RETURN") {
                    selected.groupBy { it.saleItemId }.map { (saleItemId, lines) ->
                        val first = lines.first()
                        val taxable = money(lines.sumOf { it.taxableAmount })
                        val gst = money(lines.sumOf { it.gstAmount })
                        val total = money(lines.sumOf { it.totalAmount })
                        val gross = money(first.rate * lines.size)
                        val discount = money((gross - taxable).coerceAtLeast(0.0))
                        SalesCreditNoteItemEntity(
                            creditNoteId = 0L,
                            originalSaleItemId = saleItemId,
                            productId = first.productId,
                            productName = first.productName,
                            power = first.power,
                            quantity = lines.size,
                            rate = first.rate,
                            discountPercent = first.discountPercent,
                            discountAmount = discount,
                            taxableAmount = taxable,
                            gstPercent = first.gstPercent,
                            gstAmount = gst,
                            totalAmount = total,
                            batchNumber = first.batchNumber
                        ) to lines.map { line ->
                            SalesCreditNoteLensEntity(
                                creditNoteItemId = 0L,
                                originalSaleLensId = line.saleLensId,
                                inventoryUnitId = line.inventoryUnitId,
                                serialNumber = line.serialNumber,
                                power = line.power,
                                batchNumber = line.batchNumber,
                                expiryDate = line.expiryDate,
                                returnToStock = true
                            )
                        }
                    }
                } else {
                    val baseItem = salesRepository.getSaleItems(sale.id).first().firstOrNull()
                        ?: error("Original Invoice has no item available for adjustment reference.")
                    listOf(
                        SalesCreditNoteItemEntity(
                            creditNoteId = 0L,
                            originalSaleItemId = baseItem.id,
                            productId = baseItem.productId,
                            productName = "Financial Adjustment - ${baseItem.productName}",
                            power = baseItem.power,
                            quantity = 1,
                            rate = adjustment,
                            discountPercent = 0.0,
                            discountAmount = 0.0,
                            taxableAmount = adjustment,
                            gstPercent = 0.0,
                            gstAmount = 0.0,
                            totalAmount = adjustment
                        ) to emptyList()
                    )
                }

                val taxable = money(itemsWithLenses.sumOf { it.first.taxableAmount })
                val gst = money(itemsWithLenses.sumOf { it.first.gstAmount })
                val total = money(itemsWithLenses.sumOf { it.first.totalAmount })
                val intra = sale.gstSupplyType.equals("INTRA_STATE", true)

                creditNoteRepository.saveCompleteCreditNote(
                    SalesCreditNoteEntity(
                        customerId = sale.customerId,
                        customerName = sale.customerName,
                        billToLegalName = sale.billToLegalName,
                        billToGstin = sale.billToGstin,
                        billToAddress = sale.billToAddress,
                        billToState = sale.billToState,
                        creditNoteNumber = state.creditNoteNumber.trim(),
                        normalizedCreditNoteNumber = state.creditNoteNumber.trim().uppercase(),
                        creditNoteDate = state.creditNoteDate.trim(),
                        financialYearStart = financialYearStart(state.creditNoteDate),
                        originalSaleId = sale.id,
                        originalInvoiceNumber = sale.invoiceNumber,
                        originalInvoiceDate = sale.invoiceDate,
                        placeOfSupplyState = sale.placeOfSupplyState,
                        gstSupplyType = sale.gstSupplyType,
                        creditNoteType = state.creditNoteType,
                        subTotal = taxable,
                        discountAmount = money(itemsWithLenses.sumOf { it.first.discountAmount }),
                        taxableAmount = taxable,
                        gstAmount = gst,
                        cgstAmount = if (intra) money(gst / 2.0) else 0.0,
                        sgstAmount = if (intra) money(gst / 2.0) else 0.0,
                        igstAmount = if (intra) 0.0 else gst,
                        adjustment = if (state.creditNoteType == "FINANCIAL_ADJUSTMENT") adjustment else 0.0,
                        roundOff = 0.0,
                        totalAmount = total,
                        reason = state.reason,
                        remarks = state.remarks,
                        createdAt = now,
                        updatedAt = now
                    ),
                    itemsWithLenses
                )
            }.onSuccess { id ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedCreditNoteId = id,
                    successMessage = "Credit Note saved successfully."
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = it.message ?: "Unable to save Credit Note."
                )
            }
        }
    }

    private fun financialYearStart(date: String): Int {
        val formats = listOf("dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd")
        formats.forEach { pattern ->
            runCatching {
                val parsed = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }.parse(date)
                if (parsed != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = parsed }
                    val year = cal.get(java.util.Calendar.YEAR)
                    val month = cal.get(java.util.Calendar.MONTH) + 1
                    return if (month >= 4) year else year - 1
                }
            }
        }
        val cal = java.util.Calendar.getInstance()
        return if (cal.get(java.util.Calendar.MONTH) + 1 >= 4) cal.get(java.util.Calendar.YEAR) else cal.get(java.util.Calendar.YEAR) - 1
    }

    private fun money(value: Double): Double = round(value * 100.0) / 100.0

    companion object {
        private fun today(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }
}
