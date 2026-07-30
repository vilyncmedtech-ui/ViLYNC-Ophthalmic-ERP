package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challans",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["challanNumber"]),
        Index(value = ["normalizedChallanNumber"]),
        Index(value = ["financialYearStart"]),
        Index(value = ["status"])
    ]
)
data class ChallanEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // CUSTOMER / HOSPITAL
    // =========================================================

    // Stable Party Master reference.
    val customerId: Long,

    // Customer name snapshot retained for history.
    val customerName: String,


    // =========================================================
    // CHALLAN DETAILS
    // =========================================================

    val challanNumber: String,

    // Canonical key for duplicate protection/search.
    val normalizedChallanNumber: String,

    val challanDate: String,

    // Internal FY key.
    // Example:
    // FY 2026-27 -> 2026
    val financialYearStart: Int,


    // =========================================================
    // REMARKS
    // =========================================================

    val remarks: String = "",


    // =========================================================
    // DOCUMENT LIFECYCLE
    // =========================================================

    /*
     * Expected lifecycle:
     *
     * OPEN
     * PARTIALLY_SETTLED
     * SETTLED
     * CANCELLED
     */
    val status: String = "OPEN",

    val cancelledAt: Long? = null,

    val cancellationReason: String = "",


    // =========================================================
    // AUDIT TIMESTAMPS
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)