package com.vilync.ophthalmicerp.core.document.engine

/**
 * Core engine responsible for merging a Template Layout with real-world ERP data.
 */
interface DocumentDataBinder {
    /**
     * Resolves all placeholders in the layout and replaces them with concrete render objects.
     */
    suspend fun bind(layout: RenderLayout, context: DocumentBindingContext): BindingResult
}
