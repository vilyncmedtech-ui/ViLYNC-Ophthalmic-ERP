package com.vilync.ophthalmicerp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

import com.vilync.ophthalmicerp.BuildConfig
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearViewModel
import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.core.security.PersistentSessionStore
import com.vilync.ophthalmicerp.core.security.SessionGuard
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.data.repository.*
import com.vilync.ophthalmicerp.feature.inventory.serialstock.*
import com.vilync.ophthalmicerp.feature.dashboard.*
import com.vilync.ophthalmicerp.feature.inventory.logic.GetAvailableStockUseCase
import com.vilync.ophthalmicerp.feature.inventory.alert.LowStockAlertUseCase
import com.vilync.ophthalmicerp.feature.inventory.opening.domain.OpeningStockUseCase
import com.vilync.ophthalmicerp.feature.inventory.opening.presentation.*
import com.vilync.ophthalmicerp.feature.inventory.adjustment.domain.StockAdjustmentUseCase
import com.vilync.ophthalmicerp.feature.inventory.adjustment.presentation.*
import com.vilync.ophthalmicerp.feature.inventory.reconciliation.domain.StockReconciliationUseCase
import com.vilync.ophthalmicerp.feature.inventory.reconciliation.presentation.*
import com.vilync.ophthalmicerp.feature.payment.logic.FinancialTransactionUseCase
import com.vilync.ophthalmicerp.feature.payment.logic.FinancialReportingUseCase
import com.vilync.ophthalmicerp.feature.payment.logic.OutstandingCalculationUseCase
import com.vilync.ophthalmicerp.feature.payment.logic.PaymentUseCase
import com.vilync.ophthalmicerp.feature.payment.presentation.*
import com.vilync.ophthalmicerp.feature.gst.presentation.*
import com.vilync.ophthalmicerp.feature.gst.data.*
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import com.vilync.ophthalmicerp.feature.sales.presentation.*
import com.vilync.ophthalmicerp.feature.sales.register.*
import com.vilync.ophthalmicerp.feature.sales.reports.navigation.salesReportsGraph
import com.vilync.ophthalmicerp.feature.sales.detail.*
import com.vilync.ophthalmicerp.feature.sales.challan.presentation.*
import com.vilync.ophthalmicerp.feature.sales.creditnote.presentation.*
import com.vilync.ophthalmicerp.feature.sales.proforma.presentation.*
import com.vilync.ophthalmicerp.feature.sales.sample.presentation.*
import androidx.compose.ui.graphics.Color
import com.vilync.ophthalmicerp.feature.inventory.alert.presentation.LowStockStatusScreen
import com.vilync.ophthalmicerp.feature.inventory.alert.presentation.LowStockStatusViewModel
import com.vilync.ophthalmicerp.feature.inventory.alert.presentation.LowStockStatusViewModelFactory
import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.core.document.engine.binding.*
import com.vilync.ophthalmicerp.core.document.template.*
import com.vilync.ophthalmicerp.feature.designer.domain.binding.*
import com.vilync.ophthalmicerp.feature.designer.data.repository.DocumentTemplateRepositoryImpl
import com.vilync.ophthalmicerp.feature.designer.domain.PurchaseOrderTemplateBootstrap
import com.vilync.ophthalmicerp.feature.inventory.threshold.data.InventoryThresholdRepository
import com.vilync.ophthalmicerp.feature.inventory.threshold.presentation.InventoryThresholdScreen
import com.vilync.ophthalmicerp.feature.inventory.threshold.presentation.InventoryThresholdViewModel
import com.vilync.ophthalmicerp.feature.inventory.threshold.presentation.InventoryThresholdViewModelFactory
import com.vilync.ophthalmicerp.feature.inventory.InventoryHomeScreen
import com.vilync.ophthalmicerp.feature.inventory.StockRegisterScreen
import com.vilync.ophthalmicerp.feature.inventory.StockRegisterViewModel
import com.vilync.ophthalmicerp.feature.login.*
import com.vilync.ophthalmicerp.feature.master.presentation.MasterScreen
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.presentation.*
import com.vilync.ophthalmicerp.feature.purchase.PurchaseHomeScreen
import com.vilync.ophthalmicerp.feature.purchase.presentation.*
import com.vilync.ophthalmicerp.feature.purchase.detail.*
import com.vilync.ophthalmicerp.feature.purchase.register.*
import com.vilync.ophthalmicerp.feature.purchase.returnentry.*
import com.vilync.ophthalmicerp.feature.settings.*
import com.vilync.ophthalmicerp.feature.settings.backup.BackupRestoreScreen
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import com.vilync.ophthalmicerp.feature.companyprofile.presentation.CompanyProfileScreen
import com.vilync.ophthalmicerp.feature.settings.audit.*
import com.vilync.ophthalmicerp.feature.splash.SplashScreen
import com.vilync.ophthalmicerp.feature.backup.logic.*
import com.vilync.ophthalmicerp.feature.backup.data.BackupRepository
import com.vilync.ophthalmicerp.feature.backup.domain.BackupUseCase
import com.vilync.ophthalmicerp.feature.backup.presentation.GoogleBackupViewModel
import com.vilync.ophthalmicerp.feature.backup.presentation.GoogleBackupViewModelFactory
import com.vilync.ophthalmicerp.feature.backup.model.GoogleAuthState
import com.vilync.ophthalmicerp.feature.backup.background.BackupScheduler
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListScreen
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModel
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModelFactory
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterScreen
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterViewModel
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterViewModelFactory
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupRepository
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupService

