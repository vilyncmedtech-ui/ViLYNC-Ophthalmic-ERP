package com.vilync.ophthalmicerp.feature.designer.domain.interaction

/**
 * Manages the object rotation logic using a center-pivot transformation.
 */
interface RotationController {
    fun handleEvent(event: RotationEvent, session: RotationSession?): RotationResult
}
