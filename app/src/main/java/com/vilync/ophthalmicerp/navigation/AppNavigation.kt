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
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import com.vilync.ophthalmicerp.data.repository.AuthRepository
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.UserRepository
import com.vilync.ophthalmicerp.data.repository.InventoryStockRepository
import com.vilync.ophthalmicerp.data.repository.SerialStockRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ChallanRepository
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialStockRegisterScreen
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialStockViewModel
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialStockViewModelFactory
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialMovementHistoryScreen
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialMovementHistoryViewModel
import com.vilync.ophthalmicerp.feature.inventory.serialstock.SerialMovementHistoryViewModelFactory

import com.vilync.ophthalmicerp.feature.dashboard.DashboardScreen
import com.vilync.ophthalmicerp.feature.gst.presentation.GstHomeScreen
import com.vilync.ophthalmicerp.feature.gst.presentation.GstDashboardScreen
import com.vilync.ophthalmicerp.feature.gst.presentation.GstDashboardViewModel
import com.vilync.ophthalmicerp.feature.gst.presentation.GstDashboardViewModelFactory
import com.vilync.ophthalmicerp.feature.gst.data.GstSummaryRepository
import com.vilync.ophthalmicerp.feature.gst.data.GstReportingRepository
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import com.vilync.ophthalmicerp.feature.gst.presentation.GstReportScreen
import com.vilync.ophthalmicerp.feature.gst.presentation.GstReportsViewModel
import com.vilync.ophthalmicerp.feature.gst.presentation.GstReportsViewModelFactory
import com.vilync.ophthalmicerp.feature.sales.presentation.SalesEntryScreen
import com.vilync.ophthalmicerp.feature.sales.presentation.SalesViewModel
import com.vilync.ophthalmicerp.feature.sales.presentation.SalesViewModelFactory
import com.vilync.ophthalmicerp.feature.sales.presentation.SalesHomeScreen
import com.vilync.ophthalmicerp.feature.sales.presentation.SalesDocumentCategoryScreen
import com.vilync.ophthalmicerp.feature.sales.register.SalesRegisterScreen
import com.vilync.ophthalmicerp.feature.sales.register.SalesRegisterType
import com.vilync.ophthalmicerp.feature.sales.register.SalesRegisterViewModel
import com.vilync.ophthalmicerp.feature.sales.register.SalesRegisterViewModelFactory
import com.vilync.ophthalmicerp.feature.sales.detail.SalesInvoiceDetailScreen
import com.vilync.ophthalmicerp.feature.sales.detail.SalesInvoiceDetailViewModel
import com.vilync.ophthalmicerp.feature.sales.detail.SalesInvoiceDetailViewModelFactory
import com.vilync.ophthalmicerp.feature.sales.challan.presentation.NewChallanScreen
import com.vilync.ophthalmicerp.feature.sales.challan.presentation.NewChallanViewModel
import com.vilync.ophthalmicerp.feature.sales.challan.presentation.NewChallanViewModelFactory
import com.vilync.ophthalmicerp.feature.sales.creditnote.presentation.NewCreditNoteScreen
import com.vilync.ophthalmicerp.feature.sales.creditnote.presentation.NewCreditNoteViewModel
import com.vilync.ophthalmicerp.feature.sales.creditnote.presentation.NewCreditNoteViewModelFactory
import androidx.compose.ui.graphics.Color
import com.vilync.ophthalmicerp.feature.inventory.InventoryHomeScreen
import com.vilync.ophthalmicerp.feature.inventory.StockRegisterScreen
import com.vilync.ophthalmicerp.feature.inventory.StockRegisterViewModel
import com.vilync.ophthalmicerp.feature.login.LoginScreen
import com.vilync.ophthalmicerp.feature.login.LoginViewModel
import com.vilync.ophthalmicerp.feature.login.LoginViewModelFactory
import com.vilync.ophthalmicerp.feature.login.FirstAdminSetupScreen
import com.vilync.ophthalmicerp.feature.login.FirstAdminSetupViewModel
import com.vilync.ophthalmicerp.feature.login.FirstAdminSetupViewModelFactory
import com.vilync.ophthalmicerp.feature.login.AppStartupViewModel
import com.vilync.ophthalmicerp.feature.login.AppStartupViewModelFactory
import com.vilync.ophthalmicerp.feature.login.StartupDestination
import com.vilync.ophthalmicerp.feature.master.presentation.MasterScreen

import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.presentation.ProductListScreen
import com.vilync.ophthalmicerp.feature.master.product.presentation.ProductListViewModel

import com.vilync.ophthalmicerp.feature.master.product.presentation.ProductMasterScreen
import com.vilync.ophthalmicerp.feature.master.product.presentation.ProductMasterViewModel

import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
import com.vilync.ophthalmicerp.feature.purchase.PurchaseHomeScreen
import com.vilync.ophthalmicerp.feature.purchase.presentation.AddPurchaseItemScreen
import com.vilync.ophthalmicerp.feature.purchase.presentation.AddPurchaseItemViewModel
import com.vilync.ophthalmicerp.feature.purchase.presentation.AddPurchaseItemViewModelFactory
import com.vilync.ophthalmicerp.feature.purchase.presentation.PurchaseEntryScreen
import com.vilync.ophthalmicerp.feature.purchase.presentation.PurchaseViewModel
import com.vilync.ophthalmicerp.feature.purchase.presentation.PurchaseViewModelFactory
import com.vilync.ophthalmicerp.feature.purchase.detail.PurchaseDetailScreen
import com.vilync.ophthalmicerp.feature.purchase.detail.PurchaseDetailViewModel
import com.vilync.ophthalmicerp.feature.purchase.detail.PurchaseDetailViewModelFactory
import com.vilync.ophthalmicerp.feature.purchase.register.PurchaseRegisterScreen
import com.vilync.ophthalmicerp.feature.purchase.register.PurchaseRegisterViewModel
import com.vilync.ophthalmicerp.feature.purchase.register.PurchaseRegisterViewModelFactory
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnScreen
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnViewModel
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnRegisterScreen
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnRegisterViewModel
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnSavedRegisterScreen
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnSavedRegisterViewModel
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnDetailScreen
import com.vilync.ophthalmicerp.feature.purchase.returnentry.PurchaseReturnDetailViewModel

import com.vilync.ophthalmicerp.feature.settings.FinancialYearSettingsScreen
import com.vilync.ophthalmicerp.feature.settings.SettingsScreen
import com.vilync.ophthalmicerp.feature.settings.backup.BackupRestoreScreen
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import com.vilync.ophthalmicerp.feature.companyprofile.presentation.CompanyProfileScreen
import com.vilync.ophthalmicerp.feature.settings.audit.AuditTrailScreen
import com.vilync.ophthalmicerp.feature.settings.audit.AuditTrailViewModel
import com.vilync.ophthalmicerp.feature.settings.audit.AuditTrailViewModelFactory
import com.vilync.ophthalmicerp.feature.splash.SplashScreen

