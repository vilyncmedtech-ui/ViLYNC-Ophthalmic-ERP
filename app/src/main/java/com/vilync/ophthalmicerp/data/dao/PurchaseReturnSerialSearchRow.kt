package com.vilync.ophthalmicerp.data.dao

data class PurchaseReturnSerialSearchRow(
    val purchaseLensId: Long,
    val serialNumber: String,
    val expiryDate: String,
    val purchaseItemId: Long,
    val purchaseId: Long,
    val invoiceNumber: String,
    val invoiceDate: String,
    val supplierName: String,
    val productId: Long,
    val power: String,
    val alreadyReturned: Int
)
