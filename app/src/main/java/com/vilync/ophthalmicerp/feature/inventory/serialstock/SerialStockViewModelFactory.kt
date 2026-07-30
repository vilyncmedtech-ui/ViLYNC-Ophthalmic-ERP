package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.SerialStockRepository


class SerialStockViewModelFactory(

    private val repository: SerialStockRepository

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                SerialStockViewModel::class.java
            )
        ) {

            return SerialStockViewModel(
                repository = repository
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}