package com.vilync.ophthalmicerp.feature.designer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.document.domain.*
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.style.*
import com.vilync.ophthalmicerp.core.document.engine.*
import com.vilync.ophthalmicerp.core.document.template.*
import com.vilync.ophthalmicerp.feature.designer.domain.camera.ViewportCamera
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.interaction.*
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager
import com.vilync.ophthalmicerp.feature.designer.domain.property.*
import com.vilync.ophthalmicerp.feature.designer.domain.history.*
import com.vilync.ophthalmicerp.feature.designer.domain.clipboard.ClipboardManager
import com.vilync.ophthalmicerp.feature.designer.domain.precision.*
import com.vilync.ophthalmicerp.feature.designer.domain.session.DesignerDocumentState
import com.vilync.ophthalmicerp.feature.designer.domain.session.DesignerSession
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.CanvasLayer
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.LayerRegistry
import com.vilync.ophthalmicerp.feature.designer.presentation.canvas.RenderingBridge
import com.vilync.ophthalmicerp.core.document.engine.binding.BindingEngine
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderResolver
import com.vilync.ophthalmicerp.feature.designer.domain.binding.CompanyDynamicFieldProvider
import com.vilync.ophthalmicerp.feature.designer.domain.binding.SampleDynamicFieldProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Production ViewModel managing the Document Designer state, interaction, history, and runtime.
 */
class DesignerViewModel : ViewModel() {

    private val _docState = MutableStateFlow<DesignerDocumentState?>(null)
    val docState: StateFlow<DesignerDocumentState?> = _docState.asStateFlow()

    private val _session = MutableStateFlow<DesignerSession?>(null)
    val session: StateFlow<DesignerSession?> = _session.asStateFlow()

    // The data-bound layout used for high-fidelity rendering.
    private val _resolvedLayout = MutableStateFlow<RenderLayout?>(null)
    val resolvedLayout: StateFlow<RenderLayout?> = _resolvedLayout.asStateFlow()

    private val _previewResult = MutableStateFlow<PreviewRenderResult?>(null)
    
    private val _translationPreview = MutableStateFlow<TranslationPreview?>(null)
    private val _resizePreview = MutableStateFlow<ResizePreview?>(null)
    private val _rotationPreview = MutableStateFlow<RotationPreview?>(null)

    // Property Inspector State
    private val _propertyModel = MutableStateFlow<PropertyEditorModel?>(null)
    val propertyModel: StateFlow<PropertyEditorModel?> = _propertyModel.asStateFlow()

    private val _currentPropertyValues = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val currentPropertyValues: StateFlow<Map<String, Any?>> = _currentPropertyValues.asStateFlow()

    private val _propertyValidationResults = MutableStateFlow<Map<String, PropertyValidationResult>>(emptyMap())
    val propertyValidationResults: StateFlow<Map<String, PropertyValidationResult>> = _propertyValidationResults.asStateFlow()

    private val _layers = MutableStateFlow<List<CanvasLayer>>(emptyList())
    val layers: StateFlow<List<CanvasLayer>> = _layers.asStateFlow()

    // Precision Tools State
    private val _gridConfig = MutableStateFlow(GridConfig())
    val gridConfig: StateFlow<GridConfig> = _gridConfig.asStateFlow()

    private val _activeGuides = MutableStateFlow<List<GuideLine>>(emptyList())
    val activeGuides: StateFlow<List<GuideLine>> = _activeGuides.asStateFlow()

