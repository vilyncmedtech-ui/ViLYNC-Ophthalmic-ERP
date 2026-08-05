package com.vilync.ophthalmicerp.feature.sales.challan.presentation

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
import com.vilync.ophthalmicerp.feature.sales.challan.export.ChallanExportSuite
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader

@Composable
fun ChallanDetailScreen(
    viewModel: ChallanDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val challan = uiState.challan
            if (challan != null) {
                ChallanExportSuite.exportPdf(context, it, challan, uiState.items)
            }
        }
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Delivery Challan",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.challan?.let { ChallanExportSuite.print(context, it, uiState.items) } },
                onPdf = { uiState.challan?.let { pdfLauncher.launch("Challan_${it.challanNumber.replace("/", "_")}.pdf") } },
                onShare = { /* TODO */ },
                onEdit = { uiState.challan?.let { onEdit(it.id) } },
                isEditable = uiState.challan?.status == "OPEN" || uiState.challan?.status == "PARTIALLY_SETTLED"
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val challan = uiState.challan!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Challan: ${challan.challanNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Date: ${challan.challanDate}")
                            Text("Status: ${challan.status}")
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Customer: ${challan.customerName}", fontWeight = FontWeight.Bold)
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
                            Text("Serial: ${item.serialNumber} • Power: ${item.power}")
                            Text("Settlement: ${item.settlementStatus}")
                        }
                    }
                }
                if (challan.remarks.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Remarks", fontWeight = FontWeight.Bold)
                                Text(challan.remarks)
                            }
                        }
                    }
                }
            }
        }
    }
}
