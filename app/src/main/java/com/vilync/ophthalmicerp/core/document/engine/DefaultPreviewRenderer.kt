package com.vilync.ophthalmicerp.core.document.engine

import android.graphics.Canvas
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions

/**
 * Android implementation of the PreviewRenderer.
 * Leverages CanvasInstructionDispatcher for visual parity with Print/PDF.
 */
class DefaultPreviewRenderer : PreviewRenderer {

    override suspend fun render(request: DocumentRequest, context: RenderContext): RenderResult {
        return DefaultPreviewRenderResult(
            isSuccess = false,
            message = "Use specialized renderSession to produce an interactive preview."
        )
    }

    /**
     * Prepares a session for lazy rendering.
     */
    fun createPreview(session: RenderSession, context: RenderContext): PreviewRenderResult {
        val pageDimensions = calculatePageDimensions(session, context.pageSize)
        
        return DefaultPreviewRenderResult(
            isSuccess = true,
            pageCount = session.diagnostics.pageCount,
            pageDimensions = pageDimensions,
            currentZoom = context.zoom,
            session = session,
            dpi = context.dpi
        )
    }

    private fun calculatePageDimensions(session: RenderSession, defaultSize: Dimensions): Map<Int, Dimensions> {
        val dimensions = mutableMapOf<Int, Dimensions>()
        session.instructions.forEach { inst ->
            if (!dimensions.containsKey(inst.pageIndex)) {
                dimensions[inst.pageIndex] = defaultSize
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
        override val currentZoom: Float = 1.0f,
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
            val pointToPixelScale = (dpi / 72.0f) * currentZoom
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
