package com.vilync.ophthalmicerp.feature.designer.domain.history

import java.util.Stack

/**
 * Manages the undo and redo history stacks for the document designer.
 * Enforces a history limit to maintain optimal memory usage.
 */
class UndoManager(
    private val maxHistorySize: Int = 100
) {
    private val undoStack = Stack<UndoCommand>()
    private val redoStack = Stack<UndoCommand>()

    /**
     * Records a new command in the history.
     * Clears the redo stack when a new operation is performed.
     */
    fun push(command: UndoCommand) {
        // Coalescing logic: If 'before' and 'after' are the same reference, skip.
        if (command.beforeLayout === command.afterLayout) return

        undoStack.push(command)
        redoStack.clear()

        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
    }

    /**
     * Reverts the last command.
     * @return The previous RenderLayout or null if cannot undo.
     */
    fun undo(): UndoCommand? {
        if (undoStack.isEmpty()) return null
        
        val command = undoStack.pop()
        redoStack.push(command)
        return command
    }

    /**
     * Re-applies the last undone command.
     * @return The target RenderLayout or null if cannot redo.
     */
    fun redo(): UndoCommand? {
        if (redoStack.isEmpty()) return null
        
        val command = redoStack.pop()
        undoStack.push(command)
        return command
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun getUndoDescription(): String? = undoStack.lastOrNull()?.description
    fun getRedoDescription(): String? = redoStack.lastOrNull()?.description

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun getHistoryState(): HistoryState {
        return HistoryState(
            canUndo = canUndo(),
            canRedo = canRedo(),
            undoDescription = getUndoDescription(),
            redoDescription = getRedoDescription()
        )
    }
}
