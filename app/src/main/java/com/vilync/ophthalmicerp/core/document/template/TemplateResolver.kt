package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate

/**
 * Main entry point for resolving which template to use for a document request.
 */
interface TemplateResolver {
    /**
     * Resolves the template using a prioritized strategy chain.
     */
    suspend fun resolve(request: TemplateResolutionRequest): TemplateResolution
}

/**
 * Contract for individual steps in the resolution hierarchy.
 */
interface ResolutionStrategy {
    /**
     * Attempts to resolve a template at a specific level.
     */
    suspend fun execute(request: TemplateResolutionRequest): StrategyResult
}

/**
 * Outcome of a single resolution strategy execution.
 */
sealed class StrategyResult {
    /**
     * Strategy successfully found one or more templates.
     */
    data class Resolved(val templates: List<DocumentTemplate>, val level: ResolutionLevel) : StrategyResult()
    
    /**
     * Strategy did not find any matching template.
     */
    data object NotResolved : StrategyResult()
    
    /**
     * Strategy encountered a logical or data error.
     */
    data class Error(val message: String) : StrategyResult()
}
