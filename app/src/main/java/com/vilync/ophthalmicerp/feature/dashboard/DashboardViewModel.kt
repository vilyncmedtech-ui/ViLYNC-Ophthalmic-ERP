package com.vilync.ophthalmicerp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.feature.inventory.alert.LowStockAlertUseCase
import com.vilync.ophthalmicerp.feature.payment.logic.OutstandingCalculationUseCase
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.inventory.ageing.InventoryAgeingUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DashboardUiState(
    val lowStockCount: Int = 0,
    val customerOverdueAmount: Double = 0.0,
    val customerOverdueCount: Int = 0,
    val supplierOverdueAmount: Double = 0.0,
    val supplierOverdueCount: Int = 0,
    val expiryAlertCount: Int = 0,
    val pendingChallansCount: Int = 0,
    val pendingSamplesCount: Int = 0,
    val isLoading: Boolean = false,
    
    // Search
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

class DashboardViewModel(
    private val database: AppDatabase,
    private val lowStockUseCase: LowStockAlertUseCase,
    private val outstandingCalculationUseCase: OutstandingCalculationUseCase,
    private val partyRepository: PartyRepository,
    private val searchUseCase: GlobalSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        setupReactiveDashboard()
        _refreshTrigger.tryEmit(Unit)
    }

    private fun setupReactiveDashboard() {
        // Combine multiple flows to auto-refresh Dashboard
        combine(
            database.salesDao().getAllSales(),
            database.purchaseDao().getAllPurchases(),
            database.challanDao().getAllChallans(),
            database.sampleIssueDao().getAllSampleIssues(),
            database.inventoryDao().getAllInventoryUnits(),
            partyRepository.getAllActiveParties(),
            _refreshTrigger
        ) { args -> 
            // args is Array<Any>
            Unit 
        }.onEach {
            calculateDashboardKpis()
        }.launchIn(viewModelScope)
    }

    private suspend fun calculateDashboardKpis() {
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            val thresholdOverdue = 60
            
            // 1. Low Stock
            val snapshots = lowStockUseCase.getSnapshots()
            val lowStockCount = snapshots.count { snapshot ->
                val status = lowStockUseCase.classify(snapshot)
                status == LowStockAlertUseCase.STATUS_OUT_OF_STOCK || status == LowStockAlertUseCase.STATUS_LOW_STOCK
            }

            // 2. Overdue Outstanding
            val parties = partyRepository.getAllActiveParties().first()
            var custAmount = 0.0
            var custCount = 0
            var suppAmount = 0.0
            var suppCount = 0

            parties.forEach { party ->
                if (party.partyType == PartyType.CUSTOMER || party.partyType == PartyType.BOTH) {
                    val overdue = outstandingCalculationUseCase.getCustomerOverdue(party.id, thresholdOverdue)
                    if (overdue > 0.0) {
                        custAmount += overdue
                        custCount++
                    }
                }
                if (party.partyType == PartyType.VENDOR || party.partyType == PartyType.BOTH) {
                    val overdue = outstandingCalculationUseCase.getSupplierOverdue(party.id, thresholdOverdue)
                    if (overdue > 0.0) {
                        suppAmount += overdue
                        suppCount++
                    }
                }
            }

            // 3. Expiry Alert
            val today = LocalDate.now()
            val inventoryUnits = database.inventoryDao().getAllInventoryUnits().first()
            val expiryCount = inventoryUnits.count { unit ->
                if (unit.status != "IN_STOCK" || unit.expiryDate.isBlank()) return@count false
                
                val expiryLocalDate = InventoryAgeingUseCase.parseExpiryDate(unit.expiryDate)
                    ?: return@count false
                
                val daysLeft = ChronoUnit.DAYS.between(today, expiryLocalDate)
                
                // Authoritative High Risk definition: 1-90 days
                daysLeft in 1..90
            }

            // 4. Pending Docs
            val pendingChallans = database.challanDao().observeOpenChallanCount().first()
            val pendingSamples = database.sampleIssueDao().observePendingSampleCount().first()

            _uiState.update { 
                it.copy(
                    lowStockCount = lowStockCount,
                    customerOverdueAmount = custAmount,
                    customerOverdueCount = custCount,
                    supplierOverdueAmount = suppAmount,
                    supplierOverdueCount = suppCount,
                    expiryAlertCount = expiryCount,
                    pendingChallansCount = pendingChallans,
                    pendingSamplesCount = pendingSamples,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        viewModelScope.launch {
            if (query.length >= 2) {
                _uiState.update { it.copy(isSearching = true) }
                val results = searchUseCase.execute(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } else {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun refresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    private fun money(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}
