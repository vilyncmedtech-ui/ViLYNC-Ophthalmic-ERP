package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.ProductDao
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao
) {

    fun getAllActiveProducts(): Flow<List<ProductEntity>> {
        return productDao.getAllActiveProducts()
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductById(
        productId: Long
    ): ProductEntity? {
        return productDao.getProductById(productId)
    }

    suspend fun insertProduct(
        product: ProductEntity
    ): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(
        product: ProductEntity
    ) {
        productDao.updateProduct(product)
    }

    suspend fun deactivateProduct(
        productId: Long
    ) {
        productDao.deactivateProduct(productId)
    }
}