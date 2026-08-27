package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

data class OpeningStockUiItem(
    val productId: Long = 0L,
    val productName: String = "",
    val model: String = "",
    val power: String = "",
    val batchNumber: String = "",
    val serialNumber: String = "",
    val rawSerial: String = "",
    val expiryDate: String = "",
    val quantity: Int = 1,
    val unitCost: Double = 0.0,
    val gstPercent: Double = 0.0,
    val totalCost: Double = 0.0,
    val trackingType: String = "QUANTITY"
)
