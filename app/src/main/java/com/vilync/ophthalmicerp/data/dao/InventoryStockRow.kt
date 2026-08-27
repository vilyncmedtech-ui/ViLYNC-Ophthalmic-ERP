package com.vilync.ophthalmicerp.data.dao

/**
 * Room query projection used by Inventory Stock Register.
 *
 * This is NOT a database table/entity.
 * Values are calculated from stock-affecting transactions and
 * current physical inventory status.
 */
data class InventoryStockRow(

    val productId: Long,

    val productName: String,

    val model: String,

    val category: String,

    val power: String,

    val purchasedQuantity: Int,

    val purchaseReturnQuantity: Int,

    val soldQuantity: Int,

    val otherOutQuantity: Int,

    val availableQuantity: Int
)
