package com.vilync.ophthalmicerp.feature.designer.presentation.canvas

import com.vilync.ophthalmicerp.core.document.engine.PreviewRenderResult
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.domain.session.DesignerDocumentState
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GridConfig
import com.vilync.ophthalmicerp.feature.designer.domain.precision.GuideLine
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.layers.*
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.renderer.DefaultSelectionRenderer

/**
 * Defines the immutable z-order of workspace layers.
 */
enum class LayerType {
    BACKGROUND,
    GRID,
    PAPER,
    PREVIEW,
    GUIDES,
    SELECTION,
    TRANSLATION,
    RESIZE,
    ROTATION,
    UI_OVERLAY
}

/**
 * Responsible for constructing the declarative layer stack.
 */
object LayerRegistry {

    fun createStack(
        state: DesignerDocumentState,
        documentManager: DocumentManager,
        renderingBridge: RenderingBridge,
        screenDpi: Int,
        gridConfig: GridConfig,
        activeGuides: List<GuideLine>,
        translationPreview: com.vilync.ophthalmicerp.feature.designer.domain.interaction.TranslationPreview?,
        resizePreview: com.vilync.ophthalmicerp.feature.designer.domain.interaction.ResizePreview?,
        rotationPreview: com.vilync.ophthalmicerp.feature.designer.domain.interaction.RotationPreview?
    ): List<CanvasLayer> {
        return listOf(
            BackgroundLayer(),
            GridLayer(gridConfig),
            PaperLayer(documentManager),
            PreviewLayer(renderingBridge, documentManager, screenDpi),
            GuideLayer(activeGuides),
            SelectionLayer(
                state.interactionSession,
                DefaultSelectionRenderer(state.committedLayout, documentManager)
            ),
            TranslationOverlay(
                translationPreview,
                state.committedLayout,
                documentManager
            ),
            ResizeOverlay(
                state.interactionSession,
                resizePreview,
                state.committedLayout,
                documentManager
            ),
            RotationOverlay(
                state.interactionSession,
                rotationPreview,
                state.committedLayout,
                documentManager
            ),
            UIOverlayLayer()
        )
    }
}
