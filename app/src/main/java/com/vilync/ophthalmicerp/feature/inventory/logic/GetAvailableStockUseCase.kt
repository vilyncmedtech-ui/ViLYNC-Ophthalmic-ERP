package com.vilync.ophthalmicerp.feature.inventory.logic

import android.util.Log
import com.vilync.ophthalmicerp.data.repository.InventoryStockRepository
import com.vilync.ophthalmicerp.feature.product.model.TrackingType
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for available inventory stock across the ERP.
 * Unifies Serial-tracked and Quantity-tracked logic.
 */
class GetAvailableStockUseCase(
    private val repository: InventoryStockRepository
) {

    /**
     * Returns the true available stock for all products as a list of domain snapshots.
     */
    suspend fun execute(): List<InventorySnapshot> {
        val snapshots = mutableListOf<InventorySnapshot>()

        // 1. Fetch All Active Products (The Master List)
        val activeProducts = repository.getAllActiveProducts().first()
        
        // 2. Resolve Serial Tracked Stock (Physical Count)
        val serialStock = repository.getInStockSerialCounts().associate { 
            Pair(it.productId, it.power) to it.quantity 
        }

        // 3. Resolve Quantity Tracked Stock (Ledger Aggregation)
        val openingStock = repository.getOpeningStockQuantityStock().associateBy { Pair(it.productId, it.power) }
        val purchases = repository.getPurchaseQuantityStock().associateBy { Pair(it.productId, it.power) }
        val purchaseReturns = repository.getPurchaseReturnQuantityStock().associateBy { Pair(it.productId, it.power) }
        val sales = repository.getSaleQuantityStock().associateBy { Pair(it.productId, it.power) }
        val salesReturns = repository.getSalesReturnQuantityStock().associateBy { Pair(it.productId, it.power) }

        // 4. Unify and Calculate per Product Definition
        activeProducts.forEach { product ->
            val trackingMode = try {
                TrackingType.valueOf(product.trackingType)
            } catch (e: Exception) {
                Log.e("DataIntegrity", "CRITICAL_ERROR: Unknown trackingType '${product.trackingType}' for Product ID ${product.id}. Defaulting to 0.")
                null
            }

            when (trackingMode) {
                TrackingType.SERIAL -> {
                    val productSerials = serialStock.filterKeys { it.first == product.id }
                    if (productSerials.isEmpty()) {
                        snapshots.add(createSnapshot(product, "", 0))
                    } else {
                        productSerials.forEach { (key, qty) ->
                            snapshots.add(createSnapshot(product, key.second, qty))
                        }
                    }
                }
                TrackingType.QUANTITY, TrackingType.BATCH -> {
                    val productPowers = (openingStock.keys + purchases.keys + purchaseReturns.keys + sales.keys + salesReturns.keys)
                        .filter { it.first == product.id }
                        .map { it.second }
                        .toSet()
                        .ifEmpty { setOf("") }

                    productPowers.forEach { power ->
                        val key = Pair(product.id, power)
                        val os = openingStock[key]?.quantity ?: 0
                        val p = purchases[key]?.quantity ?: 0
                        val pr = purchaseReturns[key]?.quantity ?: 0
                        val s = sales[key]?.quantity ?: 0
                        val sr = salesReturns[key]?.quantity ?: 0
                        val qty = os + p - pr - s + sr
                        
                        snapshots.add(createSnapshot(product, power, qty))
                    }
                }
                null -> {
                    snapshots.add(createSnapshot(product, "", 0))
                }
            }
        }

        return snapshots
    }

    private fun createSnapshot(
        product: com.vilync.ophthalmicerp.data.entity.ProductEntity, 
        power: String, 
        qty: Int
    ): InventorySnapshot {
        return InventorySnapshot(
            productId = product.id,
            productName = product.productName,
            brandName = product.brandName,
            model = product.model,
            category = product.category,
            power = power,
            availableQuantity = qty,
            minimumStock = product.minimumStock,
            reorderLevel = product.reorderLevel,
            maximumStock = product.maximumStock,
            reorderQuantity = product.reorderQuantity,
            leadTimeDays = product.leadTimeDays,
            trackingType = product.trackingType
        )
    }
}
