package com.vilync.ophthalmicerp.feature.purchase.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


// =============================================================
// PURCHASE DETAIL ITEM
// =============================================================

data class PurchaseDetailItem(

    val purchaseItem: PurchaseItemEntity,

    val productName: String = "",

    val brand: String = "",

    val model: String = "",

    val category: String = "",

    val hsnCode: String = "",

    val lenses: List<PurchaseLensEntity> = emptyList()
)


// =============================================================
// UI STATE
// =============================================================

data class PurchaseDetailUiState(

    val isLoading: Boolean = true,

    val purchase: PurchaseEntity? = null,

    val items: List<PurchaseDetailItem> = emptyList(),

    val errorMessage: String? = null
)


// =============================================================
// VIEW MODEL
// =============================================================

class PurchaseDetailViewModel(

    private val purchaseId: Long,

    private val purchaseRepository: PurchaseRepository,

    private val productRepository: ProductMasterRepository

) : ViewModel() {


    private val _uiState =
        MutableStateFlow(
            PurchaseDetailUiState()
        )

    val uiState: StateFlow<PurchaseDetailUiState> =
        _uiState.asStateFlow()


    init {

        loadPurchaseDetail()
    }


    // =========================================================
    // LOAD PURCHASE DETAIL
    // =========================================================

    private fun loadPurchaseDetail() {

        viewModelScope.launch {

            _uiState.value =
                PurchaseDetailUiState(
                    isLoading = true
                )

            try {

                // -------------------------------------------------
                // PURCHASE HEADER
                // -------------------------------------------------

                val purchase =
                    purchaseRepository
                        .getPurchaseById(
                            purchaseId
                        )

                if (purchase == null) {

                    _uiState.value =
                        PurchaseDetailUiState(
                            isLoading = false,
                            errorMessage =
                                "Purchase invoice not found."
                        )

                    return@launch
                }


                // -------------------------------------------------
                // PURCHASE ITEMS
                // -------------------------------------------------

                val purchaseItems =
                    purchaseRepository
                        .getPurchaseItems(
                            purchaseId
                        )
                        .first()


                // -------------------------------------------------
                // RESOLVE PRODUCT + PHYSICAL LENSES
                // -------------------------------------------------

                val detailItems =
                    purchaseItems.map { item ->

                        val product =
                            productRepository
                                .getProductById(
                                    item.productId
                                )

                        val lenses =
                            purchaseRepository
                                .getPurchaseLenses(
                                    item.id
                                )
                                .first()


                        PurchaseDetailItem(

                            purchaseItem = item,

                            productName =
                                product?.productName
                                    ?: "Unknown Product",

                            brand =
                                product?.brand
                                    ?: "",

                            model =
                                product?.model
                                    ?: "",

                            category =
                                product?.category
                                    ?.name
                                    ?: "",

                            hsnCode =
                                product?.hsnCode
                                    ?: "",

                            lenses =
                                lenses
                        )
                    }


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                _uiState.value =
                    PurchaseDetailUiState(

                        isLoading = false,

                        purchase = purchase,

                        items = detailItems,

                        errorMessage = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    PurchaseDetailUiState(

                        isLoading = false,

                        errorMessage =
                            e.message
                                ?: "Unable to load purchase details."
                    )
            }
        }
    }
}


// =============================================================
// VIEW MODEL FACTORY
// =============================================================

class PurchaseDetailViewModelFactory(

    private val purchaseId: Long,

    private val purchaseRepository: PurchaseRepository,

    private val productRepository: ProductMasterRepository

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                PurchaseDetailViewModel::class.java
            )
        ) {

            return PurchaseDetailViewModel(

                purchaseId =
                    purchaseId,

                purchaseRepository =
                    purchaseRepository,

                productRepository =
                    productRepository

            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}