package com.vilync.ophthalmicerp.feature.inventory.report

import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow

/**
 * Common report model for Stock Register exports.
 *
 * This model is intentionally independent from:
 * - Excel
 * - PDF
 * - Android Print
 *
 * All export formats will consume the same report data,
 * ensuring that quantities remain consistent across formats.
 */
data class StockRegisterReport(

    val title: String = "Stock Register",

    val generatedAt: String,

    val searchQuery: String = "",

    val rows: List<StockRegisterReportRow>,

    val totalPurchased: Int,

    val totalReturned: Int,

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

    val availableQuantity: Int
)


/**
 * Creates a report snapshot from the currently visible
 * Stock Register rows.
 *
 * IMPORTANT:
 *
 * The UI/ViewModel remains the source of the filtered rows.
 * Therefore, if the user searches for a product or power,
 * the generated report can represent exactly those visible rows.
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

        totalAvailable =
            reportRows.sumOf {
                it.availableQuantity
            }
    )
}