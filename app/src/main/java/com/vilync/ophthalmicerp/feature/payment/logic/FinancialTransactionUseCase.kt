package com.vilync.ophthalmicerp.feature.payment.logic

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.DocumentType

class FinancialTransactionUseCase(
    private val transactionRepository: FinancialTransactionRepository,
    private val accountRepository: AccountRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val numberingRepository: DocumentNumberingRepository,
    private val database: AppDatabase
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
        database.withTransaction {
            val transaction = transactionRepository.getTransactionById(transactionId)
                ?: throw IllegalArgumentException("Transaction not found")

            // Allow re-posting if already POSTED (for edits), but check logic
            if (transaction.status != "DRAFT" && transaction.status != "POSTED") {
                throw IllegalStateException("Only DRAFT or POSTED transactions can be posted. Current status: ${transaction.status}")
            }

            var updatedTransaction = transaction.copy(
                status = "POSTED",
                updatedAt = System.currentTimeMillis()
            )

            // Business Rule: Assign Receipt Number only when POSTED and if not already assigned.
            if (transaction.type == "CUSTOMER_RECEIPT" && transaction.documentNumber == null) {
                val receiptNumber = numberingRepository.getNextDocumentNumber(
                    type = DocumentType.RECEIPT,
                    financialYearStart = transaction.financialYearStart
                )
                updatedTransaction = updatedTransaction.copy(documentNumber = receiptNumber)
            }

            transactionRepository.updateTransaction(updatedTransaction)

            auditTrailRepository.recordEvent(
                module = "PAYMENT",
                action = if (transaction.status == "POSTED") "UPDATE_TRANSACTION" else "POST_TRANSACTION",
                recordId = transactionId,
                referenceNumber = updatedTransaction.documentNumber ?: transaction.referenceNumber,
                description = "${transaction.type} of amount ${transaction.amount} ${if (transaction.status == "POSTED") "updated" else "posted"}"
            )
        }
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
