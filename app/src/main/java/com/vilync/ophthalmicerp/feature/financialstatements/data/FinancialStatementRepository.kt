package com.vilync.ophthalmicerp.feature.financialstatements.data

import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.feature.financialstatements.domain.FinancialAccountGroup
import com.vilync.ophthalmicerp.feature.financialstatements.domain.TrialBalanceRow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class FinancialStatementRepository(
    private val database: AppDatabase
) {
    private val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val uiSdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private fun toDbDate(uiDate: String): String {
        return try {
            val date = uiSdf.parse(uiDate)
            dbSdf.format(date!!)
        } catch (e: Exception) {
            uiDate
        }
    }

    suspend fun getTrialBalanceData(startDate: String, endDate: String): List<TrialBalanceRow> {
        val dbStart = toDbDate(startDate)
        val dbEnd = toDbDate(endDate)
        val results = mutableListOf<TrialBalanceRow>()

        // 1. Cash & Bank Accounts
        val accounts = database.accountDao().getAllActiveAccounts().first()
        accounts.forEach { acc ->
            val openingBalance = (database.financialTransactionDao().getAccountOpeningBalance(acc.id, dbStart) ?: 0.0) + acc.initialBalance
            val periodTxs = database.financialTransactionDao().getAccountTransactionsForPeriod(acc.id, dbStart, dbEnd)
            val pDr = periodTxs.filter { it.type == "CUSTOMER_RECEIPT" || it.type == "OPENING_BALANCE" }.sumOf { it.amount }
            val pCr = periodTxs.filter { it.type == "SUPPLIER_PAYMENT" }.sumOf { it.amount }
            val closing = openingBalance + pDr - pCr
            results.add(TrialBalanceRow("ACC_${acc.id}", acc.name, if (acc.type == "CASH") FinancialAccountGroup.CASH_IN_HAND else FinancialAccountGroup.BANK_ACCOUNTS, openingDebit = if (openingBalance > 0) openingBalance else 0.0, openingCredit = if (openingBalance < 0) -openingBalance else 0.0, periodDebit = pDr, periodCredit = pCr, closingDebit = if (closing > 0) closing else 0.0, closingCredit = if (closing < 0) -closing else 0.0))
        }

        // 2. Parties (Sundry Debtors & Creditors)
        val parties = database.partyDao().getAllActiveParties().first()
        parties.forEach { party ->
            val oSales = database.salesDao().getOpeningSalesTotal(party.id, dbStart) ?: 0.0
            val oReceipts = database.financialTransactionDao().getOpeningBalanceByPartyAndType(party.id, "CUSTOMER_RECEIPT", dbStart) ?: 0.0
            val oSalesReturns = database.salesCreditNoteDao().getOpeningCreditNoteTotal(party.id, dbStart) ?: 0.0
            val oPurchases = database.purchaseDao().getOpeningPurchasesTotal(party.id, dbStart) ?: 0.0
            val oPayments = database.financialTransactionDao().getOpeningBalanceByPartyAndType(party.id, "SUPPLIER_PAYMENT", dbStart) ?: 0.0
            val oPurcReturns = database.purchaseReturnDao().getOpeningDebitNoteTotal(party.id, dbStart) ?: 0.0
            
            val openingNet = (oSales + oPayments + oPurcReturns) - (oPurchases + oReceipts + oSalesReturns)
            
            val pSales = database.salesDao().getSalesForPeriod(party.id, dbStart, dbEnd).sumOf { it.totalAmount }
            val pReceipts = database.financialTransactionDao().getTransactionsForPartyPeriod(party.id, dbStart, dbEnd).filter { it.type == "CUSTOMER_RECEIPT" }.sumOf { it.amount }
            val pSalesReturns = database.salesCreditNoteDao().getCreditNotesForPeriod(party.id, dbStart, dbEnd).sumOf { it.totalAmount }
            val pPurchases = database.purchaseDao().getPurchasesForPeriod(party.id, dbStart, dbEnd).sumOf { it.grandTotal }
            val pPayments = database.financialTransactionDao().getTransactionsForPartyPeriod(party.id, dbStart, dbEnd).filter { it.type == "SUPPLIER_PAYMENT" }.sumOf { it.amount }
            val pPurcReturns = database.purchaseReturnDao().getDebitNotesForPeriod(party.id, dbStart, dbEnd).sumOf { it.totalAmount }
            
            val pDr = pSales + pPayments + pPurcReturns
            val pCr = pPurchases + pReceipts + pSalesReturns
            val closingNet = openingNet + pDr - pCr
            results.add(TrialBalanceRow("PARTY_${party.id}", party.partyName, if (party.partyType == "VENDOR") FinancialAccountGroup.SUNDRY_CREDITORS else FinancialAccountGroup.SUNDRY_DEBTORS, openingDebit = if (openingNet > 0) openingNet else 0.0, openingCredit = if (openingNet < 0) -openingNet else 0.0, periodDebit = pDr, periodCredit = pCr, closingDebit = if (closingNet > 0) closingNet else 0.0, closingCredit = if (closingNet < 0) -closingNet else 0.0))
        }

        // 3. Nominal Accounts
        results.add(aggregateTable("NOM_SALES", "Sales Account", FinancialAccountGroup.SALES_ACCOUNTS, "sales", "taxableAmount", "invoiceDate", dbStart, dbEnd, true))
        results.add(aggregateTable("NOM_PURCHASE", "Purchase Account", FinancialAccountGroup.PURCHASE_ACCOUNTS, "purchases", "taxableAmount", "receivedDate", dbStart, dbEnd, false))
        results.add(aggregateTable("NOM_SALES_RET", "Sales Return Account", FinancialAccountGroup.SALES_ACCOUNTS, "sales_credit_notes", "taxableAmount", "creditNoteDate", dbStart, dbEnd, false))
        results.add(aggregateTable("NOM_PURC_RET", "Purchase Return Account", FinancialAccountGroup.PURCHASE_ACCOUNTS, "purchase_returns", "subTotal", "creditNoteDate", dbStart, dbEnd, true))
        
        // Duties & Taxes (GST)
        results.add(aggregateGst(dbStart, dbEnd))
        
        // Discounts
        results.add(aggregateTable("NOM_DISC_ALLOWED", "Discount Allowed", FinancialAccountGroup.INDIRECT_EXPENSES, "sales", "discountAmount", "invoiceDate", dbStart, dbEnd, false))
        results.add(aggregateTable("NOM_DISC_REC", "Discount Received", FinancialAccountGroup.INDIRECT_INCOME, "purchases", "discountAmount", "receivedDate", dbStart, dbEnd, true))

        return results
    }

    private fun aggregateTable(id: String, name: String, group: FinancialAccountGroup, table: String, col: String, dateCol: String, dbStart: String, dbEnd: String, isCredit: Boolean): TrialBalanceRow {
        val q = "SELECT SUM($col) FROM $table WHERE status = 'POSTED' AND (substr($dateCol, 7, 4) || '-' || substr($dateCol, 4, 2) || '-' || substr($dateCol, 1, 2)) BETWEEN '$dbStart' AND '$dbEnd'"
        val amt = database.openHelper.readableDatabase.query(q).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }
        return TrialBalanceRow(id, name, group, periodDebit = if (!isCredit) amt else 0.0, periodCredit = if (isCredit) amt else 0.0, closingDebit = if (!isCredit) amt else 0.0, closingCredit = if (isCredit) amt else 0.0)
    }

    private fun aggregateGst(dbStart: String, dbEnd: String): TrialBalanceRow {
        val qSales = "SELECT SUM(gstAmount) FROM sales WHERE status = 'POSTED' AND (substr(invoiceDate, 7, 4) || '-' || substr(invoiceDate, 4, 2) || '-' || substr(invoiceDate, 1, 2)) BETWEEN '$dbStart' AND '$dbEnd'"
        val qPurc = "SELECT SUM(cgstAmount + sgstAmount + igstAmount) FROM purchases WHERE status = 'POSTED' AND (substr(receivedDate, 7, 4) || '-' || substr(receivedDate, 4, 2) || '-' || substr(receivedDate, 1, 2)) BETWEEN '$dbStart' AND '$dbEnd'"
        val qSalesRet = "SELECT SUM(gstAmount) FROM sales_credit_notes WHERE status = 'POSTED' AND (substr(creditNoteDate, 7, 4) || '-' || substr(creditNoteDate, 4, 2) || '-' || substr(creditNoteDate, 1, 2)) BETWEEN '$dbStart' AND '$dbEnd'"
        val qPurcRet = "SELECT SUM(gstAmount) FROM purchase_returns WHERE status = 'POSTED' AND (substr(creditNoteDate, 7, 4) || '-' || substr(creditNoteDate, 4, 2) || '-' || substr(creditNoteDate, 1, 2)) BETWEEN '$dbStart' AND '$dbEnd'"

        val outGst = database.openHelper.readableDatabase.query(qSales).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }
        val inGst = database.openHelper.readableDatabase.query(qPurc).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }
        val srGst = database.openHelper.readableDatabase.query(qSalesRet).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }
        val prGst = database.openHelper.readableDatabase.query(qPurcRet).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }
        
        val totalDr = inGst + srGst
        val totalCr = outGst + prGst
        val net = totalDr - totalCr
        
        return TrialBalanceRow("NOM_GST", "GST Duties & Taxes", FinancialAccountGroup.DUTIES_AND_TAXES, periodDebit = totalDr, periodCredit = totalCr, closingDebit = if (net > 0) net else 0.0, closingCredit = if (net < 0) -net else 0.0)
    }

    suspend fun getClosingStockValue(): Double {
        return database.inventoryStockDao().calculateClosingStockValueTotal() ?: 0.0
    }
}
