package com.vilync.ophthalmicerp.feature.sales.presentation

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.ui.components.PartySearchField
import java.util.Calendar
import java.util.Locale

@Composable
fun SalesEntryScreen(
    viewModel: SalesViewModel,
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onSavedToDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var serialInput by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestExit(action: () -> Unit) {
        if (uiState.isDirty) {
            pendingExit = action
            showDiscardDialog = true
        } else action()
    }

    BackHandler { requestExit(onBack) }

    LaunchedEffect(uiState.isSavedSuccessfully, uiState.savedSaleId) {
        if (uiState.isSavedSuccessfully && uiState.savedSaleId != null) {
            onSavedToDetail(uiState.savedSaleId!!)
            viewModel.consumeSaveSuccess()
        }
    }

    val calendar = Calendar.getInstance()

    val invoiceDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            viewModel.updateInvoiceDate(
                String.format(
                    Locale.getDefault(),
                    "%02d-%02d-%04d",
                    day,
                    month + 1,
                    year
                )
            )

            viewModel.updateFinancialYearStart(
                if (month >= Calendar.APRIL) year else year - 1
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val poDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            viewModel.updatePoDate(
                String.format(
                    Locale.getDefault(),
                    "%02d-%02d-%04d",
                    day,
                    month + 1,
                    year
                )
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("Your changes to this sales invoice have not been saved.") },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    pendingExit = null
                }) { Text("Stay") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    pendingExit?.invoke()
                    pendingExit = null
                }) { Text("Discard & Exit") }
            }
        )
    }

    if (uiState.showSmartSerialMatchSelection) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSmartSerialMatches()
            },
            title = {
                Text(
                    text = "Select Matching Lens",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "More than one in-stock serial matches ${uiState.smartSerialQuery}. Select the correct physical lens.",
                        fontSize = 12.sp,
                        color = VilyncSecondaryText
                    )

                    uiState.smartSerialMatches.forEach { unit ->
                        val product =
                            uiState.products.firstOrNull {
                                it.id == unit.productId
                            }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    serialInput = unit.serialNumber
                                    viewModel.selectSmartSerialMatch(unit.id)
                                },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VilyncSoftBlueBorder),
                            colors = CardDefaults.cardColors(
                                containerColor = VilyncPastelBlue
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = product?.productName ?: "Unknown Product",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = unit.serialNumber,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = listOf(
                                        unit.power.takeIf { it.isNotBlank() }?.let { "Power: $it" },
                                        unit.batchNumber.takeIf { it.isNotBlank() }?.let { "Batch: $it" },
                                        unit.expiryDate.takeIf { it.isNotBlank() }?.let { "Expiry: $it" }
                                    )
                                        .filterNotNull()
                                        .joinToString("  •  ")
                                        .ifBlank { "Physical inventory unit" },
                                    fontSize = 11.sp,
                                    color = VilyncSecondaryText
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSmartSerialMatches()
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }


    if (uiState.showChallanSerialMatchSelection) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissChallanSerialMatches()
            },
            title = {
                Text(
                    text = "Select Pending Challan Lens",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "More than one pending Challan lens matches ${uiState.challanSerialQuery}. Select the correct physical lens.",
                        fontSize = 12.sp,
                        color = VilyncSecondaryText
                    )

                    uiState.challanSerialMatches.forEach { item ->
                        val challan =
                            uiState.pendingChallans.firstOrNull {
                                it.id == item.challanId
                            }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.chooseChallanSerialMatch(item.id)
                                },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VilyncSoftBlueBorder),
                            colors = CardDefaults.cardColors(
                                containerColor = VilyncPastelBlue
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = item.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = "${item.serialNumber}  •  Power: ${item.power.ifBlank { "-" }}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = "Challan: ${challan?.challanNumber ?: "-"}  •  ${challan?.challanDate ?: "-"}",
                                    fontSize = 11.sp,
                                    color = VilyncSecondaryText
                                )

                                Text(
                                    text = listOf(
                                        item.batchNumber.takeIf { it.isNotBlank() }?.let { "Batch: $it" },
                                        item.expiryDate.takeIf { it.isNotBlank() }?.let { "Expiry: $it" }
                                    )
                                        .filterNotNull()
                                        .joinToString("  •  ")
                                        .ifBlank { "Pending Challan physical unit" },
                                    fontSize = 11.sp,
                                    color = VilyncSecondaryText
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissChallanSerialMatches()
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VilyncPageBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactHeader(
            isEditMode = uiState.isEditMode,
            onBack = { requestExit(onBack) },
            onDashboard = { requestExit(onDashboard) }
        )

        SectionCard(
            title = "BILLING & INVOICE",
            background = VilyncPastelBlue,
            border = VilyncSoftBlueBorder
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                PartySearchField(
                    label = "Customer / Hospital *",
                    selectedParty = uiState.selectedCustomer,
                    allParties = uiState.customers,
                    onPartySelected = { viewModel.selectCustomer(it) },
                    modifier = Modifier.weight(2.2f),
                    readOnly = uiState.isEditMode
                )

                Row(
                    modifier = Modifier.weight(1.65f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.sameAsBillTo,
                        onCheckedChange = viewModel::updateSameAsBillTo
                    )

                    Text(
                        "Same as Bill To",
                        fontSize = 11.sp,
                        color = VilyncNavy
                    )
                }

                ReadOnlyField(
                    value = if (uiState.isEditMode) uiState.invoiceNumber else "Auto-generated",
                    label = "Invoice No. *",
                    modifier = Modifier.weight(1.15f)
                )

                ClickField(
                    value = uiState.invoiceDate,
                    label = "Invoice Date * 📅",
                    onClick = {
                        invoiceDatePicker.show()
                    },
                    modifier = Modifier.weight(1.15f)
                )
            }

            uiState.selectedCustomer?.let { customer ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    ReadOnlyField(
                        value = uiState.billToLegalName
                            .ifBlank { customer.partyName },
                        label = "Bill To",
                        modifier = Modifier.weight(1.4f)
                    )

                    ReadOnlyField(
                        value = uiState.billToGstin.ifBlank { "-" },
                        label = "Bill To GSTIN",
                        modifier = Modifier.weight(1.05f)
                    )

                    ReadOnlyField(
                        value = uiState.billToState.ifBlank { "-" },
                        label = "Bill To State",
                        modifier = Modifier.weight(.9f)
                    )

                    ReadOnlyField(
                        value = uiState.placeOfSupplyState
                            .ifBlank { uiState.billToState }
                            .ifBlank { "-" },
                        label = "Place of Supply",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.billToAddress.isNotBlank()) {
                    Text(
                        uiState.billToAddress,
                        fontSize = 11.sp,
                        color = VilyncSecondaryText
                    )
                }
            }

            if (!uiState.sameAsBillTo) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    CompactField(
                        value = uiState.shipToName,
                        onValueChange = viewModel::updateShipToName,
                        label = "Ship To Name",
                        modifier = Modifier.weight(1.4f)
                    )

                    CompactField(
                        value = uiState.shipToGstin,
                        onValueChange = viewModel::updateShipToGstin,
                        label = "Ship To GSTIN",
                        modifier = Modifier.weight(1.05f)
                    )

                    CompactField(
                        value = uiState.shipToState,
                        onValueChange = viewModel::updateShipToState,
                        label = "Ship To State",
                        modifier = Modifier.weight(.9f)
                    )

                    CompactField(
                        value = uiState.placeOfSupplyState,
                        onValueChange = viewModel::updatePlaceOfSupplyState,
                        label = "Place of Supply",
                        modifier = Modifier.weight(1f)
                    )
                }

                CompactField(
                    value = uiState.shipToAddress,
                    onValueChange = viewModel::updateShipToAddress,
                    label = "Ship To Address",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                CompactField(
                    value = uiState.poNumber,
                    onValueChange = {
                        viewModel.updatePoNumber(
                            it.uppercase(Locale.getDefault())
                        )
                    },
                    label = "P.O. No.",
                    modifier = Modifier.weight(2.2f)
                )

                ClickField(
                    value = uiState.poDate,
                    label = "P.O. Date 📅",
                    onClick = {
                        poDatePicker.show()
                    },
                    modifier = Modifier.weight(1.15f)
                )

                Spacer(
                    modifier = Modifier.weight(2.8f)
                )
            }
        }


        SectionCard(
            title =
                if (uiState.selectedCustomer != null) {
                    "SETTLE CHALLAN — ${uiState.selectedCustomer?.legalName ?: uiState.billToLegalName}"
                } else {
                    "SETTLE CHALLAN"
                },
            background = VilyncPastelBlue,
            border = VilyncSoftBlueBorder
        ) {
            if (uiState.selectedCustomer == null) {
                Text(
                    text = "Select Bill To customer to view pending challans.",
                    fontSize = 12.sp,
                    color = VilyncSecondaryText
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactField(
                        value = uiState.challanSerialQuery,
                        onValueChange = {
                            viewModel.updateChallanSerialQuery(
                                it.uppercase(Locale.getDefault())
                            )
                        },
                        label = "Challan Serial / Unique ID",
                        imeAction = ImeAction.Done,
                        onDone = {
                            viewModel.searchPendingChallanSerial()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            viewModel.searchPendingChallanSerial()
                        },
                        enabled =
                            !uiState.isChallanSerialSearching &&
                                    !uiState.isChallanLoading,
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VilyncPrimaryBlue
                        )
                    ) {
                        Text(
                            text =
                                if (uiState.isChallanSerialSearching) {
                                    "SEARCHING..."
                                } else {
                                    "FIND"
                                },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when {
                    uiState.isChallanLoading -> {
                        Text(
                            text = "Loading pending Challans...",
                            fontSize = 12.sp,
                            color = VilyncSecondaryText
                        )
                    }

                    uiState.pendingChallanItems.isEmpty() -> {
                        Text(
                            text = "No pending Challan lenses for this Bill To customer.",
                            fontSize = 12.sp,
                            color = VilyncSecondaryText
                        )
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENDING CHALLANS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Text(
                                text = "${uiState.selectedChallanItemCount} selected",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = VilyncPrimaryBlue
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 330.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.pendingChallans.forEach { challan ->
                                val challanItems =
                                    uiState.pendingChallanItems.filter {
                                        it.challanId == challan.id
                                    }

                                if (challanItems.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            VilyncSoftBlueBorder
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = VilyncPageBackground
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${challan.challanNumber}  |  ${challan.challanDate}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )

                                            challanItems.forEach { item ->
                                                val selected =
                                                    item.id in uiState.selectedChallanItemIds

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.toggleChallanItemForInvoice(
                                                                item.id
                                                            )
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = selected,
                                                        onCheckedChange = {
                                                            viewModel.toggleChallanItemForInvoice(
                                                                item.id
                                                            )
                                                        }
                                                    )

                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = item.productName,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp
                                                        )

                                                        Text(
                                                            text = "${item.power.ifBlank { "-" }}  •  ${item.serialNumber}",
                                                            fontSize = 12.sp
                                                        )

                                                        val trace =
                                                            listOf(
                                                                item.batchNumber
                                                                    .takeIf { it.isNotBlank() }
                                                                    ?.let { "Batch: $it" },
                                                                item.expiryDate
                                                                    .takeIf { it.isNotBlank() }
                                                                    ?.let { "Expiry: $it" }
                                                            )
                                                                .filterNotNull()
                                                                .joinToString("  •  ")

                                                        if (trace.isNotBlank()) {
                                                            Text(
                                                                text = trace,
                                                                fontSize = 10.sp,
                                                                color = VilyncSecondaryText
                                                            )
                                                        }
                                                    }

                                                    if (selected) {
                                                        Text(
                                                            text = "SELECTED",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            color = VilyncPrimaryBlue
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.selectedChallanItemIds.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.clearSelectedChallanItems()
                                    }
                                ) {
                                    Text("CLEAR SELECTION")
                                }
                            }
                        }

                        Text(
                            text = "Selection is temporary. Challan and Inventory are not permanently updated until the Sales Invoice is successfully saved.",
                            fontSize = 11.sp,
                            color = VilyncSecondaryText
                        )
                    }
                }
            }
        }

        SectionCard(
            title = "PRODUCT ENTRY",
            background = VilyncLavender,
            border = VilyncLavenderBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactField(
                    value = serialInput,
                    onValueChange = { serialInput = it.uppercase(Locale.getDefault()).trimStart() },
                    label = "Serial / Unique ID *",
                    imeAction = ImeAction.Done,
                    onDone = { viewModel.selectSerialNumber(serialInput) },
                    modifier = Modifier.weight(2.2f)
                )
                Button(
                    onClick = { viewModel.selectSerialNumber(serialInput) },
                    enabled = !uiState.isSmartSerialSearching,
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VilyncPrimaryBlue)
                ) {
                    Text(
                        if (uiState.isSmartSerialSearching) "SEARCHING..." else "FIND",
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "or",
                    fontSize = 11.sp,
                    color = VilyncSecondaryText
                )
                ProductDropdown(
                    selected = uiState.selectedProduct,
                    products = uiState.products,
                    onSelected = { product ->
                        serialInput = ""
                        viewModel.selectProduct(product.id)
                    },
                    modifier = Modifier.weight(1.65f)
                )
            }

            uiState.selectedProduct?.let { product ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(product.productName, "Product", Modifier.weight(2f))
                    ReadOnlyField(product.model.ifBlank { "-" }, "Model", Modifier.weight(1.15f))
                    ReadOnlyField(uiState.selectedPower.ifBlank { "-" }, "Power", Modifier.weight(.8f))
                    ReadOnlyField(uiState.currentQuantity.toString(), "Qty", Modifier.weight(.55f))
                }

                val unit = uiState.selectedSerialUnits.firstOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(unit?.serialNumber ?: "-", "Serial", Modifier.weight(1.45f))
                    ReadOnlyField(unit?.batchNumber?.ifBlank { "-" } ?: "-", "Batch / Lot", Modifier.weight(1f))
                    ReadOnlyField(unit?.expiryDate?.ifBlank { "-" } ?: "-", "Expiry", Modifier.weight(.9f))
                    ReadOnlyField(product.hsnCode.ifBlank { "-" }, "HSN", Modifier.weight(.8f))
                }

                Text(
                    listOf(product.category, product.brandName)
                        .filter { it.isNotBlank() }
                        .joinToString("  •  "),
                    fontSize = 11.sp,
                    color = VilyncSecondaryText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactField(
                        value = uiState.rate,
                        onValueChange = viewModel::updateRate,
                        label = "Rate ₹ *",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CompactField(
                        value = uiState.discountPercent,
                        onValueChange = viewModel::updateDiscountPercent,
                        label = "Disc %",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(.8f)
                    )
                    CompactField(
                        value = uiState.gstPercent,
                        onValueChange = viewModel::updateGstPercent,
                        label = "GST %",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(.8f)
                    )

                    val preview = entryPreview(
                        quantity = uiState.currentQuantity,
                        rate = uiState.rate,
                        discountPercent = uiState.discountPercent,
                        gstPercent = uiState.gstPercent
                    )
                    ReadOnlyField(money(preview.first), "Taxable ₹", Modifier.weight(1f))
                    ReadOnlyField(money(preview.second), "Amount ₹", Modifier.weight(1f))

                    Button(
                        onClick = {
                            viewModel.addCurrentItem()
                            serialInput = ""
                        },
                        enabled = uiState.currentQuantity > 0,
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VilyncPrimaryBlue)
                    ) {
                        Text("+ ADD ITEM", fontWeight = FontWeight.Bold)
                    }
                }
            } ?: Text(
                "Enter/scan a Serial / Unique ID. Product, Power, Batch, Expiry, Rate and GST will fill automatically. Manual Product selection remains available as fallback.",
                fontSize = 12.sp,
                color = VilyncSecondaryText
            )
        }

        SectionCard(
            title = "ITEMS (${uiState.items.size})",
            background = VilyncItemsPastel,
            border = VilyncItemsBorder
        ) {
            if (uiState.items.isEmpty()) {
                Text("No sales items added yet.", fontSize = 12.sp, color = VilyncSecondaryText)
            } else {
                ItemsHeader()
                uiState.items.forEach { item ->
                    val serials = item.selectedUnits.joinToString(", ") { it.serialNumber }
                    ItemDataRow(
                        productName = item.productName,
                        power = item.power,
                        serials = serials,
                        quantity = item.quantity,
                        rate = item.rate,
                        amount = item.totalAmount,
                        onEdit = { viewModel.editItem(item.localId) },
                        onRemove = { viewModel.removeItem(item.localId) }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        SectionCard(
            title = "BILL SUMMARY",
            background = VilyncBillGreen,
            border = VilyncGreenBorder
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                SummaryBox(
                    "Gross",
                    uiState.subTotal,
                    Modifier.weight(1f)
                )

                SummaryBox(
                    "Discount",
                    uiState.discountAmount,
                    Modifier.weight(1f)
                )

                SummaryBox(
                    "Taxable",
                    uiState.taxableAmount,
                    Modifier.weight(1f)
                )

                if (
                    uiState.gstSupplyType.equals(
                        other = "INTER_STATE",
                        ignoreCase = true
                    )
                ) {

                    SummaryBox(
                        "IGST",
                        uiState.igstAmount,
                        Modifier.weight(1f)
                    )

                } else {

                    SummaryBox(
                        "CGST",
                        uiState.cgstAmount,
                        Modifier.weight(1f)
                    )

                    SummaryBox(
                        "SGST",
                        uiState.sgstAmount,
                        Modifier.weight(1f)
                    )
                }

                NetBox(
                    uiState.totalAmount,
                    Modifier.weight(1.45f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactField(
                value = uiState.remarks,
                onValueChange = viewModel::updateRemarks,
                label = "Remarks",
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(2.5f)
            )

            uiState.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Button(
                onClick = viewModel::saveSale,
                enabled = !uiState.isSaving && !(uiState.isSaved && !uiState.isDirty),
                modifier = Modifier
                    .weight(1.25f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VilyncPrimaryBlue)
            ) {
                Text(
                    when {
                        uiState.isSaving -> "SAVING…"
                        uiState.isSaved && !uiState.isDirty -> "SAVED ✓"
                        else -> if (uiState.isEditMode) "UPDATE INVOICE" else "SAVE INVOICE"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CompactHeader(
    isEditMode: Boolean,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isEditMode) "EDIT SALES INVOICE" else "NEW SALES INVOICE",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncNavy,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.height(42.dp)) { Text("← Back") }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onDashboard, modifier = Modifier.height(42.dp)) { Text("⌂ Dashboard") }
    }
}

@Composable
private fun SectionCard(
    title: String,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VilyncNavy)
            content()
        }
    }
}

@Composable
private fun ProductDropdown(
    selected: ProductEntity?,
    products: List<ProductEntity>,
    onSelected: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(selected?.productName ?: "Select Product", modifier = Modifier.weight(1f), maxLines = 1, fontSize = 12.sp)
            Text("▼", fontSize = 10.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            products.forEach { product ->
                DropdownMenuItem(
                    text = { Text(product.productName) },
                    onClick = {
                        onSelected(product)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = {
                onDone?.invoke()
                focusManager.clearFocus()
            }
        ),
        colors = compactFieldColors(),
        modifier = modifier.heightIn(min = 52.dp)
    )
}

@Composable
private fun ClickField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.clickable(onClick = onClick)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = VilyncNavy,
                disabledBorderColor = VilyncBorder,
                disabledLabelColor = VilyncSecondaryText,
                disabledContainerColor = VilyncWhite
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        )
    }
}

@Composable
private fun ReadOnlyField(value: String, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        singleLine = true,
        colors = compactFieldColors(),
        modifier = modifier.heightIn(min = 52.dp)
    )
}

@Composable
private fun ItemsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            "Product",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(2f)
        )
        Text(
            "Power",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(.75f)
        )
        Text(
            "Serial / Unique ID",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(1.7f)
        )
        Text(
            "Qty",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(.45f)
        )
        Text(
            "Rate",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(.9f)
        )
        Text(
            "Amount",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VilyncSecondaryText,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(118.dp))
    }
}

