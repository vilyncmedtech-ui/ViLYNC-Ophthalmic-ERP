package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderObject

/**
 * Calculations for various object bounding boxes.
 */
interface BoundsCalculator {
    fun calculateLogicalBounds(obj: RenderObject): ObjectBounds
    fun calculateVisualBounds(obj: RenderObject): ObjectBounds
    fun calculateSelectionBounds(obj: RenderObject): ObjectBounds
}

data class ObjectBounds(
    val topLeft: Point2D,
    val dimensions: Dimensions,
    val rotation: Float
)
