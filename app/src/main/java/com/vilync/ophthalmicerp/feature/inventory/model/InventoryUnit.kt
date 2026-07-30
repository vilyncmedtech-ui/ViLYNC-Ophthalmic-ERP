package com.vilync.ophthalmicerp.feature.inventory.model

data class InventoryUnit(

    val id: Long = 0,

    val productName: String = "",

    val model: String = "",

    val category: String = "",

    val power: String = "",

    val serialNumber: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",

    val receivedDate: String = "",

    val supplierName: String = "",

    val purchaseInvoiceNumber: String = "",

    val status: InventoryStatus = InventoryStatus.IN_STOCK
)