private data class SalesCategoryConfig(
    val title: String,
    val newTitle: String,
    val newSubtitle: String,
    val newSymbol: String,
    val newRoute: String,
    val backgroundColor: Color
)

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current
    val persistentSessionStore = remember(context) { PersistentSessionStore(context) }
    val coroutineScope = rememberCoroutineScope()

    val financialYearViewModel: FinancialYearViewModel = viewModel()
    val purchaseDatabase = DatabaseProvider.getDatabase(context = context)

    val userRepository = UserRepository(userDao = purchaseDatabase.userDao())
    val authRepository = AuthRepository(userRepository = userRepository)
    val auditTrailRepository = AuditTrailRepository(auditTrailDao = purchaseDatabase.auditTrailDao())
    val companyProfileRepository = CompanyProfileRepository(companyProfileDao = purchaseDatabase.companyProfileDao())
    val numberingRepository = remember { DocumentNumberingRepository(purchaseDatabase) }

    // GOOGLE BACKUP MODULE
    val googleAuthManager = remember { GoogleAuthManager(context) }
    val backupSettingsManager = remember { BackupSettingsManager(context) }
    val backupRepository = remember { BackupRepository(purchaseDatabase) }
    val thresholdRepository = remember { InventoryThresholdRepository(purchaseDatabase.inventoryThresholdDao()) }
    val integrityVerifier = remember { BackupIntegrityVerifier(context) }
    val backupService = remember { GoogleDriveBackupService(context, backupSettingsManager) }
    val backupUseCase = remember {
        BackupUseCase(
            context = context,
            backupRepository = backupRepository,
            integrityVerifier = integrityVerifier,
            backupService = backupService,
            companyProfileRepository = companyProfileRepository,
            auditTrailRepository = auditTrailRepository,
            settingsManager = backupSettingsManager
        )
    }

    // BACKGROUND BACKUP SCHEDULER
    val authState by googleAuthManager.authState.collectAsState()
    LaunchedEffect(Unit) {
        if (backupSettingsManager.getGoogleAccountEmail() != null) {
            BackupScheduler.scheduleNightlyBackup(context)
        }
    }
    LaunchedEffect(authState) {
        if (authState is GoogleAuthState.Authenticated) {
            val email = (authState as GoogleAuthState.Authenticated).email
            backupSettingsManager.saveGoogleAccountEmail(email)
            BackupScheduler.scheduleNightlyBackup(context)
        }
    }

    val appStartupViewModel: AppStartupViewModel = viewModel(
        factory = AppStartupViewModelFactory(
            userRepository = userRepository,
            startupRepository = StartupRepository(context),
            persistentSessionStore = persistentSessionStore
        )
    )

    val purchasePartyRepository = PartyRepository(partyDao = purchaseDatabase.partyDao())
    val purchaseRepository = PurchaseRepository(purchaseDao = purchaseDatabase.purchaseDao(), database = purchaseDatabase)
    val purchaseReturnRepository = PurchaseReturnRepository(
        purchaseReturnDao = purchaseDatabase.purchaseReturnDao(),
        database = purchaseDatabase,
        numberingRepository = numberingRepository
    )
    val purchaseProductRepository = ProductMasterRepository(productDao = purchaseDatabase.productDao())

    val salesRepository = SalesRepository(
        salesDao = purchaseDatabase.salesDao(), 
        database = purchaseDatabase, 
        auditTrailRepository = auditTrailRepository,
        numberingRepository = numberingRepository
    )
    val salesProductRepository = ProductRepository(productDao = purchaseDatabase.productDao())
    val salesInventoryRepository = InventoryRepository(inventoryDao = purchaseDatabase.inventoryDao())
    val inventoryStockRepository = remember { InventoryStockRepository(purchaseDatabase.inventoryStockDao(), purchaseDatabase.productDao(), purchaseDatabase.inventoryThresholdDao()) }
    val stockEngine = remember { GetAvailableStockUseCase(inventoryStockRepository) }
    val challanRepository = ChallanRepository(
        challanDao = purchaseDatabase.challanDao(), 
        database = purchaseDatabase,
        numberingRepository = numberingRepository
    )
    val salesCreditNoteRepository = SalesCreditNoteRepository(
        creditNoteDao = purchaseDatabase.salesCreditNoteDao(), 
        database = purchaseDatabase,
        numberingRepository = numberingRepository
    )
    val proformaRepository = ProformaInvoiceRepository(
        proformaDao = purchaseDatabase.proformaInvoiceDao(),
        database = purchaseDatabase,
        numberingRepository = numberingRepository
    )
    val sampleIssueRepository = SampleIssueRepository(
        sampleIssueDao = purchaseDatabase.sampleIssueDao(),
        database = purchaseDatabase,
        numberingRepository = numberingRepository
    )

    val accountRepository = remember { AccountRepository(accountDao = purchaseDatabase.accountDao()) }
    val financialTransactionRepository = remember { FinancialTransactionRepository(financialTransactionDao = purchaseDatabase.financialTransactionDao()) }
    val financialTransactionUseCase = remember { 
        FinancialTransactionUseCase(
            transactionRepository = financialTransactionRepository, 
            accountRepository = accountRepository, 
            auditTrailRepository = auditTrailRepository,
            numberingRepository = numberingRepository,
            database = purchaseDatabase
        ) 
    }
    
    val reportingUseCase = remember {
        FinancialReportingUseCase(
            salesDao = purchaseDatabase.salesDao(),
            purchaseDao = purchaseDatabase.purchaseDao(),
            salesCreditNoteDao = purchaseDatabase.salesCreditNoteDao(),
            purchaseReturnDao = purchaseDatabase.purchaseReturnDao(),
            financialTransactionDao = purchaseDatabase.financialTransactionDao(),
            partyRepository = purchasePartyRepository
        )
    }

    val outstandingCalculationUseCase = remember { 
        OutstandingCalculationUseCase(
            salesRepository = salesRepository, 
            purchaseRepository = purchaseRepository, 
            salesCreditNoteRepository = salesCreditNoteRepository, 
            financialTransactionRepository = financialTransactionRepository,
            partyRepository = purchasePartyRepository
        ) 
    }
    val paymentUseCase = remember { PaymentUseCase(financialTransactionUseCase = financialTransactionUseCase, outstandingCalculationUseCase = outstandingCalculationUseCase, financialTransactionRepository = financialTransactionRepository) }
    val openingStockRepository = remember { OpeningStockRepository(openingStockDao = purchaseDatabase.openingStockDao(), database = purchaseDatabase) }
    val openingStockUseCase = remember { 
        OpeningStockUseCase(
            openingStockRepository = openingStockRepository, 
            inventoryRepository = salesInventoryRepository, 
            stockMovementRepository = StockMovementRepository(purchaseDatabase.stockMovementDao()), 
            productMasterRepository = purchaseProductRepository, 
            auditTrailRepository = auditTrailRepository,
            numberingRepository = numberingRepository
        ) 
    }

    val stockAdjustmentUseCase = remember {
        StockAdjustmentUseCase(
            inventoryRepository = salesInventoryRepository,
            stockMovementRepository = StockMovementRepository(purchaseDatabase.stockMovementDao()),
            auditTrailRepository = auditTrailRepository
        )
    }

    val stockReconciliationUseCase = remember {
        StockReconciliationUseCase(
            inventoryRepository = salesInventoryRepository,
            stockMovementRepository = StockMovementRepository(purchaseDatabase.stockMovementDao()),
            auditTrailRepository = auditTrailRepository
        )
    }

    val lowStockUseCase = remember { LowStockAlertUseCase(stockEngine) }

    val globalSearchUseCase = remember { GlobalSearchUseCase(database = purchaseDatabase) }

    // UNIVERSAL DOCUMENT ENGINE
    val templateRepository = remember { 
        DocumentTemplateRepositoryImpl(
            dao = purchaseDatabase.documentDesignerDao(),
            serializer = GsonTemplateSerializer()
        )
    }
    val placeholderResolver = remember {
        PlaceholderResolver(
            listOf(
                CompanyDynamicFieldProvider(companyProfileRepository),
                PurchaseDynamicFieldProvider(purchaseRepository, purchasePartyRepository, salesProductRepository),
                SalesDynamicFieldProvider(salesRepository),
                SampleDynamicFieldProvider()
            )
        )
    }
    val documentRuntime = remember {
        DocumentRuntime(
            repository = templateRepository,
            resolver = placeholderResolver,
            bindingEngine = BindingEngine(),
            layoutFactory = DefaultLayoutObjectFactory(),
            renderingEngine = DefaultRenderingEngine(),
            pdfRenderer = DefaultPdfRenderer(context),
            printRenderer = DefaultPrintRenderer(context)
        )
    }

    LaunchedEffect(Unit) {
        PurchaseOrderTemplateBootstrap.bootstrap(templateRepository)
    }

    val activeFinancialYear by financialYearViewModel.activeFinancialYear.collectAsState()
    val currentFinancialYear = activeFinancialYear.startYear

    NavHost(navController = navController, startDestination = "splash") {

        composable(route = "splash") {
            val startupDestination by appStartupViewModel.destination.collectAsState()
            SplashScreen(onSplashFinished = {
                val destination = startupDestination
                when (destination) {
                    StartupDestination.FirstAdminSetup -> { navController.navigate("first_admin_setup") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Dashboard -> { navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Login -> { navController.navigate("login") { popUpTo("splash") { inclusive = true } } }
                    is StartupDestination.RuntimeError -> { navController.navigate("runtime_error") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Loading -> { }
                }
            })
            LaunchedEffect(startupDestination) {
                val destination = startupDestination
                when (destination) {
                    StartupDestination.FirstAdminSetup -> { navController.navigate("first_admin_setup") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Dashboard -> { navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Login -> { navController.navigate("login") { popUpTo("splash") { inclusive = true } } }
                    is StartupDestination.RuntimeError -> { navController.navigate("runtime_error") { popUpTo("splash") { inclusive = true } } }
                    StartupDestination.Loading -> Unit
                }
            }
        }

        composable(route = "runtime_error") {
            val state by appStartupViewModel.destination.collectAsState()
            if (state is StartupDestination.RuntimeError) {
                val errorState = state as StartupDestination.RuntimeError
                RuntimeErrorScreen(message = errorState.message, diagnostics = errorState.diagnostics, report = errorState.report, onRetry = { navController.navigate("splash") { popUpTo(0) { inclusive = true } } })
            }
        }

        composable(route = "first_admin_setup") {
            val firstAdminSetupViewModel: FirstAdminSetupViewModel = viewModel(factory = FirstAdminSetupViewModelFactory(userRepository = userRepository))
            FirstAdminSetupScreen(viewModel = firstAdminSetupViewModel, onAdminCreated = { appStartupViewModel.onFirstAdminCreated(); navController.navigate("login") { popUpTo("first_admin_setup") { inclusive = true } } })
        }

        composable(route = "login") {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(
                    authRepository = authRepository,
                    auditTrailRepository = auditTrailRepository,
                    persistentSessionStore = persistentSessionStore
                )
            )
            LoginScreen(viewModel = loginViewModel, onLoginSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } })
        }

        composable(route = "dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(
                        database = purchaseDatabase,
                        lowStockUseCase = lowStockUseCase,
                        outstandingCalculationUseCase = OutstandingCalculationUseCase(
                            salesRepository = salesRepository,
                            purchaseRepository = purchaseRepository,
                            salesCreditNoteRepository = salesCreditNoteRepository,
                            financialTransactionRepository = financialTransactionRepository,
                            partyRepository = purchasePartyRepository
                        ),
                        partyRepository = purchasePartyRepository,
                        searchUseCase = globalSearchUseCase
                    ) as T
                }
            })
            val uiState by dashboardViewModel.uiState.collectAsState()
            DashboardScreen(
                uiState = uiState,
                activeFinancialYear = "${currentFinancialYear}-${currentFinancialYear + 1}",
                onSearchQueryChange = dashboardViewModel::updateSearchQuery,
                onSearchResultClick = { route -> navController.navigate(route) },
                onRefresh = dashboardViewModel::refresh,
                onCustomerOverdueClick = { navController.navigate("financial_report/customer_ledger") },
                onSupplierOverdueClick = { navController.navigate("financial_report/supplier_ledger") },
                onExpiryAlertClick = { navController.navigate("sales_report/inventory_ageing") },
                onLowStockClick = { navController.navigate("low_stock_status") },
                onPendingSamplesClick = { navController.navigate("sales_category/${SalesRegisterType.SAMPLE_ISSUE.name}") },
                onPendingChallansClick = { navController.navigate("sales_category/${SalesRegisterType.CHALLAN.name}") },
                onMasterClick = { navController.navigate("master_home") },
                onSalesClick = { navController.navigate("sales_home") },
                onNewSaleClick = { navController.navigate("sales/invoice/new") },
                onNewPurchaseClick = { navController.navigate("purchase_entry") },
                onNewChallanClick = { navController.navigate("sales/challan/new") },
                onReceivePaymentClick = { navController.navigate("payment_entry/RECEIPT") },
                onPurchaseClick = { navController.navigate("purchase_home") },
                onInventoryClick = { navController.navigate("inventory_home") },
                onPaymentsClick = { navController.navigate("payment_home") },
                onGstClick = { navController.navigate("gst_home") },
                onReportsClick = { navController.navigate("reports_home") },
                onSettingsClick = { navController.navigate("settings_home") },
                onLogoutClick = { coroutineScope.launch { SessionManager.clearSession(); persistentSessionStore.clear(); navController.navigate("login") { popUpTo("dashboard") { inclusive = true } } } }
            )
        }

        composable(route = "master_home") {
            MasterScreen(
                onProductMasterClick = { navController.navigate("product_master_list") },
                onPartyMasterClick = { navController.navigate("party_master_list") }
            )
        }

        composable(route = "party_master_list") {
            val partyListViewModel: PartyListViewModel = viewModel(
                factory = PartyListViewModelFactory(repository = purchasePartyRepository)
            )
            PartyListScreen(
                viewModel = partyListViewModel,
                onAddParty = { navController.navigate("party_master/0") },
                onEditParty = { partyId -> navController.navigate("party_master/$partyId") }
            )
        }

        composable(
            route = "party_master/{partyId}",
            arguments = listOf(navArgument("partyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getLong("partyId") ?: 0L
            val partyMasterViewModel: PartyMasterViewModel = viewModel(
                factory = PartyMasterViewModelFactory(
                    repository = purchasePartyRepository,
                    gstinLookupRepository = GstinLookupRepository(GstinLookupService(apiKey = BuildConfig.GST_API_KEY))
                )
            )
            LaunchedEffect(partyId) {
                if (partyId > 0L) partyMasterViewModel.loadParty(partyId)
                else partyMasterViewModel.startNewParty()
            }
            PartyMasterScreen(
                viewModel = partyMasterViewModel,
                onPartySaved = { navController.popBackStack() },
                onPartyDeleted = { navController.popBackStack() }
            )
        }

        composable(route = "product_master_list") {
            val productListViewModel: ProductListViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductListViewModel(application = context.applicationContext as android.app.Application) as T
                }
            })
            ProductListScreen(viewModel = productListViewModel, onAddProductClick = { navController.navigate("product_master/0") }, onEditProductClick = { productId -> navController.navigate("product_master/$productId") })
        }

        composable(route = "product_master/{productId}", arguments = listOf(navArgument("productId") { type = NavType.LongType })) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            val productMasterViewModel: ProductMasterViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductMasterViewModel(repository = ProductMasterRepository(productDao = purchaseDatabase.productDao()), partyRepository = purchasePartyRepository) as T
                }
            })
            LaunchedEffect(productId) {
                if (productId > 0L) productMasterViewModel.loadProduct(productId) else productMasterViewModel.startNewProduct()
            }
            ProductMasterScreen(viewModel = productMasterViewModel, onProductSaved = { navController.popBackStack() }, onProductDeleted = { navController.popBackStack() })
        }

        composable(route = "sales_home") {
            SalesHomeScreen(
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onInvoice = { navController.navigate("sales_category/${SalesRegisterType.INVOICE.name}") },
                onChallan = { navController.navigate("sales_category/${SalesRegisterType.CHALLAN.name}") },
                onCreditNote = { navController.navigate("sales_category/${SalesRegisterType.CREDIT_NOTE.name}") },
                onProformaInvoice = { navController.navigate("sales_category/${SalesRegisterType.PROFORMA.name}") },
                onSampleIssue = { navController.navigate("sales_category/${SalesRegisterType.SAMPLE_ISSUE.name}") }
            )
        }

        composable(
            route = "sales_category/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("type") ?: SalesRegisterType.INVOICE.name
            val registerType = SalesRegisterType.valueOf(typeString)
            
            val config = when (registerType) {
                SalesRegisterType.INVOICE -> SalesCategoryConfig("Sales Invoice", "New Invoice", "Issue tax invoice", "₹", "sales/invoice/new", Color(0xFFDDF6E8))
                SalesRegisterType.CHALLAN -> SalesCategoryConfig("Delivery Challan", "New Challan", "Issue delivery challan", "▤", "sales/challan/new", Color(0xFFE8F5E9))
                SalesRegisterType.CREDIT_NOTE -> SalesCategoryConfig("Credit Note", "New Credit Note", "Issue sales return", "↩", "sales/creditnote/new", Color(0xFFFFF3E0))
                SalesRegisterType.PROFORMA -> SalesCategoryConfig("Proforma Invoice", "New Proforma", "Issue proforma invoice", "▦", "sales/proforma/new", Color(0xFFF3E5F5))
                SalesRegisterType.SAMPLE_ISSUE -> SalesCategoryConfig("Sample Issue", "New Sample", "Issue product sample", "S", "sales/sample/new", Color(0xFFFFF9C4))
            }

            SalesDocumentCategoryScreen(
                title = config.title,
                subtitle = "Select action for ${registerType.displayName}",
                newTitle = config.newTitle,
                newSubtitle = config.newSubtitle,
                registerTitle = "Register",
                registerSubtitle = "View all posted documents",
                newSymbol = config.newSymbol,
                newBackground = config.backgroundColor,
                registerBackground = Color(0xFFEFF8FF),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { navController.navigate(config.newRoute) },
                onRegister = { navController.navigate("sales_register/${registerType.name}") }
            )
        }

        composable(
            route = "sales_register/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("type") ?: SalesRegisterType.INVOICE.name
            val registerType = SalesRegisterType.valueOf(typeString)
            val salesRegisterViewModel: SalesRegisterViewModel = viewModel(
                factory = SalesRegisterViewModelFactory(
                    repository = SalesRegisterRepository(
                        database = purchaseDatabase,
                        salesRepository = salesRepository
                    ),
                    registerType = registerType,
                    currentFinancialYear = currentFinancialYear
                )
            )
            SalesRegisterScreen(
                viewModel = salesRegisterViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = {
                    navController.navigate(registerType.newDocumentRoute)
                },
                onViewDetail = { id ->
                    navController.navigate(registerType.getDetailRoute(id))
                },
                onEditInvoice = { id ->
                    navController.navigate(registerType.getEditRoute(id))
                }
            )
        }

        composable(
            route = "sales/invoice/{saleId}",
            arguments = listOf(navArgument("saleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: 0L
            val detailViewModel: SalesInvoiceDetailViewModel = viewModel(factory = SalesInvoiceDetailViewModelFactory(saleId = saleId, repository = salesRepository, inventoryRepository = salesInventoryRepository, productRepository = salesProductRepository, companyProfileDao = purchaseDatabase.companyProfileDao()))
            SalesInvoiceDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("sales/invoice/edit/$id") })
        }

        composable(
            route = "sales/challan/{challanId}",
            arguments = listOf(navArgument("challanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val challanId = backStackEntry.arguments?.getLong("challanId") ?: 0L
            val detailViewModel: ChallanDetailViewModel = viewModel(
                factory = ChallanDetailViewModelFactory(
                    challanId = challanId, 
                    repository = challanRepository, 
                    productRepository = salesProductRepository, 
                    partyRepository = purchasePartyRepository,
                    companyProfileDao = purchaseDatabase.companyProfileDao()
                )
            )
            ChallanDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("sales/challan/edit/$id") })
        }

        composable(
            route = "sales/creditnote/{cnId}",
            arguments = listOf(navArgument("cnId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cnId = backStackEntry.arguments?.getLong("cnId") ?: 0L
            val detailViewModel: CreditNoteDetailViewModel = viewModel(factory = CreditNoteDetailViewModelFactory(creditNoteId = cnId, repository = salesCreditNoteRepository, productRepository = salesProductRepository, companyProfileDao = purchaseDatabase.companyProfileDao()))
            CreditNoteDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("sales/creditnote/edit/$id") })
        }

        composable(
            route = "sales/proforma/{proformaId}",
            arguments = listOf(navArgument("proformaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val proformaId = backStackEntry.arguments?.getLong("proformaId") ?: 0L
            val detailViewModel: ProformaDetailViewModel = viewModel(factory = ProformaDetailViewModelFactory(proformaId = proformaId, repository = proformaRepository, companyProfileDao = purchaseDatabase.companyProfileDao()))
            ProformaDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("sales/proforma/edit/$id") })
        }

        composable(
            route = "sales/sample/{sampleId}",
            arguments = listOf(navArgument("sampleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sampleId = backStackEntry.arguments?.getLong("sampleId") ?: 0L
            val detailViewModel: SampleDetailViewModel = viewModel(factory = SampleDetailViewModelFactory(sampleId = sampleId, repository = sampleIssueRepository, companyProfileDao = purchaseDatabase.companyProfileDao()))
            SampleDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("sales/sample/edit/$id") })
        }

        composable(route = "sales/invoice/new") {
            val salesViewModel: SalesViewModel = viewModel(
                factory = SalesViewModelFactory(
                    salesRepository = salesRepository,
                    partyRepository = purchasePartyRepository,
                    productRepository = salesProductRepository,
                    inventoryRepository = salesInventoryRepository,
                    challanRepository = challanRepository,
                    sampleIssueRepository = sampleIssueRepository,
                    numberingRepository = numberingRepository
                )
            )
            SalesEntryScreen(
                viewModel = salesViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onSavedToDetail = { id -> navController.navigate("sales/invoice/$id") { popUpTo("sales/invoice/new") { inclusive = true } } }
            )
        }

        composable(
            route = "sales/invoice/edit/{saleId}",
            arguments = listOf(navArgument("saleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: 0L
            val salesViewModel: SalesViewModel = viewModel(
                factory = SalesViewModelFactory(
                    salesRepository = salesRepository,
                    partyRepository = purchasePartyRepository,
                    productRepository = salesProductRepository,
                    inventoryRepository = salesInventoryRepository,
                    challanRepository = challanRepository,
                    sampleIssueRepository = sampleIssueRepository,
                    numberingRepository = numberingRepository,
                    editSaleId = saleId
                )
            )
            SalesEntryScreen(
                viewModel = salesViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onSavedToDetail = { id -> navController.navigate("sales/invoice/$id") { popUpTo("sales/invoice/edit/$id") { inclusive = true } } }
            )
        }

        composable(route = "sales/challan/new") {
            val challanViewModel: NewChallanViewModel = viewModel(
                factory = NewChallanViewModelFactory(
                    challanRepository = challanRepository,
                    partyRepository = purchasePartyRepository,
                    productRepository = purchaseProductRepository,
                    inventoryRepository = salesInventoryRepository,
                    numberingRepository = numberingRepository
                )
            )
            NewChallanScreen(
                viewModel = challanViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/challan/$id") { popUpTo("sales/challan/new") { inclusive = true } } }
            )
        }

        composable(
            route = "sales/challan/edit/{challanId}",
            arguments = listOf(navArgument("challanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val challanId = backStackEntry.arguments?.getLong("challanId") ?: 0L
            val challanViewModel: NewChallanViewModel = viewModel(
                factory = NewChallanViewModelFactory(
                    challanRepository = challanRepository,
                    partyRepository = purchasePartyRepository,
                    productRepository = purchaseProductRepository,
                    inventoryRepository = salesInventoryRepository,
                    numberingRepository = numberingRepository,
                    editChallanId = challanId
                )
            )
            NewChallanScreen(
                viewModel = challanViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/challan/$id") { popUpTo("sales/challan/edit/$challanId") { inclusive = true } } }
            )
        }

        composable(route = "sales/creditnote/new") {
            val creditNoteViewModel: NewCreditNoteViewModel = viewModel(
                factory = NewCreditNoteViewModelFactory(
                    salesRepository = salesRepository,
                    creditNoteRepository = salesCreditNoteRepository,
                    numberingRepository = numberingRepository
                )
            )
            NewCreditNoteScreen(
                viewModel = creditNoteViewModel,
                onBack = { navController.popBackStack() },
                onOpenRegister = { navController.navigate("sales_register/${SalesRegisterType.CREDIT_NOTE.name}") },
                onSavedToDetail = { id -> navController.navigate("sales/creditnote/$id") { popUpTo("sales/creditnote/new") { inclusive = true } } }
            )
        }

        composable(
            route = "sales/creditnote/edit/{cnId}",
            arguments = listOf(navArgument("cnId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cnId = backStackEntry.arguments?.getLong("cnId") ?: 0L
            val creditNoteViewModel: NewCreditNoteViewModel = viewModel(
                factory = NewCreditNoteViewModelFactory(
                    salesRepository = salesRepository,
                    creditNoteRepository = salesCreditNoteRepository,
                    numberingRepository = numberingRepository,
                    editingId = cnId
                )
            )
            NewCreditNoteScreen(
                viewModel = creditNoteViewModel,
                onBack = { navController.popBackStack() },
                onOpenRegister = { navController.navigate("sales_register/${SalesRegisterType.CREDIT_NOTE.name}") },
                onSavedToDetail = { id -> navController.navigate("sales/creditnote/$id") { popUpTo("sales/creditnote/edit/$cnId") { inclusive = true } } }
            )
        }

        composable(route = "sales/proforma/new") {
            val proformaViewModel: ProformaInvoiceViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProformaInvoiceViewModel(
                            repository = proformaRepository,
                            partyRepository = purchasePartyRepository,
                            productRepository = salesProductRepository,
                            inventoryRepository = salesInventoryRepository,
                            companyProfileRepository = companyProfileRepository
                        ) as T
                    }
                }
            )
            ProformaInvoiceScreen(
                viewModel = proformaViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/proforma/$id") { popUpTo("sales/proforma/new") { inclusive = true } } }
            )
        }

        composable(
            route = "sales/proforma/edit/{proformaId}",
            arguments = listOf(navArgument("proformaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val proformaId = backStackEntry.arguments?.getLong("proformaId") ?: 0L
            val proformaViewModel: ProformaInvoiceViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProformaInvoiceViewModel(
                            repository = proformaRepository,
                            partyRepository = purchasePartyRepository,
                            productRepository = salesProductRepository,
                            inventoryRepository = salesInventoryRepository,
                            companyProfileRepository = companyProfileRepository
                        ) as T
                    }
                }
            )
            LaunchedEffect(proformaId) {
                proformaViewModel.loadProforma(proformaId)
            }
            ProformaInvoiceScreen(
                viewModel = proformaViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/proforma/$id") { popUpTo("sales/proforma/edit/$proformaId") { inclusive = true } } }
            )
        }

        composable(route = "sales/sample/new") {
            val sampleViewModel: SampleIssueViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SampleIssueViewModel(
                            repository = sampleIssueRepository,
                            partyRepository = purchasePartyRepository,
                            inventoryRepository = salesInventoryRepository,
                            productRepository = purchaseProductRepository,
                            numberingRepository = numberingRepository
                        ) as T
                    }
                }
            )
            SampleIssueScreen(
                viewModel = sampleViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/sample/$id") { popUpTo("sales/sample/new") { inclusive = true } } }
            )
        }

        composable(
            route = "sales/sample/edit/{sampleId}",
            arguments = listOf(navArgument("sampleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sampleId = backStackEntry.arguments?.getLong("sampleId") ?: 0L
            val sampleViewModel: SampleIssueViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SampleIssueViewModel(
                            repository = sampleIssueRepository,
                            partyRepository = purchasePartyRepository,
                            inventoryRepository = salesInventoryRepository,
                            productRepository = purchaseProductRepository,
                            numberingRepository = numberingRepository
                        ) as T
                    }
                }
            )
            LaunchedEffect(sampleId) {
                sampleViewModel.loadSample(sampleId)
            }
            SampleIssueScreen(
                viewModel = sampleViewModel,
                onBack = { navController.popBackStack() },
                onSavedToDetail = { id -> navController.navigate("sales/sample/$id") { popUpTo("sales/sample/edit/$sampleId") { inclusive = true } } }
            )
        }

        composable(route = "purchase_home") {
            PurchaseHomeScreen(
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewPurchase = { navController.navigate("purchase_order_new") },
                onPurchaseInvoice = { navController.navigate("purchase_entry") },
                onPurchaseRegister = { navController.navigate("purchase_register") },
                onPurchaseReturn = { navController.navigate("purchase_return") },
                onPurchaseReturnRegister = { navController.navigate("purchase_return_saved_register") },
                onPurchaseOrderRegister = { navController.navigate("purchase_order_register") }
            )
        }

        composable(route = "purchase_order_new") {
            val poViewModel: PurchaseOrderViewModel = viewModel(
                factory = PurchaseOrderViewModelFactory(
                    partyRepository = purchasePartyRepository,
                    purchaseRepository = purchaseRepository,
                    productRepository = purchaseProductRepository,
                    auditTrailRepository = auditTrailRepository,
                    numberingRepository = numberingRepository
                )
            )
            PurchaseOrderEntryScreen(
                viewModel = poViewModel,
                onAddProductClick = { navController.navigate("add_purchase_order_item") },
                onEditProductClick = { index -> navController.navigate("add_purchase_order_item?index=$index") },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(
            route = "purchase_order_new_from_shortage?pid={pid}&pwr={pwr}&q={q}",
            arguments = listOf(
                navArgument("pid") { type = NavType.LongType; defaultValue = 0L },
                navArgument("pwr") { type = NavType.StringType; defaultValue = "" },
                navArgument("q") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getLong("pid") ?: 0L
            val pwr = backStackEntry.arguments?.getString("pwr") ?: ""
            val q = backStackEntry.arguments?.getInt("q") ?: 0
            
            val poViewModel: PurchaseOrderViewModel = viewModel(
                factory = PurchaseOrderViewModelFactory(
                    partyRepository = purchasePartyRepository,
                    purchaseRepository = purchaseRepository,
                    productRepository = purchaseProductRepository,
                    auditTrailRepository = auditTrailRepository,
                    numberingRepository = numberingRepository,
                    initialProductId = pid,
                    initialPower = pwr,
                    initialQty = q
                )
            )
            PurchaseOrderEntryScreen(
                viewModel = poViewModel,
                onAddProductClick = { navController.navigate("add_purchase_order_item") },
                onEditProductClick = { index -> navController.navigate("add_purchase_order_item?index=$index") },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(
            route = "add_purchase_order_item?index={index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: -1
            
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry("purchase_order_new")
                } catch (e: Exception) {
                    navController.getBackStackEntry("purchase_order_new_from_shortage?pid={pid}&pwr={pwr}&q={q}")
                }
            }

            val poViewModel: PurchaseOrderViewModel = viewModel(viewModelStoreOwner = parentEntry)
            val addItemViewModel: AddPurchaseOrderItemViewModel = viewModel(
                factory = AddPurchaseOrderItemViewModelFactory(
                    productRepository = purchaseProductRepository,
                    stockUseCase = stockEngine
                )
            )
            
            LaunchedEffect(index) {
                if (index >= 0) {
                    poViewModel.uiState.value.items.getOrNull(index)?.let {
                        addItemViewModel.loadItem(it)
                    }
                }
            }

            AddPurchaseOrderItemScreen(
                viewModel = addItemViewModel,
                onAddItems = { items ->
                    if (index >= 0) {
                        // In edit mode, we replace the original line with the first variant
                        // and add any additional variants as new lines
                        items.forEachIndexed { i, item ->
                            if (i == 0) poViewModel.updateItem(index, item)
                            else poViewModel.addItem(item)
                        }
                    } else {
                        // Add all variants as separate lines
                        items.forEach { poViewModel.addItem(it) }
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "purchase_entry?productId={productId}&power={power}&qty={qty}&status={status}", 
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("power") { type = NavType.StringType; defaultValue = "" },
                navArgument("qty") { type = NavType.IntType; defaultValue = 0 },
                navArgument("status") { type = NavType.StringType; defaultValue = "POSTED" }
            )
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getLong("productId") ?: 0L
            val pwr = backStackEntry.arguments?.getString("power") ?: ""
            val q = backStackEntry.arguments?.getInt("qty") ?: 0
            val st = backStackEntry.arguments?.getString("status") ?: "POSTED"

            val purchaseViewModel: PurchaseViewModel = viewModel(
                factory = PurchaseViewModelFactory(
                    partyRepository = purchasePartyRepository,
                    purchaseRepository = purchaseRepository,
                    productRepository = purchaseProductRepository,
                    auditTrailRepository = auditTrailRepository,
                    initialProductId = pid,
                    initialPower = pwr,
                    initialQty = q,
                    initialStatus = st
                )
            )
            PurchaseEntryScreen(
                viewModel = purchaseViewModel,
                onAddProductClick = { navController.navigate("add_purchase_items") },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(
            route = "purchase_edit/{purchaseId}",
            arguments = listOf(navArgument("purchaseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getLong("purchaseId") ?: 0L
            val purchaseViewModel: PurchaseViewModel = viewModel(
                factory = PurchaseViewModelFactory(
                    partyRepository = purchasePartyRepository,
                    purchaseRepository = purchaseRepository,
                    productRepository = purchaseProductRepository,
                    auditTrailRepository = auditTrailRepository,
                    numberingRepository = numberingRepository
                )
            )
            LaunchedEffect(purchaseId) {
                purchaseViewModel.loadPurchaseForEdit(purchaseId)
            }
            PurchaseEntryScreen(
                viewModel = purchaseViewModel,
                onAddProductClick = { navController.navigate("add_purchase_items") },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "add_purchase_items") {
            val purchaseEntryEntry = remember(it) {
                navController.previousBackStackEntry
            }
            val entryViewModel: PurchaseViewModel = if (purchaseEntryEntry != null) {
                viewModel(viewModelStoreOwner = purchaseEntryEntry)
            } else {
                // Fallback (should not normally happen if navigating from entry)
                viewModel(
                    factory = PurchaseViewModelFactory(
                        partyRepository = purchasePartyRepository,
                        purchaseRepository = purchaseRepository,
                        productRepository = purchaseProductRepository,
                        auditTrailRepository = auditTrailRepository,
                        numberingRepository = numberingRepository
                    )
                )
            }

            val addPurchaseItemViewModel: AddPurchaseItemViewModel = viewModel(factory = AddPurchaseItemViewModelFactory(productRepository = purchaseProductRepository, purchaseRepository = purchaseRepository))
            AddPurchaseItemScreen(
                viewModel = addPurchaseItemViewModel,
                onAddItem = { item ->
                    entryViewModel.addItem(item)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "purchase_order_register") {
            val registerViewModel: PurchaseRegisterViewModel = viewModel(
                factory = PurchaseRegisterViewModelFactory(
                    purchaseRepository = purchaseRepository,
                    purchaseReturnRepository = purchaseReturnRepository,
                    requiredStatus = "ORDER"
                )
            )
            PurchaseRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewPurchase = { navController.navigate("purchase_entry?status=ORDER") },
                onPurchaseClick = { id -> navController.navigate("purchase_detail/$id") },
                onEditPurchase = { id -> navController.navigate("purchase_edit/$id") },
                title = "Purchase Order Register"
            )
        }

        composable(route = "purchase_register") {
            val purchaseRegisterViewModel: PurchaseRegisterViewModel = viewModel(factory = PurchaseRegisterViewModelFactory(purchaseRepository = purchaseRepository, purchaseReturnRepository = purchaseReturnRepository))
            PurchaseRegisterScreen(
                viewModel = purchaseRegisterViewModel, 
                onBack = { navController.popBackStack() }, 
                onDashboard = { navController.navigate("dashboard") },
                onNewPurchase = { navController.navigate("purchase_entry") },
                onPurchaseClick = { purchaseId -> navController.navigate("purchase_detail/$purchaseId") }
            )
        }

        composable(route = "purchase_detail/{purchaseId}", arguments = listOf(navArgument("purchaseId") { type = NavType.LongType })) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getLong("purchaseId") ?: 0L
            val detailViewModel: PurchaseDetailViewModel = viewModel(
                factory = PurchaseDetailViewModelFactory(
                    purchaseId = purchaseId, 
                    purchaseRepository = purchaseRepository, 
                    productRepository = purchaseProductRepository,
                    documentRuntime = documentRuntime
                )
            )
            PurchaseDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEditPurchase = { id -> navController.navigate("purchase_edit/$id") })
        }

        composable(route = "purchase_return") {
            val purchaseReturnViewModel: PurchaseReturnViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnViewModel(purchaseReturnRepository = purchaseReturnRepository, productRepository = purchaseProductRepository) as T
                }
            })
            PurchaseReturnScreen(viewModel = purchaseReturnViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onSaved = { navController.navigate("purchase_return_saved_register") { popUpTo("purchase_home") } })
        }

        composable(
            route = "purchase_return_new/{purchaseId}",
            arguments = listOf(navArgument("purchaseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getLong("purchaseId") ?: 0L
            val purchaseReturnViewModel: PurchaseReturnViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnViewModel(purchaseReturnRepository = purchaseReturnRepository, productRepository = purchaseProductRepository) as T
                }
            })
            LaunchedEffect(purchaseId) {
                purchaseReturnViewModel.loadOriginalPurchase(purchaseId)
            }
            PurchaseReturnScreen(viewModel = purchaseReturnViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onSaved = { navController.navigate("purchase_return_saved_register") { popUpTo("purchase_home") } })
        }

        composable(
            route = "purchase_return_edit/{returnId}",
            arguments = listOf(navArgument("returnId") { type = NavType.LongType })
        ) { backStackEntry ->
            val returnId = backStackEntry.arguments?.getLong("returnId") ?: 0L
            val purchaseReturnViewModel: PurchaseReturnViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnViewModel(purchaseReturnRepository = purchaseReturnRepository, productRepository = purchaseProductRepository) as T
                }
            })
            LaunchedEffect(returnId) {
                purchaseReturnViewModel.loadPurchaseReturnForEdit(returnId)
            }
            PurchaseReturnScreen(viewModel = purchaseReturnViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onSaved = { navController.navigate("purchase_return_saved_register") { popUpTo("purchase_home") } })
        }

        composable(route = "purchase_return_register/{supplierId}/{supplierName}", arguments = listOf(navArgument("supplierId") { type = NavType.LongType }, navArgument("supplierName") { type = NavType.StringType })) { backStackEntry ->
            val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
            val supplierName = backStackEntry.arguments?.getString("supplierName") ?: ""
            val registerViewModel: PurchaseReturnRegisterViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnRegisterViewModel(repository = purchaseReturnRepository) as T
                }
            })
            PurchaseReturnRegisterScreen(viewModel = registerViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onCreateReturn = { purchaseId -> navController.navigate("purchase_return_new/$purchaseId") })
        }

        composable(route = "purchase_return_saved_register") {
            val savedRegisterViewModel: PurchaseReturnSavedRegisterViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnSavedRegisterViewModel(repository = purchaseReturnRepository) as T
                }
            })
            PurchaseReturnSavedRegisterScreen(viewModel = savedRegisterViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onReturnClick = { returnId -> navController.navigate("purchase_return_detail/$returnId") })
        }

        composable(route = "purchase_return_detail/{returnId}", arguments = listOf(navArgument("returnId") { type = NavType.LongType })) { backStackEntry ->
            val returnId = backStackEntry.arguments?.getLong("returnId") ?: 0L
            val detailViewModel: PurchaseReturnDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchaseReturnDetailViewModel(repository = purchaseReturnRepository) as T
                }
            })
            LaunchedEffect(returnId) {
                detailViewModel.loadPurchaseReturn(returnId)
            }
            PurchaseReturnDetailScreen(viewModel = detailViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onEdit = { id -> navController.navigate("purchase_return_edit/$id") })
        }

        composable(route = "inventory_home") {
            InventoryHomeScreen(
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onStockRegisterClick = { navController.navigate("stock_register") },
                onSerialStockRegisterClick = { navController.navigate("serial_stock_register") },
                onOpeningStockClick = { navController.navigate("opening_stock_list") },
                onStockAdjustmentClick = { navController.navigate("stock_adjustment") },
                onStockReconciliationClick = { navController.navigate("stock_reconciliation") },
                onAlertSettingsClick = { navController.navigate("inventory_alert_settings") }
            )
        }

        composable(route = "stock_adjustment") {
            val adjustmentViewModel: StockAdjustmentViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StockAdjustmentViewModel(
                            productRepository = purchaseProductRepository,
                            useCase = stockAdjustmentUseCase
                        ) as T
                    }
                }
            )
            StockAdjustmentScreen(
                viewModel = adjustmentViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "stock_reconciliation") {
            val reconciliationViewModel: StockReconciliationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StockReconciliationViewModel(
                            productRepository = purchaseProductRepository,
                            useCase = stockReconciliationUseCase
                        ) as T
                    }
                }
            )
            StockReconciliationScreen(
                viewModel = reconciliationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "low_stock_status") {
            val lowStockViewModel: LowStockStatusViewModel = viewModel(
                factory = LowStockStatusViewModelFactory(lowStockUseCase)
            )
            LowStockStatusScreen(
                viewModel = lowStockViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onPurchaseEntry = { navController.navigate("purchase_order_new") },
                onPurchaseEntryWithParams = { productId, power, qty ->
                    navController.navigate("purchase_order_new_from_shortage?pid=$productId&pwr=$power&q=$qty")
                },
                onThresholdSettings = { navController.navigate("inventory_alert_settings") }
            )
        }

        composable(route = "inventory_alert_settings") {
            val thresholdViewModel: InventoryThresholdViewModel = viewModel(
                factory = InventoryThresholdViewModelFactory(
                    productDao = purchaseDatabase.productDao(),
                    thresholdRepository = thresholdRepository,
                    stockUseCase = stockEngine
                )
            )
            InventoryThresholdScreen(
                viewModel = thresholdViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "stock_register") {
            val stockRegisterViewModel: StockRegisterViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StockRegisterViewModel(repository = inventoryStockRepository) as T
                }
            })
            StockRegisterScreen(viewModel = stockRegisterViewModel, onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") })
        }

        composable(route = "serial_stock_register") {
            val serialStockViewModel: SerialStockViewModel = viewModel(factory = SerialStockViewModelFactory(repository = SerialStockRepository(serialStockDao = purchaseDatabase.serialStockDao())))
            SerialStockRegisterScreen(
                viewModel = serialStockViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onSerialClick = { inventoryUnitId -> navController.navigate("serial_movement_history/$inventoryUnitId") }
            )
        }

        composable(route = "serial_movement_history/{unitId}", arguments = listOf(navArgument("unitId") { type = NavType.LongType })) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getLong("unitId") ?: 0L
            val movementViewModel: SerialMovementHistoryViewModel = viewModel(factory = SerialMovementHistoryViewModelFactory(inventoryUnitId = unitId, serialStockRepository = SerialStockRepository(serialStockDao = purchaseDatabase.serialStockDao()), stockMovementRepository = StockMovementRepository(purchaseDatabase.stockMovementDao())))
            SerialMovementHistoryScreen(viewModel = movementViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = "opening_stock_list") {
            val openingStockViewModel: OpeningStockViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OpeningStockViewModel(
                        repository = openingStockRepository,
                        useCase = openingStockUseCase,
                        productMasterRepository = purchaseProductRepository,
                        numberingRepository = numberingRepository
                    ) as T
                }
            })
            OpeningStockListScreen(viewModel = openingStockViewModel, onBack = { navController.popBackStack() }, onNewEntry = { navController.navigate("opening_stock_entry/0") }, onEditEntry = { id -> navController.navigate("opening_stock_entry/$id") })
        }

        composable(route = "opening_stock_entry/{stockId}", arguments = listOf(navArgument("stockId") { type = NavType.LongType })) { backStackEntry ->
            val stockId = backStackEntry.arguments?.getLong("stockId") ?: 0L
            val openingStockViewModel: OpeningStockViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OpeningStockViewModel(
                        repository = openingStockRepository,
                        useCase = openingStockUseCase,
                        productMasterRepository = purchaseProductRepository,
                        numberingRepository = numberingRepository
                    ) as T
                }
            })
            OpeningStockEntryScreen(
                stockId = stockId,
                viewModel = openingStockViewModel,
                onAddItemClick = { navController.navigate("add_opening_stock_item") },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "add_opening_stock_item") {
            val parentEntry = navController.previousBackStackEntry
            val openingStockViewModel: OpeningStockViewModel = if (parentEntry != null) {
                viewModel(viewModelStoreOwner = parentEntry)
            } else {
                viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return OpeningStockViewModel(
                            repository = openingStockRepository,
                            useCase = openingStockUseCase,
                            productMasterRepository = purchaseProductRepository,
                            numberingRepository = numberingRepository
                        ) as T
                    }
                })
            }
            AddOpeningStockItemScreen(
                productRepository = purchaseProductRepository, 
                onAddItem = { items -> 
                    items.forEach { openingStockViewModel.addItem(it) }
                    navController.popBackStack() 
                }, 
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "payment_home") {
            PaymentHomeScreen(
                onBack = { navController.popBackStack() }, 
                onReceiptClick = { navController.navigate("payment_entry/RECEIPT") }, 
                onPaymentClick = { navController.navigate("payment_entry/PAYMENT") }, 
                onReceiptRegisterClick = { navController.navigate("payment_register/RECEIPT") }, 
                onPaymentRegisterClick = { navController.navigate("payment_register/PAYMENT") },
                onReceivablesEnquiryClick = { navController.navigate("receivables_enquiry") }
            )
        }

        composable(route = "receivables_enquiry") {
            val receivablesViewModel: ReceivablesViewModel = viewModel(
                factory = ReceivablesViewModelFactory(reportingUseCase = reportingUseCase)
            )
            ReceivablesEnquiryScreen(
                viewModel = receivablesViewModel,
                onBack = { navController.popBackStack() },
                onPartyClick = { partyId ->
                    navController.navigate("financial_report/customer_ledger?partyId=$partyId")
                }
            )
        }

        composable(route = "payment_entry/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "RECEIPT"
            val paymentViewModel: PaymentViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PaymentViewModel(
                        paymentUseCase = paymentUseCase, 
                        accountRepository = accountRepository,
                        partyRepository = purchasePartyRepository,
                        financialTransactionRepository = financialTransactionRepository,
                        salesRepository = salesRepository,
                        purchaseRepository = purchaseRepository
                    ) as T
                }
            })
            PaymentEntryScreen(type = type, viewModel = paymentViewModel, fyStart = currentFinancialYear, onBack = { navController.popBackStack() })
        }

        composable(route = "payment_register/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "RECEIPT"
            val paymentViewModel: PaymentViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PaymentViewModel(
                        paymentUseCase = paymentUseCase, 
                        accountRepository = accountRepository,
                        partyRepository = purchasePartyRepository,
                        financialTransactionRepository = financialTransactionRepository,
                        salesRepository = salesRepository,
                        purchaseRepository = purchaseRepository
                    ) as T
                }
            })
            PaymentRegisterScreen(
                type = type, 
                viewModel = paymentViewModel, 
                repository = financialTransactionRepository, 
                onBack = { navController.popBackStack() }, 
                onViewDetail = { transactionId -> navController.navigate("payment_detail/$transactionId") }, 
                onEditDraft = { transactionId -> 
                    paymentViewModel.loadForEdit(transactionId)
                    navController.navigate("payment_entry/$type")
                }
            )
        }

        composable(route = "payment_detail/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.LongType })) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            val paymentViewModel: PaymentViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PaymentViewModel(
                        paymentUseCase = paymentUseCase, 
                        accountRepository = accountRepository,
                        partyRepository = purchasePartyRepository,
                        financialTransactionRepository = financialTransactionRepository,
                        salesRepository = salesRepository,
                        purchaseRepository = purchaseRepository
                    ) as T
                }
            })
            PaymentDetailScreen(
                transactionId = transactionId, 
                viewModel = paymentViewModel, 
                onBack = { navController.popBackStack() }, 
                onDashboard = { navController.navigate("dashboard") },
                onEdit = { id, type ->
                    navController.navigate("payment_entry/$type") 
                }
            )
        }

        composable(route = "gst_home") {
            GstHomeScreen(onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") }, onGstDashboardClick = { navController.navigate("gst_dashboard") }, onGstReportClick = { type -> navController.navigate("gst_reports/$type") })
        }

        composable(route = "gst_dashboard") {
            val gstDashboardViewModel: GstDashboardViewModel = viewModel(factory = GstDashboardViewModelFactory(repository = GstSummaryRepository(salesRepository = salesRepository, purchaseRepository = purchaseRepository)))
            GstDashboardScreen(viewModel = gstDashboardViewModel, financialYearStart = currentFinancialYear, financialYearDisplayName = "${currentFinancialYear}-${currentFinancialYear + 1}", onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") })
        }

        composable(route = "gst_reports/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: GstReportType.GSTR1.routeKey
            val gstReportsViewModel: GstReportsViewModel = viewModel(factory = GstReportsViewModelFactory(repository = GstReportingRepository(salesRepository = salesRepository, purchaseRepository = purchaseRepository, salesCreditNoteRepository = salesCreditNoteRepository, productRepository = salesProductRepository)))
            GstReportScreen(reportType = GstReportType.fromRouteKey(type), viewModel = gstReportsViewModel, financialYearStart = currentFinancialYear, financialYearDisplayName = "${currentFinancialYear}-${currentFinancialYear + 1}", onBack = { navController.popBackStack() }, onDashboard = { navController.navigate("dashboard") })
        }

        salesReportsGraph(navController = navController, database = purchaseDatabase, lowStockUseCase = lowStockUseCase)

        composable(route = "settings_home") {
            SessionGuard(onSessionExpired = { navController.navigate("login") { popUpTo("splash") { inclusive = true }; launchSingleTop = true } }) {
                SettingsScreen(onBack = { navController.popBackStack() }, onCompanyProfileClick = { navController.navigate("company_profile") }, onFinancialYearClick = { navController.navigate("financial_year_settings") }, onAuditTrailClick = { navController.navigate("audit_trail") }, onBackupRestoreClick = { navController.navigate("backup_restore") })
            }
        }

        composable(route = "backup_restore") {
            SessionGuard(onSessionExpired = { navController.navigate("login") { popUpTo("splash") { inclusive = true }; launchSingleTop = true } }) {
                val googleBackupViewModel: GoogleBackupViewModel = viewModel(
                    factory = GoogleBackupViewModelFactory(
                        authManager = googleAuthManager,
                        backupUseCase = backupUseCase,
                        backupRepository = backupRepository,
                        settingsManager = backupSettingsManager
                    )
                )
                BackupRestoreScreen(onBack = { navController.popBackStack() }, viewModel = googleBackupViewModel)
            }
        }

        composable(route = "company_profile") {
            CompanyProfileScreen(
                repository = companyProfileRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "financial_year_settings") {
            FinancialYearSettingsScreen(viewModel = financialYearViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = "audit_trail") {
            val auditTrailViewModel: AuditTrailViewModel = viewModel(factory = AuditTrailViewModelFactory(auditTrailRepository = auditTrailRepository))
            AuditTrailScreen(viewModel = auditTrailViewModel, onBack = { navController.popBackStack() })
        }
    }
}
