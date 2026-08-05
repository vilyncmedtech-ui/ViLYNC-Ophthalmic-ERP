package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.dao.ChallanDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

class ChallanRepository(
    private val challanDao: ChallanDao,
    private val database: AppDatabase? = null,
    private val numberingRepository: DocumentNumberingRepository? = null
) {

    suspend fun insertChallan(
        challan: ChallanEntity
    ): Long {
        return challanDao.insertChallan(challan)
    }

    suspend fun insertChallanItems(
        items: List<ChallanItemEntity>
    ) {
        if (items.isEmpty()) return
        challanDao.insertChallanItems(items)
    }

    suspend fun updateChallan(
        challan: ChallanEntity
    ) {
        challanDao.updateChallan(challan)
    }

    suspend fun updateChallanItem(
        item: ChallanItemEntity
    ) {
        challanDao.updateChallanItem(item)
    }

    suspend fun getChallanById(
        challanId: Long
    ): ChallanEntity? {
        return challanDao.getChallanById(challanId)
    }

    suspend fun getItemsByChallanId(
        challanId: Long
    ): List<ChallanItemEntity> {
        return challanDao.getItemsByChallanId(challanId)
    }

    suspend fun getPendingItemsByChallanId(
        challanId: Long
    ): List<ChallanItemEntity> {
        return challanDao.getPendingItemsByChallanId(challanId)
    }

    fun getPendingChallansForCustomer(
        customerId: Long
    ): Flow<List<ChallanEntity>> {
        return challanDao.getPendingChallansForCustomer(customerId)
    }

    fun getPendingItemsForCustomer(
        customerId: Long
    ): Flow<List<ChallanItemEntity>> {
        return challanDao.getPendingItemsForCustomer(customerId)
    }

    suspend fun smartSearchPendingSerialForCustomer(
        customerId: Long,
        serialQuery: String
    ): List<ChallanItemEntity> {

        val query = serialQuery.trim()
        if (query.isBlank()) return emptyList()

        val exactMatch =
            challanDao.findPendingExactSerialForCustomer(
                customerId = customerId,
                serialNumber = query
            )

        if (exactMatch != null) {
            return listOf(exactMatch)
        }

        return challanDao.findPendingSerialSuffixForCustomer(
            customerId = customerId,
            serialSuffix = query
        )
    }

    suspend fun getItemByInventoryUnitId(
        inventoryUnitId: Long
    ): ChallanItemEntity? {
        return challanDao.getItemByInventoryUnitId(inventoryUnitId)
    }

    suspend fun markItemInvoiced(
        challanItemId: Long,
        saleId: Long,
        settledAt: Long
    ): Int {
        return challanDao.markItemInvoiced(
            challanItemId = challanItemId,
            saleId = saleId,
            settledAt = settledAt
        )
    }

    suspend fun getPendingItemCount(
        challanId: Long
    ): Int {
        return challanDao.getPendingItemCount(challanId)
    }

    suspend fun getTotalItemCount(
        challanId: Long
    ): Int {
        return challanDao.getTotalItemCount(challanId)
    }

    suspend fun refreshChallanSettlementStatus(
        challanId: Long,
        updatedAt: Long
    ) {
        val totalCount =
            challanDao.getTotalItemCount(challanId)

        val pendingCount =
            challanDao.getPendingItemCount(challanId)

        val newStatus =
            when {
                totalCount <= 0 -> "OPEN"
                pendingCount <= 0 -> "SETTLED"
                pendingCount < totalCount -> "PARTIALLY_SETTLED"
                else -> "OPEN"
            }

        challanDao.updateChallanStatus(
            challanId = challanId,
            status = newStatus,
            updatedAt = updatedAt
        )
    }

    suspend fun challanNumberExists(
        normalizedChallanNumber: String,
        financialYearStart: Int
    ): Boolean {
        return challanDao.challanNumberExists(
            normalizedChallanNumber =
                normalizedChallanNumber.trim(),
            financialYearStart =
                financialYearStart
        )
    }

    // =========================================================
    // ATOMIC NEW CHALLAN SAVE
    // =========================================================
    //
    // Header + physical items + inventory lifecycle +
    // stock movement are committed as one Room transaction.
    //
    // Any failure rolls the complete operation back.
    // =========================================================

    suspend fun saveCompleteNewChallan(
        challan: ChallanEntity,
        items: List<ChallanItemEntity>
    ): Long {

        val db =
            requireNotNull(database) {
                "AppDatabase is required for atomic Challan save."
            }

        require(items.isNotEmpty()) {
            "Please add at least one physical item to the Challan."
        }

        val inventoryIds =
            items.map { it.inventoryUnitId }

        require(
            inventoryIds.distinct().size == inventoryIds.size
        ) {
            "The same physical inventory unit cannot be added twice."
        }

        return db.withTransaction {

            val inventoryDao = db.inventoryDao()
            val movementDao = db.stockMovementDao()

            items.forEach { item ->

                val unit =
                    inventoryDao.getById(item.inventoryUnitId)
                        ?: error(
                            "Inventory unit not found for serial ${item.serialNumber}."
                        )

                require(
                    unit.status.trim().uppercase() == "IN_STOCK"
                ) {
                    "Serial ${unit.serialNumber} is no longer IN_STOCK."
                }

                require(
                    unit.productId == item.productId
                ) {
                    "Product mismatch for serial ${unit.serialNumber}."
                }
            }

            val finalChallanNumber =
                numberingRepository?.getNextDocumentNumber(
                    DocumentType.CHALLAN,
                    challan.financialYearStart
                ) ?: challan.challanNumber.trim()

            val challanId =
                challanDao.insertChallan(
                    challan.copy(
                        challanNumber = finalChallanNumber,
                        normalizedChallanNumber = finalChallanNumber.uppercase()
                    )
                )

            require(challanId > 0L) {
                "Challan could not be saved."
            }

            val finalItems =
                items.map { item ->
                    item.copy(
                        id = 0L,
                        challanId = challanId,
                        settlementStatus = "PENDING",
                        saleId = null,
                        settledAt = null
                    )
                }

            challanDao.insertChallanItems(finalItems)

            finalItems.forEach { item ->

                inventoryDao.updateStatus(
                    unitId = item.inventoryUnitId,
                    newStatus = "ON_CHALLAN"
                )

                movementDao.insertMovement(
                    StockMovementEntity(
                        inventoryUnitId = item.inventoryUnitId,
                        serialNumber = item.serialNumber.trim(),
                        movementType = "CHALLAN_ISSUED",
                        fromStatus = "IN_STOCK",
                        toStatus = "ON_CHALLAN",
                        partyName = challan.customerName.trim(),
                        referenceNumber = challan.challanNumber.trim(),
                        movementDate = challan.challanDate.trim(),
                        remarks =
                            "Issued through Challan ${challan.challanNumber.trim()}"
                    )
                )
            }

            challanId
        }
    }
}
