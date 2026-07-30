package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.SerialStockRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository


class SerialMovementHistoryViewModelFactory(

    private val inventoryUnitId: Long,

    private val serialStockRepository:
    SerialStockRepository,

    private val stockMovementRepository:
    StockMovementRepository

) : ViewModelProvider.Factory {


    // =========================================================
    // CREATE VIEW MODEL
    // =========================================================

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {


        if (
            modelClass.isAssignableFrom(
                SerialMovementHistoryViewModel::class.java
            )
        ) {

            return SerialMovementHistoryViewModel(

                inventoryUnitId =
                    inventoryUnitId,

                serialStockRepository =
                    serialStockRepository,

                stockMovementRepository =
                    stockMovementRepository

            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}