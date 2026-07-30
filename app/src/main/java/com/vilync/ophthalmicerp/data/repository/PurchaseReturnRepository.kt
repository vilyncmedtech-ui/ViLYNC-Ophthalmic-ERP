package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.PurchaseReturnDao
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnSerialSearchRow
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import kotlinx.coroutines.flow.Flow

class PurchaseReturnRepository(
    private val purchaseReturnDao: PurchaseReturnDao
) {

    // =========================================================
    // SAVE COMPLETE PURCHASE RETURN
    // =========================================================

    suspend fun saveCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses: List<Pair<PurchaseReturnItemEntity, List<PurchaseReturnLensEntity>>>
    ): Long {
        return purchaseReturnDao.saveCompletePurchaseReturn(
            purchaseReturn = purchaseReturn,
            itemsWithLenses = itemsWithLenses
        )
    }

    // =========================================================
    // UPDATE / CORRECT COMPLETE PURCHASE RETURN
    // =========================================================

    suspend fun updateCompletePurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        itemsWithLenses: List<Pair<PurchaseReturnItemEntity, List<PurchaseReturnLensEntity>>>
    ) {
        require(purchaseReturn.id > 0L) {
            "Purchase Return ID is required for update."
        }

        purchaseReturnDao.updateCompletePurchaseReturn(
            purchaseReturn = purchaseReturn,
            itemsWithLenses = itemsWithLenses
        )
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
            financialYearStart = financialYearStart
        )
    }

    // =========================================================
    // PURCHASE RETURN BY ID / CANCEL
    // =========================================================

    suspend fun getPurchaseReturnById(
        purchaseReturnId: Long
    ): PurchaseReturnEntity? {
        return purchaseReturnDao.getPurchaseReturnById(
            purchaseReturnId = purchaseReturnId
        )
    }

    suspend fun cancelPostedPurchaseReturn(
        purchaseReturnId: Long,
        reason: String
    ): Boolean {
        if (purchaseReturnId <= 0L || reason.trim().isBlank()) return false

        return purchaseReturnDao.cancelPostedPurchaseReturn(
            purchaseReturnId = purchaseReturnId,
            reason = reason.trim(),
            cancelledAt = System.currentTimeMillis()
        ) > 0
    }

    // =========================================================
    // PURCHASE RETURN ITEMS / LENSES
    // =========================================================

    fun getPurchaseReturnItems(
        purchaseReturnId: Long
    ): Flow<List<PurchaseReturnItemEntity>> {
        return purchaseReturnDao.getPurchaseReturnItems(
            purchaseReturnId = purchaseReturnId
        )
    }

    fun getPurchaseReturnLenses(
        purchaseReturnItemId: Long
    ): Flow<List<PurchaseReturnLensEntity>> {
        return purchaseReturnDao.getPurchaseReturnLenses(
            purchaseReturnItemId = purchaseReturnItemId
        )
    }

    // =========================================================
    // ORIGINAL PURCHASE
    // =========================================================

    suspend fun getOriginalPurchase(
        purchaseId: Long
    ): PurchaseEntity? {
        return purchaseReturnDao.getOriginalPurchase(
            purchaseId = purchaseId
        )
    }

    suspend fun getOriginalPurchaseItems(
        purchaseId: Long
    ): List<PurchaseItemEntity> {
        return purchaseReturnDao.getOriginalPurchaseItems(
            purchaseId = purchaseId
        )
    }

    suspend fun getOriginalPurchaseItemById(
        purchaseItemId: Long
    ): PurchaseItemEntity? {
        return purchaseReturnDao.getOriginalPurchaseItemById(
            purchaseItemId = purchaseItemId
        )
    }

    suspend fun getOriginalPurchaseLenses(
        purchaseItemId: Long
    ): List<PurchaseLensEntity> {
        return purchaseReturnDao.getOriginalPurchaseLenses(
            purchaseItemId = purchaseItemId
        )
    }

    // =========================================================
    // AVAILABLE PHYSICAL IOL LENSES
    // =========================================================

    suspend fun getAvailablePurchaseLensesForReturn(
        purchaseItemId: Long
    ): List<PurchaseLensEntity> {
        return purchaseReturnDao.getAvailablePurchaseLensesForReturn(
            purchaseItemId = purchaseItemId
        )
    }

    /**
     * Edit-mode variant.
     *
     * The Purchase Return currently being corrected is excluded, so its
     * own previously selected lenses remain available for selection.
     */
    suspend fun getAvailablePurchaseLensesForReturn(
        purchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): List<PurchaseLensEntity> {
        if (excludePurchaseReturnId <= 0L) {
            return getAvailablePurchaseLensesForReturn(
                purchaseItemId = purchaseItemId
            )
        }

        return purchaseReturnDao
            .getAvailablePurchaseLensesForReturnExcludingReturn(
                purchaseItemId = purchaseItemId,
                excludePurchaseReturnId = excludePurchaseReturnId
            )
    }

    // =========================================================
    // CHECK EXACT PHYSICAL IOL
    // =========================================================

    suspend fun isPurchaseLensAlreadyReturned(
        originalPurchaseLensId: Long
    ): Boolean {
        return purchaseReturnDao.isPurchaseLensAlreadyReturned(
            originalPurchaseLensId = originalPurchaseLensId
        )
    }

    /**
     * Edit-mode variant. Ignores the return currently being corrected.
     */
    suspend fun isPurchaseLensAlreadyReturned(
        originalPurchaseLensId: Long,
        excludePurchaseReturnId: Long
    ): Boolean {
        if (excludePurchaseReturnId <= 0L) {
            return isPurchaseLensAlreadyReturned(
                originalPurchaseLensId = originalPurchaseLensId
            )
        }

        return purchaseReturnDao
            .isPurchaseLensAlreadyReturnedExcludingReturn(
                originalPurchaseLensId = originalPurchaseLensId,
                excludePurchaseReturnId = excludePurchaseReturnId
            )
    }

    // =========================================================
    // ALREADY RETURNED / REMAINING QUANTITY
    // =========================================================

    suspend fun getAlreadyReturnedQuantity(
        originalPurchaseItemId: Long
    ): Int {
        return purchaseReturnDao.getAlreadyReturnedQuantity(
            originalPurchaseItemId = originalPurchaseItemId
        )
    }

    /**
     * Edit-mode variant. Excludes the current return from the total.
     */
    suspend fun getAlreadyReturnedQuantity(
        originalPurchaseItemId: Long,
        excludePurchaseReturnId: Long
    ): Int {
        if (excludePurchaseReturnId <= 0L) {
            return getAlreadyReturnedQuantity(
                originalPurchaseItemId = originalPurchaseItemId
            )
        }

        return purchaseReturnDao
            .getAlreadyReturnedQuantityExcludingReturn(
                originalPurchaseItemId = originalPurchaseItemId,
                excludePurchaseReturnId = excludePurchaseReturnId
            )
    }

    suspend fun getRemainingReturnableQuantity(
        originalPurchaseItemId: Long,
        originalPurchasedQuantity: Int
    ): Int {
        val alreadyReturned = getAlreadyReturnedQuantity(
            originalPurchaseItemId = originalPurchaseItemId
        )

        return (originalPurchasedQuantity - alreadyReturned)
            .coerceAtLeast(0)
    }

    /**
     * Edit-mode remaining quantity.
     *
     * The current return's own old quantity is excluded, allowing the user
     * to retain, reduce, or correct that quantity without false over-return
     * validation.
     */
    suspend fun getRemainingReturnableQuantity(
        originalPurchaseItemId: Long,
        originalPurchasedQuantity: Int,
        excludePurchaseReturnId: Long
    ): Int {
        val alreadyReturned = getAlreadyReturnedQuantity(
            originalPurchaseItemId = originalPurchaseItemId,
            excludePurchaseReturnId = excludePurchaseReturnId
        )

        return (originalPurchasedQuantity - alreadyReturned)
            .coerceAtLeast(0)
    }

    // =========================================================
    // DUPLICATE DEBIT / CREDIT NOTE CHECK
    // =========================================================

    suspend fun creditNoteExists(
        supplierId: Long,
        creditNoteNumber: String
    ): Boolean {
        val normalizedCreditNote = creditNoteNumber.trim()

        if (supplierId <= 0L || normalizedCreditNote.isBlank()) {
            return false
        }

        return purchaseReturnDao.creditNoteExists(
            supplierId = supplierId,
            creditNoteNumber = normalizedCreditNote
        )
    }

    /**
     * Edit-mode duplicate check.
     *
     * Allows the current Purchase Return to keep its own document number,
     * but still blocks that Supplier + document number on another return.
     */
    suspend fun creditNoteExists(
        supplierId: Long,
        creditNoteNumber: String,
        excludePurchaseReturnId: Long
    ): Boolean {
        val normalizedCreditNote = creditNoteNumber.trim()

        if (supplierId <= 0L || normalizedCreditNote.isBlank()) {
            return false
        }

        if (excludePurchaseReturnId <= 0L) {
            return creditNoteExists(
                supplierId = supplierId,
                creditNoteNumber = normalizedCreditNote
            )
        }

        return purchaseReturnDao.creditNoteExistsExcludingReturn(
            supplierId = supplierId,
            creditNoteNumber = normalizedCreditNote,
            excludePurchaseReturnId = excludePurchaseReturnId
        )
    }

    // =========================================================
    // RETURNS AGAINST ORIGINAL PURCHASE
    // =========================================================

    fun getReturnsForPurchase(
        originalPurchaseId: Long
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getReturnsForPurchase(
            originalPurchaseId = originalPurchaseId
        )
    }

    // =========================================================
    // RETURNS BY SUPPLIER
    // =========================================================

    fun getPurchaseReturnsBySupplier(
        supplierId: Long
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.getPurchaseReturnsBySupplier(
            supplierId = supplierId
        )
    }

    // =========================================================
    // SEARCH PURCHASE RETURNS
    // =========================================================

    fun searchPurchaseReturns(
        query: String
    ): Flow<List<PurchaseReturnEntity>> {
        return purchaseReturnDao.searchPurchaseReturns(
            query = query.trim()
        )
    }

    // =========================================================
    // AVAILABLE IOL SERIAL SEARCH
    // =========================================================

    fun searchAvailableIolSerials(
        query: String
    ): Flow<List<PurchaseReturnSerialSearchRow>> {
        return purchaseReturnDao.searchAvailableIolSerials(
            query = query.trim()
        )
    }
}
