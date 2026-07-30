package com.vilync.ophthalmicerp.core.financialyear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Global Financial Year Manager for the complete ERP.
 *
 * Indian Financial Year:
 * 1 April -> 31 March
 *
 * Responsibilities:
 *
 * 1. Maintain the currently selected ERP Working Financial Year.
 * 2. Persist the selected FY on the device.
 * 3. Restore the selected FY when the app starts again.
 * 4. Determine FY from transaction dates.
 * 5. Prevent future Financial Years from becoming active.
 *
 * IMPORTANT:
 *
 * Only the selected FY identifier is stored in Preferences.
 * Purchase, Sales, Inventory, Ledger and other transaction
 * data remain in the Room Database.
 */
object FinancialYearManager {

    // =========================================================
    // PREFERENCES
    // =========================================================

    private var financialYearPreferences:
            FinancialYearPreferences? = null


    // =========================================================
    // ACTIVE FINANCIAL YEAR
    // =========================================================

    private val _activeFinancialYear =
        MutableStateFlow(
            FinancialYear.current()
        )

    val activeFinancialYear: StateFlow<FinancialYear> =
        _activeFinancialYear.asStateFlow()


    // =========================================================
    // INITIALIZE
    // =========================================================

    /**
     * Must be called once when Financial Year system starts.
     *
     * If a saved Working FY exists, it is restored.
     *
     * If nothing has been saved yet, the current Indian
     * Financial Year remains active.
     */
    fun initialize(
        context: Context
    ) {

        if (financialYearPreferences != null) {
            return
        }

        val preferences =
            FinancialYearPreferences(
                context = context.applicationContext
            )

        financialYearPreferences =
            preferences

        val currentFinancialYear =
            FinancialYear.current()

        val savedFinancialYear =
            preferences.loadActiveFinancialYear()


        // A future FY must never be restored as the working FY.
        val validSavedFinancialYear =
            savedFinancialYear?.takeIf {

                it.startYear <=
                        currentFinancialYear.startYear
            }


        if (validSavedFinancialYear != null) {

            _activeFinancialYear.value =
                validSavedFinancialYear

        } else {

            _activeFinancialYear.value =
                currentFinancialYear

            preferences.saveActiveFinancialYear(
                currentFinancialYear
            )
        }
    }


    // =========================================================
    // CHANGE ACTIVE FINANCIAL YEAR
    // =========================================================

    /**
     * Changes the ERP Working Financial Year.
     *
     * Future FY selection is intentionally blocked.
     */
    fun setActiveFinancialYear(
        financialYear: FinancialYear
    ) {

        val currentFinancialYear =
            FinancialYear.current()


        if (
            financialYear.startYear >
            currentFinancialYear.startYear
        ) {
            return
        }


        _activeFinancialYear.value =
            financialYear


        financialYearPreferences
            ?.saveActiveFinancialYear(
                financialYear
            )
    }


    // =========================================================
    // TRANSACTION DATE -> FINANCIAL YEAR
    // =========================================================

    /**
     * Transaction FY is determined from the actual
     * transaction date.
     *
     * Example:
     *
     * 31-03-2026 -> FY 2025-26
     * 01-04-2026 -> FY 2026-27
     */
    fun financialYearForDate(
        date: LocalDate
    ): FinancialYear {

        return FinancialYear.fromDate(
            date
        )
    }


    // =========================================================
    // CHECK DATE AGAINST ACTIVE FY
    // =========================================================

    fun belongsToActiveFinancialYear(
        date: LocalDate
    ): Boolean {

        return _activeFinancialYear
            .value
            .contains(date)
    }


    // =========================================================
    // AVAILABLE FINANCIAL YEARS
    // =========================================================

    /**
     * Current FY + previous FYs only.
     *
     * Future FYs are NOT shown.
     *
     * Temporary behaviour:
     *
     * If Current FY = 2026-27:
     *
     * 2026-27
     * 2025-26
     * 2024-25
     * 2023-24
     *
     * Later this list will be generated from actual
     * historical ERP transaction data, so old FYs will
     * appear only when relevant data exists.
     */
    fun availableFinancialYears(
        previousYears: Int = 3
    ): List<FinancialYear> {

        val current =
            FinancialYear.current()


        return (
                current.startYear downTo
                        (current.startYear - previousYears)
                ).map { startYear ->

                FinancialYear(
                    startYear = startYear,
                    endYear = startYear + 1
                )
            }
    }


    // =========================================================
    // RESET WORKING FY
    // =========================================================

    /**
     * Resets the Working FY to the current Indian FY.
     *
     * This does NOT delete any ERP transaction data.
     */
    fun resetToCurrentFinancialYear() {

        val current =
            FinancialYear.current()

        _activeFinancialYear.value =
            current

        financialYearPreferences
            ?.saveActiveFinancialYear(
                current
            )
    }
}