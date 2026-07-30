package com.vilync.ophthalmicerp.feature.purchase.model

data class Purchase(
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val receivedDate: String = "",
    val purchaseType: String = "Invoice",
    val paymentType: String = "Credit",
    val creditDays: Int = 0,
    val reference: String = "",
    val items: List<PurchaseItem> = emptyList()
)