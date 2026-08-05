package com.vilync.ophthalmicerp.core.document.engine

/**
 * Production implementation of the DocumentDataBinder.
 * Coordinates multiple providers to resolve dynamic fields in a fault-tolerant manner.
 */
class DefaultDocumentDataBinder(
    private val providers: List<DynamicFieldProvider>
) : DocumentDataBinder {

    override suspend fun bind(layout: RenderLayout, context: DocumentBindingContext): BindingResult {
        val startTime = System.currentTimeMillis()
        val resolvedFields = mutableMapOf<String, BindingValue>()
        val traces = mutableListOf<ProviderResolutionTrace>()

        // 1. Gather fields from all eligible providers
        providers.filter { it.supportedTypes.contains(context.documentType) }.forEach { provider ->
            val providerStart = System.currentTimeMillis()
            val fields = provider.getFields(context)
            val providerDuration = System.currentTimeMillis() - providerStart

            resolvedFields.putAll(fields)

            // Simplistic trace calculation for Sprint 9
            traces.add(
                ProviderResolutionTrace(
                    providerName = provider.name,
                    totalFieldsRequested = fields.size,
                    resolvedCount = fields.size,
                    missingCount = 0,
                    unsupportedCount = 0,
                    executionTimeMs = providerDuration
                )
            )
        }

        // 2. Perform the binding transformation
        var totalResolvedCount = 0
        var totalMissingCount = 0

        val boundPages = layout.pages.map { page ->
            val boundObjects = page.objects.map { obj ->
                if (obj is RenderDynamicField) {
                    val value = resolvedFields[obj.fieldId]
                    if (value != null) {
                        totalResolvedCount++
                        transformValueToRenderObject(obj, value)
                    } else {
                        totalMissingCount++
                        // Keep as dynamic field (unresolved)
                        obj
                    }
                } else {
                    obj
                }
            }
            page.copy(objects = boundObjects)
        }

        val duration = System.currentTimeMillis() - startTime
        
        val report = BindingReport(
            traces = traces,
            totalResolved = totalResolvedCount,
            totalMissing = totalMissingCount,
            executionTimeMs = duration
        )

        return BindingResult(
            layout = layout.copy(pages = boundPages),
            report = report,
            isSuccess = true
        )
    }

    private fun transformValueToRenderObject(field: RenderDynamicField, value: BindingValue): RenderObject {
        return when (value) {
            is BindingValue.Text -> RenderText(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                text = value.value
            )
            is BindingValue.Number -> RenderText(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                text = value.value.toString()
            )
            is BindingValue.Date -> RenderText(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                text = value.timestamp.toString()
            )
            is BindingValue.Image -> RenderImage(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                sourceUri = value.uri,
                sourcePath = value.path
            )
            is BindingValue.Barcode -> RenderBarcode(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                content = value.content,
                barcodeType = value.format.name
            )
            is BindingValue.QrCode -> RenderQrCode(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                content = value.content
            )
            is BindingValue.Table -> RenderTable(
                id = field.id,
                position = field.position,
                dimensions = field.dimensions,
                rotation = field.rotation,
                style = field.style,
                properties = mapOf("headers" to value.headers, "rows" to value.rows)
            )
        }
    }
}
