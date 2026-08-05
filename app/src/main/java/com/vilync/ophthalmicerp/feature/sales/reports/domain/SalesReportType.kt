package com.vilync.ophthalmicerp.feature.sales.reports.domain

enum class SalesReportType {
    SUMMARY,
    DETAIL,
    MONTHLY;

    val displayName: String
        get() = when (this) {
            SUMMARY -> "Product-wise Summary"
            DETAIL -> "Date-wise Sales Detail"
            MONTHLY -> "Monthly Sales Report"
        }
}
