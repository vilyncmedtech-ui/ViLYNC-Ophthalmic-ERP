package com.vilync.ophthalmicerp.core.document.engine

import android.graphics.Canvas
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit

/**
 * Android implementation of the PreviewRenderer.
 * Leverages CanvasInstructionDispatcher for visual parity with Print/PDF.
 */
class DefaultPreviewRenderer : PreviewRenderer {

    override suspend fun render(request: DocumentRequest, context: RenderContext): RenderResult {
        // Base implementation would need to produce a full result. 
        // In Sprint 12, we focus on the specialized renderSession logic.
        return DefaultPreviewRenderResult(
            isSuccess = false,
            message = "Use specialized renderSession to produce an interactive preview."
        )
    }

    /**
     * Prepares a session for lazy rendering.
     */
    fun createPreview(session: RenderSession, context: RenderContext): PreviewRenderResult {
        val pageDimensions = calculatePageDimensions(session)
        
        return DefaultPreviewRenderResult(
            isSuccess = true,
            pageCount = session.diagnostics.pageCount,
            pageDimensions = pageDimensions,
            currentScale = context.scale,
            session = session,
            dpi = context.dpi
        )
    }

    private fun calculatePageDimensions(session: RenderSession): Map<Int, Dimensions> {
        val dimensions = mutableMapOf<Int, Dimensions>()
        session.instructions.forEach { inst ->
            // In a real system, page dimensions would be explicit in the layout.
            // For Phase 1, we assume A4 if first instruction on page doesn't hint otherwise.
            if (!dimensions.containsKey(inst.pageIndex)) {
                dimensions[inst.pageIndex] = Dimensions(210.0, 297.0, MeasurementUnit.MILLIMETER)
            }
        }
        return dimensions
    }

    private class DefaultPreviewRenderResult(
        override val isSuccess: Boolean,
        override val errorCode: String? = null,
        override val message: String? = null,
        override val metadata: Map<String, String> = emptyMap(),
        override val pageCount: Int = 0,
        override val pageDimensions: Map<Int, Dimensions> = emptyMap(),
        override val currentScale: Float = 1.0f,
        private val session: RenderSession? = null,
        private val dpi: Int = 160
    ) : PreviewRenderResult {

        override suspend fun renderPage(pageIndex: Int, output: PreviewOutput): Boolean {
            if (session == null) return false
            val canvasOutput = output as? CanvasPreviewOutput ?: return false
            val canvas = canvasOutput.canvas
            val warnings = mutableListOf<String>()

            val instructions = session.instructions.filter { it.pageIndex == pageIndex }
            
            canvas.save()
            
            // 1. Convert Points to Pixels for the screen
            // Standard PDF is 72 DPI. Screen is 'dpi'.
            val pointToPixelScale = (dpi / 72.0f) * currentScale
            canvas.scale(pointToPixelScale, pointToPixelScale)

            // 2. Dispatch instructions
            // CanvasInstructionDispatcher internally converts mm -> Points
            instructions.forEach { inst ->
                CanvasInstructionDispatcher.dispatch(canvas, inst, warnings)
            }

            canvas.restore()
            return true
        }
    }

    /**
     * Android-specific output target wrapping a Canvas.
     */
    class CanvasPreviewOutput(val canvas: Canvas) : PreviewOutput
}
