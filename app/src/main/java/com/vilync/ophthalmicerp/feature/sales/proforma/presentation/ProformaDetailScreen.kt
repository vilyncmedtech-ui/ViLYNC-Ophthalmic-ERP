package com.vilync.ophthalmicerp.feature.sales.proforma.presentation

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
import com.vilync.ophthalmicerp.feature.sales.proforma.export.ProformaInvoiceExportSuite
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader

@Composable
fun ProformaDetailScreen(
    viewModel: ProformaDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            uiState.proforma?.let { proforma ->
                ProformaInvoiceExportSuite.exportPdf(context, it, proforma, uiState.items)
            }
        }
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Proforma Invoice",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.proforma?.let { ProformaInvoiceExportSuite.print(context, it, uiState.items) } },
                onPdf = { uiState.proforma?.let { pdfLauncher.launch("Proforma_${it.proformaNumber.replace("/", "_")}.pdf") } },
                onExcel = {
                    uiState.proforma?.let { proforma ->
                        ProformaInvoiceExportSuite.exportExcelAndShare(context, proforma, uiState.items)
                    }
                },
                onShare = { uiState.proforma?.let { ProformaInvoiceExportSuite.sharePdf(context, it, uiState.items) } },
                onEdit = { uiState.proforma?.let { onEdit(it.id) } },
                isEditable = uiState.proforma?.status == "OPEN"
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val proforma = uiState.proforma!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Proforma: ${proforma.proformaNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Date: ${proforma.proformaDate}")
                            Text("Status: ${proforma.status}")
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Customer: ${proforma.customerName}", fontWeight = FontWeight.Bold)
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
                            Text("Qty: ${item.quantity} • Rate: ₹${item.rate} • GST: ${item.gstPercent}%")
                            Text("Total: ₹${item.totalAmount}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold)
                            Text("₹ %.2f".format(proforma.totalAmount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (proforma.remarks.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Remarks", fontWeight = FontWeight.Bold)
                                Text(proforma.remarks)
                            }
                        }
                    }
                }
            }
        }
    }
}
