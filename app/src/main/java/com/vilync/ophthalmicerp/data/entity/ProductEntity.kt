package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "products"
)
data class ProductEntity(

    // =========================================================
    // PRIMARY KEY
    // =========================================================

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,


    // =========================================================
    // BASIC PRODUCT INFORMATION
    // =========================================================

    val productName: String,

    val brandName: String = "",

    val model: String = "",

    val category: String,


    // =========================================================
    // UNIT / TRACKING
    // =========================================================

    /*
     * Examples:
     *
     * PCS
     * BOX
     * PACK
     * BOTTLE
     * SYRINGE
     * SET
     */

    val unit: String = "PCS",


    /*
     * Existing ERP tracking field.
     *
     * We are keeping this field because other inventory
     * code may already depend on it.
     *
     * Examples:
     *
     * SERIAL
     * BATCH
     * QUANTITY
     */

    val trackingType: String = "QUANTITY",


    // =========================================================
    // TAX INFORMATION
    // =========================================================

    val hsnCode: String = "",

    val gstPercent: Double = 0.0,


    // =========================================================
    // PURCHASE PRICING
    // =========================================================

    /*
     * Base Purchase Price before GST
     */

    val purchasePrice: Double = 0.0,


    /*
     * GST amount calculated on Purchase Price
     */

    val purchaseGstAmount: Double = 0.0,


    /*
     * Purchase Price + GST
     */

    val netPurchasePrice: Double = 0.0,


    // =========================================================
    // RETAIL PRICING
    // =========================================================

    /*
     * Base Retail Price before GST
     */

    val retailPrice: Double = 0.0,


    /*
     * GST amount calculated on Retail Price
     */

    val retailGstAmount: Double = 0.0,


    /*
     * Retail Price + GST
     */

    val netRetailPrice: Double = 0.0,


    // =========================================================
    // MRP
    // =========================================================

    /*
     * MRP is treated as tax-inclusive.
     */

    val mrp: Double = 0.0,


    // =========================================================
    // INVENTORY CONTROL
    // =========================================================

    val powerApplicable: Boolean = false,

    val batchApplicable: Boolean = false,

    val expiryApplicable: Boolean = false,

    val serialNumberRequired: Boolean = false,

    val serialPrefix: String = "",


    // =========================================================
    // INVENTORY PLANNING
    // =========================================================

    val minimumStock: Int = 0,

    val reorderLevel: Int = 0,

    val maximumStock: Int = 0,

    val reorderQuantity: Int = 0,

    val leadTimeDays: Int = 0,


    // =========================================================
    // STATUS
    // =========================================================

    val isActive: Boolean = true
)