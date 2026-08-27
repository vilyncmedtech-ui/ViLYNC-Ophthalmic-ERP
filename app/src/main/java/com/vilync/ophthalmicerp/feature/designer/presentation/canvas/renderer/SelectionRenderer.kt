package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.renderer

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.InteractionSession

/**
 * Interface for rendering selection feedback and interaction handles.
 */
interface SelectionRenderer {
    fun drawSelection(
        scope: DrawScope,
        session: InteractionSession,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    )
}
