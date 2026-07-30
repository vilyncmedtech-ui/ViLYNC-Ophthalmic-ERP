package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "purchase_return_lenses",

    foreignKeys = [

        ForeignKey(
            entity = PurchaseReturnItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseReturnItemId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = PurchaseLensEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalPurchaseLensId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],

    indices = [

        Index(
            value = ["purchaseReturnItemId"]
        ),

        /**
         * UNIQUE is intentional.
         *
         * One physical purchased IOL must not
         * be returned to the supplier twice.
         */
        Index(
            value = ["originalPurchaseLensId"],
            unique = true
        ),

        Index(
            value = ["serialNumber"]
        )
    ]
)
data class PurchaseReturnLensEntity(

    // =========================================================
    // PRIMARY KEY
    // =========================================================

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,


    // =========================================================
    // PURCHASE RETURN ITEM LINK
    // =========================================================

    /**
     * Parent Purchase Return Item.
     */
    val purchaseReturnItemId: Long,


    // =========================================================
    // ORIGINAL PHYSICAL IOL LINK
    // =========================================================

    /**
     * Exact physical lens from purchase_lenses.
     *
     * This provides permanent traceability
     * between Purchase → Lens → Purchase Return.
     */
    val originalPurchaseLensId: Long,


    // =========================================================
    // PHYSICAL LENS SNAPSHOT
    // =========================================================

    /**
     * Serial number snapshot.
     */
    val serialNumber: String,

    /**
     * Expiry date snapshot.
     */
    val expiryDate: String
)