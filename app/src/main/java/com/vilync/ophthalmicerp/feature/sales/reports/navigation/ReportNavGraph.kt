package com.vilync.ophthalmicerp.feature.sales.reports.navigation

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchemas
import com.vilync.ophthalmicerp.feature.sales.reports.presentation.ReportsHomeScreen
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportScreen
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportViewModel
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.data.repository.InventoryStockRepository
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase
import com.vilync.ophthalmicerp.feature.inventory.ageing.InventoryAgeingUseCase
import com.vilync.ophthalmicerp.feature.inventory.alert.LowStockAlertUseCase
import com.vilync.ophthalmicerp.feature.inventory.movement.InventoryMovementUseCase
import com.vilync.ophthalmicerp.feature.inventory.movement.InventoryMovementViewModel
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.feature.sales.reports.data.SalesReportRepository
import com.vilync.ophthalmicerp.feature.sales.reports.domain.SalesReportUseCase
import com.vilync.ophthalmicerp.feature.payment.logic.FinancialReportingUseCase
import com.vilync.ophthalmicerp.feature.payment.presentation.FinancialReportSchemas
import com.vilync.ophthalmicerp.feature.payment.presentation.FinancialReportScreen
import com.vilync.ophthalmicerp.feature.payment.presentation.FinancialReportViewModel
import com.vilync.ophthalmicerp.feature.payment.presentation.FinancialReportViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vilync.ophthalmicerp.data.repository.AccountRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.sales.reports.data.LensLibraryReportProvider
import com.vilync.ophthalmicerp.feature.sales.invoicehub.presentation.InvoiceHubScreen
import com.vilync.ophthalmicerp.feature.sales.invoicehub.presentation.InvoiceHubViewModel
import com.vilync.ophthalmicerp.feature.sales.invoicehub.presentation.InvoiceHubViewModelFactory
import com.vilync.ophthalmicerp.feature.financialstatements.data.FinancialStatementRepository
import com.vilync.ophthalmicerp.feature.financialstatements.domain.FinancialStatementUseCase
import com.vilync.ophthalmicerp.feature.financialstatements.presentation.FinancialStatementsScreen
import com.vilync.ophthalmicerp.feature.financialstatements.presentation.FinancialStatementsViewModel
import com.vilync.ophthalmicerp.feature.financialstatements.presentation.FinancialStatementsViewModelFactory

