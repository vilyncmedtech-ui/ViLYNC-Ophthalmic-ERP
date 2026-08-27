package com.vilync.ophthalmicerp.feature.inventory.threshold.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryThresholdScreen(
    viewModel: InventoryThresholdViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stock Alert Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.powerThresholds.any { it.isModified }) {
                        Button(
                            onClick = { viewModel.saveChanges() },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF345FA8))
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BRAND SELECTOR
            FilterDropdown(
                label = "Company / Brand",
                selected = uiState.selectedBrand ?: "Select Company",
                options = uiState.brands,
                onSelected = { viewModel.selectBrand(it) }
            )

            // PRODUCT SELECTOR
            if (uiState.selectedBrand != null) {
                FilterDropdown(
                    label = "Product",
                    selected = uiState.selectedProduct?.productName ?: "Select Product",
                    options = uiState.products.map { it.productName },
                    onSelected = { name -> 
                        uiState.products.find { it.productName == name }?.let { viewModel.selectProduct(it) }
                    }
                )
            }

            // THRESHOLD TABLE
            if (uiState.selectedProduct != null) {
                Text(
                    text = "Configure Thresholds for ${uiState.selectedProduct?.productName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Gray
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ThresholdTableHeader()
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.powerThresholds) { item ->
                            ThresholdRow(
                                item = item,
                                onUpdate = { min, reorder -> viewModel.updateThreshold(item.power, min, reorder) }
                            )
                            HorizontalDivider(color = Color(0xFFF2F4F7))
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a company and product to manage thresholds.", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF345FA8))
        Box {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selected, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Variant", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Stock", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Min", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Reorder", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Status", modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun ThresholdRow(
    item: PowerThresholdItem,
    onUpdate: (String, String) -> Unit
) {
    val status = remember(item.currentStock, item.minimumStock, item.reorderLevel) {
        val qty = item.currentStock
        val min = item.minimumStock.toIntOrNull() ?: 0
        val reorder = item.reorderLevel.toIntOrNull() ?: 0
        
        when {
            qty == 0 -> "Out of Stock"
            qty < min -> "Low Stock"
            qty < reorder -> "Reorder"
            else -> "Normal"
        }
    }
    
    val statusColor = when(status) {
        "Out of Stock" -> Color(0xFFB42318)
        "Low Stock" -> Color(0xFFB54708)
        "Reorder" -> Color(0xFF345FA8)
        else -> Color(0xFF027A48)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (item.power.isBlank()) "Default" else item.power,
            modifier = Modifier.weight(1.2f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.currentStock.toString(),
            modifier = Modifier.weight(0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.currentStock == 0) Color.Red else Color.Black
        )
        
        OutlinedTextField(
            value = item.minimumStock,
            onValueChange = { onUpdate(it, item.reorderLevel) },
            modifier = Modifier.weight(0.8f).height(48.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )
        
        OutlinedTextField(
            value = item.reorderLevel,
            onValueChange = { onUpdate(item.minimumStock, it) },
            modifier = Modifier.weight(0.8f).height(48.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )

        Text(
            text = status,
            modifier = Modifier.weight(0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            maxLines = 1
        )
    }
}
