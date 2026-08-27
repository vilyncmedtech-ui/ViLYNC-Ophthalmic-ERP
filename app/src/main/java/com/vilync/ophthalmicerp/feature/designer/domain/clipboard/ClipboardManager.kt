package com.vilync.ophthalmicerp.feature.designer.domain.clipboard

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.*
import java.util.UUID

/**
 * Manages the internal designer clipboard for copy/paste operations.
 * Ensures data integrity by generating unique IDs for cloned elements.
 */
class ClipboardManager {

    private var buffer: List<RenderObject> = emptyList()

    /**
     * Snapshots the selected objects into the clipboard buffer.
     */
    fun copy(objects: List<RenderObject>) {
        // Deep copy not strictly necessary as objects are immutable,
        // but we store the references.
        buffer = objects.toList()
    }

    /**
     * Returns a list of cloned objects from the buffer with fresh IDs and a physical offset.
     */
    fun paste(offsetMm: Point2D = Point2D(5.0, 5.0)): List<RenderObject> {
        return buffer.map { obj ->
            val newId = UUID.randomUUID().toString()
            val newPos = Point2D(
                x = obj.position.x + offsetMm.x,
                y = obj.position.y + offsetMm.y,
                unit = obj.position.unit
            )
            
            cloneWithNewIdentity(obj, newId, newPos)
        }
    }

    fun hasContent(): Boolean = buffer.isNotEmpty()

    fun clear() {
        buffer = emptyList()
    }

    private fun cloneWithNewIdentity(obj: RenderObject, newId: String, newPos: Point2D): RenderObject {
        return when (obj) {
            is RenderText -> obj.copy(id = newId, position = newPos)
            is RenderImage -> obj.copy(id = newId, position = newPos)
            is RenderShape -> obj.copy(id = newId, position = newPos)
            is RenderBarcode -> obj.copy(id = newId, position = newPos)
            is RenderQrCode -> obj.copy(id = newId, position = newPos)
            is RenderTable -> obj.copy(id = newId, position = newPos)
            is RenderInvoiceTable -> obj.copy(id = newId, position = newPos)
            is RenderDynamicField -> obj.copy(id = newId, position = newPos)
        }
    }
}
