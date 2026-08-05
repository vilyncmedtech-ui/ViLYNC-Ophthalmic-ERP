package com.vilync.ophthalmicerp.feature.payment.logic

import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository

class FinancialTransactionUseCase(
    private val transactionRepository: FinancialTransactionRepository,
    private val accountRepository: AccountRepository,
    private val auditTrailRepository: AuditTrailRepository
) {

    suspend fun saveTransaction(transaction: FinancialTransactionEntity): Long {
        validateTransaction(transaction)
        return if (transaction.id == 0L) {
            transactionRepository.insertTransaction(transaction)
        } else {
            transactionRepository.updateTransaction(transaction)
            transaction.id
        }
    }

    suspend fun postTransaction(transactionId: Long) {
        val transaction = transactionRepository.getTransactionById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found")

        if (transaction.status != "DRAFT") {
            throw IllegalStateException("Only DRAFT transactions can be posted")
        }

        val postedTransaction = transaction.copy(
            status = "POSTED",
            updatedAt = System.currentTimeMillis()
        )

        transactionRepository.updateTransaction(postedTransaction)

        auditTrailRepository.recordEvent(
            module = "PAYMENT",
            action = "POST_TRANSACTION",
            recordId = transactionId,
            referenceNumber = transaction.referenceNumber,
            description = "${transaction.type} of amount ${transaction.amount} posted"
        )
    }

    suspend fun cancelTransaction(transactionId: Long, reason: String) {
        val transaction = transactionRepository.getTransactionById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found")

        if (transaction.status != "POSTED") {
            throw IllegalStateException("Only POSTED transactions can be cancelled")
        }

        val cancelledTransaction = transaction.copy(
            status = "CANCELLED",
            updatedAt = System.currentTimeMillis()
        )

        transactionRepository.updateTransaction(cancelledTransaction)

        auditTrailRepository.recordEvent(
            module = "PAYMENT",
            action = "CANCEL_TRANSACTION",
            recordId = transactionId,
            referenceNumber = transaction.referenceNumber,
            description = "${transaction.type} of amount ${transaction.amount} cancelled. Reason: $reason"
        )
    }

    private suspend fun validateTransaction(transaction: FinancialTransactionEntity) {
        if (transaction.amount <= 0) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        val account = accountRepository.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Valid account is required")

        if (!account.isActive) {
            throw IllegalStateException("Selected account is inactive")
        }

        if (transaction.partyId <= 0) {
            throw IllegalArgumentException("Valid party (Customer/Supplier) is required")
        }
    }
}
