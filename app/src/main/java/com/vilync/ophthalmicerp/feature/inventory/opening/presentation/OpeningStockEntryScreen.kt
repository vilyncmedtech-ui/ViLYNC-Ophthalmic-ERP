package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun OpeningStockEntryScreen(
    stockId: Long,
    viewModel: OpeningStockViewModel,
    onAddItemClick: () -> Unit,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stockId) {
        if (stockId == 0L) {
            viewModel.startNewEntry()
        } else {
            viewModel.loadEntry(stockId)
        }
    }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year)
            viewModel.updateEntryDate(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FD))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                if (uiState.isEditMode) "Edit Opening Stock" else "New Opening Stock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            OutlinedButton(onClick = onDashboard) {
                Text("⌂ Dashboard")
            }
        }

        if (uiState.isEntryLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEADER DETAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF667085))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val displayEntryNumber = if (uiState.entryNumber.startsWith("DRAFT-", ignoreCase = true)) {
                            "Draft (Unassigned)"
                        } else {
                            uiState.entryNumber
                        }
                        
                        OutlinedTextField(
                            value = displayEntryNumber,
                            onValueChange = {},
                            label = { Text("Entry Number") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F4F7),
                                unfocusedContainerColor = Color(0xFFF2F4F7)
                            )
                        )
                        OutlinedTextField(
                            value = uiState.entryDate,
                            onValueChange = {},
                            label = { Text("Entry Date *") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                if (!uiState.isPosted && !uiState.isCancelled) {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Text("📅")
                                    }
                                }
                            }
                        )
                    }

                    OutlinedTextField(
                        value = uiState.remarks,
                        onValueChange = viewModel::updateRemarks,
                        label = { Text("Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.isPosted || uiState.isCancelled
                    )
                }
            }

            // Items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LINE ITEMS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF667085))
                        if (!uiState.isPosted && !uiState.isCancelled) {
                            TextButton(onClick = onAddItemClick) {
                                Text("+ Add Product")
                            }
                        }
                    }

                    if (uiState.items.isEmpty()) {
                        Text("No items added.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        uiState.items.forEachIndexed { index, item ->
                            ItemRow(
                                index = index,
                                item = item, 
                                showRemove = !uiState.isPosted && !uiState.isCancelled
                            ) {
                                viewModel.removeItem(index)
                            }
                        }
                    }
                }
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3FAF5))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL VALUE", fontWeight = FontWeight.Bold, color = Color(0xFF14233C))
                    val total = uiState.items.sumOf { it.totalCost }
                    Text("₹ %.2f".format(total), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF027A48))
                }
            }

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }

            if (showCancelDialog) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    title = { Text("Cancel Opening Stock") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Are you sure you want to cancel this entry? This will reverse all physical inventory units.")
                            OutlinedTextField(
                                value = cancelReason,
                                onValueChange = { cancelReason = it },
                                label = { Text("Reason for cancellation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (cancelReason.isNotBlank()) {
                                    viewModel.cancel(cancelReason)
                                    showCancelDialog = false
                                }
                            },
                            enabled = cancelReason.isNotBlank()
                        ) {
                            Text("YES, CANCEL", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = false }) {
                            Text("KEEP DOCUMENT")
                        }
                    }
                )
            }

            // Actions
            if (!uiState.isPosted && !uiState.isCancelled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.saveDraft() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476EA8))
                    ) {
                        Text("SAVE DRAFT")
                    }
                    Button(
                        onClick = { viewModel.post() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14945A))
                    ) {
                        Text("POST TO STOCK")
                    }
                }
            } else if (uiState.isPosted) {
                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
                ) {
                    Text("CANCEL DOCUMENT")
                }
            }
        }
    }
}

@Composable
private fun ItemRow(index: Int, item: OpeningStockUiItem, showRemove: Boolean, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD9DEE8))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}. ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF476EA8)
            )
            
            Text(
                modifier = Modifier.weight(1f),
                text = buildString {
                    append(item.productName)
                    if (item.model.isNotBlank()) append(" | ${item.model}")
                    if (item.power.isNotBlank()) append(" | ${item.power}")
                    append(" | Qty ${item.quantity}")
                    
                    if (item.trackingType == "SERIAL") {
                        append(" | SN: ${item.serialNumber}")
                    } else if (item.batchNumber.isNotBlank()) {
                        append(" | Batch: ${item.batchNumber}")
                    }
                    
                    if (item.expiryDate.isNotBlank()) {
                        append(" | EXP ${item.expiryDate}")
                    }
                    
                    append(" | Rate ₹${String.format(Locale.getDefault(), "%.0f", item.unitCost)}")
                    append(" | GST ${item.gstPercent}%")
                    append(" | Total ₹${String.format(Locale.getDefault(), "%.2f", item.totalCost)}")
                },
                fontSize = 12.sp,
                color = Color(0xFF2C3850),
                lineHeight = 15.sp
            )

            if (showRemove) {
                IconButton(
                    onClick = onRemove, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
