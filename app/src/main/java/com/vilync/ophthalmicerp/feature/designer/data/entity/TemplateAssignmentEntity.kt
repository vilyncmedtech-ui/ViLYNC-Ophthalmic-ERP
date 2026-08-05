package com.vilync.ophthalmicerp.feature.designer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType

/**
 * Maps which template should be used for a specific Document Type, Company, or Branch.
 */
@Entity(
    tableName = "template_assignments",
    foreignKeys = [
        ForeignKey(
            entity = DocumentTemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentType", "companyId", "branchId"], unique = true),
        Index(value = ["templateId"])
    ]
)
data class TemplateAssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val assignmentId: Long = 0,
    val documentType: DesignerDocumentType,
    val companyId: Long = 0, // 0 means Global fallback
    val branchId: Long = 0,  // 0 means Company fallback
    val templateId: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
