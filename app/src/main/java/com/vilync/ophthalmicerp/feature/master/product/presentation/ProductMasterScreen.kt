package com.vilync.ophthalmicerp.feature.master.product.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import com.vilync.ophthalmicerp.feature.master.product.model.ProductUnit
import com.vilync.ophthalmicerp.feature.product.model.ProductCategory


private val ProductNavy = Color(0xFF16365C)
private val ProductNavyDark = Color(0xFF0E2947)
private val ProductBlue = Color(0xFF1F4E79)
private val ProductGold = Color(0xFFD4A72C)
private val ProductPage = Color(0xFFF5F7FA)
private val ProductCard = Color.White
private val ProductSoftBlue = Color(0xFFF1F6FB)
private val ProductBorder = Color(0xFFD7DEE8)
private val ProductText = Color(0xFF1F2937)
private val ProductMuted = Color(0xFF667085)
private val ProductGreen = Color(0xFF198754)
private val ProductSoftGreen = Color(0xFFEAF7F0)
private val ProductDanger = Color(0xFFB42318)


@Composable
fun ProductMasterScreen(
    viewModel: ProductMasterViewModel,
    onProductSaved: (ProductMaster) -> Unit = {},
    onProductDeleted: () -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }


    if (showDeleteConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
            },
            title = {
                Text(
                    "Delete Product?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete this product. " +
                            "Use this only when the product should be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false

                        viewModel.deleteProduct(
                            onSuccess = onProductDeleted
                        )
                    }
                ) {
                    Text(
                        "Delete",
                        color = ProductDanger
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductPage)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProductNavy
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text =
                            if (uiState.isEditMode) {
                                "EDIT PRODUCT"
                            } else {
                                "PRODUCT MASTER"
                            },
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "Product • Pricing • Tax • Inventory",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            ProductGold,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 7.dp
                        )
                ) {

                    Text(
                        text =
                            if (uiState.isEditMode) {
                                "EDIT MODE"
                            } else {
                                "ADD PRODUCT"
                            },
                        color = ProductNavyDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        // =====================================================
        // BASIC INFORMATION
        // =====================================================

        ProductSectionCard(
            title = "BASIC INFORMATION"
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {

                ProductTextField(
                    value = uiState.productName,
                    onValueChange = viewModel::updateProductName,
                    label = "Product Name *",
                    modifier = Modifier.weight(1.25f),
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct
                )


                // =================================================
                // COMPANY / MANUFACTURER LIVE AUTOCOMPLETE
                // =================================================
                //
                // IMPORTANT:
                // No clickable modifier on editable TextField.
                //
                // L    -> search
                // Li   -> narrower search
                // Lif  -> narrower search
                //
                // =================================================

                Box(
                    modifier = Modifier.weight(1.25f)
                ) {

                    ProductTextField(
                        value = uiState.brand,
                        onValueChange = { value ->

                            viewModel.updateBrand(value)
                        },
                        label = "Company / Manufacturer",
                        modifier = Modifier.fillMaxWidth(),
                        enabled =
                            !isSaving &&
                                    !uiState.isLoadingProduct
                    )


                    DropdownMenu(
                        expanded =
                            uiState.showManufacturerSuggestions &&
                                    uiState
                                        .filteredManufacturerSuggestions
                                        .isNotEmpty(),
                        onDismissRequest = {
                            viewModel.hideManufacturerSuggestions()
                        },
                        modifier = Modifier.background(Color.White)
                    ) {

                        uiState
                            .filteredManufacturerSuggestions
                            .forEach { manufacturer ->

                                DropdownMenuItem(
                                    text = {

                                        Column {

                                            Text(
                                                text = manufacturer,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ProductText
                                            )

                                            Text(
                                                text = "Existing manufacturer",
                                                color = ProductMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {

                                        viewModel.selectManufacturer(
                                            manufacturer
                                        )
                                    }
                                )
                            }
                    }
                }


                ProductTextField(
                    value = uiState.model,
                    onValueChange = viewModel::updateModel,
                    label = "Model",
                    modifier = Modifier.weight(0.85f),
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct
                )
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // =================================================
                // CATEGORY
                // =================================================

                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    ProductTextField(
                        value = uiState.category.name,
                        onValueChange = {},
                        label = "Category *",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled =
                                    !isSaving &&
                                            !uiState.isLoadingProduct
                            ) {
                                categoryExpanded = true
                            },
                        enabled =
                            !isSaving &&
                                    !uiState.isLoadingProduct,
                        readOnly = true
                    )


                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = {
                            categoryExpanded = false
                        }
                    ) {

                        ProductCategory.entries.forEach { category ->

                            DropdownMenuItem(
                                text = {
                                    Text(category.name)
                                },
                                onClick = {

                                    viewModel.updateCategory(
                                        category
                                    )

                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }


                // =================================================
                // UNIT
                // =================================================

                Box(
                    modifier = Modifier.weight(0.85f)
                ) {

                    ProductTextField(
                        value = uiState.unit.displayName,
                        onValueChange = {},
                        label = "Unit *",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled =
                                    !isSaving &&
                                            !uiState.isLoadingProduct
                            ) {
                                unitExpanded = true
                            },
                        enabled =
                            !isSaving &&
                                    !uiState.isLoadingProduct,
                        readOnly = true
                    )


                    DropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = {
                            unitExpanded = false
                        }
                    ) {

                        ProductUnit.entries.forEach { unit ->

                            DropdownMenuItem(
                                text = {
                                    Text(unit.displayName)
                                },
                                onClick = {

                                    viewModel.updateUnit(
                                        unit
                                    )

                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }


                ProductTextField(
                    value = uiState.hsnCode,
                    onValueChange = viewModel::updateHsnCode,
                    label = "HSN Code *",
                    modifier = Modifier.weight(1f),
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    keyboardType = KeyboardType.Number
                )


                ProductTextField(
                    value = uiState.gstPercent,
                    onValueChange = viewModel::updateGstPercent,
                    label = "GST % *",
                    modifier = Modifier.weight(0.7f),
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    keyboardType = KeyboardType.Decimal
                )
            }
        }


        // =====================================================
        // PRICING
        // =====================================================

        ProductSectionCard(
            title = "PRICING"
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {

                // =================================================
                // PURCHASE
                // =================================================

                PricingPanel(
                    title = "PURCHASE",
                    subtitle = "Cost & input tax",
                    modifier = Modifier.weight(1f)
                ) {

                    ProductTextField(
                        value = uiState.purchasePrice,
                        onValueChange = viewModel::updatePurchasePrice,
                        label = "Purchase Rate Before GST *",
                        modifier = Modifier.fillMaxWidth(),
                        enabled =
                            !isSaving &&
                                    !uiState.isLoadingProduct,
                        keyboardType = KeyboardType.Decimal
                    )


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        MoneySummary(
                            title = "GST AMOUNT",
                            value = uiState.purchaseGstAmount,
                            modifier = Modifier.weight(1f)
                        )

                        MoneySummary(
                            title = "NET PURCHASE",
                            value = uiState.netPurchasePrice,
                            modifier = Modifier.weight(1f),
                            highlight = true
                        )
                    }
                }


                // =================================================
                // SALES
                // =================================================

                PricingPanel(
                    title = "SALES",
                    subtitle = "Selling price & MRP",
                    modifier = Modifier.weight(1f)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        ProductTextField(
                            value = uiState.retailPrice,
                            onValueChange = viewModel::updateRetailPrice,
                            label = "Selling Rate Before GST *",
                            modifier = Modifier.weight(1f),
                            enabled =
                                !isSaving &&
                                        !uiState.isLoadingProduct,
                            keyboardType = KeyboardType.Decimal
                        )


                        ProductTextField(
                            value = uiState.mrp,
                            onValueChange = viewModel::updateMrp,
                            label = "MRP Inclusive *",
                            modifier = Modifier.weight(0.8f),
                            enabled =
                                !isSaving &&
                                        !uiState.isLoadingProduct,
                            keyboardType = KeyboardType.Decimal
                        )
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        MoneySummary(
                            title = "GST AMOUNT",
                            value = uiState.retailGstAmount,
                            modifier = Modifier.weight(1f)
                        )

                        MoneySummary(
                            title = "NET SELLING",
                            value = uiState.netRetailPrice,
                            modifier = Modifier.weight(1f),
                            highlight = true
                        )
                    }
                }
            }
        }


        // =====================================================
        // INVENTORY & TRACEABILITY
        // =====================================================

        ProductSectionCard(
            title = "INVENTORY & TRACEABILITY"
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                TraceabilityControl(
                    title = "Power",
                    subtitle = "IOL Power",
                    checked = uiState.powerApplicable,
                    onCheckedChange =
                        viewModel::updatePowerApplicable,
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1f)
                )


                TraceabilityControl(
                    title = "Batch",
                    subtitle = "Track Batch",
                    checked = uiState.batchApplicable,
                    onCheckedChange =
                        viewModel::updateBatchApplicable,
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1f)
                )


                TraceabilityControl(
                    title = "Expiry",
                    subtitle = "Track Expiry",
                    checked = uiState.expiryApplicable,
                    onCheckedChange =
                        viewModel::updateExpiryApplicable,
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1f)
                )


                TraceabilityControl(
                    title = "Serial No.",
                    subtitle = "Individual Serial",
                    checked =
                        uiState.serialNumberRequired,
                    onCheckedChange =
                        viewModel::updateSerialNumberRequired,
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1f)
                )


                StatusControl(
                    checked = uiState.isActive,
                    onCheckedChange =
                        viewModel::updateIsActive,
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1.15f)
                )
            }
        }


        if (uiState.serialNumberRequired) {

            OutlinedTextField(
                value = uiState.serialPrefix,
                onValueChange = viewModel::updateSerialPrefix,
                label = {
                    Text("Serial Prefix")
                },
                supportingText = {
                    Text(
                        "Fixed prefix from Product Master. Example: LMDE or LMMS. " +
                                "Transaction users will enter only the numeric serial part."
                    )
                },
                singleLine = true,
                enabled =
                    !isSaving &&
                            !uiState.isLoadingProduct,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProductBlue,
                    focusedLabelColor = ProductBlue
                )
            )
        }


        // =====================================================
        // ERROR
        // =====================================================

        uiState.errorMessage?.let { message ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                )
            ) {

                Text(
                    text = message,
                    modifier = Modifier.padding(10.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }


        // =====================================================
        // SUCCESS
        // =====================================================

        if (saveSuccess) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ProductSoftGreen
                )
            ) {

                Text(
                    text =
                        if (uiState.isEditMode) {
                            "✓ Product updated successfully"
                        } else {
                            "✓ Product saved successfully"
                        },
                    modifier = Modifier.padding(10.dp),
                    color = ProductGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }


        // =====================================================
        // ACTION BAR
        // =====================================================

        if (uiState.isEditMode) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {

                        viewModel.saveProduct(
                            onSuccess = {

                                val savedProduct =
                                    viewModel.createProductMaster()

                                if (savedProduct != null) {

                                    onProductSaved(
                                        savedProduct
                                    )
                                }
                            }
                        )
                    },
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProductGold,
                        contentColor = ProductNavyDark
                    )
                ) {

                    Text(
                        text =
                            when {

                                uiState.isLoadingProduct ->
                                    "LOADING PRODUCT..."

                                isSaving ->
                                    "UPDATING..."

                                else ->
                                    "UPDATE PRODUCT"
                            },
                        fontWeight = FontWeight.Bold
                    )
                }


                OutlinedButton(
                    onClick = {
                        showDeleteConfirmation = true
                    },
                    enabled =
                        !isSaving &&
                                !uiState.isLoadingProduct,
                    modifier = Modifier.weight(0.38f)
                ) {

                    Text(
                        "DELETE",
                        color = ProductDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        } else {

            Button(
                onClick = {

                    viewModel.saveProduct(
                        onSuccess = {

                            val savedProduct =
                                viewModel.createProductMaster()

                            if (savedProduct != null) {

                                onProductSaved(
                                    savedProduct
                                )
                            }
                        }
                    )
                },
                enabled =
                    !isSaving &&
                            !uiState.isLoadingProduct,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductGold,
                    contentColor = ProductNavyDark
                )
            ) {

                Text(
                    text =
                        if (isSaving) {
                            "SAVING PRODUCT..."
                        } else {
                            "SAVE PRODUCT"
                        },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }


        Spacer(
            modifier = Modifier.height(2.dp)
        )
    }
}


