package com.vilync.ophthalmicerp.feature.designer.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Orchestrates the rendering of the multi-layered document workspace.
 */
@Composable
fun DocumentCanvas(
    camera: ViewportCamera,
    mapper: CoordinateMapper,
    layers: List<CanvasLayer>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        layers.forEach { layer ->
            layer.draw(this, camera, mapper)
        }
    }
}