@Composable
private fun ItemDataRow(
    productName: String,
    power: String,
    serials: String,
    quantity: Int,
    rate: Double,
    amount: Double,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VilyncWhite, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            productName,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VilyncNavy,
            maxLines = 2,
            modifier = Modifier.weight(2f).padding(end = 5.dp)
        )
        Text(
            power.ifBlank { "-" },
            fontSize = 11.sp,
            color = VilyncNavy,
            modifier = Modifier.weight(.75f).padding(end = 5.dp)
        )
        Text(
            serials,
            fontSize = 11.sp,
            color = VilyncNavy,
            maxLines = 2,
            modifier = Modifier.weight(1.7f).padding(end = 5.dp)
        )
        Text(
            quantity.toString(),
            fontSize = 11.sp,
            color = VilyncNavy,
            modifier = Modifier.weight(.45f).padding(end = 5.dp)
        )
        Text(
            "₹${money(rate)}",
            fontSize = 11.sp,
            color = VilyncNavy,
            modifier = Modifier.weight(.9f).padding(end = 5.dp)
        )
        Text(
            "₹${money(amount)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VilyncNavy,
            modifier = Modifier.weight(1f).padding(end = 5.dp)
        )
        TextButton(onClick = onEdit) {
            Text("Edit", fontSize = 11.sp)
        }
        TextButton(onClick = onRemove) {
            Text(
                "Remove",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SummaryBox(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(58.dp), colors = CardDefaults.cardColors(containerColor = VilyncWhite)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(label, fontSize = 10.sp, color = VilyncSecondaryText)
            Text("₹${money(amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VilyncNavy)
        }
    }
}

@Composable
private fun NetBox(amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(58.dp), colors = CardDefaults.cardColors(containerColor = VilyncGreen)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text("NET AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VilyncNavy)
            Text("₹${money(amount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VilyncNavy)
        }
    }
}

@Composable
private fun compactFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VilyncPrimaryBlue,
    unfocusedBorderColor = VilyncBorder,
    focusedContainerColor = VilyncWhite,
    unfocusedContainerColor = VilyncWhite
)

