package com.vilync.ophthalmicerp.feature.designer.domain.geometry

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera

/**
 * Sole authority for coordinate conversions between different spaces.
 */
class CoordinateMapper(
    private val camera: ViewportCamera,
    private val screenDpi: Int
) {
    private val mmToInch = 1.0 / 25.4

    /**
     * Converts a point from Screen Space (Pixels) to Workspace Space (Millimeters).
     */
    fun screenToWorkspace(screenPoint: Point2D): Point2D {
        val pxFromCenter = Point2D(
            x = screenPoint.x - camera.viewportSize.width / 2.0,
            y = screenPoint.y - camera.viewportSize.height / 2.0
        )
        
        val mmFromCenter = pixelsToMm(pxFromCenter)
        
        return Point2D(
            x = camera.center.x + mmFromCenter.x / camera.zoom,
            y = camera.center.y + mmFromCenter.y / camera.zoom
        )
    }

    /**
     * Converts a point from Workspace Space (Millimeters) to Viewport Space (Pixels relative to canvas top-left).
     */
    fun workspaceToViewport(workspacePoint: Point2D): Point2D {
        val mmFromCenter = Point2D(
            x = (workspacePoint.x - camera.center.x) * camera.zoom,
            y = (workspacePoint.y - camera.center.y) * camera.zoom
        )
        
        val pxFromCenter = mmToPixels(mmFromCenter)
        
        return Point2D(
            x = camera.viewportSize.width / 2.0 + pxFromCenter.x,
            y = camera.viewportSize.height / 2.0 + pxFromCenter.y
        )
    }

    fun mmToPixels(mm: Double): Float {
        return (mm * mmToInch * screenDpi).toFloat()
    }

    fun pixelsToMm(px: Float): Double {
        return px.toDouble() / screenDpi / mmToInch
    }

    private fun mmToPixels(mmPoint: Point2D): Point2D {
        return Point2D(
            x = (mmPoint.x * mmToInch * screenDpi),
            y = (mmPoint.y * mmToInch * screenDpi)
        )
    }

    private fun pixelsToMm(pxPoint: Point2D): Point2D {
        return Point2D(
            x = pxPoint.x / screenDpi / mmToInch,
            y = pxPoint.y / screenDpi / mmToInch
        )
    }
}
