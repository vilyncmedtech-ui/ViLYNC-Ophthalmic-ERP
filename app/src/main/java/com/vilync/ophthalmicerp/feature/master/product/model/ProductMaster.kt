package com.vilync.ophthalmicerp.feature.master.product.model

import com.vilync.ophthalmicerp.feature.product.model.ProductCategory

data class ProductMaster(

    // =========================================================
    // BASIC INFORMATION
    // =========================================================

    val id: Long = 0L,

    val productName: String = "",

    val brand: String = "",

    val model: String = "",

    val category: ProductCategory = ProductCategory.OTHER,

    val unit: ProductUnit = ProductUnit.PCS,


    // =========================================================
    // TAX INFORMATION
    // =========================================================

    val hsnCode: String = "",

    val gstPercent: Double = 0.0,


    // =========================================================
    // PURCHASE PRICING
    // =========================================================

    /*
     * Purchase Price is BEFORE GST.
     *
     * Example:
     *
     * Purchase Price = 5000
     * GST = 5%
     *
     * Purchase GST Amount = 250
     * Net Purchase Price = 5250
     */

    val purchasePrice: Double = 0.0,

    val purchaseGstAmount: Double = 0.0,

    val netPurchasePrice: Double = 0.0,


    // =========================================================
    // RETAIL PRICING
    // =========================================================

    /*
     * Retail Price is BEFORE GST.
     *
     * Example:
     *
     * Retail Price = 8000
     * GST = 5%
     *
     * Retail GST Amount = 400
     * Net Retail Price = 8400
     */

    val retailPrice: Double = 0.0,

    val retailGstAmount: Double = 0.0,

    val netRetailPrice: Double = 0.0,


    // =========================================================
    // MRP
    // =========================================================

    /*
     * MRP is treated as final GST-inclusive price.
     *
     * Therefore GST will NOT be added again to MRP.
     */

    val mrp: Double = 0.0,


    // =========================================================
    // INVENTORY CONTROL
    // =========================================================

    /*
     * Example:
     *
     * IOL:
     *
     * Power Applicable       = true
     * Batch Applicable       = true
     * Expiry Applicable      = true
     * Serial Number Required = true
     */

    val powerApplicable: Boolean = false,

    val batchApplicable: Boolean = false,

    val expiryApplicable: Boolean = false,

    val serialNumberRequired: Boolean = false,

    val serialPrefix: String = "",


    // =========================================================
    // STATUS
    // =========================================================

    /*
     * Inactive products remain in old Purchase/Sales records
     * but should not normally appear for new transactions.
     */

    val isActive: Boolean = true
)