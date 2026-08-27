package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.*

/**
 * Manages the object translation (move) logic.
 */
interface TranslationController {
    fun handleEvent(event: TranslationEvent, session: TranslationSession?): TranslationResult
}

class DefaultTranslationController : TranslationController {

    override fun handleEvent(event: TranslationEvent, session: TranslationSession?): TranslationResult {
        return when (event) {
            is TranslationEvent.Start -> start(event)
            is TranslationEvent.Move -> {
                if (session == null) TranslationResult.Cancelled
                else update(event, session)
            }
            is TranslationEvent.Commit -> {
                if (session == null) TranslationResult.Cancelled
                else commit(session)
            }
            is TranslationEvent.Cancel -> TranslationResult.Cancelled
        }
    }

    private fun start(event: TranslationEvent.Start): TranslationResult {
        val initialPositions = mutableMapOf<String, Point2D>()
        
        event.layout.pages.forEach { page ->
            page.objects.filter { event.selectedIds.contains(it.id) }.forEach { obj ->
                initialPositions[obj.id] = obj.position
            }
        }
        
        return TranslationResult.Started(
            TranslationSession(
                initialPointerPos = event.pointerPos,
                initialPositions = initialPositions,
                isMoving = true
            )
        )
    }

    private fun update(event: TranslationEvent.Move, session: TranslationSession): TranslationResult {
        val deltaX = event.pointerPos.x - session.initialPointerPos.x
        val deltaY = event.pointerPos.y - session.initialPointerPos.y
        
        val offset = Point2D(deltaX, deltaY)
        val updatedSession = session.copy(currentOffsetMm = offset)
        
        val preview = TranslationPreview(
            objectOffsets = session.initialPositions.mapValues { (_, pos) ->
                Point2D(pos.x + deltaX, pos.y + deltaY)
            }
        )
        
        return TranslationResult.Updated(updatedSession, preview)
    }

    private fun commit(session: TranslationSession): TranslationResult {
        // Implementation note: The actual layout modification happens here
        // or is deferred to the ViewModel based on the Committed result.
        // For architectural purity, we produce the new layout.
        
        // This is a placeholder for the complex RenderLayout deep-cloning logic.
        // In Sprint 17, the ViewModel will handle the merge for simplicity.
        return TranslationResult.Committed(RenderLayout(emptyList())) 
    }
}
