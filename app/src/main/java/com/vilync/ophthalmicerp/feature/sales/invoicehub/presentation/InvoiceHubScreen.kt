package com.vilync.ophthalmicerp.feature.sales.invoicehub.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHubScreen(
    viewModel: InvoiceHubViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    fun showDatePicker(currentDate: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        try {
            val parts = currentDate.split("-")
            if (parts.size == 3) {
                calendar.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                calendar.set(Calendar.YEAR, parts[2].toInt())
            }
        } catch (e: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formatted = String.format(Locale.US, "%02d-%02d-%04d", day, month + 1, year)
                onDateSelected(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Invoice Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.selectedIds.isNotEmpty()) {
                        Text(
                            text = "${state.selectedIds.size} Selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.exportSelected(context) },
                        enabled = state.selectedIds.isNotEmpty() && !state.isExporting,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F9FF))
        ) {
            // Filters
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box {
                        OutlinedTextField(
                            value = state.partySearch,
                            onValueChange = viewModel::updatePartySearch,
                            placeholder = { Text("Search Party Name / Invoice No. / Serial No.", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (state.partySuggestions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { viewModel.selectParty(state.selectedParty) }, // Just close
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                state.partySuggestions.forEach { party ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(party.partyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (party.gstin.isNotBlank()) {
                                                    Text("GST: ${party.gstin}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = { viewModel.selectParty(party) }
                                    )
                                }
                            }
                        }
                    }

                    if (state.selectedParty != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InputChip(
                            selected = true,
                            onClick = { viewModel.selectParty(null) },
                            label = { Text(state.selectedParty!!.partyName) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.dateFrom,
                                onValueChange = {},
                                label = { Text("From Date", fontSize = 11.sp) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker(state.dateFrom, viewModel::updateDateFrom) })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.dateTo,
                                onValueChange = {},
                                label = { Text("To Date", fontSize = 11.sp) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker(state.dateTo, viewModel::updateDateTo) })
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = state.statusFilter == "POSTED",
                                onClick = { viewModel.updateStatusFilter("POSTED") },
                                label = { Text("Posted", fontSize = 12.sp) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = state.statusFilter == "CANCELLED",
                                onClick = { viewModel.updateStatusFilter("CANCELLED") },
                                label = { Text("Cancelled", fontSize = 12.sp) }
                            )
                        }
                        
                        Row {
                            TextButton(onClick = viewModel::selectAll) { Text("Select All", fontSize = 12.sp) }
                            TextButton(onClick = viewModel::clearSelection) { Text("Clear", fontSize = 12.sp) }
                        }
                    }
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.invoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No invoices found", color = Color.Gray)
                }
            } else {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(32.dp)) // Checkbox space
                    Text("Invoice No.", modifier = Modifier.weight(1.1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Party Name", modifier = Modifier.weight(1.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Status", modifier = Modifier.weight(1.0f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("Date", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.End)
                    Text("Amount", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.End)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.invoices, key = { it.id }) { sale ->
                        InvoiceHubCompactRow(
                            sale = sale,
                            isSelected = state.selectedIds.contains(sale.id),
                            onToggle = { viewModel.toggleSelection(sale.id) },
                            money = money
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
    
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { /* No-op */ },
            title = { Text("Error") },
            text = { Text(state.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.loadInvoices() }) { Text("Retry") }
            }
        )
    }
}

@Composable
private fun InvoiceHubCompactRow(
    sale: SaleEntity,
    isSelected: Boolean,
    onToggle: () -> Unit,
    money: NumberFormat
) {
    val statusColor = if (sale.status == "POSTED") Color(0xFF2E7D32) else Color(0xFFC62828)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .background(if (isSelected) Color(0xFFE8F0FE) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected, 
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = sale.invoiceNumber,
            modifier = Modifier.weight(1.1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = sale.customerName,
            modifier = Modifier.weight(1.6f).padding(horizontal = 4.dp),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.Center) {
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = sale.status, 
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }
        
        Text(
            text = sale.invoiceDate,
            modifier = Modifier.weight(0.9f),
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.End
        )
        
        Text(
            text = money.format(sale.totalAmount),
            modifier = Modifier.weight(1.2f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B5E20),
            textAlign = TextAlign.End
        )
    }
}
