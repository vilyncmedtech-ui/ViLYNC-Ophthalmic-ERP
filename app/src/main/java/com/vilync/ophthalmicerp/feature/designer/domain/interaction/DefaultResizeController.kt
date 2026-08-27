package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.GeometryTransform

/**
 * Production implementation of ResizeController using anchor-based scaling.
 */
class DefaultResizeController : ResizeController {

    override fun handleEvent(event: ResizeEvent, session: ResizeSession?): ResizeResult {
        return when (event) {
            is ResizeEvent.Start -> start(event)
            is ResizeEvent.Move -> {
                if (session == null) ResizeResult.Cancelled
                else update(event, session)
            }
            is ResizeEvent.Commit -> {
                if (session == null) ResizeResult.Cancelled
                else commit(session)
            }
            is ResizeEvent.Cancel -> ResizeResult.Cancelled
        }
    }

    private fun start(event: ResizeEvent.Start): ResizeResult {
        var initialObj: RenderObject? = null
        for (page in event.layout.pages) {
            initialObj = page.objects.find { it.id == event.objectId }
            if (initialObj != null) break
        }
        
        if (initialObj == null) return ResizeResult.Cancelled

        // Calculate anchor (opposite to handle)
        val anchor = calculateAnchor(initialObj.position, initialObj.dimensions, event.handleType)

        return ResizeResult.Started(
            ResizeSession(
                objectId = event.objectId,
                initialPointerPos = event.pointerPos,
                initialDimensions = initialObj.dimensions,
                initialPosition = initialObj.position,
                handleType = event.handleType,
                anchorPoint = anchor,
                currentDimensions = initialObj.dimensions,
                currentPosition = initialObj.position,
                isResizing = true
            )
        )
    }

    private fun update(event: ResizeEvent.Move, session: ResizeSession): ResizeResult {
        val deltaX = event.pointerPos.x - session.initialPointerPos.x
        val deltaY = event.pointerPos.y - session.initialPointerPos.y
        
        var newW = session.initialDimensions.width
        var newH = session.initialDimensions.height
        var newX = session.initialPosition.x
        var newY = session.initialPosition.y

        when (session.handleType) {
            ResizeHandleType.R -> {
                newW = (session.initialDimensions.width + deltaX).coerceAtLeast(1.0)
            }
            ResizeHandleType.L -> {
                val d = deltaX.coerceAtMost(session.initialDimensions.width - 1.0)
                newW = session.initialDimensions.width - d
                newX = session.initialPosition.x + d
            }
            ResizeHandleType.B -> {
                newH = (session.initialDimensions.height + deltaY).coerceAtLeast(1.0)
            }
            ResizeHandleType.T -> {
                val d = deltaY.coerceAtMost(session.initialDimensions.height - 1.0)
                newH = session.initialDimensions.height - d
                newY = session.initialPosition.y + d
            }
            ResizeHandleType.BR -> {
                newW = (session.initialDimensions.width + deltaX).coerceAtLeast(1.0)
                newH = (session.initialDimensions.height + deltaY).coerceAtLeast(1.0)
            }
            ResizeHandleType.TL -> {
                val dx = deltaX.coerceAtMost(session.initialDimensions.width - 1.0)
                val dy = deltaY.coerceAtMost(session.initialDimensions.height - 1.0)
                newW = session.initialDimensions.width - dx
                newH = session.initialDimensions.height - dy
                newX = session.initialPosition.x + dx
                newY = session.initialPosition.y + dy
            }
            ResizeHandleType.TR -> {
                newW = (session.initialDimensions.width + deltaX).coerceAtLeast(1.0)
                val dy = deltaY.coerceAtMost(session.initialDimensions.height - 1.0)
                newH = session.initialDimensions.height - dy
                newY = session.initialPosition.y + dy
            }
            ResizeHandleType.BL -> {
                val dx = deltaX.coerceAtMost(session.initialDimensions.width - 1.0)
                newW = session.initialDimensions.width - dx
                newX = session.initialPosition.x + dx
                newH = (session.initialDimensions.height + deltaY).coerceAtLeast(1.0)
            }
        }

        val updatedSession = session.copy(
            currentDimensions = Dimensions(newW, newH),
            currentPosition = Point2D(newX, newY, unit = session.initialPosition.unit)
        )
        
        val preview = ResizePreview(
            objectId = session.objectId,
            dimensions = updatedSession.currentDimensions,
            position = updatedSession.currentPosition,
            handleType = session.handleType
        )
        
        return ResizeResult.Updated(updatedSession, preview)
    }

    private fun commit(session: ResizeSession): ResizeResult {
        return ResizeResult.Committed(RenderLayout(emptyList()))
    }

    private fun calculateAnchor(pos: Point2D, dim: Dimensions, handle: ResizeHandleType): Point2D {
        val type = when (handle) {
            ResizeHandleType.TL -> GeometryTransform.BoxPointType.BOTTOM_RIGHT
            ResizeHandleType.BR -> GeometryTransform.BoxPointType.TOP_LEFT
            ResizeHandleType.TR -> GeometryTransform.BoxPointType.BOTTOM_LEFT
            ResizeHandleType.BL -> GeometryTransform.BoxPointType.TOP_RIGHT
            ResizeHandleType.T  -> GeometryTransform.BoxPointType.BOTTOM_CENTER
            ResizeHandleType.B  -> GeometryTransform.BoxPointType.TOP_CENTER
            ResizeHandleType.L  -> GeometryTransform.BoxPointType.RIGHT_CENTER
            ResizeHandleType.R  -> GeometryTransform.BoxPointType.LEFT_CENTER
        }
        return GeometryTransform.getBoxPoint(pos, dim, type)
    }
}
