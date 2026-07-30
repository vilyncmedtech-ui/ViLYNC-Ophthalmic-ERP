package com.vilync.ophthalmicerp.core.financialyear

import android.content.Context

/**
 * Stores only the user's selected ERP Working Financial Year.
 *
 * IMPORTANT:
 *
 * This does NOT store Purchase, Sales, Inventory,
 * Ledger, Payment or any transaction data.
 *
 * Example:
 *
 * Selected FY = 2025-26
 * Stored value = 2025
 *
 * Actual ERP transaction data remains in Room Database.
 */
class FinancialYearPreferences(
    context: Context
) {

    // =========================================================
    // SHARED PREFERENCES
    // =========================================================

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )


    // =========================================================
    // SAVE ACTIVE FINANCIAL YEAR
    // =========================================================

    fun saveActiveFinancialYear(
        financialYear: FinancialYear
    ) {

        preferences
            .edit()
            .putInt(
                KEY_ACTIVE_FY_START_YEAR,
                financialYear.startYear
            )
            .apply()
    }


    // =========================================================
    // LOAD ACTIVE FINANCIAL YEAR
    // =========================================================

    fun loadActiveFinancialYear(): FinancialYear? {

        if (
            !preferences.contains(
                KEY_ACTIVE_FY_START_YEAR
            )
        ) {
            return null
        }


        val startYear =
            preferences.getInt(
                KEY_ACTIVE_FY_START_YEAR,
                INVALID_YEAR
            )


        if (startYear == INVALID_YEAR) {
            return null
        }


        return FinancialYear(
            startYear = startYear,
            endYear = startYear + 1
        )
    }


    // =========================================================
    // CHECK IF ACTIVE FY IS SAVED
    // =========================================================

    fun hasSavedFinancialYear(): Boolean {

        return preferences.contains(
            KEY_ACTIVE_FY_START_YEAR
        )
    }


    // =========================================================
    // CLEAR SAVED ACTIVE FY
    // =========================================================

    fun clearActiveFinancialYear() {

        preferences
            .edit()
            .remove(
                KEY_ACTIVE_FY_START_YEAR
            )
            .apply()
    }


    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {

        private const val PREFERENCES_NAME =
            "vilync_erp_financial_year_preferences"

        private const val KEY_ACTIVE_FY_START_YEAR =
            "active_financial_year_start_year"

        private const val INVALID_YEAR =
            -1
    }
}