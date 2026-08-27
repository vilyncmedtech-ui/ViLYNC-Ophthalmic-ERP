package com.vilync.ophthalmicerp.feature.sales.challan.presentation

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

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Delivery Challan",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { uiState.challan?.let { 
                    ChallanExportSuite.print(context, it, uiState.lines, uiState.companyProfile, uiState.customer) 
                } },
                onPdf = { uiState.challan?.let { challan ->
                    ChallanExportSuite.sharePdf(context, challan, uiState.lines, uiState.companyProfile, uiState.customer)
                } },
                onExcel = {
                    uiState.challan?.let { challan ->
                        val rawItems = uiState.lines.flatMap { it.items }
                        ChallanExportSuite.exportExcelAndShare(context, challan, rawItems)
                    }
                },
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
                items(uiState.lines) { line ->
                    val item = line.item
                    val qty = line.items.size
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.productName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (line.hsn.isNotBlank()) {
                                    Text("HSN: ${line.hsn}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("Power: ${item.power} • Qty: $qty")
                            
                            if (line.items.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Serials", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    line.items.forEach { lens ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { 
                                                Text(
                                                    "${lens.serialNumber} ${formatExp(lens.expiryDate)}",
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

private fun formatExp(v: String): String {
    val t = v.trim()
    return if (t.length == 4 && t.all { it.isDigit() }) "${t.substring(0, 2)}/${t.substring(2, 4)}" else t
}
