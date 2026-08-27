package com.vilync.ophthalmicerp.feature.designer.domain.session

import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.InteractionSession
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.TranslationSession
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.ResizeSession
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.RotationSession

/**
 * The editable working document model.
 * Owns temporary editing state and preserves architectural isolation.
 */
data class DesignerDocumentState(
    val committedLayout: RenderLayout,
    val interactionSession: InteractionSession = InteractionSession(),
    val translationSession: TranslationSession? = null,
    val resizeSession: ResizeSession? = null,
    val rotationSession: RotationSession? = null
)
