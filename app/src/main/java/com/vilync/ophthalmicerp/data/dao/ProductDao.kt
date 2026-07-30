package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ProductDao {


    // =========================================================
    // INSERT PRODUCT
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertProduct(
        product: ProductEntity
    ): Long


    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    @Update
    suspend fun updateProduct(
        product: ProductEntity
    ): Int


    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    @Query(
        """
        SELECT *
        FROM products
        WHERE id = :productId
        LIMIT 1
        """
    )
    suspend fun getProductById(
        productId: Long
    ): ProductEntity?


    // =========================================================
    // GET ALL ACTIVE PRODUCTS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM products
        WHERE isActive = 1
        ORDER BY productName COLLATE NOCASE ASC
        """
    )
    fun getAllActiveProducts():
            Flow<List<ProductEntity>>


    // =========================================================
    // GET ALL PRODUCTS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM products
        ORDER BY productName COLLATE NOCASE ASC
        """
    )
    fun getAllProducts():
            Flow<List<ProductEntity>>


    // =========================================================
    // SEARCH ACTIVE PRODUCTS
    // =========================================================

    @Query(
        """
        SELECT *
        FROM products
        WHERE isActive = 1
          AND (
                productName LIKE '%' || :query || '%' COLLATE NOCASE
                OR brandName LIKE '%' || :query || '%' COLLATE NOCASE
                OR model LIKE '%' || :query || '%' COLLATE NOCASE
                OR category LIKE '%' || :query || '%' COLLATE NOCASE
                OR hsnCode LIKE '%' || :query || '%' COLLATE NOCASE
              )
        ORDER BY
            CASE
                WHEN productName LIKE :query || '%' COLLATE NOCASE
                THEN 0
                ELSE 1
            END,
            productName COLLATE NOCASE ASC
        """
    )
    fun searchProducts(
        query: String
    ): Flow<List<ProductEntity>>


    // =========================================================
    // MANUFACTURER / COMPANY AUTOCOMPLETE
    // =========================================================
    //
    // Existing ProductEntity column:
    // brandName
    //
    // Returns unique manufacturer/company names already saved
    // in Product Master.
    //
    // =========================================================

    @Query(
        """
        SELECT DISTINCT TRIM(brandName)
        FROM products
        WHERE brandName IS NOT NULL
          AND TRIM(brandName) <> ''
        ORDER BY TRIM(brandName) COLLATE NOCASE ASC
        """
    )
    fun getManufacturerSuggestions():
            Flow<List<String>>


    // =========================================================
    // SEARCH MANUFACTURER / COMPANY
    // =========================================================

    @Query(
        """
        SELECT DISTINCT TRIM(brandName)
        FROM products
        WHERE brandName IS NOT NULL
          AND TRIM(brandName) <> ''
          AND brandName LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE
                WHEN brandName LIKE :query || '%' COLLATE NOCASE
                THEN 0
                ELSE 1
            END,
            TRIM(brandName) COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchManufacturerSuggestions(
        query: String,
        limit: Int = 10
    ): List<String>


    // =========================================================
    // DUPLICATE PRODUCT CHECK
    // =========================================================
    //
    // BUSINESS RULE:
    //
    // PRODUCT NAME MUST BE UNIQUE.
    //
    // Company / Manufacturer does not matter.
    // Model does not matter.
    //
    // Repository supplies normalized product name.
    //
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM products
        WHERE LOWER(TRIM(productName)) = :normalizedProductName
        """
    )
    suspend fun countDuplicateProduct(
        normalizedProductName: String
    ): Int


    // =========================================================
    // DUPLICATE CHECK WHILE EDITING
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM products
        WHERE id != :excludeProductId
          AND LOWER(TRIM(productName)) = :normalizedProductName
        """
    )
    suspend fun countDuplicateProductForEdit(
        normalizedProductName: String,
        excludeProductId: Long
    ): Int


    // =========================================================
    // DEACTIVATE PRODUCT
    // =========================================================
    //
    // Required by:
    //
    // data/repository/ProductRepository.kt
    //
    // Product is retained in database but hidden from normal
    // active-product selections.
    //
    // =========================================================

    @Query(
        """
        UPDATE products
        SET isActive = 0
        WHERE id = :productId
        """
    )
    suspend fun deactivateProduct(
        productId: Long
    ): Int


    // =========================================================
    // PERMANENTLY DELETE PRODUCT
    // =========================================================
    //
    // Used by Product Master delete functionality.
    //
    // =========================================================

    @Query(
        """
        DELETE FROM products
        WHERE id = :productId
        """
    )
    suspend fun deleteProduct(
        productId: Long
    ): Int
}