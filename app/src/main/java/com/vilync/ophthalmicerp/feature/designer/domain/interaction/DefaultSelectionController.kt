package com.vilync.ophthalmicerp.feature.designer.domain.interaction

/**
 * Production implementation of SelectionController.
 */
class DefaultSelectionController : SelectionController {

    override fun onHit(session: InteractionSession, hit: HitTestResult): InteractionSession {
        if (hit.hitType == HitType.NONE) {
            return if (session.currentMode == InteractionMode.REPLACE) {
                session.copy(selection = SelectionState())
            } else {
                session
            }
        }
        
        val hitId = hit.objectId ?: return session
        
        return when (session.currentMode) {
            InteractionMode.REPLACE -> {
                session.copy(
                    selection = SelectionState(
                        selectedIds = setOf(hitId),
                        primaryId = hitId
                    )
                )
            }
            InteractionMode.ADD -> {
                session.copy(
                    selection = SelectionState(
                        selectedIds = session.selection.selectedIds + hitId,
                        primaryId = hitId
                    )
                )
            }
            InteractionMode.TOGGLE -> {
                val newSelection = if (session.selection.selectedIds.contains(hitId)) {
                    session.selection.selectedIds - hitId
                } else {
                    session.selection.selectedIds + hitId
                }
                session.copy(
                    selection = SelectionState(
                        selectedIds = newSelection,
                        primaryId = if (newSelection.contains(hitId)) hitId else session.selection.primaryId
                    )
                )
            }
        }
    }

    override fun clearSelection(session: InteractionSession): InteractionSession {
        return session.copy(selection = SelectionState())
    }
}
