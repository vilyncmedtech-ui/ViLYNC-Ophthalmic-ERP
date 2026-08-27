package com.vilync.ophthalmicerp.feature.sales.register

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SalesRegisterScreen(
    viewModel: SalesRegisterViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onNewDocument: () -> Unit,
    onViewDetail: (Long) -> Unit,
    onEditInvoice: ((Long) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show action messages as Snackbar
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.registerType.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Search
                    IconButton(onClick = { /* TODO: Show search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    // Filter
                    IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    // Export
                    IconButton(onClick = {
                        viewModel.update { it.copy(showExportDialog = true) }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    // Refresh
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // Dashboard
                    IconButton(onClick = onDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.registerType.isEditable) {
                FloatingActionButton(
                    onClick = onNewDocument
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::updateQuery,
                onRefresh = viewModel::refresh
            )

            // Summary Row
            SummaryRow(
                totalAmount = uiState.totalAmount,
                totalCount = uiState.count,
                postedCount = uiState.postedCount,
                cancelledCount = uiState.cancelledCount,
                isLoading = uiState.isLoading
            )

            // Status Filter Chips
            StatusFilterChips(
                selectedStatus = uiState.statusFilter,
                onStatusSelected = viewModel::updateStatusFilter,
                statuses = filterOptions.statuses
            )

            // List or Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = viewModel::refresh) {
                            Text("Retry")
                        }
                    }
                }
            } else if (uiState.rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No records found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.rows) { row ->
                        SalesRegisterRowItem(
                            row = row,
                            onClick = { onViewDetail(row.id) },
                            onCancelClick = {
                                viewModel.setCancelDialog(row)
                            },
                            onDeleteClick = {
                                if (row.status.uppercase() == "DRAFT") {
                                    viewModel.deleteInvoice(row.id)
                                }
                            },
                            onEditClick = {
                                onEditInvoice?.invoke(row.id)
                            },
                            onSelectionToggle = {
                                viewModel.toggleSelection(row.id)
                            },
                            isSelected = uiState.selectedIds.contains(row.id)
                        )
                    }
                }
            }
        }
    }

    // Cancel Dialog
    if (uiState.showCancelDialog) {
        CancelDialog(
            reason = uiState.cancelReason,
            onReasonChange = viewModel::updateCancelReason,
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissCancelDialog,
            isRunning = uiState.isActionRunning
        )
    }

    // Export Dialog
    if (uiState.showExportDialog) {
        ExportDialog(
            onDismiss = { viewModel.update { it.copy(showExportDialog = false) } },
            onPdfExport = {
                viewModel.update { it.copy(showExportDialog = false) }
                viewModel.exportPdfAndShare(context)
            },
            onExcelExport = {
                viewModel.update { it.copy(showExportDialog = false) }
                viewModel.exportExcelAndShare(context)
            },
            onPrint = {
                viewModel.update { it.copy(showExportDialog = false) }
                viewModel.printRegister(context)
            }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by No., Customer, Date...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun SummaryRow(
    totalAmount: Double,
    totalCount: Int,
    postedCount: Int,
    cancelledCount: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                label = "Total",
                value = if (isLoading) "..." else "₹${String.format("%.2f", totalAmount)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SummaryItem(
                label = "Count",
                value = if (isLoading) "..." else totalCount.toString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SummaryItem(
                label = "Posted",
                value = if (isLoading) "..." else postedCount.toString(),
                color = Color(0xFF4CAF50)
            )
            SummaryItem(
                label = "Cancelled",
                value = if (isLoading) "..." else cancelledCount.toString(),
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusFilterChips(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    statuses: List<String>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statuses) { status ->
            val isSelected = status == selectedStatus
            FilterChip(
                selected = isSelected,
                onClick = { onStatusSelected(status) },
                label = { Text(status) },
                modifier = Modifier.animateItem(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SalesRegisterRowItem(
    row: SalesRegisterRow,
    onClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    isSelected: Boolean
) {
    val statusColor = when (row.status.uppercase()) {
        "POSTED" -> Color(0xFF4CAF50)
        "CANCELLED" -> Color(0xFFF44336)
        "DRAFT" -> Color(0xFFFF9800)
        "CONVERTED" -> Color(0xFF2196F3)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox
            if (row.isCancellable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle() }
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = row.documentNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.documentDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = row.customerName,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.secondaryInfo.isNotEmpty()) {
                    Text(
                        text = row.secondaryInfo,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!row.creditNoteNumber.isNullOrBlank()) {
                    Text(
                        text = "Credit Note: ${row.creditNoteNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB42318),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Amount
                row.amount?.let { amount ->
                    Text(
                        text = "₹${String.format("%.2f", amount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Status Chip with Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Edit Button
                    if (row.isEditable && row.status.uppercase() != "CANCELLED") {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Cancel Button
                    if (row.isCancellable && row.status.uppercase() != "CANCELLED") {
                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF44336)
                            )
                        }
                    }
                    // Delete Button (Soft Delete Draft Only)
                    if (row.status.uppercase() == "DRAFT") {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFB42318)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = row.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CancelDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isRunning: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Document") },
        text = {
            Column {
                Text("Are you sure you want to cancel this document?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Cancellation Reason *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    enabled = !isRunning
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = reason.trim().isNotEmpty() && !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Cancel Document")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRunning
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onPdfExport: () -> Unit,
    onExcelExport: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Register") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Choose export format:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onPdfExport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                    Button(
                        onClick = onExcelExport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel")
                    }
                    Button(
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = null
    )
}