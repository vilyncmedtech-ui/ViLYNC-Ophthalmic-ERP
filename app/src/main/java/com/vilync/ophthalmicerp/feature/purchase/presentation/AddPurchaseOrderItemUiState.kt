package com.vilync.ophthalmicerp.feature.purchase.presentation

import java.util.UUID

data class OrderItemVariant(
    val id: String = UUID.randomUUID().toString(),
    val power: String = "",
    val quantity: Int = 1
)

data class AddPurchaseOrderItemUiState(
    val productId: Long = 0L,
    val productName: String = "",
    val model: String = "",
    val category: String = "",
    val hsnCode: String = "",
    
    // Multiple variants per product
    val variants: List<OrderItemVariant> = emptyList(),
    val availablePowers: List<String> = emptyList(),

    val purchaseRate: Double = 0.0,
    val discountPercent: Double = 0.0,
    val gstPercent: Double = 18.0,

    val totalQuantity: Int = 0,
    val grossAmount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val overallTotal: Double = 0.0,

    val isEditMode: Boolean = false,
    val errorMessage: String? = null
)
