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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            HeaderCard(
                activeFinancialYear = activeFinancialYear,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    SectionTitle("Business Overview")
                    BusinessOverviewGrid(
                        uiState = uiState,
                        onCustomerOverdueClick = onCustomerOverdueClick,
                        onSupplierOverdueClick = onSupplierOverdueClick,
                        onExpiryAlertClick = onExpiryAlertClick,
                        onLowStockClick = onLowStockClick,
                        onPendingSamplesClick = onPendingSamplesClick,
                        onPendingChallansClick = onPendingChallansClick,
                        columns = if (isTabletLandscape) 3 else 2
                    )

                    SectionTitle("Quick Actions")
                    QuickActionsGrid(
                        onNewSaleClick = onNewSaleClick,
                        onNewPurchaseClick = onNewPurchaseClick,
                        onNewChallanClick = onNewChallanClick,
                        onReceivePaymentClick = onReceivePaymentClick,
                        columns = if (isTabletLandscape) 4 else 2
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
                        columns = if (isTabletLandscape) 4 else 3
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Search overlay handled by GlobalSearchBar internal logic or here
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
            placeholder = { Text("Search Product / Serial / Document") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
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
                                    headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            if (uiState.customerOverdueCount > 0) "₹ %.2f".format(uiState.customerOverdueAmount) else "No Overdue",
            if (uiState.customerOverdueCount > 0) "${uiState.customerOverdueCount} Customers" else "Account Healthy",
            Icons.Default.Warning,
            if (uiState.customerOverdueCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onCustomerOverdueClick
        ),
        KpiItem(
            "Supplier Overdue",
            if (uiState.supplierOverdueCount > 0) "₹ %.2f".format(uiState.supplierOverdueAmount) else "No Overdue",
            if (uiState.supplierOverdueCount > 0) "${uiState.supplierOverdueCount} Suppliers" else "All Paid",
            Icons.Default.Payment,
            if (uiState.supplierOverdueCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onSupplierOverdueClick
        ),
        KpiItem(
            "Expiry Alert",
            if (uiState.expiryAlertCount > 0) uiState.expiryAlertCount.toString() else "Safe",
            if (uiState.expiryAlertCount > 0) "Items (90 Days)" else "No Expiring Products",
            Icons.Default.EventBusy,
            if (uiState.expiryAlertCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onExpiryAlertClick
        ),
        KpiItem(
            "Low Stock",
            if (uiState.lowStockCount > 0) uiState.lowStockCount.toString() else "Sufficient",
            if (uiState.lowStockCount > 0) "Critical Items" else "No Low Stock",
            Icons.Default.Inventory,
            if (uiState.lowStockCount > 0) Color(0xFFB42318) else Color(0xFF027A48),
            onLowStockClick
        ),
        KpiItem(
            "Pending Samples",
            if (uiState.pendingSamplesCount > 0) uiState.pendingSamplesCount.toString() else "None",
            if (uiState.pendingSamplesCount > 0) "Open Documents" else "No Pending Samples",
            Icons.Default.Share,
            if (uiState.pendingSamplesCount > 0) Color(0xFFE57A1F) else Color(0xFF027A48),
            onPendingSamplesClick
        ),
        KpiItem(
            "Pending Challans",
            if (uiState.pendingChallansCount > 0) uiState.pendingChallansCount.toString() else "Clean",
            if (uiState.pendingChallansCount > 0) "Open Challans" else "No Pending Challans",
            Icons.Default.Send,
            if (uiState.pendingChallansCount > 0) Color(0xFFE57A1F) else Color(0xFF027A48),
            onPendingChallansClick
        )
    )

    GridRows(items = items, columns = columns, horizontalGap = 10.dp, verticalGap = 10.dp) { item ->
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
        QuickActionData("New Invoice", Icons.Default.AddShoppingCart, onNewSaleClick),
        QuickActionData("New Purchase", Icons.Default.AddBusiness, onNewPurchaseClick),
        QuickActionData("New Challan", Icons.Default.Description, onNewChallanClick),
        QuickActionData("Receive Payment", Icons.Default.AccountBalanceWallet, onReceivePaymentClick)
    )

    GridRows(items = items, columns = columns, horizontalGap = 10.dp, verticalGap = 10.dp) { item ->
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
    columns: Int
) {
    val modules = listOf(
        ModuleData("Sales", "₹", Color(0xFFD7F7E4), Color(0xFF14945A), onSalesClick),
        ModuleData("Purchase", "↓", Color(0xFFFFECD8), Color(0xFFE57A1F), onPurchaseClick),
        ModuleData("Inventory", "▣", Color(0xFFEDE6FF), Color(0xFF7D57D1), onInventoryClick),
        ModuleData("Master", "M", Color(0xFFE7F0FF), Color(0xFF3455A4), onMasterClick),
        ModuleData("Payments", "₹", Color(0xFFFFF2C7), Color(0xFFC98A00), onPaymentsClick),
        ModuleData("GST", "G", Color(0xFFFFE2EB), Color(0xFFD9346B), onGstClick),
        ModuleData("Reports", "▥", Color(0xFFE4F6E7), Color(0xFF348A4A), onReportsClick)
    )

    GridRows(items = modules, columns = columns, horizontalGap = 10.dp, verticalGap = 8.dp) { module ->
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ViLYNC ERP", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2530))
                Text(text = "Ophthalmic Edition", fontSize = 12.sp, color = Color(0xFF6B7280))
            }

            if (activeFinancialYear.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F0FE)
                ) {
                    Text(
                        text = "FY $activeFinancialYear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3455A4),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF4B5563))
            }

            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4569AB), contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Logout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF252A33),
        modifier = Modifier.padding(vertical = 4.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = item.tint)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = item.title, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = item.tint)
            Text(text = item.subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
private fun QuickActionCard(item: QuickActionData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3455A4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun ModuleCard(item: ModuleData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = item.iconColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF252A33), maxLines = 1)
        }
    }
}

private data class KpiItem(val title: String, val value: String, val subtitle: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)
private data class QuickActionData(val title: String, val icon: ImageVector, val onClick: () -> Unit)
private data class ModuleData(val title: String, val iconText: String, val backgroundColor: Color, val iconColor: Color, val onClick: () -> Unit)
