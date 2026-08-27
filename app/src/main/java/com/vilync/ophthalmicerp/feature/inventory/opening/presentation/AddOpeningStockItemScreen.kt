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
import com.vilync.ophthalmicerp.core.util.SerialFormatter
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Local state for a single physical serialized unit.
 */
data class OpeningStockUnitEntry(
    val rawSerial: String = "",
    val serialNumber: String = "",
    val expiryDate: String = ""
)

@Composable
fun AddOpeningStockItemScreen(
    productRepository: ProductMasterRepository,
    onAddItem: (List<OpeningStockUiItem>) -> Unit,
    onBack: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductMaster?>(null) }
    var model by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("0") }
    var gstPercent by remember { mutableStateOf("0") }
    
    // For SERIAL tracked products, we manage multiple physical units.
    var unitDetails by remember { mutableStateOf(listOf(OpeningStockUnitEntry())) }
    
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<ProductMaster>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val isSerialTracked = selectedProduct?.serialNumberRequired == true

    // Synchronize unitDetails with quantity for SERIAL items
    LaunchedEffect(quantity, isSerialTracked) {
        if (isSerialTracked) {
            val q = quantity.toIntOrNull() ?: 0
            if (q > 0 && q <= 100) {
                if (unitDetails.size < q) {
                    unitDetails = unitDetails + List(q - unitDetails.size) { OpeningStockUnitEntry() }
                } else if (unitDetails.size > q) {
                    unitDetails = unitDetails.take(q)
                }
            }
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
                                    rate = product.purchasePrice.toString()
                                    gstPercent = product.gstPercent.toString()
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
                    if (!isSerialTracked) {
                        OutlinedTextField(
                            value = batchNumber,
                            onValueChange = { batchNumber = it },
                            label = { Text("Batch Number") },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // For serial items, quantity field is primary.
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                if (!isSerialTracked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Expiry (MMYY)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) rate = it },
                        label = { Text("Rate (Excl. GST)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = gstPercent,
                        onValueChange = { },
                        label = { Text("GST %") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F4F7),
                            unfocusedContainerColor = Color(0xFFF2F4F7)
                        )
                    )
                }

                // =====================================================
                // SERIAL DETAILS (MULTI-UNIT ENTRY)
                // =====================================================
                if (isSerialTracked && unitDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SERIAL DETAILS", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp, 
                        color = Color(0xFF476EA8)
                    )
                    
                    unitDetails.forEachIndexed { index, unit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE9FA))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Unit ${index + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = unit.rawSerial,
                                        onValueChange = { value ->
                                            val list = unitDetails.toMutableList()
                                            val prefix = selectedProduct?.serialPrefix ?: ""
                                            list[index] = unit.copy(
                                                rawSerial = value,
                                                serialNumber = SerialFormatter.format(prefix, value)
                                            )
                                            unitDetails = list
                                        },
                                        label = { Text("Serial No (Numeric)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = unit.expiryDate,
                                        onValueChange = { value ->
                                            val list = unitDetails.toMutableList()
                                            list[index] = unit.copy(expiryDate = value)
                                            unitDetails = list
                                        },
                                        label = { Text("EXP (MMYY)") },
                                        modifier = Modifier.weight(0.7f),
                                        singleLine = true
                                    )
                                }
                                
                                Text(
                                    text = "Full Serial: ${unit.serialNumber.ifBlank { "—" }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF667085)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val product = selectedProduct ?: return@Button
                        val qty = quantity.toIntOrNull() ?: 1
                        val r = rate.toDoubleOrNull() ?: 0.0
                        val g = gstPercent.toDoubleOrNull() ?: 0.0
                        
                        val items = if (isSerialTracked) {
                            unitDetails.map { unit ->
                                OpeningStockUiItem(
                                    productId = product.id,
                                    productName = product.productName,
                                    model = product.model,
                                    power = power,
                                    batchNumber = "", // Primary identity is in serialNumber
                                    rawSerial = unit.rawSerial,
                                    serialNumber = unit.serialNumber,
                                    expiryDate = unit.expiryDate,
                                    quantity = 1,
                                    unitCost = r,
                                    gstPercent = g,
                                    totalCost = money(1 * r * (1 + g / 100.0)),
                                    trackingType = "SERIAL"
                                )
                            }
                        } else {
                            listOf(
                                OpeningStockUiItem(
                                    productId = product.id,
                                    productName = product.productName,
                                    model = product.model,
                                    power = power,
                                    batchNumber = batchNumber,
                                    rawSerial = "",
                                    serialNumber = "",
                                    expiryDate = expiryDate,
                                    quantity = qty,
                                    unitCost = r,
                                    gstPercent = g,
                                    totalCost = money(qty * r * (1 + g / 100.0)),
                                    trackingType = "QUANTITY"
                                )
                            )
                        }
                        
                        onAddItem(items)
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

private fun money(value: Double): Double {
    return round(value * 100.0) / 100.0
}
