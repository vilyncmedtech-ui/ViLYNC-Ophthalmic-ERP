package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.InventoryStockDao
import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryStockRepository(
    private val inventoryStockDao: InventoryStockDao
) {

    // =========================================================
    // STOCK REGISTER
    // =========================================================

    /**
     * Returns transaction-derived current stock.
     *
     * Current calculation:
     *
     * Purchase
     * minus
     * Active Purchase Return
     *
     * Cancelled Purchase Returns do not reduce stock.
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
                        availableQuantity =
                            row.availableQuantity
                    )
                }
            }
    }
}