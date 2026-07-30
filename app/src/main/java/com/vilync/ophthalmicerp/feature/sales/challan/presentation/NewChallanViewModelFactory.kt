package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository

class NewChallanViewModelFactory(
    private val challanRepository: ChallanRepository,
    private val inventoryRepository: InventoryRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductMasterRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                NewChallanViewModel::class.java
            )
        ) {
            return NewChallanViewModel(
                challanRepository =
                    challanRepository,
                inventoryRepository =
                    inventoryRepository,
                partyRepository =
                    partyRepository,
                productRepository =
                    productRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
