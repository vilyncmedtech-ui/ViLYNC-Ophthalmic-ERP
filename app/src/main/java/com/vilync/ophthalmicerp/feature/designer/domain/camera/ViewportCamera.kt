package com.vilync.ophthalmicerp.feature.designer.domain.camera

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions

/**
 * Immutable state representing the designer's viewport.
 */
data class ViewportCamera(
    val zoom: Double = 1.0,
    val center: Point2D = Point2D(0.0, 0.0), // center in Workspace Space
    val viewportSize: Dimensions = Dimensions(0.0, 0.0) // in screen pixels
)
