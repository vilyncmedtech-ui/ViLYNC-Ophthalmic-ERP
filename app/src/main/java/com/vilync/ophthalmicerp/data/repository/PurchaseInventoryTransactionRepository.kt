package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity

class PurchaseInventoryTransactionRepository(
    private val database: AppDatabase
) {

    suspend fun savePurchaseWithInventory(
        purchase: PurchaseEntity,
        purchaseItems: List<PurchaseItemEntity>,
        inventoryUnits: List<InventoryUnitEntity>
    ): Long {

        return database.withTransaction {

            // -------------------------------------------------
            // 1. SAVE PURCHASE HEADER
            // -------------------------------------------------

            val purchaseId = database
                .purchaseDao()
                .insertPurchase(purchase)


            // -------------------------------------------------
            // 2. SAVE PURCHASE ITEMS
            // -------------------------------------------------

            val itemsWithPurchaseId = purchaseItems.map { item ->

                item.copy(
                    purchaseId = purchaseId
                )
            }

            database
                .purchaseDao()
                .insertPurchaseItems(itemsWithPurchaseId)


            // -------------------------------------------------
            // 3. SAVE INDIVIDUAL INVENTORY UNITS
            // -------------------------------------------------

            inventoryUnits.forEach { unit ->

                val inventoryUnitId = database
                    .inventoryDao()
                    .insertInventoryUnit(unit)


                // ---------------------------------------------
                // 4. CREATE PURCHASE RECEIVED MOVEMENT
                // ---------------------------------------------

                database
                    .stockMovementDao()
                    .insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = inventoryUnitId,
                            serialNumber = unit.serialNumber,
                            movementType = "PURCHASE_RECEIVED",
                            fromStatus = "",
                            toStatus = "IN_STOCK",
                            partyName = purchase.supplierName,
                            referenceNumber = purchase.invoiceNumber,
                            movementDate = purchase.receivedDate,
                            remarks = "Stock received through purchase"
                        )
                    )
            }


            // -------------------------------------------------
            // RETURN SAVED PURCHASE ID
            // -------------------------------------------------

            purchaseId
        }
    }
}