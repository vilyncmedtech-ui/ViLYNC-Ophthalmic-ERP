package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "purchase_return_items",

    foreignKeys = [

        ForeignKey(
            entity = PurchaseReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseReturnId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = PurchaseItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalPurchaseItemId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],

    indices = [

        Index(
            value = ["purchaseReturnId"]
        ),

        Index(
            value = ["originalPurchaseItemId"]
        ),

        Index(
            value = ["productId"]
        )
    ]
)
data class PurchaseReturnItemEntity(

    // =========================================================
    // PRIMARY KEY
    // =========================================================

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,


    // =========================================================
    // PURCHASE RETURN LINK
    // =========================================================

    /**
     * Purchase Return / Credit Note header ID.
     */
    val purchaseReturnId: Long,


    // =========================================================
    // ORIGINAL PURCHASE ITEM LINK
    // =========================================================

    /**
     * Exact original Purchase Item from which
     * this product is being returned.
     */
    val originalPurchaseItemId: Long,


    // =========================================================
    // PRODUCT SNAPSHOT
    // =========================================================

    /**
     * Product ID preserved for reporting,
     * searching and future traceability.
     */
    val productId: Long,

    /**
     * Product name snapshot at the time
     * of Purchase Return.
     */
    val productName: String,


    // =========================================================
    // RETURN QUANTITY
    // =========================================================

    /**
     * Total quantity being returned.
     *
     * For serial-controlled IOLs this should
     * correspond with the number of physical
     * lenses selected for return.
     */
    val quantity: Int,


    // =========================================================
    // COMMERCIAL VALUES
    // =========================================================

    /**
     * Purchase rate used for return calculation.
     */
    val rate: Double,

    /**
     * GST percentage applicable to this item.
     */
    val gstPercent: Double,


    // =========================================================
    // CALCULATED VALUES
    // =========================================================

    /**
     * Taxable value before GST.
     */
    val taxableAmount: Double,

    /**
     * GST amount for this returned item.
     */
    val gstAmount: Double,

    /**
     * Final value of this returned item.
     */
    val totalAmount: Double,


    // =========================================================
    // BATCH / LOT SNAPSHOT
    // =========================================================

    /**
     * Batch number where applicable.
     */
    val batchNumber: String = "",

    /**
     * Lot number where applicable.
     */
    val lotNumber: String = ""
)