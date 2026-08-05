package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.style.ResolvedStyle

/**
 * Top-level immutable container for a render-ready document.
 */
data class RenderLayout(
    val pages: List<RenderPage>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents a single renderable page.
 */
data class RenderPage(
    val index: Int,
    val dimensions: Dimensions,
    val objects: List<RenderObject>
)

/**
 * Base for all immutable render objects.
 * After resolution, objects contain only strongly typed values.
 */
sealed class RenderObject {
    abstract val id: String
    abstract val position: Point2D
    abstract val dimensions: Dimensions
    abstract val rotation: Float
    abstract val style: ResolvedStyle
}

/**
 * Static text element.
 */
data class RenderText(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val text: String
) : RenderObject()

/**
 * Static or dynamic image element.
 */
data class RenderImage(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val sourceUri: String? = null,
    val sourcePath: String? = null
) : RenderObject()

/**
 * Geometric shape element.
 */
data class RenderShape(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val shapeType: ShapeType
) : RenderObject() {
    enum class ShapeType { RECTANGLE, CIRCLE, LINE }
}

/**
 * Barcode element for product tracking.
 */
data class RenderBarcode(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val content: String,
    val barcodeType: String // "CODE128", "EAN13", etc.
) : RenderObject()

/**
 * QR Code element for payments or verification.
 */
data class RenderQrCode(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val content: String
) : RenderObject()

/**
 * Tabular data element (e.g., Sales Items).
 */
data class RenderTable(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val properties: Map<String, Any> = emptyMap()
) : RenderObject()

/**
 * ERP data placeholder (e.g., {{InvoiceNo}}).
 */
data class RenderDynamicField(
    override val id: String,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    val fieldId: String
) : RenderObject()
