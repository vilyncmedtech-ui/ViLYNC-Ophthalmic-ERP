package com.vilync.ophthalmicerp.feature.sales.sample.presentation

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
import androidx.compose.ui.text.style.TextAlign
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

    var showReturnConfirmation by remember { mutableStateOf(false) }
    var showEvaluationConfirmation by remember { mutableStateOf(false) }

    if (showReturnConfirmation) {
        AlertDialog(
            onDismissRequest = { showReturnConfirmation = false },
            title = { Text("Return to Stock") },
            text = { Text("Are you sure you want to return all issued serials from this Sample Note back to active inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        showReturnConfirmation = false
                        viewModel.returnToStock()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showEvaluationConfirmation) {
        AlertDialog(
            onDismissRequest = { showEvaluationConfirmation = false },
            title = { Text("Mark as Evaluated") },
            text = { Text("This will mark the evaluation as successful. Note: Physical serials will remain OUT of stock with the Hospital/Doctor.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEvaluationConfirmation = false
                        viewModel.markAsEvaluated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirm Evaluation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvaluationConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Sample Issue",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.sample?.let { SampleIssueExportSuite.print(context, it, uiState.items) } },
                onPdf = { uiState.sample?.let { SampleIssueExportSuite.exportPdfAndShare(context, it, uiState.items) } },
                onExcel = {
                    uiState.sample?.let { sample ->
                        SampleIssueExportSuite.exportExcelAndShare(context, sample, uiState.items)
                    }
                },
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

                if (sample.status == "ISSUED" && !uiState.isActionRunning) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showEvaluationConfirmation = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("MARK AS EVALUATED", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }

                            Button(
                                onClick = { showReturnConfirmation = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("RETURN TO STOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                if (uiState.isActionRunning) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
