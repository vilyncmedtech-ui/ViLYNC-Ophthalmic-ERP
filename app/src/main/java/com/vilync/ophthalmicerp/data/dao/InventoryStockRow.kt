package com.vilync.ophthalmicerp.data.dao

/**
 * Room query projection used by Inventory Stock Register.
 *
 * This is NOT a database table/entity.
 * Values are calculated from stock-affecting transactions.
 *
 * Current phase:
 * Purchase IN
 * minus
 * Active Purchase Return OUT
 *
 * Future inventory sources such as Opening Stock,
 * Stock Adjustment and Sales can be incorporated into
 * the Inventory stock query without changing the UI model.
 */
data class InventoryStockRow(

    val productId: Long,

    val productName: String,

    val model: String,

    val category: String,

    val power: String,

    val purchasedQuantity: Int,

    val purchaseReturnQuantity: Int,

    val availableQuantity: Int
)