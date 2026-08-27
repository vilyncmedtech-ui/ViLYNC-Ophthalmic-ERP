package com.vilync.ophthalmicerp.feature.designer.data.repository

import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate
import com.vilync.ophthalmicerp.core.document.domain.TemplateVersion
import com.vilync.ophthalmicerp.core.document.template.DocumentTemplateRepository
import com.vilync.ophthalmicerp.core.document.template.LoadedTemplate
import com.vilync.ophthalmicerp.core.document.template.TemplateSource
import com.vilync.ophthalmicerp.core.document.template.TemplateSerializer
import com.vilync.ophthalmicerp.feature.designer.data.dao.DocumentDesignerDao
import com.vilync.ophthalmicerp.feature.designer.data.entity.*
import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout
import com.vilync.ophthalmicerp.core.document.domain.TemplateStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DocumentTemplateRepositoryImpl(
    private val dao: DocumentDesignerDao,
    private val serializer: TemplateSerializer
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
            schemaVersion = 1,
            source = TemplateSource.DATABASE,
            rawJson = entity.layoutJson,
            checksum = "",
            lastModified = entity.createdAt
        )
    }

    override fun getAllTemplates(): Flow<List<DocumentTemplate>> {
        return dao.getAllTemplates().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveTemplate(template: DocumentTemplate, layout: TemplateLayout) {
        val json = serializer.serialize(layout)
        val now = System.currentTimeMillis()
        
        val templateEntity = DocumentTemplateEntity(
            templateId = template.templateId,
            templateName = template.templateName,
            documentType = template.documentType,
            description = template.description,
            activeVersion = template.activeVersion,
            status = template.status,
            createdBy = "SYSTEM", // Placeholder for actual user auth
            modifiedBy = "SYSTEM",
            updatedAt = now
        )
        
        // As per SPRINT 26 rules: Overwrite existing version (No history)
        val versionEntity = TemplateVersionEntity(
            templateId = template.templateId,
            versionNumber = template.activeVersion,
            layoutJson = json,
            status = template.status,
            createdBy = "SYSTEM",
            createdAt = now
        )
        
        dao.saveNewVersion(templateEntity, versionEntity)
    }

    override suspend fun duplicateTemplate(templateId: String, newName: String): String {
        val original = dao.getTemplateById(templateId) ?: throw Exception("Template not found")
        val version = dao.getVersion(templateId, original.activeVersion) ?: throw Exception("Layout not found")
        
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val newTemplate = DocumentTemplateEntity(
            templateId = newId,
            templateName = newName,
            documentType = original.documentType,
            description = original.description,
            activeVersion = 1,
            status = TemplateStatus.ACTIVE,
            createdBy = "SYSTEM",
            modifiedBy = "SYSTEM",
            createdAt = now,
            updatedAt = now
        )
        
        val newVersion = TemplateVersionEntity(
            templateId = newId,
            versionNumber = 1,
            layoutJson = version.layoutJson,
            status = TemplateStatus.ACTIVE,
            createdBy = "SYSTEM",
            createdAt = now
        )
        
        dao.insertTemplate(newTemplate)
        dao.insertVersion(newVersion)
        
        return newId
    }

    override suspend fun deleteTemplate(templateId: String) {
        val entity = dao.getTemplateById(templateId) ?: return
        dao.deleteTemplate(entity)
    }

    override suspend fun setDefaultTemplate(templateId: String, isDefault: Boolean) {
        val template = dao.getTemplateById(templateId) ?: return
        
        dao.assignTemplate(TemplateAssignmentEntity(
            documentType = template.documentType,
            companyId = 0, // Global Default
            branchId = 0,
            templateId = templateId,
            isActive = isDefault
        ))
    }

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
        layout = serializer.deserialize(layoutJson),
        status = status
    )
}
