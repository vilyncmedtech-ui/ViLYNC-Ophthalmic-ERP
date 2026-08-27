package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.GeometryTransform
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager

/**
 * Production implementation of HitTestEngine using Oriented Bounding Boxes (OBB).
 * Strictly follows Z-order (backwards iteration).
 */
class DefaultHitTestEngine(
    private val documentManager: DocumentManager,
    private val boundsCalculator: BoundsCalculator = DefaultBoundsCalculator()
) : HitTestEngine {

    override fun test(point: Point2D, layout: RenderLayout): HitTestResult {
        // Iterate pages backwards (Top-most page first)
        for (pageIndex in layout.pages.indices.reversed()) {
            val page = layout.pages[pageIndex]
            val pageOffset = documentManager.getPageLocation(pageIndex) ?: continue
            
            // Translate workspace point to page-local space
            val pageTopLeft = Point2D(
                x = pageOffset.x - page.dimensions.width / 2.0,
                y = pageOffset.y - page.dimensions.height / 2.0,
                unit = page.dimensions.unit
            )
            
            val localPoint = Point2D(
                x = point.x - pageTopLeft.x,
                y = point.y - pageTopLeft.y,
                unit = point.unit
            )
            
            // Iterate objects on page backwards (Z-order highest first)
            for (objIndex in page.objects.indices.reversed()) {
                val obj = page.objects[objIndex]
                val bounds = boundsCalculator.calculateSelectionBounds(obj)
                
                if (GeometryTransform.isPointInBox(localPoint, bounds.topLeft, bounds.dimensions, bounds.rotation)) {
                    return HitTestResult(
                        hitType = HitType.OBJECT,
                        objectId = obj.id,
                        priority = InteractionPriority.OBJECT
                    )
                }
            }
        }
        
        return HitTestResult(hitType = HitType.NONE)
    }
}
