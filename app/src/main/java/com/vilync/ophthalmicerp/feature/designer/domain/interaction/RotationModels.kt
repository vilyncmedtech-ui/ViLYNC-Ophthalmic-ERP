package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout

/**
 * Immutable state for an active rotation operation.
 */
data class RotationSession(
    val objectId: String,
    val center: Point2D, // Pivot point in Workspace mm
    val initialAngle: Double, // Initial object angle in degrees
    val initialPointerAngle: Double, // Initial pointer angle in degrees
    val currentAngle: Double, // Current calculated angle
    val isRotating: Boolean = false
)

/**
 * Events that drive the rotation pipeline.
 */
sealed class RotationEvent {
    data class Start(
        val pointerPos: Point2D,
        val objectId: String,
        val layout: RenderLayout,
        val center: Point2D
    ) : RotationEvent()
    
    data class Move(val pointerPos: Point2D) : RotationEvent()
    object Commit : RotationEvent()
    object Cancel : RotationEvent()
}

/**
 * Outcome of a rotation operation.
 */
sealed class RotationResult {
    data class Started(val session: RotationSession) : RotationResult()
    data class Updated(val session: RotationSession, val preview: RotationPreview) : RotationResult()
    data class Committed(val newLayout: RenderLayout) : RotationResult()
    object Cancelled : RotationResult()
}

/**
 * Derived data for real-time visual feedback.
 */
data class RotationPreview(
    val objectId: String,
    val angle: Double,
    val center: Point2D
)

/**
 * Result of a rotation handle hit-test.
 */
data class RotationHandleResult(
    val hitType: HitType,
    val objectId: String? = null
)

// =============================================================
// EXTENSION POINTS (POLICIES)
// =============================================================

interface AngleSnapPolicy {
    fun apply(angle: Double): Double
}

interface MinRotationPolicy {
    fun apply(angle: Double): Double
}

interface MaxRotationPolicy {
    fun apply(angle: Double): Double
}

/**
 * Placeholder for future Undo/Redo commands.
 */
interface RotationCommand
