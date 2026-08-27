package com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.RenderingBridge

/**
 * Renders the document content using the non-blocking RenderingBridge.
 */
class PreviewLayer(
    private val renderingBridge: RenderingBridge,
    private val documentManager: DocumentManager,
    private val screenDpi: Int
) : CanvasLayer {

    override fun draw(scope: DrawScope, camera: ViewportCamera, mapper: CoordinateMapper) {
        // Iterate through physical pages
        val pageIndex = 0
        val pagePos = documentManager.getPageLocation(pageIndex) ?: return
        val layoutPage = documentManager.getLayoutPage(pageIndex) ?: return
        
        val bitmap = renderingBridge.getPage(pageIndex, screenDpi)
        
        val topLeftMm = Point2D(pagePos.x - layoutPage.width / 2.0, pagePos.y - layoutPage.height / 2.0)
        val screenPos = mapper.workspaceToViewport(topLeftMm)
        
        val screenW = mapper.mmToPixels(layoutPage.width.toDouble()) * camera.zoom.toFloat()
        val screenH = mapper.mmToPixels(layoutPage.height.toDouble()) * camera.zoom.toFloat()

        if (bitmap != null) {
            scope.drawImage(
                image = bitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(screenPos.x.toInt(), screenPos.y.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(screenW.toInt(), screenH.toInt())
            )
        }
    }
}
