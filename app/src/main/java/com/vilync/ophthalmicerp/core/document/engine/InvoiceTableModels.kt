package com.vilync.ophthalmicerp.core.document.engine

/**
 * Defines a single column in the ERP Invoice Repeater.
 */
data class InvoiceColumnDefinition(
    val id: String,
    val header: String,
    val widthMm: Double,
    val alignment: com.vilync.ophthalmicerp.core.document.domain.style.HorizontalAlignment = com.vilync.ophthalmicerp.core.document.domain.style.HorizontalAlignment.LEFT
)

/**
 * Configuration for the Invoice Table (Repeater).
 */
data class InvoiceTableConfig(
    val columns: List<InvoiceColumnDefinition>,
    val headerHeightMm: Double = 8.0,
    val minRowHeightMm: Double = 6.0,
    val showBorders: Boolean = true,
    val repeatHeaderOnNewPage: Boolean = true
)
