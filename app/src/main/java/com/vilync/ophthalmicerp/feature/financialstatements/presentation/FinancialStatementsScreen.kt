package com.vilync.ophthalmicerp.feature.financialstatements.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.financialstatements.domain.*
import com.vilync.ophthalmicerp.feature.financialstatements.export.FinancialStatementExporters
import com.vilync.ophthalmicerp.feature.financialstatements.export.FinancialStatementPdfExporter
import com.vilync.ophthalmicerp.feature.financialstatements.export.FinancialStatementPrintAdapter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialStatementsScreen(
    viewModel: FinancialStatementsViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onDrillDown: (String, Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var exportMenuExpanded by remember { mutableStateOf(false) }
    val tabs = listOf("Trial Balance", "Profit & Loss", "Balance Sheet")

    val calendar = Calendar.getInstance()
    
    val fromDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val date = String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y)
            viewModel.updatePendingFrom(date)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val toDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val date = String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y)
            viewModel.updatePendingTo(date)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Financial Statements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${uiState.appliedDateFrom} to ${uiState.appliedDateTo}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    }
                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Print") },
                                onClick = {
                                    exportMenuExpanded = false
                                    val pdfResult = when (uiState.selectedTab) {
                                        0 -> uiState.trialBalance?.let { FinancialStatementPdfExporter.exportTrialBalance(context, it, uiState.appliedDateFrom, uiState.appliedDateTo) }
                                        1 -> uiState.profitLoss?.let { FinancialStatementPdfExporter.exportProfitLoss(context, it, uiState.appliedDateFrom, uiState.appliedDateTo) }
                                        2 -> uiState.balanceSheet?.let { FinancialStatementPdfExporter.exportBalanceSheet(context, it, uiState.appliedDateTo) }
                                        else -> null
                                    }
                                    pdfResult?.onSuccess {
                                        FinancialStatementPrintAdapter.print(context, it, "Financial_Report")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("PDF") },
                                onClick = {
                                    exportMenuExpanded = false
                                    val pdfResult = when (uiState.selectedTab) {
                                        0 -> uiState.trialBalance?.let { FinancialStatementPdfExporter.exportTrialBalance(context, it, uiState.appliedDateFrom, uiState.appliedDateTo) }
                                        1 -> uiState.profitLoss?.let { FinancialStatementPdfExporter.exportProfitLoss(context, it, uiState.appliedDateFrom, uiState.appliedDateTo) }
                                        2 -> uiState.balanceSheet?.let { FinancialStatementPdfExporter.exportBalanceSheet(context, it, uiState.appliedDateTo) }
                                        else -> null
                                    }
                                    pdfResult?.onSuccess {
                                        FinancialStatementExporters.shareFile(context, it, "Financial_Report")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excel") },
                                onClick = {
                                    exportMenuExpanded = false
                                    val report = when (uiState.selectedTab) {
                                        0 -> uiState.trialBalance?.let { "Trial Balance" to FinancialStatementExporters.getTrialBalanceRows(it) }
                                        1 -> uiState.profitLoss?.let { "Profit Loss" to FinancialStatementExporters.getProfitLossRows(it) }
                                        2 -> uiState.balanceSheet?.let { "Balance Sheet" to FinancialStatementExporters.getBalanceSheetRows(it) }
                                        else -> null
                                    }
                                    report?.let { (title, rows) ->
                                        FinancialStatementExporters.exportToCsv(context, title, rows).onSuccess {
                                            FinancialStatementExporters.shareFile(context, it, title)
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1F2530)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Period Filter Row
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDateField(label = "From", value = uiState.pendingDateFrom, onClick = { fromDatePicker.show() }, modifier = Modifier.weight(1f))
                    FilterDateField(label = "To", value = uiState.pendingDateTo, onClick = { toDatePicker.show() }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { viewModel.applyFilters() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3455A4))
                    ) {
                        Text("APPLY", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SecondaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF3455A4),
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.updateSelectedTab(index) },
                        text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3455A4))
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Report Error", fontWeight = FontWeight.Bold, color = Color(0xFFB83A3A))
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.errorMessage ?: "Unknown error", fontSize = 13.sp, color = Color.Black)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadAllReports() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3455A4))
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> TrialBalanceView(uiState.trialBalance, onDrillDown)
                    1 -> ProfitLossView(uiState.profitLoss)
                    2 -> BalanceSheetView(uiState.balanceSheet)
                }
            }
        }
    }
}

@Composable
fun FilterDateField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, fontSize = 9.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3455A4))
        }
    }
}

