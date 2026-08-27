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
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.RotationPreview
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the rotation handle and manipulation ghosts.
 */
class RotationOverlay(
    private val session: InteractionSession,
    private val preview: RotationPreview?,
    private val layout: RenderLayout,
    private val documentManager: DocumentManager,
    private val rotationOffsetPx: Double = 40.0 // Matches HitTestEngine
) : CanvasLayer {

    private val handleColor = Color.White
    private val handleBorderColor = Color(0xFF4CAF50) // Interaction Green
    private val ghostColor = Color(0x664CAF50)
    private val handleRadiusPx = 10f

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        val primaryId = session.selection.primaryId ?: return
        
        // 1. Draw Rotation Preview (Ghost)
        preview?.let {
            drawRotationGhost(scope, it, camera, mapper)
        }
        
        // 2. Draw Rotation Handle around primary selection
        drawRotationHandle(scope, primaryId, camera, mapper)
    }

    private fun drawRotationGhost(
        scope: DrawScope,
        p: RotationPreview,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    ) {
        for (page in layout.pages) {
            val obj = page.objects.find { it.id == p.objectId } ?: continue
            
            val screenCenter = mapper.workspaceToViewport(p.center)
            val screenW = mapper.mmToPixels(obj.dimensions.width) * camera.zoom.toFloat()
            val screenH = mapper.mmToPixels(obj.dimensions.height) * camera.zoom.toFloat()
            
            scope.withTransform({
                rotate(p.angle.toFloat(), Offset(screenCenter.x.toFloat(), screenCenter.y.toFloat()))
            }) {
                drawRect(
                    color = ghostColor,
                    topLeft = Offset(
                        (screenCenter.x - screenW / 2.0).toFloat(), 
                        (screenCenter.y - screenH / 2.0).toFloat()
                    ),
                    size = Size(screenW, screenH),
                    style = Stroke(width = 2f)
                )
            }
            break
        }
    }

    private fun drawRotationHandle(
        scope: DrawScope,
        id: String,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    ) {
        for (pageIndex in layout.pages.indices) {
            val page = layout.pages[pageIndex]
            val obj = page.objects.find { it.id == id } ?: continue
            
            val pageOffset = documentManager.getPageLocation(pageIndex) ?: continue
            val layoutPage = documentManager.getLayoutPage(pageIndex) ?: continue
            
            val pageTopLeft = Point2D(
                x = pageOffset.x - layoutPage.width / 2.0,
                y = pageOffset.y - layoutPage.height / 2.0
            )
            
            val objTopLeftMm = Point2D(
                x = pageTopLeft.x + obj.position.x,
                y = pageTopLeft.y + obj.position.y
            )
            
            val handleMm = calculateHandlePosition(objTopLeftMm, obj.dimensions.width, obj.dimensions.height, obj.rotation, mapper)
            val screenPos = mapper.workspaceToViewport(handleMm)
            
            // Draw Connector Line
            val objCenterMm = Point2D(objTopLeftMm.x + obj.dimensions.width / 2.0, objTopLeftMm.y + obj.dimensions.height / 2.0)
            val screenCenter = mapper.workspaceToViewport(objCenterMm)
            
            scope.drawLine(
                color = handleBorderColor,
                start = Offset(screenCenter.x.toFloat(), screenCenter.y.toFloat()),
                end = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
                strokeWidth = 1f
            )

            // Draw Handle
            scope.drawCircle(
                color = handleColor,
                radius = handleRadiusPx,
                center = Offset(screenPos.x.toFloat(), screenPos.y.toFloat())
            )
            scope.drawCircle(
                color = handleBorderColor,
                radius = handleRadiusPx,
                center = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
                style = Stroke(width = 2f)
            )
            break
        }
    }

    private fun calculateHandlePosition(
        topLeft: Point2D,
        w: Double,
        h: Double,
        rotation: Float,
        mapper: CoordinateMapper
    ): Point2D {
        val center = Point2D(topLeft.x + w / 2.0, topLeft.y + h / 2.0)
        val rad = Math.toRadians(rotation.toDouble())
        
        val offsetMm = mapper.pixelsToMm(rotationOffsetPx.toFloat())
        val p = Point2D(topLeft.x + w / 2.0, topLeft.y - offsetMm)
        
        val tx = p.x - center.x
        val ty = p.y - center.y
        return Point2D(
            x = center.x + tx * cos(rad) - ty * sin(rad),
            y = center.y + tx * sin(rad) + ty * cos(rad)
        )
    }
}
