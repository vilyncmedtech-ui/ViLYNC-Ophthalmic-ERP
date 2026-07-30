package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.dao.SalesCreditNoteDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

class SalesCreditNoteRepository(
    private val creditNoteDao: SalesCreditNoteDao,
    private val database: AppDatabase? = null
) {

    suspend fun insertCreditNote(creditNote: SalesCreditNoteEntity): Long =
        creditNoteDao.insertCreditNote(creditNote)

    suspend fun insertCreditNoteItems(items: List<SalesCreditNoteItemEntity>): List<Long> {
        if (items.isEmpty()) return emptyList()
        return creditNoteDao.insertCreditNoteItems(items)
    }

    suspend fun insertCreditNoteLenses(lenses: List<SalesCreditNoteLensEntity>) {
        if (lenses.isNotEmpty()) creditNoteDao.insertCreditNoteLenses(lenses)
    }

    suspend fun updateCreditNote(creditNote: SalesCreditNoteEntity) =
        creditNoteDao.updateCreditNote(creditNote)

    suspend fun getCreditNoteById(creditNoteId: Long): SalesCreditNoteEntity? =
        creditNoteDao.getCreditNoteById(creditNoteId)

    fun getAllCreditNotes(): Flow<List<SalesCreditNoteEntity>> =
        creditNoteDao.getAllCreditNotes()

    fun getCreditNotesByFinancialYear(financialYearStart: Int): Flow<List<SalesCreditNoteEntity>> =
        creditNoteDao.getCreditNotesByFinancialYear(financialYearStart)

    fun getCreditNotesForCustomer(customerId: Long): Flow<List<SalesCreditNoteEntity>> =
        creditNoteDao.getCreditNotesForCustomer(customerId)

    suspend fun getItemsByCreditNoteId(creditNoteId: Long): List<SalesCreditNoteItemEntity> =
        creditNoteDao.getItemsByCreditNoteId(creditNoteId)

    suspend fun getLensesByCreditNoteId(creditNoteId: Long): List<SalesCreditNoteLensEntity> =
        creditNoteDao.getLensesByCreditNoteId(creditNoteId)

    suspend fun creditNoteNumberExists(
        normalizedCreditNoteNumber: String,
        financialYearStart: Int
    ): Boolean = creditNoteDao.creditNoteNumberExists(
        normalizedCreditNoteNumber.trim(), financialYearStart
    )

    suspend fun physicalReturnAlreadyExists(inventoryUnitId: Long): Boolean =
        creditNoteDao.physicalReturnAlreadyExists(inventoryUnitId)

    /**
     * Saves a complete Credit Note atomically. For SALES_RETURN, every lens marked
     * returnToStock must still be SOLD and is restored to IN_STOCK in the same
     * Room transaction, with a permanent SALES_RETURN stock movement.
     */
    suspend fun saveCompleteCreditNote(
        creditNote: SalesCreditNoteEntity,
        itemsWithLenses: List<Pair<SalesCreditNoteItemEntity, List<SalesCreditNoteLensEntity>>>
    ): Long {
        val db = requireNotNull(database) {
            "AppDatabase is required for atomic Credit Note posting."
        }

        require(creditNote.id == 0L) { "New Credit Note must not already have a database ID." }
        require(creditNote.customerId > 0L) { "Customer / Hospital is required." }
        require(creditNote.creditNoteNumber.trim().isNotBlank()) { "Credit Note Number is required." }
        require(creditNote.creditNoteDate.trim().isNotBlank()) { "Credit Note Date is required." }
        require(creditNote.financialYearStart > 0) { "Valid Financial Year is required." }
        require(creditNote.creditNoteType in setOf("SALES_RETURN", "FINANCIAL_ADJUSTMENT")) {
            "Invalid Credit Note type."
        }
        require(itemsWithLenses.isNotEmpty()) { "At least one Credit Note item is required." }

        return db.withTransaction {
            val normalizedNumber = creditNote.creditNoteNumber.trim().uppercase()
            require(!creditNoteDao.creditNoteNumberExists(normalizedNumber, creditNote.financialYearStart)) {
                "Credit Note Number ${creditNote.creditNoteNumber.trim()} already exists in this Financial Year."
            }

            val allLenses = itemsWithLenses.flatMap { it.second }
            val returnLenses = allLenses.filter { it.returnToStock }

            require(returnLenses.map { it.inventoryUnitId }.distinct().size == returnLenses.size) {
                "The same physical Serial Number cannot be returned more than once in one Credit Note."
            }

            if (creditNote.creditNoteType == "FINANCIAL_ADJUSTMENT") {
                require(returnLenses.isEmpty()) {
                    "Financial Adjustment cannot return physical inventory to stock."
                }
            }

            returnLenses.forEach { lens ->
                require(lens.inventoryUnitId > 0L) { "Returned serial has an invalid Inventory Unit." }
                require(!creditNoteDao.physicalReturnAlreadyExists(lens.inventoryUnitId)) {
                    "Serial Number ${lens.serialNumber.trim()} has already been returned through another active Credit Note."
                }
                val unit = requireNotNull(db.inventoryDao().getById(lens.inventoryUnitId)) {
                    "Inventory Unit for Serial Number ${lens.serialNumber.trim()} does not exist."
                }
                require(unit.status.equals("SOLD", ignoreCase = true)) {
                    "Serial Number ${unit.serialNumber} cannot be returned because its current status is ${unit.status}."
                }
                require(unit.serialNumber.trim().equals(lens.serialNumber.trim(), ignoreCase = true)) {
                    "Serial traceability mismatch for Inventory Unit ${unit.id}."
                }
            }

            val creditNoteId = creditNoteDao.insertCreditNote(
                creditNote.copy(
                    id = 0L,
                    creditNoteNumber = creditNote.creditNoteNumber.trim(),
                    normalizedCreditNoteNumber = normalizedNumber,
                    customerName = creditNote.customerName.trim(),
                    reason = creditNote.reason.trim(),
                    remarks = creditNote.remarks.trim(),
                    status = "POSTED",
                    cancelledAt = null,
                    cancellationReason = ""
                )
            )
            require(creditNoteId > 0L) { "Credit Note could not be saved." }

            itemsWithLenses.forEach { (item, lenses) ->
                require(item.productId > 0L) { "Valid Product is required." }
                require(item.quantity > 0) { "Credit Note item quantity must be greater than zero." }

                val itemId = creditNoteDao.insertCreditNoteItems(
                    listOf(
                        item.copy(
                            id = 0L,
                            creditNoteId = creditNoteId,
                            productName = item.productName.trim(),
                            power = item.power.trim(),
                            batchNumber = item.batchNumber.trim(),
                            lotNumber = item.lotNumber.trim()
                        )
                    )
                ).single()

                if (lenses.isNotEmpty()) {
                    creditNoteDao.insertCreditNoteLenses(
                        lenses.map { lens ->
                            lens.copy(
                                id = 0L,
                                creditNoteItemId = itemId,
                                serialNumber = lens.serialNumber.trim(),
                                power = lens.power.trim(),
                                batchNumber = lens.batchNumber.trim(),
                                expiryDate = lens.expiryDate.trim()
                            )
                        }
                    )
                }
            }

            returnLenses.forEach { lens ->
                val unit = requireNotNull(db.inventoryDao().getById(lens.inventoryUnitId))
                require(unit.status.equals("SOLD", ignoreCase = true)) {
                    "Serial Number ${unit.serialNumber} is no longer in SOLD status."
                }

                db.inventoryDao().updateStatus(unit.id, "IN_STOCK")
                db.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        inventoryUnitId = unit.id,
                        serialNumber = unit.serialNumber.trim(),
                        movementType = "SALES_RETURN",
                        fromStatus = "SOLD",
                        toStatus = "IN_STOCK",
                        partyName = creditNote.customerName.trim(),
                        referenceNumber = creditNote.creditNoteNumber.trim(),
                        movementDate = creditNote.creditNoteDate.trim(),
                        remarks = "Returned to stock through Credit Note ${creditNote.creditNoteNumber.trim()} against Invoice ${creditNote.originalInvoiceNumber.trim()}"
                    )
                )
            }

            creditNoteId
        }
    }
}
