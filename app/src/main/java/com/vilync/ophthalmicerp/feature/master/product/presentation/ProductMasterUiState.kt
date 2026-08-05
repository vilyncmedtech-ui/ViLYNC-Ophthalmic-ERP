package com.vilync.ophthalmicerp.feature.master.product.presentation

import com.vilync.ophthalmicerp.feature.master.product.model.ProductUnit
import com.vilync.ophthalmicerp.feature.product.model.ProductCategory


data class ProductMasterUiState(

    // =========================================================
    // PRODUCT ID / EDIT MODE
    // =========================================================

    val productId: Long = 0L,


    // =========================================================
    // BASIC INFORMATION
    // =========================================================

    val productName: String = "",

    /*
     * Database column = brandName
     * ProductMaster model = brand
     *
     * UI label =
     * Company / Manufacturer
     */
    val brand: String = "",

    val model: String = "",

    val category: ProductCategory =
        ProductCategory.IOL,

    val unit: ProductUnit =
        ProductUnit.PCS,


    // =========================================================
    // MANUFACTURER AUTOCOMPLETE
    // =========================================================
    //
    // manufacturerSuggestions
    //     Complete unique manufacturer list loaded from Room.
    //
    // filteredManufacturerSuggestions
    //     Suggestions currently matching what user typed.
    //
    // showManufacturerSuggestions
    //     Controls dropdown visibility.
    //
    // =========================================================

    val manufacturerSuggestions: List<String> =
        emptyList(),

    val filteredManufacturerSuggestions: List<String> =
        emptyList(),

    val showManufacturerSuggestions: Boolean =
        false,


    // =========================================================
    // TAX INFORMATION
    // =========================================================

    val hsnCode: String = "",

    val gstPercent: String = "0",


    // =========================================================
    // PURCHASE PRICING
    // =========================================================

    val purchasePrice: String = "",

    val purchaseGstAmount: Double = 0.0,

    val netPurchasePrice: Double = 0.0,


    // =========================================================
    // SALES PRICING
    // =========================================================

    val retailPrice: String = "",

    val retailGstAmount: Double = 0.0,

    val netRetailPrice: Double = 0.0,


    // =========================================================
    // MRP
    // =========================================================

    val mrp: String = "",


    // =========================================================
    // INVENTORY & TRACEABILITY
    // =========================================================

    val powerApplicable: Boolean = true,

    val batchApplicable: Boolean = true,

    val expiryApplicable: Boolean = true,

    val serialNumberRequired: Boolean = true,

    val serialPrefix: String = "",


    // =========================================================
    // INVENTORY PLANNING
    // =========================================================

    val minimumStock: String = "0",

    val reorderLevel: String = "0",

    val maximumStock: String = "0",

    val reorderQuantity: String = "0",

    val leadTimeDays: String = "0",


    // =========================================================
    // STATUS
    // =========================================================

    val isActive: Boolean = true,


    // =========================================================
    // SCREEN MODE
    // =========================================================

    val isEditMode: Boolean = false,


    // =========================================================
    // LOADING
    // =========================================================

    val isLoadingProduct: Boolean = false,


    // =========================================================
    // VALIDATION / MESSAGE
    // =========================================================

    val errorMessage: String? = null
)