package com.vilync.ophthalmicerp.feature.purchase.presentation

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.vilync.ophthalmicerp.feature.purchase.report.PurchaseInvoiceExcelExporter
import com.vilync.ophthalmicerp.feature.purchase.report.PurchaseInvoicePdfExporter
import com.vilync.ophthalmicerp.feature.purchase.report.PurchaseInvoicePrintAdapter
import com.vilync.ophthalmicerp.feature.purchase.report.PurchaseInvoiceReport

@Composable
fun PurchaseEntryScreen(
    viewModel: PurchaseViewModel,
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
    var showExportMenu by remember { mutableStateOf(false) }

    fun requestExit(onExit: () -> Unit) {
        if (uiState.isDirty) {
            pendingExitAction = onExit
            showDiscardChangesDialog = true
        } else {
            onExit()
        }
    }

    BackHandler {
        requestExit(onBack)
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            Toast.makeText(
                context,
                if (uiState.isEditMode) {
                    "Purchase updated successfully."
                } else {
                    "Purchase saved successfully."
                },
                Toast.LENGTH_LONG
            ).show()

            viewModel.clearSaveSuccess()
        }
    }

    val invoiceCalendar = Calendar.getInstance()
    val invoiceDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format(
                Locale.getDefault(),
                "%02d-%02d-%04d",
                dayOfMonth,
                month + 1,
                year
            )
            viewModel.updateInvoiceDate(selectedDate)
        },
        invoiceCalendar.get(Calendar.YEAR),
        invoiceCalendar.get(Calendar.MONTH),
        invoiceCalendar.get(Calendar.DAY_OF_MONTH)
    )

    val receivedCalendar = Calendar.getInstance()
    val receivedDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format(
                Locale.getDefault(),
                "%02d-%02d-%04d",
                dayOfMonth,
                month + 1,
                year
            )
            viewModel.updateReceivedDate(selectedDate)
        },
        receivedCalendar.get(Calendar.YEAR),
        receivedCalendar.get(Calendar.MONTH),
        receivedCalendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VilyncPageBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { requestExit(onBack) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VilyncNavy)
            ) {
                Text("← Back")
            }

            Text(
                text = if (uiState.isEditMode) "Edit Purchase Invoice" else "New Purchase Invoice",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { showExportMenu = true },
                        enabled = uiState.isSaved && !uiState.isDirty,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VilyncPrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("EXPORT ▾", fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Print") },
                            onClick = {
                                showExportMenu = false
                                PurchaseInvoicePrintAdapter.print(
                                    context,
                                    PurchaseInvoiceReport.fromUiState(uiState)
                                ).onFailure {
                                    Toast.makeText(context, it.message ?: "Unable to print.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("PDF") },
                            onClick = {
                                showExportMenu = false
                                PurchaseInvoicePdfExporter.exportAndShare(
                                    context,
                                    PurchaseInvoiceReport.fromUiState(uiState)
                                ).onFailure {
                                    Toast.makeText(context, it.message ?: "Unable to export PDF.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Excel") },
                            onClick = {
                                showExportMenu = false
                                PurchaseInvoiceExcelExporter.exportAndShare(
                                    context,
                                    PurchaseInvoiceReport.fromUiState(uiState)
                                ).onFailure {
                                    Toast.makeText(context, it.message ?: "Unable to export Excel.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { requestExit(onDashboard) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VilyncNavy)
                ) {
                    Text("⌂ Dashboard")
                }
            }
        }

        if (showDiscardChangesDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDiscardChangesDialog = false
                },
                title = {
                    Text("Discard unsaved changes?")
                },
                text = {
                    Text("Your changes to this purchase have not been saved.")
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDiscardChangesDialog = false
                            pendingExitAction = null
                        }
                    ) {
                        Text("Stay")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardChangesDialog = false
                            pendingExitAction?.invoke()
                            pendingExitAction = null
                        }
                    ) {
                        Text("Discard & Exit")
                    }
                }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VilyncPastelBlue
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VilyncSoftBlueBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "VENDOR & INVOICE DETAILS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VilyncNavy
                )

                // ROW 1: Vendor 58% | Invoice No. 42%
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VendorDropdown(
                        selectedVendorName = uiState.supplierName,
                        vendors = vendors,
                        onVendorSelected = viewModel::selectVendor,
                        modifier = Modifier.weight(58f)
                    )

                    PurchaseTextField(
                        value = uiState.invoiceNumber,
                        onValueChange = {
                            viewModel.updateInvoiceNumber(
                                it.uppercase(Locale.getDefault())
                            )
                        },
                        label = "Supplier Invoice No. *",
                        modifier = Modifier.weight(42f)
                    )
                }

                // Optional vendor information stays compact and only appears
                // after a vendor has been selected.
                if (uiState.supplierName.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReadOnlyPurchaseField(
                            value = uiState.supplierGstin,
                            label = "Vendor GSTIN",
                            modifier = Modifier.weight(1f)
                        )

                        ReadOnlyPurchaseField(
                            value = uiState.creditDays.ifBlank { "0" },
                            label = "Credit Days",
                            modifier = Modifier.weight(0.45f)
                        )

                        ReadOnlyPurchaseField(
                            value = buildString {
                                if (uiState.supplierAddress.isNotBlank()) {
                                    append(uiState.supplierAddress)
                                }

                                val place = listOf(
                                    uiState.supplierCity,
                                    uiState.supplierDistrict,
                                    uiState.supplierState,
                                    uiState.supplierPinCode
                                ).filter { it.isNotBlank() }.joinToString(", ")

                                if (place.isNotBlank()) {
                                    if (isNotBlank()) append(", ")
                                    append(place)
                                }
                            },
                            label = "Vendor Address",
                            modifier = Modifier.weight(1.55f)
                        )
                    }
                }

                // ROW 2: four compact fields in one horizontal line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePurchaseField(
                        value = uiState.invoiceDate,
                        label = "Invoice Date *",
                        onClick = { invoiceDatePicker.show() },
                        modifier = Modifier.weight(1f)
                    )

                    DatePurchaseField(
                        value = uiState.receivedDate,
                        label = "Received Date",
                        onClick = { receivedDatePicker.show() },
                        modifier = Modifier.weight(1f)
                    )

                    SimplePurchaseDropdown(
                        value = uiState.purchaseType,
                        label = "Purchase Type",
                        options = listOf("Invoice", "Challan"),
                        onSelected = viewModel::updatePurchaseType,
                        modifier = Modifier.weight(1f)
                    )

                    SimplePurchaseDropdown(
                        value = uiState.paymentType,
                        label = "Payment Type",
                        options = listOf("Credit", "Cash", "Bank"),
                        onSelected = viewModel::updatePaymentType,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 3: full-width remarks
                PurchaseTextField(
                    value = uiState.reference,
                    onValueChange = {
                        viewModel.updateReference(
                            it.uppercase(Locale.getDefault())
                        )
                    },
                    label = "Reference / Remarks",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VilyncLavender
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VilyncLavenderBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PRODUCTS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VilyncNavy
                )

                if (uiState.items.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.58f),
                                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No products added yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VilyncNavy
                        )

                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VilyncPrimaryBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("+ Add Product", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    uiState.items.forEachIndexed { index, item ->
                        val gross = item.quantity * item.purchaseRate
                        val discount = gross * (item.discountPercent / 100.0)
                        val taxable = gross - discount
                        val tax = taxable * (item.gstPercent / 100.0)
                        val total = taxable + tax

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.72f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.productName,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VilyncNavy
                                    )
                                    Text(
                                        text = "Model: ${item.model.ifBlank { "-" }}   |   Power: ${item.power.ifBlank { "-" }}   |   Qty: ${item.quantity}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Rate: ₹${money(item.purchaseRate)}   |   Disc: ${money(item.discountPercent)}%   |   GST: ${money(item.gstPercent)}%   |   Amount: ₹${money(total)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditProductClick(index) }
                                    ) {
                                        Text("Edit")
                                    }

                                    Button(
                                        onClick = { viewModel.removeItem(index) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VilyncPrimaryBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("+ Add Product", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        var paidText by remember(uiState.paidAmount) {
            mutableStateOf(
                if (uiState.paidAmount == 0.0) "" else money(uiState.paidAmount)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VilyncBillGreen
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VilyncGreenBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BILL SUMMARY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VilyncNavy
                )

                // Row 1: four compact summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        label = "Gross Amount",
                        amount = uiState.grossAmount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Discount",
                        amount = uiState.discountAmount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Taxable Amount",
                        amount = uiState.taxableAmount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "GST",
                        amount = uiState.taxAmount,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Adjustment | Round Off | Net Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        label = "Adjustment",
                        amount = uiState.adjustmentAmount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Round Off",
                        amount = uiState.roundOffAmount,
                        modifier = Modifier.weight(1f)
                    )
                    NetAmountCard(
                        amount = uiState.netAmount,
                        modifier = Modifier.weight(2f)
                    )
                }

                // Row 3: Paid Amount | Due Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PurchaseTextField(
                        value = paidText,
                        onValueChange = { value ->
                            val clean = value.filter { it.isDigit() || it == '.' }
                            paidText = clean
                            viewModel.updatePaidAmount(
                                clean.toDoubleOrNull() ?: 0.0
                            )
                        },
                        label = "Paid Amount",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.weight(1f)
                    )

                    DueAmountCard(
                        amount = uiState.dueAmount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = {
                viewModel.savePurchase()
            },
            enabled = !uiState.isSaving &&
                    !(uiState.isSaved && !uiState.isDirty),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VilyncPrimaryBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                when {
                    uiState.isSaving -> "SAVING…"
                    uiState.isSaved && !uiState.isDirty -> "SAVED ✓"
                    uiState.isEditMode -> "UPDATE PURCHASE"
                    else -> "SAVE PURCHASE"
                }
            )
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

    Column(modifier = modifier) {

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = selectedVendorName.ifBlank { "Select Vendor *" },
                modifier = Modifier.weight(1f),
                fontWeight = if (selectedVendorName.isBlank()) {
                    FontWeight.Normal
                } else {
                    FontWeight.SemiBold
                }
            )
            Text(
                text = "▼",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            if (vendors.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text("No Vendor found in Party Master")
                    },
                    onClick = {
                        expanded = false
                    }
                )
            } else {
                vendors.forEach { vendor ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = vendor.partyName,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (vendor.gstin.isNotBlank()) {
                                    Text(
                                        text = vendor.gstin,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVendorSelected(vendor)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplePurchaseDropdown(
    value: String,
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "▼",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DatePurchaseField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(label)
        },
        placeholder = {
            Text("Select Date")
        },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📅",
                    fontSize = 22.sp
                )
            }
        },
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        singleLine = true
    )
}

