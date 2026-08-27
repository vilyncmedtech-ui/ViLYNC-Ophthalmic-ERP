package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GridConfig
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Renders a millimeter-based architectural grid.
 */
class GridLayer(
    private val config: GridConfig = GridConfig(),
    private val majorGridColor: Color = Color(0x33000000),
    private val minorGridColor: Color = Color(0x11000000)
) : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        if (!config.isVisible) return
        
        val zoom = camera.zoom
        val spacingMm = config.sizeMm
        
        // Draw Major Grid (Size x 2)
        drawLines(scope, mapper, spacingMm * 2, majorGridColor)
        
        // Draw Minor Grid (Size) only if zoom is sufficient
        if (zoom > 1.0) {
            drawLines(scope, mapper, spacingMm, minorGridColor)
        }
    }

    private fun drawLines(scope: DrawScope, mapper: CoordinateMapper, spacingMm: Double, color: Color) {
        val viewportW = scope.size.width
        val viewportH = scope.size.height
        
        // Convert viewport bounds to workspace space
        val topLeft = mapper.screenToWorkspace(Point2D(0.0, 0.0))
        val bottomRight = mapper.screenToWorkspace(Point2D(viewportW.toDouble(), viewportH.toDouble()))
        
        val startX = (Math.floor(topLeft.x / spacingMm) * spacingMm)
        val endX = (Math.ceil(bottomRight.x / spacingMm) * spacingMm)
        
        val startY = (Math.floor(topLeft.y / spacingMm) * spacingMm)
        val endY = (Math.ceil(bottomRight.y / spacingMm) * spacingMm)
        
        // Vertical Lines
        var currX = startX
        while (currX <= endX) {
            val screenPos = mapper.workspaceToViewport(Point2D(currX, 0.0))
            scope.drawLine(
                color = color,
                start = Offset(screenPos.x.toFloat(), 0f),
                end = Offset(screenPos.x.toFloat(), viewportH),
                strokeWidth = 1f
            )
            currX += spacingMm
        }
        
        // Horizontal Lines
        var currY = startY
        while (currY <= endY) {
            val screenPos = mapper.workspaceToViewport(Point2D(0.0, currY))
            scope.drawLine(
                color = color,
                start = Offset(0f, screenPos.y.toFloat()),
                end = Offset(viewportW, screenPos.y.toFloat()),
                strokeWidth = 1f
            )
            currY += spacingMm
        }
    }
}
