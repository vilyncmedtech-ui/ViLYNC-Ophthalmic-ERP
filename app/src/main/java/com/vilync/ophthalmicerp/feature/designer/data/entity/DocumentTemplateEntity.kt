package com.vilync.ophthalmicerp.feature.designer.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.domain.TemplateStatus

/**
 * Core entity for Document Templates.
 */
@Entity(
    tableName = "document_templates",
    indices = [
        Index(value = ["documentType"]),
        Index(value = ["status"])
    ]
)
data class DocumentTemplateEntity(
    @PrimaryKey
    val templateId: String, // UUID
    val templateName: String,
    val documentType: DesignerDocumentType,
    val description: String = "",
    val activeVersion: Int = 1,
    val status: TemplateStatus = TemplateStatus.DRAFT,
    val createdBy: String,
    val modifiedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
