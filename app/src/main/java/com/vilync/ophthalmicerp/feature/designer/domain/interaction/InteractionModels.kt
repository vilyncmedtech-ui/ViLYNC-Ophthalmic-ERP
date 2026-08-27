package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D

/**
 * Platform-neutral representation of a pointer event.
 */
data class PointerInput(
    val position: Point2D, // In Workspace Space (Millimeters)
    val type: PointerType,
    val action: PointerAction,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PointerType { TOUCH, MOUSE, STYLUS }
enum class PointerAction { DOWN, MOVE, UP, HOVER, CANCEL }

/**
 * Governs how new selection events affect the current selection set.
 */
enum class InteractionMode {
    REPLACE, // Clear current and select new
    ADD,     // Keep current and add new
    TOGGLE   // Flip selection state of target
}

/**
 * Defines priority for event consumption.
 */
enum class InteractionPriority {
    HANDLE, // Resize/Rotate handles (Top)
    OBJECT, // Document content
    CANVAS  // Workspace background (Bottom)
}

/**
 * Result of a hit-test operation.
 */
data class HitTestResult(
    val hitType: HitType,
    val objectId: String? = null,
    val localPoint: Point2D? = null,
    val priority: InteractionPriority = InteractionPriority.CANVAS
)

enum class HitType { NONE, OBJECT, HANDLE }

/**
 * Encapsulates the set of selected objects.
 */
data class SelectionState(
    val selectedIds: Set<String> = emptySet(),
    val primaryId: String? = null
)