// =============================================================
// ViLYNC SMART ENTRY MODE V2
// Keyboard-safe + auto bring-into-view + IME focus navigation.
// CAPITAL conversion remains at business-field call sites.
// =============================================================

@Composable
private fun PurchaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                maxLines = 1
            )
        },
        singleLine = true,

        // ViLYNC Smart Entry Mode:
        // correct keyboard + explicit IME behaviour.
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Next)
            },
            onDone = {
                focusManager.clearFocus()
            }
        ),

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VilyncPrimaryBlue,
            unfocusedBorderColor = VilyncBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),

        // Important:
        // 1. No fixed 56dp field height — prevents clipped glyphs.
        // 2. When keyboard opens/focus changes, bring this exact field
        //    into the visible viewport automatically.
        modifier = modifier
            .heightIn(min = 64.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        // Wait for IME resize/inset to settle, then scroll.
                        delay(180)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
    )
}

@Composable
private fun ReadOnlyPurchaseField(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(
                text = label,
                maxLines = 1
            )
        },
        // Long Vendor Address may use a second line.
        singleLine = false,
        minLines = 1,
        maxLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VilyncPrimaryBlue,
            unfocusedBorderColor = VilyncBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier.heightIn(min = 64.dp)
    )
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = VilyncSecondaryText
            )
            Text(
                text = "₹${money(amount)}",
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )
        }
    }
}

