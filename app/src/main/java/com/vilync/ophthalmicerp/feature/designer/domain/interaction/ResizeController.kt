package com.vilync.ophthalmicerp.feature.designer.domain.interaction

/**
 * Manages the object resize logic using an anchor-based transformation.
 */
interface ResizeController {
    fun handleEvent(event: ResizeEvent, session: ResizeSession?): ResizeResult
}
