package com.vilync.ophthalmicerp.feature.financialstatements.domain

import androidx.compose.ui.graphics.Color

enum class FinancialAccountType {
    ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
}

enum class FinancialAccountGroup(val displayName: String, val type: FinancialAccountType) {
    CASH_IN_HAND("Cash in Hand", FinancialAccountType.ASSET),
    BANK_ACCOUNTS("Bank Accounts", FinancialAccountType.ASSET),
    SUNDRY_DEBTORS("Sundry Debtors", FinancialAccountType.ASSET),
    FIXED_ASSETS("Fixed Assets", FinancialAccountType.ASSET),
    CURRENT_ASSETS("Current Assets", FinancialAccountType.ASSET),
    INVENTORY("Inventory / Closing Stock", FinancialAccountType.ASSET),
    
    CAPITAL_ACCOUNT("Capital Account", FinancialAccountType.EQUITY),
    RETAINED_EARNINGS("Retained Earnings", FinancialAccountType.EQUITY),
    CURRENT_PROFIT_LOSS("Profit & Loss (Current Year)", FinancialAccountType.EQUITY),
    
    LOANS_LIABILITY("Loans & Borrowings", FinancialAccountType.LIABILITY),
    SUNDRY_CREDITORS("Sundry Creditors", FinancialAccountType.LIABILITY),
    DUTIES_AND_TAXES("Duties & Taxes", FinancialAccountType.LIABILITY),
    CURRENT_LIABILITIES("Current Liabilities", FinancialAccountType.LIABILITY),
    
    SALES_ACCOUNTS("Sales Accounts", FinancialAccountType.INCOME),
    DIRECT_INCOME("Direct Incomes", FinancialAccountType.INCOME),
    INDIRECT_INCOME("Indirect Incomes", FinancialAccountType.INCOME),
    
    PURCHASE_ACCOUNTS("Purchase Accounts", FinancialAccountType.EXPENSE),
    DIRECT_EXPENSES("Direct Expenses", FinancialAccountType.EXPENSE),
    INDIRECT_EXPENSES("Indirect Expenses", FinancialAccountType.EXPENSE)
}

data class TrialBalanceRow(
    val accountId: String, // Can be "ACC_1", "PARTY_1", or nominal like "SALES"
    val accountName: String,
    val group: FinancialAccountGroup,
    val openingDebit: Double = 0.0,
    val openingCredit: Double = 0.0,
    val periodDebit: Double = 0.0,
    val periodCredit: Double = 0.0,
    val closingDebit: Double = 0.0,
    val closingCredit: Double = 0.0,
    val level: Int = 0, // For hierarchy display
    val isGroup: Boolean = false
)

data class TrialBalanceReport(
    val rows: List<TrialBalanceRow>,
    val totalOpeningDebit: Double,
    val totalOpeningCredit: Double,
    val totalPeriodDebit: Double,
    val totalPeriodCredit: Double,
    val totalClosingDebit: Double,
    val totalClosingCredit: Double
)

data class ProfitLossSection(
    val title: String,
    val items: List<ProfitLossItem>,
    val total: Double
)

data class ProfitLossItem(
    val label: String,
    val amount: Double,
    val isSubTotal: Boolean = false,
    val isMainTotal: Boolean = false,
    val color: Color? = null
)

data class ProfitLossReport(
    val incomeSection: ProfitLossSection,
    val directCostSection: ProfitLossSection,
    val grossProfit: Double,
    val operatingExpenseSection: ProfitLossSection,
    val operatingProfit: Double,
    val otherIncomeSection: ProfitLossSection,
    val otherExpenseSection: ProfitLossSection,
    val netProfit: Double
)

data class BalanceSheetSection(
    val title: String,
    val items: List<BalanceSheetItem>,
    val total: Double
)

data class BalanceSheetItem(
    val label: String,
    val amount: Double,
    val isSubTotal: Boolean = false,
    val isMainTotal: Boolean = false,
    val color: Color? = null
)

data class BalanceSheetReport(
    val liabilitiesSection: BalanceSheetSection,
    val assetsSection: BalanceSheetSection,
    val totalLiabilitiesAndEquity: Double,
    val totalAssets: Double,
    val difference: Double
)
