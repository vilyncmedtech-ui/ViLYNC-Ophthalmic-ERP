package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader

@Composable
fun PaymentDetailScreen(
    transactionId: Long,
    viewModel: PaymentViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    LaunchedEffect(transactionId) {
        viewModel.loadTransactionDetail(transactionId)
    }

    val tx = uiState.selectedTransaction

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = if (tx?.type == "CUSTOMER_RECEIPT") "Receipt Detail" else "Payment Detail",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { /* TODO: Payment Voucher Print if needed */ },
                onPdf = { /* TODO: Payment Voucher PDF if needed */ }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FD))
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading || tx == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (tx.documentNumber != null) {
                            DetailRowItem("Receipt No.", tx.documentNumber, isProminent = true)
                            HorizontalDivider(color = Color(0xFFF2F4F7))
                        }
                        DetailRowItem("Transaction ID", tx.id.toString())
                        DetailRowItem("Date", tx.transactionDate)
                        DetailRowItem("Type", tx.type)
                        DetailRowItem("Party", uiState.selectedPartyName)
                        DetailRowItem("Account", uiState.selectedAccountName)
                        DetailRowItem("Amount", "₹ %.2f".format(tx.amount))
                        DetailRowItem("Reference", tx.referenceNumber.ifBlank { "N/A" })
                        DetailRowItem("Status", tx.status)
                        DetailRowItem("Remarks", tx.remarks.ifBlank { "N/A" })
                    }
                }

                if (tx.status == "POSTED") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                val navType = if (tx.type == "CUSTOMER_RECEIPT") "RECEIPT" else "PAYMENT"
                                viewModel.loadForEdit(transactionId)
                                onEdit(transactionId, navType)
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476EA8))
                        ) {
                            Text("EDIT TRANSACTION", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Bold)
                        }
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
                        viewModel.cancel(transactionId, cancelReason)
                        showCancelDialog = false
                    },
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("YES, CANCEL", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("KEEP")
                }
            }
        )
    }
}

@Composable
private fun DetailRowItem(label: String, value: String, isProminent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF667085), fontSize = if (isProminent) 16.sp else 14.sp, fontWeight = if (isProminent) FontWeight.Medium else FontWeight.Normal)
        Text(value, fontWeight = FontWeight.Bold, fontSize = if (isProminent) 18.sp else 14.sp, color = Color(0xFF14233C))
    }
}
