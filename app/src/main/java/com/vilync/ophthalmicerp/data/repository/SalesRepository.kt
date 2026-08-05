package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.dao.SalesDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SaleItemEntity
import com.vilync.ophthalmicerp.data.entity.SaleLensEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


class SalesRepository(

    private val salesDao: SalesDao,

    private val database: AppDatabase,

    private val auditTrailRepository: AuditTrailRepository,

    private val numberingRepository: DocumentNumberingRepository? = null

) {


    // =========================================================
    // DUPLICATE INVOICE CHECK - NEW SALE
    // =========================================================

    suspend fun saleInvoiceExists(
        customerId: Long,
        invoiceNumber: String
    ): Boolean {

        val normalizedInvoiceNumber =
            invoiceNumber.trim()

        if (
            customerId <= 0L ||
            normalizedInvoiceNumber.isBlank()
        ) {
            return false
        }

        return salesDao.saleInvoiceExists(
            customerId = customerId,
            invoiceNumber = normalizedInvoiceNumber
        )
    }


    // =========================================================
    // DUPLICATE INVOICE CHECK - EDIT SALE
    // =========================================================

    suspend fun saleInvoiceExistsExcludingSale(
        customerId: Long,
        invoiceNumber: String,
        excludeSaleId: Long
    ): Boolean {

        val normalizedInvoiceNumber =
            invoiceNumber.trim()

        if (
            customerId <= 0L ||
            normalizedInvoiceNumber.isBlank() ||
            excludeSaleId <= 0L
        ) {
            return false
        }

        return salesDao
            .saleInvoiceExistsExcludingSale(
                customerId = customerId,
                invoiceNumber = normalizedInvoiceNumber,
                excludeSaleId = excludeSaleId
            )
    }


    // =========================================================
    // SAVE COMPLETE NEW SALE
    // =========================================================
    //
    // ViLYNC SAFE SALES INVENTORY TRANSACTION
    //
    // 1. Validate Sale header.
    // 2. Validate every selected physical Inventory Unit.
    // 3. Every selected serial must still be IN_STOCK.
    // 4. Product / Power must match the Sale Item.
    // 5. Same physical Inventory Unit cannot appear twice.
    // 6. Save Sale + Items + Lenses.
    // 7. Change selected Inventory Units to SOLD.
    // 8. Create permanent SOLD Stock Movement entries.
    //
    // Entire operation runs inside one Room transaction.
    //
    // If any validation / insert / stock update fails,
    // the complete transaction is rolled back.
    // =========================================================

    suspend fun saveCompleteSale(
        sale: SaleEntity,
        itemsWithLenses:
        List<Pair<SaleItemEntity, List<SaleLensEntity>>>
    ): Long {

        // =====================================================
        // BASIC DOCUMENT VALIDATION
        // =====================================================

        require(
            sale.id == 0L
        ) {
            "New Sale must not already have a database ID."
        }

        require(
            sale.customerId > 0L
        ) {
            "Customer / Hospital is required."
        }

        require(
            sale.customerName.trim().isNotBlank()
        ) {
            "Customer / Hospital name is required."
        }

        require(
            sale.invoiceNumber.trim().isNotBlank() || numberingRepository != null
        ) {
            "Sales Invoice Number is required."
        }

        require(
            sale.invoiceDate.trim().isNotBlank()
        ) {
            "Sales Invoice Date is required."
        }

        require(
            sale.financialYearStart > 0
        ) {
            "Valid Financial Year is required."
        }

        require(
            itemsWithLenses.isNotEmpty()
        ) {
            "At least one Sales item is required."
        }


        return database.withTransaction {

            val inventoryDao =
                database.inventoryDao()

            val stockMovementDao =
                database.stockMovementDao()


            // =================================================
            // STEP 1
            // FINAL DUPLICATE INVOICE CHECK
            // =================================================
            //
            // UI/ViewModel may already perform a live duplicate
            // check, but repository validates again immediately
            // before persistence.
            // =================================================

            val duplicateInvoiceExists =
                salesDao.saleInvoiceExists(
                    customerId =
                        sale.customerId,

                    invoiceNumber =
                        sale.invoiceNumber.trim()
                )


            require(
                !duplicateInvoiceExists
            ) {
                "Sales Invoice Number ${sale.invoiceNumber.trim()} already exists for ${sale.customerName.trim()}."
            }


            // =================================================
            // STEP 2
            // VALIDATE ITEM STRUCTURE
            // =================================================

            itemsWithLenses.forEachIndexed {
                    itemIndex,
                    itemWithLenses ->

                val item =
                    itemWithLenses.first

                require(
                    item.productId > 0L
                ) {
                    "Valid Product is required for Sales item ${itemIndex + 1}."
                }

                require(
                    item.productName.trim().isNotBlank()
                ) {
                    "Product name is required for Sales item ${itemIndex + 1}."
                }

                require(
                    item.quantity > 0
                ) {
                    "Quantity must be greater than zero for ${item.productName.trim()}."
                }

                require(
                    item.rate >= 0.0
                ) {
                    "Rate cannot be negative for ${item.productName.trim()}."
                }

                require(
                    item.discountPercent >= 0.0
                ) {
                    "Discount cannot be negative for ${item.productName.trim()}."
                }

                require(
                    item.gstPercent >= 0.0
                ) {
                    "GST cannot be negative for ${item.productName.trim()}."
                }
            }


            // =================================================
            // STEP 3
            // COLLECT + VALIDATE SELECTED INVENTORY UNIT IDS
            // =================================================

            val selectedInventoryUnitIds =
                itemsWithLenses
                    .flatMap {
                        it.second
                    }
                    .map {
                        it.inventoryUnitId
                    }


            require(
                selectedInventoryUnitIds.none {
                    it <= 0L
                }
            ) {
                "Every selected serial must have a valid Inventory Unit."
            }


            require(
                selectedInventoryUnitIds.size ==
                        selectedInventoryUnitIds
                            .distinct()
                            .size
            ) {
                "The same physical Serial Number cannot be selected more than once in one Sales Invoice."
            }


            // =================================================
            // STEP 4
            // VALIDATE EVERY PHYSICAL SERIAL AGAINST LIVE STOCK
            // =================================================
            //
            // Never trust UI state alone.
            //
            // InventoryUnit is the authoritative physical stock
            // identity.
            // =================================================

            itemsWithLenses.forEach { itemWithLenses ->

                val saleItem =
                    itemWithLenses.first

                val saleLenses =
                    itemWithLenses.second


                saleLenses.forEach { saleLens ->

                    val inventoryUnit =
                        inventoryDao.getById(
                            unitId =
                                saleLens.inventoryUnitId
                        )


                    require(
                        inventoryUnit != null
                    ) {
                        "Selected Inventory Unit ${saleLens.inventoryUnitId} does not exist."
                    }


                    // =========================================
                    // CURRENT STOCK STATUS
                    // =========================================

                    require(
                        inventoryUnit.status
                            .trim()
                            .equals(
                                other = "IN_STOCK",
                                ignoreCase = true
                            )
                    ) {
                        "Serial Number ${inventoryUnit.serialNumber} is no longer available in stock. Current status: ${inventoryUnit.status}."
                    }


                    // =========================================
                    // PRODUCT SAFETY
                    // =========================================

                    require(
                        inventoryUnit.productId ==
                                saleItem.productId
                    ) {
                        "Serial Number ${inventoryUnit.serialNumber} does not belong to Product ${saleItem.productName.trim()}."
                    }


                    // =========================================
                    // SERIAL SNAPSHOT SAFETY
                    // =========================================
                    //
                    // SaleLens serial may come from UI selection,
                    // but Inventory remains authoritative.
                    // =========================================

                    val requestedSerial =
                        saleLens
                            .serialNumber
                            .trim()


                    if (requestedSerial.isNotBlank()) {

                        require(
                            inventoryUnit
                                .serialNumber
                                .trim()
                                .equals(
                                    other =
                                        requestedSerial,

                                    ignoreCase =
                                        true
                                )
                        ) {
                            "Selected Serial Number does not match its Inventory Unit."
                        }
                    }


                    // =========================================
                    // POWER SAFETY
                    // =========================================
                    //
                    // Enforce power only when the Sales Item
                    // itself carries a power value.
                    // =========================================

                    val itemPower =
                        saleItem
                            .power
                            .trim()


                    if (itemPower.isNotBlank()) {

                        require(
                            inventoryUnit
                                .power
                                .trim()
                                .equals(
                                    other =
                                        itemPower,

                                    ignoreCase =
                                        true
                                )
                        ) {
                            "Serial Number ${inventoryUnit.serialNumber} belongs to Power ${inventoryUnit.power}, not $itemPower."
                        }
                    }
                }
            }


            // =================================================
            // STEP 5
            // SAVE SALES DOCUMENT
            // =================================================

            val finalInvoiceNumber =
                numberingRepository?.getNextDocumentNumber(
                    DocumentType.INVOICE,
                    sale.financialYearStart
                ) ?: sale.invoiceNumber.trim()

            val saleId =
                salesDao.saveCompleteSaleDocument(
                    sale =
                        sale.copy(
                            id = 0L,

                            customerName =
                                sale.customerName.trim(),

                            invoiceNumber =
                                finalInvoiceNumber,

                            normalizedInvoiceNumber =
                                finalInvoiceNumber
                                    .uppercase(),

                            invoiceDate =
                                sale.invoiceDate.trim(),

                            remarks =
                                sale.remarks.trim(),

                            status =
                                "POSTED",

                            cancelledAt =
                                null,

                            cancellationReason =
                                ""
                        ),

                    itemsWithLenses =
                        itemsWithLenses.map {
                                itemWithLenses ->

                            val item =
                                itemWithLenses.first

                            val lenses =
                                itemWithLenses.second


                            item.copy(
                                id = 0L,
                                saleId = 0L,
                                productName =
                                    item.productName.trim(),
                                power =
                                    item.power.trim(),
                                batchNumber =
                                    item.batchNumber.trim(),
                                lotNumber =
                                    item.lotNumber.trim()
                            ) to
                                    lenses.map { lens ->
                                        // -----------------------------------------------------
                                        // RESOLVE HISTORICAL COST SNAPSHOT
                                        // -----------------------------------------------------
                                        val unit = inventoryDao.getById(lens.inventoryUnitId)
                                        var cost = 0.0
                                        var gst = 0.0
                                        var pInvoiceId: Long? = null
                                        var pItemId: Long? = null
                                        var pDate = ""
                                        var pInvNo = ""
                                        var source = "UNKNOWN"

                                        if (unit != null) {
                                            pInvNo = unit.purchaseInvoiceNumber
                                            val purchaseItem = if (unit.purchaseItemId != null) {
                                                database.purchaseDao().getPurchaseItems(unit.purchaseId ?: 0L).first().find { it.id == unit.purchaseItemId }
                                            } else if (unit.purchaseInvoiceNumber.isNotBlank()) {
                                                val purchase = database.purchaseDao().searchByInvoiceNumber(unit.purchaseInvoiceNumber).first().firstOrNull()
                                                if (purchase != null) {
                                                    database.purchaseDao().getPurchaseItems(purchase.id).first().find { 
                                                        it.productId == item.productId && it.power == item.power 
                                                    }
                                                } else null
                                            } else null

                                            if (purchaseItem != null) {
                                                cost = purchaseItem.purchaseRate
                                                gst = purchaseItem.gstAmount / purchaseItem.quantity.toDouble()
                                                pItemId = purchaseItem.id
                                                pInvoiceId = purchaseItem.purchaseId
                                                source = "SNAPSHOT"
                                                
                                                val purchase = database.purchaseDao().getPurchaseById(purchaseItem.purchaseId)
                                                pDate = purchase?.receivedDate ?: ""
                                            } else {
                                                // Fallback to Master if trace fails
                                                val product = database.productDao().getProductById(item.productId)
                                                cost = product?.purchasePrice ?: 0.0
                                                gst = product?.purchaseGstAmount ?: 0.0
                                                source = "MASTER_FALLBACK"
                                            }
                                        }

                                        lens.copy(
                                            id = 0L,
                                            saleItemId = 0L,
                                            serialNumber =
                                                lens.serialNumber.trim(),
                                            power =
                                                lens.power.trim(),
                                            batchNumber =
                                                lens.batchNumber.trim(),
                                            expiryDate =
                                                lens.expiryDate.trim(),
                                            purchasePriceSnapshot = cost,
                                            purchaseGstAmountSnapshot = gst,
                                            purchaseInvoiceId = pInvoiceId,
                                            purchaseInvoiceNumber = pInvNo,
                                            purchaseItemId = pItemId,
                                            purchaseDate = pDate,
                                            costResolutionSource = source
                                        )
                                    }
                        }
                )


            require(
                saleId > 0L
            ) {
                "Sales Invoice could not be saved."
            }


            // =================================================
            // STEP 6
            // MARK PHYSICAL INVENTORY AS SOLD
            // =================================================

            itemsWithLenses.forEach { itemWithLenses ->

                val saleLenses =
                    itemWithLenses.second


                saleLenses.forEach { saleLens ->

                    val inventoryUnit =
                        inventoryDao.getById(
                            unitId =
                                saleLens.inventoryUnitId
                        )


                    require(
                        inventoryUnit != null
                    ) {
                        "Inventory Unit ${saleLens.inventoryUnitId} could not be found during Sales posting."
                    }


                    // =========================================
                    // RECHECK BEFORE MUTATION
                    // =========================================

                    require(
                        inventoryUnit.status
                            .trim()
                            .equals(
                                other = "IN_STOCK",
                                ignoreCase = true
                            )
                    ) {
                        "Serial Number ${inventoryUnit.serialNumber} is not available for Sale."
                    }


                    inventoryDao.updateStatus(
                        unitId =
                            inventoryUnit.id,

                        newStatus =
                            "SOLD"
                    )


                    // =========================================
                    // PERMANENT SOLD MOVEMENT
                    // =========================================

                    stockMovementDao.insertMovement(

                        StockMovementEntity(

                            inventoryUnitId =
                                inventoryUnit.id,

                            serialNumber =
                                inventoryUnit
                                    .serialNumber
                                    .trim(),

                            movementType =
                                "SOLD",

                            fromStatus =
                                "IN_STOCK",

                            toStatus =
                                "SOLD",

                            partyName =
                                sale.customerName
                                    .trim(),

                            referenceNumber =
                                sale.invoiceNumber
                                    .trim(),

                            movementDate =
                                sale.invoiceDate
                                    .trim(),

                            remarks =
                                "Stock sold through Sales Invoice ${sale.invoiceNumber.trim()}"
                        )
                    )
                }
            }


            // =================================================
            // STEP 7
            // COMPLETE
            // =================================================

            saleId
        }
    }



    // =========================================================
    // UPDATE COMPLETE POSTED SALE
    // =========================================================
    //
    // Inventory-safe edit transaction:
    //
    // - unchanged physical serials remain SOLD
    // - removed serials: SOLD -> IN_STOCK + SALE_EDIT_RETURN
    // - newly added serials: IN_STOCK -> SOLD + SALE_EDIT_SOLD
    // - header/items/lenses are replaced atomically
    // - original Sale ID and createdAt are preserved
    // - cancelled invoices cannot be edited
    // =========================================================

    suspend fun updateCompleteSale(
        saleId: Long,
        sale: SaleEntity,
        itemsWithLenses:
        List<Pair<SaleItemEntity, List<SaleLensEntity>>>
    ): Long {

        require(saleId > 0L) {
            "Valid Sales Invoice is required for editing."
        }

        require(sale.customerId > 0L) {
            "Customer / Hospital is required."
        }

        require(sale.customerName.trim().isNotBlank()) {
            "Customer / Hospital name is required."
        }

        require(sale.invoiceNumber.trim().isNotBlank()) {
            "Sales Invoice Number is required."
        }

        require(sale.invoiceDate.trim().isNotBlank()) {
            "Sales Invoice Date is required."
        }

        require(sale.financialYearStart > 0) {
            "Valid Financial Year is required."
        }

        require(itemsWithLenses.isNotEmpty()) {
            "At least one Sales item is required."
        }

        return database.withTransaction {

            val inventoryDao = database.inventoryDao()
            val stockMovementDao = database.stockMovementDao()

            val existingSale =
                requireNotNull(
                    salesDao.getSaleById(saleId)
                ) {
                    "Sales Invoice could not be found."
                }

            require(
                existingSale.status.trim().equals(
                    other = "POSTED",
                    ignoreCase = true
                )
            ) {
                "Only a POSTED Sales Invoice can be edited. Current status: ${existingSale.status}."
            }

            val duplicateInvoiceExists =
                salesDao.saleInvoiceExistsExcludingSale(
                    customerId = sale.customerId,
                    invoiceNumber = sale.invoiceNumber.trim(),
                    excludeSaleId = saleId
                )

            require(!duplicateInvoiceExists) {
                "Sales Invoice Number ${sale.invoiceNumber.trim()} already exists for ${sale.customerName.trim()}."
            }

            itemsWithLenses.forEachIndexed { itemIndex, pair ->
                val item = pair.first

                require(item.productId > 0L) {
                    "Valid Product is required for Sales item ${itemIndex + 1}."
                }

                require(item.productName.trim().isNotBlank()) {
                    "Product name is required for Sales item ${itemIndex + 1}."
                }

                require(item.quantity > 0) {
                    "Quantity must be greater than zero for ${item.productName.trim()}."
                }

                require(item.quantity == pair.second.size) {
                    "Physical Serial quantity does not match item quantity for ${item.productName.trim()}."
                }

                require(item.rate >= 0.0) {
                    "Rate cannot be negative for ${item.productName.trim()}."
                }

                require(item.discountPercent in 0.0..100.0) {
                    "Discount must be between 0 and 100 for ${item.productName.trim()}."
                }

                require(item.gstPercent in 0.0..100.0) {
                    "GST must be between 0 and 100 for ${item.productName.trim()}."
                }
            }

            val existingItems =
                salesDao.getSaleItemsList(saleId)

            val existingLenses =
                existingItems.flatMap { item ->
                    salesDao.getSaleLensesList(item.id)
                }

            val oldIds =
                existingLenses
                    .map { it.inventoryUnitId }
                    .toSet()

            val newLenses =
                itemsWithLenses.flatMap { it.second }

            val newIds =
                newLenses
                    .map { it.inventoryUnitId }

            require(newIds.none { it <= 0L }) {
                "Every selected serial must have a valid Inventory Unit."
            }

            require(newIds.size == newIds.distinct().size) {
                "The same physical Serial Number cannot be selected more than once in one Sales Invoice."
            }

            val newIdsSet = newIds.toSet()
            val removedIds = oldIds - newIdsSet
            val addedIds = newIdsSet - oldIds
            val unchangedIds = oldIds intersect newIdsSet

            // Validate every new document lens against authoritative inventory.
            itemsWithLenses.forEach { pair ->
                val saleItem = pair.first

                pair.second.forEach { saleLens ->
                    val unit =
                        requireNotNull(
                            inventoryDao.getById(saleLens.inventoryUnitId)
                        ) {
                            "Selected Inventory Unit ${saleLens.inventoryUnitId} does not exist."
                        }

                    require(unit.productId == saleItem.productId) {
                        "Serial Number ${unit.serialNumber} does not belong to Product ${saleItem.productName.trim()}."
                    }

                    if (saleLens.serialNumber.trim().isNotBlank()) {
                        require(
                            unit.serialNumber.trim().equals(
                                saleLens.serialNumber.trim(),
                                ignoreCase = true
                            )
                        ) {
                            "Selected Serial Number does not match its Inventory Unit."
                        }
                    }

                    if (saleItem.power.trim().isNotBlank()) {
                        require(
                            unit.power.trim().equals(
                                saleItem.power.trim(),
                                ignoreCase = true
                            )
                        ) {
                            "Serial Number ${unit.serialNumber} belongs to Power ${unit.power}, not ${saleItem.power.trim()}."
                        }
                    }

                    when (unit.id) {
                        in unchangedIds -> require(
                            unit.status.trim().equals("SOLD", ignoreCase = true)
                        ) {
                            "Existing Serial Number ${unit.serialNumber} is no longer in SOLD status and cannot be edited safely."
                        }

                        in addedIds -> require(
                            unit.status.trim().equals("IN_STOCK", ignoreCase = true)
                        ) {
                            "New Serial Number ${unit.serialNumber} is not available in stock. Current status: ${unit.status}."
                        }
                    }
                }
            }

            // Validate removed serials before any mutation.
            removedIds.forEach { inventoryUnitId ->
                val unit =
                    requireNotNull(
                        inventoryDao.getById(inventoryUnitId)
                    ) {
                        "Existing Inventory Unit $inventoryUnitId could not be found."
                    }

                require(
                    unit.status.trim().equals("SOLD", ignoreCase = true)
                ) {
                    "Serial Number ${unit.serialNumber} cannot be removed from this invoice because its current status is ${unit.status}."
                }
            }

            // Return removed serials to stock.
            removedIds.forEach { inventoryUnitId ->
                val unit = requireNotNull(inventoryDao.getById(inventoryUnitId))

                inventoryDao.updateStatus(
                    unitId = unit.id,
                    newStatus = "IN_STOCK"
                )

                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        inventoryUnitId = unit.id,
                        serialNumber = unit.serialNumber.trim(),
                        movementType = "SALE_EDIT_RETURN",
                        fromStatus = "SOLD",
                        toStatus = "IN_STOCK",
                        partyName = sale.customerName.trim(),
                        referenceNumber = sale.invoiceNumber.trim(),
                        movementDate = sale.invoiceDate.trim(),
                        remarks = "Serial returned to stock while editing Sales Invoice ${sale.invoiceNumber.trim()}"
                    )
                )
            }

            // Sell newly added serials.
            addedIds.forEach { inventoryUnitId ->
                val unit = requireNotNull(inventoryDao.getById(inventoryUnitId))

                require(
                    unit.status.trim().equals("IN_STOCK", ignoreCase = true)
                ) {
                    "Serial Number ${unit.serialNumber} is no longer available for this invoice edit."
                }

                inventoryDao.updateStatus(
                    unitId = unit.id,
                    newStatus = "SOLD"
                )

                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        inventoryUnitId = unit.id,
                        serialNumber = unit.serialNumber.trim(),
                        movementType = "SALE_EDIT_SOLD",
                        fromStatus = "IN_STOCK",
                        toStatus = "SOLD",
                        partyName = sale.customerName.trim(),
                        referenceNumber = sale.invoiceNumber.trim(),
                        movementDate = sale.invoiceDate.trim(),
                        remarks = "Serial sold while editing Sales Invoice ${sale.invoiceNumber.trim()}"
                    )
                )
            }

            salesDao.replaceCompleteSaleDocument(
                sale = sale.copy(
                    id = saleId,
                    customerName = sale.customerName.trim(),
                    invoiceNumber = sale.invoiceNumber.trim(),
                    normalizedInvoiceNumber = sale.invoiceNumber.trim().uppercase(),
                    invoiceDate = sale.invoiceDate.trim(),
                    remarks = sale.remarks.trim(),
                    status = "POSTED",
                    cancelledAt = null,
                    cancellationReason = "",
                    createdAt = existingSale.createdAt,
                    updatedAt = System.currentTimeMillis()
                ),
                itemsWithLenses = itemsWithLenses
            )

            saleId
        }
    }

    // =========================================================
    // CANCEL POSTED SALES INVOICE
    // =========================================================
    //
    // Audit-safe cancellation:
    //
    // 1. Original Sale / Items / Lenses remain in the database.
    // 2. Original SOLD Stock Movement remains permanent.
    // 3. Every physical serial must still be SOLD.
    // 4. Inventory is reversed SOLD -> IN_STOCK.
    // 5. A permanent SALES_CANCELLED_RETURN movement is created.
    // 6. Sale header is marked CANCELLED.
    //
    // If any validation or mutation fails, Room rolls back the
    // complete cancellation transaction.
    // =========================================================

    suspend fun cancelSale(
        saleId: Long,
        cancellationReason: String
    ) {

        require(
            saleId > 0L
        ) {
            "Valid Sales Invoice is required."
        }

        val normalizedReason =
            cancellationReason.trim()

        require(
            normalizedReason.isNotBlank()
        ) {
            "Cancellation reason is required."
        }

        database.withTransaction {

            val sale =
                salesDao.getSaleById(
                    saleId = saleId
                )

            require(
                sale != null
            ) {
                "Sales Invoice could not be found."
            }

            require(
                !sale.status
                    .trim()
                    .equals(
                        other = "CANCELLED",
                        ignoreCase = true
                    )
            ) {
                "Sales Invoice is already cancelled."
            }

            require(
                sale.status
                    .trim()
                    .equals(
                        other = "POSTED",
                        ignoreCase = true
                    )
            ) {
                "Only a POSTED Sales Invoice can be cancelled. Current status: ${sale.status}."
            }

            val inventoryDao =
                database.inventoryDao()

            val stockMovementDao =
                database.stockMovementDao()

            val saleItems =
                salesDao.getSaleItemsList(
                    saleId = sale.id
                )

            val allLenses =
                saleItems.flatMap { item ->
                    salesDao.getSaleLensesList(
                        saleItemId = item.id
                    )
                }

            require(
                allLenses
                    .map { it.inventoryUnitId }
                    .filter { it > 0L }
                    .distinct()
                    .size ==
                        allLenses
                            .map { it.inventoryUnitId }
                            .filter { it > 0L }
                            .size
            ) {
                "The Sales Invoice contains duplicate physical Inventory Units and cannot be cancelled safely."
            }

            // Validate the complete physical stock reversal first.
            allLenses.forEach { lens ->

                require(
                    lens.inventoryUnitId > 0L
                ) {
                    "Serial Number ${lens.serialNumber} has no valid Inventory Unit."
                }

                val inventoryUnit =
                    inventoryDao.getById(
                        unitId = lens.inventoryUnitId
                    )

                require(
                    inventoryUnit != null
                ) {
                    "Inventory Unit ${lens.inventoryUnitId} for Serial Number ${lens.serialNumber} could not be found."
                }

                require(
                    inventoryUnit.status
                        .trim()
                        .equals(
                            other = "SOLD",
                            ignoreCase = true
                        )
                ) {
                    "Serial Number ${inventoryUnit.serialNumber} cannot be reversed because its current status is ${inventoryUnit.status}. It may already have been returned or used in another transaction."
                }

                if (lens.serialNumber.trim().isNotBlank()) {
                    require(
                        inventoryUnit.serialNumber
                            .trim()
                            .equals(
                                other = lens.serialNumber.trim(),
                                ignoreCase = true
                            )
                    ) {
                        "Inventory Serial Number does not match the Sales Invoice snapshot."
                    }
                }
            }

            // Reverse stock and append permanent movement history.
            allLenses.forEach { lens ->

                val inventoryUnit =
                    requireNotNull(
                        inventoryDao.getById(
                            unitId = lens.inventoryUnitId
                        )
                    )

                inventoryDao.updateStatus(
                    unitId = inventoryUnit.id,
                    newStatus = "IN_STOCK"
                )

                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        inventoryUnitId =
                            inventoryUnit.id,

                        serialNumber =
                            inventoryUnit.serialNumber.trim(),

                        movementType =
                            "SALES_CANCELLED_RETURN",

                        fromStatus =
                            "SOLD",

                        toStatus =
                            "IN_STOCK",

                        partyName =
                            sale.customerName.trim(),

                        referenceNumber =
                            sale.invoiceNumber.trim(),

                        movementDate =
                            sale.invoiceDate.trim(),

                        remarks =
                            "Stock returned to inventory because Sales Invoice ${sale.invoiceNumber.trim()} was cancelled. Reason: $normalizedReason"
                    )
                )
            }

            salesDao.markSaleCancelled(
                saleId = sale.id,
                cancelledAt = System.currentTimeMillis(),
                cancellationReason = normalizedReason
            )
        }
    }


    // =========================================================
    // SOFT DELETE DRAFT SALES INVOICE
    // =========================================================

    suspend fun softDeleteSale(
        saleId: Long,
        reason: String? = null
    ) {

        require(saleId > 0L) { "Valid Sales Invoice is required." }

        database.withTransaction {

            val sale = salesDao.getSaleById(saleId)
                ?: throw IllegalStateException("Sales Invoice not found.")

            require(sale.status.trim().equals("DRAFT", ignoreCase = true)) {
                "Only DRAFT invoices can be deleted. Current status: ${sale.status}."
            }

            val updatedSale = sale.copy(
                status = "DELETED",
                updatedAt = System.currentTimeMillis()
            )

            salesDao.updateSale(updatedSale)

            // Audit
            try {
                auditTrailRepository.recordEvent(
                    module = "SALES",
                    action = "SOFT_DELETE",
                    recordId = saleId,
                    referenceNumber = sale.invoiceNumber,
                    description = "Draft Invoice ${sale.invoiceNumber} moved to Deleted. Reason: ${reason ?: "N/A"}",
                    oldValue = "DRAFT",
                    newValue = "DELETED"
                )
            } catch (_: Exception) {}
        }
    }


    // =========================================================
    // SALES REGISTER
    // =========================================================

    fun getAllSales():
            Flow<List<SaleEntity>> =
        salesDao.getAllSales()


    // =========================================================
    // SALES REGISTER - FINANCIAL YEAR
    // =========================================================

    fun getSalesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SaleEntity>> =
        salesDao.getSalesByFinancialYear(
            financialYearStart =
                financialYearStart
        )


    // =========================================================
    // SALE BY ID
    // =========================================================

    suspend fun getSaleById(
        saleId: Long
    ): SaleEntity? =
        salesDao.getSaleById(
            saleId =
                saleId
        )


    // =========================================================
    // SALE ITEMS
    // =========================================================

    fun getSaleItems(
        saleId: Long
    ): Flow<List<SaleItemEntity>> =
        salesDao.getSaleItems(
            saleId =
                saleId
        )


    // =========================================================
    // SALE LENSES
    // =========================================================

    fun getSaleLenses(
        saleItemId: Long
    ): Flow<List<SaleLensEntity>> =
        salesDao.getSaleLenses(
            saleItemId =
                saleItemId
        )



    suspend fun getSaleItemsListForEdit(
        saleId: Long
    ): List<SaleItemEntity> =
        salesDao.getSaleItemsList(
            saleId = saleId
        )

    suspend fun getSaleLensesListForEdit(
        saleItemId: Long
    ): List<SaleLensEntity> =
        salesDao.getSaleLensesList(
            saleItemId = saleItemId
        )


    // =========================================================
    // SEARCH SALES REGISTER
    // =========================================================

    fun searchSales(
        query: String
    ): Flow<List<SaleEntity>> =
        salesDao.searchSales(
            query =
                query.trim()
        )


    // =========================================================
    // SALES BY CUSTOMER / HOSPITAL
    // =========================================================

    fun getSalesByCustomer(
        customerId: Long
    ): Flow<List<SaleEntity>> =
        salesDao.getSalesByCustomer(
            customerId =
                customerId
        )

    suspend fun getTotalSaleAmountForCustomer(customerId: Long): Double =
        salesDao.getTotalSaleAmountForCustomer(customerId) ?: 0.0
}