package com.vilync.ophthalmicerp.feature.designer.domain.property

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.feature.designer.domain.session.DesignerDocumentState

/**
 * Manages the application of property changes to the working document state.
 */
interface PropertyEditingController {
    fun applyChange(
        state: DesignerDocumentState,
        change: PropertyChange
    ): DesignerDocumentState
}

/**
 * Production implementation of the PropertyEditingController.
 */
class DefaultPropertyEditingController : PropertyEditingController {

    override fun applyChange(
        state: DesignerDocumentState,
        change: PropertyChange
    ): DesignerDocumentState {
        val updatedLayout = applyToLayout(state.committedLayout, change)
        return state.copy(committedLayout = updatedLayout)
    }

    private fun applyToLayout(layout: RenderLayout, change: PropertyChange): RenderLayout {
        val updatedPages = layout.pages.map { page ->
            val updatedObjects = page.objects.map { obj ->
                if (obj.id == change.objectId) {
                    applyToObject(obj, change)
                } else {
                    obj
                }
            }
            page.copy(objects = updatedObjects)
        }
        return layout.copy(pages = updatedPages)
    }

    private fun applyToObject(obj: RenderObject, change: PropertyChange): RenderObject {
        return when (change.propertyId) {
            "x" -> updatePosition(obj, x = (change.newValue as? Number)?.toDouble() ?: obj.position.x)
            "y" -> updatePosition(obj, y = (change.newValue as? Number)?.toDouble() ?: obj.position.y)
            "width" -> updateDimensions(obj, w = (change.newValue as? Number)?.toDouble() ?: obj.dimensions.width)
            "height" -> updateDimensions(obj, h = (change.newValue as? Number)?.toDouble() ?: obj.dimensions.height)
            "rotation" -> updateRotation(obj, (change.newValue as? Number)?.toFloat() ?: obj.rotation)
            "text" -> if (obj is RenderText) obj.copy(text = change.newValue?.toString() ?: "") else obj
            // Future: Handle table column config edits here
            else -> obj 
        }
    }

    private fun updatePosition(obj: RenderObject, x: Double? = null, y: Double? = null): RenderObject {
        val newPos = Point2D(x ?: obj.position.x, y ?: obj.position.y, obj.position.unit)
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

    private fun updateDimensions(obj: RenderObject, w: Double? = null, h: Double? = null): RenderObject {
        val newDim = Dimensions(w ?: obj.dimensions.width, h ?: obj.dimensions.height, obj.dimensions.unit)
        return when (obj) {
            is RenderText -> obj.copy(dimensions = newDim)
            is RenderImage -> obj.copy(dimensions = newDim)
            is RenderShape -> obj.copy(dimensions = newDim)
            is RenderBarcode -> obj.copy(dimensions = newDim)
            is RenderQrCode -> obj.copy(dimensions = newDim)
            is RenderTable -> obj.copy(dimensions = newDim)
            is RenderInvoiceTable -> obj.copy(dimensions = newDim)
            is RenderDynamicField -> obj.copy(dimensions = newDim)
        }
    }

    private fun updateRotation(obj: RenderObject, angle: Float): RenderObject {
        return when (obj) {
            is RenderText -> obj.copy(rotation = angle)
            is RenderImage -> obj.copy(rotation = angle)
            is RenderShape -> obj.copy(rotation = angle)
            is RenderBarcode -> obj.copy(rotation = angle)
            is RenderQrCode -> obj.copy(rotation = angle)
            is RenderTable -> obj.copy(rotation = angle)
            is RenderInvoiceTable -> obj.copy(rotation = angle)
            is RenderDynamicField -> obj.copy(rotation = angle)
        }
    }
}
