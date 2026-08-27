package com.vilync.ophthalmicerp.feature.designer.domain.interaction

/**
 * Manages the logic for updating selection state based on interaction events.
 */
interface SelectionController {
    /**
     * Processes a hit-test result and updates the session accordingly.
     */
    fun onHit(session: InteractionSession, hit: HitTestResult): InteractionSession

    /**
     * Clears the current selection.
     */
    fun clearSelection(session: InteractionSession): InteractionSession
}
