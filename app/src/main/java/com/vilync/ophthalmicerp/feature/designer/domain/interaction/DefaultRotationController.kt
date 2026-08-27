package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.GeometryTransform

/**
 * Production implementation of object rotation using center-pivot transformation.
 */
class DefaultRotationController : RotationController {

    override fun handleEvent(event: RotationEvent, session: RotationSession?): RotationResult {
        return when (event) {
            is RotationEvent.Start -> start(event)
            is RotationEvent.Move -> {
                if (session == null) RotationResult.Cancelled
                else update(event, session)
            }
            is RotationEvent.Commit -> {
                if (session == null) RotationResult.Cancelled
                else commit(session)
            }
            is RotationEvent.Cancel -> RotationResult.Cancelled
        }
    }

    private fun start(event: RotationEvent.Start): RotationResult {
        var initialObj: RenderObject? = null
        for (page in event.layout.pages) {
            initialObj = page.objects.find { it.id == event.objectId }
            if (initialObj != null) break
        }
        
        if (initialObj == null) return RotationResult.Cancelled

        val initialPointerAngle = GeometryTransform.calculateAngle(event.center, event.pointerPos)

        return RotationResult.Started(
            RotationSession(
                objectId = event.objectId,
                center = event.center,
                initialAngle = initialObj.rotation.toDouble(),
                initialPointerAngle = initialPointerAngle,
                currentAngle = initialObj.rotation.toDouble(),
                isRotating = true
            )
        )
    }

    private fun update(event: RotationEvent.Move, session: RotationSession): RotationResult {
        val currentPointerAngle = GeometryTransform.calculateAngle(session.center, event.pointerPos)
        val angleDelta = currentPointerAngle - session.initialPointerAngle
        val newAngle = GeometryTransform.normalizeAngle(session.initialAngle + angleDelta)
        
        val updatedSession = session.copy(currentAngle = newAngle)
        
        val preview = RotationPreview(
            objectId = session.objectId,
            angle = newAngle,
            center = session.center
        )
        
        return RotationResult.Updated(updatedSession, preview)
    }

    private fun commit(session: RotationSession): RotationResult {
        return RotationResult.Committed(RenderLayout(emptyList()))
    }
}
