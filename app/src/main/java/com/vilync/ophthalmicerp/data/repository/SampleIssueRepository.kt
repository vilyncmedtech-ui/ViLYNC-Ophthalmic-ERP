package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.dao.SampleIssueDao
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

class SampleIssueRepository(
    private val sampleIssueDao: SampleIssueDao,
    private val database: AppDatabase,
    private val numberingRepository: DocumentNumberingRepository
) {

    // =========================================================
    // SAVE COMPLETE SAMPLE ISSUE
    // =========================================================

    suspend fun saveCompleteSampleIssue(
        sampleIssue: SampleIssueEntity,
        items: List<SampleIssueItemEntity>
    ): Long {
        return database.withTransaction {
            val inventoryDao = database.inventoryDao()
            val movementDao = database.stockMovementDao()

            val finalNumber = numberingRepository.getNextDocumentNumber(
                DocumentType.SAMPLE_ISSUE,
                sampleIssue.financialYearStart
            )

            val sampleId = sampleIssueDao.insertSampleIssue(
                sampleIssue.copy(
                    sampleIssueNumber = finalNumber,
                    normalizedSampleIssueNumber = finalNumber.uppercase()
                )
            )

            if (items.isNotEmpty()) {
                val finalItems = items.map { it.copy(sampleIssueId = sampleId) }
                sampleIssueDao.insertSampleIssueItems(finalItems)

                finalItems.forEach { item ->
                    // Update Inventory Status
                    inventoryDao.updateStatus(item.inventoryUnitId, "SAMPLE")

                    // Record Movement
                    movementDao.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = item.inventoryUnitId,
                            serialNumber = item.serialNumber,
                            movementType = "SAMPLE_ISSUED",
                            fromStatus = "IN_STOCK",
                            toStatus = "SAMPLE",
                            partyName = sampleIssue.customerName,
                            referenceNumber = finalNumber,
                            movementDate = sampleIssue.sampleIssueDate,
                            remarks = "Issued as sample. Purpose: ${sampleIssue.sampleType}"
                        )
                    )
                }
            }
            sampleId
        }
    }

    // =========================================================
    // UPDATE COMPLETE SAMPLE ISSUE
    // =========================================================

    suspend fun updateCompleteSampleIssue(
        sampleIssue: SampleIssueEntity,
        items: List<SampleIssueItemEntity>
    ) {
        database.withTransaction {
            val inventoryDao = database.inventoryDao()
            val movementDao = database.stockMovementDao()

            val oldItems = sampleIssueDao.getItemsBySampleIssueIdList(sampleIssue.id)
            val oldUnitIds = oldItems.map { it.inventoryUnitId }.toSet()
            val newUnitIds = items.map { it.inventoryUnitId }.toSet()

            // Serials removed: SAMPLE -> IN_STOCK
            val removedIds = oldUnitIds - newUnitIds
            removedIds.forEach { unitId ->
                val unit = inventoryDao.getById(unitId)
                if (unit != null && unit.status == "SAMPLE") {
                    inventoryDao.updateStatus(unitId, "IN_STOCK")
                    movementDao.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = unitId,
                            serialNumber = unit.serialNumber,
                            movementType = "SAMPLE_REMOVED",
                            fromStatus = "SAMPLE",
                            toStatus = "IN_STOCK",
                            partyName = sampleIssue.customerName,
                            referenceNumber = sampleIssue.sampleIssueNumber,
                            movementDate = sampleIssue.sampleIssueDate,
                            remarks = "Removed from Sample Note ${sampleIssue.sampleIssueNumber} during edit."
                        )
                    )
                }
            }

            // Serials added: IN_STOCK -> SAMPLE
            val addedIds = newUnitIds - oldUnitIds
            addedIds.forEach { unitId ->
                val unit = inventoryDao.getById(unitId)
                if (unit != null && unit.status == "IN_STOCK") {
                    inventoryDao.updateStatus(unitId, "SAMPLE")
                    movementDao.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = unitId,
                            serialNumber = unit.serialNumber,
                            movementType = "SAMPLE_ISSUED",
                            fromStatus = "IN_STOCK",
                            toStatus = "SAMPLE",
                            partyName = sampleIssue.customerName,
                            referenceNumber = sampleIssue.sampleIssueNumber,
                            movementDate = sampleIssue.sampleIssueDate,
                            remarks = "Added to Sample Note ${sampleIssue.sampleIssueNumber} during edit."
                        )
                    )
                }
            }

            sampleIssueDao.updateSampleIssue(sampleIssue)
            sampleIssueDao.deleteItemsBySampleIssueId(sampleIssue.id)
            if (items.isNotEmpty()) {
                sampleIssueDao.insertSampleIssueItems(
                    items.map { it.copy(sampleIssueId = sampleIssue.id) }
                )
            }
        }
    }

    suspend fun getSampleWithItems(sampleId: Long): Pair<SampleIssueEntity, List<SampleIssueItemEntity>>? {
        val sample = sampleIssueDao.getSampleIssueById(sampleId) ?: return null
        val items = sampleIssueDao.getItemsBySampleIssueId(sampleId)
        return sample to items
    }

    suspend fun insertSampleIssue(
        sampleIssue: SampleIssueEntity
    ): Long {
        return sampleIssueDao.insertSampleIssue(sampleIssue)
    }

    suspend fun insertSampleIssueItems(
        items: List<SampleIssueItemEntity>
    ) {
        if (items.isEmpty()) return
        sampleIssueDao.insertSampleIssueItems(items)
    }

    suspend fun updateSampleIssue(
        sampleIssue: SampleIssueEntity
    ) {
        sampleIssueDao.updateSampleIssue(sampleIssue)
    }

    suspend fun getSampleIssueById(
        sampleIssueId: Long
    ): SampleIssueEntity? {
        return sampleIssueDao.getSampleIssueById(sampleIssueId)
    }

    fun getAllSampleIssues(): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getAllSampleIssues()
    }

    fun getSampleIssuesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getSampleIssuesByFinancialYear(
            financialYearStart
        )
    }

    fun getSampleIssuesForCustomer(
        customerId: Long
    ): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getSampleIssuesForCustomer(customerId)
    }

    suspend fun getItemsBySampleIssueId(
        sampleIssueId: Long
    ): List<SampleIssueItemEntity> {
        return sampleIssueDao.getItemsBySampleIssueId(sampleIssueId)
    }

    suspend fun inventoryUnitHasActiveSampleIssue(
        inventoryUnitId: Long
    ): Boolean {
        return sampleIssueDao.inventoryUnitHasActiveSampleIssue(
            inventoryUnitId
        )
    }

    suspend fun markItemReturned(
        sampleIssueItemId: Long,
        returnedAt: Long
    ): Int {
        return sampleIssueDao.markItemReturned(
            sampleIssueItemId = sampleIssueItemId,
            returnedAt = returnedAt
        )
    }

    suspend fun markItemConsumed(
        sampleIssueItemId: Long,
        consumedAt: Long
    ): Int {
        return sampleIssueDao.markItemConsumed(
            sampleIssueItemId = sampleIssueItemId,
            consumedAt = consumedAt
        )
    }

    suspend fun refreshSampleIssueStatus(
        sampleIssueId: Long,
        updatedAt: Long
    ) {
        val total =
            sampleIssueDao.getTotalItemCount(sampleIssueId)

        val issued =
            sampleIssueDao.getIssuedItemCount(sampleIssueId)

        val evaluated =
            sampleIssueDao.getEvaluatedItemCount(sampleIssueId)

        val newStatus =
            when {
                total <= 0 -> "ISSUED"
                (issued + evaluated) <= 0 -> "CLOSED"
                issued > 0 -> if (issued < total) "PARTIALLY_RETURNED" else "ISSUED"
                evaluated > 0 -> "EVALUATED"
                else -> "CLOSED"
            }

        sampleIssueDao.updateSampleIssueStatus(
            sampleIssueId = sampleIssueId,
            status = newStatus,
            updatedAt = updatedAt
        )
    }

    suspend fun sampleIssueNumberExists(
        normalizedSampleIssueNumber: String,
        financialYearStart: Int
    ): Boolean {
        return sampleIssueDao.sampleIssueNumberExists(
            normalizedSampleIssueNumber =
                normalizedSampleIssueNumber.trim(),
            financialYearStart =
                financialYearStart
        )
    }

    suspend fun findEvaluatedSampleItemsForCustomer(
        customerId: Long,
        query: String
    ): List<SampleIssueItemEntity> {
        return sampleIssueDao.findEvaluatedSampleItemsForCustomer(customerId, query)
    }

    // =========================================================
    // RETURN SAMPLE TO STOCK (ATOMIC)
    // =========================================================

    suspend fun returnCompleteSampleToStock(sampleId: Long) {
        database.withTransaction {
            val sample = sampleIssueDao.getSampleIssueById(sampleId)
                ?: error("Sample Note not found.")

            require(sample.status == "ISSUED") {
                "Only an ISSUED sample can be returned to stock."
            }

            val items = sampleIssueDao.getItemsBySampleIssueId(sampleId)
            val inventoryDao = database.inventoryDao()
            val movementDao = database.stockMovementDao()
            val now = System.currentTimeMillis()

            // 1. Update Sample Header
            sampleIssueDao.updateSampleIssueStatus(sampleId, "RETURNED", now)

            // 2. Update Items and physical stock
            items.forEach { item ->
                if (item.settlementStatus == "ISSUED") {
                    sampleIssueDao.markItemReturned(item.id, now)
                    
                    // Physical Return
                    inventoryDao.updateStatus(item.inventoryUnitId, "IN_STOCK")

                    movementDao.insertMovement(
                        StockMovementEntity(
                            inventoryUnitId = item.inventoryUnitId,
                            serialNumber = item.serialNumber.trim(),
                            movementType = "RETURNED",
                            fromStatus = "SAMPLE",
                            toStatus = "IN_STOCK",
                            partyName = sample.customerName.trim(),
                            referenceNumber = sample.sampleIssueNumber.trim(),
                            movementDate = sample.sampleIssueDate.trim(),
                            remarks = "Returned to stock from Sample Note ${sample.sampleIssueNumber.trim()}"
                        )
                    )
                }
            }
        }
    }

    // =========================================================
    // MARK SAMPLE AS EVALUATED (ATOMIC BUSINESS STATUS CHANGE)
    // =========================================================

    suspend fun markCompleteSampleEvaluated(sampleId: Long) {
        database.withTransaction {
            val sample = sampleIssueDao.getSampleIssueById(sampleId)
                ?: error("Sample Note not found.")

            require(sample.status == "ISSUED") {
                "Only an ISSUED sample can be marked as evaluated."
            }

            val items = sampleIssueDao.getItemsBySampleIssueId(sampleId)
            val now = System.currentTimeMillis()

            // 1. Update Sample Header
            sampleIssueDao.updateSampleIssueStatus(sampleId, "EVALUATED", now)

            // 2. Update Items settlementStatus to EVALUATED
            items.forEach { item ->
                if (item.settlementStatus == "ISSUED") {
                    sampleIssueDao.updateSampleIssueItemSettlementStatus(item.id, "EVALUATED", now)
                }
            }
            
            // NOTE: No stock movement is created as per Rule 3.
            // Physical serial remains in status 'SAMPLE' (OUT).
        }
    }
}
