package com.vilync.ophthalmicerp.feature.inventory.serialstock

import com.vilync.ophthalmicerp.data.entity.StockMovementEntity


data class SerialMovementHistoryUiState(

    // =========================================================
    // LOADING
    // =========================================================

    val isLoading: Boolean = true,


    // =========================================================
    // INVENTORY UNIT
    // =========================================================

    val inventoryUnitId: Long = 0L,


    // =========================================================
    // SERIAL INFORMATION
    // =========================================================

    val serialNumber: String = "",

    val productName: String = "",

    val brandName: String = "",

    val model: String = "",

    val category: String = "",

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",

    val receivedDate: String = "",

    val supplierName: String = "",

    val purchaseInvoiceNumber: String = "",

    val currentStatus: String = "",


    // =========================================================
    // MOVEMENT HISTORY
    // =========================================================

    val movements: List<StockMovementEntity> =
        emptyList(),


    // =========================================================
    // ERROR
    // =========================================================

    val errorMessage: String? = null

) {


    // =========================================================
    // MOVEMENT COUNT
    // =========================================================

    val movementCount: Int
        get() =
            movements.size


    // =========================================================
    // HAS HISTORY
    // =========================================================

    val hasMovementHistory: Boolean
        get() =
            movements.isNotEmpty()
}