package com.vilync.ophthalmicerp.feature.payment.logic

import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository
import com.vilync.ophthalmicerp.feature.payment.logic.OutstandingCalculationUseCase

class PaymentUseCase(
    private val financialTransactionUseCase: FinancialTransactionUseCase,
    private val outstandingCalculationUseCase: OutstandingCalculationUseCase,
    private val financialTransactionRepository: FinancialTransactionRepository
) {

    suspend fun getCustomerOutstanding(customerId: Long): Double {
        return outstandingCalculationUseCase.getCustomerOutstanding(customerId)
    }

    suspend fun getSupplierOutstanding(supplierId: Long): Double {
        return outstandingCalculationUseCase.getSupplierOutstanding(supplierId)
    }

    suspend fun recordCustomerReceipt(
        id: Long = 0L,
        date: String,
        amount: Double,
        accountId: Long,
        customerId: Long,
        reference: String,
        remarks: String,
        fyStart: Int,
        isPost: Boolean
    ): Long {
        val transaction = FinancialTransactionEntity(
            id = id,
            transactionDate = date,
            amount = amount,
            type = "CUSTOMER_RECEIPT",
            accountId = accountId,
            partyId = customerId,
            referenceNumber = reference,
            remarks = remarks,
            status = "DRAFT",
            financialYearStart = fyStart
        )
        
        val savedId = financialTransactionUseCase.saveTransaction(transaction)
        if (isPost) {
            financialTransactionUseCase.postTransaction(savedId)
        }
        return savedId
    }

    suspend fun recordSupplierPayment(
        id: Long = 0L,
        date: String,
        amount: Double,
        accountId: Long,
        supplierId: Long,
        reference: String,
        remarks: String,
        fyStart: Int,
        isPost: Boolean
    ): Long {
        val transaction = FinancialTransactionEntity(
            id = id,
            transactionDate = date,
            amount = amount,
            type = "SUPPLIER_PAYMENT",
            accountId = accountId,
            partyId = supplierId,
            referenceNumber = reference,
            remarks = remarks,
            status = "DRAFT",
            financialYearStart = fyStart
        )
        val savedId = financialTransactionUseCase.saveTransaction(transaction)
        if (isPost) {
            financialTransactionUseCase.postTransaction(savedId)
        }
        return savedId
    }

    suspend fun cancelPayment(id: Long, reason: String) {
        financialTransactionUseCase.cancelTransaction(id, reason)
    }
}
