package com.vilync.ophthalmicerp.feature.inventory.logic

/**
 * Lightweight data class for internal inventory calculation.
 */
data class ProductPowerStock(
    val productId: Long,
    val power: String,
    val quantity: Int
)
