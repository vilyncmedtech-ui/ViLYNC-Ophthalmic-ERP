package com.vilync.ophthalmicerp.feature.designer.domain.model.JSON

import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType

/**
 * High-level JSON structure for the Document Designer.
 */
data class TemplateLayout(
    val pages: List<TemplatePage> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

data class TemplatePage(
    val index: Int,
    val width: Float,
    val height: Float,
    val orientation: String = "PORTRAIT", // "PORTRAIT", "LANDSCAPE"
    val objects: List<LayoutObject> = emptyList()
)

data class LayoutObject(
    val id: String,
    val type: String, // "TEXT", "IMAGE", "RECTANGLE", "CIRCLE", "LINE", "BARCODE", "QR_CODE", "TABLE", "DYNAMIC_FIELD"
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val properties: Map<String, Any> = emptyMap(),
    val styles: Map<String, String> = emptyMap(),
    val bindings: Map<String, String> = emptyMap()
)
