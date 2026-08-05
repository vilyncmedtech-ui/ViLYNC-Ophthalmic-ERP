package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.reports.export.UniversalExportSuite
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportUiState

@Composable
fun MasterDetailReportLayout(
    uiState: UniversalReportUiState,
    schema: ReportSchema,
    dynamicOptions: Map<String, List<String>> = emptyMap(),
    onFilterChange: (String, Any?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    val headerBlue = Color(0xFFE4EEFF)
    val blueAccent = Color(0xFF345FA8)
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. Title Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(42.dp).clickable { onBack() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = headerBlue)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = blueAccent)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = schema.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = schema.subtitle, fontSize = 12.sp, color = Color.Gray)
                }
                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = blueAccent)
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Print Report") },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.print(context, schema, uiState.rows)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.exportPdfAndShare(context, schema, uiState.rows)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Excel") },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.exportExcelAndShare(context, schema, uiState.rows)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    modifier = Modifier.clickable { onDashboard() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = headerBlue)
                ) {
                    Text(text = "Dashboard", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold, color = blueAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Middle Row: Filters (Left) + Summary (Right)
        Row(modifier = Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Filters Pane
            FilterPane(
                modifier = Modifier.weight(1.5f),
                filters = schema.filters,
                currentValues = uiState.filters,
                dynamicOptions = dynamicOptions,
                onFilterChange = onFilterChange,
                onApply = onApply,
                onReset = onReset
            )

            // Summary Pane
            ReportSummarySection(
                modifier = Modifier.weight(1f),
                schema = schema,
                uiState = uiState
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Bottom: Transaction Table
        ReportDataTable(
            modifier = Modifier.fillMaxWidth().weight(1f),
            rows = uiState.filteredRows,
            schema = schema,
            isLoading = uiState.isLoading
        )
    }
}
