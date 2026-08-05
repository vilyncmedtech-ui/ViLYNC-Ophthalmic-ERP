package com.vilync.ophthalmicerp.feature.inventory.logic

/**
 * Lightweight, immutable domain model for inventory state.
 * This is the primary data structure for all analytical inventory modules.
 */
data class InventorySnapshot(
    val productId: Long,
    val productName: String,
    val brandName: String,
    val model: String,
    val category: String,
    val power: String,
    val availableQuantity: Int,
    val minimumStock: Int,
    val reorderLevel: Int,
    val maximumStock: Int,
    val reorderQuantity: Int,
    val leadTimeDays: Int,
    val trackingType: String
)
