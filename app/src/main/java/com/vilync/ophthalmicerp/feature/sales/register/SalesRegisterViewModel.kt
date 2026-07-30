package com.vilync.ophthalmicerp.feature.sales.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SalesRegisterViewModel(
    private val database: AppDatabase,
    val registerType: SalesRegisterType
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesRegisterUiState())
    val uiState: StateFlow<SalesRegisterUiState> = _uiState.asStateFlow()

    private var allRows: List<SalesRegisterRow> = emptyList()

    private val salesRepository by lazy {
        SalesRepository(
            salesDao = database.salesDao(),
            database = database
        )
    }

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isActionRunning = MutableStateFlow(false)
    val isActionRunning: StateFlow<Boolean> = _isActionRunning.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { withContext(Dispatchers.IO) { loadRows() } }
                .onSuccess {
                    allRows = it
                    applyFilters()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Unable to load register."
                    )
                }
        }
    }

    fun updateQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        applyFilters()
    }

    fun updateStatusFilter(value: String) {
        _uiState.value = _uiState.value.copy(statusFilter = value)
        applyFilters()
    }


    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun cancelInvoice(
        row: SalesRegisterRow,
        reason: String
    ) {

        if (registerType != SalesRegisterType.INVOICE) {
            _actionMessage.value =
                "Cancellation is currently available only for Sales Invoices."
            return
        }

        if (
            row.status.trim().equals(
                other = "CANCELLED",
                ignoreCase = true
            )
        ) {
            _actionMessage.value =
                "This Sales Invoice is already cancelled."
            return
        }

        val normalizedReason =
            reason.trim()

        if (normalizedReason.isBlank()) {
            _actionMessage.value =
                "Cancellation reason is required."
            return
        }

        viewModelScope.launch {

            _isActionRunning.value = true
            _actionMessage.value = null

            runCatching {
                withContext(Dispatchers.IO) {
                    salesRepository.cancelSale(
                        saleId = row.id,
                        cancellationReason = normalizedReason
                    )
                }
            }
                .onSuccess {
                    _actionMessage.value =
                        "Sales Invoice ${row.documentNumber} cancelled successfully."
                    refresh()
                }
                .onFailure {
                    _actionMessage.value =
                        it.message ?: "Unable to cancel Sales Invoice."
                }

            _isActionRunning.value = false
        }
    }

    private fun applyFilters() {
        val q = _uiState.value.query.trim()
        val status = _uiState.value.statusFilter
        val filtered = allRows.filter { row ->
            val searchOk = q.isBlank() ||
                    row.documentNumber.contains(q, true) ||
                    row.customerName.contains(q, true) ||
                    row.documentDate.contains(q, true) ||
                    row.status.contains(q, true) ||
                    row.secondaryInfo.contains(q, true)
            val statusOk = status == "ALL" || row.status.equals(status, true)
            searchOk && statusOk
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            rows = filtered,
            errorMessage = null
        )
    }

    private fun loadRows(): List<SalesRegisterRow> {
        val sql = when (registerType) {
            SalesRegisterType.INVOICE ->
                """SELECT id, invoiceNumber AS docNo, invoiceDate AS docDate,
                   customerName, status, totalAmount AS amount, '' AS secondaryInfo
                   FROM sales ORDER BY id DESC"""
            SalesRegisterType.CHALLAN ->
                """SELECT c.id, c.challanNumber AS docNo, c.challanDate AS docDate,
                   c.customerName, c.status, NULL AS amount,
                   (SELECT COUNT(*) FROM challan_items ci WHERE ci.challanId=c.id) || ' unit(s)' AS secondaryInfo
                   FROM challans c ORDER BY c.id DESC"""
            SalesRegisterType.CREDIT_NOTE ->
                """SELECT id, creditNoteNumber AS docNo, creditNoteDate AS docDate,
                   customerName, status, totalAmount AS amount,
                   originalInvoiceNumber AS secondaryInfo
                   FROM sales_credit_notes ORDER BY id DESC"""
            SalesRegisterType.PROFORMA ->
                """SELECT id, proformaNumber AS docNo, proformaDate AS docDate,
                   customerName, status, totalAmount AS amount,
                   validUntilDate AS secondaryInfo
                   FROM proforma_invoices ORDER BY id DESC"""
            SalesRegisterType.SAMPLE_ISSUE ->
                """SELECT s.id, s.sampleIssueNumber AS docNo, s.sampleIssueDate AS docDate,
                   s.customerName, s.status, NULL AS amount,
                   (SELECT COUNT(*) FROM sample_issue_items si WHERE si.sampleIssueId=s.id) || ' unit(s)' AS secondaryInfo
                   FROM sample_issues s ORDER BY s.id DESC"""
        }

        val cursor = database.openHelper.readableDatabase.query(sql)
        return cursor.use {
            val rows = mutableListOf<SalesRegisterRow>()
            val idIx = it.getColumnIndexOrThrow("id")
            val noIx = it.getColumnIndexOrThrow("docNo")
            val dateIx = it.getColumnIndexOrThrow("docDate")
            val customerIx = it.getColumnIndexOrThrow("customerName")
            val statusIx = it.getColumnIndexOrThrow("status")
            val amountIx = it.getColumnIndexOrThrow("amount")
            val secondaryIx = it.getColumnIndexOrThrow("secondaryInfo")
            while (it.moveToNext()) {
                rows += SalesRegisterRow(
                    id = it.getLong(idIx),
                    documentNumber = it.getString(noIx).orEmpty(),
                    documentDate = it.getString(dateIx).orEmpty(),
                    customerName = it.getString(customerIx).orEmpty(),
                    status = it.getString(statusIx).orEmpty(),
                    amount = if (it.isNull(amountIx)) null else it.getDouble(amountIx),
                    secondaryInfo = it.getString(secondaryIx).orEmpty()
                )
            }
            rows
        }
    }
}

class SalesRegisterViewModelFactory(
    private val database: AppDatabase,
    private val registerType: SalesRegisterType
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesRegisterViewModel::class.java)) {
            return SalesRegisterViewModel(database, registerType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
