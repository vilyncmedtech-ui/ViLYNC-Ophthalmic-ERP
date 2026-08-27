package com.vilync.ophthalmicerp.feature.gst.presentation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.gst.export.GstExportSuite
import com.vilync.ophthalmicerp.feature.gst.model.GstDocumentRow
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GstReportScreen(
    reportType: GstReportType,
    viewModel: GstReportsViewModel,
    financialYearStart: Int,
    financialYearDisplayName: String,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var exportMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(financialYearStart) {
        viewModel.load(financialYearStart)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(onClick = onBack) {
                Text("Back")
            }

            Text(
                text = reportType.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Box {
                Button(
                    onClick = { exportMenuOpen = true },
                    enabled = uiState.snapshot != null && !uiState.isLoading
                ) {
                    Text("Export")
                }

                DropdownMenu(
                    expanded = exportMenuOpen,
                    onDismissRequest = { exportMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Print") },
                        onClick = {
                            exportMenuOpen = false
                            val snapshot = uiState.snapshot
                            if (snapshot == null) {
                                Toast.makeText(context, "Report data is not ready yet.", Toast.LENGTH_SHORT).show()
                            } else {
                                GstExportSuite.print(context, reportType, snapshot, financialYearDisplayName).onFailure { error ->
                                    Toast.makeText(context, error.message ?: "Unable to print.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("PDF") },
                        onClick = {
                            exportMenuOpen = false
                            val snapshot = uiState.snapshot
                            if (snapshot == null) {
                                Toast.makeText(context, "Report data is not ready yet.", Toast.LENGTH_SHORT).show()
                            } else {
                                GstExportSuite.exportPdfAndShare(context, reportType, snapshot, financialYearDisplayName).onFailure { error ->
                                    Toast.makeText(context, error.message ?: "Unable to export PDF.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Excel") },
                        onClick = {
                            exportMenuOpen = false
                            val snapshot = uiState.snapshot
                            if (snapshot == null) {
                                Toast.makeText(context, "Report data is not ready yet.", Toast.LENGTH_SHORT).show()
                            } else {
                                GstExportSuite.exportExcelAndShare(context, reportType, snapshot, financialYearDisplayName).onFailure { error ->
                                    Toast.makeText(context, error.message ?: "Unable to export Excel.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

            Button(onClick = onDashboard) {
                Text("Dashboard")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Financial Year: $financialYearDisplayName",
                fontSize = 13.sp,
                color = Color(0xFF6D7480)
            )

            if (reportType == GstReportType.HSN) {
                PeriodIndicator(uiState)
            }
        }

        if (reportType == GstReportType.HSN) {
            HSNPeriodFilter(uiState, viewModel)
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {
            uiState.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                ReportMessageCard(uiState.errorMessage ?: "Unable to load report.")
            }

            uiState.snapshot != null -> {
                ReportBody(
                    type = reportType,
                    snapshot = uiState.snapshot!!,
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun PeriodIndicator(uiState: GstReportsUiState) {
    val text = when (uiState.selectedPeriod) {
        FilingPeriod.FINANCIAL_YEAR -> "Full FY"
        FilingPeriod.MONTHLY -> {
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            months.getOrNull(uiState.selectedMonth) ?: ""
        }
        FilingPeriod.QUARTERLY -> "Q${uiState.selectedQuarter}"
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Period: $text",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF345FA8)
        )
    }
}

@Composable
private fun HSNPeriodFilter(uiState: GstReportsUiState, viewModel: GstReportsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Reporting Period", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilingPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.updatePeriod(period) },
                        label = { 
                            Text(period.name.replace("_", " ").lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) 
                        }
                    )
                }
            }

            if (uiState.selectedPeriod == FilingPeriod.MONTHLY) {
                Spacer(modifier = Modifier.height(8.dp))
                // FY starts from April (3)
                val fyMonths = listOf(
                    3 to "Apr", 4 to "May", 5 to "Jun", 6 to "Jul", 7 to "Aug", 8 to "Sep",
                    9 to "Oct", 10 to "Nov", 11 to "Dec", 0 to "Jan", 1 to "Feb", 2 to "Mar"
                )
                
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fyMonths.forEach { (calMonth, label) ->
                        FilterChip(
                            selected = uiState.selectedMonth == calMonth,
                            onClick = { viewModel.updateMonth(calMonth) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDDF6E8),
                                selectedLabelColor = Color(0xFF1B5E20)
                            )
                        )
                    }
                }
            }

            if (uiState.selectedPeriod == FilingPeriod.QUARTERLY) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { q ->
                        FilterChip(
                            selected = uiState.selectedQuarter == q,
                            onClick = { viewModel.updateQuarter(q) },
                            label = { Text("Q$q") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFF9C4),
                                selectedLabelColor = Color(0xFFFBC02D)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBody(
    type: GstReportType,
    snapshot: GstReportSnapshot,
    uiState: GstReportsUiState,
    viewModel: GstReportsViewModel
) {
    when (type) {
        GstReportType.GSTR1 -> {
            Gstr1SummaryGrid(
                snapshot = snapshot,
                filter = uiState.gstr1Filter,
                onFilterChange = viewModel::updateGstr1Filter
            )

            if (uiState.gstr1Filter == Gstr1InvoiceFilter.ALL || uiState.gstr1Filter == Gstr1InvoiceFilter.B2B) {
                ReportSectionTitle("B2B Invoices")
                B2BDocumentTable(snapshot.b2bSales)
            }

            if (uiState.gstr1Filter == Gstr1InvoiceFilter.ALL || uiState.gstr1Filter == Gstr1InvoiceFilter.B2C) {
                ReportSectionTitle("B2C Invoices")
                ReportDocumentRows(snapshot.b2cSales)
            }
        }

        GstReportType.GSTR3B -> {
            ReportSummaryCards(
                "Outward Taxable" to snapshot.outputTaxable,
                "Output GST" to snapshot.outputGst,
                "Input GST / ITC" to snapshot.inputGst,
                "Net GST Position" to snapshot.netGstPosition
            )
            ReportMessageCard("Purchase ITC is shown from persisted purchase GST totals.")
        }

        GstReportType.SALES_REGISTER -> {
            ReportSummaryCards(
                "Posted Sales" to snapshot.sales.size.toDouble(),
                "Taxable Value" to snapshot.outputTaxable,
                "Output GST" to snapshot.outputGst
            )
            ReportDocumentRows(snapshot.sales)
        }

        GstReportType.PURCHASE_REGISTER -> {
            ReportSummaryCards(
                "Purchases" to snapshot.purchases.size.toDouble(),
                "Taxable Value" to snapshot.inputTaxable,
                "Input GST" to snapshot.inputGst
            )
            ReportDocumentRows(snapshot.purchases)
        }

        GstReportType.HSN -> {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HSN", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Qty", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Taxable", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("GST%", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("GST", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Total", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    snapshot.hsnRows.forEach { row ->
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(row.hsnCode, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                            Text(row.quantity.toString(), modifier = Modifier.weight(0.8f), fontSize = 12.sp)
                            Text(compactMoney(row.taxableAmount), modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                            Text("${row.gstPercent}%", modifier = Modifier.weight(0.8f), fontSize = 12.sp)
                            Text(compactMoney(row.gstAmount), modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                            Text(compactMoney(row.totalAmount), modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (snapshot.hsnRows.isEmpty()) {
                        ReportMessageCard("No HSN transaction data found for this period.")
                    }
                }
            }
        }

        GstReportType.B2B -> {
            ReportSummaryCards(
                "B2B Invoices" to snapshot.b2bSales.size.toDouble(),
                "Taxable Value" to snapshot.b2bSales.sumOf { it.taxableAmount },
                "GST" to snapshot.b2bSales.sumOf { it.totalGst }
            )
            B2BDocumentTable(snapshot.b2bSales)
        }

        GstReportType.B2C -> {
            ReportSummaryCards(
                "B2C Invoices" to snapshot.b2cSales.size.toDouble(),
                "Taxable Value" to snapshot.b2cSales.sumOf { it.taxableAmount },
                "GST" to snapshot.b2cSales.sumOf { it.totalGst }
            )
            ReportDocumentRows(snapshot.b2cSales)
        }

        GstReportType.RETURNS -> {
            ReportSummaryCards(
                "Posted Sales Credit Notes" to snapshot.creditNotes.size.toDouble(),
                "Taxable Adjustment" to snapshot.creditNoteTaxable,
                "GST Adjustment" to snapshot.creditNoteGst
            )
            ReportDocumentRows(snapshot.creditNotes)
        }

        GstReportType.ITC -> {
            ReportSummaryCards(
                "Purchase Taxable Value" to snapshot.inputTaxable,
                "Total Input GST / ITC" to snapshot.inputGst
            )
            ReportDocumentRows(snapshot.purchases)
        }

        GstReportType.OUTPUT_TAX -> {
            ReportSummaryCards(
                "Output CGST" to snapshot.outputCgst,
                "Output SGST" to snapshot.outputSgst,
                "Output IGST" to snapshot.outputIgst,
                "Total Output GST" to snapshot.outputGst
            )
        }

        GstReportType.TAX_HEADS -> {
            ReportSummaryCards(
                "CGST" to snapshot.outputCgst,
                "SGST" to snapshot.outputSgst,
                "IGST" to snapshot.outputIgst,
                "Total" to snapshot.outputGst
            )
        }
    }
}

@Composable
private fun B2BDocumentTable(rows: List<GstDocumentRow>) {
    if (rows.isEmpty()) {
        ReportMessageCard("No records found.")
        return
    }

    val hasIgst = rows.any { it.igstAmount > 0 }
    val hasSgstCgst = rows.any { it.cgstAmount > 0 || it.sgstAmount > 0 }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GST No.", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Party Name", modifier = Modifier.weight(1.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Date", modifier = Modifier.weight(1.0f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Invoice No.", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Taxable", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (hasSgstCgst) {
                    Text("SGST", modifier = Modifier.weight(1.0f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("CGST", modifier = Modifier.weight(1.0f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (hasIgst) {
                    Text("IGST", modifier = Modifier.weight(1.0f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("Total", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            rows.forEach { row ->
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.gstin, modifier = Modifier.weight(1.2f), fontSize = 11.sp)
                    Text(row.partyName, modifier = Modifier.weight(1.8f), fontSize = 11.sp)
                    Text(row.documentDate, modifier = Modifier.weight(1.0f), fontSize = 11.sp)
                    Text(row.documentNumber, modifier = Modifier.weight(1.2f), fontSize = 11.sp)
                    Text(compactMoney(row.taxableAmount), modifier = Modifier.weight(1.2f), fontSize = 11.sp)
                    if (hasSgstCgst) {
                        Text(if (row.sgstAmount > 0) compactMoney(row.sgstAmount) else "-", modifier = Modifier.weight(1.0f), fontSize = 11.sp)
                        Text(if (row.cgstAmount > 0) compactMoney(row.cgstAmount) else "-", modifier = Modifier.weight(1.0f), fontSize = 11.sp)
                    }
                    if (hasIgst) {
                        Text(if (row.igstAmount > 0) compactMoney(row.igstAmount) else "-", modifier = Modifier.weight(1.0f), fontSize = 11.sp)
                    }
                    Text(compactMoney(row.totalAmount), modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Gstr1SummaryGrid(
    snapshot: GstReportSnapshot,
    filter: Gstr1InvoiceFilter,
    onFilterChange: (Gstr1InvoiceFilter) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)), // Very light blue
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFD1E9FF)) // Subtle blue border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left 2/3: Summary Grid
            Column(modifier = Modifier.weight(2.1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(
                        label = "Taxable Outward",
                        value = snapshot.outputTaxable,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(32.dp),
                        color = Color(0xFFD1E9FF),
                        thickness = 1.dp
                    )
                    SummaryItem(
                        label = "Output GST",
                        value = snapshot.outputGst,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Color(0xFFD1E9FF),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(
                        label = "Credit Note GST",
                        value = snapshot.creditNoteGst,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(32.dp),
                        color = Color(0xFFD1E9FF),
                        thickness = 1.dp
                    )
                    SummaryItem(
                        label = "Adjusted Output GST",
                        value = snapshot.adjustedOutputGst,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stronger Vertical Divider before Filter
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                color = Color(0xFFCBD5E1),
                thickness = 1.2.dp
            )

            // Right 1/3: Filter Area
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                var expanded by remember { mutableStateOf(false) }
                
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "FILTER",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filterText = when(filter) {
                                Gstr1InvoiceFilter.ALL -> "All"
                                Gstr1InvoiceFilter.B2B -> "B2B Invoice"
                                Gstr1InvoiceFilter.B2C -> "B2C Invoice"
                            }
                            Text(
                                text = filterText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Text("▼", fontSize = 10.sp, color = Color(0xFF64748B))
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(180.dp)
                        ) {
                            Gstr1InvoiceFilter.entries.forEach { f ->
                                DropdownMenuItem(
                                    text = { 
                                        val label = when(f) {
                                            Gstr1InvoiceFilter.ALL -> "All"
                                            Gstr1InvoiceFilter.B2B -> "B2B Invoice"
                                            Gstr1InvoiceFilter.B2C -> "B2C Invoice"
                                        }
                                        Text(
                                            text = label,
                                            fontWeight = if (f == filter) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    onClick = {
                                        onFilterChange(f)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 10.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = reportMoney(value),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E40AF) // Strong blue for amounts
        )
    }
}

@Composable
private fun ReportSummaryCards(vararg values: Pair<String, Double>) {
    values.forEach { value ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = value.first, color = Color(0xFF5F6670))
                val isCount = value.first.contains("Invoices") || value.first.contains("Purchases") || value.first == "Posted Sales"
                Text(
                    text = if (isCount) value.second.toInt().toString() else reportMoney(value.second),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReportSectionTitle(text: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30343B))
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ReportDocumentRows(rows: List<GstDocumentRow>) {
    if (rows.isEmpty()) {
        ReportMessageCard("No records found.")
        return
    }
    rows.forEach { row ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = row.documentNumber, fontWeight = FontWeight.Bold)
                Text(text = "${row.documentDate}  •  ${row.partyName}")
                if (row.gstin.isNotBlank()) {
                    Text(text = "GSTIN: ${row.gstin}", fontSize = 12.sp)
                }
                Text(
                    text = "Taxable " + reportMoney(row.taxableAmount) + "  |  GST " + reportMoney(row.totalGst) + "  |  Total " + reportMoney(row.totalAmount),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ReportMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text = message, modifier = Modifier.padding(14.dp), color = Color(0xFF5F6670), fontSize = 13.sp)
    }
}

private fun reportMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}

private fun compactMoney(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(value)
}
