package com.vilync.ophthalmicerp.core.document.domain.geometry

/**
 * Supported units of measurement for the document engine.
 * The canonical internal unit is MILLIMETER.
 */
enum class MeasurementUnit(val mmFactor: Double) {
    /** Physical millimeter. Internal canonical unit. */
    MILLIMETER(1.0),

    /** 1/72nd of an inch. Standard for PDF/PostScript. */
    POINT(25.4 / 72.0),
    
    /** Physical inch. 1 inch = 25.4 mm. */
    INCH(25.4),
    
    /** Screen pixels. To be resolved by renderers using DPI. */
    PIXEL(1.0) 
}

/**
 * Represents physical dimensions.
 */
data class Dimensions(
    val width: Double,
    val height: Double,
    val unit: MeasurementUnit = MeasurementUnit.MILLIMETER
) {
    /** Converts the current dimensions to a target unit. */
    fun convertTo(targetUnit: MeasurementUnit): Dimensions {
        if (unit == targetUnit) return this
        val widthInMm = width * unit.mmFactor
        val heightInMm = height * unit.mmFactor
        return Dimensions(
            width = widthInMm / targetUnit.mmFactor,
            height = heightInMm / targetUnit.mmFactor,
            unit = targetUnit
        )
    }
}

/**
 * Represents a coordinate in 2D space.
 */
data class Point2D(
    val x: Double,
    val y: Double,
    val unit: MeasurementUnit = MeasurementUnit.MILLIMETER
) {
    /** Converts the current point to a target unit. */
    fun convertTo(targetUnit: MeasurementUnit): Point2D {
        if (unit == targetUnit) return this
        val xInMm = x * unit.mmFactor
        val yInMm = y * unit.mmFactor
        return Point2D(
            x = xInMm / targetUnit.mmFactor,
            y = yInMm / targetUnit.mmFactor,
            unit = targetUnit
        )
    }
}

/**
 * Represents page margins.
 */
data class Margins(
    val top: Double,
    val left: Double,
    val bottom: Double,
    val right: Double,
    val unit: MeasurementUnit = MeasurementUnit.MILLIMETER
) {
    /** Converts the current margins to a target unit. */
    fun convertTo(targetUnit: MeasurementUnit): Margins {
        if (unit == targetUnit) return this
        val f = unit.mmFactor / targetUnit.mmFactor
        return Margins(
            top = top * f,
            left = left * f,
            bottom = bottom * f,
            right = right * f,
            unit = targetUnit
        )
    }
}

/**
 * Standard page size definitions in physical Millimeters.
 */
object StandardPageSizes {
    val A4 = Dimensions(210.0, 297.0, MeasurementUnit.MILLIMETER)
    val A5 = Dimensions(148.0, 210.0, MeasurementUnit.MILLIMETER)
    val LETTER = Dimensions(215.9, 279.4, MeasurementUnit.MILLIMETER)
}
