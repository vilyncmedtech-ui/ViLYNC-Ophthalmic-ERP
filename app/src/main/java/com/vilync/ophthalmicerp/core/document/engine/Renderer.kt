package com.vilync.ophthalmicerp.core.document.engine

/**
 * Base contract for all document renderers.
 */
interface Renderer {
    /**
     * Executes the rendering logic for the given request and context.
     */
    suspend fun render(request: DocumentRequest, context: RenderContext): RenderResult
}

/**
 * Specialization for physical hardware printing.
 */
interface PrintRenderer : Renderer

/**
 * Specialization for PDF document generation.
 */
interface PdfRenderer : Renderer

/**
 * Specialization for real-time UI previews.
 */
interface PreviewRenderer : Renderer

/**
 * Specialization for image (PNG/JPG) generation.
 */
interface ImageRenderer : Renderer

/**
 * Specialization for social/external application sharing.
 */
interface ShareRenderer : Renderer
