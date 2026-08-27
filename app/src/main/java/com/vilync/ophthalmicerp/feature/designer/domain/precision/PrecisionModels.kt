package com.vilync.ophthalmicerp.feature.designer.domain.precision

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D

/**
 * Configuration for the millimeter-based grid.
 */
data class GridConfig(
    val sizeMm: Double = 5.0,
    val isVisible: Boolean = true,
    val isEnabled: Boolean = true
)

/**
 * Represents a visual alignment guide line.
 */
data class GuideLine(
    val position: Double, // Coordinate in Workspace mm
    val orientation: Orientation,
    val type: GuideType
) {
    enum class Orientation { HORIZONTAL, VERTICAL }
    enum class GuideType { PAGE_CENTER, MARGIN, OBJECT_EDGE, OBJECT_CENTER }
}

/**
 * Result of a snapping operation.
 */
data class SnapResult(
    val snappedPosition: Point2D,
    val activeGuides: List<GuideLine> = emptyList()
)

/**
 * Supported basic alignment actions.
 */
enum class AlignmentAction {
    LEFT, RIGHT, TOP, BOTTOM, HORIZONTAL_CENTER, VERTICAL_CENTER
}
