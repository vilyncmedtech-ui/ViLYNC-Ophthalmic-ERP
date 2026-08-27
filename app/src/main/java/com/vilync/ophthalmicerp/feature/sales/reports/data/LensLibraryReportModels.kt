package com.vilync.ophthalmicerp.feature.sales.reports.data

/**
 * Raw data row returned by the Lens Library Status query.
 */
data class LensLibraryRawRow(
    val productId: Long,
    val productName: String,
    val power: String,
    val settlementStatus: String,
    val serialNumber: String,
    val expiryDate: String,
    val challanId: Long,
    val challanNumber: String,
    val challanDate: String,
    val saleId: Long?,
    val invoiceNumber: String?,
    val invoiceDate: String?
)
