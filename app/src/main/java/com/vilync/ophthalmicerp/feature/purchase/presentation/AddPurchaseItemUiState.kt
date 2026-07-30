package com.vilync.ophthalmicerp.feature.purchase.presentation

import com.vilync.ophthalmicerp.feature.purchase.model.IolLensDetail

data class AddPurchaseItemUiState(
    val productId: Long = 0L,
    val productName: String = "",
    val model: String = "",
    val category: String = "IOL",
    val hsnCode: String = "",
    val power: String = "",
    val quantity: String = "1",
    val purchaseRate: String = "",
    val discountPercent: String = "0",
    val gstPercent: String = "0",
    val batchNumber: String = "",

    // Every physical IOL keeps its own Serial Number + Expiry.
    val lensDetails: List<IolLensDetail> = listOf(IolLensDetail()),

    // Indexes currently being checked against saved purchase_lenses.
    val checkingSerialIndexes: Set<Int> = emptySet(),

    // Indexes whose serial is already present in saved purchase_lenses
    // OR duplicated in another row of this current item.
    val duplicateSerialIndexes: Set<Int> = emptySet(),

    val errorMessage: String? = null
)
