package com.vilync.ophthalmicerp.feature.gst.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.gst.export.GstExportSuite
import com.vilync.ophthalmicerp.feature.gst.model.GstDocumentRow
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import android.widget.Toast
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

    val uiState by
    viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var exportMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(
        financialYearStart
    ) {

        viewModel.load(
            financialYearStart
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF7F9FF)
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack
            ) {
                Text("Back")
            }

            Text(
                text =
                    reportType.title,

                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 14.dp
                    ),

                fontSize = 21.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(0xFF20242C)
            )

            Box {
                Button(
                    onClick = {
                        exportMenuOpen = true
                    },
                    enabled = uiState.snapshot != null && !uiState.isLoading
                ) {
                    Text("Export")
                }

                DropdownMenu(
                    expanded = exportMenuOpen,
                    onDismissRequest = {
                        exportMenuOpen = false
                    }
                ) {
                    DropdownMenuItem(
                        text = { Text("Print") },
                        onClick = {
                            exportMenuOpen = false
                            val snapshot = uiState.snapshot

                            if (snapshot == null) {
                                Toast.makeText(
                                    context,
                                    "Report data is not ready yet.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                GstExportSuite.print(
                                    context = context,
                                    reportType = reportType,
                                    snapshot = snapshot,
                                    financialYearDisplayName = financialYearDisplayName
                                ).onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Unable to print GST report.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                                Toast.makeText(
                                    context,
                                    "Report data is not ready yet.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                GstExportSuite.exportPdfAndShare(
                                    context = context,
                                    reportType = reportType,
                                    snapshot = snapshot,
                                    financialYearDisplayName = financialYearDisplayName
                                ).onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Unable to export PDF.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                                Toast.makeText(
                                    context,
                                    "Report data is not ready yet.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                GstExportSuite.exportExcelAndShare(
                                    context = context,
                                    reportType = reportType,
                                    snapshot = snapshot,
                                    financialYearDisplayName = financialYearDisplayName
                                ).onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Unable to export Excel.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Button(
                onClick = onDashboard
            ) {
                Text("Dashboard")
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Financial Year: " +
                        financialYearDisplayName,

            fontSize = 13.sp,

            color =
                Color(0xFF6D7480)
        )

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        when {

            uiState.isLoading -> {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {

                ReportMessageCard(
                    uiState.errorMessage
                        ?: "Unable to load report."
                )
            }

            uiState.snapshot != null -> {

                ReportBody(
                    type = reportType,

                    snapshot =
                        uiState.snapshot!!
                )
            }
        }
    }
}

@Composable
private fun ReportBody(
    type: GstReportType,
    snapshot: GstReportSnapshot
) {

    when (type) {

        GstReportType.GSTR1 -> {

            ReportSummaryCards(
                "Taxable Outward Supplies" to
                        snapshot.outputTaxable,

                "Output GST" to
                        snapshot.outputGst,

                "Credit Note GST" to
                        snapshot.creditNoteGst,

                "Adjusted Output GST" to
                        snapshot.adjustedOutputGst
            )

            ReportSectionTitle(
                "B2B Invoices"
            )

            ReportDocumentRows(
                snapshot.b2bSales
            )

            ReportSectionTitle(
                "B2C Invoices"
            )

            ReportDocumentRows(
                snapshot.b2cSales
            )
        }

        GstReportType.GSTR3B -> {

            ReportSummaryCards(
                "Outward Taxable" to
                        snapshot.outputTaxable,

                "Output GST" to
                        snapshot.outputGst,

                "Input GST / ITC" to
                        snapshot.inputGst,

                "Net GST Position" to
                        snapshot.netGstPosition
            )

            ReportMessageCard(
                "Purchase ITC is shown from " +
                        "persisted purchase GST totals. " +
                        "Historical purchase GST is not " +
                        "reclassified into tax heads."
            )
        }

        GstReportType.SALES_REGISTER -> {

            ReportSummaryCards(
                "Posted Sales" to
                        snapshot.sales.size.toDouble(),

                "Taxable Value" to
                        snapshot.outputTaxable,

                "Output GST" to
                        snapshot.outputGst
            )

            ReportDocumentRows(
                snapshot.sales
            )
        }

        GstReportType.PURCHASE_REGISTER -> {

            ReportSummaryCards(
                "Purchases" to
                        snapshot.purchases.size.toDouble(),

                "Taxable Value" to
                        snapshot.inputTaxable,

                "Input GST" to
                        snapshot.inputGst
            )

            ReportDocumentRows(
                snapshot.purchases
            )
        }

        GstReportType.HSN -> {

            snapshot.hsnRows
                .forEach { row ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 5.dp
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(
                                        0xFFFFF8DF
                                    )
                            ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    14.dp
                                )
                        ) {

                            Text(
                                text =
                                    "HSN " +
                                            row.hsnCode,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Qty: " +
                                            row.quantity
                            )

                            Text(
                                text =
                                    "Taxable: " +
                                            reportMoney(
                                                row.taxableAmount
                                            )
                            )

                            Text(
                                text =
                                    "GST: " +
                                            reportMoney(
                                                row.gstAmount
                                            )
                            )
                        }
                    }
                }

            if (
                snapshot.hsnRows.isEmpty()
            ) {

                ReportMessageCard(
                    "No HSN transaction data " +
                            "found for this Financial Year."
                )
            }
        }

        GstReportType.B2B -> {

            ReportSummaryCards(
                "B2B Invoices" to
                        snapshot.b2bSales
                            .size
                            .toDouble(),

                "Taxable Value" to
                        snapshot.b2bSales
                            .sumOf {
                                it.taxableAmount
                            },

                "GST" to
                        snapshot.b2bSales
                            .sumOf {
                                it.totalGst
                            }
            )

            ReportDocumentRows(
                snapshot.b2bSales
            )
        }

        GstReportType.B2C -> {

            ReportSummaryCards(
                "B2C Invoices" to
                        snapshot.b2cSales
                            .size
                            .toDouble(),

                "Taxable Value" to
                        snapshot.b2cSales
                            .sumOf {
                                it.taxableAmount
                            },

                "GST" to
                        snapshot.b2cSales
                            .sumOf {
                                it.totalGst
                            }
            )

            ReportDocumentRows(
                snapshot.b2cSales
            )
        }

        GstReportType.RETURNS -> {

            ReportSummaryCards(
                "Posted Sales Credit Notes" to
                        snapshot.creditNotes
                            .size
                            .toDouble(),

                "Taxable Adjustment" to
                        snapshot.creditNoteTaxable,

                "GST Adjustment" to
                        snapshot.creditNoteGst
            )

            ReportDocumentRows(
                snapshot.creditNotes
            )

            ReportMessageCard(
                "Purchase Returns remain protected " +
                        "in the existing Purchase Return " +
                        "Register and are not silently " +
                        "netted into ITC here."
            )
        }

        GstReportType.ITC -> {

            ReportSummaryCards(
                "Purchase Taxable Value" to
                        snapshot.inputTaxable,

                "Total Input GST / ITC" to
                        snapshot.inputGst
            )

            ReportMessageCard(
                "Input GST is read exactly from " +
                        "persisted Purchase GST. No " +
                        "historical CGST/SGST/IGST " +
                        "reclassification is performed."
            )

            ReportDocumentRows(
                snapshot.purchases
            )
        }

        GstReportType.OUTPUT_TAX -> {

            ReportSummaryCards(
                "Output CGST" to
                        snapshot.outputCgst,

                "Output SGST" to
                        snapshot.outputSgst,

                "Output IGST" to
                        snapshot.outputIgst,

                "Total Output GST" to
                        snapshot.outputGst
            )
        }

        GstReportType.TAX_HEADS -> {

            ReportSummaryCards(
                "CGST" to
                        snapshot.outputCgst,

                "SGST" to
                        snapshot.outputSgst,

                "IGST" to
                        snapshot.outputIgst,

                "Total" to
                        snapshot.outputGst
            )

            ReportMessageCard(
                "Tax-head summary uses persisted " +
                        "POSTED Sales tax heads only."
            )
        }
    }
}

@Composable
private fun ReportSummaryCards(
    vararg values:
    Pair<String, Double>
) {

    values.forEach { value ->

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                ),

            shape =
                RoundedCornerShape(14.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = value.first,
                    color =
                        Color(0xFF5F6670)
                )

                val isCount =
                    value.first.contains(
                        "Invoices"
                    ) ||
                            value.first.contains(
                                "Purchases"
                            ) ||
                            value.first.contains(
                                "Sales Credit Notes"
                            ) ||
                            value.first ==
                            "Posted Sales"

                Text(
                    text =
                        if (isCount) {
                            value.second
                                .toInt()
                                .toString()
                        } else {
                            reportMoney(
                                value.second
                            )
                        },

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReportSectionTitle(
    text: String
) {

    Spacer(
        modifier =
            Modifier.height(12.dp)
    )

    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF30343B)
    )

    Spacer(
        modifier =
            Modifier.height(6.dp)
    )
}

@Composable
private fun ReportDocumentRows(
    rows: List<GstDocumentRow>
) {

    if (rows.isEmpty()) {

        ReportMessageCard(
            "No records found."
        )

        return
    }

    rows.forEach { row ->

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                ),

            shape =
                RoundedCornerShape(14.dp)
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {

                Text(
                    text =
                        row.documentNumber,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "${row.documentDate}  •  " +
                                row.partyName
                )

                if (
                    row.gstin.isNotBlank()
                ) {

                    Text(
                        text =
                            "GSTIN: ${row.gstin}",

                        fontSize = 12.sp
                    )
                }

                Text(
                    text =
                        "Taxable " +
                                reportMoney(
                                    row.taxableAmount
                                ) +
                                "  |  GST " +
                                reportMoney(
                                    row.totalGst
                                ) +
                                "  |  Total " +
                                reportMoney(
                                    row.totalAmount
                                ),

                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ReportMessageCard(
    message: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        shape =
            RoundedCornerShape(14.dp)
    ) {

        Text(
            text = message,

            modifier =
                Modifier.padding(14.dp),

            color =
                Color(0xFF5F6670),

            fontSize = 13.sp
        )
    }
}

private fun reportMoney(
    value: Double
): String {

    return NumberFormat
        .getCurrencyInstance(
            Locale("en", "IN")
        )
        .format(value)
}