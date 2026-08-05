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

@Composable
fun PaymentDetailScreen(
    transactionId: Long,
    viewModel: PaymentViewModel,
    onBack: () -> Unit,
    onDuplicate: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    LaunchedEffect(transactionId) {
        viewModel.loadTransactionDetail(transactionId)
    }

    val tx = uiState.selectedTransaction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FD))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
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
                "Transaction Details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

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
                    DetailRowItem("Transaction No.", tx.id.toString())
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
                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
                ) {
                    Text("CANCEL TRANSACTION", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { 
                    viewModel.duplicateTransaction(transactionId)
                    onDuplicate(transactionId)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476EA8))
            ) {
                Text("DUPLICATE AS DRAFT", fontWeight = FontWeight.Bold)
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
private fun DetailRowItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF667085), fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF14233C))
    }
}
