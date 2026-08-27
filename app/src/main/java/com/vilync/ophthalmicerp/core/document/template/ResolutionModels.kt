package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.domain.OutputType

/**
 * Priority levels for template resolution.
 */
enum class ResolutionLevel {
    BRANCH,
    COMPANY,
    GLOBAL,
    SYSTEM_DEFAULT
}

/**
 * Outcome status of a template resolution request.
 */
enum class TemplateResolutionStatus {
    SUCCESS,
    FALLBACK_USED,
    NOT_FOUND,
    CONFLICT,
    ERROR
}

/**
 * Structured entry in the resolution trace for diagnostics.
 */
data class ResolutionStep(
    val level: ResolutionLevel,
    val status: TemplateResolutionStatus,
    val resolvedTemplateId: String? = null,
    val resolvedVersion: Int? = null,
    val reason: String? = null,
    val executionTimeMs: Long = 0
)

/**
 * Encapsulates the context for a template resolution operation.
 */
data class TemplateResolutionRequest(
    val documentType: DesignerDocumentType,
    val companyId: Long = 0,
    val branchId: Long = 0,
    val requestedVersion: Int? = null,
    val language: String = "EN",
    val outputType: OutputType = OutputType.PRINT
)

/**
 * Final result of the template resolution process.
 */
data class TemplateResolution(
    val status: TemplateResolutionStatus,
    val templateId: String? = null,
    val versionNumber: Int? = null,
    val resolvedLevel: ResolutionLevel? = null,
    val trace: List<ResolutionStep> = emptyList(),
    val errorMessage: String? = null
)
