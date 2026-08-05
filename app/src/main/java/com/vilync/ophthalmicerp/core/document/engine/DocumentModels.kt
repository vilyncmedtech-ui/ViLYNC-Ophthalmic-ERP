package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.feature.designer.domain.model.OutputType

/**
 * Immutable request for document generation.
 * Separates "What" is being requested from "How" it is rendered.
 */
data class DocumentRequest(
    val templateId: String,
    val data: Map<String, Any?>,
    val language: String = "EN",
    val outputType: OutputType,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Runtime environmental properties for a rendering operation.
 * Provides metadata required for hardware-specific scaling and layout.
 */
interface RenderContext {
    val dpi: Int
    val scale: Float // Combined Zoom and Viewport Fit factor
    val targetUnit: MeasurementUnit
    val isGrayscale: Boolean
    fun getProperty(key: String): String?
}

/**
 * Platform-independent result of a rendering operation.
 */
interface RenderResult {
    val isSuccess: Boolean
    val errorCode: String?
    val message: String?
    val metadata: Map<String, String>
}

/**
 * specialized result for on-screen previews.
 * Supports lazy rendering and future backend evolution.
 */
interface PreviewRenderResult : RenderResult {
    val pageCount: Int
    val pageDimensions: Map<Int, Dimensions>
    val currentScale: Float
    
    /**
     * Renders a specific page to a generic output target.
     * Ownership of the output target remains with the caller.
     */
    suspend fun renderPage(pageIndex: Int, output: PreviewOutput): Boolean
}

/**
 * Opaque marker for a rendering target (e.g. Bitmap, GPU Surface).
 */
interface PreviewOutput