// =============================================================
// SECTION CARD
// =============================================================

@Composable
private fun ProductSectionCard(
    title: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ProductCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 9.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(
                            ProductGold,
                            RoundedCornerShape(4.dp)
                        )
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = title,
                    color = ProductNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}


// =============================================================
// TEXT FIELD
// =============================================================

@Composable
private fun ProductTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardType: KeyboardType =
        KeyboardType.Text
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {

            Text(
                text = label,
                fontSize = 11.sp
            )
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProductBlue,
            unfocusedBorderColor = ProductBorder,
            focusedLabelColor = ProductBlue,
            cursorColor = ProductBlue,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}


// =============================================================
// PRICING PANEL
// =============================================================

@Composable
private fun PricingPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = ProductSoftBlue
        )
    ) {

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = title,
                    color = ProductNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = ProductMuted,
                    fontSize = 10.sp
                )
            }

            content()
        }
    }
}


// =============================================================
// MONEY SUMMARY
// =============================================================

@Composable
private fun MoneySummary(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (highlight) {
                    ProductNavy
                } else {
                    Color.White
                }
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            )
        ) {

            Text(
                text = title,
                color =
                    if (highlight) {
                        Color.White.copy(alpha = 0.75f)
                    } else {
                        ProductMuted
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text = "₹ %.2f".format(value),
                color =
                    if (highlight) {
                        Color.White
                    } else {
                        ProductNavy
                    },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// =============================================================
// TRACEABILITY CONTROL
// =============================================================

@Composable
private fun TraceabilityControl(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (checked) {
                    ProductSoftGreen
                } else {
                    ProductSoftBlue
                }
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = ProductNavy,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = ProductMuted,
                    fontSize = 9.sp
                )
            }


            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ProductGreen
                )
            )
        }
    }
}


// =============================================================
// STATUS CONTROL
// =============================================================

@Composable
private fun StatusControl(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (checked) {
                    ProductSoftGreen
                } else {
                    ProductSoftBlue
                }
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Product Status",
                    color = ProductNavy,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        if (checked) {
                            "ACTIVE"
                        } else {
                            "INACTIVE"
                        },
                    color =
                        if (checked) {
                            ProductGreen
                        } else {
                            ProductDanger
                        },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ProductGreen
                )
            )
        }
    }
}