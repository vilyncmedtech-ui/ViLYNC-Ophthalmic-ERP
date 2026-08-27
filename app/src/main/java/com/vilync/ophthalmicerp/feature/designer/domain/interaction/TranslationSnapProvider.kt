package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D

/**
 * Extension point for future snapping logic (Grid, Guides).
 */
interface TranslationSnapProvider {
    fun snap(offsetMm: Point2D): Point2D
}
