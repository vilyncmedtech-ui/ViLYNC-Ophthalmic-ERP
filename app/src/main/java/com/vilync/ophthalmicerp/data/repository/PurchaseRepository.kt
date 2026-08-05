package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.dao.PurchaseDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


class PurchaseRepository(

    private val purchaseDao: PurchaseDao,

    private val database: AppDatabase

) {


    // =========================================================
    // INSERT PURCHASE
    // =========================================================

    suspend fun insertPurchase(
        purchase: PurchaseEntity
    ): Long =
        purchaseDao.insertPurchase(
            purchase
        )


    // =========================================================
    // INSERT PURCHASE ITEM
    // =========================================================

    suspend fun insertPurchaseItem(
        item: PurchaseItemEntity
    ): Long =
        purchaseDao.insertPurchaseItem(
            item
        )


    suspend fun insertPurchaseItems(
        items: List<PurchaseItemEntity>
    ) {
        purchaseDao.insertPurchaseItems(
            items
        )
    }


    // =========================================================
    // INSERT PURCHASE LENS
    // =========================================================

    suspend fun insertPurchaseLens(
        lens: PurchaseLensEntity
    ): Long =
        purchaseDao.insertPurchaseLens(
            lens
        )


    suspend fun insertPurchaseLenses(
        lenses: List<PurchaseLensEntity>
    ) {
        purchaseDao.insertPurchaseLenses(
            lenses
        )
    }


    // =========================================================
    // DUPLICATE INVOICE CHECK - NEW PURCHASE
    // =========================================================

    suspend fun purchaseInvoiceExists(
        supplierId: Long,
        invoiceNumber: String
    ): Boolean {

        return purchaseDao.purchaseInvoiceExists(
            supplierId = supplierId,
            invoiceNumber = invoiceNumber.trim()
        )
    }


    // =========================================================
    // DUPLICATE INVOICE CHECK - EDIT PURCHASE
    // =========================================================

    suspend fun purchaseInvoiceExistsExcludingPurchase(
        supplierId: Long,
        invoiceNumber: String,
        excludePurchaseId: Long
    ): Boolean {

        return purchaseDao
            .purchaseInvoiceExistsExcludingPurchase(
                supplierId = supplierId,
                invoiceNumber = invoiceNumber.trim(),
                excludePurchaseId = excludePurchaseId
            )
    }


    // =========================================================
    // DUPLICATE SERIAL CHECK - NEW PURCHASE
    // =========================================================

    suspend fun purchaseLensSerialExists(
        serialNumber: String
    ): Boolean {

        val normalized =
            serialNumber.trim()

        if (normalized.isBlank()) {
            return false
        }

        return purchaseDao.purchaseLensSerialExists(
            normalized
        )
    }


    // =========================================================
    // DUPLICATE SERIAL CHECK - EDIT PURCHASE
    // =========================================================

    suspend fun purchaseLensSerialExistsExcludingPurchase(
        serialNumber: String,
        excludePurchaseId: Long
    ): Boolean {

        val normalized =
            serialNumber.trim()

        if (normalized.isBlank()) {
            return false
        }

        return purchaseDao
            .purchaseLensSerialExistsExcludingPurchase(
                serialNumber = normalized,
                excludePurchaseId = excludePurchaseId
            )
    }


    // =========================================================
    // SAVE COMPLETE NEW PURCHASE
    // =========================================================

    suspend fun saveCompletePurchase(
        purchase: PurchaseEntity,
        itemsWithLenses:
        List<Pair<PurchaseItemEntity, List<PurchaseLensEntity>>>
    ): Long {

        return database.withTransaction {

            // =================================================
            // STEP 1
            // SAVE PURCHASE + ITEMS + PURCHASE LENSES
            // =================================================

            val purchaseId =
                purchaseDao.saveCompletePurchase(
                    purchase = purchase,
                    itemsWithLenses = itemsWithLenses
                )


            require(
                purchaseId > 0L
            ) {
                "Purchase could not be saved."
            }


            // =================================================
            // STEP 2
            // READ SAVED PURCHASE ITEMS
            // =================================================

            val savedItems =
                purchaseDao
                    .getPurchaseItems(
                        purchaseId = purchaseId
                    )
                    .first()


            // =================================================
            // STEP 3
            // CREATE SERIAL-WISE INVENTORY
            // =================================================

            savedItems.forEach { savedItem ->

                val savedLenses =
                    purchaseDao
                        .getPurchaseLenses(
                            purchaseItemId =
                                savedItem.id
                        )
                        .first()


                savedLenses.forEach { savedLens ->

                    val serialNumber =
                        savedLens
                            .serialNumber
                            .trim()


                    require(
                        serialNumber.isNotBlank()
                    ) {
                        "Serial number cannot be blank for serial-tracked inventory."
                    }


                    // =========================================
                    // DUPLICATE INVENTORY SERIAL SAFETY
                    // =========================================

                    val inventorySerialExists =
                        database
                            .inventoryDao()
                            .serialNumberExists(
                                serialNumber =
                                    serialNumber
                            )


                    require(
                        !inventorySerialExists
                    ) {
                        "Serial Number $serialNumber already exists in Inventory."
                    }


                    // =========================================
                    // EXPIRY
                    // =========================================

                    val finalExpiryDate =
                        savedLens
                            .expiryDate
                            .trim()
                            .ifBlank {

                                savedItem
                                    .expiryDate
                                    .trim()
                            }


                    // =========================================
                    // CREATE INVENTORY UNIT
                    // =========================================

                    val inventoryUnitId =
                        database
                            .inventoryDao()
                            .insertInventoryUnit(

                                InventoryUnitEntity(

                                    productId =
                                        savedItem.productId,

                                    power =
                                        savedItem
                                            .power
                                            .trim(),

                                    serialNumber =
                                        serialNumber,

                                    batchNumber =
                                        savedItem
                                            .batchNumber
                                            .trim(),

                                    expiryDate =
                                        finalExpiryDate,

                                    receivedDate =
                                        purchase
                                            .receivedDate
                                            .trim(),

                                    supplierName =
                                        purchase
                                        .supplierName
                                        .trim(),

                                    purchaseInvoiceNumber =
                                        purchase
                                        .invoiceNumber
                                        .trim(),

                                    purchaseId = purchaseId,
                                    purchaseItemId = savedItem.id,

                                    status =
                                        "IN_STOCK"
                                )
                            )


                    require(
                        inventoryUnitId > 0L
                    ) {
                        "Inventory unit could not be created for Serial Number $serialNumber."
                    }


                    // =========================================
                    // CREATE PURCHASE RECEIVED MOVEMENT
                    // =========================================

                    database
                        .stockMovementDao()
                        .insertMovement(

                            StockMovementEntity(

                                inventoryUnitId =
                                    inventoryUnitId,

                                serialNumber =
                                    serialNumber,

                                movementType =
                                    "PURCHASE_RECEIVED",

                                fromStatus =
                                    "",

                                toStatus =
                                    "IN_STOCK",

                                partyName =
                                    purchase
                                        .supplierName
                                        .trim(),

                                referenceNumber =
                                    purchase
                                        .invoiceNumber
                                        .trim(),

                                movementDate =
                                    purchase
                                        .receivedDate
                                        .trim(),

                                remarks =
                                    "Stock received through Purchase Invoice ${purchase.invoiceNumber.trim()}"
                            )
                        )
                }
            }


            purchaseId
        }
    }


    // =========================================================
    // UPDATE COMPLETE EXISTING PURCHASE
    // =========================================================
    //
    // ViLYNC SAFE PURCHASE INVENTORY RECONCILIATION
    //
    // Existing serial:
    //     Inventory ID preserved
    //     Current status preserved
    //
    // Missing / newly added serial:
    //     Inventory Unit created
    //     PURCHASE_RECEIVED movement created
    //
    // Removed serial:
    //     Allowed only when still IN_STOCK and there are
    //     no downstream stock movements.
    //
    // Entire process runs inside one Room transaction.
    // =========================================================

    suspend fun updateCompletePurchase(
        purchase: PurchaseEntity,
        itemsWithLenses:
        List<Pair<PurchaseItemEntity, List<PurchaseLensEntity>>>
    ) {

        require(
            purchase.id > 0L
        ) {
            "Existing Purchase ID is required for update."
        }


        database.withTransaction {

            val inventoryDao =
                database.inventoryDao()

            val stockMovementDao =
                database.stockMovementDao()


            // =================================================
            // STEP 1
            // READ OLD PURCHASE SERIALS BEFORE UPDATE
            // =================================================

            val oldPurchaseItems =
                purchaseDao
                    .getPurchaseItems(
                        purchaseId =
                            purchase.id
                    )
                    .first()


            val oldSerialNumbers =
                mutableSetOf<String>()


            oldPurchaseItems.forEach { oldItem ->

                val oldLenses =
                    purchaseDao
                        .getPurchaseLenses(
                            purchaseItemId =
                                oldItem.id
                        )
                        .first()


                oldLenses.forEach { lens ->

                    val serial =
                        lens
                            .serialNumber
                            .trim()

                    if (serial.isNotBlank()) {

                        oldSerialNumbers.add(
                            serial.uppercase()
                        )
                    }
                }
            }


            // =================================================
            // STEP 2
            // COLLECT NEW SERIALS FROM EDITED PURCHASE
            // =================================================

            val newSerialNumbers =
                itemsWithLenses
                    .flatMap {
                        it.second
                    }
                    .map {
                        it.serialNumber
                            .trim()
                            .uppercase()
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .toSet()


            // =================================================
            // STEP 3
            // FIND REMOVED SERIALS
            // =================================================

            val removedSerialNumbers =
                oldSerialNumbers -
                        newSerialNumbers


            // =================================================
            // STEP 4
            // SAFETY CHECK BEFORE REMOVAL
            // =================================================

            removedSerialNumbers.forEach { serial ->

                val existingUnit =
                    inventoryDao
                        .getBySerialNumber(
                            serialNumber =
                                serial
                        )


                if (existingUnit != null) {

                    val downstreamMovementCount =
                        stockMovementDao
                            .countDownstreamMovements(
                                inventoryUnitId =
                                    existingUnit.id
                            )


                    require(
                        downstreamMovementCount == 0
                    ) {
                        "Serial Number $serial cannot be removed because it already has downstream stock movement."
                    }


                    require(
                        existingUnit
                            .status
                            .trim()
                            .equals(
                                other = "IN_STOCK",
                                ignoreCase = true
                            )
                    ) {
                        "Serial Number $serial cannot be removed because its current status is ${existingUnit.status}."
                    }
                }
            }


            // =================================================
            // STEP 5
            // UPDATE PURCHASE + REPLACE ITEMS / LENSES
            // =================================================

            purchaseDao.updateCompletePurchase(
                purchase = purchase,
                itemsWithLenses =
                    itemsWithLenses
            )


            // =================================================
            // STEP 6
            // REMOVE SAFE OLD INVENTORY UNITS
            // =================================================

            removedSerialNumbers.forEach { serial ->

                val existingUnit =
                    inventoryDao
                        .getBySerialNumber(
                            serialNumber =
                                serial
                        )


                if (existingUnit != null) {

                    stockMovementDao
                        .deletePurchaseReceivedMovement(
                            inventoryUnitId =
                                existingUnit.id
                        )


                    inventoryDao
                        .deleteInventoryUnit(
                            unitId =
                                existingUnit.id
                        )
                }
            }


            // =================================================
            // STEP 7
            // READ NEW SAVED ITEMS
            // =================================================

            val savedItems =
                purchaseDao
                    .getPurchaseItems(
                        purchaseId =
                            purchase.id
                    )
                    .first()


            // =================================================
            // STEP 8
            // RECONCILE LIVE SERIAL INVENTORY
            // =================================================

            savedItems.forEach { savedItem ->

                val savedLenses =
                    purchaseDao
                        .getPurchaseLenses(
                            purchaseItemId =
                                savedItem.id
                        )
                        .first()


                savedLenses.forEach { savedLens ->

                    val serialNumber =
                        savedLens
                            .serialNumber
                            .trim()


                    require(
                        serialNumber.isNotBlank()
                    ) {
                        "Serial number cannot be blank for serial-tracked inventory."
                    }


                    // =========================================
                    // FINAL EXPIRY
                    // =========================================

                    val finalExpiryDate =
                        savedLens
                            .expiryDate
                            .trim()
                            .ifBlank {

                                savedItem
                                    .expiryDate
                                    .trim()
                            }


                    // =========================================
                    // FIND EXISTING INVENTORY UNIT
                    // =========================================

                    val existingUnit =
                        inventoryDao
                            .getBySerialNumber(
                                serialNumber =
                                    serialNumber
                            )


                    if (existingUnit != null) {

                        // =====================================
                        // EXISTING SERIAL
                        //
                        // Preserve:
                        // - Inventory Unit ID
                        // - Current Inventory Status
                        //
                        // Update only Purchase-origin metadata.
                        // =====================================

                        inventoryDao
                            .updateInventoryUnit(

                                existingUnit.copy(

                                    productId =
                                        savedItem.productId,

                                    power =
                                        savedItem
                                            .power
                                            .trim(),

                                    serialNumber =
                                        serialNumber,

                                    batchNumber =
                                        savedItem
                                            .batchNumber
                                            .trim(),

                                    expiryDate =
                                        finalExpiryDate,

                                    receivedDate =
                                        purchase
                                            .receivedDate
                                            .trim(),

                                    supplierName =
                                        purchase
                                            .supplierName
                                            .trim(),

                                    purchaseInvoiceNumber =
                                        purchase
                                            .invoiceNumber
                                            .trim()
                                )
                            )

                    } else {

                        // =====================================
                        // NEW OR HISTORICALLY MISSING SERIAL
                        // =====================================

                        val inventoryUnitId =
                            inventoryDao
                                .insertInventoryUnit(

                                    InventoryUnitEntity(

                                        productId =
                                            savedItem.productId,

                                        power =
                                            savedItem
                                                .power
                                                .trim(),

                                        serialNumber =
                                            serialNumber,

                                        batchNumber =
                                            savedItem
                                                .batchNumber
                                                .trim(),

                                        expiryDate =
                                            finalExpiryDate,

                                        receivedDate =
                                            purchase
                                                .receivedDate
                                                .trim(),

                                        supplierName =
                                            purchase
                                                .supplierName
                                                .trim(),

                                        purchaseInvoiceNumber =
                                            purchase
                                                .invoiceNumber
                                                .trim(),

                                        status =
                                            "IN_STOCK"
                                    )
                                )


                        require(
                            inventoryUnitId > 0L
                        ) {
                            "Inventory unit could not be created for Serial Number $serialNumber."
                        }


                        // =====================================
                        // CREATE PURCHASE RECEIVED MOVEMENT
                        // =====================================

                        stockMovementDao
                            .insertMovement(

                                StockMovementEntity(

                                    inventoryUnitId =
                                        inventoryUnitId,

                                    serialNumber =
                                        serialNumber,

                                    movementType =
                                        "PURCHASE_RECEIVED",

                                    fromStatus =
                                        "",

                                    toStatus =
                                        "IN_STOCK",

                                    partyName =
                                        purchase
                                            .supplierName
                                            .trim(),

                                    referenceNumber =
                                        purchase
                                            .invoiceNumber
                                            .trim(),

                                    movementDate =
                                        purchase
                                            .receivedDate
                                            .trim(),

                                    remarks =
                                        "Stock received through Purchase Invoice ${purchase.invoiceNumber.trim()}"
                                )
                            )
                    }
                }
            }
        }
    }


    // =========================================================
    // DELETE COMPLETE PURCHASE
    // =========================================================

    /**
     * Physically deletes a complete Purchase after all required
     * downstream dependency checks have already passed.
     *
     * Do not call this directly from UI without dependency
     * validation.
     */
    suspend fun deleteCompletePurchase(
        purchaseId: Long
    ) {

        require(
            purchaseId > 0L
        ) {
            "Valid Purchase ID is required for delete."
        }


        purchaseDao.deleteCompletePurchase(
            purchaseId =
                purchaseId
        )
    }


    // =========================================================
    // PURCHASE REGISTER
    // =========================================================

    fun getAllPurchases():
            Flow<List<PurchaseEntity>> =
        purchaseDao.getAllPurchases()


    // =========================================================
    // PURCHASE REGISTER - FINANCIAL YEAR
    // =========================================================

    fun getPurchasesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<PurchaseEntity>> =
        purchaseDao.getPurchasesByFinancialYear(
            financialYearStart =
                financialYearStart
        )


    // =========================================================
    // PURCHASE BY ID
    // =========================================================

    suspend fun getPurchaseById(
        purchaseId: Long
    ): PurchaseEntity? =
        purchaseDao.getPurchaseById(
            purchaseId
        )


    // =========================================================
    // PURCHASE ITEMS
    // =========================================================

    fun getPurchaseItems(
        purchaseId: Long
    ): Flow<List<PurchaseItemEntity>> =
        purchaseDao.getPurchaseItems(
            purchaseId
        )


    // =========================================================
    // PURCHASE LENSES
    // =========================================================

    fun getPurchaseLenses(
        purchaseItemId: Long
    ): Flow<List<PurchaseLensEntity>> =
        purchaseDao.getPurchaseLenses(
            purchaseItemId
        )


    // =========================================================
    // SEARCH BY INVOICE NUMBER
    // =========================================================

    fun searchByInvoiceNumber(
        query: String
    ): Flow<List<PurchaseEntity>> =
        purchaseDao.searchByInvoiceNumber(
            query
        )


    // =========================================================
    // PURCHASES BY SUPPLIER
    // =========================================================

    fun getPurchasesBySupplier(
        supplierName: String
    ): Flow<List<PurchaseEntity>> =
        purchaseDao.getPurchasesBySupplier(
            supplierName
        )

    suspend fun getTotalPurchaseAmountForSupplier(supplierId: Long): Double =
        purchaseDao.getTotalPurchaseAmountForSupplier(supplierId) ?: 0.0
}