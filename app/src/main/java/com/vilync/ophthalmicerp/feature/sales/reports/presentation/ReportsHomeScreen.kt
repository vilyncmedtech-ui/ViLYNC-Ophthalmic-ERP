package com.vilync.ophthalmicerp.feature.sales.reports.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun ReportsHomeScreen(
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onSalesTransactionReportClick: () -> Unit = {},
    onProductWiseSalesReportClick: () -> Unit = {},
    onStockReportsClick: () -> Unit = {},
    onInventoryMovementReportClick: () -> Unit = {},
    onInventoryAgeingReportClick: () -> Unit = {},
    onLowStockAlertClick: () -> Unit = {},
    onLensLibraryStatusReportClick: () -> Unit = {},
    onGstReportsClick: () -> Unit = {},
    onCustomerLedgerClick: () -> Unit = {},
    onSupplierLedgerClick: () -> Unit = {},
    onCashBookClick: () -> Unit = {},
    onBankBookClick: () -> Unit = {},
    onFinancialStatementsClick: () -> Unit = {},
    onBulkInvoiceHubClick: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        Log.d("Phase1Verify", "ReportsHomeScreen LaunchedEffect triggered.")
    }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFF), Color(0xFFF4F7FF))))
    ) {
        val isTabletLandscape = maxWidth >= 900.dp
        val columns = if (isTabletLandscape) 4 else 2

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.size(36.dp).clickable { onBack() },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3455A4))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Reports Hub", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2530))
                        Text(text = "Business performance analytics", fontSize = 11.sp, color = Color(0xFF6B7280))
                    }

                    Card(
                        modifier = Modifier.clickable { onDashboard() },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                    ) {
                        Text(
                            text = "Dashboard",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3455A4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Sales Reports
            ReportSectionTitle("Sales Reports")
            GridRows(
                items = listOf(
                    ReportTileData("Sales Report", "₹", Color(0xFFE7F0FF), Color(0xFF3455A4), 
                        onClick = { expandedSection = if (expandedSection == "sales") null else "sales" }),
                    ReportTileData("Bulk Invoice Hub", "📁", Color(0xFFFFF1C9), Color(0xFFB8860B), 
                        onClick = onBulkInvoiceHubClick)
                ),
                columns = columns
            ) { tile -> ReportTile(tile) }

            if (expandedSection == "sales") {
                Spacer(modifier = Modifier.height(8.dp))
                GridRows(
                    items = listOf(
                        ReportTileData("Transaction Report", "T", Color.White, Color(0xFF3455A4), onClick = onSalesTransactionReportClick, isSubItem = true),
                        ReportTileData("Product Wise Summary", "P", Color.White, Color(0xFF3455A4), onClick = onProductWiseSalesReportClick, isSubItem = true)
                    ),
                    columns = columns
                ) { tile -> ReportTile(tile) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Stock & Inventory
            ReportSectionTitle("Stock & Inventory")
            GridRows(
                items = listOf(
                    ReportTileData("Lens Library Status", "▣", Color(0xFFEDE6FF), Color(0xFF7D57D1), onClick = onLensLibraryStatusReportClick),
                    ReportTileData("Movement Register", "⇄", Color(0xFFF0F5FF), Color(0xFF3267C8), onClick = onInventoryMovementReportClick),
                    ReportTileData("Inventory Ageing", "⏳", Color(0xFFFFF9F0), Color(0xFFC98A00), onClick = onInventoryAgeingReportClick),
                    ReportTileData("Low Stock Alert", "⚠", Color(0xFFFFF5E9), Color(0xFFC96A12), onClick = onLowStockAlertClick)
                ),
                columns = columns
            ) { tile -> ReportTile(tile) }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Financial Reports
            ReportSectionTitle("Financial Reports")
            GridRows(
                items = listOf(
                    ReportTileData("Financial Statements", "📈", Color(0xFFF1F6FB), Color(0xFF3455A4), onClick = onFinancialStatementsClick),
                    ReportTileData("Ledgers & Books", "₹", Color(0xFFFFEEF5), Color(0xFFC43E7A), 
                        onClick = { expandedSection = if (expandedSection == "financial") null else "financial" })
                ),
                columns = columns
            ) { tile -> ReportTile(tile) }

            if (expandedSection == "financial") {
                Spacer(modifier = Modifier.height(8.dp))
                GridRows(
                    items = listOf(
                        ReportTileData("Customer Ledger", "C", Color.White, Color(0xFFC43E7A), onClick = onCustomerLedgerClick, isSubItem = true),
                        ReportTileData("Supplier Ledger", "S", Color.White, Color(0xFFC43E7A), onClick = onSupplierLedgerClick, isSubItem = true),
                        ReportTileData("Cash Book", "C", Color.White, Color(0xFFC43E7A), onClick = onCashBookClick, isSubItem = true),
                        ReportTileData("Bank Book", "B", Color.White, Color(0xFFC43E7A), onClick = onBankBookClick, isSubItem = true)
                    ),
                    columns = columns
                ) { tile -> ReportTile(tile) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Tax & Compliance
            ReportSectionTitle("Tax & Compliance")
            GridRows(
                items = listOf(
                    ReportTileData("GST Filing Reports", "G", Color(0xFFDFF6E8), Color(0xFF14945A), onClick = onGstReportsClick)
                ),
                columns = columns
            ) { tile -> ReportTile(tile) }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun ReportTile(item: ReportTileData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSubItem) 1.dp else 2.dp),
        border = if (item.isSubItem) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E6ED)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(if (item.isSubItem) item.iconColor.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.84f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = item.iconColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF252A33),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun <T> GridRows(
    items: List<T>,
    columns: Int,
    horizontalGap: Dp = 10.dp,
    verticalGap: Dp = 10.dp,
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

private data class ReportTileData(
    val title: String,
    val iconText: String,
    val backgroundColor: Color,
    val iconColor: Color,
    val isSubItem: Boolean = false,
    val onClick: () -> Unit
)
