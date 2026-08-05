package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.vilync.ophthalmicerp.core.reports.domain.*

object FinancialReportSchemas {

    private val commonColumns = listOf(
        ReportColumn(id = "date", displayName = "Date", weight = 1.0f),
        ReportColumn(id = "refNo", displayName = "Ref No", weight = 1.2f),
        ReportColumn(id = "particulars", displayName = "Particulars", weight = 2.5f),
        ReportColumn(id = "debit", displayName = "Debit (Dr)", weight = 1.2f, type = ColumnType.CURRENCY),
        ReportColumn(id = "credit", displayName = "Credit (Cr)", weight = 1.2f, type = ColumnType.CURRENCY),
        ReportColumn(id = "balance", displayName = "Balance", weight = 1.5f, type = ColumnType.CURRENCY)
    )

    private val commonSummaries = listOf(
        SummaryCardConfig(id = "opening", label = "Opening Balance", icon = Icons.Default.Start, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2), prefix = "₹ "),
        SummaryCardConfig(id = "debit", label = "Total Debit", icon = Icons.Default.AddCircle, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48), prefix = "₹ "),
        SummaryCardConfig(id = "credit", label = "Total Credit", icon = Icons.Default.RemoveCircle, backgroundColor = Color(0xFFFFEEEE), valueColor = Color(0xFFB83A3A), prefix = "₹ "),
        SummaryCardConfig(id = "closing", label = "Closing Balance", icon = Icons.Default.AccountBalanceWallet, backgroundColor = Color(0xFFF9F5FF), valueColor = Color(0xFF6941C6), prefix = "₹ ")
    )

    val CustomerLedgerSchema = ReportSchema(
        id = "customer_ledger",
        title = "Customer Ledger",
        subtitle = "Chronological history of customer transactions",
        icon = "C",
        columns = commonColumns,
        filters = listOf(
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-04-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2027-03-31"),
            ReportFilterDescriptor(id = "party_id", label = "Select Customer", type = FilterType.DROPDOWN, icon = Icons.Default.Person, weight = 1f, defaultValue = "0")
        ),
        summaries = commonSummaries
    )

    val SupplierLedgerSchema = ReportSchema(
        id = "supplier_ledger",
        title = "Supplier Ledger",
        subtitle = "Chronological history of supplier transactions",
        icon = "S",
        columns = commonColumns,
        filters = listOf(
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-04-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2027-03-31"),
            ReportFilterDescriptor(id = "party_id", label = "Select Supplier", type = FilterType.DROPDOWN, icon = Icons.Default.Store, weight = 1f, defaultValue = "0")
        ),
        summaries = commonSummaries
    )

    val CashBookSchema = ReportSchema(
        id = "cash_book",
        title = "Cash Book",
        subtitle = "Chronological record of cash transactions",
        icon = "₿",
        columns = commonColumns,
        filters = listOf(
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-04-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2027-03-31")
        ),
        summaries = commonSummaries
    )

    val BankBookSchema = ReportSchema(
        id = "bank_book",
        title = "Bank Book",
        subtitle = "Chronological record of bank transactions",
        icon = "🏛",
        columns = commonColumns,
        filters = listOf(
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-04-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2027-03-31"),
            ReportFilterDescriptor(id = "account_id", label = "Select Bank Account", type = FilterType.DROPDOWN, icon = Icons.Default.AccountBalance, weight = 1f, defaultValue = "0")
        ),
        summaries = commonSummaries
    )
}
