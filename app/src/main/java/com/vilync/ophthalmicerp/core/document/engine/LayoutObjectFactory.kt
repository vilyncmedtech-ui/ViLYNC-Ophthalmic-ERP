package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.feature.designer.domain.model.JSON.TemplateLayout

/**
 * Result of the layout object factory transformation.
 */
sealed class FactoryResult {
    data class Success(val renderLayout: RenderLayout) : FactoryResult()
    data class Failure(val errors: List<FactoryError>) : FactoryResult()
}

/**
 * Structured error during the transformation process.
 */
data class FactoryError(
    val objectId: String?,
    val message: String,
    val code: String
)

/**
 * Interface for transforming TemplateLayout into an immutable RenderLayout.
 */
interface LayoutObjectFactory {
    /**
     * Creates a render-ready layout from a template.
     */
    fun create(layout: TemplateLayout): FactoryResult
}
