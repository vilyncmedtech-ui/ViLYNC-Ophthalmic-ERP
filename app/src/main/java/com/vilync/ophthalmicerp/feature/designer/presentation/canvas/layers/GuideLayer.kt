package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GuideLine
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GuideLine.Orientation
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Renders temporary visual alignment guides.
 */
class GuideLayer(
    private val guides: List<GuideLine>,
    private val guideColor: Color = Color(0xFF2196F3) // ERP Blue for guides
) : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        if (guides.isEmpty()) return

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        val viewportW = scope.size.width
        val viewportH = scope.size.height

        guides.forEach { guide ->
            val screenPos = mapper.workspaceToViewport(
                if (guide.orientation == Orientation.VERTICAL) Point2D(guide.position, 0.0)
                else Point2D(0.0, guide.position)
            )

            if (guide.orientation == Orientation.VERTICAL) {
                scope.drawLine(
                    color = guideColor,
                    start = Offset(screenPos.x.toFloat(), 0f),
                    end = Offset(screenPos.x.toFloat(), viewportH),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            } else {
                scope.drawLine(
                    color = guideColor,
                    start = Offset(0f, screenPos.y.toFloat()),
                    end = Offset(viewportW, screenPos.y.toFloat()),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }
        }
    }
}
