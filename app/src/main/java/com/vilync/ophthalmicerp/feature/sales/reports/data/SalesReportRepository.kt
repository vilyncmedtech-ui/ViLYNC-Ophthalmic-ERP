package com.vilync.ophthalmicerp.feature.sales.reports.data

import android.util.Log
import com.vilync.ophthalmicerp.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SalesReportRepository(
    private val database: AppDatabase
) {
    suspend fun getProductWiseSummary(
        startDate: String, 
        endDate: String,
        customerId: Long? = null,
        productId: Long? = null,
        transactionType: String? = "All"
    ): List<ProductWiseSummary> = 
        withContext(Dispatchers.IO) {
            val results = database.salesDao().getOptimizedProductWiseSummary(startDate, endDate, customerId, productId, transactionType)
            results
        }

    suspend fun getDateWiseSales(startDate: String, endDate: String): List<SalesTransactionDetail> = 
        withContext(Dispatchers.IO) {
            Log.d("CustomerFilterTrace", "Repository getDateWiseSales: startDate=$startDate, endDate=$endDate")
            val results = database.salesDao().getDateWiseSales(startDate, endDate)
            Log.d("CustomerFilterTrace", "Repository getDateWiseSales returned ${results.size} rows")
            results
        }
}
