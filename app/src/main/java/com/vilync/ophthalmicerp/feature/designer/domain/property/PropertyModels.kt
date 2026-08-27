package com.vilync.ophthalmicerp.feature.designer.domain.property

/**
 * Supported data types for editable properties.
 */
enum class PropertyType {
    STRING,
    NUMBER,
    COLOR,
    ENUM,
    BOOLEAN
}

/**
 * Metadata for a single editable property.
 */
data class PropertyDescriptor(
    val id: String,
    val label: String,
    val type: PropertyType,
    val category: String,
    val isReadOnly: Boolean = false,
    val constraints: Map<String, Any> = emptyMap()
)

/**
 * Logical grouping of property descriptors.
 */
data class PropertyGroup(
    val title: String,
    val descriptors: List<PropertyDescriptor>
)

/**
 * Command representing an intended change to a property.
 */
data class PropertyChange(
    val objectId: String,
    val propertyId: String,
    val newValue: Any?
)

/**
 * Result of a property validation attempt.
 */
data class PropertyValidationResult(
    val isValid: Boolean,
    val message: String? = null,
    val errorCode: String? = null
)
