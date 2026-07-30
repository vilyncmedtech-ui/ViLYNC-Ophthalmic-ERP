package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sample_issues",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["sampleIssueNumber"]),
        Index(value = ["normalizedSampleIssueNumber", "financialYearStart"], unique = true),
        Index(value = ["financialYearStart"]),
        Index(value = ["status"])
    ]
)
data class SampleIssueEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // CUSTOMER / HOSPITAL / PARTY
    // =========================================================

    val customerId: Long,

    val customerName: String,

    val customerGstin: String = "",

    val customerAddress: String = "",

    val customerState: String = "",

    // Optional doctor / recipient snapshot.
    val recipientName: String = "",

    // =========================================================
    // DOCUMENT DETAILS
    // =========================================================

    val sampleIssueNumber: String,

    val normalizedSampleIssueNumber: String,

    val sampleIssueDate: String,

    val financialYearStart: Int,

    // =========================================================
    // SAMPLE POLICY
    // =========================================================
    //
    // RETURNABLE
    // NON_RETURNABLE
    //
    // Physical inventory is still traceable in either case.
    // =========================================================

    val sampleType: String = "NON_RETURNABLE",

    val expectedReturnDate: String = "",

    val remarks: String = "",

    // =========================================================
    // DOCUMENT LIFECYCLE
    // =========================================================
    //
    // Expected examples:
    // ISSUED
    // PARTIALLY_RETURNED
    // RETURNED
    // CLOSED
    // CANCELLED
    // =========================================================

    val status: String = "ISSUED",

    val cancelledAt: Long? = null,

    val cancellationReason: String = "",

    // =========================================================
    // AUDIT
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)
