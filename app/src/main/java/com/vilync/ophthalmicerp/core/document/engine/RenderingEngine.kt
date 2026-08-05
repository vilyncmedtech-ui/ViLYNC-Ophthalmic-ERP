package com.vilync.ophthalmicerp.core.document.engine

/**
 * Main contract for transforming a RenderLayout into a sequence of drawing instructions.
 */
interface RenderingEngine {
    /**
     * Processes the layout and generates a deterministic instruction set.
     */
    suspend fun process(layout: RenderLayout): RenderSession
}
