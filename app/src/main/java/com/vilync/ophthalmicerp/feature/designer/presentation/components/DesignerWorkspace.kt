package com.vilync.ophthalmicerp.feature.designer.presentation.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.camera.CameraController
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.GeometryTransform
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.*
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer

/**
 * Top-level container for the interactive document canvas.
 * Handles camera control, object interaction, resizing, and rotation.
 */
@Composable
fun DesignerWorkspace(
    camera: ViewportCamera,
    onCameraChange: (ViewportCamera) -> Unit,
    onPointerInput: (PointerInput) -> Unit,
    onTranslationEvent: (TranslationEvent) -> Unit,
    onResizeEvent: (ResizeEvent) -> Unit,
    onRotationEvent: (RotationEvent) -> Unit,
    interactionSession: InteractionSession,
    layout: RenderLayout?,
    documentManager: DocumentManager,
    layers: List<CanvasLayer>,
    screenDpi: Int,
    modifier: Modifier = Modifier
) {
    val controller = remember { CameraController() }
    val mapper = remember(camera, screenDpi) { CoordinateMapper(camera, screenDpi) }
    val hitTestEngine = remember(documentManager) { DefaultHitTestEngine(documentManager) }
    val handleHitTestEngine = remember(documentManager) { DefaultHandleHitTestEngine(documentManager) }
    
    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(camera, layout, interactionSession) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val workspacePos = mapper.screenToWorkspace(Point2D(offset.x.toDouble(), offset.y.toDouble()))
                        if (layout != null) {
                            // 1. Priority: Rotation Handle
                            val rotationHit = handleHitTestEngine.testRotation(workspacePos, interactionSession.selection.selectedIds, layout, mapper)
                            if (rotationHit.hitType == HitType.HANDLE && rotationHit.objectId != null) {
                                dragMode = DragMode.ROTATE
                                val center = calculateObjectCenterMm(rotationHit.objectId, layout, documentManager)
                                onRotationEvent(RotationEvent.Start(workspacePos, rotationHit.objectId, layout, center))
                                return@detectDragGestures
                            }

                            // 2. Priority: Resize Handles
                            val handleHit = handleHitTestEngine.test(workspacePos, interactionSession.selection.selectedIds, layout, mapper)
                            if (handleHit.hitType == HitType.HANDLE && handleHit.handleType != null && handleHit.objectId != null) {
                                dragMode = DragMode.RESIZE
                                onResizeEvent(ResizeEvent.Start(workspacePos, handleHit.handleType, handleHit.objectId, layout))
                                return@detectDragGestures
                            }
                            
                            // 3. Priority: Objects (Translation)
                            val hit = hitTestEngine.test(workspacePos, layout)
                            if (hit.hitType == HitType.OBJECT && interactionSession.selection.selectedIds.contains(hit.objectId)) {
                                dragMode = DragMode.MOVE
                                onTranslationEvent(TranslationEvent.Start(
                                    pointerPos = workspacePos,
                                    selectedIds = interactionSession.selection.selectedIds,
                                    layout = layout
                                ))
                            } else {
                                dragMode = DragMode.PAN
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val workspacePos = mapper.screenToWorkspace(Point2D(change.position.x.toDouble(), change.position.y.toDouble()))
                        when (dragMode) {
                            DragMode.ROTATE -> onRotationEvent(RotationEvent.Move(workspacePos))
                            DragMode.RESIZE -> onResizeEvent(ResizeEvent.Move(workspacePos))
                            DragMode.MOVE -> onTranslationEvent(TranslationEvent.Move(workspacePos))
                            DragMode.PAN -> {
                                val updated = controller.pan(camera, Point2D(dragAmount.x.toDouble(), dragAmount.y.toDouble()), screenDpi)
                                onCameraChange(updated)
                            }
                            DragMode.NONE -> {}
                        }
                    },
                    onDragEnd = {
                        when (dragMode) {
                            DragMode.ROTATE -> onRotationEvent(RotationEvent.Commit)
                            DragMode.RESIZE -> onResizeEvent(ResizeEvent.Commit)
                            DragMode.MOVE -> onTranslationEvent(TranslationEvent.Commit)
                            else -> {}
                        }
                        dragMode = DragMode.NONE
                    },
                    onDragCancel = {
                        when (dragMode) {
                            DragMode.ROTATE -> onRotationEvent(RotationEvent.Cancel)
                            DragMode.RESIZE -> onResizeEvent(ResizeEvent.Cancel)
                            DragMode.MOVE -> onTranslationEvent(TranslationEvent.Cancel)
                            else -> {}
                        }
                        dragMode = DragMode.NONE
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val workspacePos = mapper.screenToWorkspace(Point2D(offset.x.toDouble(), offset.y.toDouble()))
                    onPointerInput(
                        PointerInput(
                            position = workspacePos,
                            type = PointerType.TOUCH,
                            action = PointerAction.UP
                        )
                    )
                }
            }
            .pointerInput(camera) {
                detectTransformGestures { centroid, _, zoom, _ ->
                    if (zoom != 1f) {
                        val focusWorkspace = mapper.screenToWorkspace(Point2D(centroid.x.toDouble(), centroid.y.toDouble()))
                        val updated = controller.zoom(camera, zoom.toDouble(), focusWorkspace)
                        onCameraChange(updated)
                    }
                }
            }
    ) {
        DocumentCanvas(
            camera = camera,
            mapper = mapper,
            layers = layers,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun calculateObjectCenterMm(id: String, layout: RenderLayout, documentManager: DocumentManager): Point2D {
    for (pageIndex in layout.pages.indices) {
        val page = layout.pages[pageIndex]
        val obj = page.objects.find { it.id == id } ?: continue
        
        val pageOffset = documentManager.getPageLocation(pageIndex) ?: continue
        val layoutPage = documentManager.getLayoutPage(pageIndex) ?: continue
        
        val pageTopLeft = Point2D(
            x = pageOffset.x - layoutPage.width / 2.0,
            y = pageOffset.y - layoutPage.height / 2.0,
            unit = com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit.MILLIMETER
        )
        
        val objTopLeftMm = Point2D(
            x = pageTopLeft.x + obj.position.x,
            y = pageTopLeft.y + obj.position.y,
            unit = pageTopLeft.unit
        )
        
        return GeometryTransform.calculateCenter(objTopLeftMm, obj.dimensions)
    }
    return Point2D(0.0, 0.0)
}

private enum class DragMode { NONE, PAN, MOVE, RESIZE, ROTATE }
