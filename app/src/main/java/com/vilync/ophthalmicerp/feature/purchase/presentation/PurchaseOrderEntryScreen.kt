package com.vilync.ophthalmicerp.feature.purchase.presentation

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PurchaseOrderEntryScreen(
    viewModel: PurchaseOrderViewModel,
    onAddProductClick: () -> Unit = {},
    onEditProductClick: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val vendors by viewModel.vendors.collectAsState()
    val context = LocalContext.current
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestExit(onExit: () -> Unit) {
        if (uiState.isDirty) {
            pendingExitAction = onExit
            showDiscardChangesDialog = true
        } else {
            onExit()
        }
    }

    BackHandler { requestExit(onBack) }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            Toast.makeText(context, "Purchase Order ${if (uiState.isEditMode) "updated" else "saved"} successfully.", Toast.LENGTH_LONG).show()
            viewModel.clearSaveSuccess()
        }
    }

    val orderCalendar = Calendar.getInstance()
    val orderDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.updatePoDate(String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year))
        },
        orderCalendar.get(Calendar.YEAR), orderCalendar.get(Calendar.MONTH), orderCalendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VilyncPageBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { requestExit(onBack) }) { Text("← Back") }
            Text(
                text = if (uiState.isEditMode) "Edit Purchase Order" else "New Purchase Order",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )
            OutlinedButton(onClick = { requestExit(onDashboard) }) { Text("⌂ Dashboard") }
        }

        if (showDiscardChangesDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardChangesDialog = false },
                title = { Text("Discard unsaved changes?") },
                text = { Text("Your changes to this Purchase Order have not been saved.") },
                confirmButton = { TextButton(onClick = { showDiscardChangesDialog = false; pendingExitAction?.invoke() }) { Text("Discard") } },
                dismissButton = { TextButton(onClick = { showDiscardChangesDialog = false }) { Text("Stay") } }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VilyncPastelBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, VilyncSoftBlueBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ORDER DETAILS", fontWeight = FontWeight.Bold, color = VilyncNavy)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VendorDropdown(
                        selectedVendorName = uiState.supplierName,
                        vendors = vendors,
                        onVendorSelected = viewModel::selectVendor,
                        modifier = Modifier.weight(1.5f)
                    )
                    PurchaseTextField(
                        value = uiState.poNumber,
                        onValueChange = {},
                        label = "Purchase Order No.",
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DatePurchaseField(
                        value = uiState.poDate,
                        label = "Order Date *",
                        onClick = { orderDatePicker.show() },
                        modifier = Modifier.weight(1f)
                    )
                    PurchaseTextField(
                        value = uiState.reference,
                        onValueChange = viewModel::updateReference,
                        label = "Reference / Remarks",
                        modifier = Modifier.weight(2f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VilyncLavender),
            border = androidx.compose.foundation.BorderStroke(1.dp, VilyncLavenderBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PRODUCTS", fontWeight = FontWeight.Bold, color = VilyncNavy)
                if (uiState.items.isEmpty()) {
                    Button(onClick = onAddProductClick, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("+ Add Product") }
                } else {
                    uiState.items.forEachIndexed { index, item ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f))) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, fontWeight = FontWeight.SemiBold)
                                    Text("Power: ${item.power} | Qty: ${item.quantity} | Rate: ₹${money(item.purchaseRate)}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { onEditProductClick(index) }) { Text("✎", fontSize = 18.sp) }
                                    IconButton(onClick = { viewModel.removeItem(index) }) { Text("✕", color = Color.Red, fontSize = 18.sp) }
                                }
                            }
                        }
                    }
                    Button(onClick = onAddProductClick, modifier = Modifier.align(Alignment.End)) { Text("+ Add Product") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VilyncBillGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, VilyncGreenBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ORDER SUMMARY", fontWeight = FontWeight.Bold, color = VilyncNavy)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(label = "Taxable", amount = uiState.taxableAmount, modifier = Modifier.weight(1f))
                    SummaryCard(label = "GST", amount = uiState.taxAmount, modifier = Modifier.weight(1f))
                    NetAmountCard(amount = uiState.netAmount, modifier = Modifier.weight(1.5f))
                }
            }
        }

        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.saveOrder() },
            enabled = !uiState.isSaving && !(uiState.isSaved && !uiState.isDirty),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(if (uiState.isSaving) "SAVING..." else if (uiState.isSaved && !uiState.isDirty) "ORDER SAVED ✓" else if (uiState.isEditMode) "UPDATE ORDER" else "SAVE PURCHASE ORDER")
        }
    }
}

@Composable
private fun VendorDropdown(
    selectedVendorName: String,
    vendors: List<com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster>,
    onVendorSelected: (com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(selectedVendorName.ifBlank { "Select Vendor *" }, modifier = Modifier.weight(1f))
            Text("▼")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vendors.forEach { vendor ->
                DropdownMenuItem(text = { Text(vendor.partyName) }, onClick = { onVendorSelected(vendor); expanded = false })
            }
        }
    }
}

@Composable
private fun DatePurchaseField(value: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
        trailingIcon = { IconButton(onClick = onClick) { Text("📅") } },
        modifier = modifier.heightIn(min = 64.dp).clickable { onClick() }
    )
}

@Composable
private fun PurchaseTextField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next, readOnly: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }, onDone = { focusManager.clearFocus() }),
        modifier = modifier.heightIn(min = 64.dp).bringIntoViewRequester(bringIntoViewRequester).onFocusChanged {
            if (it.isFocused) scope.launch { delay(200); bringIntoViewRequester.bringIntoView() }
        }
    )
}

@Composable
private fun SummaryCard(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f))) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("₹${money(amount)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NetAmountCard(amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFFDDF4E7))) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("NET TOTAL", style = MaterialTheme.typography.bodySmall)
            Text("₹${money(amount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

private fun money(value: Double) = String.format(Locale.getDefault(), "%.2f", value)

private val VilyncPageBackground = Color(0xFFF7F9FD)
private val VilyncNavy = Color(0xFF14233C)
private val VilyncPastelBlue = Color(0xFFF2F7FF)
private val VilyncSoftBlueBorder = Color(0xFFD5E3F7)
private val VilyncLavender = Color(0xFFEDE8FC)
private val VilyncLavenderBorder = Color(0xFFDDD3F5)
private val VilyncBillGreen = Color(0xFFF3FAF5)
private val VilyncGreenBorder = Color(0xFFD5E9DA)
