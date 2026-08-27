package com.vilync.ophthalmicerp.feature.designer.domain.history

import com.vilync.ophthalmicerp.core.document.engine.RenderLayout

/**
 * Represents an undoable action in the designer.
 * Uses reference snapshots of RenderLayout for high performance and low memory overhead.
 */
data class UndoCommand(
    val description: String,
    val beforeLayout: RenderLayout,
    val afterLayout: RenderLayout
)

/**
 * Exposes the current status of the command history.
 */
data class HistoryState(
    val canUndo: Boolean,
    val canRedo: Boolean,
    val undoDescription: String? = null,
    val redoDescription: String? = null
)
