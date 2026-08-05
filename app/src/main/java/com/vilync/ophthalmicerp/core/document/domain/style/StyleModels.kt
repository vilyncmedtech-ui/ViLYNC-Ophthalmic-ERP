package com.vilync.ophthalmicerp.core.document.domain.style

/**
 * Platform-independent Color representation (ARGB).
 */
data class DocumentColor(
    val alpha: Int,
    val red: Int,
    val green: Int,
    val blue: Int
) {
    companion object {
        val BLACK = DocumentColor(255, 0, 0, 0)
        val WHITE = DocumentColor(255, 255, 255, 255)
        val TRANSPARENT = DocumentColor(0, 0, 0, 0)
    }
}

/**
 * Text styling properties.
 */
enum class FontStyle {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC
}

/**
 * Horizontal text and object alignment.
 */
enum class HorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

/**
 * Vertical text and object alignment.
 */
enum class VerticalAlignment {
    TOP,
    MIDDLE,
    BOTTOM
}

/**
 * Border definition for shapes and tables.
 */
data class DocumentBorder(
    val width: Double, // in Millimeters
    val color: DocumentColor,
    val style: BorderStyle = BorderStyle.SOLID
) {
    enum class BorderStyle { NONE, SOLID, DASHED, DOTTED }
}

/**
 * Final immutable aggregate of all resolved style attributes.
 * Defaults are provided for mandatory fields.
 */
data class ResolvedStyle(
    val textColor: DocumentColor = DocumentColor.BLACK,
    val backgroundColor: DocumentColor = DocumentColor.TRANSPARENT,
    val fontSize: Double = 10.0, // in Points (standard for typography)
    val fontStyle: FontStyle = FontStyle.NORMAL,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
    val opacity: Float = 1.0f,
    val border: DocumentBorder? = null
)
