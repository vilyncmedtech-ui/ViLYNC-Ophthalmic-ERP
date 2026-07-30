package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository

class AddPurchaseItemViewModelFactory(
    private val productRepository: ProductMasterRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(AddPurchaseItemViewModel::class.java)) {
            return AddPurchaseItemViewModel(
                productRepository = productRepository,
                purchaseRepository = purchaseRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
