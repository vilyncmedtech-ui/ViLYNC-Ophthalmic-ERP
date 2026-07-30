package com.vilync.ophthalmicerp.feature.master.product.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    application: Application
) : AndroidViewModel(application) {


    // =========================================================
    // DATABASE / REPOSITORY
    // =========================================================

    private val database =
        DatabaseProvider.getDatabase(
            application
        )


    private val repository =
        ProductRepository(
            productDao =
                database.productDao()
        )


    // =========================================================
    // SEARCH QUERY
    // =========================================================

    private val _searchQuery =
        MutableStateFlow("")


    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()


    // =========================================================
    // PRODUCT LIST
    // =========================================================

    val products: StateFlow<List<ProductEntity>> =
        _searchQuery
            .flatMapLatest {
                    query ->

                if (
                    query.isBlank()
                ) {

                    repository
                        .getAllActiveProducts()

                } else {

                    repository
                        .searchProducts(
                            query.trim()
                        )
                }
            }
            .stateIn(
                scope =
                    viewModelScope,

                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = 5_000
                    ),

                initialValue =
                    emptyList()
            )


    // =========================================================
    // UI MESSAGE
    // =========================================================

    private val _message =
        MutableStateFlow<String?>(null)


    val message: StateFlow<String?> =
        _message.asStateFlow()


    // =========================================================
    // UPDATE SEARCH
    // =========================================================

    fun updateSearchQuery(
        value: String
    ) {

        _searchQuery.value =
            value
    }


    // =========================================================
    // CLEAR SEARCH
    // =========================================================

    fun clearSearch() {

        _searchQuery.value =
            ""
    }


    // =========================================================
    // DEACTIVATE PRODUCT
    // =========================================================

    fun deactivateProduct(
        productId: Long
    ) {

        if (
            productId <= 0L
        ) {

            _message.value =
                "Invalid product"

            return
        }


        viewModelScope.launch {

            try {

                repository
                    .deactivateProduct(
                        productId
                    )


                _message.value =
                    "Product deactivated successfully"

            } catch (
                exception: Exception
            ) {

                _message.value =
                    exception.message
                        ?: "Unable to deactivate product"
            }
        }
    }


    // =========================================================
    // CLEAR MESSAGE
    // =========================================================

    fun clearMessage() {

        _message.value =
            null
    }
}