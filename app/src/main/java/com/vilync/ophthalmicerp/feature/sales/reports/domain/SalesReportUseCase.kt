package com.vilync.ophthalmicerp.feature.sales.reports.domain

import com.vilync.ophthalmicerp.feature.sales.reports.data.SalesReportRepository
import com.vilync.ophthalmicerp.feature.sales.reports.data.ProductWiseSummary
import com.vilync.ophthalmicerp.feature.sales.reports.data.SalesTransactionDetail

class SalesReportUseCase(
    private val repository: SalesReportRepository
) {
    suspend fun getProductSummary(
        startDate: String, 
        endDate: String,
        customerId: Long? = null,
        productId: Long? = null,
        transactionType: String? = "All"
    ): List<ProductWiseSummary> {
        return repository.getProductWiseSummary(startDate, endDate, customerId, productId, transactionType)
    }

    suspend fun getSalesDetails(startDate: String, endDate: String): List<SalesTransactionDetail> {
        return repository.getDateWiseSales(startDate, endDate)
    }
}
