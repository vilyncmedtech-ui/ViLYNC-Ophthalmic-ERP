package com.vilync.ophthalmicerp.feature.inventory.alert.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.inventory.alert.LowStockAlertUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockStatusScreen(
    viewModel: LowStockStatusViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onPurchaseEntry: () -> Unit,
    onPurchaseEntryWithParams: (Long, String, Int) -> Unit,
    onThresholdSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val items by viewModel.filteredItems.collectAsState()

    val backgroundColor = Color(0xFFF5F7FF)
    val blueAccent = Color(0xFF345FA8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Low Stock Status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Variant-wise inventory health", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", tint = blueAccent)
                    }
                    IconButton(onClick = onThresholdSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = blueAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPurchaseEntry,
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("Create Purchase Order") },
                containerColor = blueAccent,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SUMMARY CARDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Out of Stock",
                    count = uiState.outOfStockCount,
                    color = Color(0xFFB42318),
                    backgroundColor = Color(0xFFFEF3F2),
                    icon = Icons.Default.Cancel
                )
                StatusSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Low Stock",
                    count = uiState.lowStockCount,
                    color = Color(0xFFB54708),
                    backgroundColor = Color(0xFFFFFAEB),
                    icon = Icons.Default.Warning
                )
                StatusSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Alerts",
                    count = uiState.totalAlerts,
                    color = blueAccent,
                    backgroundColor = Color(0xFFEFF8FF),
                    icon = Icons.Default.NotificationsActive
                )
            }

            // 2. SEARCH & FILTER TABS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text("Search product or power...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterTab(
                            text = "Shortages",
                            isSelected = uiState.selectedFilter == AlertFilter.SHORTAGES_ONLY,
                            onClick = { viewModel.updateFilter(AlertFilter.SHORTAGES_ONLY) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterTab(
                            text = "Out of Stock",
                            isSelected = uiState.selectedFilter == AlertFilter.OUT_OF_STOCK,
                            onClick = { viewModel.updateFilter(AlertFilter.OUT_OF_STOCK) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterTab(
                            text = "All",
                            isSelected = uiState.selectedFilter == AlertFilter.ALL,
                            onClick = { viewModel.updateFilter(AlertFilter.ALL) },
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }
            }

            // 3. VARIANT LIST
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No stock alerts found matching your criteria.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items, key = { (it.snapshot.productId.toString() + it.snapshot.power) }) { item ->
                        LowStockVariantRow(
                            item = item,
                            onClick = { 
                                val shortage = (item.snapshot.minimumStock - item.snapshot.availableQuantity).coerceAtLeast(0)
                                onPurchaseEntryWithParams(item.snapshot.productId, item.snapshot.power, shortage)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color,
    backgroundColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier.height(84.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) Color.White else Color(0xFF475467)
    val bgColor = if (isSelected) Color(0xFF345FA8) else Color(0xFFF2F4F7)

    Surface(
        modifier = modifier.height(36.dp).clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

@Composable
private fun LowStockVariantRow(
    item: LowStockAlertItem,
    onClick: () -> Unit
) {
    val snapshot = item.snapshot
    
    val statusColor = when (item.status) {
        LowStockAlertUseCase.STATUS_OUT_OF_STOCK -> Color(0xFFB42318)
        LowStockAlertUseCase.STATUS_LOW_STOCK -> Color(0xFFB54708)
        LowStockAlertUseCase.STATUS_REORDER -> Color(0xFF345FA8)
        else -> Color(0xFF027A48)
    }

    val statusIcon = when (item.status) {
        LowStockAlertUseCase.STATUS_OUT_OF_STOCK -> "🔴"
        LowStockAlertUseCase.STATUS_LOW_STOCK -> "🔶"
        LowStockAlertUseCase.STATUS_REORDER -> "ℹ️"
        else -> "✅"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(snapshot.productName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snapshot.brandName, fontSize = 11.sp, color = Color.Gray)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusIcon, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.status.replace("_", " "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF2F4F7))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Variant/Power", snapshot.power.ifBlank { "Default" }, Modifier.weight(1.2f))
                InfoColumn("Stock", snapshot.availableQuantity.toString(), Modifier.weight(0.6f), isBold = true)
                InfoColumn("Min", snapshot.minimumStock.toString(), Modifier.weight(0.7f))
                InfoColumn("Reorder", snapshot.reorderLevel.toString(), Modifier.weight(0.7f))
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, modifier: Modifier = Modifier, isBold: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium)
    }
}
