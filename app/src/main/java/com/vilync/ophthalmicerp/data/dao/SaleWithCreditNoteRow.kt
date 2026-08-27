package com.vilync.ophthalmicerp.data.dao

import androidx.room.Embedded
import com.vilync.ophthalmicerp.data.entity.SaleEntity

data class SaleWithCreditNoteRow(
    @Embedded val sale: SaleEntity,
    val creditNoteNumber: String?
)
