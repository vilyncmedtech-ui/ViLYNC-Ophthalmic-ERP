package com.vilync.ophthalmicerp.feature.designer.domain.precision

import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.core.document.engine.RenderObject
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GuideLine.Orientation
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GuideLine.GuideType

/**
 * Calculates visual alignment guides based on the current document layout.
 */
class SmartGuideEngine {

    /**
     * Generates a list of all potential guide lines for the current context.
     */
    fun calculateGuides(
        layout: RenderLayout,
        excludedIds: Set<String>
    ): List<GuideLine> {
        val guides = mutableListOf<GuideLine>()

        layout.pages.forEach { page ->
            // 1. Page Center Guides
            val centerX = page.dimensions.width / 2.0
            val centerY = page.dimensions.height / 2.0
            
            guides.add(GuideLine(centerX, Orientation.VERTICAL, GuideType.PAGE_CENTER))
            guides.add(GuideLine(centerY, Orientation.HORIZONTAL, GuideType.PAGE_CENTER))

            // 2. Page Margin Guides (Assume 10mm default for ERP)
            val margin = 10.0
            guides.add(GuideLine(margin, Orientation.VERTICAL, GuideType.MARGIN))
            guides.add(GuideLine(page.dimensions.width - margin, Orientation.VERTICAL, GuideType.MARGIN))
            guides.add(GuideLine(margin, Orientation.HORIZONTAL, GuideType.MARGIN))
            guides.add(GuideLine(page.dimensions.height - margin, Orientation.HORIZONTAL, GuideType.MARGIN))

            // 3. Object Edge Alignment Hints
            page.objects.filterNot { excludedIds.contains(it.id) }.forEach { obj ->
                addObjectGuides(obj, guides)
            }
        }

        return guides
    }

    private fun addObjectGuides(obj: RenderObject, guides: MutableList<GuideLine>) {
        val left = obj.position.x
        val right = obj.position.x + obj.dimensions.width
        val top = obj.position.y
        val bottom = obj.position.y + obj.dimensions.height
        val centerX = obj.position.x + (obj.dimensions.width / 2.0)
        val centerY = obj.position.y + (obj.dimensions.height / 2.0)

        guides.add(GuideLine(left, Orientation.VERTICAL, GuideType.OBJECT_EDGE))
        guides.add(GuideLine(right, Orientation.VERTICAL, GuideType.OBJECT_EDGE))
        guides.add(GuideLine(centerX, Orientation.VERTICAL, GuideType.OBJECT_CENTER))
        
        guides.add(GuideLine(top, Orientation.HORIZONTAL, GuideType.OBJECT_EDGE))
        guides.add(GuideLine(bottom, Orientation.HORIZONTAL, GuideType.OBJECT_EDGE))
        guides.add(GuideLine(centerY, Orientation.HORIZONTAL, GuideType.OBJECT_CENTER))
    }
}
