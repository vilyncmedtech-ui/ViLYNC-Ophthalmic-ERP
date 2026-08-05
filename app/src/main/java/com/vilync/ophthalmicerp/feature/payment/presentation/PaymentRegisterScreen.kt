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
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository

@Composable
fun PaymentRegisterScreen(
    type: String, // RECEIPT or PAYMENT
    viewModel: PaymentViewModel,
    repository: FinancialTransactionRepository,
    onBack: () -> Unit,
    onViewDetail: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onEditDraft: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by repository.getAllTransactions().collectAsState(initial = emptyList())
    
    val filteredTransactions = remember(transactions, type, uiState.registerSearchQuery, uiState.statusFilter) {
        val mappedType = if (type == "RECEIPT") "CUSTOMER_RECEIPT" else "SUPPLIER_PAYMENT"
        transactions.filter { tx ->
            tx.type == mappedType &&
            (uiState.statusFilter == "All" || tx.status == uiState.statusFilter) &&
            (uiState.registerSearchQuery.isBlank() || 
             tx.referenceNumber.contains(uiState.registerSearchQuery, ignoreCase = true) ||
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
                items(filteredTransactions) { tx ->
                    TransactionCard(
                        tx = tx, 
                        onClick = { onViewDetail(tx.id) }, 
                        onLongClick = {
                            if (tx.status == "POSTED") {
                                selectedTx = tx
                                showCancelDialog = true
                            }
                        },
                        onDuplicate = onDuplicate,
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
    tx: FinancialTransactionEntity, 
    onClick: () -> Unit, 
    onLongClick: () -> Unit,
    onDuplicate: (Long) -> Unit,
    onEditDraft: (Long) -> Unit
) {
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Date: ${tx.transactionDate}", fontSize = 12.sp, color = Color.Gray)
                Text("Ref: ${tx.referenceNumber.ifBlank { "N/A" }}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (tx.status == "CANCELLED") {
                    Text("CANCELLED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text(
                "₹ %.2f".format(tx.amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (tx.status == "CANCELLED") Color.Gray else if (tx.type == "CUSTOMER_RECEIPT") Color(0xFF027A48) else Color(0xFFB42318)
            )

            Spacer(modifier = Modifier.width(8.dp))
            
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("⋮", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("View Details") }, onClick = { onClick(); menuExpanded = false })
                    if (tx.status == "DRAFT") {
                        DropdownMenuItem(text = { Text("Edit / Reopen") }, onClick = { onEditDraft(tx.id); menuExpanded = false })
                    }
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { onDuplicate(tx.id); menuExpanded = false })
                    if (tx.status == "POSTED") {
                        DropdownMenuItem(text = { Text("Cancel") }, onClick = { onLongClick(); menuExpanded = false })
                    }
                }
            }
        }
    }
}
