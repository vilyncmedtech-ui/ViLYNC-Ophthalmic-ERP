package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.sales.creditnote.export.CreditNoteExportSuite
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader

@Composable
fun CreditNoteDetailScreen(
    viewModel: CreditNoteDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Credit Note",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.creditNote?.let { cn ->
                    // Re-collecting items and lenses for export suite to maintain compatibility
                    val items = uiState.lines.map { it.item }
                    val lenses = uiState.lines.flatMap { it.lenses }
                    CreditNoteExportSuite.print(context, cn, items, lenses, uiState.companyProfile)
                } },
                onPdf = { uiState.creditNote?.let { cn ->
                    val items = uiState.lines.map { it.item }
                    val lenses = uiState.lines.flatMap { it.lenses }
                    CreditNoteExportSuite.sharePdf(context, cn, items, lenses, uiState.companyProfile)
                } },
                onExcel = {
                    uiState.creditNote?.let { cn ->
                        val items = uiState.lines.map { it.item }
                        val lenses = uiState.lines.flatMap { it.lenses }
                        CreditNoteExportSuite.exportExcelAndShare(context, cn, items, lenses)
                    }
                },
                onShare = null,
                onEdit = { uiState.creditNote?.let { onEdit(it.id) } },
                isEditable = uiState.creditNote?.status != "CANCELLED"
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val cn = uiState.creditNote!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Credit Note: ${cn.creditNoteNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Date: ${cn.creditNoteDate}")
                            Text("Type: ${cn.creditNoteType}")
                            Text("Against Invoice: ${cn.originalInvoiceNumber}")
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Customer: ${cn.customerName}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Text("ITEMS", fontWeight = FontWeight.Bold)
                }
                items(uiState.lines) { line ->
                    val item = line.item
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.productName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (line.hsn.isNotBlank()) {
                                    Text("HSN: ${line.hsn}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("Power: ${item.power} • Qty: ${item.quantity}")
                            Text("Rate: ₹${item.rate} • Total: ₹${item.totalAmount}")
                            
                            if (line.lenses.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Serials", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    line.lenses.forEach { lens ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { 
                                                Text(
                                                    "${formatSerial(lens.serialNumber)} ${formatExp(lens.expiryDate)}",
                                                    fontSize = 11.sp
                                                ) 
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFF4F9FF)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFDDEAF5))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold)
                            Text("₹ %.2f".format(cn.totalAmount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (cn.reason.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Reason", fontWeight = FontWeight.Bold)
                                Text(cn.reason)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSerial(sn: String): String {
    val t = sn.trim()
    return if (t.isEmpty() || t.startsWith("LMDE", ignoreCase = true)) t else "LMDE$t"
}

private fun formatExp(v: String): String {
    val t = v.trim()
    return if (t.length == 4 && t.all { it.isDigit() }) "${t.substring(0, 2)}/${t.substring(2, 4)}" else t
}
