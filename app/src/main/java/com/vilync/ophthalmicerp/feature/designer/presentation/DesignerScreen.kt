package com.vilync.ophthalmicerp.feature.designer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.VerticalAlignCenter
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.ArrangeController
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignVerticalTop
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignVerticalCenter
import com.vilync.ophthalmicerp.feature.designer.domain.precision.AlignmentAction
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ContentCopy
import com.vilync.ophthalmicerp.core.document.domain.DocumentTemplate
import com.vilync.ophthalmicerp.feature.designer.presentation.components.DesignerWorkspace
import com.vilync.ophthalmicerp.feature.designer.presentation.components.inspector.PropertyInspector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignerScreen(
    viewModel: DesignerViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val docState by viewModel.docState.collectAsState()
    val layers by viewModel.layers.collectAsState()
    
    val propertyModel by viewModel.propertyModel.collectAsState()
    val propertyValues by viewModel.currentPropertyValues.collectAsState()
    val validationResults by viewModel.propertyValidationResults.collectAsState()
    
    val historyState by viewModel.historyState.collectAsState()
    val gridConfig by viewModel.gridConfig.collectAsState()
    val templateInfo by viewModel.templateInfo.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val density = LocalDensity.current
    val screenDpi = (density.density * 160).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.workspaceName ?: "Document Designer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Production Actions
                    IconButton(onClick = { viewModel.printDocument(0, "Invoice") }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = { /* PDF Export logic */ }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    
                    Spacer(Modifier.width(8.dp))

                    // Grid Controls
                    IconButton(onClick = { viewModel.toggleGridVisible(screenDpi) }) {
                        Icon(
                            Icons.Default.GridOn, 
                            contentDescription = "Toggle Grid",
                            tint = if (gridConfig.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Alignment Controls (Show only if multiple selected)
                    if ((docState?.interactionSession?.selection?.selectedIds?.size ?: 0) >= 2) {
                        IconButton(onClick = { viewModel.align(AlignmentAction.LEFT, screenDpi) }) {
                            Icon(Icons.Default.AlignHorizontalLeft, contentDescription = "Align Left")
                        }
                        IconButton(onClick = { viewModel.align(AlignmentAction.TOP, screenDpi) }) {
                            Icon(Icons.Default.AlignVerticalTop, contentDescription = "Align Top")
                        }
                        IconButton(onClick = { viewModel.align(AlignmentAction.HORIZONTAL_CENTER, screenDpi) }) {
                            Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Center Horizontal")
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.undo(screenDpi) },
                        enabled = historyState.canUndo
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo(screenDpi) },
                        enabled = historyState.canRedo
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                    
                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = { viewModel.copy() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = { viewModel.paste(screenDpi) }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                    IconButton(onClick = { viewModel.duplicate(screenDpi) }) {
                        Icon(Icons.Default.Layers, contentDescription = "Duplicate")
                    }
                    
                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = { viewModel.arrange(ArrangeController.OrderAction.BRING_TO_FRONT, screenDpi) }) {
                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "Bring to Front")
                    }
                    IconButton(onClick = { viewModel.arrange(ArrangeController.OrderAction.SEND_TO_BACK, screenDpi) }) {
                        Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Send to Back")
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(onClick = onDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    }
                    Button(
                        onClick = { viewModel.saveDocument(screenDpi) },
                        enabled = !isSaving,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Workspace (Canvas)
            Box(modifier = Modifier.weight(1f)) {
                docState?.let { state ->
                    DesignerWorkspace(
                        camera = session?.camera ?: com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera(),
                        onCameraChange = { viewModel.updateCamera(it, screenDpi) },
                        onPointerInput = { viewModel.handlePointerInput(it, screenDpi) },
                        onTranslationEvent = { viewModel.handleTranslation(it, screenDpi) },
                        onResizeEvent = { viewModel.handleResize(it, screenDpi) },
                        onRotationEvent = { viewModel.handleRotation(it, screenDpi) },
                        interactionSession = state.interactionSession,
                        layout = state.committedLayout,
                        documentManager = session?.documentManager ?: com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager(),
                        layers = layers,
                        screenDpi = screenDpi
                    )
                }
            }

            // 2. Property Inspector (Sidebar)
            PropertyInspector(
                model = propertyModel,
                currentValues = propertyValues,
                validationResults = validationResults,
                onPropertyChange = { viewModel.onPropertyChange(it, screenDpi) },
                onPropertyCommit = { viewModel.commitPropertyEdit() }
            )
        }
    }
}
