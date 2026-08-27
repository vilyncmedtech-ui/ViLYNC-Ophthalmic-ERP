package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Renders physical document pages as white sheets with shadows.
 */
class PaperLayer(
    private val documentManager: DocumentManager
) : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        // Render each page's visual boundary
        // In this stage, we assume a single A4 page for the prototype session
        val pagePos = documentManager.getPageLocation(0) ?: Point2D(0.0, 0.0)
        
        // Hardcoded A4 for foundation stage
        val widthMm = 210.0
        val heightMm = 297.0
        
        val topLeftMm = Point2D(pagePos.x - widthMm / 2.0, pagePos.y - heightMm / 2.0)
        val screenPos = mapper.workspaceToViewport(topLeftMm)
        
        val screenW = mapper.mmToPixels(widthMm) * camera.zoom.toFloat()
        val screenH = mapper.mmToPixels(heightMm) * camera.zoom.toFloat()
        
        // Draw Shadow (Conceptual for foundation)
        scope.drawRect(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(screenPos.x.toFloat() + 4f, screenPos.y.toFloat() + 4f),
            size = Size(screenW, screenH)
        )
        
        // Draw Paper
        scope.drawRect(
            color = Color.White,
            topLeft = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
            size = Size(screenW, screenH)
        )
    }
}
