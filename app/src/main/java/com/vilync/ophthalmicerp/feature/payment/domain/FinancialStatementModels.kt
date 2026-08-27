package com.vilync.ophthalmicerp.feature.payment.domain

/**
 * Reusable row model for all chronological financial reports.
 */
data class LedgerRow(
    val date: String,
    val type: String, // INVOICE, RECEIPT, CREDIT_NOTE, DEBIT_NOTE, etc.
    val particulars: String,
    val refNo: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val balance: Double = 0.0
)

/**
 * Reusable statement model containing chronological records and balances.
 */
data class FinancialStatement(
    val openingBalance: Double,
    val rows: List<LedgerRow>,
    val closingBalance: Double,
    val totalDebit: Double,
    val totalCredit: Double
)

/**
 * Summary model for Party-wise Receivables.
 */
data class PartyReceivableSummary(
    val partyId: Long,
    val partyName: String,
    val totalDue: Double,
    val totalReceived: Double,
    val totalAdjustments: Double,
    val balanceOutstanding: Double
)
