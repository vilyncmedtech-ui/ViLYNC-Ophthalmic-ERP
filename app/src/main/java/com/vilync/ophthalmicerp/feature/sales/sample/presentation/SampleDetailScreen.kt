package com.vilync.ophthalmicerp.feature.sales.sample.presentation

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
import com.vilync.ophthalmicerp.feature.sales.sample.export.SampleIssueExportSuite
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader

@Composable
fun SampleDetailScreen(
    viewModel: SampleDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            uiState.sample?.let { sample ->
                SampleIssueExportSuite.exportPdf(context, it, sample, uiState.items)
            }
        }
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Sample Issue",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.sample?.let { SampleIssueExportSuite.print(context, it, uiState.items) } },
                onPdf = { uiState.sample?.let { pdfLauncher.launch("Sample_${it.sampleIssueNumber.replace("/", "_")}.pdf") } },
                onShare = { uiState.sample?.let { SampleIssueExportSuite.sharePdf(context, it, uiState.items) } },
                onEdit = { uiState.sample?.let { onEdit(it.id) } },
                isEditable = uiState.sample?.status == "ISSUED"
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val sample = uiState.sample!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Sample: ${sample.sampleIssueNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Date: ${sample.sampleIssueDate}")
                            Text("Purpose: ${sample.sampleType}")
                            Text("Expected Return: ${sample.expectedReturnDate}")
                            Text("Status: ${sample.status}")
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Hospital / Doctor: ${sample.customerName}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Text("ISSUED SERIALS", fontWeight = FontWeight.Bold)
                }
                items(uiState.items) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.productName, fontWeight = FontWeight.Bold)
                            Text("Serial: ${item.serialNumber} • Power: ${item.power}")
                            Text("Status: ${item.settlementStatus}")
                        }
                    }
                }
                if (sample.remarks.isNotBlank()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Remarks", fontWeight = FontWeight.Bold)
                                Text(sample.remarks)
                            }
                        }
                    }
                }
            }
        }
    }
}
