package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Renders workspace guides like rulers and safety markers.
 */
class UIOverlayLayer : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        val width = scope.size.width
        val height = scope.size.height
        
        // Render Top Ruler Background
        scope.drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = scope.size.copy(height = 20f)
        )
        
        // Render Left Ruler Background
        scope.drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = scope.size.copy(width = 20f)
        )
        
        // Draw Ruler Outlines
        scope.drawLine(Color.Gray, Offset(0f, 20f), Offset(width, 20f))
        scope.drawLine(Color.Gray, Offset(20f, 0f), Offset(20f, height))
    }
}
