package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.feature.designer.domain.model.JSON.TemplateLayout

/**
 * Origin of the template data.
 */
enum class TemplateSource {
    DATABASE,
    ASSETS,
    NETWORK,
    EMBEDDED
}

/**
 * Domain model representing raw template data and its metadata.
 * Serves as the primary transport object for the loading pipeline.
 */
data class LoadedTemplate(
    val templateId: String,
    val version: Int,
    val schemaVersion: Int,
    val source: TemplateSource,
    val rawJson: String,
    val checksum: String,
    val lastModified: Long
)

/**
 * Aggregate result of the layout parsing process.
 */
data class ParseResult(
    val layout: TemplateLayout?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val schemaVersion: Int,
    val processingTimeMs: Long
)
