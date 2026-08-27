package com.vilync.ophthalmicerp.core.document.engine.binding

import com.vilync.ophthalmicerp.core.document.engine.*
import java.util.regex.Pattern

/**
 * Transforms a generic RenderLayout into a bound, data-populated RenderLayout.
 */
class BindingEngine {

    private val placeholderPattern = Pattern.compile("\\\$\\{([^}]+)\\}")

    /**
     * Binds a layout with the provided values.
     */
    fun bind(layout: RenderLayout, values: Map<String, BindingValue>): RenderLayout {
        val boundPages = layout.pages.map { page ->
            val boundObjects = page.objects.map { obj ->
                bindObject(obj, values)
            }
            page.copy(objects = boundObjects)
        }
        return layout.copy(pages = boundPages)
    }

    private fun bindObject(obj: RenderObject, values: Map<String, BindingValue>): RenderObject {
        return when (obj) {
            is RenderText -> bindText(obj, values)
            is RenderInvoiceTable -> bindInvoiceTable(obj, values)
            is RenderDynamicField -> bindDynamicField(obj, values)
            // Shapes, Tables (for now), etc. are passed through.
            else -> obj
        }
    }

    private fun bindInvoiceTable(obj: RenderInvoiceTable, values: Map<String, BindingValue>): RenderInvoiceTable {
        // Assume the table data is provided under the ID of the table object if not explicitly placeholder-mapped
        val tableData = values[obj.id] as? BindingValue.Table
        return obj.copy(data = tableData)
    }

    private fun bindText(obj: RenderText, values: Map<String, BindingValue>): RenderText {
        val matcher = placeholderPattern.matcher(obj.text)
        val sb = StringBuilder()
        var lastEnd = 0
        
        while (matcher.find()) {
            sb.append(obj.text.substring(lastEnd, matcher.start()))
            val key = matcher.group(1) ?: ""
            val resolvedValue = values[key]
            
            val textValue = when (resolvedValue) {
                is BindingValue.Text -> resolvedValue.value
                is BindingValue.Number -> resolvedValue.value.toString()
                is BindingValue.Date -> resolvedValue.timestamp.toString() // Simplified for now
                null -> "[? $key]"
                else -> resolvedValue.toString() // Fallback
            }
            
            sb.append(textValue)
            lastEnd = matcher.end()
        }
        sb.append(obj.text.substring(lastEnd))
        
        return obj.copy(text = sb.toString())
    }

    private fun bindDynamicField(obj: RenderDynamicField, values: Map<String, BindingValue>): RenderObject {
        val resolvedValue = values[obj.fieldId]
        
        return when (resolvedValue) {
            is BindingValue.Text -> RenderText(
                id = obj.id, position = obj.position, dimensions = obj.dimensions,
                rotation = obj.rotation, style = obj.style, text = resolvedValue.value
            )
            is BindingValue.Number -> RenderText(
                id = obj.id, position = obj.position, dimensions = obj.dimensions,
                rotation = obj.rotation, style = obj.style, text = resolvedValue.value.toString()
            )
            is BindingValue.Barcode -> RenderBarcode(
                id = obj.id, position = obj.position, dimensions = obj.dimensions,
                rotation = obj.rotation, style = obj.style, content = resolvedValue.content,
                barcodeType = resolvedValue.format.name
            )
            is BindingValue.QrCode -> RenderQrCode(
                id = obj.id, position = obj.position, dimensions = obj.dimensions,
                rotation = obj.rotation, style = obj.style, content = resolvedValue.content
            )
            is BindingValue.Image -> RenderImage(
                id = obj.id, position = obj.position, dimensions = obj.dimensions,
                rotation = obj.rotation, style = obj.style, sourceUri = resolvedValue.uri,
                sourcePath = resolvedValue.path
            )
            else -> {
                // Unknown or unhandled type (like Table) defaults to a Text placeholder
                RenderText(
                    id = obj.id, position = obj.position, dimensions = obj.dimensions,
                    rotation = obj.rotation, style = obj.style, text = "[MISSING: ${obj.fieldId}]"
                )
            }
        }
    }
}