fun NavGraphBuilder.salesReportsGraph(
    navController: NavController,
    database: AppDatabase,
    lowStockUseCase: LowStockAlertUseCase
) {
    composable(route = "reports_home") {
        ReportsHomeScreen(
            onBack = { navController.popBackStack() },
            onDashboard = { navController.navigate("dashboard") },
            onSalesTransactionReportClick = { navController.navigate("sales_report/sales_transactions") },
            onProductWiseSalesReportClick = { navController.navigate("sales_report/product_wise_sales") },
            onStockReportsClick = { /* TODO */ },
            onInventoryMovementReportClick = { navController.navigate("sales_report/inventory_movement") },
            onInventoryAgeingReportClick = { navController.navigate("sales_report/inventory_ageing") },
            onLowStockAlertClick = { navController.navigate("low_stock_status") },
            onGstReportsClick = { navController.navigate("gst_home") },
            onCustomerLedgerClick = { navController.navigate("financial_report/customer_ledger") },
            onSupplierLedgerClick = { navController.navigate("financial_report/supplier_ledger") },
            onCashBookClick = { navController.navigate("financial_report/cash_book") },
            onBankBookClick = { navController.navigate("financial_report/bank_book") },
            onFinancialStatementsClick = { navController.navigate("financial_statements") },
            onLensLibraryStatusReportClick = { navController.navigate("sales_report/lens_library_status") },
            onBulkInvoiceHubClick = { navController.navigate("invoice_hub") }
        )
    }

    composable(route = "financial_statements") {
        val repository = remember { FinancialStatementRepository(database) }
        val useCase = remember { FinancialStatementUseCase(repository) }
        val financialViewModel: FinancialStatementsViewModel = viewModel(
            factory = FinancialStatementsViewModelFactory(useCase)
        )
        
        FinancialStatementsScreen(
            viewModel = financialViewModel,
            onBack = { navController.popBackStack() },
            onDashboard = { navController.navigate("dashboard") },
            onDrillDown = { type, id ->
                navController.navigate("financial_report/$type?partyId=$id")
            }
        )
    }

    composable(route = "invoice_hub") {
        val auditTrailRepository = remember { com.vilync.ophthalmicerp.data.repository.AuditTrailRepository(database.auditTrailDao()) }
        val salesRepository = remember { SalesRepository(database.salesDao(), database, auditTrailRepository, null) }
        val productRepository = remember { ProductRepository(database.productDao()) }
        val partyRepository = remember { PartyRepository(database.partyDao()) }
        val companyProfileDao = remember { database.companyProfileDao() }
        
        val hubViewModel: InvoiceHubViewModel = viewModel(
            factory = InvoiceHubViewModelFactory(salesRepository, productRepository, partyRepository, companyProfileDao)
        )
        
        InvoiceHubScreen(
            viewModel = hubViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = "financial_report/{reportId}?partyId={partyId}",
        arguments = listOf(
            navArgument("reportId") { type = NavType.StringType },
            navArgument("partyId") { type = NavType.LongType; defaultValue = 0L }
        )
    ) { backStackEntry ->
        val reportId = backStackEntry.arguments?.getString("reportId")
        val partyId = backStackEntry.arguments?.getLong("partyId") ?: 0L
        val partyRepository = remember { PartyRepository(database.partyDao()) }
        val accountRepository = remember { AccountRepository(database.accountDao()) }

        val reportingUseCase = remember {
            FinancialReportingUseCase(
                salesDao = database.salesDao(),
                purchaseDao = database.purchaseDao(),
                salesCreditNoteDao = database.salesCreditNoteDao(),
                purchaseReturnDao = database.purchaseReturnDao(),
                financialTransactionDao = database.financialTransactionDao(),
                partyRepository = partyRepository
            )
        }
        
        val schema = when (reportId) {
            "customer_ledger" -> FinancialReportSchemas.CustomerLedgerSchema
            "supplier_ledger" -> FinancialReportSchemas.SupplierLedgerSchema
            "cash_book" -> FinancialReportSchemas.CashBookSchema
            "bank_book" -> FinancialReportSchemas.BankBookSchema
            else -> FinancialReportSchemas.CustomerLedgerSchema
        }
        
        val financialViewModel: FinancialReportViewModel = viewModel(
            key = "financial_report_${reportId}_$partyId",
            factory = FinancialReportViewModelFactory(
                schema = schema,
                reportingUseCase = reportingUseCase,
                partyRepository = partyRepository,
                accountRepository = accountRepository,
                initialPartyId = partyId
            )
        )
        
        FinancialReportScreen(
            viewModel = financialViewModel,
            onBack = { navController.popBackStack() },
            onDashboard = { navController.navigate("dashboard") }
        )
    }

    composable(
        route = "sales_report/{reportId}",
        arguments = listOf(navArgument("reportId") { type = NavType.StringType })
    ) { backStackEntry ->
        val reportId = backStackEntry.arguments?.getString("reportId")
        val reportsRepository = remember { SalesReportRepository(database) }
        val useCase = remember { SalesReportUseCase(reportsRepository) }

        val movementRepository = remember { StockMovementRepository(database.stockMovementDao()) }
        val movementUseCase = remember { InventoryMovementUseCase(movementRepository) }
        
        val inventoryRepository = remember { InventoryRepository(database.inventoryDao()) }
        val salesProductRepository = remember { ProductRepository(database.productDao()) }
        val partyRepository = remember { PartyRepository(database.partyDao()) }
        val ageingUseCase = remember { InventoryAgeingUseCase(inventoryRepository, salesProductRepository, partyRepository) }
        
        // Observe master data for dropdowns with stable flows to prevent stale lambda captures
        val productsState = remember(database) { 
            database.productDao().getAllActiveProducts() 
        }.collectAsState(initial = emptyList())
        
        val partiesState = remember(database) {
            database.partyDao().getAllActiveParties()
        }.collectAsState(initial = emptyList())

        val libraryPartiesState = remember(database) {
            database.challanDao().getPartiesWithLibraryChallans()
        }.collectAsState(initial = emptyList())

        val products = productsState.value
        val parties = partiesState.value
        val libraryParties = libraryPartiesState.value
        
        val schema = when (reportId) {
            "product_wise_sales" -> ReportSchemas.ProductWiseSalesSchema
            "inventory_movement" -> ReportSchemas.InventoryMovementSchema
            "inventory_ageing" -> ReportSchemas.InventoryAgeingSchema
            "low_stock_alert" -> ReportSchemas.LowStockAlertSchema
            "lens_library_status" -> ReportSchemas.LensLibraryStatusSchema
            else -> ReportSchemas.SalesTransactionSchema
        }

        val movementViewModel = remember(products, parties) {
            InventoryMovementViewModel(movementUseCase, products, parties)
        }

        val universalViewModel = remember(reportId) {
            UniversalReportViewModel(
                schema = schema,
                dataProvider = { filters ->
                    val start = filters["date_from"]?.toString()?.takeIf { it.isNotBlank() } ?: "2026-07-01"
                    val end = filters["date_to"]?.toString()?.takeIf { it.isNotBlank() } ?: "2026-07-31"
                    val searchQuery = filters["search"] as? String ?: ""
                    
                    if (schema.id == "lens_library_status") {
                        LensLibraryReportProvider.provideData(database, filters)
                    } else if (schema.id == "low_stock_alert") {
                        lowStockUseCase.getAlerts(filters)
                    } else if (schema.id == "inventory_ageing") {
                        ageingUseCase.getAgeingReport(filters)
                    } else if (schema.id == "inventory_movement") {
                        movementViewModel.provideReportData(filters)
                    } else if (schema.id == "product_wise_sales") {
                        val custIdStr = filters["customer"]?.toString() ?: "0"
                        val custId = custIdStr.toLongOrNull()?.let { if (it == 0L) null else it }
                        
                        val productFilterId = filters["product"]?.toString() ?: "0"
                        val prodId = if (productFilterId == "0") null else productFilterId.toLongOrNull()

                        val txType = filters["transaction_type"]?.toString() ?: "All"

                        // SQL-level aggregation and filtering
                        val summaries = useCase.getProductSummary(
                            startDate = start,
                            endDate = end,
                            customerId = custId,
                            productId = prodId,
                            transactionType = txType
                        )
                        
                        summaries.map { summary ->
                            ReportRowData(
                                id = 0,
                                values = mapOf(
                                    "product" to summary.productName,
                                    "qty" to summary.qty,
                                    "amount" to summary.amount
                                )
                            )
                        }
                    } else {
                        // Sales Transaction logic
                        val txType = filters["transaction_type"] as? String ?: "Sales Invoice"
                        
                        val productFilterId = filters["product"]?.toString() ?: "0"
                        val customerFilterId = filters["customer"]?.toString() ?: "0"
                        
                        val details = useCase.getSalesDetails(start, end)

                        val filtered = details.filter { detail ->
                            val matchesSearch = searchQuery.isBlank() ||
                                    detail.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                                    detail.customer.contains(searchQuery, ignoreCase = true) ||
                                    detail.customerGstin.contains(searchQuery, ignoreCase = true)

                            val matchesTxType = when (txType) {
                                "Sales Invoice" -> true
                                "Sales and Pending Sales Challan both" -> true
                                else -> false
                            }

                            val matchesProduct = productFilterId == "0" || detail.productId.toString() == productFilterId
                            val matchesCustomer = customerFilterId == "0" || detail.customerId.toString() == customerFilterId

                            matchesSearch && matchesTxType && matchesProduct && matchesCustomer
                        }
                        
                        filtered.groupBy { 
                            // Grouping by Invoice Number, Product, and Power
                            Triple(it.invoiceNo, it.product, it.power) 
                        }.map { (_, group) ->
                            val first = group.first()
                            ReportRowData(
                                id = 0,
                                values = mapOf(
                                    "date" to first.date,
                                    "invoiceNo" to first.invoiceNo,
                                    "customer" to first.customer,
                                    "customerGstin" to first.customerGstin,
                                    "product" to first.product,
                                    "power" to first.power,
                                    "serial" to group.joinToString("\n") { it.serial },
                                    "qty" to group.size,
                                    "gst_percent" to first.gst_percent,
                                    "cgst" to group.sumOf { it.cgst },
                                    "sgst" to group.sumOf { it.sgst },
                                    "igst" to group.sumOf { it.igst },
                                    "amount" to group.sumOf { it.amount },
                                    "status" to first.status,
                                    "purchasePrice" to group.sumOf { it.purchasePrice },
                                    "costResolutionSource" to first.costResolutionSource
                                )
                            )
                        }
                    }
                },
                summaryCalculator = { rows ->
                    if (schema.id == "lens_library_status") {
                        LensLibraryReportProvider.calculateSummary(rows)
                    } else if (schema.id == "low_stock_alert") {
                        lowStockUseCase.calculateSummary(rows)
                    } else if (schema.id == "inventory_ageing") {
                        val fresh = rows.count { it.values["expiryStatus"] == InventoryAgeingUseCase.RISK_FRESH }
                        val highRisk = rows.count { it.values["expiryStatus"] == InventoryAgeingUseCase.RISK_HIGH_RISK }
                        val expired = rows.count { it.values["expiryStatus"] == InventoryAgeingUseCase.RISK_EXPIRED }
                        
                        mapOf(
                            "total_stock" to rows.size.toString(),
                            "fresh_stock" to fresh.toString(),
                            "high_risk" to highRisk.toString(),
                            "expired" to expired.toString()
                        )
                    } else if (schema.id == "inventory_movement") {
                        val totalIn = rows.sumOf { it.values["qtyIn"]?.toString()?.toIntOrNull() ?: 0 }
                        val totalOut = rows.sumOf { it.values["qtyOut"]?.toString()?.toIntOrNull() ?: 0 }
                        val currentBalance = rows.firstOrNull()?.values["balance"]?.toString() ?: "0"
                        mapOf(
                            "total_movements" to rows.size.toString(),
                            "stock_in" to totalIn.toString(),
                            "stock_out" to totalOut.toString(),
                            "current_balance" to currentBalance
                        )
                    } else if (schema.id == "product_wise_sales") {
                        val totalQty = rows.sumOf { it.values["qty"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                        val totalAmt = rows.sumOf { it.values["amount"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                        mapOf(
                            "qty" to "%.0f".format(totalQty),
                            "amount" to "%.2f".format(totalAmt)
                        )
                    } else {
                        val totalQty = rows.sumOf { it.values["qty"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                        val totalAmt = rows.sumOf { it.values["amount"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                        val totalCost = rows.sumOf { it.values["purchasePrice"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                        
                        val totalProfit = totalAmt - totalCost
                        val margin = if (totalAmt > 0) (totalProfit / totalAmt) * 100.0 else 0.0
                        
                        mapOf(
                            "qty" to "%.0f".format(totalQty),
                            "amount" to "%.2f".format(totalAmt),
                            "profit" to "%.2f".format(totalProfit),
                            "margin" to "%.2f".format(margin)
                        )
                    }
                }
            )
        }

        LaunchedEffect(reportId, products, parties, libraryParties) {
            // Options format: "ID|Name|Subtext"
            
            val productOptions = products.map { 
                val cleanName = it.productName.replace(Regex("^\\d+\\s+"), "").trim()
                "${it.id}|$cleanName|${it.brandName}" 
            }
            
            val categories = products.map { it.category }.distinct().sorted()
            val manufacturers = products.map { it.brandName }.filter { it.isNotBlank() }.distinct().sorted()
            
            val effectiveParties = if (reportId == "lens_library_status") libraryParties else parties

            val customerOptions = effectiveParties
                .filter { it.partyType == "CUSTOMER" || it.partyType == "HOSPITAL" }
                .map { "${it.id}|${it.partyName}|${it.gstin}" }
            
            val vendorOptions = effectiveParties
                .filter { it.partyType == "SUPPLIER" || it.partyType == "MANUFACTURER" }
                .map { "${it.id}|${it.partyName}|${it.gstin}" }
            
            val distinctPowers = inventoryRepository.getDistinctPowers()

            universalViewModel.setDynamicOptions("product", listOf("0|All Products") + productOptions)
            universalViewModel.setDynamicOptions("category", listOf("All Categories") + categories)
            universalViewModel.setDynamicOptions("company", listOf("All Companies") + manufacturers)
            universalViewModel.setDynamicOptions("customer", listOf("0|All Customers") + customerOptions)
            universalViewModel.setDynamicOptions("vendor", listOf("0|All Vendors") + vendorOptions)
            universalViewModel.setDynamicOptions("power", listOf("All") + distinctPowers)
        }

        UniversalReportScreen(
            viewModel = universalViewModel,
            onBack = { navController.popBackStack() },
            onDashboard = { navController.navigate("dashboard") }
        )
    }
}
