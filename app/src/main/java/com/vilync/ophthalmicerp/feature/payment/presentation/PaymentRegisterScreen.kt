package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.dao.FinancialTransactionRow
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository

@Composable
fun PaymentRegisterScreen(
    type: String, // RECEIPT or PAYMENT
    viewModel: PaymentViewModel,
    repository: FinancialTransactionRepository,
    onBack: () -> Unit,
    onViewDetail: (Long) -> Unit,
    onEditDraft: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactionRows by repository.getAllTransactionRows().collectAsState(initial = emptyList())
    
    val filteredTransactions = remember(transactionRows, type, uiState.registerSearchQuery, uiState.statusFilter) {
        val mappedType = if (type == "RECEIPT") "CUSTOMER_RECEIPT" else "SUPPLIER_PAYMENT"
        transactionRows.filter { row ->
            val tx = row.transaction
            tx.type == mappedType &&
            (uiState.statusFilter == "All" || tx.status == uiState.statusFilter) &&
            (uiState.registerSearchQuery.isBlank() || 
             (tx.documentNumber?.contains(uiState.registerSearchQuery, ignoreCase = true) == true) ||
             tx.referenceNumber.contains(uiState.registerSearchQuery, ignoreCase = true) ||
             (row.partyName?.contains(uiState.registerSearchQuery, ignoreCase = true) == true) ||
             tx.remarks.contains(uiState.registerSearchQuery, ignoreCase = true))
        }
    }
    
    var selectedTx by remember { mutableStateOf<FinancialTransactionEntity?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FD))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }
            Text(
                if (type == "RECEIPT") "Receipt Register" else "Payment Register",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Search and Filters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.registerSearchQuery,
                    onValueChange = viewModel::updateRegisterSearch,
                    label = { Text("Search Ref / Remarks") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                var statusExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { statusExpanded = true }) {
                        Text("Status: ${uiState.statusFilter}")
                    }
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        listOf("All", "POSTED", "DRAFT", "CANCELLED").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    viewModel.updateStatusFilter(status)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Transaction") },
                text = {
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason for cancellation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedTx?.let { viewModel.cancel(it.id, cancelReason) }
                            showCancelDialog = false
                        },
                        enabled = cancelReason.isNotBlank()
                    ) {
                        Text("CANCEL", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("CLOSE")
                    }
                }
            )
        }

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions) { row ->
                    TransactionCard(
                        row = row, 
                        onClick = { onViewDetail(row.transaction.id) }, 
                        onLongClick = {
                            if (row.transaction.status == "POSTED") {
                                selectedTx = row.transaction
                                showCancelDialog = true
                            }
                        },
                        onEditDraft = onEditDraft
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionCard(
    row: FinancialTransactionRow, 
    onClick: () -> Unit, 
    onLongClick: () -> Unit,
    onEditDraft: (Long) -> Unit
) {
    val tx = row.transaction
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (tx.status == "CANCELLED") Color(0xFFF2F2F2) else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Receipt No / Ref
                RegisterField(
                    label = if (tx.type == "CUSTOMER_RECEIPT") "Receipt No." else "Ref",
                    value = if (tx.type == "CUSTOMER_RECEIPT") (tx.documentNumber ?: "Pending") else tx.referenceNumber.ifBlank { "N/A" },
                    modifier = Modifier.weight(1.3f)
                )

                // 2. Party Name
                RegisterField(
                    label = "Party",
                    value = row.partyName ?: "Unknown",
                    modifier = Modifier.weight(2.5f),
                    valueFontWeight = FontWeight.SemiBold
                )

                // 3. Payment Date
                RegisterField(
                    label = "Date",
                    value = tx.transactionDate,
                    modifier = Modifier.weight(1.2f)
                )

                // 4. Payment Amount
                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Amount", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        "₹ %.2f".format(tx.amount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (tx.status == "CANCELLED") Color.Gray else if (tx.type == "CUSTOMER_RECEIPT") Color(0xFF027A48) else Color(0xFFB42318)
                    )
                    if (tx.status == "CANCELLED") {
                        Text("CANCELLED", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("⋮", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("View Details") }, onClick = { onClick(); menuExpanded = false })
                    if (tx.status == "DRAFT" || tx.status == "POSTED") {
                        DropdownMenuItem(text = { Text("Edit / Reopen") }, onClick = { onEditDraft(tx.id); menuExpanded = false })
                    }
                    if (tx.status == "POSTED") {
                        DropdownMenuItem(text = { Text("Cancel") }, onClick = { onLongClick(); menuExpanded = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueFontWeight: FontWeight = FontWeight.Bold
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = valueFontWeight,
            color = Color(0xFF14233C),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
