package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.InteractionSession
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.ResizeHandleType
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.ResizePreview
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders resize handles and manipulation ghosts.
 * Handles are drawn at a constant pixel size.
 */
class ResizeOverlay(
    private val session: InteractionSession,
    private val preview: ResizePreview?,
    private val layout: RenderLayout,
    private val documentManager: DocumentManager
) : CanvasLayer {

    private val handleColor = Color.White
    private val handleBorderColor = Color(0xFF2196F3)
    private val ghostColor = Color(0x662196F3)
    private val handleSizePx = 10f

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        val primaryId = session.selection.primaryId ?: return
        
        // 1. Draw Resize Preview (Ghost)
        preview?.let {
            drawResizeGhost(scope, it, camera, mapper)
        }
        
        // 2. Draw Handles around primary selection
        drawHandles(scope, primaryId, camera, mapper)
    }

    private fun drawResizeGhost(
        scope: DrawScope, 
        p: ResizePreview, 
        camera: ViewportCamera, 
        mapper: CoordinateMapper
    ) {
        // Ghost is drawn using preview dimensions and position
        val screenPos = mapper.workspaceToViewport(p.position)
        val screenW = mapper.mmToPixels(p.dimensions.width) * camera.zoom.toFloat()
        val screenH = mapper.mmToPixels(p.dimensions.height) * camera.zoom.toFloat()
        
        scope.drawRect(
            color = ghostColor,
            topLeft = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
            size = Size(screenW, screenH),
            style = Stroke(width = 2f)
        )
    }

    private fun drawHandles(
        scope: DrawScope, 
        id: String, 
        camera: ViewportCamera, 
        mapper: CoordinateMapper
    ) {
        for (pageIndex in layout.pages.indices) {
            val page = layout.pages[pageIndex]
            val obj = page.objects.find { it.id == id } ?: continue
            
            val pageOffset = documentManager.getPageLocation(pageIndex) ?: continue
            val pageTopLeft = Point2D(
                x = pageOffset.x - page.dimensions.width / 2.0,
                y = pageOffset.y - page.dimensions.height / 2.0
            )
            
            val objTopLeftMm = Point2D(
                x = pageTopLeft.x + obj.position.x,
                y = pageTopLeft.y + obj.position.y
            )
            
            val w = obj.dimensions.width
            val h = obj.dimensions.height
            val rotation = obj.rotation

            // Calculate handle positions in Workspace MM
            val points = calculateHandlePoints(objTopLeftMm, w, h, rotation)
            
            points.values.forEach { pMm ->
                val screenPos = mapper.workspaceToViewport(pMm)
                
                // Draw Handle (Constant Pixel Size)
                scope.drawRect(
                    color = handleColor,
                    topLeft = Offset(screenPos.x.toFloat() - handleSizePx / 2f, screenPos.y.toFloat() - handleSizePx / 2f),
                    size = Size(handleSizePx, handleSizePx)
                )
                scope.drawRect(
                    color = handleBorderColor,
                    topLeft = Offset(screenPos.x.toFloat() - handleSizePx / 2f, screenPos.y.toFloat() - handleSizePx / 2f),
                    size = Size(handleSizePx, handleSizePx),
                    style = Stroke(width = 2f)
                )
            }
            break
        }
    }

    private fun calculateHandlePoints(
        topLeft: Point2D, 
        w: Double, 
        h: Double, 
        rotation: Float
    ): Map<ResizeHandleType, Point2D> {
        val center = Point2D(topLeft.x + w / 2.0, topLeft.y + h / 2.0)
        val rad = Math.toRadians(rotation.toDouble())
        
        fun rotatePoint(p: Point2D): Point2D {
            val tx = p.x - center.x
            val ty = p.y - center.y
            return Point2D(
                x = center.x + tx * cos(rad) - ty * sin(rad),
                y = center.y + tx * sin(rad) + ty * cos(rad)
            )
        }

        return mapOf(
            ResizeHandleType.TL to rotatePoint(Point2D(topLeft.x, topLeft.y)),
            ResizeHandleType.T  to rotatePoint(Point2D(topLeft.x + w / 2.0, topLeft.y)),
            ResizeHandleType.TR to rotatePoint(Point2D(topLeft.x + w, topLeft.y)),
            ResizeHandleType.R  to rotatePoint(Point2D(topLeft.x + w, topLeft.y + h / 2.0)),
            ResizeHandleType.BR to rotatePoint(Point2D(topLeft.x + w, topLeft.y + h)),
            ResizeHandleType.B  to rotatePoint(Point2D(topLeft.x + w / 2.0, topLeft.y + h)),
            ResizeHandleType.BL to rotatePoint(Point2D(topLeft.x, topLeft.y + h)),
            ResizeHandleType.L  to rotatePoint(Point2D(topLeft.x, topLeft.y + h / 2.0))
        )
    }
}
