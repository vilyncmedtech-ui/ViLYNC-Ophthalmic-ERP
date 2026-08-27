package com.vilync.ophthalmicerp.feature.designer.domain.property

/**
 * Interface for validating property changes before they are applied to the layout.
 */
interface PropertyValidator {
    /**
     * Validates a proposed property change against domain rules.
     */
    fun validate(change: PropertyChange, descriptor: PropertyDescriptor): PropertyValidationResult
}

/**
 * Default implementation for universal document designer properties.
 */
class DefaultPropertyValidator : PropertyValidator {

    override fun validate(change: PropertyChange, descriptor: PropertyDescriptor): PropertyValidationResult {
        return when (descriptor.type) {
            PropertyType.NUMBER -> validateNumber(change.newValue, descriptor.constraints)
            PropertyType.STRING -> validateString(change.newValue, descriptor.constraints)
            else -> PropertyValidationResult(true) // Default to valid for other types
        }
    }

    private fun validateNumber(value: Any?, constraints: Map<String, Any>): PropertyValidationResult {
        val num = (value as? Number)?.toDouble() ?: return PropertyValidationResult(false, "Value must be a number", "ERR_NOT_A_NUMBER")

        val min = (constraints["min"] as? Number)?.toDouble()
        if (min != null && num < min) {
            return PropertyValidationResult(false, "Value must be at least $min", "ERR_TOO_SMALL")
        }

        val max = (constraints["max"] as? Number)?.toDouble()
        if (max != null && num > max) {
            return PropertyValidationResult(false, "Value must be at most $max", "ERR_TOO_LARGE")
        }

        return PropertyValidationResult(true)
    }

    private fun validateString(value: Any?, constraints: Map<String, Any>): PropertyValidationResult {
        val str = value?.toString() ?: ""
        
        val minLen = constraints["minLength"] as? Int
        if (minLen != null && str.length < minLen) {
            return PropertyValidationResult(false, "Text too short", "ERR_TOO_SHORT")
        }

        return PropertyValidationResult(true)
    }
}
