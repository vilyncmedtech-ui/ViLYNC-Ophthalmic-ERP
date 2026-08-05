package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            uiState.creditNote?.let { cn ->
                CreditNoteExportSuite.exportPdf(context, it, cn, uiState.items, uiState.lenses)
            }
        }
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Credit Note",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.creditNote?.let { CreditNoteExportSuite.print(context, it, uiState.items, uiState.lenses) } },
                onPdf = { uiState.creditNote?.let { pdfLauncher.launch("CreditNote_${it.creditNoteNumber.replace("/", "_")}.pdf") } },
                onShare = { uiState.creditNote?.let { CreditNoteExportSuite.sharePdf(context, it, uiState.items, uiState.lenses) } },
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
                items(uiState.items) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.productName, fontWeight = FontWeight.Bold)
                            Text("Qty: ${item.quantity} • Rate: ₹${item.rate} • Total: ₹${item.totalAmount}")
                            val itemLenses = uiState.lenses.filter { it.creditNoteItemId == item.id }
                            if (itemLenses.isNotEmpty()) {
                                Text("Serials: ${itemLenses.joinToString(", ") { it.serialNumber }}", style = MaterialTheme.typography.bodySmall)
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
