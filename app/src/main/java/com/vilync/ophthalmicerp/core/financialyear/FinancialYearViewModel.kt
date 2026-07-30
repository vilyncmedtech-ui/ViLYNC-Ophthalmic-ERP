package com.vilync.ophthalmicerp.core.financialyear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Global Financial Year ViewModel.
 *
 * Responsibilities:
 *
 * 1. Initialize the Financial Year system.
 * 2. Restore the saved Working Financial Year.
 * 3. Expose Active FY to the UI.
 * 4. Provide available Financial Years.
 * 5. Change and persist the selected Working FY.
 *
 * IMPORTANT:
 *
 * This ViewModel does not load yearly transaction data.
 * Purchase, Sales, Inventory and other transaction data
 * remain in the Room Database.
 */
class FinancialYearViewModel(
    application: Application
) : AndroidViewModel(application) {

    // =========================================================
    // INITIALIZATION
    // =========================================================

    init {

        FinancialYearManager.initialize(
            context = application
        )
    }


    // =========================================================
    // ACTIVE FINANCIAL YEAR
    // =========================================================

    val activeFinancialYear: StateFlow<FinancialYear> =
        FinancialYearManager.activeFinancialYear


    // =========================================================
    // AVAILABLE FINANCIAL YEARS
    // =========================================================

    val availableFinancialYears: List<FinancialYear>
        get() =
            FinancialYearManager
                .availableFinancialYears()


    // =========================================================
    // CHANGE ACTIVE FINANCIAL YEAR
    // =========================================================

    fun changeFinancialYear(
        financialYear: FinancialYear
    ) {

        FinancialYearManager
            .setActiveFinancialYear(
                financialYear
            )
    }


    // =========================================================
    // CHECK TRANSACTION DATE
    // =========================================================

    fun belongsToActiveFinancialYear(
        date: java.time.LocalDate
    ): Boolean {

        return FinancialYearManager
            .belongsToActiveFinancialYear(
                date
            )
    }


    // =========================================================
    // RESET TO CURRENT FY
    // =========================================================

    fun resetToCurrentFinancialYear() {

        FinancialYearManager
            .resetToCurrentFinancialYear()
    }
}