package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.PurchaseReturnDao
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnSerialSearchRow
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class PurchaseReturnRepository(
    private val purchaseReturnDao: PurchaseReturnDao,
    private val database: AppDatabase? = null,
    private val numberingRepository: DocumentNumberingRepository? = null
) {

    // =========================================================
    // SAVE COMPLETE PURCHASE RETURN
    // =========================================================

    suspend fun saveCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses: List<Pair<PurchaseReturnItemEntity, List<PurchaseReturnLensEntity>>>
    ): Long {
        val db = database ?: throw IllegalStateException("AppDatabase is required for atomic save.")
        
        return db.withTransaction {
            val finalDebitNoteNumber = numberingRepository?.getNextDocumentNumber(
                DocumentType.DEBIT_NOTE,
                purchaseReturn.financialYearStart
            ) ?: purchaseReturn.creditNoteNumber.trim()

            val returnId = purchaseReturnDao.insertPurchaseReturn(
                purchaseReturn.copy(
                    creditNoteNumber = finalDebitNoteNumber
                )
            )

            itemsWithLenses.forEach { (item, lenses) ->
                val itemId = purchaseReturnDao.insertPurchaseReturnItem(
                    item.copy(purchaseReturnId = returnId)
                )
                if (lenses.isNotEmpty()) {
                    purchaseReturnDao.insertPurchaseReturnLenses(
                        lenses.map { it.copy(purchaseReturnItemId = itemId) }
                    )
                }
            }
            returnId
        }
    }

    // =========================================================
    // UPDATE COMPLETE PURCHASE RETURN
    // =========================================================

    suspend fun updateCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses: List<Pair<PurchaseReturnItemEntity, List<PurchaseReturnLensEntity>>>
    ) {
        val db = database ?: throw IllegalStateException("AppDatabase is required for atomic update.")

        db.withTransaction {
            purchaseReturnDao.updatePurchaseReturn(purchaseReturn)
            purchaseReturnDao.deletePurchaseReturnLensesForReturn(purchaseReturn.id)
            purchaseReturnDao.deletePurchaseReturnItemsForReturn(purchaseReturn.id)

            itemsWithLenses.forEach { (item, lenses) ->
                val itemId = purchaseReturnDao.insertPurchaseReturnItem(
                    item.copy(purchaseReturnId = purchaseReturn.id)
                )
                if (lenses.isNotEmpty()) {
                    purchaseReturnDao.insertPurchaseReturnLenses(
                        lenses.map { it.copy(purchaseReturnItemId = itemId) }
                    )
                }
            }
        }
    }

    // =========================================================
    // PURCHASE RETURN REGISTER
    // =========================================================

    fun getAllPurchaseReturns(): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getAllPurchaseReturns()
    }

    fun getPurchaseReturnsByFinancialYear(
        financialYearStart: Int
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getPurchaseReturnsByFinancialYear(
            financialYearStart
        )
    }


    // =========================================================
    // PURCHASE RETURN BY ID
    // =========================================================

    suspend fun getPurchaseReturnById(
        purchaseReturnId: Long
    ): PurchaseReturnEntity? {
        return purchaseReturnDao.getPurchaseReturnById(purchaseReturnId)
    }


    // =========================================================
    // CANCEL
    // =========================================================

    suspend fun cancelPostedPurchaseReturn(
        purchaseReturnId: Long,
        cancellationReason: String
    ): Boolean {
        return purchaseReturnDao.cancelPostedPurchaseReturn(
            purchaseReturnId = purchaseReturnId,
            reason = cancellationReason.trim(),
            cancelledAt = System.currentTimeMillis()
        ) > 0
    }


    // =========================================================
    // ITEMS / LENSES
    // =========================================================

    fun getPurchaseReturnItems(
        purchaseReturnId: Long
    ): Flow<List<PurchaseReturnItemEntity>> {
        return purchaseReturnDao.getPurchaseReturnItems(purchaseReturnId)
    }

    fun getPurchaseReturnLenses(
        purchaseReturnItemId: Long
    ): Flow<List<PurchaseReturnLensEntity>> {
        return purchaseReturnDao.getPurchaseReturnLenses(purchaseReturnItemId)
    }


    // =========================================================
    // ORIGINAL PURCHASE
    // =========================================================

    suspend fun getOriginalPurchase(
        purchaseId: Long
    ): PurchaseEntity? {
        return purchaseReturnDao.getOriginalPurchase(purchaseId)
    }

    suspend fun getOriginalPurchaseItems(
        purchaseId: Long
    ): List<PurchaseItemEntity> {
        return purchaseReturnDao.getOriginalPurchaseItems(purchaseId)
    }

    suspend fun getOriginalPurchaseItemById(
        originalPurchaseItemId: Long
    ): PurchaseItemEntity? {
        return purchaseReturnDao.getOriginalPurchaseItemById(originalPurchaseItemId)
    }

    suspend fun getOriginalPurchaseLenses(
        purchaseItemId: Long
    ): List<PurchaseLensEntity> {
        return purchaseReturnDao.getOriginalPurchaseLenses(purchaseItemId)
    }


    // =========================================================
    // AVAILABLE FOR RETURN
    // =========================================================

    suspend fun getAvailablePurchaseLensesForReturn(
        purchaseItemId: Long
    ): List<PurchaseLensEntity> {
        return purchaseReturnDao.getAvailablePurchaseLensesForReturn(purchaseItemId)
    }

    suspend fun getAvailablePurchaseLensesForReturn(
        purchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): List<PurchaseLensEntity> {

        return purchaseReturnDao.getAvailablePurchaseLensesForReturnExcludingReturn(
            purchaseItemId = purchaseItemId,
            excludePurchaseReturnId = excludePurchaseReturnId
        )
    }


    // =========================================================
    // ALREADY RETURNED?
    // =========================================================

    suspend fun isPurchaseLensAlreadyReturned(
        originalPurchaseLensId: Long
    ): Boolean {
        return purchaseReturnDao.isPurchaseLensAlreadyReturned(originalPurchaseLensId)
    }

    suspend fun isPurchaseLensAlreadyReturned(
        originalPurchaseLensId: Long,
        excludePurchaseReturnId: Long
    ): Boolean {
        return purchaseReturnDao.isPurchaseLensAlreadyReturnedExcludingReturn(
            originalPurchaseLensId = originalPurchaseLensId,
            excludePurchaseReturnId = excludePurchaseReturnId
        )
    }


    // =========================================================
    // QUANTITY ALREADY RETURNED
    // =========================================================

    suspend fun getAlreadyReturnedQuantity(
        originalPurchaseItemId: Long
    ): Int {
        return purchaseReturnDao.getAlreadyReturnedQuantity(originalPurchaseItemId)
    }

    suspend fun getAlreadyReturnedQuantity(
        originalPurchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): Int {
        return purchaseReturnDao.getAlreadyReturnedQuantityExcludingReturn(
            originalPurchaseItemId = originalPurchaseItemId,
            excludePurchaseReturnId = excludePurchaseReturnId
        )
    }


    // =========================================================
    // REMAINING RETURNABLE QUANTITY
    // =========================================================

    suspend fun getRemainingReturnableQuantity(
        originalPurchaseItemId: Long,
        originalPurchasedQuantity: Int
    ): Int {
        val returned = getAlreadyReturnedQuantity(originalPurchaseItemId)
        return (originalPurchasedQuantity - returned).coerceAtLeast(0)
    }

    suspend fun getRemainingReturnableQuantity(
        originalPurchaseItemId: Long,
        originalPurchasedQuantity: Int,
        excludePurchaseReturnId: Long
    ): Int {

        val returnedOther =
            getAlreadyReturnedQuantity(
                originalPurchaseItemId =
                    originalPurchaseItemId,
                excludePurchaseReturnId =
                    excludePurchaseReturnId
            )

        return (originalPurchasedQuantity - returnedOther)
            .coerceAtLeast(0)
    }


    // =========================================================
    // DUPLICATE PROTECTION
    // =========================================================

    suspend fun creditNoteExists(
        supplierId: Long,
        creditNoteNumber: String
    ): Boolean {
        return purchaseReturnDao.creditNoteExists(
            supplierId = supplierId,
            creditNoteNumber = creditNoteNumber.trim()
        )
    }

    suspend fun creditNoteExists(
        supplierId: Long,
        creditNoteNumber: String,
        excludePurchaseReturnId: Long
    ): Boolean {
        return purchaseReturnDao.creditNoteExistsExcludingReturn(
            supplierId = supplierId,
            creditNoteNumber = creditNoteNumber.trim(),
            excludePurchaseReturnId = excludePurchaseReturnId
        )
    }


    // =========================================================
    // BY PURCHASE
    // =========================================================

    fun getReturnsForPurchase(
        originalPurchaseId: Long
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getReturnsForPurchase(originalPurchaseId)
    }


    // =========================================================
    // BY SUPPLIER
    // =========================================================

    fun getPurchaseReturnsBySupplier(
        supplierId: Long
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getPurchaseReturnsBySupplier(supplierId)
    }


    // =========================================================
    // SEARCH
    // =========================================================

    fun searchPurchaseReturns(
        query: String
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.searchPurchaseReturns(query.trim())
    }


    // =========================================================
    // SERIAL SEARCH
    // =========================================================

    fun searchAvailableIolSerials(
        query: String
    ): Flow<List<PurchaseReturnSerialSearchRow>> {
        return purchaseReturnDao.searchAvailableIolSerials(query.trim())
    }
}
