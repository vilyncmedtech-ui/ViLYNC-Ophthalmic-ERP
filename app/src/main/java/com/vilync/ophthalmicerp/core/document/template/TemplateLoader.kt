package com.vilync.ophthalmicerp.core.document.template

/**
 * Responsible for retrieving raw template data based on a resolution result.
 */
interface TemplateLoader {
    /**
     * Loads the raw template data from the appropriate source.
     */
    suspend fun load(resolution: TemplateResolution): LoadedTemplate?
}

/**
 * Production implementation that bridges the Repository and the Loading Pipeline.
 */
class DefaultTemplateLoader(
    private val repository: DocumentTemplateRepository
) : TemplateLoader {

    override suspend fun load(resolution: TemplateResolution): LoadedTemplate? {
        val templateId = resolution.templateId ?: return null
        val versionNumber = resolution.versionNumber ?: return null
        
        // The repository is the bridge to the DATABASE source.
        return repository.getLoadedTemplate(templateId, versionNumber)
    }
}
