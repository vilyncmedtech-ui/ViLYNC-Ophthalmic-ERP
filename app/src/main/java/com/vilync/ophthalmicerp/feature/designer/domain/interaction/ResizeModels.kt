package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout

/**
 * Supported resize handles for a single object.
 */
enum class ResizeHandleType {
    TL, T, TR, R, BR, B, BL, L
}

/**
 * Immutable state for an active resize operation.
 */
data class ResizeSession(
    val objectId: String,
    val initialPointerPos: Point2D,
    val initialDimensions: Dimensions,
    val initialPosition: Point2D,
    val handleType: ResizeHandleType,
    val anchorPoint: Point2D, // The point that remains fixed during resize
    val currentDimensions: Dimensions,
    val currentPosition: Point2D,
    val isResizing: Boolean = false
)

/**
 * Events that drive the resize pipeline.
 */
sealed class ResizeEvent {
    data class Start(
        val pointerPos: Point2D, 
        val handleType: ResizeHandleType, 
        val objectId: String, 
        val layout: RenderLayout
    ) : ResizeEvent()
    
    data class Move(val pointerPos: Point2D) : ResizeEvent()
    object Commit : ResizeEvent()
    object Cancel : ResizeEvent()
}

/**
 * Outcome of a resize operation.
 */
sealed class ResizeResult {
    data class Started(val session: ResizeSession) : ResizeResult()
    data class Updated(val session: ResizeSession, val preview: ResizePreview) : ResizeResult()
    data class Committed(val newLayout: RenderLayout) : ResizeResult()
    object Cancelled : ResizeResult()
}

/**
 * Derived data for real-time visual feedback.
 */
data class ResizePreview(
    val objectId: String,
    val dimensions: Dimensions,
    val position: Point2D,
    val handleType: ResizeHandleType
)

/**
 * Result of a handle hit-test.
 */
data class ResizeHandleResult(
    val hitType: HitType,
    val handleType: ResizeHandleType? = null,
    val objectId: String? = null
)

// =============================================================
// EXTENSION POINTS (POLICIES)
// =============================================================

interface MinimumSizePolicy {
    fun apply(dimensions: Dimensions): Dimensions
}

interface MaximumSizePolicy {
    fun apply(dimensions: Dimensions): Dimensions
}

interface AspectRatioPolicy {
    fun apply(dimensions: Dimensions, ratio: Double): Dimensions
}

interface FixedWidthPolicy {
    fun apply(dimensions: Dimensions): Dimensions
}

interface FixedHeightPolicy {
    fun apply(dimensions: Dimensions): Dimensions
}
