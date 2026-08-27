package com.vilync.ophthalmicerp.feature.dashboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    activeFinancialYear: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchResultClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onCustomerOverdueClick: () -> Unit,
    onSupplierOverdueClick: () -> Unit,
    onExpiryAlertClick: () -> Unit,
    onLowStockClick: () -> Unit,
    onPendingSamplesClick: () -> Unit,
    onPendingChallansClick: () -> Unit,
    onNewSaleClick: () -> Unit,
    onNewPurchaseClick: () -> Unit,
    onNewChallanClick: () -> Unit,
    onReceivePaymentClick: () -> Unit,
    onMasterClick: () -> Unit,
    onSalesClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onGstClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFF), Color(0xFFF4F7FF))))
    ) {
        val isTabletLandscape = maxWidth >= 900.dp
        val isCompactWidth = maxWidth < 400.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp) // Optimized outer padding
        ) {
            HeaderCard(
                activeFinancialYear = activeFinancialYear,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick
            )

            Spacer(modifier = Modifier.height(10.dp)) // Optimized spacing

            GlobalSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                isSearching = uiState.isSearching,
                results = uiState.searchResults,
                onResultClick = onSearchResultClick
            )

            if (uiState.searchQuery.length < 2) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp) // Optimized section spacing
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    SectionTitle("Business Overview")
                    BusinessOverviewGrid(
                        uiState = uiState,
                        onCustomerOverdueClick = onCustomerOverdueClick,
                        onSupplierOverdueClick = onSupplierOverdueClick,
                        onExpiryAlertClick = onExpiryAlertClick,
                        onLowStockClick = onLowStockClick,
                        onPendingSamplesClick = onPendingSamplesClick,
                        onPendingChallansClick = onPendingChallansClick,
                        columns = if (isTabletLandscape) 6 else 3 // Higher density grid
                    )

                    SectionTitle("Quick Actions")
                    QuickActionsGrid(
                        onNewSaleClick = onNewSaleClick,
                        onNewPurchaseClick = onNewPurchaseClick,
                        onNewChallanClick = onNewChallanClick,
                        onReceivePaymentClick = onReceivePaymentClick,
                        columns = if (isCompactWidth) 2 else 4 // Force single row if width allows
                    )

                    SectionTitle("Modules")
                    ModulesGrid(
                        onSalesClick = onSalesClick,
                        onPurchaseClick = onPurchaseClick,
                        onInventoryClick = onInventoryClick,
                        onMasterClick = onMasterClick,
                        onPaymentsClick = onPaymentsClick,
                        onGstClick = onGstClick,
                        onReportsClick = onReportsClick,
                        onSettingsClick = onSettingsClick,
                        columns = if (isTabletLandscape) 4 else 2 // Balanced 4x2 or 2x4 for 8 items
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GlobalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    results: List<SearchResult>,
    onResultClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Product / Serial / Document", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        if (query.length >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 400.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (results.isEmpty() && !isSearching) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No results found", color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        val grouped = results.groupBy { it.category }
                        grouped.forEach { (category, items) ->
                            item {
                                Text(
                                    text = category.uppercase(),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF3455A4),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(items) { item ->
                                ListItem(
                                    headlineContent = { Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(item.subtitle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.clickable { onResultClick(item.route) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessOverviewGrid(
    uiState: DashboardUiState,
    onCustomerOverdueClick: () -> Unit,
    onSupplierOverdueClick: () -> Unit,
    onExpiryAlertClick: () -> Unit,
    onLowStockClick: () -> Unit,
    onPendingSamplesClick: () -> Unit,
    onPendingChallansClick: () -> Unit,
    columns: Int
) {
    val items = listOf(
        KpiItem(
            "Customer Overdue",
            if (uiState.customerOverdueCount > 0) "₹ %.0f".format(uiState.customerOverdueAmount) else "None",
            if (uiState.customerOverdueCount > 0) "${uiState.customerOverdueCount} Parties" else "Healthy",
            Icons.Default.Warning,
            if (uiState.customerOverdueCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onCustomerOverdueClick
        ),
        KpiItem(
            "Supplier Overdue",
            if (uiState.supplierOverdueCount > 0) "₹ %.0f".format(uiState.supplierOverdueAmount) else "None",
            if (uiState.supplierOverdueCount > 0) "${uiState.supplierOverdueCount} Parties" else "Paid",
            Icons.Default.Payment,
            if (uiState.supplierOverdueCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onSupplierOverdueClick
        ),
        KpiItem(
            "Expiry Alert",
            if (uiState.expiryAlertCount > 0) uiState.expiryAlertCount.toString() else "Safe",
            if (uiState.expiryAlertCount > 0) "Items (90d)" else "No Expiry",
            Icons.Default.EventBusy,
            if (uiState.expiryAlertCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onExpiryAlertClick
        ),
        KpiItem(
            "Low Stock",
            if (uiState.lowStockCount > 0) uiState.lowStockCount.toString() else "Safe",
            if (uiState.lowStockCount > 0) "Crit. Items" else "Sufficient",
            Icons.Default.Inventory,
            if (uiState.lowStockCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onLowStockClick
        ),
        KpiItem(
            "Samples",
            if (uiState.pendingSamplesCount > 0) uiState.pendingSamplesCount.toString() else "None",
            "Pending",
            Icons.Default.Share,
            if (uiState.pendingSamplesCount > 0) Color(0xFFE57A1F) else Color(0xFF027A48),
            onPendingSamplesClick
        ),
        KpiItem(
            "Challans",
            if (uiState.pendingChallansCount > 0) uiState.pendingChallansCount.toString() else "None",
            "Pending",
            Icons.Default.Send,
            if (uiState.pendingChallansCount > 0) Color(0xFFE57A1F) else Color(0xFF027A48),
            onPendingChallansClick
        )
    )

    GridRows(items = items, columns = columns, horizontalGap = 8.dp, verticalGap = 8.dp) { item ->
        OverviewCard(item)
    }
}

@Composable
private fun QuickActionsGrid(
    onNewSaleClick: () -> Unit,
    onNewPurchaseClick: () -> Unit,
    onNewChallanClick: () -> Unit,
    onReceivePaymentClick: () -> Unit,
    columns: Int
) {
    val items = listOf(
        QuickActionData("New Sale", Icons.Default.AddShoppingCart, onNewSaleClick),
        QuickActionData("Purchase", Icons.Default.AddBusiness, onNewPurchaseClick),
        QuickActionData("Challan", Icons.Default.Description, onNewChallanClick),
        QuickActionData("Payment", Icons.Default.AccountBalanceWallet, onReceivePaymentClick)
    )

    GridRows(items = items, columns = columns, horizontalGap = 8.dp, verticalGap = 8.dp) { item ->
        QuickActionCard(item)
    }
}

@Composable
private fun ModulesGrid(
    onSalesClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onMasterClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onGstClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    columns: Int
) {
    val modules = listOf(
        ModuleData("Sales", iconText = "₹", backgroundColor = Color(0xFFD7F7E4), iconColor = Color(0xFF14945A), onClick = onSalesClick),
        ModuleData("Purchase", iconText = "↓", backgroundColor = Color(0xFFFFECD8), iconColor = Color(0xFFE57A1F), onClick = onPurchaseClick),
        ModuleData("Inventory", iconText = "▣", backgroundColor = Color(0xFFEDE6FF), iconColor = Color(0xFF7D57D1), onClick = onInventoryClick),
        ModuleData("Master", iconText = "M", backgroundColor = Color(0xFFE7F0FF), iconColor = Color(0xFF3455A4), onClick = onMasterClick),
        ModuleData("Payments", iconText = "₹", backgroundColor = Color(0xFFFFF2C7), iconColor = Color(0xFFC98A00), onClick = onPaymentsClick),
        ModuleData("GST", iconText = "G", backgroundColor = Color(0xFFFFE2EB), iconColor = Color(0xFFD9346B), onClick = onGstClick),
        ModuleData("Reports", iconText = "▥", backgroundColor = Color(0xFFE4F6E7), iconColor = Color(0xFF348A4A), onClick = onReportsClick),
        ModuleData("Settings", icon = Icons.Default.Settings, backgroundColor = Color(0xFFF3F4F6), iconColor = Color(0xFF4B5563), onClick = onSettingsClick)
    )

    GridRows(items = modules, columns = columns, horizontalGap = 8.dp, verticalGap = 8.dp) { module ->
        ModuleCard(module)
    }
}

@Composable
private fun HeaderCard(
    activeFinancialYear: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // Tighter header
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ViLYNC ERP", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2530))
                Text(text = "Ophthalmic Edition", fontSize = 11.sp, color = Color(0xFF6B7280))
            }

            if (activeFinancialYear.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F0FE)
                ) {
                    Text(
                        text = "FY $activeFinancialYear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3455A4),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF4B5563), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4569AB), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "Logout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF252A33),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun <T> GridRows(
    items: List<T>,
    columns: Int,
    horizontalGap: Dp,
    verticalGap: Dp,
    itemContent: @Composable (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(verticalGap)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { itemContent(item) }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(item: KpiItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) { // Tighter internal padding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = item.tint)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = item.title, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = item.tint, maxLines = 1)
            Text(text = item.subtitle, fontSize = 8.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickActionCard(item: QuickActionData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3455A4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ModuleCard(item: ModuleData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp) // Optimized height from 72.dp
            .clickable { item.onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(30.dp).background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.icon != null) {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = item.iconColor)
                } else {
                    Text(text = item.iconText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = item.iconColor)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF252A33), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class KpiItem(val title: String, val value: String, val subtitle: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)
private data class QuickActionData(val title: String, val icon: ImageVector, val onClick: () -> Unit)
private data class ModuleData(val title: String, val iconText: String = "", val icon: ImageVector? = null, val backgroundColor: Color, val iconColor: Color, val onClick: () -> Unit)
