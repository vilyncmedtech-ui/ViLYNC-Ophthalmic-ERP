package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.feature.sales.register.SalesRegisterType
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF071B33)
private val Gold = Color(0xFFD4AF37)
private val Page = Color(0xFFF7F9FC)

@Composable
fun NewCreditNoteScreen(
    viewModel: NewCreditNoteViewModel,
    onBack: () -> Unit,
    onOpenRegister: () -> Unit,
    onSavedToDetail: (Long) -> Unit = {}
) {
    val state = viewModel.uiState.collectAsState().value
    val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val isEditMode = state.editingId != null

    androidx.compose.runtime.LaunchedEffect(state.savedCreditNoteId) {
        if (state.savedCreditNoteId != null) {
            onSavedToDetail(state.savedCreditNoteId!!)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Text(
                text = if (isEditMode) "Edit Credit Note" else "New Credit Note",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            OutlinedButton(onClick = onOpenRegister) { Text("Credit Note Register") }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Page),
            border = BorderStroke(1.dp, Gold.copy(alpha = .55f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Original Sales Invoice", fontWeight = FontWeight.Bold, color = Navy)
                OutlinedTextField(
                    value = state.invoiceQuery,
                    onValueChange = viewModel::updateInvoiceQuery,
                    label = { Text("Search Invoice No. / Customer / Date") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = isEditMode,
                    enabled = !isEditMode
                )
                if (!isEditMode && (state.selectedInvoice == null || state.invoices.isNotEmpty())) {
                    state.invoices.take(8).forEach { sale ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectInvoice(sale) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFDDE3EC))
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, color = Navy)
                                Text("${sale.invoiceDate} • ${sale.customerName}")
                                Text(money.format(sale.totalAmount), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                state.selectedInvoice?.let { sale ->
                    Text("Selected: ${sale.invoiceNumber} • ${sale.customerName}", fontWeight = FontWeight.SemiBold, color = Navy)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.creditNoteNumber.ifBlank { 
                    if (state.selectedInvoice == null) "Assigning on selection..." else "Auto-assigning..."
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Credit Note Number") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.creditNoteDate,
                onValueChange = viewModel::updateCreditNoteDate,
                label = { Text("Credit Note Date (DD-MM-YYYY)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Text("Credit Note Type", fontWeight = FontWeight.Bold, color = Navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.creditNoteType == "SALES_RETURN",
                onClick = { viewModel.setCreditNoteType("SALES_RETURN") },
                label = { Text("Sales Return") },
                enabled = !isEditMode // Prevent changing type in edit mode to avoid data inconsistency
            )
            FilterChip(
                selected = state.creditNoteType == "FINANCIAL_ADJUSTMENT",
                onClick = { viewModel.setCreditNoteType("FINANCIAL_ADJUSTMENT") },
                label = { Text("Financial Adjustment") },
                enabled = !isEditMode
            )
        }

        if (state.creditNoteType == "SALES_RETURN") {
            Text("Select physical sold serial(s) to return to stock", fontWeight = FontWeight.Bold, color = Navy)
            if (state.isLoadingInvoice) {
                CircularProgressIndicator()
            } else if (state.selectedInvoice != null && state.invoiceLines.isEmpty()) {
                Text("No serial-tracked items found on this invoice.")
            } else {
                state.invoiceLines.forEach { line ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleLine(line.saleLensId) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (line.selected) Gold else Color(0xFFDDE3EC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(checked = line.selected, onCheckedChange = { viewModel.toggleLine(line.saleLensId) })
                            Column(Modifier.weight(1f)) {
                                Text(line.productName, fontWeight = FontWeight.Bold, color = Navy)
                                Text("Serial: ${line.serialNumber}   Power: ${line.power}")
                                if (line.batchNumber.isNotBlank()) Text("Batch: ${line.batchNumber}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(money.format(line.totalAmount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = state.adjustmentAmount,
                onValueChange = viewModel::updateAdjustmentAmount,
                label = { Text("Financial Adjustment Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Financial Adjustment does not alter physical inventory.", style = MaterialTheme.typography.bodySmall, color = Navy)
        }

        OutlinedTextField(
            value = state.reason,
            onValueChange = viewModel::updateReason,
            label = { Text("Reason") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.remarks,
            onValueChange = viewModel::updateRemarks,
            label = { Text("Remarks") },
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
        state.successMessage?.let { Text(it, color = Navy, fontWeight = FontWeight.Bold) }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) {
            if (state.isSaving) CircularProgressIndicator() 
            else Text(if (isEditMode) "UPDATE CREDIT NOTE" else "SAVE CREDIT NOTE")
        }
    }
}
