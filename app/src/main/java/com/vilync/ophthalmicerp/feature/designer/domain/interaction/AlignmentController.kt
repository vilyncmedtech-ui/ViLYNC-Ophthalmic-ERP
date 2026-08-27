package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.feature.designer.domain.precision.AlignmentAction

/**
 * Executes basic alignment operations for multiple objects.
 */
class AlignmentController {

    /**
     * Produces a new RenderLayout with objects aligned.
     */
    fun align(layout: RenderLayout, targetIds: Set<String>, action: AlignmentAction): RenderLayout {
        if (targetIds.size < 2) return layout

        val selectedObjects = layout.pages.flatMap { it.objects }.filter { targetIds.contains(it.id) }
        if (selectedObjects.size < 2) return layout

        val bounds = calculateSelectionBounds(selectedObjects)

        val updatedPages = layout.pages.map { page ->
            val updatedObjects = page.objects.map { obj ->
                if (targetIds.contains(obj.id)) {
                    applyAlignment(obj, bounds, action)
                } else {
                    obj
                }
            }
            page.copy(objects = updatedObjects)
        }

        return layout.copy(pages = updatedPages)
    }

    private data class SelectionBounds(
        val minX: Double, val maxX: Double,
        val minY: Double, val maxY: Double,
        val centerX: Double, val centerY: Double
    )

    private fun calculateSelectionBounds(objects: List<RenderObject>): SelectionBounds {
        val minX = objects.minOf { it.position.x }
        val maxX = objects.maxOf { it.position.x + it.dimensions.width }
        val minY = objects.minOf { it.position.y }
        val maxY = objects.maxOf { it.position.y + it.dimensions.height }

        return SelectionBounds(
            minX = minX, maxX = maxX,
            minY = minY, maxY = maxY,
            centerX = minX + (maxX - minX) / 2.0,
            centerY = minY + (maxY - minY) / 2.0
        )
    }

    private fun applyAlignment(obj: RenderObject, bounds: SelectionBounds, action: AlignmentAction): RenderObject {
        val newPos = when (action) {
            AlignmentAction.LEFT -> obj.position.copy(x = bounds.minX)
            AlignmentAction.RIGHT -> obj.position.copy(x = bounds.maxX - obj.dimensions.width)
            AlignmentAction.TOP -> obj.position.copy(y = bounds.minY)
            AlignmentAction.BOTTOM -> obj.position.copy(y = bounds.maxY - obj.dimensions.height)
            AlignmentAction.HORIZONTAL_CENTER -> obj.position.copy(x = bounds.centerX - obj.dimensions.width / 2.0)
            AlignmentAction.VERTICAL_CENTER -> obj.position.copy(y = bounds.centerY - obj.dimensions.height / 2.0)
        }

        return when (obj) {
            is RenderText -> obj.copy(position = newPos)
            is RenderImage -> obj.copy(position = newPos)
            is RenderShape -> obj.copy(position = newPos)
            is RenderBarcode -> obj.copy(position = newPos)
            is RenderQrCode -> obj.copy(position = newPos)
            is RenderTable -> obj.copy(position = newPos)
            is RenderInvoiceTable -> obj.copy(position = newPos)
            is RenderDynamicField -> obj.copy(position = newPos)
        }
    }
}
