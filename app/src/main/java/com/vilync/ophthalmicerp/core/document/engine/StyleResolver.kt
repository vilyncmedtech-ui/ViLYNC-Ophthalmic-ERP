package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.style.ResolvedStyle

/**
 * Contract for a generic Style Resolution Engine.
 */
interface StyleResolver {
    /**
     * Resolves raw style properties into a strongly typed, immutable ResolvedStyle.
     * Supports cascading from defaults.
     */
    fun resolve(rawStyles: Map<String, String>, defaults: ResolvedStyle? = null): StyleResolutionResult
}

/**
 * Outcome of a style resolution operation.
 */
sealed class StyleResolutionResult {
    data class Success(val style: ResolvedStyle) : StyleResolutionResult()
    data class Failure(val errors: List<StyleError>) : StyleResolutionResult()
}

/**
 * Capture specifics of style resolution failures for diagnostics.
 */
data class StyleError(
    val key: String?,
    val message: String,
    val code: String
)
