package com.vilync.ophthalmicerp.feature.sales.register

data class SalesRegisterRow(
    val id: Long,
    val documentNumber: String,
    val documentDate: String,
    val customerName: String,
    val customerId: Long = 0,
    val status: String,
    val amount: Double? = null,
    val taxableAmount: Double? = null,
    val gstAmount: Double? = null,
    val cgstAmount: Double? = null,
    val sgstAmount: Double? = null,
    val igstAmount: Double? = null,
    val secondaryInfo: String = "",
    val creditNoteNumber: String? = null,
    val financialYearStart: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isCancellable: Boolean = true,
    val isEditable: Boolean = true,
    val isSelected: Boolean = false
)

data class SalesRegisterUiState(
    val isLoading: Boolean = false,
    val rows: List<SalesRegisterRow> = emptyList(),
    val filteredRows: List<SalesRegisterRow> = emptyList(),
    val query: String = "",
    val statusFilter: String = "ALL",
    val customerFilter: Long = 0,
    val financialYearFilter: Int = 0,
    val fromDate: String = "",
    val toDate: String = "",
    val sortColumn: String = "date",
    val sortDirection: String = "DESC",
    val errorMessage: String? = null,
    val totalAmount: Double = 0.0,
    val totalTaxable: Double = 0.0,
    val totalGst: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val count: Int = 0,
    val postedCount: Int = 0,
    val cancelledCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val isActionRunning: Boolean = false,
    val actionMessage: String? = null,
    val showCancelDialog: Boolean = false,
    val cancelReason: String = "",
    val cancelRowId: Long? = null,
    val showFilterDialog: Boolean = false,
    val showExportDialog: Boolean = false
)

data class SalesRegisterFilterOptions(
    val statuses: List<String> = listOf("ALL", "POSTED", "CANCELLED", "DRAFT", "CONVERTED"),
    val customers: List<CustomerOption> = emptyList(),
    val financialYears: List<Int> = emptyList()
)

data class CustomerOption(
    val id: Long,
    val name: String
)

data class SalesRegisterSummary(
    val totalCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalTaxable: Double = 0.0,
    val totalGst: Double = 0.0,
    val postedCount: Int = 0,
    val cancelledCount: Int = 0
)

enum class ExportType {
    PDF,
    EXCEL,
    CSV,
    PRINT
}