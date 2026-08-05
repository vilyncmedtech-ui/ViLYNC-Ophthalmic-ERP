package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import kotlinx.coroutines.launch

@Composable
fun AddOpeningStockItemScreen(
    productRepository: ProductMasterRepository,
    onAddItem: (OpeningStockUiItem) -> Unit,
    onBack: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductMaster?>(null) }
    var model by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var batchSerial by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitCost by remember { mutableStateOf("0") }
    
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<ProductMaster>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

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
                "Add Item",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { 
                            productName = it
                            expanded = true
                            scope.launch {
                                productRepository.searchProducts(it).collect { suggestions = it }
                            }
                        },
                        label = { Text("Search Product") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded && suggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        suggestions.forEach { product ->
                            DropdownMenuItem(
                                text = { Text("${product.productName} (${product.model})") },
                                onClick = {
                                    selectedProduct = product
                                    productName = product.productName
                                    model = product.model
                                    category = product.category.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { power = it },
                        label = { Text("Power") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchSerial,
                        onValueChange = { batchSerial = it },
                        label = { 
                            val label = if (selectedProduct?.serialNumberRequired == true) "Serial Number" else "Batch Number"
                            Text(label) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        readOnly = selectedProduct?.serialNumberRequired == true
                    )
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry (MMYY)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = unitCost,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) unitCost = it },
                    label = { Text("Unit Cost") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Button(
                    onClick = {
                        val product = selectedProduct ?: return@Button
                        val qty = quantity.toIntOrNull() ?: 1
                        val cost = unitCost.toDoubleOrNull() ?: 0.0
                        onAddItem(
                            OpeningStockUiItem(
                                productId = product.id,
                                productName = product.productName,
                                model = product.model,
                                power = power,
                                batchNumber = batchSerial,
                                expiryDate = expiryDate,
                                quantity = qty,
                                unitCost = cost,
                                totalCost = qty * cost,
                                trackingType = if (product.serialNumberRequired) "SERIAL" else "QUANTITY"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF476EA8)),
                    enabled = selectedProduct != null
                ) {
                    Text("Add to List", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
