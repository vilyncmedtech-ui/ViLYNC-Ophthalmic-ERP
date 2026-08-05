package com.vilync.ophthalmicerp.feature.payment.logic

import com.vilync.ophthalmicerp.data.dao.FinancialTransactionDao
import com.vilync.ophthalmicerp.data.dao.PurchaseDao
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnDao
import com.vilync.ophthalmicerp.data.dao.SalesCreditNoteDao
import com.vilync.ophthalmicerp.data.dao.SalesDao
import com.vilync.ophthalmicerp.feature.payment.domain.FinancialStatement
import com.vilync.ophthalmicerp.feature.payment.domain.LedgerRow
import java.text.SimpleDateFormat
import java.util.*

class FinancialReportingUseCase(
    private val salesDao: SalesDao,
    private val purchaseDao: PurchaseDao,
    private val salesCreditNoteDao: SalesCreditNoteDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val financialTransactionDao: FinancialTransactionDao
) {
    private val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun getCustomerLedger(customerId: Long, startDate: String, endDate: String): FinancialStatement {
        // 1. Opening Balance
        val openingSales = salesDao.getOpeningSalesTotal(customerId, startDate) ?: 0.0
        val openingReceipts = financialTransactionDao.getOpeningBalanceByPartyAndType(customerId, "CUSTOMER_RECEIPT", startDate) ?: 0.0
        val openingCreditNotes = salesCreditNoteDao.getOpeningCreditNoteTotal(customerId, startDate) ?: 0.0
        
        val openingBalance = openingSales - openingReceipts - openingCreditNotes

        // 2. Period Transactions
        val sales = salesDao.getSalesForPeriod(customerId, startDate, endDate).map {
            LedgerRow(it.invoiceDate, "SALES", "Sales Invoice", it.invoiceNumber, debit = it.totalAmount)
        }
        val receipts = financialTransactionDao.getTransactionsForPartyPeriod(customerId, startDate, endDate).map {
            LedgerRow(it.transactionDate, "RECEIPT", it.remarks.ifBlank { "Receipt" }, it.referenceNumber, credit = it.amount)
        }
        val creditNotes = salesCreditNoteDao.getCreditNotesForPeriod(customerId, startDate, endDate).map {
            LedgerRow(it.creditNoteDate, "CREDIT_NOTE", it.reason.ifBlank { "Credit Note" }, it.creditNoteNumber, credit = it.totalAmount)
        }

        // 3. Unify and Sort
        val rows = (sales + receipts + creditNotes).sortedBy { 
            try { dbSdf.format(sdf.parse(it.date)!!) } catch (e: Exception) { it.date } 
        }

        // 4. Running Balance
        var currentBalance = openingBalance
        val rowsWithBalance = rows.map { row ->
            currentBalance += (row.debit - row.credit)
            row.copy(balance = currentBalance)
        }

        return FinancialStatement(
            openingBalance = openingBalance,
            rows = rowsWithBalance,
            closingBalance = currentBalance,
            totalDebit = rows.sumOf { it.debit },
            totalCredit = rows.sumOf { it.credit }
        )
    }

    suspend fun getSupplierLedger(supplierId: Long, startDate: String, endDate: String): FinancialStatement {
        // 1. Opening Balance
        val openingPurchases = purchaseDao.getOpeningPurchasesTotal(supplierId, startDate) ?: 0.0
        val openingPayments = financialTransactionDao.getOpeningBalanceByPartyAndType(supplierId, "SUPPLIER_PAYMENT", startDate) ?: 0.0
        val openingDebitNotes = purchaseReturnDao.getOpeningDebitNoteTotal(supplierId, startDate) ?: 0.0
        
        // Supplier balance: Purchases (Cr) - Payments (Dr) - Returns (Dr)
        val openingBalance = openingPurchases - openingPayments - openingDebitNotes

        // 2. Period Transactions
        val purchases = purchaseDao.getPurchasesForPeriod(supplierId, startDate, endDate).map {
            LedgerRow(it.invoiceDate, "PURCHASE", "Purchase Invoice", it.invoiceNumber, credit = it.grandTotal)
        }
        val payments = financialTransactionDao.getTransactionsForPartyPeriod(supplierId, startDate, endDate).map {
            LedgerRow(it.transactionDate, "PAYMENT", it.remarks.ifBlank { "Payment" }, it.referenceNumber, debit = it.amount)
        }
        val debitNotes = purchaseReturnDao.getDebitNotesForPeriod(supplierId, startDate, endDate).map {
            LedgerRow(it.creditNoteDate, "DEBIT_NOTE", it.remarks.ifBlank { "Debit Note" }, it.creditNoteNumber, debit = it.totalAmount)
        }

        // 3. Unify and Sort
        val rows = (purchases + payments + debitNotes).sortedBy { 
            try { dbSdf.format(sdf.parse(it.date)!!) } catch (e: Exception) { it.date } 
        }

        // 4. Running Balance
        var currentBalance = openingBalance
        val rowsWithBalance = rows.map { row ->
            currentBalance += (row.credit - row.debit)
            row.copy(balance = currentBalance)
        }

        return FinancialStatement(
            openingBalance = openingBalance,
            rows = rowsWithBalance,
            closingBalance = currentBalance,
            totalDebit = rows.sumOf { it.debit },
            totalCredit = rows.sumOf { it.credit }
        )
    }

    suspend fun getAccountBook(accountId: Long, startDate: String, endDate: String): FinancialStatement {
        // 1. Opening Balance
        val openingBalance = financialTransactionDao.getAccountOpeningBalance(accountId, startDate) ?: 0.0

        // 2. Period Transactions
        val transactions = financialTransactionDao.getAccountTransactionsForPeriod(accountId, startDate, endDate).map {
            val isInflow = it.type == "CUSTOMER_RECEIPT" || it.type == "OPENING_BALANCE"
            LedgerRow(
                it.transactionDate, 
                it.type, 
                it.remarks.ifBlank { it.type }, 
                it.referenceNumber, 
                debit = if (isInflow) it.amount else 0.0,
                credit = if (!isInflow) it.amount else 0.0
            )
        }

        // 3. Sort
        val sortedRows = transactions.sortedBy { 
            try { dbSdf.format(sdf.parse(it.date)!!) } catch (e: Exception) { it.date } 
        }

        // 4. Running Balance
        var currentBalance = openingBalance
        val rowsWithBalance = sortedRows.map { row ->
            currentBalance += (row.debit - row.credit)
            row.copy(balance = currentBalance)
        }

        return FinancialStatement(
            openingBalance = openingBalance,
            rows = rowsWithBalance,
            closingBalance = currentBalance,
            totalDebit = sortedRows.sumOf { it.debit },
            totalCredit = sortedRows.sumOf { it.credit }
        )
    }
}
