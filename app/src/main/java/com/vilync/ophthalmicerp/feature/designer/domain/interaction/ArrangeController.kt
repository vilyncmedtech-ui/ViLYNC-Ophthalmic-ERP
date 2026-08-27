package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.engine.*

/**
 * Executes Z-order reordering within a page.
 */
class ArrangeController {

    enum class OrderAction {
        BRING_FORWARD,
        SEND_BACKWARD,
        BRING_TO_FRONT,
        SEND_TO_BACK
    }

    /**
     * Produces a new RenderLayout with objects reordered.
     */
    fun arrange(layout: RenderLayout, targetIds: Set<String>, action: OrderAction): RenderLayout {
        if (targetIds.isEmpty()) return layout

        val updatedPages = layout.pages.map { page ->
            val targetsInPage = page.objects.filter { targetIds.contains(it.id) }
            if (targetsInPage.isEmpty()) {
                page
            } else {
                val newObjects = reorder(page.objects, targetIds, action)
                page.copy(objects = newObjects)
            }
        }
        
        return layout.copy(pages = updatedPages)
    }

    private fun reorder(objects: List<RenderObject>, targetIds: Set<String>, action: OrderAction): List<RenderObject> {
        val result = objects.toMutableList()
        
        when (action) {
            OrderAction.BRING_TO_FRONT -> {
                val targets = result.filter { targetIds.contains(it.id) }
                result.removeAll(targets)
                result.addAll(targets)
            }
            OrderAction.SEND_TO_BACK -> {
                val targets = result.filter { targetIds.contains(it.id) }
                result.removeAll(targets)
                result.addAll(0, targets)
            }
            OrderAction.BRING_FORWARD -> {
                // Iterate backwards to not mess up indices when moving forward
                for (i in result.indices.reversed()) {
                    if (targetIds.contains(result[i].id) && i < result.size - 1) {
                        // Swap with next
                        val temp = result[i]
                        result[i] = result[i + 1]
                        result[i + 1] = temp
                    }
                }
            }
            OrderAction.SEND_BACKWARD -> {
                // Iterate forwards
                for (i in result.indices) {
                    if (targetIds.contains(result[i].id) && i > 0) {
                        // Swap with previous
                        val temp = result[i]
                        result[i] = result[i - 1]
                        result[i - 1] = temp
                    }
                }
            }
        }
        
        return result
    }
}
