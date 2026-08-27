package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase

class AddPurchaseOrderItemViewModelFactory(
    private val productRepository: ProductMasterRepository,
    private val stockUseCase: GetAvailableStockUseCase? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddPurchaseOrderItemViewModel::class.java)) {
            return AddPurchaseOrderItemViewModel(productRepository, stockUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
