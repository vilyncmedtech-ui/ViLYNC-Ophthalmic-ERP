package com.vilync.ophthalmicerp.feature.inventory.adjustment.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    viewModel: StockAdjustmentViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuccess() },
            title = { Text("Adjustment Recorded") },
            text = { Text("The stock adjustment has been successfully saved and recorded in movement history.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSuccess() }) {
                    Text("OK")
                }
            },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF14945A)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Adjustment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFF)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFF))
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Product Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ITEM SELECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    var productSearch by remember { mutableStateOf("") }
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedTextField(
                            value = uiState.selectedProduct?.productName ?: productSearch,
                            onValueChange = { 
                                productSearch = it
                                expanded = true 
                            },
                            label = { Text("Select Product") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Inventory, null) },
                            readOnly = uiState.selectedProduct != null
                        )
                        
                        DropdownMenu(
                            expanded = expanded && uiState.selectedProduct == null,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            uiState.products.filter { it.productName.contains(productSearch, ignoreCase = true) }.take(10).forEach { product ->
                                DropdownMenuItem(
                                    text = { Text("${product.productName} (${product.model})") },
                                    onClick = {
                                        viewModel.onProductSelected(product)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    val selectedProduct = uiState.selectedProduct
                    if (selectedProduct != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Selected: ${selectedProduct.productName}", fontWeight = FontWeight.SemiBold, color = Color(0xFF3455A4))
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.onProductSelected(selectedProduct.copy(id = -1)) /* Trigger reset */; viewModel.resetSuccess() }) {
                                Text("Change")
                            }
                        }

                        OutlinedTextField(
                            value = uiState.selectedPower,
                            onValueChange = viewModel::onPowerChanged,
                            label = { Text("Power (e.g. 19)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter power") }
                        )
                    }
                }
            }

            // 2. Serial Selection (if applicable)
            if (uiState.selectedProduct?.serialNumberRequired == true && uiState.selectedPower.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SERIAL SELECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        
                        if (uiState.availableSerials.isEmpty()) {
                            Text("No available serials found for this power.", color = Color.Red, fontSize = 14.sp)
                        } else {
                            var serialExpanded by remember { mutableStateOf(false) }
                            
                            Box {
                                OutlinedTextField(
                                    value = uiState.serialSearchQuery,
                                    onValueChange = { 
                                        viewModel.onSerialSearchQueryChanged(it)
                                        serialExpanded = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Search Physical Serial") },
                                    placeholder = { Text("Enter serial number") },
                                    trailingIcon = {
                                        if (uiState.selectedSerialId > 0L) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF14945A))
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = serialExpanded && uiState.filteredSerials.isNotEmpty(), 
                                    onDismissRequest = { serialExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    uiState.filteredSerials.take(10).forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.serialNumber) },
                                            onClick = {
                                                viewModel.onSerialSelected(unit)
                                                serialExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            if (uiState.serialSearchQuery.isNotBlank() && 
                                uiState.selectedSerialId == 0L && 
                                uiState.filteredSerials.isEmpty()) {
                                Text("Serial not available for adjustment.", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Adjustment Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ADJUSTMENT DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    val reasons = listOf("SHORTAGE", "DAMAGED", "DESTROYED")
                    var reasonExpanded by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedTextField(
                            value = uiState.reason,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().clickable { reasonExpanded = true },
                            readOnly = true,
                            label = { Text("Reason for Adjustment") },
                            placeholder = { Text("Select Reason") },
                            trailingIcon = {
                                IconButton(onClick = { reasonExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                            reasons.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        viewModel.onReasonChanged(r)
                                        reasonExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.remarks,
                        onValueChange = viewModel::onRemarksChanged,
                        label = { Text("Remarks / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.errorMessage!!, color = Color.Red, fontSize = 14.sp)
                }
            }

            Button(
                onClick = { viewModel.submitAdjustment() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3455A4)),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("CONFIRM ADJUSTMENT", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
