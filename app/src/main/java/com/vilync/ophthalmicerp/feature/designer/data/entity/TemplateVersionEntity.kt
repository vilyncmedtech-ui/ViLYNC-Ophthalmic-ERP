package com.vilync.ophthalmicerp.feature.designer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vilync.ophthalmicerp.feature.designer.domain.model.TemplateStatus

/**
 * Stores historical and current JSON layouts for a template.
 */
@Entity(
    tableName = "template_versions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentTemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["versionNumber"]),
        Index(value = ["status"])
    ]
)
data class TemplateVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val templateId: String,
    val versionNumber: Int,
    val layoutJson: String, // The JSON payload defining pages, objects, etc.
    val status: TemplateStatus = TemplateStatus.DRAFT,
    val changeLog: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)
