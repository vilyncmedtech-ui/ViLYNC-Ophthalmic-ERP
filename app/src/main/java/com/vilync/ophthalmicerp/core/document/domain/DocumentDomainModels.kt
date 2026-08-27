package com.vilync.ophthalmicerp.core.document.domain

/**
 * Domain model for a Document Template.
 * Decoupled from Room persistence.
 */
data class DocumentTemplate(
    val templateId: String,
    val templateName: String,
    val documentType: DesignerDocumentType,
    val description: String,
    val activeVersion: Int,
    val status: TemplateStatus,
    val isDefault: Boolean = false,
    val lastModified: Long = 0L
)

/**
 * Domain model for a specific Template Version.
 */
data class TemplateVersion(
    val templateId: String,
    val versionNumber: Int,
    val layout: TemplateLayout,
    val status: TemplateStatus
)
