package com.vilync.ophthalmicerp.feature.designer.domain.interaction

/**
 * Encapsulates the current state of user interaction with the designer.
 * Strictly decoupled from Document state.
 */
data class InteractionSession(
    val selection: SelectionState = SelectionState(),
    val currentMode: InteractionMode = InteractionMode.REPLACE,
    
    // Future extension points
    val isHovered: Boolean = false,
    val focusedId: String? = null
)
