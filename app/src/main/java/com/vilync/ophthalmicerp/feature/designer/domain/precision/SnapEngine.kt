package com.vilync.ophthalmicerp.feature.designer.domain.precision

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import kotlin.math.round

/**
 * Stateless engine for snapping coordinates to the grid.
 */
class SnapEngine {

    /**
     * Snaps a workspace coordinate to the configured grid.
     */
    fun snapToGrid(position: Point2D, config: GridConfig): Point2D {
        if (!config.isEnabled) return position

        val snappedX = round(position.x / config.sizeMm) * config.sizeMm
        val snappedY = round(position.y / config.sizeMm) * config.sizeMm

        return position.copy(x = snappedX, y = snappedY)
    }

    /**
     * Snaps a value to the nearest guide line if within threshold.
     */
    fun snapToGuides(value: Double, guides: List<Double>, threshold: Double = 2.0): Double {
        if (guides.isEmpty()) return value
        
        val nearestGuide = guides.minByOrNull { Math.abs(it - value) } ?: return value
        return if (Math.abs(nearestGuide - value) <= threshold) {
            nearestGuide
        } else {
            value
        }
    }
}
