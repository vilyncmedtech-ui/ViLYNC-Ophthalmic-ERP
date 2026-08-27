package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper

/**
 * Responsible for finding interaction handles at a specific screen coordinate.
 */
interface HandleHitTestEngine {
    /**
     * Performs hit-testing against resize handles.
     */
    fun test(
        pointerWorkspacePos: Point2D,
        selectedIds: Set<String>,
        layout: RenderLayout,
        mapper: CoordinateMapper
    ): ResizeHandleResult

    /**
     * Performs hit-testing against the rotation handle.
     */
    fun testRotation(
        pointerWorkspacePos: Point2D,
        selectedIds: Set<String>,
        layout: RenderLayout,
        mapper: CoordinateMapper
    ): RotationHandleResult
}
