package com.vilync.ophthalmicerp.feature.designer.data.repository

import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate
import com.vilync.ophthalmicerp.core.document.domain.TemplateVersion
import com.vilync.ophthalmicerp.core.document.template.LoadedTemplate
import com.vilync.ophthalmicerp.core.document.template.TemplateSource
import com.vilync.ophthalmicerp.feature.designer.data.dao.DocumentDesignerDao
import com.vilync.ophthalmicerp.feature.designer.data.entity.DocumentTemplateEntity
import com.vilync.ophthalmicerp.feature.designer.data.entity.TemplateVersionEntity
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType
import com.vilync.ophthalmicerp.feature.designer.domain.model.JSON.TemplateLayout

class DocumentTemplateRepositoryImpl(
    private val dao: DocumentDesignerDao
) : DocumentTemplateRepository {

    override suspend fun getTemplatesByContext(
        documentType: DesignerDocumentType,
        companyId: Long,
        branchId: Long
    ): List<DocumentTemplate> {
        val entity = dao.getAssignedTemplate(documentType, companyId, branchId)
        return listOfNotNull(entity?.toDomain())
    }

    override suspend fun getTemplateById(templateId: String): DocumentTemplate? {
        return dao.getTemplateById(templateId)?.toDomain()
    }

    override suspend fun getVersion(templateId: String, versionNumber: Int): TemplateVersion? {
        return dao.getVersion(templateId, versionNumber)?.toDomain()
    }

    override suspend fun getActiveVersion(templateId: String): TemplateVersion? {
        val template = dao.getTemplateById(templateId) ?: return null
        return dao.getVersion(templateId, template.activeVersion)?.toDomain()
    }

    override suspend fun getLoadedTemplate(templateId: String, versionNumber: Int): LoadedTemplate? {
        val entity = dao.getVersion(templateId, versionNumber) ?: return null
        
        return LoadedTemplate(
            templateId = entity.templateId,
            version = entity.versionNumber,
            schemaVersion = 1, // Default for now
            source = TemplateSource.DATABASE,
            rawJson = entity.layoutJson,
            checksum = "", // To be implemented with a hashing util
            lastModified = entity.createdAt
        )
    }

    // =========================================================
    // MAPPERS
    // =========================================================

    private fun DocumentTemplateEntity.toDomain() = DocumentTemplate(
        templateId = templateId,
        templateName = templateName,
        documentType = documentType,
        description = description,
        activeVersion = activeVersion,
        status = status
    )

    private fun TemplateVersionEntity.toDomain() = TemplateVersion(
        templateId = templateId,
        versionNumber = versionNumber,
        layout = parseLayout(layoutJson),
        status = status
    )

    private fun parseLayout(json: String): TemplateLayout {
        // Placeholder for JSON deserialization logic.
        // In production, use a validated JSON parser.
        return TemplateLayout()
    }
}
