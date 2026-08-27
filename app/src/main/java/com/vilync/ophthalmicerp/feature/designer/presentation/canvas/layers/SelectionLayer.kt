package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.InteractionSession
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.renderer.SelectionRenderer

/**
 * Top-most layer for rendering selection visuals.
 */
class SelectionLayer(
    private val session: InteractionSession,
    private val renderer: SelectionRenderer
) : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        renderer.drawSelection(scope, session, camera, mapper)
    }
}
