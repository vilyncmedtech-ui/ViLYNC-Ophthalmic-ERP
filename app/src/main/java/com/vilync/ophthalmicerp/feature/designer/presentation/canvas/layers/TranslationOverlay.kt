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
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.TranslationPreview
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Renders the real-time movement preview (ghosts) during translation.
 */
class TranslationOverlay(
    private val preview: TranslationPreview?,
    private val layout: RenderLayout,
    private val documentManager: DocumentManager
) : CanvasLayer {

    private val ghostColor = Color(0x662196F3) // Dimmed Blueprint Blue
    private val dashWidth = 1f

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        val currentPreview = preview ?: return
        
        currentPreview.objectOffsets.forEach { (id, newPos) ->
            drawGhost(scope, id, newPos, camera, mapper)
        }
    }

    private fun drawGhost(
        scope: DrawScope,
        id: String,
        workspacePosMm: Point2D,
        camera: ViewportCamera,
        mapper: CoordinateMapper
    ) {
        // Find object for dimensions and rotation
        for (page in layout.pages) {
            val obj = page.objects.find { it.id == id } ?: continue
            
            // Note: workspacePosMm is already global Workspace Space as calculated by controller
            val screenPos = mapper.workspaceToViewport(workspacePosMm)
            val screenW = mapper.mmToPixels(obj.dimensions.width) * camera.zoom.toFloat()
            val screenH = mapper.mmToPixels(obj.dimensions.height) * camera.zoom.toFloat()
            
            scope.withTransform({
                rotate(obj.rotation, Offset(screenPos.x.toFloat(), screenPos.y.toFloat()))
            }) {
                drawRect(
                    color = ghostColor,
                    topLeft = Offset(screenPos.x.toFloat(), screenPos.y.toFloat()),
                    size = Size(screenW, screenH),
                    style = Stroke(width = dashWidth)
                )
            }
            break
        }
    }
}
