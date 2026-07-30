package com.vilync.ophthalmicerp.feature.purchase.model

data class PurchaseItem(

    // =========================================================
    // PRODUCT MASTER REFERENCE
    // =========================================================

    val productId: Long = 0L,


    // =========================================================
    // PRODUCT DETAILS
    // =========================================================

    val productName: String = "",

    val model: String = "",

    val category: String = "",

    // HSN Code for GST / Invoice / Accounting
    val hsnCode: String = "",


    // =========================================================
    // POWER
    // =========================================================

    /*
     * Examples:
     *
     * 20D
     * 20.5D
     */
    val power: String = "",


    // =========================================================
    // QUANTITY
    // =========================================================

    /*
     * For IOL:
     *
     * Quantity represents number of physical lenses.
     *
     * quantity should match:
     *
     * lensDetails.size
     */
    val quantity: Int = 0,


    // =========================================================
    // PURCHASE VALUES
    // =========================================================

    val purchaseRate: Double = 0.0,

    val discountPercent: Double = 0.0,

    val gstPercent: Double = 0.0,


    // =========================================================
    // BATCH / LOT
    // =========================================================

    val batchNumber: String = "",


    // =========================================================
    // PHYSICAL IOL DETAILS
    // =========================================================

    /*
     * Every physical IOL has its own:
     *
     * Serial Number + Expiry
     *
     * Example:
     *
     * Lens 1
     * SN001 | 1229
     *
     * Lens 2
     * SN002 | 0630
     *
     * Lens 3
     * SN003 | 1129
     *
     * Therefore:
     *
     * 3 lensDetails = Quantity 3
     */
    val lensDetails: List<IolLensDetail> = emptyList()
)