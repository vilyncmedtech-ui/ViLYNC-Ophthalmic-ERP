package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderObject

/**
 * Production implementation of BoundsCalculator.
 */
class DefaultBoundsCalculator(
    private val touchSlopMm: Double = 2.0
) : BoundsCalculator {

    override fun calculateLogicalBounds(obj: RenderObject): ObjectBounds {
        return ObjectBounds(
            topLeft = obj.position,
            dimensions = obj.dimensions,
            rotation = obj.rotation
        )
    }

    override fun calculateVisualBounds(obj: RenderObject): ObjectBounds {
        // For Phase 1, visual bounds match logical bounds
        return calculateLogicalBounds(obj)
    }

    override fun calculateSelectionBounds(obj: RenderObject): ObjectBounds {
        val logical = calculateLogicalBounds(obj)
        return logical.copy(
            topLeft = Point2D(
                x = logical.topLeft.x - touchSlopMm,
                y = logical.topLeft.y - touchSlopMm
            ),
            dimensions = Dimensions(
                width = logical.dimensions.width + (touchSlopMm * 2),
                height = logical.dimensions.height + (touchSlopMm * 2)
            )
        )
    }
}
