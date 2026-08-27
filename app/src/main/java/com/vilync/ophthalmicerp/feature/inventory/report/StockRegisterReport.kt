package com.vilync.ophthalmicerp.feature.inventory.report

import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow

/**
 * Common report model for Stock Register exports.
 */
data class StockRegisterReport(

    val title: String = "Stock Register",

    val generatedAt: String,

    val searchQuery: String = "",

    val rows: List<StockRegisterReportRow>,

    val totalPurchased: Int,

    val totalReturned: Int,

    val totalSold: Int,

    val totalAvailable: Int
)


/**
 * Immutable row used by Stock Register reports.
 */
data class StockRegisterReportRow(

    val productName: String,

    val model: String,

    val category: String,

    val power: String,

    val purchasedQuantity: Int,

    val returnedQuantity: Int,

    val soldQuantity: Int,

    val availableQuantity: Int
)


/**
 * Creates a report snapshot from the currently visible
 * Stock Register rows.
 */
fun createStockRegisterReport(
    stockRows: List<StockRegisterRow>,
    generatedAt: String,
    searchQuery: String = ""
): StockRegisterReport {

    val reportRows =
        stockRows.map { row ->

            StockRegisterReportRow(
                productName = row.productName,
                model = row.model,
                category = row.category,
                power = row.power,
                purchasedQuantity =
                    row.purchasedQuantity,
                returnedQuantity =
                    row.purchaseReturnQuantity,
                soldQuantity =
                    row.soldQuantity,
                availableQuantity =
                    row.availableQuantity
            )
        }


    return StockRegisterReport(

        title = "Stock Register",

        generatedAt = generatedAt,

        searchQuery = searchQuery.trim(),

        rows = reportRows,

        totalPurchased =
            reportRows.sumOf {
                it.purchasedQuantity
            },

        totalReturned =
            reportRows.sumOf {
                it.returnedQuantity
            },

        totalSold = 
            reportRows.sumOf {
                it.soldQuantity
            },

        totalAvailable =
            reportRows.sumOf {
                it.availableQuantity
            }
    )
}
