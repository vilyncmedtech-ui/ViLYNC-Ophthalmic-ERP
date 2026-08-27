package com.vilync.ophthalmicerp.feature.designer.domain.manager

import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera

/**
 * Manages physical page locations and visibility in the workspace.
 */
class DocumentManager {
    private var pageLocations: Map<Int, Point2D> = emptyMap()
    private var currentLayout: TemplateLayout? = null
    private val pageSpacingMm = 20.0

    /**
     * Calculates the layout of pages in Workspace Space.
     */
    fun layoutPages(layout: TemplateLayout) {
        currentLayout = layout
        val locations = mutableMapOf<Int, Point2D>()
        var currentY = 0.0
        
        layout.pages.forEachIndexed { index, page ->
            locations[index] = Point2D(0.0, currentY + page.height / 2.0)
            currentY += page.height + pageSpacingMm
        }
        
        pageLocations = locations
    }

    fun getPageLocation(pageIndex: Int): Point2D? = pageLocations[pageIndex]

    fun getLayoutPage(pageIndex: Int): com.vilync.ophthalmicerp.core.document.domain.TemplatePage? {
        return currentLayout?.pages?.getOrNull(pageIndex)
    }

    /**
     * Identifies which pages are currently visible in the camera viewport.
     */
    fun getVisiblePageIndices(camera: ViewportCamera, layout: TemplateLayout): List<Int> {
        // API-ready only: currently returns all pages. 
        // Viewport culling to be implemented when virtualization is required.
        return layout.pages.indices.toList()
    }
}