@Composable
private fun NetAmountCard(
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = VilyncGreen
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NET AMOUNT",
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )
            Text(
                text = "₹${money(amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = VilyncNavy
            )
        }
    }
}

@Composable
private fun DueAmountCard(
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(
            containerColor = VilyncDuePastel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DUE AMOUNT",
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )
            Text(
                text = "₹${money(amount)}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// =============================================================
// ViLYNC PASTEL ERP THEME
// =============================================================

private val VilyncPageBackground = Color(0xFFF7F9FD)
private val VilyncNavy = Color(0xFF14233C)
private val VilyncPrimaryBlue = Color(0xFF476EA8)
private val VilyncBorder = Color(0xFFD9DEE8)
private val VilyncGreen = Color(0xFFDDF4E7)
private val VilyncLavender = Color(0xFFEDE8FC)
private val VilyncPastelBlue = Color(0xFFF2F7FF)
private val VilyncSoftBlueBorder = Color(0xFFD5E3F7)
private val VilyncLavenderBorder = Color(0xFFDDD3F5)
private val VilyncBillGreen = Color(0xFFF3FAF5)
private val VilyncGreenBorder = Color(0xFFD5E9DA)
private val VilyncDuePastel = Color(0xFFFFF2F2)
private val VilyncSecondaryText = Color(0xFF667085)


private fun money(
    value: Double
): String {

    return String.format(
        Locale.getDefault(),
        "%.2f",
        value
    )
}