private fun entryPreview(
    quantity: Int,
    rate: String,
    discountPercent: String,
    gstPercent: String
): Pair<Double, Double> {
    val gross = quantity * (rate.toDoubleOrNull() ?: 0.0)
    val discount = gross * ((discountPercent.toDoubleOrNull() ?: 0.0) / 100.0)
    val taxable = gross - discount
    val gst = taxable * ((gstPercent.toDoubleOrNull() ?: 0.0) / 100.0)
    return taxable to (taxable + gst)
}



private fun buildAddress(customer: PartyMaster): String = listOf(
    customer.addressLine1,
    customer.addressLine2,
    customer.city,
    customer.district,
    customer.state,
    customer.pinCode
).filter { it.isNotBlank() }.joinToString(", ")

private fun money(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

private val VilyncPageBackground = androidx.compose.ui.graphics.Color(0xFFF7F9FD)
private val VilyncNavy = androidx.compose.ui.graphics.Color(0xFF14233C)
private val VilyncPrimaryBlue = androidx.compose.ui.graphics.Color(0xFF476EA8)
private val VilyncBorder = androidx.compose.ui.graphics.Color(0xFFD9DEE8)
private val VilyncGreen = androidx.compose.ui.graphics.Color(0xFFDDF4E7)
private val VilyncLavender = androidx.compose.ui.graphics.Color(0xFFEDE8FC)
private val VilyncPastelBlue = androidx.compose.ui.graphics.Color(0xFFF2F7FF)
private val VilyncSoftBlueBorder = androidx.compose.ui.graphics.Color(0xFFD5E3F7)
private val VilyncLavenderBorder = androidx.compose.ui.graphics.Color(0xFFDDD3F5)
private val VilyncBillGreen = androidx.compose.ui.graphics.Color(0xFFF3FAF5)
private val VilyncGreenBorder = androidx.compose.ui.graphics.Color(0xFFD5E9DA)
private val VilyncSecondaryText = androidx.compose.ui.graphics.Color(0xFF667085)
private val VilyncItemsPastel = androidx.compose.ui.graphics.Color(0xFFFFF8ED)
private val VilyncItemsBorder = androidx.compose.ui.graphics.Color(0xFFF0DFC2)
private val VilyncWhite = androidx.compose.ui.graphics.Color.White
