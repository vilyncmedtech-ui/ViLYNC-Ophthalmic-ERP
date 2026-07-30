package com.vilync.ophthalmicerp.feature.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository

class SalesViewModelFactory(
    private val salesRepository: SalesRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val challanRepository: ChallanRepository,
    private val editSaleId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            return SalesViewModel(
                salesRepository = salesRepository,
                partyRepository = partyRepository,
                productRepository = productRepository,
                inventoryRepository = inventoryRepository,
                challanRepository = challanRepository,
                editSaleId = editSaleId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
