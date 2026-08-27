package com.vilync.ophthalmicerp.feature.designer.data.dao

import androidx.room.*
import com.vilync.ophthalmicerp.feature.designer.data.entity.*
import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Universal Document Designer.
 * 
 * Follows ERP naming and transaction conventions:
 * - Observable lists via Flow.
 * - One-shot writes/lookups via suspend.
 * - Atomic operations via @Transaction.
 */
@Dao
interface DocumentDesignerDao {

    // =========================================================
    // TEMPLATES
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: DocumentTemplateEntity)

    @Update
    suspend fun updateTemplate(template: DocumentTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: DocumentTemplateEntity)

    @Query("SELECT * FROM document_templates WHERE templateId = :templateId LIMIT 1")
    suspend fun getTemplateById(templateId: String): DocumentTemplateEntity?

    @Query("SELECT * FROM document_templates ORDER BY templateName COLLATE NOCASE ASC")
    fun getAllTemplates(): Flow<List<DocumentTemplateEntity>>

    @Query("SELECT * FROM document_templates WHERE documentType = :type AND status = 'ACTIVE'")
    fun getActiveTemplatesByType(type: DesignerDocumentType): Flow<List<DocumentTemplateEntity>>

    // =========================================================
    // VERSIONS
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: TemplateVersionEntity)

    @Query("SELECT * FROM template_versions WHERE templateId = :templateId ORDER BY versionNumber DESC")
    fun getVersionsForTemplate(templateId: String): Flow<List<TemplateVersionEntity>>

    @Query("SELECT * FROM template_versions WHERE templateId = :templateId AND versionNumber = :versionNumber LIMIT 1")
    suspend fun getVersion(templateId: String, versionNumber: Int): TemplateVersionEntity?

    /**
     * Saves a new template version and updates the master template's active version reference.
     * 
     * Implementation ensures that the parent template record exists before inserting 
     * the child version to satisfy Foreign Key constraints.
     */
    @Transaction
    suspend fun saveNewVersion(template: DocumentTemplateEntity, version: TemplateVersionEntity) {
        val existing = getTemplateById(template.templateId)
        if (existing == null) {
            insertTemplate(template)
        } else {
            updateTemplate(template)
        }
        insertVersion(version)
    }

    // =========================================================
    // ASSIGNMENTS
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignTemplate(assignment: TemplateAssignmentEntity)

    @Query("""
        SELECT t.* FROM document_templates t
        INNER JOIN template_assignments a ON t.templateId = a.templateId
        WHERE a.documentType = :type AND a.companyId = :companyId AND a.branchId = :branchId AND a.isActive = 1
        LIMIT 1
    """)
    suspend fun getAssignedTemplate(type: DesignerDocumentType, companyId: Long, branchId: Long): DocumentTemplateEntity?

    @Query("SELECT * FROM template_assignments WHERE templateId = :templateId")
    fun getAssignmentsForTemplate(templateId: String): Flow<List<TemplateAssignmentEntity>>

    // =========================================================
    // ASSETS
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: TemplateAssetEntity)

    @Query("SELECT * FROM template_assets ORDER BY assetName COLLATE NOCASE ASC")
    fun getAllAssets(): Flow<List<TemplateAssetEntity>>

    @Query("SELECT * FROM template_assets WHERE assetId = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: String): TemplateAssetEntity?
}
