package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository

class PurchaseOrderViewModelFactory(
    private val partyRepository: PartyRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductMasterRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val numberingRepository: DocumentNumberingRepository,
    private val initialProductId: Long = 0L,
    private val initialPower: String = "",
    private val initialQty: Int = 0
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderViewModel::class.java)) {
            return PurchaseOrderViewModel(
                partyRepository,
                purchaseRepository,
                productRepository,
                auditTrailRepository,
                numberingRepository,
                initialProductId,
                initialPower,
                initialQty
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
