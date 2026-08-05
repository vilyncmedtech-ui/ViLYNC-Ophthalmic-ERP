package com.vilync.ophthalmicerp.core.document.domain

import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType
import com.vilync.ophthalmicerp.feature.designer.domain.model.JSON.TemplateLayout
import com.vilync.ophthalmicerp.feature.designer.domain.model.TemplateStatus

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
    val status: TemplateStatus
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