import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupRepository
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupService
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListScreen
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModel
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModelFactory
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterScreen
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterViewModel
import com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterViewModelFactory


// =============================================================
// PRODUCT MASTER VIEWMODEL FACTORY
// =============================================================

private class ProductMasterViewModelFactory(
    private val repository: ProductMasterRepository,
    private val partyRepository: PartyRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>

    ): T {

        if (
            modelClass.isAssignableFrom(
                ProductMasterViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return ProductMasterViewModel(
                repository = repository,
                partyRepository = partyRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}


// =============================================================
// APP NAVIGATION
// =============================================================

@Composable
fun AppNavigation() {

    val navController =
        rememberNavController()

    val context =
        LocalContext.current


    val persistentSessionStore =
        remember(context) {
            PersistentSessionStore(
                context = context
            )
        }


    val coroutineScope =
        rememberCoroutineScope()



    // =========================================================
    // GLOBAL FINANCIAL YEAR
    // =========================================================

    val financialYearViewModel:
            FinancialYearViewModel =
        viewModel()


    // =========================================================
    // SHARED PURCHASE VIEWMODEL
    // =========================================================

    val purchaseDatabase =
        DatabaseProvider.getDatabase(
            context = context
        )


    // =========================================================
    // USER / STARTUP AUTHENTICATION
    // =========================================================

    val userRepository =
        UserRepository(
            userDao =
                purchaseDatabase.userDao()
        )

    val authRepository =
        AuthRepository(
            userRepository =
                userRepository
        )


    // =========================================================
    // CENTRAL AUDIT TRAIL REPOSITORY
    // =========================================================

    val auditTrailRepository =
        AuditTrailRepository(
            auditTrailDao =

                purchaseDatabase.auditTrailDao()
        )


    // =========================================================
    // COMPANY PROFILE REPOSITORY
    // =========================================================

    val companyProfileRepository =
        CompanyProfileRepository(
            companyProfileDao =
                purchaseDatabase.companyProfileDao()
        )

    val appStartupViewModel:
            AppStartupViewModel =
        viewModel(
            factory =
                AppStartupViewModelFactory(
                    userRepository =
                        userRepository,

                    persistentSessionStore =
                        persistentSessionStore
                )
        )

    val purchasePartyDao =
        purchaseDatabase.partyDao()

    val purchasePartyRepository =
        PartyRepository(
            partyDao = purchasePartyDao
        )


    // =========================================================
    // PURCHASE REPOSITORY
    // =========================================================

    val purchaseDao =
        purchaseDatabase.purchaseDao()

    val purchaseRepository =
        PurchaseRepository(
            purchaseDao = purchaseDao,
            database = purchaseDatabase
        )

    val purchaseReturnRepository =
        PurchaseReturnRepository(
            purchaseReturnDao = purchaseDatabase.purchaseReturnDao()
        )


    // =========================================================
    // PURCHASE PRODUCT REPOSITORY
    // =========================================================

    val purchaseProductRepository =
        ProductMasterRepository(
            productDao =

                purchaseDatabase.productDao()
        )


    // =========================================================
    // PURCHASE VIEWMODEL
    // =========================================================

    val purchaseViewModel:
            PurchaseViewModel =
        viewModel(
            factory =
                PurchaseViewModelFactory(
                    partyRepository =
                        purchasePartyRepository,

                    purchaseRepository =
                        purchaseRepository,

                    productRepository =
                        purchaseProductRepository,

                    auditTrailRepository =
                        auditTrailRepository
                )
        )


    // =========================================================
    // SALES REPOSITORIES
    // =========================================================

    val salesRepository =
        SalesRepository(
            salesDao =
                purchaseDatabase.salesDao(),
            database =
                purchaseDatabase
        )

    val salesProductRepository =
        ProductRepository(
            productDao =
                purchaseDatabase.productDao()
        )

    val salesInventoryRepository =
        InventoryRepository(
            inventoryDao =
                purchaseDatabase.inventoryDao()
        )

    val challanRepository =
        ChallanRepository(
            challanDao =
                purchaseDatabase.challanDao(),
            database =
                purchaseDatabase
        )

    val salesCreditNoteRepository =
        SalesCreditNoteRepository(
            creditNoteDao = purchaseDatabase.salesCreditNoteDao(),
            database = purchaseDatabase
        )


    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {


        // =====================================================
        // SPLASH
        // =====================================================

        composable(
            route = "splash"
        ) {

            val startupDestination by
            appStartupViewModel
                .destination

                .collectAsState()

            SplashScreen(
                onSplashFinished = {

                    when (startupDestination) {

                        StartupDestination.FirstAdminSetup -> {

                            navController.navigate(
                                "first_admin_setup"
                            ) {

                                popUpTo(
                                    "splash"
                                ) {
                                    inclusive = true
                                }
                            }
                        }

                        StartupDestination.Dashboard -> {

                            navController.navigate("dashboard") {
                                popUpTo("splash") {
                                    inclusive = true
                                }
                            }

                        }

                        StartupDestination.Login -> {

                            navController.navigate(
                                "login"
                            ) {

                                popUpTo(
                                    "splash"
                                ) {
                                    inclusive = true
                                }
                            }
                        }


                        StartupDestination.Loading -> {
                            // Startup database check is still running.
                            // Stay on Splash until the destination resolves.
                        }
                    }
                }
            )


            // If the Splash timer finishes before the Room check,
            // navigate as soon as startup destination becomes ready.
            LaunchedEffect(
                startupDestination
            ) {

                when (startupDestination) {

                    StartupDestination.FirstAdminSetup -> {

                        navController.navigate(
                            "first_admin_setup"
                        ) {

                            popUpTo(
                                "splash"
                            ) {
                                inclusive = true
                            }
                        }
                    }

                    StartupDestination.Dashboard -> {

                        navController.navigate("dashboard") {
                            popUpTo("splash") {
                                inclusive = true
                            }
                        }

                    }

                    StartupDestination.Login -> {

                        navController.navigate(
                            "login"

                        ) {

                            popUpTo(
                                "splash"
                            ) {
                                inclusive = true
                            }
                        }
                    }

                    StartupDestination.Loading -> Unit
                }
            }
        }


        // =====================================================
        // FIRST ADMINISTRATOR SETUP
        // =====================================================

        composable(
            route = "first_admin_setup"
        ) {

            val firstAdminSetupViewModel:
                    FirstAdminSetupViewModel =
                viewModel(
                    factory =
                        FirstAdminSetupViewModelFactory(
                            userRepository =
                                userRepository
                        )
                )

            FirstAdminSetupScreen(
                viewModel =
                    firstAdminSetupViewModel,

                onAdminCreated = {

                    appStartupViewModel
                        .onFirstAdminCreated()

                    navController.navigate(
                        "login"

                    ) {

                        popUpTo(
                            "first_admin_setup"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        // =====================================================
        // LOGIN
        // =====================================================

        composable(
            route = "login"
        ) {

            val loginViewModel:
                    LoginViewModel =
                viewModel(
                    factory =
                        LoginViewModelFactory(
                            authRepository =
                                authRepository,

                            auditTrailRepository =
                                auditTrailRepository,

                            persistentSessionStore =
                                persistentSessionStore
                        )
                )

            LoginScreen(
                viewModel =
                    loginViewModel,

                onLoginSuccess = {

                    navController.navigate(
                        "dashboard"

                    ) {

                        popUpTo(
                            "login"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        // =====================================================
        // DASHBOARD
        // =====================================================

        composable(
            route = "dashboard"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val activeFinancialYear by
                financialYearViewModel
                    .activeFinancialYear
                    .collectAsState()

                DashboardScreen(

                    onSalesClick = {

                        navController.navigate(
                            "sales_home"
                        )
                    },


                    onNewSaleClick = {

                        navController.navigate(
                            "sales"
                        )
                    },


                    onPurchaseClick = {

                        navController.navigate(
                            "purchase_home"
                        )
                    },


                    onNewPurchaseClick = {

                        // Quick Action: start a genuinely fresh Purchase.
                        purchaseViewModel
                            .startNewPurchase()

                        navController.navigate(
                            "purchase"
                        )
                    },

                    onInventoryClick = {

                        navController.navigate(
                            "inventory_home"
                        )
                    },

                    onMasterClick = {

                        navController.navigate(
                            "master"
                        )
                    },

                    onGstClick = {

                        navController.navigate(
                            "gst_home"
                        )
                    },

                    onSettingsClick = {

                        navController.navigate(
                            "settings"
                        )
                    },

                    onLogoutClick = {

                        coroutineScope.launch {

                            // =====================================
                            // AUDIT LOGOUT BEFORE CLEARING SESSION
                            // =====================================
                            //
                            // Keep the authenticated session active
                            // while writing the audit event so the
                            // correct user identity is captured.
                            // =====================================

                            try {

                                auditTrailRepository.recordEvent(
                                    module = "SECURITY",
                                    action = "LOGOUT",
                                    recordId =

                                        SessionManager
                                            .currentUser
                                            .value
                                            ?.id,
                                    description =
                                        "User logged out"
                                )

                            } catch (_: Exception) {

                                /*
                                 * Audit failure must never prevent
                                 * the user from logging out.
                                 */
                            }


                            // =====================================
                            // CLEAR AUTHENTICATED ERP SESSION
                            // =====================================

                            persistentSessionStore.clear()

                            SessionManager.clearSession()


                            // =====================================
                            // RETURN TO LOGIN
                            // =====================================

                            navController.navigate(
                                "login"
                            ) {

                                popUpTo(
                                    "dashboard"
                                ) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        }
                    },


                    activeFinancialYear =
                        activeFinancialYear.displayName
                )

            }
        }


        // =====================================================
        // GST HOME
        // =====================================================

        composable(
            route = "gst_home"
        ) {

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                GstHomeScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate(
                            "dashboard"
                        ) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onGstDashboardClick = {
                        navController.navigate(
                            "gst_dashboard"
                        )
                    },
                    onGstReportClick = { reportKey ->
                        navController.navigate(
                            "gst_report/$reportKey"
                        )
                    }
                )
            }
        }


        // =====================================================


        // =====================================================
        // GST DASHBOARD
        // =====================================================

        composable(
            route = "gst_dashboard"
        ) {

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                val activeFinancialYear by
                financialYearViewModel
                    .activeFinancialYear
                    .collectAsState()

                /*
                 * FinancialYear displayName is already the global FY label
                 * used by the existing Dashboard (for example 2026-27).
                 * Parsing its first four digits avoids creating a second,
                 * competing FY state inside the GST module.
                 */
                val gstFinancialYearStart =
                    activeFinancialYear
                        .displayName
                        .trim()
                        .take(4)
                        .toIntOrNull()
                        ?: 0

                val gstSummaryRepository =
                    remember(
                        salesRepository,
                        purchaseRepository
                    ) {
                        GstSummaryRepository(
                            salesRepository =
                                salesRepository,
                            purchaseRepository =
                                purchaseRepository
                        )
                    }

                val gstDashboardViewModel:
                        GstDashboardViewModel =
                    viewModel(
                        factory =
                            GstDashboardViewModelFactory(
                                repository =
                                    gstSummaryRepository
                            )
                    )

                GstDashboardScreen(
                    viewModel =
                        gstDashboardViewModel,
                    financialYearStart =
                        gstFinancialYearStart,
                    financialYearDisplayName =
                        activeFinancialYear.displayName,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate(
                            "dashboard"
                        ) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // GST REPORTS
        // =====================================================

        composable(
            route = "gst_report/{reportKey}",
            arguments = listOf(
                navArgument("reportKey") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {
                val activeFinancialYear by
                financialYearViewModel
                    .activeFinancialYear
                    .collectAsState()

                val gstFinancialYearStart =
                    activeFinancialYear
                        .displayName
                        .trim()
                        .take(4)
                        .toIntOrNull()
                        ?: 0

                val reportKey =
                    backStackEntry.arguments
                        ?.getString("reportKey")
                        .orEmpty()

                val reportType =
                    GstReportType.fromRouteKey(
                        reportKey
                    )

                val gstReportingRepository =
                    remember(
                        salesRepository,
                        purchaseRepository,
                        salesCreditNoteRepository
                    ) {
                        GstReportingRepository(
                            salesRepository = salesRepository,
                            purchaseRepository = purchaseRepository,
                            salesCreditNoteRepository = salesCreditNoteRepository
                        )
                    }

                val gstReportsViewModel:
                        GstReportsViewModel =
                    viewModel(
                        key = "gst_report_$reportKey",
                        factory =
                            GstReportsViewModelFactory(
                                repository =
                                    gstReportingRepository
                            )
                    )

                GstReportScreen(
                    reportType = reportType,
                    viewModel = gstReportsViewModel,
                    financialYearStart = gstFinancialYearStart,
                    financialYearDisplayName = activeFinancialYear.displayName,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate(
                            "dashboard"
                        ) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


// INVENTORY HOME
        // =====================================================

        composable(
            route = "inventory_home"
        ) {

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                InventoryHomeScreen(

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate(
                            "dashboard"
                        ) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },

                    onStockRegisterClick = {
                        navController.navigate(
                            "stock_register"
                        )
                    },

                    onSerialStockRegisterClick = {
                        navController.navigate(
                            "serial_stock_register"
                        )
                    }
                )
            }
        }


        // =====================================================
        // STOCK REGISTER
        // =====================================================

        composable(
            route = "stock_register"
        ) {

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                val inventoryStockRepository =
                    InventoryStockRepository(
                        inventoryStockDao =
                            purchaseDatabase.inventoryStockDao()
                    )

                val stockRegisterViewModel:
                        StockRegisterViewModel =
                    viewModel(
                        factory =
                            object : ViewModelProvider.Factory {

                                override fun <T : ViewModel> create(
                                    modelClass: Class<T>
                                ): T {

                                    if (
                                        modelClass.isAssignableFrom(
                                            StockRegisterViewModel::class.java
                                        )
                                    ) {

                                        @Suppress("UNCHECKED_CAST")
                                        return StockRegisterViewModel(
                                            repository =
                                                inventoryStockRepository
                                        ) as T
                                    }

                                    throw IllegalArgumentException(
                                        "Unknown ViewModel class: ${modelClass.name}"
                                    )
                                }
                            }
                    )

                StockRegisterScreen(
                    viewModel =
                        stockRegisterViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate(
                            "dashboard"
                        ) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // SERIAL STOCK REGISTER
        // =====================================================

        composable(
            route = "serial_stock_register"
        ) {

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                val serialStockRepository =
                    SerialStockRepository(
                        serialStockDao =
                            purchaseDatabase.serialStockDao()
                    )


                val serialStockViewModel:
                        SerialStockViewModel =
                    viewModel(
                        factory =
                            SerialStockViewModelFactory(
                                repository =
                                    serialStockRepository
                            )
                    )


                SerialStockRegisterScreen(
                    viewModel =
                        serialStockViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onSerialClick = { inventoryUnitId ->

                        navController.navigate(
                            "serial_movement_history/$inventoryUnitId"
                        )
                    }
                )
            }
        }


        // =====================================================
        // SERIAL MOVEMENT HISTORY
        // =====================================================

        composable(
            route =
                "serial_movement_history/{inventoryUnitId}",

            arguments =
                listOf(
                    navArgument(
                        "inventoryUnitId"
                    ) {
                        type =
                            NavType.LongType
                    }
                )
        ) { backStackEntry ->

            SessionGuard(
                onSessionExpired = {
                    navController.navigate(
                        "login"
                    ) {
                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                val inventoryUnitId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "inventoryUnitId"
                        )
                        ?: 0L


                val serialStockRepository =
                    SerialStockRepository(
                        serialStockDao =
                            purchaseDatabase
                                .serialStockDao()
                    )


                val stockMovementRepository =
                    StockMovementRepository(
                        stockMovementDao =
                            purchaseDatabase
                                .stockMovementDao()
                    )


                val serialMovementHistoryViewModel:
                        SerialMovementHistoryViewModel =
                    viewModel(
                        key =
                            "serial_movement_history_$inventoryUnitId",

                        factory =
                            SerialMovementHistoryViewModelFactory(

                                inventoryUnitId =
                                    inventoryUnitId,

                                serialStockRepository =
                                    serialStockRepository,

                                stockMovementRepository =
                                    stockMovementRepository
                            )
                    )


                SerialMovementHistoryScreen(
                    viewModel =
                        serialMovementHistoryViewModel,

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }


        // =====================================================
        // SETTINGS
        // =====================================================

        composable(
            route = "settings"
        ) {

            SessionGuard(
                onSessionExpired = {

                    navController.navigate(
                        "login"
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                SettingsScreen(

                    onBack = {

                        navController.popBackStack()
                    },

                    onCompanyProfileClick = {

                        navController.navigate(
                            "company_profile"
                        )
                    },

                    onFinancialYearClick = {

                        navController.navigate(
                            "financial_year_settings"

                        )
                    },

                    onAuditTrailClick = {

                        navController.navigate(
                            "audit_trail"
                        )
                    },

                    onBackupRestoreClick = {

                        navController.navigate(
                            "backup_restore"
                        )
                    }
                )
            }
        }



        // =====================================================
        // BACKUP & RESTORE
        // =====================================================

        composable(
            route = "backup_restore"
        ) {

            SessionGuard(
                onSessionExpired = {

                    navController.navigate(
                        "login"
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                BackupRestoreScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }



        // =====================================================
        // COMPANY PROFILE
        // =====================================================

        composable(
            route = "company_profile"
        ) {

            SessionGuard(
                onSessionExpired = {

                    navController.navigate(
                        "login"
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                CompanyProfileScreen(
                    repository =
                        companyProfileRepository,

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }



        // =====================================================
        // AUDIT TRAIL
        // =====================================================

        composable(
            route = "audit_trail"
        ) {

            SessionGuard(
                onSessionExpired = {

                    navController.navigate(
                        "login"
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                val auditTrailViewModel:
                        AuditTrailViewModel =
                    viewModel(
                        factory =
                            AuditTrailViewModelFactory(

                                auditTrailRepository =
                                    auditTrailRepository
                            )
                    )


                AuditTrailScreen(

                    viewModel =
                        auditTrailViewModel,

                    onBack = {

                        navController.popBackStack()
                    }
                )
            }
        }


        // =====================================================
        // FINANCIAL YEAR SETTINGS
        // =====================================================

        composable(
            route = "financial_year_settings"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                FinancialYearSettingsScreen(
                    viewModel =
                        financialYearViewModel,

                    onBack = {
                        navController.popBackStack()

                    }
                )

            }
        }


        // =====================================================
        // MASTER
        // =====================================================

        composable(
            route = "master"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                MasterScreen(

                    onProductMasterClick = {

                        navController.navigate(
                            "product_list"
                        )
                    },

                    onPartyMasterClick = {

                        navController.navigate(
                            "party_list"
                        )
                    }
                )

            }
        }



        // =====================================================
        // PRODUCT LIST
        // =====================================================

        composable(
            route = "product_list"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val productListViewModel:
                        ProductListViewModel =
                    viewModel()


                ProductListScreen(

                    viewModel =
                        productListViewModel,

                    onAddProductClick = {

                        navController.navigate(
                            "product_master"
                        )
                    },

                    onEditProductClick = { productId ->

                        navController.navigate(
                            "product_master_edit/$productId"
                        )
                    }
                )


            }
        }


        // =====================================================
        // PRODUCT MASTER / ADD PRODUCT
        // =====================================================

        composable(
            route = "product_master"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val database =
                    DatabaseProvider.getDatabase(
                        context = context
                    )

                val productDao =
                    database.productDao()

                val productMasterRepository =
                    ProductMasterRepository(
                        productDao = productDao
                    )

                val productMasterViewModel:
                        ProductMasterViewModel =
                    viewModel(
                        factory =
                            ProductMasterViewModelFactory(
                                repository =
                                    productMasterRepository,
                                partyRepository =
                                    purchasePartyRepository
                            )

                    )


                ProductMasterScreen(

                    viewModel =
                        productMasterViewModel,

                    onProductSaved = {

                        navController
                            .popBackStack()
                    },

                    onProductDeleted = {

                        navController
                            .popBackStack()
                    }
                )

            }
        }


        // =====================================================
        // PRODUCT MASTER / EDIT PRODUCT
        // =====================================================

        composable(
            route =
                "product_master_edit/{productId}",

            arguments =
                listOf(
                    navArgument(
                        "productId"
                    ) {
                        type =
                            NavType.LongType
                    }
                )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {

                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val productId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "productId"
                        )
                        ?: 0L


                val database =
                    DatabaseProvider.getDatabase(
                        context = context
                    )

                val productDao =
                    database.productDao()

                val productMasterRepository =
                    ProductMasterRepository(
                        productDao =
                            productDao
                    )

                val productMasterViewModel:
                        ProductMasterViewModel =
                    viewModel(
                        key =
                            "product_master_edit_$productId",

                        factory =
                            ProductMasterViewModelFactory(
                                repository =
                                    productMasterRepository,
                                partyRepository =
                                    purchasePartyRepository
                            )
                    )



                LaunchedEffect(
                    productId
                ) {

                    productMasterViewModel
                        .loadProduct(
                            productId
                        )
                }


                ProductMasterScreen(

                    viewModel =
                        productMasterViewModel,

                    onProductSaved = {

                        navController
                            .popBackStack()
                    },

                    onProductDeleted = {

                        navController
                            .popBackStack()
                    }
                )

            }
        }


        // =====================================================
        // PARTY LIST
        // =====================================================

        composable(
            route = "party_list"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {

                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val database =
                    DatabaseProvider.getDatabase(
                        context = context
                    )

                val partyDao =
                    database.partyDao()

                val partyRepository =
                    PartyRepository(
                        partyDao = partyDao
                    )

                val partyListViewModel:
                        PartyListViewModel =
                    viewModel(
                        factory =
                            PartyListViewModelFactory(
                                repository =
                                    partyRepository
                            )
                    )


                PartyListScreen(

                    viewModel =
                        partyListViewModel,

                    onAddParty = {

                        navController.navigate(
                            "party_master"
                        )
                    },


                    onEditParty = { partyId ->

                        navController.navigate(
                            "party_master_edit/$partyId"
                        )
                    }
                )

            }
        }


        // =====================================================
        // PARTY MASTER / ADD PARTY
        // =====================================================

        composable(
            route = "party_master"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val database =
                    DatabaseProvider.getDatabase(
                        context = context
                    )

                val partyDao =
                    database.partyDao()

                val partyRepository =
                    PartyRepository(
                        partyDao = partyDao
                    )



                // =================================================
                // GSTIN LOOKUP
                // =================================================

                val gstinLookupService =
                    GstinLookupService(
                        apiKey =
                            BuildConfig.GST_API_KEY
                    )

                val gstinLookupRepository =
                    GstinLookupRepository(
                        service =
                            gstinLookupService
                    )


                val partyMasterViewModel:
                        PartyMasterViewModel =
                    viewModel(
                        factory =
                            PartyMasterViewModelFactory(

                                repository =
                                    partyRepository,

                                gstinLookupRepository =
                                    gstinLookupRepository
                            )
                    )


                PartyMasterScreen(

                    viewModel =
                        partyMasterViewModel,

                    onPartySaved = {

                        navController
                            .popBackStack()
                    },

                    onPartyDeleted = {


                        navController
                            .popBackStack()
                    },

                    onSearchGstin = {

                        partyMasterViewModel
                            .startGstinLookup()
                    }
                )

            }
        }


        // =====================================================
        // PARTY MASTER / EDIT PARTY
        // =====================================================

        composable(
            route =
                "party_master_edit/{partyId}",

            arguments =
                listOf(
                    navArgument(
                        "partyId"
                    ) {
                        type =
                            NavType.LongType
                    }
                )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {



                val partyId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "partyId"
                        )
                        ?: 0L


                val database =
                    DatabaseProvider.getDatabase(
                        context = context
                    )

                val partyDao =
                    database.partyDao()

                val partyRepository =
                    PartyRepository(
                        partyDao = partyDao
                    )


                // =================================================
                // GSTIN LOOKUP
                // =================================================

                val gstinLookupService =
                    GstinLookupService(
                        apiKey =
                            BuildConfig.GST_API_KEY
                    )

                val gstinLookupRepository =
                    GstinLookupRepository(
                        service =
                            gstinLookupService
                    )


                val partyMasterViewModel:
                        PartyMasterViewModel =
                    viewModel(
                        key =
                            "party_master_edit_$partyId",


                        factory =
                            PartyMasterViewModelFactory(

                                repository =
                                    partyRepository,

                                gstinLookupRepository =
                                    gstinLookupRepository
                            )
                    )


                LaunchedEffect(
                    partyId
                ) {

                    partyMasterViewModel
                        .loadParty(
                            partyId
                        )
                }


                PartyMasterScreen(

                    viewModel =
                        partyMasterViewModel,

                    onPartySaved = {

                        navController
                            .popBackStack()
                    },

                    onPartyDeleted = {

                        navController
                            .popBackStack()
                    },

                    onSearchGstin = {

                        partyMasterViewModel
                            .startGstinLookup()

                    }
                )

            }
        }


        // =====================================================
        // PURCHASE REGISTER
        // =====================================================

        composable(
            route = "purchase_register"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val purchaseRegisterViewModel:
                        PurchaseRegisterViewModel =
                    viewModel(
                        factory =
                            PurchaseRegisterViewModelFactory(
                                purchaseRepository =
                                    purchaseRepository,
                                purchaseReturnRepository =
                                    purchaseReturnRepository
                            )
                    )

                PurchaseRegisterScreen(
                    viewModel =
                        purchaseRegisterViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },

                    onNewPurchase = {


                        purchaseViewModel
                            .startNewPurchase()

                        navController.navigate(
                            "purchase"
                        )
                    },

                    onPurchaseClick = { purchaseId ->

                        navController.navigate(
                            "purchase_detail/$purchaseId"
                        )
                    },

                    onEditPurchase = { purchaseId ->

                        purchaseViewModel
                            .loadPurchaseForEdit(
                                purchaseId
                            )

                        navController.navigate(
                            "purchase"
                        )
                    }
                )

            }
        }


        // =====================================================
        // PURCHASE DETAIL
        // =====================================================

        composable(
            route = "purchase_detail/{purchaseId}",
            arguments =
                listOf(
                    navArgument(
                        "purchaseId"
                    ) {
                        type =
                            NavType.LongType
                    }
                )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }

                }
            ) {


                val purchaseId =
                    backStackEntry
                        .arguments
                        ?.getLong(
                            "purchaseId"
                        )
                        ?: 0L

                val productDao =
                    purchaseDatabase.productDao()

                val purchaseProductRepository =
                    ProductMasterRepository(
                        productDao = productDao
                    )

                val purchaseDetailViewModel:
                        PurchaseDetailViewModel =
                    viewModel(
                        key =
                            "purchase_detail_$purchaseId",
                        factory =
                            PurchaseDetailViewModelFactory(
                                purchaseId =
                                    purchaseId,
                                purchaseRepository =
                                    purchaseRepository,
                                productRepository =
                                    purchaseProductRepository
                            )
                    )

                PurchaseDetailScreen(
                    viewModel =
                        purchaseDetailViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },

                    onEditPurchase = { editPurchaseId ->


                        purchaseViewModel
                            .loadPurchaseForEdit(
                                editPurchaseId
                            )

                        navController.navigate(
                            "purchase"
                        )
                    }
                )

            }
        }


        // =====================================================
        // PURCHASE RETURN SAVED REGISTER
        // =====================================================

        composable(route = "purchase_return_register") {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                val savedRegisterViewModel: PurchaseReturnSavedRegisterViewModel =
                    viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                if (modelClass.isAssignableFrom(PurchaseReturnSavedRegisterViewModel::class.java)) {
                                    @Suppress("UNCHECKED_CAST")
                                    return PurchaseReturnSavedRegisterViewModel(
                                        repository = purchaseReturnRepository
                                    ) as T
                                }
                                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                            }
                        }
                    )

                PurchaseReturnSavedRegisterScreen(
                    viewModel = savedRegisterViewModel,
                    onBack = { navController.popBackStack() },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onReturnClick = { returnId ->
                        navController.navigate("purchase_return_detail/$returnId")
                    },
                    onEditReturn = { returnId ->
                        navController.navigate("purchase_return_edit/$returnId")
                    }
                )
            }
        }

        // =====================================================
        // PURCHASE RETURN DETAIL
        // =====================================================

        composable(
            route = "purchase_return_detail/{returnId}",
            arguments = listOf(
                navArgument("returnId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                val returnId = backStackEntry.arguments?.getLong("returnId") ?: 0L
                val returnDetailViewModel: PurchaseReturnDetailViewModel = viewModel(
                    key = "purchase_return_detail_$returnId",
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(PurchaseReturnDetailViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return PurchaseReturnDetailViewModel(
                                    repository = purchaseReturnRepository
                                ) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                        }
                    }
                )

                LaunchedEffect(returnId) {
                    returnDetailViewModel.loadPurchaseReturn(returnId)
                }

                PurchaseReturnDetailScreen(
                    viewModel = returnDetailViewModel,
                    onBack = { navController.popBackStack() },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onEdit = { editReturnId ->
                        navController.navigate("purchase_return_edit/$editReturnId")
                    }
                )
            }
        }

        // =====================================================
        // PURCHASE RETURN EDIT
        // =====================================================

        composable(
            route = "purchase_return_edit/{returnId}",
            arguments = listOf(
                navArgument("returnId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                val returnId = backStackEntry.arguments?.getLong("returnId") ?: 0L

                val purchaseReturnViewModel: PurchaseReturnViewModel = viewModel(
                    key = "purchase_return_edit_$returnId",
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(PurchaseReturnViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return PurchaseReturnViewModel(
                                    purchaseReturnRepository = purchaseReturnRepository,
                                    productRepository = purchaseProductRepository
                                ) as T
                            }
                            throw IllegalArgumentException(
                                "Unknown ViewModel class: ${modelClass.name}"
                            )
                        }
                    }
                )

                LaunchedEffect(returnId) {
                    purchaseReturnViewModel.loadPurchaseReturnForEdit(returnId)
                }

                PurchaseReturnScreen(
                    viewModel = purchaseReturnViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        // =====================================================
        // PURCHASE RETURN SERIAL SEARCH / NEW RETURN
        // =====================================================

        composable(
            route = "purchase_return_search"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {
                val registerViewModel: PurchaseReturnRegisterViewModel =
                    viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(
                                modelClass: Class<T>
                            ): T {
                                if (modelClass.isAssignableFrom(PurchaseReturnRegisterViewModel::class.java)) {
                                    @Suppress("UNCHECKED_CAST")
                                    return PurchaseReturnRegisterViewModel(
                                        repository = purchaseReturnRepository
                                    ) as T
                                }

                                throw IllegalArgumentException(
                                    "Unknown ViewModel class: ${modelClass.name}"
                                )
                            }
                        }
                    )

                PurchaseReturnRegisterScreen(
                    viewModel = registerViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onCreateReturn = { purchaseId ->
                        navController.navigate("purchase_return/$purchaseId")
                    }
                )
            }
        }



        // =====================================================
        // SALES HOME
        // =====================================================

        composable(route = "sales_home") {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                SalesHomeScreen(
                    onBack = { navController.popBackStack() },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onInvoice = { navController.navigate("sales_invoice_home") },
                    onChallan = { navController.navigate("sales_challan_home") },
                    onCreditNote = { navController.navigate("sales_credit_note_home") },
                    onProformaInvoice = { navController.navigate("sales_proforma_home") },
                    onSampleIssue = { navController.navigate("sales_sample_home") }
                )
            }
        }


        // =====================================================
        // SALES - INVOICE HOME
        // =====================================================

        composable(route = "sales_invoice_home") {
            SalesDocumentCategoryScreen(
                title = "Invoice",
                subtitle = "Create sales invoices and view saved invoices",
                newTitle = "New Invoice",
                newSubtitle = "Create a new sales invoice",
                registerTitle = "Invoice Register",
                registerSubtitle = "View and export saved invoices",
                newSymbol = "+",
                newBackground = Color(0xFFDCE8FA),
                registerBackground = Color(0xFFE8EEF8),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { navController.navigate("sales") },
                onRegister = { navController.navigate("sales_register_invoice") }
            )
        }


        // =====================================================
        // SALES - CHALLAN HOME
        // =====================================================

        composable(route = "sales_challan_home") {
            SalesDocumentCategoryScreen(
                title = "Challan",
                subtitle = "Create challans and view saved challans",
                newTitle = "New Challan",
                newSubtitle = "Create a new delivery challan",
                registerTitle = "Challan Register",
                registerSubtitle = "View and export saved challans",
                newSymbol = "+",
                newBackground = Color(0xFFDDF3E6),
                registerBackground = Color(0xFFE8F5ED),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { navController.navigate("sales_new_challan") },
                onRegister = { navController.navigate("sales_register_challan") }
            )
        }


        // =====================================================
        // SALES - CREDIT NOTE HOME
        // =====================================================

        composable(route = "sales_credit_note_home") {
            SalesDocumentCategoryScreen(
                title = "Credit Note",
                subtitle = "Manage sales credit notes",
                newTitle = "New Credit Note",
                newSubtitle = "Create a new sales credit note",
                registerTitle = "Credit Note Register",
                registerSubtitle = "View and export saved credit notes",
                newSymbol = "+",
                newBackground = Color(0xFFFFE4C7),
                registerBackground = Color(0xFFFFEDDA),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { navController.navigate("sales_new_credit_note") },
                onRegister = { navController.navigate("sales_register_credit_note") }
            )
        }


        // =====================================================
        // NEW CREDIT NOTE
        // =====================================================

        composable(route = "sales_new_credit_note") {
            val creditNoteViewModel: NewCreditNoteViewModel =
                viewModel(
                    factory = NewCreditNoteViewModelFactory(
                        salesRepository = salesRepository,
                        creditNoteRepository = salesCreditNoteRepository
                    )
                )

            NewCreditNoteScreen(
                viewModel = creditNoteViewModel,
                onBack = { navController.popBackStack() },
                onOpenRegister = { navController.navigate("sales_register_credit_note") }
            )
        }


        // =====================================================
        // SALES - PROFORMA INVOICE HOME
        // =====================================================

        composable(route = "sales_proforma_home") {
            SalesDocumentCategoryScreen(
                title = "Proforma Invoice",
                subtitle = "Manage proforma invoices",
                newTitle = "New Proforma Invoice",
                newSubtitle = "Create a new proforma invoice",
                registerTitle = "Proforma Invoice Register",
                registerSubtitle = "View and export saved proforma invoices",
                newSymbol = "+",
                newBackground = Color(0xFFECE2FA),
                registerBackground = Color(0xFFF2EBFA),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { },
                onRegister = { navController.navigate("sales_register_proforma") }
            )
        }


        // =====================================================
        // SALES - SAMPLE ISSUE HOME
        // =====================================================

        composable(route = "sales_sample_home") {
            SalesDocumentCategoryScreen(
                title = "Sample Issue",
                subtitle = "Manage sample issue transactions",
                newTitle = "New Sample Issue",
                newSubtitle = "Issue products as samples",
                registerTitle = "Sample Issue Register",
                registerSubtitle = "View and export saved sample issues",
                newSymbol = "+",
                newBackground = Color(0xFFFFF1C9),
                registerBackground = Color(0xFFFFF6DC),
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onNewDocument = { },
                onRegister = { navController.navigate("sales_register_sample") }
            )
        }


        // =====================================================
        // NEW CHALLAN
        // =====================================================

        composable(route = "sales_new_challan") {
            val challanViewModel: NewChallanViewModel =
                viewModel(
                    factory = NewChallanViewModelFactory(
                        challanRepository = challanRepository,
                        inventoryRepository = salesInventoryRepository,
                        partyRepository = purchasePartyRepository,
                        productRepository = purchaseProductRepository
                    )
                )

            NewChallanScreen(
                viewModel = challanViewModel,
                onBack = { navController.popBackStack() }
            )
        }


        // =====================================================
        // SALES REGISTERS
        // =====================================================

        composable(route = "sales_register_invoice") {
            val registerViewModel: SalesRegisterViewModel =
                viewModel(
                    factory = SalesRegisterViewModelFactory(
                        database = purchaseDatabase,
                        registerType = SalesRegisterType.INVOICE
                    )
                )
            SalesRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") },
                onViewInvoice = { saleId ->
                    navController.navigate("sales_invoice_detail/$saleId")
                },
                onEditInvoice = { saleId ->
                    navController.navigate("sales_edit/$saleId")
                }
            )
        }

        composable(route = "sales_register_challan") {
            val registerViewModel: SalesRegisterViewModel =
                viewModel(
                    factory = SalesRegisterViewModelFactory(
                        database = purchaseDatabase,
                        registerType = SalesRegisterType.CHALLAN
                    )
                )
            SalesRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "sales_register_credit_note") {
            val registerViewModel: SalesRegisterViewModel =
                viewModel(
                    factory = SalesRegisterViewModelFactory(
                        database = purchaseDatabase,
                        registerType = SalesRegisterType.CREDIT_NOTE
                    )
                )
            SalesRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "sales_register_proforma") {
            val registerViewModel: SalesRegisterViewModel =
                viewModel(
                    factory = SalesRegisterViewModelFactory(
                        database = purchaseDatabase,
                        registerType = SalesRegisterType.PROFORMA
                    )
                )
            SalesRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }

        composable(route = "sales_register_sample") {
            val registerViewModel: SalesRegisterViewModel =
                viewModel(
                    factory = SalesRegisterViewModelFactory(
                        database = purchaseDatabase,
                        registerType = SalesRegisterType.SAMPLE_ISSUE
                    )
                )
            SalesRegisterScreen(
                viewModel = registerViewModel,
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate("dashboard") }
            )
        }


        // =====================================================
        // SALES INVOICE DETAIL
        // =====================================================

        composable(
            route = "sales_invoice_detail/{saleId}",
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val saleId =
                backStackEntry.arguments
                    ?.getLong("saleId")
                    ?: 0L

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                val salesInvoiceDetailViewModel:
                        SalesInvoiceDetailViewModel =
                    viewModel(
                        key = "sales_invoice_detail_$saleId",
                        factory =
                            SalesInvoiceDetailViewModelFactory(
                                saleId = saleId,
                                repository = salesRepository,
                                inventoryRepository = salesInventoryRepository,
                                productRepository = salesProductRepository,
                                companyProfileDao =
                                    purchaseDatabase.companyProfileDao()
                            )
                    )

                SalesInvoiceDetailScreen(
                    viewModel = salesInvoiceDetailViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // SALES ENTRY - EDIT EXISTING INVOICE
        // =====================================================


        composable(
            route = "sales_edit/{saleId}",
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val saleId =
                backStackEntry.arguments
                    ?.getLong("saleId")
                    ?: 0L

            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {

                val salesViewModel: SalesViewModel =
                    viewModel(
                        key = "sales_edit_$saleId",
                        factory =
                            SalesViewModelFactory(
                                salesRepository = salesRepository,
                                partyRepository = purchasePartyRepository,
                                productRepository = salesProductRepository,
                                inventoryRepository = salesInventoryRepository,
                                challanRepository = challanRepository,
                                editSaleId = saleId
                            )
                    )

                SalesEntryScreen(
                    viewModel = salesViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // SALES ENTRY
        // =====================================================

        composable(
            route = "sales"
        ) {

            SessionGuard(
                onSessionExpired = {

                    navController.navigate(
                        "login"
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            ) {

                val salesViewModel:
                        SalesViewModel =
                    viewModel(
                        factory =
                            SalesViewModelFactory(
                                salesRepository =
                                    salesRepository,

                                partyRepository =
                                    purchasePartyRepository,

                                productRepository =
                                    salesProductRepository,

                                inventoryRepository =
                                    salesInventoryRepository,

                                challanRepository =
                                    challanRepository
                            )
                    )


                SalesEntryScreen(
                    viewModel =
                        salesViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {

                        navController.navigate(
                            "dashboard"
                        ) {

                            popUpTo(
                                "dashboard"
                            ) {
                                inclusive = false
                            }

                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // PURCHASE HOME
        // =====================================================

        composable(route = "purchase_home") {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                PurchaseHomeScreen(
                    onBack = { navController.popBackStack() },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNewPurchase = {
                        purchaseViewModel.startNewPurchase()
                        navController.navigate("purchase")
                    },
                    onPurchaseRegister = {
                        navController.navigate("purchase_register")
                    },
                    onPurchaseReturn = {
                        navController.navigate("purchase_return_search")
                    },
                    onPurchaseReturnRegister = {
                        navController.navigate("purchase_return_register")
                    }
                )
            }
        }

        // =====================================================
        // PURCHASE ENTRY
        // =====================================================

        composable(
            route = "purchase"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                PurchaseEntryScreen(

                    viewModel =
                        purchaseViewModel,

                    onAddProductClick = {

                        navController.navigate(
                            "add_purchase_item"
                        )

                    },

                    onEditProductClick = { itemIndex ->

                        navController.navigate(
                            "edit_purchase_item/$itemIndex"
                        )
                    },

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )

            }
        }


        // =====================================================
        // PURCHASE RETURN ENTRY
        // =====================================================

        composable(
            route = "purchase_return/{purchaseId}",
            arguments = listOf(
                navArgument("purchaseId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {
                val purchaseId =
                    backStackEntry.arguments?.getLong("purchaseId") ?: 0L

                val purchaseReturnViewModel: PurchaseReturnViewModel =
                    viewModel(
                        key = "purchase_return_$purchaseId",
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(
                                modelClass: Class<T>
                            ): T {
                                if (modelClass.isAssignableFrom(PurchaseReturnViewModel::class.java)) {
                                    @Suppress("UNCHECKED_CAST")
                                    return PurchaseReturnViewModel(
                                        purchaseReturnRepository = purchaseReturnRepository,
                                        productRepository = purchaseProductRepository
                                    ) as T
                                }

                                throw IllegalArgumentException(
                                    "Unknown ViewModel class: ${modelClass.name}"
                                )
                            }
                        }
                    )

                LaunchedEffect(purchaseId) {
                    purchaseReturnViewModel.loadOriginalPurchase(purchaseId)
                }

                PurchaseReturnScreen(
                    viewModel = purchaseReturnViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }


        // =====================================================
        // ADD PURCHASE ITEM
        // =====================================================

        composable(
            route = "add_purchase_item"
        ) {
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            ) {


                val productDao =
                    purchaseDatabase.productDao()

                val purchaseProductRepository =
                    ProductMasterRepository(
                        productDao = productDao
                    )

                val addPurchaseItemViewModel:
                        AddPurchaseItemViewModel =
                    viewModel(
                        factory =

                            AddPurchaseItemViewModelFactory(
                                productRepository =
                                    purchaseProductRepository,

                                purchaseRepository =
                                    purchaseRepository
                            )
                    )


                AddPurchaseItemScreen(

                    viewModel =
                        addPurchaseItemViewModel,

                    onAddItem = {

                        val itemState =
                            addPurchaseItemViewModel
                                .uiState
                                .value


                        // =========================================
                        // IOL / SERIAL TRACKED QUANTITY
                        // =========================================

                        val isIol =
                            itemState.category.equals(
                                "IOL",
                                ignoreCase = true
                            )


                        val finalQuantity =
                            if (isIol) {

                                /*
                                 * One physical IOL =
                                 * one lensDetails row.
                                 *
                                 * Therefore quantity is controlled
                                 * by the number of physical lenses.
                                 */
                                itemState

                                    .lensDetails
                                    .size

                            } else {

                                itemState
                                    .quantity
                                    .toIntOrNull()
                                    ?: 0
                            }


                        // =========================================
                        // CREATE PURCHASE ITEM
                        // =========================================

                        val purchaseItem =
                            PurchaseItem(

                                productId =
                                    itemState
                                        .productId,

                                productName =
                                    itemState
                                        .productName
                                        .trim(),

                                model =
                                    itemState
                                        .model
                                        .trim(),

                                category =
                                    itemState
                                        .category
                                        .trim(),

                                hsnCode =
                                    itemState
                                        .hsnCode
                                        .trim(),

                                power =
                                    itemState

                                        .power
                                        .trim(),

                                quantity =
                                    finalQuantity,

                                purchaseRate =
                                    itemState
                                        .purchaseRate
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                discountPercent =
                                    itemState
                                        .discountPercent
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                gstPercent =
                                    itemState
                                        .gstPercent
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                batchNumber =
                                    itemState
                                        .batchNumber
                                        .trim(),

                                lensDetails =
                                    itemState
                                        .lensDetails
                                        .map { lensDetail ->

                                            lensDetail.copy(

                                                serialNumber =
                                                    lensDetail
                                                        .serialNumber
                                                        .trim(),

                                                expiryDate =
                                                    lensDetail
                                                        .expiryDate
                                                        .trim()

                                            )
                                        }
                            )


                        val itemAdded =
                            purchaseViewModel
                                .addItemSafely(
                                    purchaseItem
                                )


                        if (itemAdded) {

                            navController
                                .popBackStack()
                        }
                    },

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )

            }
        }


        // =====================================================
        // EDIT PURCHASE ITEM
        // =====================================================

        composable(
            route = "edit_purchase_item/{itemIndex}",
            arguments = listOf(
                navArgument("itemIndex") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            SessionGuard(
                onSessionExpired = {
                    navController.navigate("login") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

            ) {


                val itemIndex =
                    backStackEntry.arguments
                        ?.getInt("itemIndex")
                        ?: -1

                val existingItem =
                    purchaseViewModel.uiState.value.items
                        .getOrNull(itemIndex)

                val productDao =
                    purchaseDatabase.productDao()

                val purchaseProductRepository =
                    ProductMasterRepository(
                        productDao = productDao
                    )

                val editPurchaseItemViewModel:
                        AddPurchaseItemViewModel =
                    viewModel(
                        key = "edit_purchase_item_$itemIndex",
                        factory =
                            AddPurchaseItemViewModelFactory(
                                productRepository =
                                    purchaseProductRepository,

                                purchaseRepository =
                                    purchaseRepository
                            )
                    )

                LaunchedEffect(
                    itemIndex,
                    existingItem
                ) {
                    if (existingItem != null) {
                        editPurchaseItemViewModel
                            .loadPurchaseItem(existingItem)
                    }
                }

                AddPurchaseItemScreen(

                    viewModel =
                        editPurchaseItemViewModel,

                    onAddItem = {

                        val itemState =
                            editPurchaseItemViewModel
                                .uiState
                                .value

                        val isIol =
                            itemState.category.equals(
                                "IOL",
                                ignoreCase = true
                            )

                        val finalQuantity =
                            if (isIol) {
                                itemState.lensDetails.size
                            } else {
                                itemState.quantity
                                    .toIntOrNull()
                                    ?: 0
                            }

                        val updatedPurchaseItem =
                            PurchaseItem(
                                productId =
                                    itemState.productId,

                                productName =
                                    itemState.productName.trim(),

                                model =
                                    itemState.model.trim(),

                                category =
                                    itemState.category.trim(),

                                hsnCode =
                                    itemState.hsnCode.trim(),

                                power =
                                    itemState.power.trim(),


                                quantity =
                                    finalQuantity,

                                purchaseRate =
                                    itemState.purchaseRate
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                discountPercent =
                                    itemState.discountPercent
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                gstPercent =
                                    itemState.gstPercent
                                        .toDoubleOrNull()
                                        ?: 0.0,

                                batchNumber =
                                    itemState.batchNumber.trim(),

                                lensDetails =
                                    itemState.lensDetails
                                        .map { lensDetail ->
                                            lensDetail.copy(
                                                serialNumber =
                                                    lensDetail.serialNumber.trim(),
                                                expiryDate =
                                                    lensDetail.expiryDate.trim()
                                            )
                                        }
                            )

                        val itemUpdated =
                            purchaseViewModel
                                .updateItemSafely(
                                    index = itemIndex,
                                    item = updatedPurchaseItem
                                )

                        if (itemUpdated) {
                            navController.popBackStack()
                        }
                    },

                    onBack = {
                        navController.popBackStack()
                    },

                    onDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )


            }
        }

    }
}
