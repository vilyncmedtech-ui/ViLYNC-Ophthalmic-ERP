package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vilync.ophthalmicerp.master.party.data.PartyEntity

@Entity(
    tableName = "financial_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["partyId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["financialYearStart"])
    ]
)
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // dd-MM-yyyy
    val transactionDate: String,

    val amount: Double,

    // CUSTOMER_RECEIPT, SUPPLIER_PAYMENT, OPENING_BALANCE, PAYMENT_ADJUSTMENT
    val type: String,

    // Internal account (Cash/Bank)
    val accountId: Long,

    // External entity (Customer/Supplier)
    val partyId: Long,

    // Reference like Cheque No, UTR, etc.
    val referenceNumber: String = "",

    val remarks: String = "",

    // DRAFT, POSTED, CANCELLED
    val status: String = "DRAFT",

    val financialYearStart: Int,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
