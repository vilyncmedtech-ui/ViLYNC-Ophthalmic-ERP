package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.geometry.StandardPageSizes
import com.vilync.ophthalmicerp.core.document.engine.binding.BindingEngine
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderResolver
import com.vilync.ophthalmicerp.core.document.template.DocumentTemplateRepository
import java.io.File

/**
 * High-level orchestrator for the ERP Document Pipeline.
 * Strictly coordinates existing engines to produce production-ready output.
 */
class DocumentRuntime(
    private val repository: DocumentTemplateRepository,
    private val resolver: PlaceholderResolver,
    private val bindingEngine: BindingEngine,
    private val layoutFactory: LayoutObjectFactory,
    private val renderingEngine: RenderingEngine,
    private val pdfRenderer: DefaultPdfRenderer,
    private val printRenderer: DefaultPrintRenderer
) {

    /**
     * Executes the full pipeline to generate a production PDF.
     */
    suspend fun generatePdf(
        entityId: Long,
        documentType: DesignerDocumentType,
        outputFile: File
    ): RenderResult {
        // 1. Fetch Template
        val template = repository.getTemplatesByContext(documentType, 0, 0).firstOrNull() 
            ?: return createErrorResult("No template assigned for ${documentType.name}")
            
        val version = repository.getActiveVersion(template.templateId)
            ?: return createErrorResult("No active version found for template ${template.templateName}")

        // 2. Resolve Data
        val context = DocumentBindingContext(entityId, documentType)
        val values = resolver.resolveAll(context)

        // 3. Map Template to Render Model
        val factoryResult = layoutFactory.create(version.layout)
        val baseLayout = when (factoryResult) {
            is FactoryResult.Success -> factoryResult.renderLayout
            is FactoryResult.Failure -> {
                return createErrorResult("Template mapping failed: ${factoryResult.errors.firstOrNull()?.message}")
            }
        }

        // 4. Bind Data
        val boundLayout = bindingEngine.bind(baseLayout, values)
        
        // 5. Process Rendering Instructions (Includes Invoice Table Decomposition)
        val renderSession = renderingEngine.process(boundLayout)
        
        // 6. Physical Output
        val renderContext = RuntimeRenderContext(
            dpi = 300, // Production Print DPI
            pageSize = boundLayout.pages.firstOrNull()?.dimensions ?: StandardPageSizes.A4,
            zoom = 1.0f
        )
        
        return pdfRenderer.renderSession(renderSession, renderContext, outputFile)
    }

    /**
     * Executes the full pipeline and returns a PrintDocumentAdapter for system printing.
     */
    suspend fun print(
        entityId: Long,
        documentType: DesignerDocumentType,
        documentTitle: String
    ): RenderResult {
        // 1. Fetch Template
        val template = repository.getTemplatesByContext(documentType, 0, 0).firstOrNull() 
            ?: return createErrorResult("No template assigned")
            
        val version = repository.getActiveVersion(template.templateId)
            ?: return createErrorResult("No active layout")

        // 2. Resolve Data
        val context = DocumentBindingContext(entityId, documentType)
        val values = resolver.resolveAll(context)

        // 3. Map & Bind
        val factoryResult = layoutFactory.create(version.layout)
        val boundLayout = when (factoryResult) {
            is FactoryResult.Success -> bindingEngine.bind(factoryResult.renderLayout, values)
            is FactoryResult.Failure -> return createErrorResult("Mapping failed")
        }

        // 4. Render
        val renderSession = renderingEngine.process(boundLayout)
        val renderContext = RuntimeRenderContext(
            dpi = 300,
            pageSize = boundLayout.pages.firstOrNull()?.dimensions ?: StandardPageSizes.A4,
            zoom = 1.0f
        )

        return printRenderer.createPrintAdapter(renderSession, renderContext, documentTitle)
    }

    private fun createErrorResult(msg: String) = object : RenderResult {
        override val isSuccess: Boolean = false
        override val errorCode: String = "ERR_RUNTIME"
        override val message: String = msg
        override val metadata: Map<String, String> = emptyMap()
    }

    private data class RuntimeRenderContext(
        override val dpi: Int,
        override val pageSize: com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions,
        override val zoom: Float,
        override val targetUnit: MeasurementUnit = MeasurementUnit.MILLIMETER,
        override val isGrayscale: Boolean = false
    ) : RenderContext {
        override fun getProperty(key: String): String? = null
    }
}
