package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout

/**
 * Immutable state for an active translation (move) operation.
 */
data class TranslationSession(
    val initialPointerPos: Point2D,
    val currentOffsetMm: Point2D = Point2D(0.0, 0.0),
    val initialPositions: Map<String, Point2D>, // objectId -> initial position
    val isMoving: Boolean = false
)

/**
 * Events that drive the translation pipeline.
 */
sealed class TranslationEvent {
    data class Start(val pointerPos: Point2D, val selectedIds: Set<String>, val layout: RenderLayout) : TranslationEvent()
    data class Move(val pointerPos: Point2D) : TranslationEvent()
    object Commit : TranslationEvent()
    object Cancel : TranslationEvent()
}

/**
 * Outcome of a translation operation.
 */
sealed class TranslationResult {
    data class Started(val session: TranslationSession) : TranslationResult()
    data class Updated(val session: TranslationSession, val preview: TranslationPreview) : TranslationResult()
    data class Committed(val newLayout: RenderLayout) : TranslationResult()
    object Cancelled : TranslationResult()
}

/**
 * Derived data for real-time visual feedback.
 */
data class TranslationPreview(
    val objectOffsets: Map<String, Point2D> // objectId -> current translated position
)

/**
 * Extension point for future movement constraints (snapping, clamping).
 */
interface TranslationConstraint {
    fun apply(offset: Point2D): Point2D
}

/**
 * Future extension point for movement policy.
 */
enum class MovementPolicy {
    ALLOW_OUTSIDE_PAGE,
    CLAMP_TO_PAGE,
    CLAMP_TO_MARGIN
}

/**
 * Placeholder for future Undo/Redo commands.
 */
interface TranslationCommand
