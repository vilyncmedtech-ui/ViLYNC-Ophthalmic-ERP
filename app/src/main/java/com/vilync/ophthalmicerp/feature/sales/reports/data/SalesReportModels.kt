package com.vilync.ophthalmicerp.feature.sales.reports.data

import com.vilync.ophthalmicerp.feature.sales.reports.domain.SalesReportType

enum class SalesReportType {
    DETAIL,
    PRODUCT_WISE,
    CUSTOMER_WISE
}

enum class CostResolutionSource {
    SNAPSHOT,
    TRACE,
    MASTER_FALLBACK,
    UNKNOWN
}

data class ProductWiseSummary(
    val productName: String,
    val qty: Int,
    val amount: Double
)

data class SalesTransactionDetail(
    val date: String,
    val invoiceNo: String,
    val customer: String,
    val customerGstin: String,
    val product: String,
    val power: String,
    val serial: String,
    val qty: Int,
    val gst_percent: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val amount: Double,
    val status: String,
    val customerId: Long = 0,
    val productId: Long = 0,
    val purchasePrice: Double = 0.0,
    val costResolutionSource: String = "UNKNOWN"
)

data class SalesReportUiState(
    val isLoading: Boolean = false,
    val salesList: List<SalesTransactionDetail> = emptyList(),
    val productSummary: List<ProductWiseSummary> = emptyList(),
    val totalCount: Int = 0,
    val totalAmount: Double = 0.0,
    val errorMessage: String? = null,
    val reportTitle: String = "Date-wise Sales Report",
    val selectedReportType: SalesReportType = SalesReportType.DETAIL,
    val startDate: String = "",
    val endDate: String = ""
)
