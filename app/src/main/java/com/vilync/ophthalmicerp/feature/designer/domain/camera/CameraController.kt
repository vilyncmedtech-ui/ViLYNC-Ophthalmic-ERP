package com.vilync.ophthalmicerp.feature.designer.domain.camera

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions

/**
 * Controller for updating the ViewportCamera state.
 */
class CameraController(
    private val minZoom: Double = 0.1,
    private val maxZoom: Double = 10.0
) {
    /**
     * Zooms the camera centered on a specific workspace point.
     */
    fun zoom(current: ViewportCamera, factor: Double, focusWorkspace: Point2D): ViewportCamera {
        val newZoom = (current.zoom * factor).coerceIn(minZoom, maxZoom)
        val actualFactor = newZoom / current.zoom
        
        // Adjust center to keep the focus point static on screen
        val newCenterX = focusWorkspace.x + (current.center.x - focusWorkspace.x) / actualFactor
        val newCenterY = focusWorkspace.y + (current.center.y - focusWorkspace.y) / actualFactor
        
        return current.copy(
            zoom = newZoom,
            center = Point2D(newCenterX, newCenterY)
        )
    }

    /**
     * Pans the camera by a specific screen pixel offset.
     */
    fun pan(current: ViewportCamera, deltaPx: Point2D, screenDpi: Int): ViewportCamera {
        val mmPerPx = 25.4 / screenDpi
        val deltaMmX = deltaPx.x * mmPerPx / current.zoom
        val deltaMmY = deltaPx.y * mmPerPx / current.zoom
        
        return current.copy(
            center = Point2D(
                x = current.center.x - deltaMmX,
                y = current.center.y - deltaMmY
            )
        )
    }

    fun updateViewportSize(current: ViewportCamera, size: Dimensions): ViewportCamera {
        return current.copy(viewportSize = size)
    }
}
