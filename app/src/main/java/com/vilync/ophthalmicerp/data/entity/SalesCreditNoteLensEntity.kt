package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_credit_note_lenses",
    foreignKeys = [
        ForeignKey(
            entity = SalesCreditNoteItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditNoteItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SaleLensEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalSaleLensId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = InventoryUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryUnitId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["creditNoteItemId"]),
        Index(value = ["originalSaleLensId"]),
        Index(value = ["inventoryUnitId"]),
        Index(value = ["serialNumber"])
    ]
)
data class SalesCreditNoteLensEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val creditNoteItemId: Long,

    // Exact physical lens on the original invoice.
    val originalSaleLensId: Long? = null,

    val inventoryUnitId: Long,

    val serialNumber: String,

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",

    // True only when this Credit Note physically returns the
    // unit to inventory. Repository transaction will enforce it.
    val returnToStock: Boolean = true
)
