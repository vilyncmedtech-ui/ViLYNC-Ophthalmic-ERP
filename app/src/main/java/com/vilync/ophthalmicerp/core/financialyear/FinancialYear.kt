package com.vilync.ophthalmicerp.core.financialyear

import java.time.LocalDate

/**
 * Global Indian Financial Year model.
 *
 * Indian Financial Year:
 * 1 April -> 31 March
 *
 * Examples:
 *
 * 31-03-2026 -> FY 2025-26
 * 01-04-2026 -> FY 2026-27
 */
data class FinancialYear(
    val startYear: Int,
    val endYear: Int
) {

    init {
        require(endYear == startYear + 1) {
            "Financial Year endYear must be startYear + 1"
        }
    }

    /**
     * Example:
     * 2026-27
     */
    val displayName: String
        get() {
            val shortEndYear =
                (endYear % 100)
                    .toString()
                    .padStart(2, '0')

            return "$startYear-$shortEndYear"
        }


    /**
     * First date of FY.
     *
     * Example:
     * FY 2026-27 -> 01-04-2026
     */
    val startDate: LocalDate
        get() =
            LocalDate.of(
                startYear,
                4,
                1
            )


    /**
     * Last date of FY.
     *
     * Example:
     * FY 2026-27 -> 31-03-2027
     */
    val endDate: LocalDate
        get() =
            LocalDate.of(
                endYear,
                3,
                31
            )


    /**
     * Checks whether a transaction date belongs
     * to this Financial Year.
     */
    fun contains(
        date: LocalDate
    ): Boolean {

        return !date.isBefore(startDate) &&
                !date.isAfter(endDate)
    }


    companion object {

        /**
         * Calculates Indian FY from any date.
         *
         * Examples:
         *
         * 20-03-2026 -> 2025-26
         * 01-04-2026 -> 2026-27
         * 24-07-2026 -> 2026-27
         */
        fun fromDate(
            date: LocalDate
        ): FinancialYear {

            val startYear =
                if (date.monthValue >= 4) {
                    date.year
                } else {
                    date.year - 1
                }

            return FinancialYear(
                startYear = startYear,
                endYear = startYear + 1
            )
        }


        /**
         * Financial Year according to today's date.
         */
        fun current(): FinancialYear {

            return fromDate(
                LocalDate.now()
            )
        }
    }
}