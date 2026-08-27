package com.vilync.ophthalmicerp.feature.inventory.reconciliation.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReconciliationScreen(
    viewModel: StockReconciliationViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuccess(); onBack() },
            title = { Text("Reconciliation Complete") },
            text = { Text("The physical stock audit has been successfully recorded. All discrepancies have been adjusted.") },
            confirmButton = {
                Button(onClick = { viewModel.resetSuccess(); onBack() }) { Text("Done") }
            },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF14945A)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Reconciliation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFF))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFF))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Context Selection (Upper Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AUDIT CONTEXT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    ProductSelector(
                        selectedProduct = uiState.selectedProduct,
                        products = uiState.products,
                        onProductSelected = viewModel::onProductSelected
                    )

                    if (uiState.selectedProduct != null) {
                        OutlinedTextField(
                            value = uiState.powerInput,
                            onValueChange = viewModel::onPowerChanged,
                            label = { Text("Power (e.g. 19)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter power to start audit") }
                        )
                    }
                }
            }

            if (uiState.selectedProduct != null && uiState.normalizedPower.isNotBlank()) {
                // 2. Statistics Bar
                val matchedCount = uiState.physicalEntries.count { it.status == ValidationStatus.MATCHED }
                val extraCount = uiState.physicalEntries.count { it.status != ValidationStatus.MATCHED }
                val systemCount = uiState.systemSerials.size
                val missingCount = systemCount - matchedCount

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("System", systemCount.toString(), Color(0xFF3455A4), Modifier.weight(1f))
                    StatCard("Matched", matchedCount.toString(), Color(0xFF14945A), Modifier.weight(1f))
                    StatCard("Missing", missingCount.toString(), Color(0xFFD32F2F), Modifier.weight(1f))
                    StatCard("Extra", extraCount.toString(), Color(0xFFE57A1F), Modifier.weight(1f))
                }

                // 3. Serial Entry & Comparison
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = uiState.serialInput,
                                onValueChange = viewModel::onSerialInputChanged,
                                label = { Text("Scan / Type Physical Serial") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = viewModel::addPhysicalSerial) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("PHYSICAL VERIFICATION LIST", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        
                        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            items(uiState.physicalEntries.reversed()) { entry ->
                                SerialAuditRow(entry, onRemove = { viewModel.removePhysicalSerial(entry.serialNumber) })
                                HorizontalDivider(color = Color(0xFFF2F4F7))
                            }
                        }
                    }
                }

                // 4. Action Area
                if (uiState.errorMessage != null) {
                    Text(uiState.errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                Button(
                    onClick = viewModel::submitReconciliation,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3455A4)),
                    enabled = !uiState.isLoading && !uiState.isValidatingSerial
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else if (uiState.isValidatingSerial) Text("VALIDATING...", fontWeight = FontWeight.Bold)
                    else Text("COMPLETE RECONCILIATION", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProductSelector(
    selectedProduct: ProductMaster?,
    products: List<ProductMaster>,
    onProductSelected: (ProductMaster) -> Unit
) {
    var productSearch by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedProduct?.productName ?: productSearch,
            onValueChange = { productSearch = it; expanded = true },
            label = { Text("Select Product") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Inventory, null) },
            readOnly = selectedProduct != null
        )
        
        DropdownMenu(
            expanded = expanded && selectedProduct == null,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            products.filter { it.productName.contains(productSearch, ignoreCase = true) }.take(10).forEach { product ->
                DropdownMenuItem(
                    text = { Text("${product.productName} (${product.model})") },
                    onClick = { onProductSelected(product); expanded = false }
                )
            }
        }
    }

    if (selectedProduct != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Selected: ${selectedProduct.productName}", fontWeight = FontWeight.SemiBold, color = Color(0xFF3455A4), fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onProductSelected(selectedProduct.copy(id = -1)) }) { Text("Change", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SerialAuditRow(entry: PhysicalSerialEntry, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color, label) = when (entry.status) {
            ValidationStatus.MATCHED -> Triple(Icons.Default.CheckCircle, Color(0xFF14945A), "Matched")
            ValidationStatus.WRONG_PRODUCT -> Triple(Icons.Default.Warning, Color(0xFFD32F2F), "Wrong Product")
            ValidationStatus.WRONG_POWER -> Triple(Icons.Default.Warning, Color(0xFFD32F2F), "Wrong Power")
            ValidationStatus.ALREADY_CONSUMED -> Triple(Icons.Default.Block, Color(0xFFD32F2F), "Not in Stock")
            ValidationStatus.UNKNOWN -> Triple(Icons.Default.Help, Color(0xFFE57A1F), "Unknown Serial")
            ValidationStatus.PENDING -> Triple(Icons.Default.Schedule, Color.Gray, "Validating...")
        }

        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.serialNumber, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(if (entry.errorMessage != null) "${label}: ${entry.errorMessage}" else label, fontSize = 11.sp, color = color)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}
