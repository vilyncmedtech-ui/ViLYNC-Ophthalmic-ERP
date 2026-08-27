package com.vilync.ophthalmicerp.feature.designer.presentation.canvas

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper

/**
 * Base contract for workspace rendering layers.
 */
interface CanvasLayer {
    /**
     * Executes the drawing logic for this layer.
     */
    fun draw(
        scope: DrawScope,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    )
}
