package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.feature.designer.domain.model.JSON.TemplateLayout

/**
 * Contract for resolving a layout from a template identifier.
 */
interface TemplateProvider {
    suspend fun getLayout(templateId: String): TemplateLayout?
}

/**
 * Contract for ensuring a template layout is structurally and logically sound.
 */
interface TemplateValidator {
    fun validate(layout: TemplateLayout): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)
