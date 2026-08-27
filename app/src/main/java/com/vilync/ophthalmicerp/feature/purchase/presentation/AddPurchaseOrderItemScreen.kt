package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AddPurchaseOrderItemScreen(
    viewModel: AddPurchaseOrderItemViewModel,
    onAddItems: (List<com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem>) -> Unit = {},
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val productSuggestions by viewModel.productSuggestions.collectAsState()
    var productDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VilyncPageBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Text(
                text = if (uiState.isEditMode) "Edit Order Item" else "Add Order Product",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VilyncNavy
            )
            OutlinedButton(onClick = onDashboard) { Text("⌂ Dashboard") }
        }

        // 1. PRODUCT HEADER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VilyncPastelBlue)
        ) {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PRODUCT SELECTION", fontWeight = FontWeight.Bold)
                Box {
                    OutlinedTextField(
                        value = uiState.productName,
                        onValueChange = { viewModel.updateProductName(it); productDropdownExpanded = true },
                        label = { Text("Search Product Master") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                    )
                    DropdownMenu(
                        expanded = productDropdownExpanded && productSuggestions.isNotEmpty(),
                        onDismissRequest = { productDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        productSuggestions.forEach { product ->
                            DropdownMenuItem(
                                text = { Text("${product.productName} (${product.model})") },
                                onClick = { viewModel.selectProduct(product); productDropdownExpanded = false }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = uiState.model, onValueChange = {}, label = { Text("Model") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.category, onValueChange = {}, label = { Text("Category") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.hsnCode, onValueChange = {}, label = { Text("HSN") }, readOnly = true, modifier = Modifier.weight(0.8f))
                }
            }
        }

        // 2. COMMERCIALS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9F4))
        ) {
            Row(modifier = Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.purchaseRate.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { r -> viewModel.updatePurchaseRate(r) } },
                    label = { Text("Purchase Rate") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )
                OutlinedTextField(
                    value = uiState.gstPercent.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { g -> viewModel.updateGstPercent(g) } },
                    label = { Text("GST %") },
                    modifier = Modifier.weight(0.6f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )
            }
        }

        // 3. VARIANT ROWS (POWER + QTY)
        Text("ORDER VARIANTS", fontWeight = FontWeight.Bold, color = VilyncNavy)
        
        uiState.variants.forEach { variant ->
            VariantRow(
                variant = variant,
                availablePowers = uiState.availablePowers,
                onUpdate = { p, q -> viewModel.updateVariant(variant.id, p, q) },
                onRemove = { viewModel.removeVariant(variant.id) }
            )
        }

        Button(
            onClick = viewModel::addVariant,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0FE), contentColor = Color(0xFF345FA8))
        ) {
            Text("+ Add Power variant")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. TOTALS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14233C))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Quantity: ${uiState.totalQuantity}", color = Color.White, fontSize = 12.sp)
                    Text("GST: ₹${"%.2f".format(uiState.taxAmount)}", color = Color.Gray, fontSize = 11.sp)
                }
                Text("OVERALL TOTAL: ₹${"%.2f".format(uiState.overallTotal)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) }

        Button(
            onClick = { if (viewModel.validate()) onAddItems(viewModel.getPurchaseItems()) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF345FA8))
        ) {
            Text("ADD VARIANTS TO ORDER", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VariantRow(
    variant: OrderItemVariant,
    availablePowers: List<String>,
    onUpdate: (String, Int) -> Unit,
    onRemove: () -> Unit
) {
    var powerDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFDDE3EC)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power Selection
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(
                    value = variant.power,
                    onValueChange = { onUpdate(it, variant.quantity) },
                    label = { Text("Power") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { powerDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                if (availablePowers.isNotEmpty()) {
                    DropdownMenu(
                        expanded = powerDropdownExpanded,
                        onDismissRequest = { powerDropdownExpanded = false }
                    ) {
                        availablePowers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = { onUpdate(p, variant.quantity); powerDropdownExpanded = false }
                            )
                        }
                    }
                }
            }

            // Quantity
            OutlinedTextField(
                value = if (variant.quantity == 0) "" else variant.quantity.toString(),
                onValueChange = { it.toIntOrNull()?.let { q -> onUpdate(variant.power, q) } },
                label = { Text("Qty") },
                modifier = Modifier.weight(0.7f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Remove
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

private val VilyncPageBackground = Color(0xFFF7F9FD)
private val VilyncNavy = Color(0xFF14233C)
private val VilyncPastelBlue = Color(0xFFF2F7FF)
