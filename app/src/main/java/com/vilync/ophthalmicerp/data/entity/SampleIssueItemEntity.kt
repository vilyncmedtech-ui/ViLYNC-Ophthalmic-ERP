package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sample_issue_items",
    foreignKeys = [
        ForeignKey(
            entity = SampleIssueEntity::class,
            parentColumns = ["id"],
            childColumns = ["sampleIssueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventoryUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryUnitId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["sampleIssueId"]),
        Index(value = ["inventoryUnitId"]),
        Index(value = ["productId"]),
        Index(value = ["serialNumber"]),
        Index(value = ["settlementStatus"])
    ]
)
data class SampleIssueItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val sampleIssueId: Long,

    // =========================================================
    // EXACT PHYSICAL INVENTORY UNIT
    // =========================================================

    val inventoryUnitId: Long,

    // =========================================================
    // PRODUCT / SERIAL SNAPSHOT
    // =========================================================

    val productId: Long,

    val productName: String,

    val power: String = "",

    val serialNumber: String,

    val batchNumber: String = "",

    val expiryDate: String = "",

    // Optional commercial value snapshot for reporting/export.
    // Issue Sample itself is not a Sales Invoice.
    val referenceRate: Double = 0.0,

    // =========================================================
    // PHYSICAL SAMPLE LIFECYCLE
    // =========================================================
    //
    // Expected:
    // ISSUED
    // RETURNED
    // CONSUMED
    //
    // Inventory repository transaction will control actual
    // InventoryUnitEntity.status changes.
    // =========================================================

    val settlementStatus: String = "ISSUED",

    val returnedAt: Long? = null,

    val consumedAt: Long? = null,

    // =========================================================
    // AUDIT
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)
