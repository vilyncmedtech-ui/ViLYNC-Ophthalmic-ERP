package com.vilync.ophthalmicerp.feature.sales.proforma.presentation

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import com.vilync.ophthalmicerp.feature.sales.proforma.export.ProformaInvoiceExportSuite
import com.vilync.ophthalmicerp.ui.components.PartySearchField
import com.vilync.ophthalmicerp.ui.components.PrintExportActionBar
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProformaInvoiceScreen(
    viewModel: ProformaInvoiceViewModel,
    onBack: () -> Unit,
    onSavedToDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(uiState.isSaved, uiState.savedProformaId) {
        if (uiState.isSaved && uiState.savedProformaId != null) {
            onSavedToDetail(uiState.savedProformaId!!)
            viewModel.clearMessage()
        }
    }

    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d -> viewModel.updateDate("${d}-${m + 1}-$y") },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val validUntilPicker = DatePickerDialog(
        context,
        { _, y, m, d -> viewModel.updateValidUntil("${d}-${m + 1}-$y") },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val proforma = ProformaInvoiceEntity(
                proformaNumber = uiState.proformaNumber,
                normalizedProformaNumber = uiState.proformaNumber.uppercase(),
                proformaDate = uiState.proformaDate,
                taxableAmount = uiState.taxableAmount,
                gstAmount = uiState.gstAmount,
                totalAmount = uiState.totalAmount,
                financialYearStart = uiState.financialYearStart,
                customerId = 0, customerName = "", createdAt = 0, updatedAt = 0
            )
            val items = uiState.items.map {
                ProformaInvoiceItemEntity(
                    proformaInvoiceId = 0, productId = 0, productName = it.productName, power = it.power,
                    quantity = it.quantity, rate = it.rate, gstPercent = it.gstPercent,
                    taxableAmount = it.taxableAmount, totalAmount = it.totalAmount
                )
            }
            ProformaInvoiceExportSuite.exportPdf(context, it, proforma, items)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved && !uiState.isEditMode) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Proforma: ${uiState.proformaNumber}" else "New Proforma") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (uiState.isSaved) {
                        PrintExportActionBar(
                            onPrint = {
                                val proforma = ProformaInvoiceEntity(
                                    proformaNumber = uiState.proformaNumber,
                                    normalizedProformaNumber = uiState.proformaNumber.uppercase(),
                                    proformaDate = uiState.proformaDate,
                                    taxableAmount = uiState.taxableAmount,
                                    gstAmount = uiState.gstAmount,
                                    totalAmount = uiState.totalAmount,
                                    financialYearStart = uiState.financialYearStart,
                                    customerId = 0, customerName = "", createdAt = 0, updatedAt = 0
                                )
                                val items = uiState.items.map {
                                    ProformaInvoiceItemEntity(
                                        proformaInvoiceId = 0, productId = 0, productName = it.productName, power = it.power,
                                        quantity = it.quantity, rate = it.rate, gstPercent = it.gstPercent,
                                        taxableAmount = it.taxableAmount, totalAmount = it.totalAmount
                                    )
                                }
                                ProformaInvoiceExportSuite.print(context, proforma, items)
                            },
                            onExportPdf = { pdfLauncher.launch("Proforma_${uiState.proformaNumber.replace("/", "_")}.pdf") },
                            onExportExcel = { /* Excel handled by Register */ }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = if (uiState.isEditMode) uiState.proformaNumber else "Auto-generated",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proforma Number") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.proformaDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f).clickable { datePicker.show() },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    
                    OutlinedTextField(
                        value = uiState.validUntilDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Valid Until") },
                        modifier = Modifier.fillMaxWidth().clickable { validUntilPicker.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    PartySearchField(
                        label = "Customer / Hospital *",
                        selectedParty = uiState.selectedCustomer,
                        allParties = uiState.customers,
                        onPartySelected = { viewModel.selectCustomer(it) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.isSaved
                    )
                }
            }

            // Declaration
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "This is a Proforma Invoice. This document is not a Tax Invoice. No tax liability arises from this document.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF856404)
                )
            }

            // Items
            Text("ITEMS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            uiState.items.forEachIndexed { index, item ->
                ListItem(
                    headlineContent = { Text(item.productName) },
                    supportingContent = { Text("Qty: ${item.quantity} • Rate: ₹${item.rate} • GST: ${item.gstPercent}%") },
                    trailingContent = {
                        if (!uiState.isSaved) {
                            IconButton(onClick = { viewModel.removeItem(index) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = Color.Red)
                            }
                        }
                    }
                )
            }

            if (!uiState.isSaved) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Product")
                }
            }

            // Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9ECEF))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Taxable Value", uiState.taxableAmount)
                    SummaryRow("GST Estimate", uiState.gstAmount)
                    HorizontalDivider()
                    SummaryRow("Grand Total", uiState.totalAmount, isBold = true)
                }
            }

            OutlinedTextField(
                value = uiState.remarks,
                onValueChange = viewModel::updateRemarks,
                label = { Text("Remarks / Terms") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                readOnly = uiState.isSaved
            )

            if (!uiState.isSaved) {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) CircularProgressIndicator(color = Color.White)
                    else Text("SAVE PROFORMA")
                }
            }
            
            uiState.successMessage?.let {
                Text(it, color = Color(0xFF28A745), fontWeight = FontWeight.Bold)
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            products = uiState.products,
            onDismiss = { showAddDialog = false },
            onAdd = { p, q, r -> viewModel.addProduct(p, q, r); showAddDialog = false }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: Double, isBold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text("₹ %.2f".format(value), fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AddProductDialog(
    products: List<com.vilync.ophthalmicerp.data.entity.ProductEntity>,
    onDismiss: () -> Unit,
    onAdd: (com.vilync.ophthalmicerp.data.entity.ProductEntity, Int, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<com.vilync.ophthalmicerp.data.entity.ProductEntity?>(null) }
    var qty by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = selectedProduct?.productName ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.productName) },
                                onClick = { 
                                    selectedProduct = product
                                    rate = product.retailPrice.toString()
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    selectedProduct?.let { 
                        onAdd(it, qty.toIntOrNull() ?: 1, rate.toDoubleOrNull() ?: 0.0) 
                    }
                },
                enabled = selectedProduct != null
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
