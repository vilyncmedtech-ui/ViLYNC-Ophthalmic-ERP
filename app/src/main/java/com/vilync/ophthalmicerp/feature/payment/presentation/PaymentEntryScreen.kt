package com.vilync.ophthalmicerp.feature.payment.presentation

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun PaymentEntryScreen(
    type: String, // RECEIPT or PAYMENT
    viewModel: PaymentViewModel,
    fyStart: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year)
            viewModel.updateDate(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (uiState.saveSuccess) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Transaction saved successfully", Toast.LENGTH_LONG).show()
            viewModel.clearSaveSuccess()
            onBack()
        }
    }

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
                if (type == "RECEIPT") "Customer Receipt" else "Supplier Payment",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Party Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PARTY DETAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF667085))
                
                var partyQuery by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = partyQuery,
                        onValueChange = { 
                            partyQuery = it
                            viewModel.searchParty(it, type)
                            expanded = true
                        },
                        label = { Text(if (type == "RECEIPT") "Search Customer" else "Search Supplier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded && uiState.filteredParties.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.filteredParties.forEach { party ->
                            DropdownMenuItem(
                                text = { Text(party.partyName) },
                                onClick = {
                                    viewModel.selectParty(party, type)
                                    partyQuery = party.partyName
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (uiState.selectedPartyId != 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF3F2), RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Outstanding", color = Color(0xFFB42318), fontWeight = FontWeight.SemiBold)
                        Text("₹ %.2f".format(uiState.outstanding), color = Color(0xFFB42318), fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("OUTSTANDING INVOICES (REFERENCE ONLY)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF667085))
                    
                    if (type == "RECEIPT") {
                        uiState.invoices.filter { it.status == "POSTED" }.forEach { invoice ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.updateAmount(invoice.totalAmount.toString()) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Inv: ${invoice.invoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("₹ %.2f".format(invoice.totalAmount), fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        uiState.bills.forEach { bill ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.updateAmount(bill.grandTotal.toString()) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bill: ${bill.invoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("₹ %.2f".format(bill.grandTotal), fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transaction Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TRANSACTION DETAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF667085))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.date,
                        onValueChange = {},
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Text("📅")
                            }
                        }
                    )
                    
                    var accountExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedAccount = uiState.accounts.find { it.id == uiState.selectedAccountId }
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "Select Account",
                            onValueChange = {},
                            label = { Text("Cash / Bank") },
                            modifier = Modifier.fillMaxWidth().clickable { accountExpanded = true },
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.Gray
                            )
                        )
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            uiState.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        viewModel.updateAccount(account.id)
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.reference,
                    onValueChange = viewModel::updateReference,
                    label = { Text("Reference (Cheque/UTR)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = viewModel::updateRemarks,
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.errorMessage != null) {
            Text(uiState.errorMessage!!, color = Color.Red, fontSize = 14.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.save(type, false, fyStart) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476EA8))
            ) {
                Text("SAVE DRAFT", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.save(type, true, fyStart) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14945A))
            ) {
                Text("POST TRANSACTION", fontWeight = FontWeight.Bold)
            }
        }
    }
}
