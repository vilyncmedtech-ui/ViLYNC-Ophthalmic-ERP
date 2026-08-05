package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.reports.domain.SortDirection
import com.vilync.ophthalmicerp.core.reports.domain.SummaryCardConfig
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportUiState

@Composable
fun UniversalDataTable(
    modifier: Modifier = Modifier,
    schema: ReportSchema,
    uiState: UniversalReportUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onPaginationChange: (Int, Int) -> Unit,
    onCustomizeColumns: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Search & Toolbar
            TableToolbar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onCustomizeColumns = onCustomizeColumns,
                onRefresh = onRefresh
            )

            // 2. Data Table
            val horizontalScrollState = rememberScrollState()
            
            val visibleColumns = schema.columns.filter { col -> col.id in uiState.visibleColumnIds }

            Column(modifier = Modifier.weight(1f)) {
                // Sticky Header
                TableHeaderRow(
                    visibleColumns = visibleColumns,
                    scrollState = horizontalScrollState,
                    sortColumnId = uiState.sortColumnId,
                    sortDirection = uiState.sortDirection,
                    onSortChange = onSortChange
                )

                HorizontalDivider(color = Color(0xFFEDF1F7))

                // Scrollable Content
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.paginatedRows.isEmpty()) {
                        Text(
                            text = if (uiState.searchQuery.isEmpty()) "No records found" else "No matching records",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(uiState.paginatedRows) { index, row ->
                                TableDataRow(
                                    row = row,
                                    visibleColumns = visibleColumns,
                                    scrollState = horizontalScrollState,
                                    isAlternate = index % 2 != 0
                                )
                                HorizontalDivider(color = Color(0xFFF2F4F7))
                            }
                        }
                    }
                }

                // 3. Table Footer (Totals)
                TableFooter(
                    visibleColumns = visibleColumns,
                    scrollState = horizontalScrollState,
                    rowCount = uiState.totalRecords,
                    rows = uiState.filteredRows
                )
            }

            // 4. Pagination Bar
            TablePaginationBar(
                currentPage = uiState.currentPage,
                rowsPerPage = uiState.rowsPerPage,
                totalRecords = uiState.totalRecords,
                onPaginationChange = onPaginationChange
            )
        }
    }
}

@Composable
private fun TableToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCustomizeColumns: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "TRANSACTION TABLE", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2530))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search Invoice / Customer / Product / Serial...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.width(420.dp).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E6ED),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            FilledTonalButton(
                onClick = onCustomizeColumns,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEEF4FF)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF3538CD))
                Spacer(Modifier.width(8.dp))
                Text("Customize Columns", color = Color(0xFF3538CD), fontSize = 13.sp)
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp), tint = Color(0xFF667085))
            }
        }
    }
}

