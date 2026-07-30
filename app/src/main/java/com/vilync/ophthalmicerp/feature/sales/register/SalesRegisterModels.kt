package com.vilync.ophthalmicerp.feature.sales.register

enum class SalesRegisterType(
    val screenTitle: String,
    val tableName: String
) {
    INVOICE("Invoice Register", "sales"),
    CHALLAN("Challan Register", "challans"),
    CREDIT_NOTE("Credit Note Register", "sales_credit_notes"),
    PROFORMA("Proforma Invoice Register", "proforma_invoices"),
    SAMPLE_ISSUE("Sample Issue Register", "sample_issues")
}

data class SalesRegisterRow(
    val id: Long,
    val documentNumber: String,
    val documentDate: String,
    val customerName: String,
    val status: String,
    val amount: Double? = null,
    val secondaryInfo: String = ""
)

data class SalesRegisterUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val statusFilter: String = "ALL",
    val rows: List<SalesRegisterRow> = emptyList(),
    val errorMessage: String? = null
)
