package com.vilync.ophthalmicerp.feature.designer.domain.property

import com.vilync.ophthalmicerp.core.document.engine.*

/**
 * A reactive model that snapshots the current property values of a selected object.
 */
class PropertyEditorModel(
    val objectId: String,
    val groups: List<PropertyGroup>
) {
    companion object {
        /**
         * Factory method to create a property model for a specific document object.
         */
        fun fromObject(obj: RenderObject): PropertyEditorModel {
            val groups = mutableListOf<PropertyGroup>()
            
            // 1. Layout Group
            groups.add(PropertyGroup("Layout", listOf(
                PropertyDescriptor("x", "Position X", PropertyType.NUMBER, "Layout"),
                PropertyDescriptor("y", "Position Y", PropertyType.NUMBER, "Layout"),
                PropertyDescriptor("width", "Width", PropertyType.NUMBER, "Layout"),
                PropertyDescriptor("height", "Height", PropertyType.NUMBER, "Layout"),
                PropertyDescriptor("rotation", "Rotation", PropertyType.NUMBER, "Layout")
            )))
            
            // 2. Object Specific Groups
            when (obj) {
                is RenderText -> {
                    groups.add(PropertyGroup("Text", listOf(
                        PropertyDescriptor("text", "Content", PropertyType.STRING, "Content")
                    )))
                }
                is RenderBarcode -> {
                    groups.add(PropertyGroup("Barcode", listOf(
                        PropertyDescriptor("content", "Content", PropertyType.STRING, "Content"),
                        PropertyDescriptor("barcodeType", "Symbology", PropertyType.ENUM, "Content")
                    )))
                }
                // Future: Add specific properties for other types
                else -> {}
            }
            
            return PropertyEditorModel(obj.id, groups)
        }
    }
}
