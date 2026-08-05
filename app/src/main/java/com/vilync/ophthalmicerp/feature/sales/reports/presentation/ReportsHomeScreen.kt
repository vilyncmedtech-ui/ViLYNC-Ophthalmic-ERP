package com.vilync.ophthalmicerp.feature.sales.reports.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    onGstReportsClick: () -> Unit = {},
    onCustomerLedgerClick: () -> Unit = {},
    onSupplierLedgerClick: () -> Unit = {},
    onCashBookClick: () -> Unit = {},
    onBankBookClick: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        Log.d("Phase1Verify", "ReportsHomeScreen LaunchedEffect triggered.")
    }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFF),
                        Color(0xFFF4F7FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onBack() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3455A4))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Reports Hub", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2530))
                        Text(text = "Analyze your business performance", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }

                    Card(
                        modifier = Modifier.clickable { onDashboard() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                    ) {
                        Text(
                            text = "Dashboard",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3455A4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sales Reports
            ReportSectionTitle("Sales Reports")
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportMenuCard(
                    icon = "₹",
                    title = "Sales Report",
                    subtitle = "Comprehensive sales analysis and transaction logs",
                    backgroundColor = Color(0xFFE7F0FF),
                    iconColor = Color(0xFF3455A4),
                    onClick = { 
                        expandedSection = if (expandedSection == "sales") null else "sales"
                    }
                )

                if (expandedSection == "sales") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubReportTab(
                            title = "Sales Transaction Report",
                            modifier = Modifier.weight(1f),
                            onClick = onSalesTransactionReportClick
                        )
                        SubReportTab(
                            title = "Product Wise Sales Report",
                            modifier = Modifier.weight(1f),
                            onClick = onProductWiseSalesReportClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Stock Reports
            ReportSectionTitle("Stock & Inventory")
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportMenuCard(
                    icon = "▣",
                    title = "Inventory Valuation",
                    subtitle = "Current stock value and aging reports",
                    backgroundColor = Color(0xFFEDE6FF),
                    iconColor = Color(0xFF7D57D1),
                    onClick = onStockReportsClick
                )

                ReportMenuCard(
                    icon = "⇄",
                    title = "Inventory Movement Register",
                    subtitle = "Complete stock movement history and master ledger",
                    backgroundColor = Color(0xFFF0F5FF),
                    iconColor = Color(0xFF3267C8),
                    onClick = onInventoryMovementReportClick
                )

                ReportMenuCard(
                    icon = "⏳",
                    title = "Inventory Ageing",
                    subtitle = "Stock classification by shelf days and movement speed",
                    backgroundColor = Color(0xFFFFF9F0),
                    iconColor = Color(0xFFC98A00),
                    onClick = onInventoryAgeingReportClick
                )

                ReportMenuCard(
                    icon = "⚠",
                    title = "Low Stock Alert",
                    subtitle = "Identify items below reorder thresholds",
                    backgroundColor = Color(0xFFFFF5E9),
                    iconColor = Color(0xFFC96A12),
                    onClick = onLowStockAlertClick
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Financial Reports
            ReportSectionTitle("Financial Reports")
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportMenuCard(
                    icon = "₹",
                    title = "Ledgers & Books",
                    subtitle = "Customer/Supplier ledgers and Cash/Bank books",
                    backgroundColor = Color(0xFFFFEEF5),
                    iconColor = Color(0xFFC43E7A),
                    onClick = {
                        expandedSection = if (expandedSection == "financial") null else "financial"
                    }
                )

                if (expandedSection == "financial") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SubReportTab(
                                title = "Customer Ledger",
                                modifier = Modifier.weight(1f),
                                onClick = onCustomerLedgerClick
                            )
                            SubReportTab(
                                title = "Supplier Ledger",
                                modifier = Modifier.weight(1f),
                                onClick = onSupplierLedgerClick
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SubReportTab(
                                title = "Cash Book",
                                modifier = Modifier.weight(1f),
                                onClick = onCashBookClick
                            )
                            SubReportTab(
                                title = "Bank Book",
                                modifier = Modifier.weight(1f),
                                onClick = onBankBookClick
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gst Reports
            ReportSectionTitle("Tax & Compliance")
            Spacer(modifier = Modifier.height(8.dp))
            ReportMenuCard(
                icon = "G",
                title = "GST Filing Reports",
                subtitle = "Data for GSTR-1 and tax summaries",
                backgroundColor = Color(0xFFDFF6E8),
                iconColor = Color(0xFF14945A),
                onClick = onGstReportsClick
            )
        }
    }
}

@Composable
private fun SubReportTab(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E6ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF345FA8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ReportSectionTitle(title: String) {
    Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF252A33))
}

@Composable
private fun ReportMenuCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = Color.White.copy(alpha = 0.84f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = iconColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF252A33), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF667085), maxLines = 2)
            }
        }
    }
}
