package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository


class PurchaseViewModelFactory(

    private val partyRepository: PartyRepository,

    private val purchaseRepository: PurchaseRepository,

    private val productRepository: ProductMasterRepository,

    private val auditTrailRepository: AuditTrailRepository,

    private val numberingRepository: com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository? = null,

    private val initialProductId: Long = 0L,
    private val initialPower: String = "",
    private val initialQty: Int = 0,
    private val initialStatus: String = "POSTED"

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {


        if (
            modelClass.isAssignableFrom(
                PurchaseViewModel::class.java
            )
        ) {

            return PurchaseViewModel(

                partyRepository =
                    partyRepository,

                purchaseRepository =
                    purchaseRepository,

                productRepository =
                    productRepository,

                auditTrailRepository =
                    auditTrailRepository,

                numberingRepository =
                    numberingRepository,

                initialProductId = initialProductId,
                initialPower = initialPower,
                initialQty = initialQty,
                initialStatus = initialStatus

            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}