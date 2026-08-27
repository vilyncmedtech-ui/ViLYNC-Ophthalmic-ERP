package com.vilync.ophthalmicerp.feature.designer.domain.session

import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager

/**
 * Lightweight state container for a designer session.
 */
data class DesignerSession(
    val layout: TemplateLayout,
    val camera: ViewportCamera = ViewportCamera(),
    val documentManager: DocumentManager = DocumentManager(),
    val workspaceName: String = "Untitled Document"
)
