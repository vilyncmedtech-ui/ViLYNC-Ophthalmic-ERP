package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AddPurchaseItemScreen(
    viewModel: AddPurchaseItemViewModel,
    onAddItem: () -> Unit = {},
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {}
) {

    val uiState by
    viewModel.uiState.collectAsState()

    val productSuggestions by
    viewModel.productSuggestions.collectAsState()

    var productDropdownExpanded by
    remember {
        mutableStateOf(false)
    }

    // Only one physical lens row is edited at a time.
    // The first default blank lens starts in edit mode.
    var editingLensIndex by
    remember {
        mutableStateOf<Int?>(0)
    }

    val isIol =
        uiState.category.equals(
            "IOL",
            ignoreCase = true
        )


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VilyncPageBackground)
            .verticalScroll(
                rememberScrollState()
            )
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {


        // =====================================================
        // TITLE
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }

            Text(
                text = "Add Purchase Product",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )

            OutlinedButton(onClick = onDashboard) {
                Text("⌂ Dashboard")
            }
        }


        Text(
            text = "Enter product and inventory details",
            style = MaterialTheme.typography.bodyMedium,
            color = VilyncSecondaryText
        )

        // =====================================================
        // PRODUCT DETAILS — COMPACT ROW
        // Product 40% | Model 20% | Category 20% | HSN 20%
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VilyncPastelBlue
            ),
            border = BorderStroke(1.dp, VilyncBlueBorder)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PRODUCT DETAILS",
                    fontWeight = FontWeight.Bold,
                    color = VilyncNavy
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = uiState.productName,
                            onValueChange = { value ->
                                viewModel.updateProductName(value)
                                productDropdownExpanded = true
                            },
                            label = { Text("Product Name") },
                            placeholder = { Text("Search Product Master") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        viewModel.loadProductSuggestions()
                                        productDropdownExpanded = true
                                    }
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            colors = compactFieldColors()
                        )

                        DropdownMenu(
                            expanded =
                                productDropdownExpanded &&
                                        productSuggestions.isNotEmpty(),
                            onDismissRequest = {
                                productDropdownExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            productSuggestions.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = product.productName,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            val detailText =
                                                listOf(
                                                    product.model,
                                                    product.brand,
                                                    product.category.name,
                                                    product.hsnCode
                                                )
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" • ")

                                            if (detailText.isNotBlank()) {
                                                Text(
                                                    text = detailText,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectProduct(product)
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    CompactPurchaseField(
                        value = uiState.model,
                        onValueChange = {
                            viewModel.updateModel(
                                it.uppercase(Locale.getDefault())
                            )
                        },
                        label = "Model",
                        modifier = Modifier.weight(1f)
                    )

                    CompactPurchaseField(
                        value = uiState.category,
                        onValueChange = {
                            viewModel.updateCategory(
                                it.uppercase(Locale.getDefault())
                            )
                        },
                        label = "Category",
                        modifier = Modifier.weight(1f)
                    )

                    CompactPurchaseField(
                        value = uiState.hsnCode,
                        onValueChange = viewModel::updateHsnCode,
                        label = "HSN Code",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // =====================================================
        // PURCHASE DETAILS — ONE COMPACT ROW
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VilyncPastelPeach
            ),
            border = BorderStroke(1.dp, VilyncPeachBorder)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PURCHASE DETAILS",
                    fontWeight = FontWeight.Bold,
                    color = VilyncNavy
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.power,
                        onValueChange = viewModel::updatePower,
                        label = { Text("Power") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    viewModel.formatPower()
                                }
                            },
                        singleLine = true,
                        colors = compactFieldColors()
                    )

                    CompactPurchaseField(
                        value =
                            if (isIol) {
                                uiState.lensDetails.size.toString()
                            } else {
                                uiState.quantity
                            },
                        onValueChange = { value ->
                            if (!isIol) {
                                viewModel.updateQuantity(value)
                            }
                        },
                        label = "Qty",
                        keyboardType = KeyboardType.Number,
                        readOnly = isIol,
                        modifier = Modifier.weight(0.72f)
                    )

                    CompactPurchaseField(
                        value = uiState.purchaseRate,
                        onValueChange = viewModel::updatePurchaseRate,
                        label = "Purchase Rate",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )

                    CompactPurchaseField(
                        value = uiState.discountPercent,
                        onValueChange = viewModel::updateDiscountPercent,
                        label = "Discount %",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(0.85f)
                    )

                    CompactPurchaseField(
                        value = uiState.gstPercent,
                        onValueChange = viewModel::updateGstPercent,
                        label = "GST %",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(0.75f)
                    )

                    CompactPurchaseField(
                        value = uiState.batchNumber,
                        onValueChange = {
                            viewModel.updateBatchNumber(
                                it.uppercase(Locale.getDefault())
                            )
                        },
                        label = "Batch / Lot",
                        modifier = Modifier.weight(1.25f)
                    )
                }

                if (isIol) {
                    Text(
                        text = "Quantity is automatic from physical IOL lens rows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VilyncSecondaryText
                    )
                }
            }
        }

        // =====================================================
        // IOL PHYSICAL LENS DETAILS
        // =====================================================

        if (isIol) {

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VilyncLavender
                ),
                border = BorderStroke(1.dp, VilyncLavenderBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text =
                            "Physical IOL Lens Details",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Text(
                        text =
                            "Each physical IOL must have its own " +
                                    "unique Serial Number and Expiry."
                    )


                    Text(
                        text =
                            "Quantity: ${uiState.lensDetails.size}",

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    // =================================================
                    // LENS ROWS
                    // =================================================

                    uiState.lensDetails
                        .forEachIndexed {
                                index,
                                lensDetail ->

                            val isEditing =
                                editingLensIndex == index


                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            Color.White.copy(alpha = 0.72f)
                                    )
                            ) {

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        verticalAlignment =
                                            Alignment.CenterVertically,

                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                12.dp
                                            )
                                    ) {

                                        Text(
                                            text =
                                                "Lens ${index + 1}",

                                            fontSize =
                                                16.sp,

                                            fontWeight =
                                                FontWeight.SemiBold,

                                            modifier =
                                                Modifier.weight(1f)
                                        )


                                        OutlinedButton(
                                            onClick = {

                                                editingLensIndex =
                                                    if (isEditing) {
                                                        null
                                                    } else {
                                                        index
                                                    }
                                            }
                                        ) {

                                            Text(
                                                if (isEditing) {
                                                    "Done"
                                                } else {
                                                    "Edit"
                                                }
                                            )
                                        }


                                        OutlinedButton(
                                            onClick = {

                                                viewModel
                                                    .removeLens(
                                                        index
                                                    )

                                                // Indexes may shift after removal,
                                                // so close row edit mode safely.
                                                editingLensIndex = null
                                            }
                                        ) {

                                            Text("Remove")
                                        }
                                    }


                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                12.dp
                                            ),

                                        verticalAlignment =
                                            Alignment.Top
                                    ) {

                                        OutlinedTextField(
                                            value =
                                                lensDetail
                                                    .serialNumber,

                                            onValueChange = {
                                                    value ->

                                                if (isEditing) {
                                                    viewModel
                                                        .updateLensSerialNumber(
                                                            index = index,
                                                            value = value.uppercase(Locale.getDefault())
                                                        )
                                                }
                                            },

                                            label = {
                                                Text(
                                                    "Serial Number"
                                                )
                                            },

                                            placeholder = {
                                                Text(
                                                    "SN${index + 1}"
                                                )
                                            },

                                            isError =
                                                index in
                                                        uiState
                                                            .duplicateSerialIndexes,

                                            supportingText = {
                                                when {
                                                    index in uiState.checkingSerialIndexes -> {
                                                        Text(
                                                            "Checking Serial Number..."
                                                        )
                                                    }

                                                    index in uiState.duplicateSerialIndexes -> {
                                                        Text(
                                                            "Serial Number already exists / is duplicated.",
                                                            color =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .error
                                                        )
                                                    }

                                                    lensDetail.serialNumber.isNotBlank() -> {
                                                        Text(
                                                            "Serial Number available"
                                                        )
                                                    }
                                                }
                                            },

                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                ),

                                            readOnly =
                                                !isEditing,

                                            singleLine =
                                                true
                                        )


                                        OutlinedTextField(
                                            value =
                                                lensDetail
                                                    .expiryDate,

                                            onValueChange = {
                                                    value ->

                                                if (isEditing) {

                                                    viewModel
                                                        .updateLensExpiry(
                                                            index =
                                                                index,

                                                            value =
                                                                value
                                                        )
                                                }
                                            },

                                            label = {
                                                Text(
                                                    "Expiry MMYY"
                                                )
                                            },

                                            placeholder = {
                                                Text(
                                                    "1229"
                                                )
                                            },

                                            supportingText = {

                                                when {

                                                    lensDetail
                                                        .expiryDate
                                                        .isBlank() -> {

                                                        Text(
                                                            "Example: 1229"
                                                        )
                                                    }


                                                    lensDetail
                                                        .expiryDate
                                                        .length < 4 -> {

                                                        Text(
                                                            "Enter MMYY"
                                                        )
                                                    }


                                                    else -> {

                                                        val month =
                                                            lensDetail
                                                                .expiryDate
                                                                .take(2)
                                                                .toIntOrNull()

                                                        if (
                                                            month == null ||
                                                            month !in 1..12
                                                        ) {

                                                            Text(
                                                                text =
                                                                    "Invalid month",

                                                                color =
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .error
                                                            )

                                                        } else {

                                                            Text(
                                                                "MMYY accepted"
                                                            )
                                                        }
                                                    }
                                                }
                                            },

                                            isError =
                                                run {

                                                    if (
                                                        lensDetail
                                                            .expiryDate
                                                            .length != 4
                                                    ) {

                                                        false

                                                    } else {

                                                        val month =
                                                            lensDetail
                                                                .expiryDate
                                                                .take(2)
                                                                .toIntOrNull()

                                                        month == null ||
                                                                month !in 1..12
                                                    }
                                                },

                                            keyboardOptions =
                                                KeyboardOptions(
                                                    keyboardType =
                                                        KeyboardType.Number
                                                ),

                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                ),

                                            readOnly =
                                                !isEditing,

                                            singleLine =
                                                true
                                        )
                                    }
                                }
                            }
                        }


                    // =================================================
                    // ADD LENS
                    // =================================================

                    OutlinedButton(
                        onClick = {

                            // New lens will be appended at the current size index.
                            val newLensIndex =
                                uiState.lensDetails.size

                            viewModel.addLens()

                            // Newly added blank lens opens directly in edit mode.
                            editingLensIndex =
                                newLensIndex
                        },

                        modifier =
                            Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, VilyncPrimaryBlue)
                    ) {

                        Text(
                            "+ Add Lens",
                            color = VilyncPrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }


                    if (
                        uiState.lensDetails.isEmpty()
                    ) {

                        Text(
                            text =
                                "Add at least one physical lens.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }

                }
            }
        }


        // =====================================================
        // ERROR MESSAGE
        // =====================================================

        uiState.errorMessage?.let {
                message ->

            Text(
                text =
                    message,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                fontWeight =
                    FontWeight.Medium
            )
        }


        // =====================================================
        // ADD TO PURCHASE
        // =====================================================

        Button(
            onClick = {

                viewModel.formatPower()

                val isValid =
                    viewModel
                        .validateCurrentItem()

                if (
                    isValid
                ) {

                    onAddItem()
                }
            },

            enabled =
                uiState.checkingSerialIndexes.isEmpty() &&
                        uiState.duplicateSerialIndexes.isEmpty(),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VilyncPrimaryBlue,
                contentColor = Color.White
            )
        ) {

            Text(
                "ADD TO PURCHASE",
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )
    }
}


// =============================================================
// ViLYNC PREMIUM COMPACT PURCHASE UI
// =============================================================

@Composable
private fun CompactPurchaseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        colors = compactFieldColors(),
        modifier = modifier
    )
}

@Composable
private fun compactFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VilyncPrimaryBlue,
        unfocusedBorderColor = VilyncFieldBorder,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White.copy(alpha = 0.70f)
    )

private val VilyncPageBackground = Color(0xFFF7F9FD)
private val VilyncNavy = Color(0xFF18233A)
private val VilyncSecondaryText = Color(0xFF667085)
private val VilyncPrimaryBlue = Color(0xFF476EA8)
private val VilyncFieldBorder = Color(0xFFD7DEE8)

private val VilyncPastelBlue = Color(0xFFF1F6FD)
private val VilyncBlueBorder = Color(0xFFD7E4F5)

private val VilyncPastelPeach = Color(0xFFFFF2E4)
private val VilyncPeachBorder = Color(0xFFF1DDC7)

private val VilyncLavender = Color(0xFFF0EBFC)
private val VilyncLavenderBorder = Color(0xFFDED4F5)
