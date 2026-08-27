package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout

/**
 * Responsible for finding document objects at a specific physical coordinate.
 */
interface HitTestEngine {
    /**
     * Performs a z-order aware intersection test.
     */
    fun test(point: Point2D, layout: RenderLayout): HitTestResult
}
