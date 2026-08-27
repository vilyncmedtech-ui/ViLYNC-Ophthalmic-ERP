package com.vilync.ophthalmicerp.core.reports.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color

object ReportSchemas {

    val SalesTransactionSchema = ReportSchema(
        id = "sales_transactions",
        title = "Sales Transaction Report",
        subtitle = "Detailed analysis of sales documents",
        icon = "₹",
        columns = listOf(
            ReportColumn(id = "invoiceNo", displayName = "Invoice No.", weight = 1.2f),
            ReportColumn(id = "date", displayName = "Date", weight = 1.0f),
            ReportColumn(id = "customer", displayName = "Customer", weight = 2.5f),
            ReportColumn(id = "customerGstin", displayName = "Party GST No", weight = 1.8f),
            ReportColumn(id = "product", displayName = "Product", weight = 2.5f),
            ReportColumn(id = "power", displayName = "Power", weight = 0.8f),
            ReportColumn(id = "serial", displayName = "Serial No", weight = 1.5f),
            ReportColumn(id = "qty", displayName = "Qty", weight = 0.6f, type = ColumnType.NUMBER),
            ReportColumn(id = "gst_percent", displayName = "GST %", weight = 0.8f),
            ReportColumn(id = "cgst", displayName = "CGST", weight = 1.0f, type = ColumnType.CURRENCY),
            ReportColumn(id = "sgst", displayName = "SGST", weight = 1.0f, type = ColumnType.CURRENCY),
            ReportColumn(id = "igst", displayName = "IGST", weight = 1.0f, type = ColumnType.CURRENCY),
            ReportColumn(id = "amount", displayName = "Net Amount", weight = 1.3f, type = ColumnType.CURRENCY),
            ReportColumn(id = "status", displayName = "Status", weight = 1.0f, type = ColumnType.STATUS)
        ),
        filters = listOf(
            // PERIOD
            ReportFilterDescriptor(id = "date_from", label = "Date From", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, section = "PERIOD", weight = 0.5f, defaultValue = "2026-07-01"),
            ReportFilterDescriptor(id = "date_to", label = "Date To", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, section = "PERIOD", weight = 0.5f, defaultValue = "2026-07-31"),
            
            // PARTY
            ReportFilterDescriptor(id = "customer", label = "Customer", type = FilterType.DROPDOWN, icon = Icons.Default.Person, section = "PARTY", weight = 1f, defaultValue = "0"),
            
            // PRODUCT
            ReportFilterDescriptor(id = "product", label = "Product", type = FilterType.DROPDOWN, icon = Icons.Default.Inventory, section = "PRODUCT", defaultValue = "0"),
            
            // TRANSACTION
            ReportFilterDescriptor(
                id = "transaction_type",
                label = "Transaction Type",
                type = FilterType.DROPDOWN,
                icon = Icons.Default.ReceiptLong,
                section = "TRANSACTION",
                defaultValue = "Sales Invoice",
                options = listOf(
                    "Sales Invoice",
                    "Sales Challan",
                    "Pending Challan",
                    "Sales Credit Note",
                    "Debit Note",
                    "Sample Distribution",
                    "Proforma Invoice"
                )
            )
        ),
        summaries = listOf(
            SummaryCardConfig(id = "qty", label = "Total Qty", icon = Icons.Default.Inventory2, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48)),
            SummaryCardConfig(id = "amount", label = "Net Amt", icon = Icons.Default.AccountBalanceWallet, backgroundColor = Color(0xFFF9F5FF), valueColor = Color(0xFF6941C6), prefix = "₹ "),
            SummaryCardConfig(id = "profit", label = "Gross Profit", icon = Icons.Default.TrendingUp, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2), prefix = "₹ "),
            SummaryCardConfig(id = "margin", label = "Margin %", icon = Icons.Default.DonutLarge, backgroundColor = Color(0xFFF5FBFA), valueColor = Color(0xFF0E7090), suffix = " %")
        )
    )

    val ProductWiseSalesSchema = ReportSchema(
        id = "product_wise_sales",
        title = "Product Wise Sales Summary",
        subtitle = "Sales analysis categorized by products",
        icon = "▣",
        columns = listOf(
            ReportColumn(id = "product", displayName = "Product Name"),
            ReportColumn(id = "qty", displayName = "Total Qty", type = ColumnType.NUMBER),
            ReportColumn(id = "amount", displayName = "Total Amount", type = ColumnType.CURRENCY)
        ),
        filters = listOf(
            // Row 1
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-07-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2026-07-31"),
            
            // Row 2
            ReportFilterDescriptor(
                id = "transaction_type",
                label = "Transaction Type",
                type = FilterType.DROPDOWN,
                icon = Icons.Default.ReceiptLong,
                defaultValue = "All",
                weight = 0.5f,
                options = listOf(
                    "All",
                    "Sales Invoice",
                    "Sales Challan",
                    "Sales and Pending Challan Both",
                    "Credit Note",
                    "Debit Note",
                    "Sample Distribution",
                    "Proforma Invoice"
                )
            ),
            ReportFilterDescriptor(
                id = "product",
                label = "Product Type",
                type = FilterType.DROPDOWN,
                icon = Icons.Default.Inventory,
                defaultValue = "0",
                weight = 0.5f,
                enableSearch = true
            ),

            // Row 3
            ReportFilterDescriptor(
                id = "customer",
                label = "Customer",
                type = FilterType.AUTOCOMPLETE,
                icon = Icons.Default.Person,
                defaultValue = "0",
                weight = 1f
            )
        ),
        summaries = listOf(
            SummaryCardConfig(id = "qty", label = "Total Qty", icon = Icons.Default.Inventory2, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48)),
            SummaryCardConfig(id = "amount", label = "Total Amount", icon = Icons.Default.AccountBalanceWallet, backgroundColor = Color(0xFFF9F5FF), valueColor = Color(0xFF6941C6), prefix = "₹ ")
        )
    )

    val InventoryMovementSchema = ReportSchema(
        id = "inventory_movement",
        title = "Inventory Movement Register",
        subtitle = "Complete stock movement history",
        icon = "⇄",
        columns = listOf(
            ReportColumn(id = "date", displayName = "Date"),
            ReportColumn(id = "type", displayName = "Type"),
            ReportColumn(id = "product", displayName = "Product"),
            ReportColumn(id = "model", displayName = "Model"),
            ReportColumn(id = "power", displayName = "Power"),
            ReportColumn(id = "serial", displayName = "Serial No"),
            ReportColumn(id = "batch", displayName = "Batch No"),
            ReportColumn(id = "party", displayName = "Party"),
            ReportColumn(id = "refNo", displayName = "Ref No"),
            ReportColumn(id = "qtyIn", displayName = "Qty In", type = ColumnType.NUMBER),
            ReportColumn(id = "qtyOut", displayName = "Qty Out", type = ColumnType.NUMBER),
            ReportColumn(id = "balance", displayName = "Balance", type = ColumnType.NUMBER),
            ReportColumn(id = "user", displayName = "User"),
            ReportColumn(id = "status", displayName = "Status")
        ),
        filters = listOf(
            ReportFilterDescriptor(id = "date_from", label = "From Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-07-01"),
            ReportFilterDescriptor(id = "date_to", label = "To Date", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2026-07-31"),
            ReportFilterDescriptor(
                id = "movement_type",
                label = "Movement Type",
                type = FilterType.DROPDOWN,
                icon = Icons.Default.ReceiptLong,
                defaultValue = "All",
                options = listOf(
                    "All", "PURCHASE_RECEIVED", "SOLD", "SALES_RETURN", "CHALLAN_ISSUED", "SAMPLE_ISSUED", "DEMO_ISSUED", "SALE_EDIT_RETURN", "SALE_EDIT_SOLD"
                )
            ),
            ReportFilterDescriptor(id = "product", label = "Product", type = FilterType.DROPDOWN, icon = Icons.Default.Inventory, defaultValue = "0", enableSearch = true),
            ReportFilterDescriptor(id = "power", label = "Power", type = FilterType.DROPDOWN, icon = Icons.Default.Bolt, defaultValue = "All"),
            ReportFilterDescriptor(id = "customer", label = "Customer", type = FilterType.AUTOCOMPLETE, icon = Icons.Default.Person, defaultValue = "0"),
            ReportFilterDescriptor(id = "vendor", label = "Vendor", type = FilterType.AUTOCOMPLETE, icon = Icons.Default.Store, defaultValue = "0")
        ),
        summaries = listOf(
            SummaryCardConfig(id = "total_movements", label = "Total Movements", icon = Icons.Default.CompareArrows, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2)),
            SummaryCardConfig(id = "stock_in", label = "Stock In", icon = Icons.Default.AddCircle, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48)),
            SummaryCardConfig(id = "stock_out", label = "Stock Out", icon = Icons.Default.RemoveCircle, backgroundColor = Color(0xFFFFEEEE), valueColor = Color(0xFFB83A3A)),
            SummaryCardConfig(id = "current_balance", label = "Current Balance", icon = Icons.Default.Inventory2, backgroundColor = Color(0xFFF9F5FF), valueColor = Color(0xFF6941C6))
        )
    )

    val InventoryAgeingSchema = ReportSchema(
        id = "inventory_ageing",
        title = "Inventory Expiry Risk",
        subtitle = "Stock classification by expiry remaining days",
        icon = "⏳",
        columns = listOf(
            ReportColumn(id = "product", displayName = "Product", weight = 2.0f),
            ReportColumn(id = "model", displayName = "Model", weight = 1.2f),
            ReportColumn(id = "power", displayName = "Power", weight = 0.8f),
            ReportColumn(id = "batch", displayName = "Batch No", weight = 1.2f),
            ReportColumn(id = "serial", displayName = "Serial No", weight = 1.5f),
            ReportColumn(id = "expiryDate", displayName = "EXPIRY DATE", weight = 1.0f),
            ReportColumn(id = "daysLeft", displayName = "DAYS LEFT", weight = 0.8f, type = ColumnType.NUMBER),
            ReportColumn(id = "expiryStatus", displayName = "EXPIRY STATUS", weight = 1.2f, type = ColumnType.STATUS),
            ReportColumn(id = "status", displayName = "Status", weight = 1.0f, type = ColumnType.STATUS),
            ReportColumn(id = "location", displayName = "Location", weight = 1.0f)
        ),
        filters = listOf(
            ReportFilterDescriptor(id = "product", label = "Product", type = FilterType.DROPDOWN, icon = Icons.Default.Inventory, defaultValue = "0", enableSearch = true),
            ReportFilterDescriptor(id = "category", label = "Category", type = FilterType.DROPDOWN, icon = Icons.Default.Category, defaultValue = "All Categories"),
            ReportFilterDescriptor(id = "power", label = "Power", type = FilterType.DROPDOWN, icon = Icons.Default.Bolt, defaultValue = "All"),
            ReportFilterDescriptor(id = "batch", label = "Batch", type = FilterType.SEARCH_BAR, icon = Icons.Default.Numbers),
            ReportFilterDescriptor(id = "vendor", label = "Vendor", type = FilterType.AUTOCOMPLETE, icon = Icons.Default.Store, defaultValue = "0"),
            ReportFilterDescriptor(
                id = "expiry_risk", 
                label = "Expiry Risk", 
                type = FilterType.DROPDOWN, 
                icon = Icons.Default.Timer, 
                defaultValue = "All",
                options = listOf("All", "Fresh", "Sensitive", "On Risk", "High Risk", "Expired")
            )
        ),
        summaries = listOf(
            SummaryCardConfig(id = "total_stock", label = "Total Stock", icon = Icons.Default.Inventory2, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2)),
            SummaryCardConfig(id = "fresh_stock", label = "Fresh Stock (>365)", icon = Icons.Default.Eco, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48)),
            SummaryCardConfig(id = "high_risk", label = "High Risk (1-90)", icon = Icons.Default.TrendingDown, backgroundColor = Color(0xFFFFF5E9), valueColor = Color(0xFFC96A12)),
            SummaryCardConfig(id = "expired", label = "Expired (≤0)", icon = Icons.Default.Warning, backgroundColor = Color(0xFFFFEEEE), valueColor = Color(0xFFB83A3A))
        )
    )

    val LowStockAlertSchema = ReportSchema(
        id = "low_stock_alert",
        title = "Low Stock Alerts",
        subtitle = "Identify items below reorder thresholds",
        icon = "⚠",
        columns = listOf(
            ReportColumn(id = "product", displayName = "Product", weight = 2.0f),
            ReportColumn(id = "company", displayName = "Company", weight = 1.2f),
            ReportColumn(id = "model", displayName = "Model", weight = 1.0f),
            ReportColumn(id = "category", displayName = "Category", weight = 1.0f),
            ReportColumn(id = "power", displayName = "Power", weight = 0.8f),
            ReportColumn(id = "available", displayName = "In Stock", weight = 0.8f, type = ColumnType.NUMBER),
            ReportColumn(id = "minStock", displayName = "Min Stock", weight = 0.8f, type = ColumnType.NUMBER),
            ReportColumn(id = "reorderLevel", displayName = "Reorder Lvl", weight = 0.8f, type = ColumnType.NUMBER),
            ReportColumn(id = "status", displayName = "Alert Status", weight = 1.2f, type = ColumnType.STATUS)
        ),
        filters = listOf(
            ReportFilterDescriptor(id = "category", label = "Category", type = FilterType.DROPDOWN, icon = Icons.Default.Category, defaultValue = "All Categories"),
            ReportFilterDescriptor(id = "company", label = "Company", type = FilterType.DROPDOWN, icon = Icons.Default.Store, defaultValue = "All Companies"),
            ReportFilterDescriptor(
                id = "status", 
                label = "Alert Status", 
                type = FilterType.DROPDOWN, 
                icon = Icons.Default.NotificationImportant, 
                defaultValue = "All",
                options = listOf("All", "OUT_OF_STOCK", "LOW_STOCK", "REORDER", "HEALTHY")
            )
        ),
        summaries = listOf(
            SummaryCardConfig(id = "total", label = "Total Items", icon = Icons.Default.List, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2)),
            SummaryCardConfig(id = "outOfStock", label = "Out of Stock", icon = Icons.Default.Cancel, backgroundColor = Color(0xFFFFEEEE), valueColor = Color(0xFFB83A3A)),
            SummaryCardConfig(id = "lowStock", label = "Low Stock", icon = Icons.Default.Report, backgroundColor = Color(0xFFFFF5E9), valueColor = Color(0xFFC96A12)),
            SummaryCardConfig(id = "reorder", label = "Reorder Level", icon = Icons.Default.Warning, backgroundColor = Color(0xFFFEFBE8), valueColor = Color(0xFFB54708))
        )
    )

    val LensLibraryStatusSchema = ReportSchema(
        id = "lens_library_status",
        title = "Lens Library Status",
        subtitle = "Reconcile lenses issued to Surgeon Libraries",
        icon = "L",
        columns = listOf(
            ReportColumn(id = "product", displayName = "Product", weight = 2.5f),
            ReportColumn(id = "power", displayName = "Power", weight = 1.0f),
            ReportColumn(id = "issued", displayName = "Issued", weight = 0.8f, type = ColumnType.NUMBER),
            ReportColumn(id = "invoiced", displayName = "Invoiced", weight = 1.0f, type = ColumnType.NUMBER),
            ReportColumn(id = "balance", displayName = "Balance", weight = 1.0f, type = ColumnType.NUMBER),
            ReportColumn(id = "serials", displayName = "Serial Details", weight = 4.0f) // Drill-down details
        ),
        filters = listOf(
            // Row 1
            ReportFilterDescriptor(id = "date_from", label = "Challan Date From", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarToday, weight = 0.5f, defaultValue = "2026-04-01"),
            ReportFilterDescriptor(id = "date_to", label = "Challan Date To", type = FilterType.DATE_RANGE, icon = Icons.Default.CalendarMonth, weight = 0.5f, defaultValue = "2027-03-31"),
            
            // Row 2
            ReportFilterDescriptor(id = "customer", label = "Account / Surgeon", type = FilterType.AUTOCOMPLETE, icon = Icons.Default.Person, weight = 1f, defaultValue = "0"),
            
            // Row 3
            ReportFilterDescriptor(id = "product", label = "Product", type = FilterType.DROPDOWN, icon = Icons.Default.Inventory, weight = 0.5f, defaultValue = "0", enableSearch = true),
            ReportFilterDescriptor(id = "power", label = "Power", type = FilterType.SEARCH_BAR, icon = Icons.Default.Numbers, weight = 0.5f, defaultValue = "")
        ),
        summaries = listOf(
            SummaryCardConfig(id = "total_issued", label = "Total Issued", icon = Icons.Default.FileUpload, backgroundColor = Color(0xFFF0F9FF), valueColor = Color(0xFF026AA2)),
            SummaryCardConfig(id = "total_invoiced", label = "Total Invoiced", icon = Icons.Default.CheckCircle, backgroundColor = Color(0xFFECFDF3), valueColor = Color(0xFF027A48)),
            SummaryCardConfig(id = "library_balance", label = "Library Balance", icon = Icons.Default.AccountBalance, backgroundColor = Color(0xFFFFF5E9), valueColor = Color(0xFFC96A12))
        )
    )
}
