package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opening_stocks",
    indices = [
        Index(value = ["entryNumber"]),
        Index(value = ["normalizedEntryNumber"], unique = true),
        Index(value = ["entryDate"]),
        Index(value = ["status"]),
        Index(value = ["financialYearStart"])
    ]
)
data class OpeningStockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // e.g. OS-2026-001
    val entryNumber: String,

    val normalizedEntryNumber: String,

    // dd-MM-yyyy
    val entryDate: String,

    val financialYearStart: Int,

    val remarks: String = "",

    // POSTED, CANCELLED
    val status: String = "POSTED",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
