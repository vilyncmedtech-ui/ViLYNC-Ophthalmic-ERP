package com.vilync.ophthalmicerp.data.dao


/**
 * Read-only row used by the Serial Stock Register.
 *
 * This is NOT a Room table.
 *
 * Data is derived from:
 *
 * inventory_units
 *      +
 * products
 *
 * One row represents one physical serial-tracked
 * inventory unit / IOL.
 */
data class SerialStockRow(

    // =========================================================
    // INVENTORY UNIT
    // =========================================================

    val inventoryUnitId: Long,

    val productId: Long,


    // =========================================================
    // PRODUCT
    // =========================================================

    val productName: String,

    val brandName: String,

    val model: String,

    val category: String,

    val serialPrefix: String,


    // =========================================================
    // PHYSICAL UNIT
    // =========================================================

    val power: String,

    val serialNumber: String,

    val batchNumber: String,

    val expiryDate: String,


    // =========================================================
    // PURCHASE / RECEIPT
    // =========================================================

    val receivedDate: String,

    val supplierName: String,

    val purchaseInvoiceNumber: String,


    // =========================================================
    // CURRENT INVENTORY STATUS
    // =========================================================

    val status: String
)