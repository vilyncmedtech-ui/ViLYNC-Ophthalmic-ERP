package com.vilync.ophthalmicerp.feature.inventory.model

data class StockRegisterRow(

    val productId: Long,

    val productName: String,

    val model: String,

    val category: String,

    val power: String,

    val purchasedQuantity: Int,

    val purchaseReturnQuantity: Int,

    val availableQuantity: Int
)