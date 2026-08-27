package com.vilync.ophthalmicerp.feature.financialstatements.domain

import com.vilync.ophthalmicerp.feature.financialstatements.data.FinancialStatementRepository

class FinancialStatementUseCase(
    private val repository: FinancialStatementRepository
) {

    suspend fun getTrialBalance(startDate: String, endDate: String): TrialBalanceReport {
        val rawRows = repository.getTrialBalanceData(startDate, endDate)
        
        // Grouping logic
        val groupedRows = mutableListOf<TrialBalanceRow>()
        FinancialAccountGroup.entries.forEach { group ->
            val items = rawRows.filter { it.group == group }
            if (items.isNotEmpty()) {
                val groupTotal = TrialBalanceRow(
                    accountId = "GRP_${group.name}",
                    accountName = group.displayName,
                    group = group,
                    openingDebit = items.sumOf { it.openingDebit },
                    openingCredit = items.sumOf { it.openingCredit },
                    periodDebit = items.sumOf { it.periodDebit },
                    periodCredit = items.sumOf { it.periodCredit },
                    closingDebit = items.sumOf { it.closingDebit },
                    closingCredit = items.sumOf { it.closingCredit },
                    level = 0,
                    isGroup = true
                )
                groupedRows.add(groupTotal)
                items.forEach { groupedRows.add(it.copy(level = 1)) }
            }
        }

        return TrialBalanceReport(
            rows = groupedRows,
            totalOpeningDebit = rawRows.sumOf { it.openingDebit },
            totalOpeningCredit = rawRows.sumOf { it.openingCredit },
            totalPeriodDebit = rawRows.sumOf { it.periodDebit },
            totalPeriodCredit = rawRows.sumOf { it.periodCredit },
            totalClosingDebit = rawRows.sumOf { it.closingDebit },
            totalClosingCredit = rawRows.sumOf { it.closingCredit }
        )
    }

    suspend fun getProfitLoss(startDate: String, endDate: String): ProfitLossReport {
        val tbRows = repository.getTrialBalanceData(startDate, endDate)
        
        // 1. Income Section
        val sales = tbRows.filter { it.accountId == "NOM_SALES" }.sumOf { it.periodCredit }
        val salesReturn = tbRows.filter { it.accountId == "NOM_SALES_RET" }.sumOf { it.periodDebit }
        val netRevenue = sales - salesReturn
        
        val incomeItems = listOf(
            ProfitLossItem("Sales", sales),
            ProfitLossItem("Sales Return", -salesReturn),
            ProfitLossItem("Net Revenue", netRevenue, isSubTotal = true)
        )

        // 2. Direct Costs
        val purchases = tbRows.filter { it.accountId == "NOM_PURCHASE" }.sumOf { it.periodDebit }
        val purcReturn = tbRows.filter { it.accountId == "NOM_PURC_RET" }.sumOf { it.periodCredit }
        
        // For COGS we also need Opening and Closing stock
        // For simplicity in this first version, we'll fetch them from repository
        val openingStockValue = 0.0 // TODO: Get from previous year closing
        val closingStockValue = repository.getClosingStockValue()
        
        val cogs = openingStockValue + (purchases - purcReturn) - closingStockValue
        
        val directCostItems = listOf(
            ProfitLossItem("Opening Stock", openingStockValue),
            ProfitLossItem("Purchases", purchases),
            ProfitLossItem("Purchase Return", -purcReturn),
            ProfitLossItem("Closing Stock", -closingStockValue),
            ProfitLossItem("Total COGS", cogs, isSubTotal = true)
        )

        val grossProfit = netRevenue - cogs

        // 3. Operating Expenses (Aggregated from TB)
        val opExpenses = tbRows.filter { it.group == FinancialAccountGroup.DIRECT_EXPENSES || it.group == FinancialAccountGroup.INDIRECT_EXPENSES }
            .sumOf { it.periodDebit - it.periodCredit }
        
        val expenseItems = listOf(
            ProfitLossItem("Operating Expenses", opExpenses, isSubTotal = true)
        )

        val operatingProfit = grossProfit - opExpenses
        val netProfit = operatingProfit // Simplified if no other income/expenses

        return ProfitLossReport(
            incomeSection = ProfitLossSection("Revenue", incomeItems, netRevenue),
            directCostSection = ProfitLossSection("Direct Costs", directCostItems, cogs),
            grossProfit = grossProfit,
            operatingExpenseSection = ProfitLossSection("Operating Expenses", expenseItems, opExpenses),
            operatingProfit = operatingProfit,
            otherIncomeSection = ProfitLossSection("Other Income", emptyList(), 0.0),
            otherExpenseSection = ProfitLossSection("Other Expenses", emptyList(), 0.0),
            netProfit = netProfit
        )
    }

    suspend fun getBalanceSheet(asOnDate: String): BalanceSheetReport {
        // We can use a 0-date to asOnDate Trial Balance to get as-on balances
        val tbRows = repository.getTrialBalanceData("01-04-2000", asOnDate)
        val pl = getProfitLoss("01-04-2000", asOnDate) // Profit for entire history? Usually it's current FY.
        
        // Assets
        val assets = tbRows.filter { it.group.type == FinancialAccountType.ASSET }
        val closingStock = repository.getClosingStockValue()
        
        val assetItems = assets.map { BalanceSheetItem(it.accountName, it.closingDebit - it.closingCredit) }.toMutableList()
        assetItems.add(BalanceSheetItem("Inventory / Closing Stock", closingStock))
        
        val totalAssets = assetItems.sumOf { it.amount }

        // Liabilities & Equity
        val liabilities = tbRows.filter { it.group.type == FinancialAccountType.LIABILITY }
        val equity = tbRows.filter { it.group.type == FinancialAccountType.EQUITY }
        
        val liabilityItems = liabilities.map { BalanceSheetItem(it.accountName, it.closingCredit - it.closingDebit) }.toMutableList()
        val equityItems = equity.map { BalanceSheetItem(it.accountName, it.closingCredit - it.closingDebit) }.toMutableList()
        equityItems.add(BalanceSheetItem("Current Year Profit/Loss", pl.netProfit))
        
        val totalLiabilities = liabilityItems.sumOf { it.amount } + equityItems.sumOf { it.amount }

        return BalanceSheetReport(
            liabilitiesSection = BalanceSheetSection("Liabilities & Equity", liabilityItems + equityItems, totalLiabilities),
            assetsSection = BalanceSheetSection("Assets", assetItems, totalAssets),
            totalLiabilitiesAndEquity = totalLiabilities,
            totalAssets = totalAssets,
            difference = totalAssets - totalLiabilities
        )
    }
}
