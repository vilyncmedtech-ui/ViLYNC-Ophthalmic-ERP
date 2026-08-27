package com.vilync.ophthalmicerp.feature.inventory.opening.domain

import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import com.vilync.ophthalmicerp.data.entity.OpeningStockItemEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.OpeningStockRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.feature.product.model.TrackingType

class OpeningStockUseCase(
    private val openingStockRepository: OpeningStockRepository,
    private val inventoryRepository: InventoryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productMasterRepository: ProductMasterRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val numberingRepository: DocumentNumberingRepository
) {

    suspend fun saveDraft(
        openingStock: OpeningStockEntity,
        items: List<OpeningStockItemEntity>
    ): Long {
        return openingStockRepository.saveCompleteOpeningStock(
            openingStock.copy(status = "DRAFT"),
            items
        )
    }

    suspend fun postOpeningStock(openingStockId: Long) {
        openingStockRepository.withTransaction {
            val openingStock = openingStockRepository.getOpeningStockById(openingStockId)
                ?: throw IllegalStateException("Opening Stock document not found.")

            if (openingStock.status != "DRAFT") {
                throw IllegalStateException("Only DRAFT documents can be posted. Current status: ${openingStock.status}")
            }

            // Obtain next VMOS sequence only at POST stage.
            val finalEntryNumber = if (openingStock.entryNumber.startsWith("DRAFT-", ignoreCase = true)) {
                numberingRepository.getNextDocumentNumber(
                    DocumentType.OPENING_STOCK,
                    openingStock.financialYearStart
                )
            } else {
                openingStock.entryNumber
            }

            val items = openingStockRepository.getOpeningStockItemsList(openingStockId)

            items.forEach { item ->
                val product = productMasterRepository.getProductById(item.productId)
                    ?: throw IllegalStateException("Product ${item.productName} not found.")

                val trackingMode = when {
                    product.serialNumberRequired -> TrackingType.SERIAL
                    product.batchApplicable -> TrackingType.BATCH
                    else -> TrackingType.QUANTITY
                }

                if (trackingMode == TrackingType.SERIAL) {
                    val serialNumber = item.serialNumber
                    
                    if (serialNumber.isBlank()) {
                        throw IllegalStateException("Serial Number (LMDE) is required for product ${item.productName}")
                    }

                    // Strict Active Serial Validation
                    val existingUnit = inventoryRepository.getBySerialNumber(serialNumber)
                    if (existingUnit != null && isStatusActive(existingUnit.status)) {
                        throw IllegalStateException("Serial Number $serialNumber is already active in inventory (Status: ${existingUnit.status})")
                    }

                    val inventoryUnitId = inventoryRepository.insertInventoryUnit(
                        InventoryUnitEntity(
                            productId = item.productId,
                            power = item.power,
                            serialNumber = serialNumber,
                            batchNumber = item.batchNumber, 
                            expiryDate = item.expiryDate,
                            receivedDate = openingStock.entryDate,
                            supplierName = "OPENING STOCK",
                            purchaseInvoiceNumber = finalEntryNumber,
                            status = "IN_STOCK"
                        )
                    )

                    stockMovementRepository.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = inventoryUnitId,
                            serialNumber = serialNumber,
                            movementType = "OPENING_STOCK",
                            fromStatus = "",
                            toStatus = "IN_STOCK",
                            partyName = "INTERNAL",
                            referenceNumber = finalEntryNumber,
                            movementDate = openingStock.entryDate,
                            remarks = "Opening stock initialization"
                        )
                    )
                }
            }

            // Update status to POSTED and assign final VMOS number.
            openingStockRepository.updateOpeningStock(
                openingStock.copy(
                    entryNumber = finalEntryNumber,
                    normalizedEntryNumber = finalEntryNumber.trim().uppercase(),
                    status = "POSTED",
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Audit Trail
            auditTrailRepository.recordEvent(
                module = "INVENTORY",
                action = "POST_OPENING_STOCK",
                recordId = openingStockId,
                referenceNumber = finalEntryNumber,
                description = "Opening Stock $finalEntryNumber posted."
            )
        }
    }

    suspend fun cancelOpeningStock(openingStockId: Long, reason: String) {
        openingStockRepository.withTransaction {
            val openingStock = openingStockRepository.getOpeningStockById(openingStockId)
                ?: throw IllegalStateException("Opening Stock document not found.")

            if (openingStock.status != "POSTED") {
                throw IllegalStateException("Only POSTED documents can be cancelled.")
            }

            val items = openingStockRepository.getOpeningStockItemsList(openingStockId)

            items.forEach { item ->
                val product = productMasterRepository.getProductById(item.productId)
                    ?: return@forEach

                val trackingMode = when {
                    product.serialNumberRequired -> TrackingType.SERIAL
                    product.batchApplicable -> TrackingType.BATCH
                    else -> TrackingType.QUANTITY
                }

                if (trackingMode == TrackingType.SERIAL) {
                    val serialToLookup = if (item.serialNumber.isNotBlank()) item.serialNumber else item.batchNumber
                    val unit = inventoryRepository.getBySerialNumber(serialToLookup)
                    
                    if (unit == null) {
                        throw IllegalStateException("Physical inventory unit for Serial Number $serialToLookup could not be found. Reversal failed.")
                    }

                    val movementCount = stockMovementRepository.countDownstreamMovements(unit.id)
                    if (movementCount > 0) {
                        throw IllegalStateException("Serial Number $serialToLookup cannot be cancelled because it has downstream stock movements ($movementCount).")
                    }

                    // Reversal Entry Pattern
                    inventoryRepository.updateStatus(unit.id, "CANCELLED_FROM_OPENING")
                    
                    stockMovementRepository.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = unit.id,
                            serialNumber = serialToLookup,
                            movementType = "OPENING_STOCK_CANCELLED",
                            fromStatus = "IN_STOCK",
                            toStatus = "CANCELLED_FROM_OPENING",
                            partyName = "INTERNAL",
                            referenceNumber = openingStock.entryNumber,
                            movementDate = openingStock.entryDate,
                            remarks = "Opening stock cancelled: $reason"
                        )
                    )
                }
            }

            // Actually cancel the document
            openingStockRepository.cancelOpeningStock(openingStockId, System.currentTimeMillis())

            auditTrailRepository.recordEvent(
                module = "INVENTORY",
                action = "CANCEL_OPENING_STOCK",
                recordId = openingStockId,
                referenceNumber = openingStock.entryNumber,
                description = "Opening Stock ${openingStock.entryNumber} cancelled. Reason: $reason"
            )
        }
    }

    /**
     * Identifies and reverses physical inventory for already-CANCELLED Opening Stock 
     * documents that failed to reverse their stock due to the previous 'Flow' bug.
     */
    suspend fun reconcileGhostStock() {
        openingStockRepository.withTransaction {
            val cancelledStocks = openingStockRepository.getCancelledOpeningStocks()
            
            cancelledStocks.forEach { stock ->
                val items = openingStockRepository.getOpeningStockItemsList(stock.id)
                
                items.forEach { item ->
                    val serialToLookup = if (item.serialNumber.isNotBlank()) item.serialNumber else item.batchNumber
                    if (serialToLookup.isNotBlank()) {
                        val unit = inventoryRepository.getBySerialNumber(serialToLookup)
                        
                        if (unit != null && unit.status == "IN_STOCK") {
                            val movementCount = stockMovementRepository.countDownstreamMovements(unit.id)
                            
                            // Reconcile ONLY if no downstream activity exists.
                            if (movementCount == 0) {
                                inventoryRepository.updateStatus(unit.id, "CANCELLED_FROM_OPENING")
                                
                                stockMovementRepository.insertMovement(
                                    StockMovementEntity(
                                        inventoryUnitId = unit.id,
                                        serialNumber = serialToLookup,
                                        movementType = "OPENING_STOCK_RECONCILED",
                                        fromStatus = "IN_STOCK",
                                        toStatus = "CANCELLED_FROM_OPENING",
                                        partyName = "SYSTEM",
                                        referenceNumber = stock.entryNumber,
                                        movementDate = stock.entryDate,
                                        remarks = "Automatic ghost stock reconciliation"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isStatusActive(status: String): Boolean {
        val s = status.uppercase()
        return s == "IN_STOCK" || s == "ON_CHALLAN" || s == "SOLD" || s == "SAMPLE_ISSUED"
    }
}
