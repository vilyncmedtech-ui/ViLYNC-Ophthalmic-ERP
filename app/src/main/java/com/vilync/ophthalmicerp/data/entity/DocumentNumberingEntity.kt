package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "document_series",
    primaryKeys = ["documentType", "financialYearStart"],
    indices = [
        Index(value = ["documentType"]),
        Index(value = ["financialYearStart"])
    ]
)
data class DocumentNumberingEntity(
    val documentType: String,
    val financialYearStart: Int,
    val lastSequenceNumber: Int,
    val prefix: String,
    val padding: Int = 4
)
