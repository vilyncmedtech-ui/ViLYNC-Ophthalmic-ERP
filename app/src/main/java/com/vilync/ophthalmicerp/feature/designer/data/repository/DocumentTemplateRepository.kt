package com.vilync.ophthalmicerp.feature.designer.data.repository

import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate
import com.vilync.ophthalmicerp.core.document.domain.TemplateVersion
import com.vilync.ophthalmicerp.core.document.template.LoadedTemplate
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType
import kotlinx.coroutines.flow.Flow

/**
 * Repository focused strictly on persistence and retrieval of Template domain models.
 */
interface DocumentTemplateRepository {

    /**
     * Retrieves all templates assigned to a specific level context.
     */
    suspend fun getTemplatesByContext(
        documentType: DesignerDocumentType,
        companyId: Long,
        branchId: Long
    ): List<DocumentTemplate>

    /**
     * Retrieves a specific template by ID.
     */
    suspend fun getTemplateById(templateId: String): DocumentTemplate?

    /**
     * Retrieves a specific version for a template.
     */
    suspend fun getVersion(templateId: String, versionNumber: Int): TemplateVersion?

    /**
     * Retrieves the latest active version for a template.
     */
    suspend fun getActiveVersion(templateId: String): TemplateVersion?

    /**
     * Retrieves the raw loading model for a specific version.
     */
    suspend fun getLoadedTemplate(templateId: String, versionNumber: Int): LoadedTemplate?
}