@Composable
private fun TableHeaderRow(
    visibleColumns: List<com.vilync.ophthalmicerp.core.reports.domain.ReportColumn>,
    scrollState: androidx.compose.foundation.ScrollState,
    sortColumnId: String?,
    sortDirection: SortDirection,
    onSortChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).horizontalScroll(scrollState).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox column placeholder
        Box(modifier = Modifier.size(20.dp).background(Color.White, RoundedCornerShape(4.dp)))

        visibleColumns.forEach { col ->
            Row(
                modifier = Modifier.width(getColumnWidth(col.id)).clickable(enabled = col.sortable) { onSortChange(col.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = col.displayName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF475467),
                    letterSpacing = 0.5.sp
                )
                if (col.sortable && sortColumnId == col.id) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp).padding(start = 4.dp),
                        tint = Color(0xFF345FA8)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableDataRow(
    row: ReportRowData,
    visibleColumns: List<com.vilync.ophthalmicerp.core.reports.domain.ReportColumn>,
    scrollState: androidx.compose.foundation.ScrollState,
    isAlternate: Boolean
) {
    val bgColor = if (isAlternate) Color(0xFFF9FAFB) else Color.White

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).horizontalScroll(scrollState).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox placeholder
        Box(modifier = Modifier.size(20.dp).background(Color.White, RoundedCornerShape(4.dp)))

        visibleColumns.forEach { col ->
            val value = row.values[col.id]?.toString() ?: "-"
            
            if (col.id == "status") {
                StatusBadge(value)
            } else {
                Text(
                    text = value,
                    modifier = Modifier.width(getColumnWidth(col.id)),
                    fontSize = 13.sp,
                    fontWeight = if (col.id == "invoiceNo") FontWeight.Bold else FontWeight.Medium,
                    color = if (col.id == "amount") Color(0xFF101828) else Color(0xFF344054),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "POSTED" -> Color(0xFFECFDF3) to Color(0xFF027A48)
        "PENDING" -> Color(0xFFFFFAEB) to Color(0xFFB45601)
        "CANCELLED" -> Color(0xFFFEF3F2) to Color(0xFFB42318)
        else -> Color(0xFFF2F4F7) to Color(0xFF344054)
    }

    Box(
        modifier = Modifier.width(100.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun TableFooter(
    visibleColumns: List<com.vilync.ophthalmicerp.core.reports.domain.ReportColumn>,
    scrollState: androidx.compose.foundation.ScrollState,
    rowCount: Int,
    rows: List<ReportRowData>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF9FAFB),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(20.dp)) // Checkbox spacing

            visibleColumns.forEach { col ->
                val footerText = when (col.id) {
                    "date" -> "Total Records: $rowCount"
                    "qty" -> rows.sumOf { it.values["qty"]?.toString()?.toDoubleOrNull() ?: 0.0 }.toInt().toString()
                    "amount" -> "₹ " + "%.2f".format(rows.sumOf { it.values["amount"]?.toString()?.toDoubleOrNull() ?: 0.0 })
                    "cgst" -> "%.2f".format(rows.sumOf { it.values["cgst"]?.toString()?.toDoubleOrNull() ?: 0.0 })
                    "sgst" -> "%.2f".format(rows.sumOf { it.values["sgst"]?.toString()?.toDoubleOrNull() ?: 0.0 })
                    "igst" -> "%.2f".format(rows.sumOf { it.values["igst"]?.toString()?.toDoubleOrNull() ?: 0.0 })
                    else -> ""
                }
                Text(
                    text = footerText,
                    modifier = Modifier.width(getColumnWidth(col.id)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101828)
                )
            }
        }
    }
}

@Composable
private fun TablePaginationBar(
    currentPage: Int,
    rowsPerPage: Int,
    totalRecords: Int,
    onPaginationChange: (Int, Int) -> Unit
) {
    val totalPages = (totalRecords + rowsPerPage - 1) / rowsPerPage.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rows per page: ", fontSize = 13.sp, color = Color(0xFF475467))
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text("$rowsPerPage", fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf(10, 25, 50, 100).forEach { size ->
                        DropdownMenuItem(text = { Text("$size") }, onClick = { 
                            onPaginationChange(1, size)
                            expanded = false 
                        })
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onPaginationChange(1, rowsPerPage) }, enabled = currentPage > 1) {
                Icon(Icons.Default.FirstPage, contentDescription = null)
            }
            IconButton(onClick = { onPaginationChange(currentPage - 1, rowsPerPage) }, enabled = currentPage > 1) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            
            Surface(
                color = Color(0xFF3538CD),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$currentPage",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            
            if (totalPages > 1) {
                Text("of $totalPages", fontSize = 13.sp, color = Color(0xFF475467))
            }

            IconButton(onClick = { onPaginationChange(currentPage + 1, rowsPerPage) }, enabled = currentPage < totalPages) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            IconButton(onClick = { onPaginationChange(totalPages, rowsPerPage) }, enabled = currentPage < totalPages) {
                Icon(Icons.Default.LastPage, contentDescription = null)
            }
        }
    }
}

private fun getColumnWidth(columnId: String): androidx.compose.ui.unit.Dp {
    return when (columnId) {
        "qty" -> 60.dp
        "date" -> 95.dp
        "invoiceNo" -> 90.dp
        "customer" -> 180.dp
        "cgst", "sgst", "igst" -> 85.dp
        "gst_percent" -> 75.dp
        "amount" -> 120.dp
        "status" -> 100.dp
        "product" -> 160.dp
        "power" -> 85.dp
        "serial" -> 130.dp
        else -> 120.dp
    }
}

@Composable
fun SummaryPane(
    modifier: Modifier = Modifier,
    summaries: List<SummaryCardConfig>,
    currentValues: Map<String, String>
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("SUMMARY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF345FA8))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summaries.forEach { config ->
                    VerticalSummaryCard(
                        label = config.label,
                        value = "${config.prefix}${currentValues[config.id] ?: "0"}${config.suffix}",
                        icon = config.icon,
                        backgroundColor = config.backgroundColor,
                        valueColor = config.valueColor
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalSummaryCard(
    label: String,
    value: String,
    icon: ImageVector?,
    backgroundColor: Color,
    valueColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(color = Color.White.copy(alpha = 0.65f), shape = RoundedCornerShape(12.dp)) {
                    Icon(icon, contentDescription = label, modifier = Modifier.padding(10.dp).size(22.dp), tint = valueColor)
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = Color(0xFF667085), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}