@Composable
fun TrialBalanceView(report: TrialBalanceReport?, onDrillDown: (String, Long) -> Unit) {
    if (report == null || report.rows.isEmpty()) {
        EmptyReportState("Trial Balance")
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            TrialBalanceHeader()
        }
        items(report.rows) { row ->
            TrialBalanceRowItem(row) {
                if (row.accountId.startsWith("PARTY_")) {
                    val id = row.accountId.substringAfter("PARTY_").toLong()
                    val type = if (row.group == FinancialAccountGroup.SUNDRY_CREDITORS) "supplier_ledger" else "customer_ledger"
                    onDrillDown(type, id)
                } else if (row.accountId.startsWith("ACC_")) {
                    val id = row.accountId.substringAfter("ACC_").toLong()
                    // Assuming ACC_ could be cash or bank. For now we use cash_book route as example or generic.
                    onDrillDown("cash_book", id)
                }
            }
        }
        item {
            TrialBalanceFooter(report)
        }
    }
}

@Composable
fun TrialBalanceHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F6FB), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Account", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Opening", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Transactions", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Closing", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun TrialBalanceRowItem(row: TrialBalanceRow, onClick: () -> Unit) {
    val bgColor = if (row.isGroup) Color(0xFFF9FAFB) else Color.Transparent
    val fontWeight = if (row.isGroup) FontWeight.Bold else FontWeight.Normal
    val paddingStart = (row.level * 16).dp

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).clickable(enabled = !row.isGroup) { onClick() }.padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(row.accountName, modifier = Modifier.weight(2f).padding(start = paddingStart), fontWeight = fontWeight, fontSize = 12.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            if (row.openingDebit > 0) Text("Dr %.2f".format(row.openingDebit), fontSize = 10.sp, color = Color(0xFF027A48))
            if (row.openingCredit > 0) Text("Cr %.2f".format(row.openingCredit), fontSize = 10.sp, color = Color(0xFFB83A3A))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (row.periodDebit > 0) Text("Dr %.2f".format(row.periodDebit), fontSize = 10.sp)
            if (row.periodCredit > 0) Text("Cr %.2f".format(row.periodCredit), fontSize = 10.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            if (row.closingDebit > 0) Text("Dr %.2f".format(row.closingDebit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (row.closingCredit > 0) Text("Cr %.2f".format(row.closingCredit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E7EB))
}

@Composable
fun TrialBalanceFooter(report: TrialBalanceReport) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F6FB), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TOTAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            Text("Dr %.0f".format(report.totalOpeningDebit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Cr %.0f".format(report.totalOpeningCredit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Dr %.0f".format(report.totalPeriodDebit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Cr %.0f".format(report.totalPeriodCredit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Dr %.0f".format(report.totalClosingDebit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Cr %.0f".format(report.totalClosingCredit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfitLossView(report: ProfitLossReport?) {
    if (report == null) {
        EmptyReportState("Profit & Loss")
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionCard(report.incomeSection) }
        item { SectionCard(report.directCostSection) }
        item { 
            TotalLine("GROSS PROFIT", report.grossProfit, Color(0xFF027A48))
        }
        item { SectionCard(report.operatingExpenseSection) }
        item {
            TotalLine("OPERATING PROFIT", report.operatingProfit, Color(0xFF027A48))
        }
        item {
            TotalLine("NET PROFIT", report.netProfit, Color(0xFF3455A4), isMain = true)
        }
    }
}

@Composable
fun BalanceSheetView(report: BalanceSheetReport?) {
    if (report == null) {
        EmptyReportState("Balance Sheet")
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionCard(report.liabilitiesSection.title, report.liabilitiesSection.items) }
        item { TotalLine("TOTAL LIABILITIES & EQUITY", report.totalLiabilitiesAndEquity, Color(0xFF1F2530), isMain = true) }
        
        item { Spacer(Modifier.height(16.dp)) }
        
        item { SectionCard(report.assetsSection.title, report.assetsSection.items) }
        item { TotalLine("TOTAL ASSETS", report.totalAssets, Color(0xFF3455A4), isMain = true) }
        
        if (Math.abs(report.difference) > 0.01) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEEE))) {
                    Text("Mismatch: %.2f".format(report.difference), modifier = Modifier.padding(12.dp), color = Color(0xFFB83A3A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SectionCard(section: ProfitLossSection) {
    SectionCard(section.title, section.items.map { BalanceSheetItem(it.label, it.amount, it.isSubTotal) })
}

@Composable
fun SectionCard(title: String, items: List<BalanceSheetItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(item.label, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = if (item.isSubTotal) FontWeight.Bold else FontWeight.Normal)
                    Text("%.2f".format(item.amount), fontSize = 13.sp, fontWeight = if (item.isSubTotal) FontWeight.Bold else FontWeight.Normal)
                }
                if (item.isSubTotal) HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))
            }
        }
    }
}

@Composable
fun TotalLine(label: String, amount: Double, color: Color, isMain: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isMain) color.copy(alpha = 0.1f) else Color.Transparent).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = if (isMain) 15.sp else 13.sp, color = color)
        Text("%.2f".format(amount), fontWeight = FontWeight.ExtraBold, fontSize = if (isMain) 15.sp else 13.sp, color = color)
    }
}

@Composable
fun EmptyReportState(reportName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No data available", fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Text("No $reportName entries found for the selected period.", fontSize = 12.sp, color = Color(0xFF6B7280))
        }
    }
}
