package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.AccountEntity
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.payment.logic.PaymentUseCase
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PaymentUiState(
    val isLoading: Boolean = false,
    val accounts: List<AccountEntity> = emptyList(),
    val parties: List<PartyMaster> = emptyList(),
    val filteredParties: List<PartyMaster> = emptyList(),
    
    // Invoices for reference
    val invoices: List<com.vilync.ophthalmicerp.data.entity.SaleEntity> = emptyList(),
    val bills: List<com.vilync.ophthalmicerp.data.entity.PurchaseEntity> = emptyList(),

    // Register State
    val allTransactions: List<FinancialTransactionEntity> = emptyList(),
    val filteredTransactions: List<FinancialTransactionEntity> = emptyList(),
    val registerSearchQuery: String = "",
    val statusFilter: String = "All",
    val dateFrom: String = "",
    val dateTo: String = "",
    val selectedFilterAccountId: Long = 0L,

    // Entry Fields
    val id: Long = 0L,
    val date: String = "",
    val amount: String = "",
    val selectedAccountId: Long = 0L,
    val selectedPartyId: Long = 0L,
    val reference: String = "",
    val remarks: String = "",
    val outstanding: Double = 0.0,
    
    // Mode
    val isEditMode: Boolean = false,

    // Detail Mode
    val selectedTransaction: FinancialTransactionEntity? = null,
    val selectedPartyName: String = "",
    val selectedAccountName: String = "",

    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class PaymentViewModel(
    private val paymentUseCase: PaymentUseCase,
    private val accountRepository: AccountRepository,
    private val partyRepository: PartyRepository,
    private val financialTransactionRepository: FinancialTransactionRepository,
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(date = today, dateFrom = today, dateTo = today) }
        loadMasterData()
    }

    fun loadRegister() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            financialTransactionRepository.getAllTransactions().collect { txs ->
                _uiState.update { it.copy(isLoading = false, allTransactions = txs) }
                applyFilters()
            }
        }
    }

    fun updateRegisterSearch(query: String) {
        _uiState.update { it.copy(registerSearchQuery = query) }
        applyFilters()
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        applyFilters()
    }

    fun updateDateFilter(from: String, to: String) {
        _uiState.update { it.copy(dateFrom = from, dateTo = to) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.allTransactions.filter { tx ->
            val matchesSearch = state.registerSearchQuery.isBlank() || 
                tx.referenceNumber.contains(state.registerSearchQuery, ignoreCase = true) ||
                tx.remarks.contains(state.registerSearchQuery, ignoreCase = true)
            
            val matchesStatus = state.statusFilter == "All" || tx.status == state.statusFilter
            
            // Simple date comparison for now
            val matchesDate = true // TODO: Implement proper date range check
            
            matchesSearch && matchesStatus && matchesDate
        }
        _uiState.update { it.copy(filteredTransactions = filtered) }
    }

    private fun loadMasterData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            accountRepository.initializeDefaultAccounts()
            accountRepository.getAllActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            partyRepository.getAllActiveParties().collect { parties ->
                _uiState.update { it.copy(parties = parties) }
            }
        }
    }

    fun searchParty(query: String, type: String) {
        val filtered = _uiState.value.parties.filter { party ->
            val matchesQuery = party.partyName.contains(query, ignoreCase = true)
            val matchesType = if (type == "RECEIPT") {
                party.partyType == PartyType.CUSTOMER
            } else {
                party.partyType == PartyType.VENDOR || party.partyType == PartyType.BOTH
            }
            matchesQuery && matchesType
        }
        _uiState.update { it.copy(filteredParties = filtered) }
    }

    fun selectParty(party: PartyMaster, type: String) {
        _uiState.update { it.copy(selectedPartyId = party.id, filteredParties = emptyList()) }
        viewModelScope.launch {
            val outstanding = if (type == "RECEIPT") {
                paymentUseCase.getCustomerOutstanding(party.id)
            } else {
                paymentUseCase.getSupplierOutstanding(party.id)
            }
            _uiState.update { it.copy(outstanding = outstanding) }
        }
        
        viewModelScope.launch {
            if (type == "RECEIPT") {
                salesRepository.getSalesByCustomer(party.id).collect { sales ->
                    _uiState.update { it.copy(invoices = sales) }
                }
            } else {
                purchaseRepository.getPurchasesBySupplier(party.partyName).collect { purchases ->
                    _uiState.update { it.copy(bills = purchases) }
                }
            }
        }
    }

    fun loadTransactionDetail(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tx = financialTransactionRepository.getTransactionById(id)
            if (tx != null) {
                val party = partyRepository.getPartyById(tx.partyId)
                val account = accountRepository.getAccountById(tx.accountId)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        selectedTransaction = tx,
                        selectedPartyName = party?.partyName ?: "Unknown",
                        selectedAccountName = account?.name ?: "Unknown"
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Transaction not found") }
            }
        }
    }

    fun duplicateTransaction(id: Long) {
        viewModelScope.launch {
            val tx = financialTransactionRepository.getTransactionById(id)
            if (tx != null) {
                _uiState.update { 
                    it.copy(
                        id = 0L,
                        amount = tx.amount.toString(),
                        selectedAccountId = tx.accountId,
                        selectedPartyId = tx.partyId,
                        reference = "COPY: " + tx.referenceNumber,
                        remarks = tx.remarks,
                        isEditMode = false
                    ) 
                }
            }
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            val tx = financialTransactionRepository.getTransactionById(id)
            if (tx != null && (tx.status == "DRAFT" || tx.status == "POSTED")) {
                val party = partyRepository.getPartyById(tx.partyId)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        id = tx.id,
                        date = tx.transactionDate,
                        amount = tx.amount.toString(),
                        selectedAccountId = tx.accountId,
                        selectedPartyId = tx.partyId,
                        reference = tx.referenceNumber,
                        remarks = tx.remarks
                    ) 
                }
                if (party != null) selectParty(party, if (tx.type == "CUSTOMER_RECEIPT") "RECEIPT" else "PAYMENT")
            }
        }
    }

    fun updateAmount(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null) {
            _uiState.update { it.copy(amount = value) }
        }
    }

    fun updateAccount(id: Long) {
        _uiState.update { it.copy(selectedAccountId = id) }
    }

    fun updateReference(value: String) {
        _uiState.update { it.copy(reference = value) }
    }

    fun updateRemarks(value: String) {
        _uiState.update { it.copy(remarks = value) }
    }

    fun updateDate(value: String) {
        _uiState.update { it.copy(date = value) }
    }

    fun save(type: String, isPost: Boolean, fyStart: Int) {
        val state = _uiState.value
        if (state.selectedPartyId == 0L || state.selectedAccountId == 0L || state.amount.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please fill all mandatory fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val amount = state.amount.toDoubleOrNull() ?: 0.0
                if (type == "RECEIPT") {
                    paymentUseCase.recordCustomerReceipt(
                        state.id, state.date, amount, state.selectedAccountId, state.selectedPartyId,
                        state.reference, state.remarks, fyStart, isPost
                    )
                } else {
                    paymentUseCase.recordSupplierPayment(
                        state.id, state.date, amount, state.selectedAccountId, state.selectedPartyId,
                        state.reference, state.remarks, fyStart, isPost
                    )
                }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun cancel(id: Long, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                paymentUseCase.cancelPayment(id, reason)
                loadTransactionDetail(id) // Refresh state
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
