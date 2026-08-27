package com.vilync.ophthalmicerp.data.dao

import androidx.room.Embedded
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity

data class FinancialTransactionRow(
    @Embedded val transaction: FinancialTransactionEntity,
    val partyName: String?
)
