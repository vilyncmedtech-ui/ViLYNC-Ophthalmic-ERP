package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.reports.export.UniversalExportSuite

@Composable
fun ReportDataTable(
    modifier: Modifier = Modifier,
    rows: List<ReportRowData>,
    schema: ReportSchema,
    isLoading: Boolean
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }

    // Dynamic Column Visibility Logic
    val hasCgst = remember(rows) { rows.any { (it.values["cgst"] as? Double ?: 0.0) > 0.0 } }
    val hasSgst = remember(rows) { rows.any { (it.values["sgst"] as? Double ?: 0.0) > 0.0 } }
    val hasIgst = remember(rows) { rows.any { (it.values["igst"] as? Double ?: 0.0) > 0.0 } }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Table Header Bar (Search + Table Actions)
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Transaction Table", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2530))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.width(300.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E6ED),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Table", tint = Color(0xFF345FA8))
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Print Report") },
                                onClick = {
                                    showExportMenu = false
                                    UniversalExportSuite.print(context, schema, rows)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF") },
                                onClick = {
                                    showExportMenu = false
                                    UniversalExportSuite.exportPdfAndShare(context, schema, rows)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Excel") },
                                onClick = {
                                    showExportMenu = false
                                    UniversalExportSuite.exportExcelAndShare(context, schema, rows)
                                }
                            )
                        }
                    }
                }
            }

            // 2. High-Density Table
            val horizontalScrollState = rememberScrollState()
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Headers (Sticky-like)
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFF)).horizontalScroll(horizontalScrollState).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    schema.columns.forEach { col ->
                        val isVisible = col.isVisible && when (col.id) {
                            "cgst" -> hasCgst
                            "sgst" -> hasSgst
                            "igst" -> hasIgst
                            else -> true
                        }
                        
                        if (isVisible) {
                            val width = when (col.id) {
                                "qty" -> 60.dp
                                "date" -> 80.dp
                                "invoiceNo", "refNo" -> 100.dp
                                "customer", "particulars" -> 200.dp
                                "customerGstin" -> 130.dp
                                "cgst", "sgst", "igst", "debit", "credit" -> 100.dp
                                "balance" -> 120.dp
                                else -> 120.dp
                            }
                            Text(
                                text = col.displayName.uppercase(),
                                modifier = Modifier.width(width),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D7480)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEDF1F7))

                // Table Rows
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No records found", color = Color.Gray)
                    }
                } else {
                    val filteredRows = if (searchQuery.isBlank()) rows else {
                        rows.filter { row ->
                            row.values.values.any { 
                                it?.toString()?.contains(searchQuery, ignoreCase = true) == true 
                            }
                        }
                    }

                    if (filteredRows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No matching records", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredRows) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState).padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    schema.columns.forEach { col ->
                                        val isVisible = col.isVisible && when (col.id) {
                                            "cgst" -> hasCgst
                                            "sgst" -> hasSgst
                                            "igst" -> hasIgst
                                            else -> true
                                        }

                                        if (isVisible) {
                                            val width = when (col.id) {
                                                "qty" -> 60.dp
                                                "date" -> 80.dp
                                                "invoiceNo", "refNo" -> 100.dp
                                                "customer", "particulars" -> 200.dp
                                                "customerGstin" -> 130.dp
                                                "cgst", "sgst", "igst", "debit", "credit" -> 100.dp
                                                "balance" -> 120.dp
                                                else -> 120.dp
                                            }
                                            Text(
                                                text = row.values[col.id]?.toString() ?: "-",
                                                modifier = Modifier.width(width),
                                                fontSize = 13.sp,
                                                fontWeight = if (col.id == "invoiceNo" || col.id == "refNo") FontWeight.Bold else FontWeight.Normal,
                                                color = if (col.id == "amount") Color(0xFF1F2530) else Color(0xFF252A33),
                                                maxLines = if (col.id == "serial") Int.MAX_VALUE else 1,
                                                overflow = if (col.id == "serial") TextOverflow.Clip else TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFEDF1F7), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
