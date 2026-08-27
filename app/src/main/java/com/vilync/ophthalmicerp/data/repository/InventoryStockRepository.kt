package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.InventoryStockDao
import com.vilync.ophthalmicerp.data.dao.ProductDao
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.feature.inventory.logic.ProductPowerStock
import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryStockRepository(
    private val inventoryStockDao: InventoryStockDao,
    private val productDao: ProductDao,
    private val thresholdDao: com.vilync.ophthalmicerp.data.dao.InventoryThresholdDao
) {

    // =========================================================
    // THRESHOLDS
    // =========================================================

    suspend fun getAllThresholds(): List<com.vilync.ophthalmicerp.data.entity.InventoryThresholdEntity> =
        thresholdDao.getAllThresholds()

    // =========================================================
    // MASTER DATA
    // =========================================================

    fun getAllActiveProducts(): Flow<List<ProductEntity>> =
        productDao.getAllActiveProducts()

    // =========================================================
    // MODULAR DATA RETRIEVAL (PHASE 1)
    // =========================================================

    suspend fun getInStockSerialCounts(): List<ProductPowerStock> =
        inventoryStockDao.getInStockSerialCounts()

    suspend fun getPurchaseQuantityStock(): List<ProductPowerStock> =
        inventoryStockDao.getPurchaseQuantityStock()

    suspend fun getPurchaseReturnQuantityStock(): List<ProductPowerStock> =
        inventoryStockDao.getPurchaseReturnQuantityStock()

    suspend fun getSaleQuantityStock(): List<ProductPowerStock> =
        inventoryStockDao.getSaleQuantityStock()

    suspend fun getSalesReturnQuantityStock(): List<ProductPowerStock> =
        inventoryStockDao.getSalesReturnQuantityStock()

    suspend fun getOpeningStockQuantityStock(): List<ProductPowerStock> =
        inventoryStockDao.getOpeningStockQuantityStock()

    // =========================================================
    // STOCK REGISTER
    // =========================================================

    /**
     * Returns reconciled current stock.
     */
    fun getStockRegister():
            Flow<List<StockRegisterRow>> {

        return inventoryStockDao
            .getStockRegister()
            .map { rows ->

                rows.map { row ->

                    StockRegisterRow(
                        productId = row.productId,
                        productName = row.productName,
                        model = row.model,
                        category = row.category,
                        power = row.power,
                        purchasedQuantity =
                            row.purchasedQuantity,
                        purchaseReturnQuantity =
                            row.purchaseReturnQuantity,
                        soldQuantity =
                            row.soldQuantity,
                        otherOutQuantity =
                            row.otherOutQuantity,
                        availableQuantity =
                            row.availableQuantity
                    )
                }
            }
    }
}