package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.feature.designer.data.repository.DocumentTemplateRepository
import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate

/**
 * Production implementation of TemplateResolver.
 * Orchestrates a sequence of ResolutionStrategies to find the best matching layout.
 */
class DefaultTemplateResolver(
    private val repository: DocumentTemplateRepository,
    private val strategies: List<ResolutionStrategy> = listOf(
        BranchStrategy(repository),
        CompanyStrategy(repository),
        GlobalStrategy(repository),
        SystemDefaultStrategy()
    )
) : TemplateResolver {

    override suspend fun resolve(request: TemplateResolutionRequest): TemplateResolution {
        val trace = mutableListOf<ResolutionStep>()

        for (strategy in strategies) {
            val startTime = System.currentTimeMillis()
            val result = strategy.execute(request)
            val executionTime = System.currentTimeMillis() - startTime

            when (result) {
                is StrategyResult.Resolved -> {
                    val templates = result.templates
                    val level = result.level

                    if (templates.size > 1) {
                        val conflictStep = ResolutionStep(
                            level = level,
                            status = TemplateResolutionStatus.CONFLICT,
                            reason = "Multiple templates (${templates.size}) found at this level.",
                            executionTimeMs = executionTime
                        )
                        return TemplateResolution(
                            status = TemplateResolutionStatus.CONFLICT,
                            trace = trace + conflictStep,
                            errorMessage = "Conflict: Multiple templates assigned to ${level.name}."
                        )
                    }

                    val template = templates.first()
                    val versionToUse = request.requestedVersion ?: template.activeVersion
                    
                    // Verify version existence
                    val versionExists = repository.getVersion(template.templateId, versionToUse) != null
                    
                    val stepStatus = if (request.requestedVersion != null && !versionExists) {
                        TemplateResolutionStatus.FALLBACK_USED
                    } else {
                        TemplateResolutionStatus.SUCCESS
                    }

                    val finalStep = ResolutionStep(
                        level = level,
                        status = stepStatus,
                        resolvedTemplateId = template.templateId,
                        resolvedVersion = if (stepStatus == TemplateResolutionStatus.FALLBACK_USED) template.activeVersion else versionToUse,
                        reason = if (stepStatus == TemplateResolutionStatus.FALLBACK_USED) "Requested version missing, using active." else "Exact match found.",
                        executionTimeMs = executionTime
                    )

                    return TemplateResolution(
                        status = stepStatus,
                        templateId = template.templateId,
                        versionNumber = finalStep.resolvedVersion,
                        resolvedLevel = level,
                        trace = trace + finalStep
                    )
                }

                is StrategyResult.NotResolved -> {
                    trace.add(
                        ResolutionStep(
                            level = (strategy as? BaseStrategy)?.level ?: ResolutionLevel.GLOBAL,
                            status = TemplateResolutionStatus.NOT_FOUND,
                            reason = "No assignment found.",
                            executionTimeMs = executionTime
                        )
                    )
                }

                is StrategyResult.Error -> {
                    val errorStep = ResolutionStep(
                        level = (strategy as? BaseStrategy)?.level ?: ResolutionLevel.GLOBAL,
                        status = TemplateResolutionStatus.ERROR,
                        reason = result.message,
                        executionTimeMs = executionTime
                    )
                    return TemplateResolution(
                        status = TemplateResolutionStatus.ERROR,
                        trace = trace + errorStep,
                        errorMessage = result.message
                    )
                }
            }
        }

        return TemplateResolution(
            status = TemplateResolutionStatus.NOT_FOUND,
            trace = trace,
            errorMessage = "Template could not be resolved after ${strategies.size} attempts."
        )
    }
}

// =============================================================
// CORE STRATEGIES
// =============================================================

abstract class BaseStrategy(val level: ResolutionLevel) : ResolutionStrategy

class BranchStrategy(private val repository: DocumentTemplateRepository) : BaseStrategy(ResolutionLevel.BRANCH) {
    override suspend fun execute(request: TemplateResolutionRequest): StrategyResult {
        if (request.branchId == 0L) return StrategyResult.NotResolved
        val matches = repository.getTemplatesByContext(request.documentType, request.companyId, request.branchId)
        return if (matches.isNotEmpty()) StrategyResult.Resolved(matches, level) else StrategyResult.NotResolved
    }
}

class CompanyStrategy(private val repository: DocumentTemplateRepository) : BaseStrategy(ResolutionLevel.COMPANY) {
    override suspend fun execute(request: TemplateResolutionRequest): StrategyResult {
        if (request.companyId == 0L) return StrategyResult.NotResolved
        val matches = repository.getTemplatesByContext(request.documentType, request.companyId, 0L)
        return if (matches.isNotEmpty()) StrategyResult.Resolved(matches, level) else StrategyResult.NotResolved
    }
}

class GlobalStrategy(private val repository: DocumentTemplateRepository) : BaseStrategy(ResolutionLevel.GLOBAL) {
    override suspend fun execute(request: TemplateResolutionRequest): StrategyResult {
        val matches = repository.getTemplatesByContext(request.documentType, 0L, 0L)
        return if (matches.isNotEmpty()) StrategyResult.Resolved(matches, level) else StrategyResult.NotResolved
    }
}

/**
 * Hardcoded system fallback for critical document types.
 */
class SystemDefaultStrategy : BaseStrategy(ResolutionLevel.SYSTEM_DEFAULT) {
    override suspend fun execute(request: TemplateResolutionRequest): StrategyResult {
        // In the future, this can return a hardcoded template or a constant ID.
        // For Phase 1, we return NotResolved to signify end of chain.
        return StrategyResult.NotResolved
    }
}
