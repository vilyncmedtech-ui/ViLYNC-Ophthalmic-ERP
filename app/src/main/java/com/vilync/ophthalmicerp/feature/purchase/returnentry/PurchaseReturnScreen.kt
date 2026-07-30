package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import java.util.Locale

@Composable
fun PurchaseReturnScreen(
    viewModel: PurchaseReturnViewModel,
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
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
                text = if (uiState.isEditMode) {
                    "Edit Purchase Return"
                } else {
                    "Purchase Return"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(onClick = onDashboard) {
                Text("⌂ Dashboard")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            return@Column
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        uiState.successMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PurchaseReturnHeaderCard(uiState = uiState)
            }

            item {
                PurchaseReturnDocumentFields(
                    uiState = uiState,
                    onCreditNoteNumberChange = viewModel::updateCreditNoteNumber,
                    onCreditNoteDateChange = viewModel::updateCreditNoteDate,
                    onRemarksChange = viewModel::updateRemarks
                )
            }

            item {
                Text(
                    text = "RETURN ITEMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = uiState.returnItems,
                key = { it.originalPurchaseItemId }
            ) { item ->
                PurchaseReturnItemCard(
                    item = item,
                    onQuantityChange = { quantity ->
                        viewModel.updateReturnQuantity(
                            originalPurchaseItemId = item.originalPurchaseItemId,
                            quantity = quantity
                        )
                    },
                    onLensToggle = { lens ->
                        viewModel.toggleLensSelection(
                            originalPurchaseItemId = item.originalPurchaseItemId,
                            lens = lens
                        )
                    },
                    onClear = {
                        viewModel.clearReturnItem(item.originalPurchaseItemId)
                    }
                )
            }

            item {
                PurchaseReturnTotals(
                    uiState = uiState,
                    onDiscountChange = { value ->
                        viewModel.updateDiscountAmount(
                            value.toDoubleOrNull() ?: 0.0
                        )
                    },
                    onRoundOffChange = { value ->
                        viewModel.updateRoundOff(
                            value.toDoubleOrNull() ?: 0.0
                        )
                    }
                )
            }

            item {
                Button(
                    onClick = viewModel::savePurchaseReturn,
                    enabled = !uiState.isSaving && !uiState.isSaved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            uiState.isSaving && uiState.isEditMode -> "UPDATING…"
                            uiState.isSaving -> "SAVING…"
                            uiState.isSaved && uiState.isEditMode -> "UPDATED ✓"
                            uiState.isSaved -> "SAVED ✓"
                            uiState.isEditMode -> "UPDATE PURCHASE RETURN"
                            else -> "SAVE PURCHASE RETURN"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseReturnHeaderCard(
    uiState: PurchaseReturnUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = uiState.originalInvoiceNumber.ifBlank { "Purchase Invoice" },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text("Supplier: ${uiState.supplierName.ifBlank { "-" }}")
            Text("Invoice Date: ${uiState.originalInvoiceDate.ifBlank { "-" }}")
        }
    }
}

@Composable
private fun PurchaseReturnDocumentFields(
    uiState: PurchaseReturnUiState,
    onCreditNoteNumberChange: (String) -> Unit,
    onCreditNoteDateChange: (String) -> Unit,
    onRemarksChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.creditNoteNumber,
                onValueChange = onCreditNoteNumberChange,
                label = { Text("Debit Note No. *") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.creditNoteDate,
                onValueChange = onCreditNoteDateChange,
                label = { Text("Debit Note Date *") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = uiState.remarks,
            onValueChange = onRemarksChange,
            label = { Text("Remarks") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

@Composable
private fun PurchaseReturnItemCard(
    item: PurchaseReturnItemUiState,
    onQuantityChange: (Int) -> Unit,
    onLensToggle: (PurchaseLensEntity) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.productName, fontWeight = FontWeight.SemiBold)
                    Text("Purchased: ${item.purchasedQuantity}  |  Already returned: ${item.alreadyReturnedQuantity}")
                    Text("Remaining: ${item.remainingReturnableQuantity}")
                }

                OutlinedButton(onClick = onClear) {
                    Text("Clear")
                }
            }

            OutlinedTextField(
                value = item.returnQuantity.toString(),
                onValueChange = { value ->
                    onQuantityChange(value.filter { it.isDigit() }.toIntOrNull() ?: 0)
                },
                label = { Text("Return Quantity") },
                supportingText = {
                    Text("Maximum: ${item.remainingReturnableQuantity}")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !item.isSerialControlled
            )

            if (item.isSerialControlled) {
                Text(
                    text = "Select IOL serial numbers",
                    fontWeight = FontWeight.SemiBold
                )

                item.availableLenses.forEach { lens ->
                    val selected = item.selectedLenses.any { it.id == lens.id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onLensToggle(lens) }
                        )
                        Column {
                            Text(
                                text = lens.serialNumber,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Expiry: ${lens.expiryDate}")
                        }
                    }
                }

                Text("Selected serials: ${item.selectedLensCount}")
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Return Total")
                Text(
                    text = money(item.totalAmount),
                    fontWeight = FontWeight.Bold
                )
            }

            item.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PurchaseReturnTotals(
    uiState: PurchaseReturnUiState,
    onDiscountChange: (String) -> Unit,
    onRoundOffChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F0FE)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RETURN SUMMARY",
                fontWeight = FontWeight.Bold
            )

            AmountRow("Sub Total", uiState.subTotal)
            AmountRow("GST", uiState.gstAmount)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.discountAmount.toString(),
                    onValueChange = onDiscountChange,
                    label = { Text("Discount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.roundOff.toString(),
                    onValueChange = onRoundOffChange,
                    label = { Text("Round Off") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            HorizontalDivider()
            AmountRow("TOTAL RETURN", uiState.totalAmount, bold = true)
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = money(amount),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun money(value: Double): String {
    return String.format(
        Locale.getDefault(),
        "₹%.2f",
        value
    )
}
