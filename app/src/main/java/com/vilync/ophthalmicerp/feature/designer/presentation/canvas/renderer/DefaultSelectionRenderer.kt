package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.renderer

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
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager

/**
 * Production implementation of SelectionRenderer.
 */
class DefaultSelectionRenderer(
    private val layout: RenderLayout,
    private val documentManager: DocumentManager
) : SelectionRenderer {

    private val selectionColor = Color(0xFF2196F3) // Blueprint Blue
    private val strokeWidth = 2f

    override fun drawSelection(
        scope: DrawScope,
        session: InteractionSession,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    ) {
        session.selection.selectedIds.forEach { id ->
            drawObjectHighlight(scope, id, camera, mapper)
        }
    }

    private fun drawObjectHighlight(
        scope: DrawScope,
        id: String,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    ) {
        // Find object and its page
        for (pageIndex in layout.pages.indices) {
            val page = layout.pages[pageIndex]
            val obj = page.objects.find { it.id == id } ?: continue
            
            val pageOffset = documentManager.getPageLocation(pageIndex) ?: continue
            val pageTopLeft = Point2D(
                x = pageOffset.x - page.dimensions.width / 2.0,
                y = pageOffset.y - page.dimensions.height / 2.0
            )
            
            val objWorkspacePos = Point2D(
                x = pageTopLeft.x + obj.position.x,
                y = pageTopLeft.y + obj.position.y
            )
            
            val screenPos = mapper.workspaceToViewport(objWorkspacePos)
            val screenW = mapper.mmToPixels(obj.dimensions.width) * camera.zoom.toFloat()
            val screenH = mapper.mmToPixels(obj.dimensions.height) * camera.zoom.toFloat()
            
            scope.withTransform({
                rotate(obj.rotation, Offset(screenPos.x.toFloat(), screenPos.y.toFloat()))
            }) {
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
                    size = Size(screenW, screenH),
                    style = Stroke(width = strokeWidth)
                )
            }
            break
        }
    }
}
