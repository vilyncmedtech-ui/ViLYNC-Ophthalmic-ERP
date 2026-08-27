package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.style.*
import com.vilync.ophthalmicerp.core.document.domain.LayoutObject
import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout
import java.util.Locale

/**
 * Production implementation of the LayoutObjectFactory.
 * 
 * Performs deterministic mapping from loose JSON properties to immutable render objects.
 * Now integrates the StyleResolver for strongly-typed styling.
 */
class DefaultLayoutObjectFactory(
    private val styleResolver: StyleResolver = DefaultStyleResolver()
) : LayoutObjectFactory {

    override fun create(layout: TemplateLayout): FactoryResult {
        val errors = mutableListOf<FactoryError>()
        
        // Resolve Template Defaults (if any) from settings
        val templateDefaults = resolveTemplateDefaults(layout.settings)
        
        val renderPages = layout.pages.map { page ->
            val renderObjects = mutableListOf<RenderObject>()
            
            page.objects.forEach { obj ->
                when (val result = mapObject(obj, templateDefaults)) {
                    is MappingResult.Success -> renderObjects.add(result.renderObject)
                    is MappingResult.Failure -> errors.add(result.error)
                }
            }
            
            RenderPage(
                index = page.index,
                dimensions = Dimensions(page.width.toDouble(), page.height.toDouble(), MeasurementUnit.MILLIMETER),
                objects = renderObjects
            )
        }

        return if (errors.isEmpty()) {
            FactoryResult.Success(RenderLayout(pages = renderPages, metadata = layout.settings))
        } else {
            FactoryResult.Failure(errors)
        }
    }

    private fun resolveTemplateDefaults(settings: Map<String, String>): ResolvedStyle {
        val result = styleResolver.resolve(settings)
        return when (result) {
            is StyleResolutionResult.Success -> result.style
            is StyleResolutionResult.Failure -> ResolvedStyle() // Fallback to engine defaults
        }
    }

    private fun mapObject(obj: LayoutObject, templateDefaults: ResolvedStyle): MappingResult {
        val position = Point2D(obj.x.toDouble(), obj.y.toDouble(), MeasurementUnit.MILLIMETER)
        val dimensions = Dimensions(obj.width.toDouble(), obj.height.toDouble(), MeasurementUnit.MILLIMETER)
        
        // Resolve specific object style cascading from template defaults
        val styleResult = styleResolver.resolve(obj.styles, templateDefaults)
        val resolvedStyle = when (styleResult) {
            is StyleResolutionResult.Success -> styleResult.style
            is StyleResolutionResult.Failure -> {
                return MappingResult.Failure(
                    FactoryError(
                        objectId = obj.id,
                        message = "Style resolution failed: ${styleResult.errors.firstOrNull()?.message}",
                        code = "ERR_STYLE_RESOLUTION"
                    )
                )
            }
        }

        return when (obj.type.uppercase(Locale.ROOT)) {
            "TEXT" -> MappingResult.Success(
                RenderText(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    text = obj.properties["text"]?.toString() ?: ""
                )
            )
            "IMAGE" -> MappingResult.Success(
                RenderImage(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    sourceUri = obj.properties["sourceUri"]?.toString(),
                    sourcePath = obj.properties["sourcePath"]?.toString()
                )
            )
            "RECTANGLE", "CIRCLE", "LINE" -> MappingResult.Success(
                RenderShape(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    shapeType = RenderShape.ShapeType.valueOf(obj.type.uppercase(Locale.ROOT))
                )
            )
            "BARCODE" -> MappingResult.Success(
                RenderBarcode(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    content = obj.properties["content"]?.toString() ?: "",
                    barcodeType = obj.properties["barcodeType"]?.toString() ?: "CODE128"
                )
            )
            "QR_CODE" -> MappingResult.Success(
                RenderQrCode(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    content = obj.properties["content"]?.toString() ?: ""
                )
            )
            "TABLE" -> MappingResult.Success(
                RenderTable(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    properties = obj.properties
                )
            )
            "INVOICE_TABLE" -> MappingResult.Success(
                RenderInvoiceTable(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    config = resolveTableConfig(obj.properties)
                )
            )
            "DYNAMIC_FIELD" -> MappingResult.Success(
                RenderDynamicField(
                    id = obj.id,
                    position = position,
                    dimensions = dimensions,
                    rotation = obj.rotation,
                    style = resolvedStyle,
                    fieldId = obj.properties["fieldId"]?.toString() ?: ""
                )
            )
            else -> MappingResult.Failure(
                FactoryError(
                    objectId = obj.id,
                    message = "Unsupported object type: ${obj.type}",
                    code = "ERR_UNSUPPORTED_TYPE"
                )
            )
        }
    }

    private fun resolveTableConfig(props: Map<String, Any?>): InvoiceTableConfig {
        // Fallback to a professional standard PO 8-column set if not explicitly defined in properties
        val columns = listOf(
            InvoiceColumnDefinition("sno", "Sr. No.", 12.0, HorizontalAlignment.CENTER),
            InvoiceColumnDefinition("product", "Product / Description", 58.0, HorizontalAlignment.LEFT),
            InvoiceColumnDefinition("model", "Model / Variant", 30.0, HorizontalAlignment.LEFT),
            InvoiceColumnDefinition("pwr", "Power", 15.0, HorizontalAlignment.CENTER),
            InvoiceColumnDefinition("hsn", "HSN", 20.0, HorizontalAlignment.CENTER),
            InvoiceColumnDefinition("qty", "Qty", 15.0, HorizontalAlignment.CENTER),
            InvoiceColumnDefinition("rate", "Rate", 20.0, HorizontalAlignment.RIGHT),
            InvoiceColumnDefinition("total", "Amount", 20.0, HorizontalAlignment.RIGHT)
        )
        
        return InvoiceTableConfig(
            columns = columns,
            headerHeightMm = 8.0,
            minRowHeightMm = 7.0,
            showBorders = true
        )
    }

    private sealed class MappingResult {
        data class Success(val renderObject: RenderObject) : MappingResult()
        data class Failure(val error: FactoryError) : MappingResult()
    }
}