    // Metadata state
    private val _templateInfo = MutableStateFlow<DocumentTemplate?>(null)
    val templateInfo: StateFlow<DocumentTemplate?> = _templateInfo.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // History & Clipboard
    private val undoManager = UndoManager()
    val historyState: StateFlow<HistoryState> = flow {
        while (true) {
            emit(undoManager.getHistoryState())
            kotlinx.coroutines.delay(100)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryState(false, false))

    private val clipboardManager = ClipboardManager()
    private val arrangeController = ArrangeController()

    // Engines
    private lateinit var hitTestEngine: HitTestEngine
    private lateinit var handleHitTestEngine: HandleHitTestEngine
    private val selectionController: SelectionController = DefaultSelectionController()
    private val translationController: TranslationController = DefaultTranslationController()
    private val resizeController: ResizeController = DefaultResizeController()
    private val rotationController: RotationController = DefaultRotationController()
    
    private val propertyValidator: PropertyValidator = DefaultPropertyValidator()
    private val propertyEditingController: PropertyEditingController = DefaultPropertyEditingController()
    
    private val snapEngine = SnapEngine()
    private val smartGuideEngine = SmartGuideEngine()
    private val alignmentController = AlignmentController()
    
    private val bindingEngine = BindingEngine()
    private var placeholderResolver: PlaceholderResolver? = null
    
    private val renderingEngine: RenderingEngine = DefaultRenderingEngine()
    private val previewRenderer = DefaultPreviewRenderer()

    private var documentRuntime: DocumentRuntime? = null
    private var templateRepository: DocumentTemplateRepository? = null
    
    // Rendering Bridge
    private val renderingBridge = RenderingBridge(previewRenderer)

    // Synchronization Tracker
    private var lastModelProperties: Map<String, Any?> = emptyMap()
    
    // Property Editing Coalescing
    private var layoutBeforePropertyEdit: RenderLayout? = null

    private data class DesignerRenderContext(
        override val dpi: Int,
        override val pageSize: Dimensions,
        override val zoom: Float,
        override val targetUnit: MeasurementUnit = MeasurementUnit.MILLIMETER,
        override val isGrayscale: Boolean = false
    ) : RenderContext {
        override fun getProperty(key: String): String? = null
    }

    init {
        // 1. Initialize Resolver with Sample Data for Designer
        placeholderResolver = PlaceholderResolver(listOf(SampleDynamicFieldProvider()))

        // 2. Observe selection AND property changes in the working document
        viewModelScope.launch {
            docState.map { state ->
                val primaryId = state?.interactionSession?.selection?.primaryId
                val obj = state?.committedLayout?.pages?.flatMap { it.objects }?.find { it.id == primaryId }
                primaryId to obj?.let { extractPropertyValues(it) }
            }.distinctUntilChanged().collect { (id, props) ->
                syncPropertyInspector(id, props)
            }
        }

        // 3. Automated Binding & Rendering Pipeline
        viewModelScope.launch {
            docState.map { it?.committedLayout }.distinctUntilChanged().collect { layout ->
                if (layout != null) {
                    // 1. Resolve Data
                    val context = DocumentBindingContext(0, DesignerDocumentType.SALES_INVOICE) 
                    val values = placeholderResolver?.resolveAll(context) ?: emptyMap()
                    
                    // 2. Bind Layout
                    val boundLayout = bindingEngine.bind(layout, values)
                    _resolvedLayout.value = boundLayout
                    
                    // 3. Process Rendering Instructions
                    val renderSession = renderingEngine.process(boundLayout)
                    
                    // 4. Create Preview
                    val currentZoom = (_session.value?.camera?.zoom ?: 1.0).toFloat()
                    val renderContext = DesignerRenderContext(
                        dpi = 160, 
                        zoom = currentZoom,
                        pageSize = boundLayout.pages.firstOrNull()?.dimensions ?: Dimensions(210.0, 297.0)
                    )
                    val preview = previewRenderer.createPreview(renderSession, renderContext)
                    
                    // 5. Update UI
                    _previewResult.value = preview
                    renderingBridge.updateResult(preview)
                    rebuildLayers(160)
                }
            }
        }
    }

    fun initialize(
        layout: TemplateLayout, 
        renderLayout: RenderLayout,
        template: DocumentTemplate? = null,
        repository: DocumentTemplateRepository? = null,
        runtime: DocumentRuntime? = null
    ) {
        val manager = DocumentManager()
        manager.layoutPages(layout)
        
        _session.value = DesignerSession(
            layout = layout,
            documentManager = manager
        )
        
        _docState.value = DesignerDocumentState(
            committedLayout = injectSampleTable(renderLayout)
        )
        
        _templateInfo.value = template
        this.templateRepository = repository
        this.documentRuntime = runtime
        
        hitTestEngine = DefaultHitTestEngine(manager)
        handleHitTestEngine = DefaultHandleHitTestEngine(manager)
        
        undoManager.clear()
        
        rebuildLayers(160) // Default DPI for initialization
    }

    private fun injectSampleTable(layout: RenderLayout): RenderLayout {
        val config = InvoiceTableConfig(
            columns = listOf(
                InvoiceColumnDefinition("sno", "S.No", 15.0, HorizontalAlignment.CENTER),
                InvoiceColumnDefinition("desc", "Product Description", 80.0, HorizontalAlignment.LEFT),
                InvoiceColumnDefinition("hsn", "HSN", 25.0, HorizontalAlignment.CENTER),
                InvoiceColumnDefinition("qty", "Qty", 20.0, HorizontalAlignment.RIGHT),
                InvoiceColumnDefinition("rate", "Rate", 25.0, HorizontalAlignment.RIGHT),
                InvoiceColumnDefinition("amount", "Amount", 30.0, HorizontalAlignment.RIGHT)
            )
        )

        val table = RenderInvoiceTable(
            id = "ItemTable",
            position = Point2D(5.0, 100.0), // Below header info
            dimensions = Dimensions(195.0, 50.0), // Full width A4 (210 - 15 margins)
            rotation = 0f,
            style = ResolvedStyle(fontSize = 9.0),
            config = config
        )

        val updatedPages = layout.pages.map { page ->
            if (page.index == 0) {
                page.copy(objects = page.objects + table)
            } else page
        }
        return layout.copy(pages = updatedPages)
    }

    // =========================================================
    // PERSISTENCE ACTIONS
    // =========================================================

    fun saveDocument(screenDpi: Int) {
        val template = _templateInfo.value ?: return
        val layout = _session.value?.layout ?: return
        val repository = templateRepository ?: return
        
        commitPropertyEdit()
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveTemplate(template, layout)
                _isSaving.value = false
            } catch (e: Exception) {
                _isSaving.value = false
            }
        }
    }

    fun duplicateDocument(newName: String) {
        val template = _templateInfo.value ?: return
        val repository = templateRepository ?: return
        
        viewModelScope.launch {
            try {
                repository.duplicateTemplate(template.templateId, newName)
            } catch (e: Exception) {
            }
        }
    }

    fun deleteDocument() {
        val template = _templateInfo.value ?: return
        val repository = templateRepository ?: return
        
        viewModelScope.launch {
            try {
                repository.deleteTemplate(template.templateId)
            } catch (e: Exception) {
            }
        }
    }

    fun setAsDefault(isDefault: Boolean) {
        val template = _templateInfo.value ?: return
        val repository = templateRepository ?: return
        
        viewModelScope.launch {
            try {
                repository.setDefaultTemplate(template.templateId, isDefault)
                _templateInfo.value = template.copy(isDefault = isDefault)
            } catch (e: Exception) {
            }
        }
    }

    fun printDocument(entityId: Long, title: String) {
        val runtime = documentRuntime ?: return
        val template = _templateInfo.value ?: return
        
        viewModelScope.launch {
            val result = runtime.print(entityId, template.documentType, title)
            // Handle result (e.g., notify UI to launch print dialog)
        }
    }

    fun exportPdf(entityId: Long, outputFile: java.io.File) {
        val runtime = documentRuntime ?: return
        val template = _templateInfo.value ?: return
        
        viewModelScope.launch {
            val result = runtime.generatePdf(entityId, template.documentType, outputFile)
            // Handle result
        }
    }

    private fun syncPropertyInspector(objectId: String?, props: Map<String, Any?>?) {
        if (objectId == null || props == null) {
            _propertyModel.value = null
            _currentPropertyValues.value = emptyMap()
            _propertyValidationResults.value = emptyMap()
            lastModelProperties = emptyMap()
            return
        }

        if (_propertyModel.value?.objectId != objectId) {
            val layout = _docState.value?.committedLayout ?: return
            val obj = layout.pages.flatMap { it.objects }.find { it.id == objectId } ?: return
            _propertyModel.value = PropertyEditorModel.fromObject(obj)
            _propertyValidationResults.value = emptyMap()
        }

        val currentUiValues = _currentPropertyValues.value
        val mergedValues = currentUiValues.toMutableMap()
        
        props.forEach { (key, modelValue) ->
            val prevModelValue = lastModelProperties[key]
            if (modelValue != prevModelValue || !currentUiValues.containsKey(key)) {
                mergedValues[key] = modelValue
            }
        }

        _currentPropertyValues.value = mergedValues
        lastModelProperties = props
    }

    private fun extractPropertyValues(obj: RenderObject): Map<String, Any?> {
        return mapOf(
            "x" to obj.position.x,
            "y" to obj.position.y,
            "width" to obj.dimensions.width,
            "height" to obj.dimensions.height,
            "rotation" to obj.rotation.toDouble(),
            "text" to (obj as? RenderText)?.text
        )
    }

    // =========================================================
    // PROPERTY EDITING
    // =========================================================

    fun onPropertyChange(change: PropertyChange, screenDpi: Int) {
        val model = _propertyModel.value ?: return
        val descriptor = model.groups.flatMap { it.descriptors }.find { it.id == change.propertyId } ?: return

        _currentPropertyValues.value = _currentPropertyValues.value + (change.propertyId to change.newValue)

        val result = propertyValidator.validate(change, descriptor)
        _propertyValidationResults.value = _propertyValidationResults.value + (change.propertyId to result)

        if (result.isValid) {
            val currentState = _docState.value ?: return
            if (layoutBeforePropertyEdit == null) {
                layoutBeforePropertyEdit = currentState.committedLayout
            }
            _docState.value = propertyEditingController.applyChange(currentState, change)
            renderingBridge.updateResult(null) 
            rebuildLayers(screenDpi)
        }
    }

    fun commitPropertyEdit() {
        val before = layoutBeforePropertyEdit ?: return
        val after = _docState.value?.committedLayout ?: return
        if (before !== after) {
            undoManager.push(UndoCommand("Edit Properties", before, after))
        }
        layoutBeforePropertyEdit = null
    }

    // =========================================================
    // HISTORY ACTIONS
    // =========================================================

    fun undo(screenDpi: Int) {
        val command = undoManager.undo() ?: return
        val currentState = _docState.value ?: return
        _docState.value = currentState.copy(committedLayout = command.beforeLayout)
        renderingBridge.updateResult(null)
        rebuildLayers(screenDpi)
    }

    fun redo(screenDpi: Int) {
        val command = undoManager.redo() ?: return
        val currentState = _docState.value ?: return
        _docState.value = currentState.copy(committedLayout = command.afterLayout)
        renderingBridge.updateResult(null)
        rebuildLayers(screenDpi)
    }

    // =========================================================
    // GRID CONFIGURATION
    // =========================================================

    fun toggleGridVisible(screenDpi: Int) {
        _gridConfig.value = _gridConfig.value.copy(isVisible = !_gridConfig.value.isVisible)
        rebuildLayers(screenDpi)
    }

    fun toggleGridEnabled() {
        _gridConfig.value = _gridConfig.value.copy(isEnabled = !_gridConfig.value.isEnabled)
    }

    fun updateGridSize(sizeMm: Double, screenDpi: Int) {
        _gridConfig.value = _gridConfig.value.copy(sizeMm = sizeMm.coerceAtLeast(1.0))
        rebuildLayers(screenDpi)
    }

    // =========================================================
    // CLIPBOARD ACTIONS
    // =========================================================

    fun copy() {
        val state = _docState.value ?: return
        val selectedIds = state.interactionSession.selection.selectedIds
        val selectedObjects = state.committedLayout.pages.flatMap { it.objects }.filter { selectedIds.contains(it.id) }
        clipboardManager.copy(selectedObjects)
    }

    fun cut(screenDpi: Int) {
        val state = _docState.value ?: return
        val before = state.committedLayout
        copy()
        val selectedIds = state.interactionSession.selection.selectedIds
        val updatedPages = before.pages.map { page ->
            page.copy(objects = page.objects.filterNot { selectedIds.contains(it.id) })
        }
        val after = before.copy(pages = updatedPages)
        applyAndRecord("Cut", before, after, screenDpi)
    }

    fun paste(screenDpi: Int) {
        val state = _docState.value ?: return
        val before = state.committedLayout
        val pasted = clipboardManager.paste()
        if (pasted.isEmpty()) return
        val updatedPages = before.pages.toMutableList()
        if (updatedPages.isNotEmpty()) {
            updatedPages[0] = updatedPages[0].copy(objects = updatedPages[0].objects + pasted)
        }
        val after = before.copy(pages = updatedPages)
        val newIds = pasted.map { it.id }.toSet()
        val updatedInteraction = state.interactionSession.copy(
            selection = SelectionState(selectedIds = newIds, primaryId = newIds.firstOrNull())
        )
        _docState.value = state.copy(committedLayout = after, interactionSession = updatedInteraction)
        undoManager.push(UndoCommand("Paste", before, after))
        renderingBridge.updateResult(null)
        rebuildLayers(screenDpi)
    }

    fun duplicate(screenDpi: Int) {
        copy()
        paste(screenDpi)
    }

    // =========================================================
    // ALIGNMENT ACTIONS
    // =========================================================

    fun align(action: AlignmentAction, screenDpi: Int) {
        val state = _docState.value ?: return
        val before = state.committedLayout
        val selectedIds = state.interactionSession.selection.selectedIds
        val after = alignmentController.align(before, selectedIds, action)
        applyAndRecord("Align Objects", before, after, screenDpi)
    }

    // =========================================================
    // ARRANGE ACTIONS
    // =========================================================

    fun arrange(action: ArrangeController.OrderAction, screenDpi: Int) {
        val state = _docState.value ?: return
        val before = state.committedLayout
        val selectedIds = state.interactionSession.selection.selectedIds
        val after = arrangeController.arrange(before, selectedIds, action)
        applyAndRecord("Arrange", before, after, screenDpi)
    }

    private fun applyAndRecord(desc: String, before: RenderLayout, after: RenderLayout, screenDpi: Int) {
        val currentState = _docState.value ?: return
        if (before !== after) {
            _docState.value = currentState.copy(committedLayout = after)
            undoManager.push(UndoCommand(desc, before, after))
            renderingBridge.updateResult(null)
            rebuildLayers(screenDpi)
        }
    }

    fun updateCamera(camera: ViewportCamera, screenDpi: Int) {
        _session.value = _session.value?.copy(camera = camera)
        rebuildLayers(screenDpi)
    }

    fun updatePreviewResult(result: PreviewRenderResult?, screenDpi: Int) {
        _previewResult.value = result
        renderingBridge.updateResult(result)
        rebuildLayers(screenDpi)
    }

    fun handlePointerInput(input: PointerInput, screenDpi: Int) {
        val currentState = _docState.value ?: return
        if (input.action == PointerAction.UP) {
            val hitResult = hitTestEngine.test(input.position, currentState.committedLayout)
            val updatedInteraction = selectionController.onHit(currentState.interactionSession, hitResult)
            _docState.value = currentState.copy(interactionSession = updatedInteraction)
            rebuildLayers(screenDpi)
        }
    }

    fun handleTranslation(event: TranslationEvent, screenDpi: Int) {
        val currentState = _docState.value ?: return
        val before = currentState.committedLayout
        val result = translationController.handleEvent(event, currentState.translationSession)
        when (result) {
            is TranslationResult.Started -> {
                _docState.value = currentState.copy(translationSession = result.session)
                _activeGuides.value = smartGuideEngine.calculateGuides(before, result.session.initialPositions.keys)
            }
            is TranslationResult.Updated -> {
                val session = result.session
                var offset = session.currentOffsetMm
                offset = snapEngine.snapToGrid(offset, _gridConfig.value)
                val primaryId = currentState.interactionSession.selection.primaryId
                val primaryInitialPos = session.initialPositions[primaryId]
                if (primaryInitialPos != null) {
                    val currentPos = Point2D(primaryInitialPos.x + offset.x, primaryInitialPos.y + offset.y)
                    val verticalGuides = _activeGuides.value.filter { it.orientation == GuideLine.Orientation.VERTICAL }.map { it.position }
                    val horizontalGuides = _activeGuides.value.filter { it.orientation == GuideLine.Orientation.HORIZONTAL }.map { it.position }
                    val snappedX = snapEngine.snapToGuides(currentPos.x, verticalGuides)
                    val snappedY = snapEngine.snapToGuides(currentPos.y, horizontalGuides)
                    offset = offset.copy(x = offset.x + (snappedX - currentPos.x), y = offset.y + (snappedY - currentPos.y))
                }
                val updatedSession = session.copy(currentOffsetMm = offset)
                val preview = TranslationPreview(objectOffsets = session.initialPositions.mapValues { (_, pos) -> Point2D(pos.x + offset.x, pos.y + offset.y) })
                _docState.value = currentState.copy(translationSession = updatedSession)
                _translationPreview.value = preview
            }
            is TranslationResult.Committed -> {
                val after = applyTranslationToLayout(before, currentState.translationSession)
                applyAndRecord("Move Object", before, after, screenDpi)
                _translationPreview.value = null
                _activeGuides.value = emptyList()
                _docState.value = _docState.value?.copy(translationSession = null)
            }
            is TranslationResult.Cancelled -> {
                _docState.value = currentState.copy(translationSession = null)
                _translationPreview.value = null
                _activeGuides.value = emptyList()
            }
        }
        rebuildLayers(screenDpi)
    }

    fun handleResize(event: ResizeEvent, screenDpi: Int) {
        val currentState = _docState.value ?: return
        val before = currentState.committedLayout
        val result = resizeController.handleEvent(event, currentState.resizeSession)
        when (result) {
            is ResizeResult.Started -> {
                _docState.value = currentState.copy(resizeSession = result.session)
                _activeGuides.value = smartGuideEngine.calculateGuides(before, setOf(result.session.objectId))
            }
            is ResizeResult.Updated -> {
                val session = result.session
                var pos = session.currentPosition
                var dim = session.currentDimensions
                if (_gridConfig.value.isEnabled) {
                    val gridSize = _gridConfig.value.sizeMm
                    val snappedW = round(dim.width / gridSize) * gridSize
                    val snappedH = round(dim.height / gridSize) * gridSize
                    dim = dim.copy(width = snappedW.coerceAtLeast(1.0), height = snappedH.coerceAtLeast(1.0))
                }
                val updatedSession = session.copy(currentDimensions = dim, currentPosition = pos)
                val preview = ResizePreview(objectId = session.objectId, dimensions = dim, position = pos, handleType = session.handleType)
                _docState.value = currentState.copy(resizeSession = updatedSession)
                _resizePreview.value = preview
            }
            is ResizeResult.Committed -> {
                val after = applyResizeToLayout(before, currentState.resizeSession)
                applyAndRecord("Resize Object", before, after, screenDpi)
                _resizePreview.value = null
                _activeGuides.value = emptyList()
                _docState.value = _docState.value?.copy(resizeSession = null)
            }
            is ResizeResult.Cancelled -> {
                _docState.value = currentState.copy(resizeSession = null)
                _resizePreview.value = null
                _activeGuides.value = emptyList()
            }
        }
        rebuildLayers(screenDpi)
    }

    fun handleRotation(event: RotationEvent, screenDpi: Int) {
        val currentState = _docState.value ?: return
        val before = currentState.committedLayout
        val result = rotationController.handleEvent(event, currentState.rotationSession)
        when (result) {
            is RotationResult.Started -> {
                _docState.value = currentState.copy(rotationSession = result.session)
            }
            is RotationResult.Updated -> {
                _docState.value = currentState.copy(rotationSession = result.session)
                _rotationPreview.value = result.preview
            }
            is RotationResult.Committed -> {
                val after = applyRotationToLayout(before, currentState.rotationSession)
                applyAndRecord("Rotate Object", before, after, screenDpi)
                _rotationPreview.value = null
                _docState.value = _docState.value?.copy(rotationSession = null)
            }
            is RotationResult.Cancelled -> {
                _docState.value = currentState.copy(rotationSession = null)
                _rotationPreview.value = null
            }
        }
        rebuildLayers(screenDpi)
    }

    fun testHandleHit(pos: Point2D, mapper: CoordinateMapper): ResizeHandleResult {
        val currentState = _docState.value ?: return ResizeHandleResult(HitType.NONE)
        return handleHitTestEngine.test(pointerWorkspacePos = pos, selectedIds = currentState.interactionSession.selection.selectedIds, layout = currentState.committedLayout, mapper = mapper)
    }

    fun testRotationHandleHit(pos: Point2D, mapper: CoordinateMapper): RotationHandleResult {
        val currentState = _docState.value ?: return RotationHandleResult(HitType.NONE)
        return handleHitTestEngine.testRotation(pointerWorkspacePos = pos, selectedIds = currentState.interactionSession.selection.selectedIds, layout = currentState.committedLayout, mapper = mapper)
    }

    private fun applyTranslationToLayout(layout: RenderLayout, session: TranslationSession?): RenderLayout {
        if (session == null) return layout
        val offset = session.currentOffsetMm
        val pages = layout.pages.map { page ->
            val updatedObjects = page.objects.map { obj -> if (session.initialPositions.containsKey(obj.id)) updateObjectPosition(obj, offset) else obj }
            page.copy(objects = updatedObjects)
        }
        return layout.copy(pages = pages)
    }

    private fun applyResizeToLayout(layout: RenderLayout, session: ResizeSession?): RenderLayout {
        if (session == null) return layout
        val pages = layout.pages.map { page ->
            val updatedObjects = page.objects.map { obj -> if (obj.id == session.objectId) updateObjectBounds(obj, session.currentPosition, session.currentDimensions) else obj }
            page.copy(objects = updatedObjects)
        }
        return layout.copy(pages = pages)
    }

    private fun applyRotationToLayout(layout: RenderLayout, session: RotationSession?): RenderLayout {
        if (session == null) return layout
        val pages = layout.pages.map { page ->
            val updatedObjects = page.objects.map { obj -> if (obj.id == session.objectId) updateObjectRotation(obj, session.currentAngle.toFloat()) else obj }
            page.copy(objects = updatedObjects)
        }
        return layout.copy(pages = pages)
    }

    private fun updateObjectPosition(obj: RenderObject, offset: Point2D): RenderObject {
        val newPos = Point2D(x = obj.position.x + offset.x, y = obj.position.y + offset.y, unit = obj.position.unit)
        return when (obj) {
            is RenderText -> obj.copy(position = newPos)
            is RenderImage -> obj.copy(position = newPos)
            is RenderShape -> obj.copy(position = newPos)
            is RenderBarcode -> obj.copy(position = newPos)
            is RenderQrCode -> obj.copy(position = newPos)
            is RenderTable -> obj.copy(position = newPos)
            is RenderInvoiceTable -> obj.copy(position = newPos)
            is RenderDynamicField -> obj.copy(position = newPos)
        }
    }

    private fun updateObjectBounds(obj: RenderObject, pos: Point2D, dim: Dimensions): RenderObject {
        return when (obj) {
            is RenderText -> obj.copy(position = pos, dimensions = dim)
            is RenderImage -> obj.copy(position = pos, dimensions = dim)
            is RenderShape -> obj.copy(position = pos, dimensions = dim)
            is RenderBarcode -> obj.copy(position = pos, dimensions = dim)
            is RenderQrCode -> obj.copy(position = pos, dimensions = dim)
            is RenderTable -> obj.copy(position = pos, dimensions = dim)
            is RenderInvoiceTable -> obj.copy(position = pos, dimensions = dim)
            is RenderDynamicField -> obj.copy(position = pos, dimensions = dim)
        }
    }

    private fun updateObjectRotation(obj: RenderObject, rotation: Float): RenderObject {
        return when (obj) {
            is RenderText -> obj.copy(rotation = rotation)
            is RenderImage -> obj.copy(rotation = rotation)
            is RenderShape -> obj.copy(rotation = rotation)
            is RenderBarcode -> obj.copy(rotation = rotation)
            is RenderQrCode -> obj.copy(rotation = rotation)
            is RenderTable -> obj.copy(rotation = rotation)
            is RenderInvoiceTable -> obj.copy(rotation = rotation)
            is RenderDynamicField -> obj.copy(rotation = rotation)
        }
    }

    private fun rebuildLayers(screenDpi: Int) {
        val currentSession = _session.value ?: return
        val currentState = _docState.value ?: return
        _layers.value = LayerRegistry.createStack(state = currentState, documentManager = currentSession.documentManager, renderingBridge = renderingBridge, screenDpi = screenDpi, gridConfig = _gridConfig.value, activeGuides = _activeGuides.value, translationPreview = _translationPreview.value, resizePreview = _resizePreview.value, rotationPreview = _rotationPreview.value)
    }

    override fun onCleared() {
        renderingBridge.dispose()
    }
}
