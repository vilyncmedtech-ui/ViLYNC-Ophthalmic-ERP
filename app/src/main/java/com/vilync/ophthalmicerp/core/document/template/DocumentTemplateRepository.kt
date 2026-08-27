package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate
import com.vilync.ophthalmicerp.core.document.domain.TemplateVersion
import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout
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

    /**
     * Observes all templates for the list view.
     */
    fun getAllTemplates(): Flow<List<DocumentTemplate>>

    /**
     * Saves the provided layout as the current version of the template.
     * Adheres to SPRINT 26 rules: stores metadata and overwrites current version.
     */
    suspend fun saveTemplate(template: DocumentTemplate, layout: TemplateLayout)

    /**
     * Clones an existing template into a new one.
     */
    suspend fun duplicateTemplate(templateId: String, newName: String): String

    /**
     * Permanently removes a template.
     */
    suspend fun deleteTemplate(templateId: String)

    /**
     * Assigns a template as the default for a specific context.
     */
    suspend fun setDefaultTemplate(templateId: String, isDefault: Boolean)
}
