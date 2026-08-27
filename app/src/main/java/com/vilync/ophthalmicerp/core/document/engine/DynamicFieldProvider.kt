package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType

/**
 * Contract for ERP modules to provide business data for document templates.
 * Each provider declares its supported document types.
 */
interface DynamicFieldProvider {
    
    /**
     * The unique name of this provider for diagnostic reporting.
     */
    val name: String

    /**
     * Set of document types this provider is capable of handling.
     */
    val supportedTypes: Set<DesignerDocumentType>

    /**
     * Resolves and returns a map of field IDs to their strongly-typed values.
     */
    suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue>
}
