package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challan_items",

    foreignKeys = [
        ForeignKey(
            entity = ChallanEntity::class,
            parentColumns = ["id"],
            childColumns = ["challanId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = InventoryUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryUnitId"],
            onDelete = ForeignKey.NO_ACTION
        ),

        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],

    indices = [
        Index(value = ["challanId"]),
        Index(value = ["inventoryUnitId"], unique = true),
        Index(value = ["productId"]),
        Index(value = ["serialNumber"]),
        Index(value = ["settlementStatus"]),
        Index(value = ["saleId"])
    ]
)
data class ChallanItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // CHALLAN REFERENCE
    // =========================================================

    val challanId: Long,


    // =========================================================
    // PHYSICAL INVENTORY UNIT
    // =========================================================

    /*
     * Authoritative identity of the physical lens/unit.
     *
     * Never identify a physical lens only by Product + Power.
     */
    val inventoryUnitId: Long,


    // =========================================================
    // PRODUCT SNAPSHOT
    // =========================================================

    val productId: Long,

    /*
     * Product name snapshot retained so historical Challan
     * remains readable even if Product Master changes later.
     */
    val productName: String,


    // =========================================================
    // LENS TRACEABILITY SNAPSHOTS
    // =========================================================

    val serialNumber: String,

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",


    // =========================================================
    // COMMERCIAL SNAPSHOT
    // =========================================================

    /*
     * Default invoice rate suggested when this Challan lens
     * is later selected for settlement.
     *
     * Final invoice rate can still be controlled by Sales UI.
     */
    val rate: Double = 0.0,

    val gstPercent: Double = 0.0,


    // =========================================================
    // SETTLEMENT
    // =========================================================

    /*
     * Expected values:
     *
     * PENDING
     * INVOICED
     *
     * IMPORTANT:
     * Selecting a lens on New Sales Invoice does NOT change
     * this field immediately.
     *
     * PENDING -> INVOICED happens only after the Sales Invoice
     * has been successfully posted inside the database
     * transaction.
     */
    val settlementStatus: String = "PENDING",

    /*
     * Sales header ID after successful settlement.
     *
     * null = not yet invoiced.
     */
    val saleId: Long? = null,

    /*
     * Time at which this physical Challan item was successfully
     * settled through a Sales Invoice.
     */
    val settledAt: Long? = null,


    // =========================================================
    // AUDIT TIMESTAMPS
    // =========================================================

    val createdAt: Long,

    val updatedAt: Long
)