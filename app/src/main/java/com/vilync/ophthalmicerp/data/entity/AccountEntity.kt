package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    // CASH, BANK
    val type: String,

    // Optional bank details
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",

    val initialBalance: Double = 0.0,
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
