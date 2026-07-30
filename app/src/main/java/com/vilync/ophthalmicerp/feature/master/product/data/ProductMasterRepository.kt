package com.vilync.ophthalmicerp.feature.master.product.data

import com.vilync.ophthalmicerp.data.dao.ProductDao
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import com.vilync.ophthalmicerp.feature.master.product.model.ProductUnit
import com.vilync.ophthalmicerp.feature.product.model.ProductCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class ProductMasterRepository(
    private val productDao: ProductDao
) {

    // =========================================================
    // SAVE RESULT
    // =========================================================

    sealed class SaveResult {

        data class Success(
            val productId: Long
        ) : SaveResult()

        data object DuplicateProduct :
            SaveResult()

        data class Error(
            val message: String
        ) : SaveResult()
    }


    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================
    //
    // IMPORTANT:
    // Used by Purchase Product autocomplete.
    // Do not remove.
    //
    // =========================================================

    fun getAllActiveProducts():
            Flow<List<ProductMaster>> {

        return productDao
            .getAllActiveProducts()
            .map { entities ->

                entities.map { entity ->

                    entityToProductMaster(
                        entity
                    )
                }
            }
    }


    // =========================================================
    // SEARCH ACTIVE PRODUCTS
    // =========================================================
    //
    // IMPORTANT:
    // Used by AddPurchaseItemViewModel.
    // Do not remove.
    //
    // =========================================================

    fun searchProducts(
        query: String
    ): Flow<List<ProductMaster>> {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {

            return getAllActiveProducts()
        }

        return productDao
            .searchProducts(
                query = cleanQuery
            )
            .map { entities ->

                entities.map { entity ->

                    entityToProductMaster(
                        entity
                    )
                }
            }
    }


    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    suspend fun getProductById(
        productId: Long
    ): ProductMaster? {

        if (productId <= 0L) {
            return null
        }

        val entity =
            productDao.getProductById(
                productId
            )
                ?: return null

        return entityToProductMaster(
            entity
        )
    }


    // =========================================================
    // MANUFACTURER / COMPANY AUTOCOMPLETE
    // =========================================================
    //
    // Company / Manufacturer is stored in ProductEntity.brandName.
    //
    // =========================================================

    fun getManufacturerSuggestions():
            Flow<List<String>> {

        return productDao
            .getManufacturerSuggestions()
            .map { manufacturers ->

                manufacturers
                    .map {
                        cleanDisplayText(it)
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinctBy {
                        it.lowercase()
                    }
                    .sortedBy {
                        it.lowercase()
                    }
            }
    }


    // =========================================================
    // SEARCH MANUFACTURER / COMPANY
    // =========================================================

    suspend fun searchManufacturerSuggestions(
        query: String,
        limit: Int = 10
    ): List<String> {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {
            return emptyList()
        }

        return productDao
            .searchManufacturerSuggestions(
                query = cleanQuery,
                limit = limit
            )
            .map {
                cleanDisplayText(it)
            }
            .filter {
                it.isNotBlank()
            }
            .distinctBy {
                it.lowercase()
            }
    }


    // =========================================================
    // SAVE PRODUCT
    // =========================================================

    suspend fun saveProduct(
        product: ProductMaster
    ): SaveResult {

        return try {

            // =================================================
            // CLEAN DISPLAY VALUES
            // =================================================

            val cleanProductName =
                cleanDisplayText(
                    product.productName
                )

            val cleanCompanyName =
                cleanDisplayText(
                    product.brand
                )

            val cleanModel =
                cleanDisplayText(
                    product.model
                )


            // =================================================
            // NORMALIZED PRODUCT NAME
            // =================================================

            val normalizedProductName =
                normalizeForDuplicateCheck(
                    cleanProductName
                )


            // =================================================
            // BASIC VALIDATION
            // =================================================

            if (normalizedProductName.isBlank()) {

                return SaveResult.Error(
                    message =
                        "Product Name is required."
                )
            }


            // =================================================
            // DUPLICATE CHECK
            // =================================================
            //
            // Business rule:
            //
            // Product Name itself must be unique.
            //
            // =================================================

            val duplicateCount =

                if (product.id > 0L) {

                    productDao
                        .countDuplicateProductForEdit(

                            normalizedProductName =
                                normalizedProductName,

                            excludeProductId =
                                product.id
                        )

                } else {

                    productDao
                        .countDuplicateProduct(

                            normalizedProductName =
                                normalizedProductName
                        )
                }


            if (duplicateCount > 0) {

                return SaveResult.DuplicateProduct
            }


            // =================================================
            // CREATE DATABASE ENTITY
            // =================================================

            val productEntity =
                ProductEntity(

                    id =
                        product.id,

                    productName =
                        cleanProductName,

                    /*
                     * Database column remains brandName.
                     *
                     * UI name:
                     * Company / Manufacturer
                     */
                    brandName =
                        cleanCompanyName,

                    model =
                        cleanModel,

                    category =
                        product.category.name,

                    unit =
                        product.unit.name,

                    trackingType =
                        determineTrackingType(
                            product
                        ),

                    hsnCode =
                        product.hsnCode.trim(),

                    gstPercent =
                        product.gstPercent,

                    purchasePrice =
                        product.purchasePrice,

                    purchaseGstAmount =
                        product.purchaseGstAmount,

                    netPurchasePrice =
                        product.netPurchasePrice,

                    retailPrice =
                        product.retailPrice,

                    retailGstAmount =
                        product.retailGstAmount,

                    netRetailPrice =
                        product.netRetailPrice,

                    mrp =
                        product.mrp,

                    powerApplicable =
                        product.powerApplicable,

                    batchApplicable =
                        product.batchApplicable,

                    expiryApplicable =
                        product.expiryApplicable,

                    serialNumberRequired =
                        product.serialNumberRequired,

                    serialPrefix =
                        product.serialPrefix.trim().uppercase(),

                    isActive =
                        product.isActive
                )


            // =================================================
            // UPDATE EXISTING PRODUCT
            // =================================================

            val savedProductId =

                if (product.id > 0L) {

                    productDao.updateProduct(
                        productEntity
                    )

                    product.id

                } else {

                    // =========================================
                    // INSERT NEW PRODUCT
                    // =========================================

                    productDao.insertProduct(
                        productEntity
                    )
                }


            // =================================================
            // SUCCESS
            // =================================================

            SaveResult.Success(
                productId =
                    savedProductId
            )

        } catch (
            exception: Exception
        ) {

            SaveResult.Error(

                message =
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to save product."
            )
        }
    }


    // =========================================================
    // DELETE PRODUCT
    // =========================================================

    suspend fun deleteProduct(
        productId: Long
    ): Boolean {

        if (productId <= 0L) {
            return false
        }

        return try {

            val deletedRows =
                productDao.deleteProduct(
                    productId
                )

            deletedRows > 0

        } catch (
            exception: Exception
        ) {

            false
        }
    }


    // =========================================================
    // ENTITY -> PRODUCT MASTER
    // =========================================================

    private fun entityToProductMaster(
        entity: ProductEntity
    ): ProductMaster {

        return ProductMaster(

            id =
                entity.id,

            productName =
                entity.productName,

            brand =
                entity.brandName,

            model =
                entity.model,

            category =
                parseCategory(
                    entity.category
                ),

            unit =
                parseUnit(
                    entity.unit
                ),

            hsnCode =
                entity.hsnCode,

            gstPercent =
                entity.gstPercent,

            purchasePrice =
                entity.purchasePrice,

            purchaseGstAmount =
                entity.purchaseGstAmount,

            netPurchasePrice =
                entity.netPurchasePrice,

            retailPrice =
                entity.retailPrice,

            retailGstAmount =
                entity.retailGstAmount,

            netRetailPrice =
                entity.netRetailPrice,

            mrp =
                entity.mrp,

            powerApplicable =
                entity.powerApplicable,

            batchApplicable =
                entity.batchApplicable,

            expiryApplicable =
                entity.expiryApplicable,

            serialNumberRequired =
                entity.serialNumberRequired,

            serialPrefix =
                entity.serialPrefix,

            isActive =
                entity.isActive
        )
    }


    // =========================================================
    // PARSE PRODUCT CATEGORY
    // =========================================================

    private fun parseCategory(
        value: String
    ): ProductCategory {

        return ProductCategory.entries
            .firstOrNull {

                it.name.equals(
                    value,
                    ignoreCase = true
                )
            }
            ?: ProductCategory.OTHER
    }


    // =========================================================
    // PARSE PRODUCT UNIT
    // =========================================================

    private fun parseUnit(
        value: String
    ): ProductUnit {

        return ProductUnit.entries
            .firstOrNull {

                it.name.equals(
                    value,
                    ignoreCase = true
                )
            }
            ?: ProductUnit.PCS
    }


    // =========================================================
    // CLEAN TEXT
    // =========================================================

    private fun cleanDisplayText(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }


    // =========================================================
    // NORMALIZE PRODUCT NAME
    // =========================================================

    private fun normalizeForDuplicateCheck(
        value: String
    ): String {

        return cleanDisplayText(
            value
        ).lowercase()
    }


    // =========================================================
    // DETERMINE INVENTORY TRACKING TYPE
    // =========================================================

    private fun determineTrackingType(
        product: ProductMaster
    ): String {

        return when {

            product.serialNumberRequired -> {
                "SERIAL"
            }

            product.batchApplicable -> {
                "BATCH"
            }

            else -> {
                "QUANTITY"
            }
        }
    }
}