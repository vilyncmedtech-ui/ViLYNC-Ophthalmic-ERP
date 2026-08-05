package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.style.*
import java.util.Locale

/**
 * Production implementation of the StyleResolver.
 * Handles cascading logic and safe parsing of raw string values.
 */
class DefaultStyleResolver : StyleResolver {

    override fun resolve(rawStyles: Map<String, String>, defaults: ResolvedStyle?): StyleResolutionResult {
        val errors = mutableListOf<StyleError>()
        
        // Start with provided defaults or engine defaults
        val base = defaults ?: ResolvedStyle()
        
        try {
            val resolved = base.copy(
                textColor = parseColor(rawStyles["color"], base.textColor, "color", errors),
                backgroundColor = parseColor(rawStyles["backgroundColor"], base.backgroundColor, "backgroundColor", errors),
                fontSize = parseDouble(rawStyles["fontSize"], base.fontSize, "fontSize", errors),
                fontStyle = parseEnum(rawStyles["fontStyle"], base.fontStyle, "fontStyle", errors),
                horizontalAlignment = parseEnum(rawStyles["textAlign"], base.horizontalAlignment, "textAlign", errors),
                verticalAlignment = parseEnum(rawStyles["verticalAlign"], base.verticalAlignment, "verticalAlign", errors),
                opacity = parseFloat(rawStyles["opacity"], base.opacity, "opacity", errors),
                border = resolveBorder(rawStyles, base.border, errors)
            )
            
            return if (errors.isEmpty()) {
                StyleResolutionResult.Success(resolved)
            } else {
                StyleResolutionResult.Failure(errors)
            }
        } catch (e: Exception) {
            return StyleResolutionResult.Failure(listOf(StyleError(null, "Internal resolution error: ${e.message}", "ERR_INTERNAL")))
        }
    }

    private fun parseColor(value: String?, fallback: DocumentColor, key: String, errors: MutableList<StyleError>): DocumentColor {
        if (value == null) return fallback
        val hex = value.trim().removePrefix("#")
        return try {
            when (hex.length) {
                6 -> DocumentColor(
                    255,
                    hex.substring(0, 2).toInt(16),
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16)
                )
                8 -> DocumentColor(
                    hex.substring(0, 2).toInt(16),
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16),
                    hex.substring(6, 8).toInt(16)
                )
                else -> {
                    errors.add(StyleError(key, "Invalid color format: $value. Expected #RRGGBB or #AARRGGBB", "ERR_INVALID_COLOR"))
                    fallback
                }
            }
        } catch (e: Exception) {
            errors.add(StyleError(key, "Unable to parse color: $value", "ERR_PARSE_COLOR"))
            fallback
        }
    }

    private fun parseDouble(value: String?, fallback: Double, key: String, errors: MutableList<StyleError>): Double {
        if (value == null) return fallback
        return value.toDoubleOrNull() ?: run {
            errors.add(StyleError(key, "Invalid numeric value: $value", "ERR_INVALID_NUMBER"))
            fallback
        }
    }

    private fun parseFloat(value: String?, fallback: Float, key: String, errors: MutableList<StyleError>): Float {
        if (value == null) return fallback
        return value.toFloatOrNull() ?: run {
            errors.add(StyleError(key, "Invalid float value: $value", "ERR_INVALID_FLOAT"))
            fallback
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, fallback: T, key: String, errors: MutableList<StyleError>): T {
        if (value == null) return fallback
        return try {
            java.lang.Enum.valueOf(T::class.java, value.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            errors.add(StyleError(key, "Unsupported value for $key: $value", "ERR_UNSUPPORTED_VALUE"))
            fallback
        }
    }

    private fun resolveBorder(raw: Map<String, String>, fallback: DocumentBorder?, errors: MutableList<StyleError>): DocumentBorder? {
        val widthStr = raw["borderWidth"]
        val colorStr = raw["borderColor"]
        val styleStr = raw["borderStyle"]
        
        if (widthStr == null && colorStr == null && styleStr == null) return fallback
        
        val baseWidth = fallback?.width ?: 0.0
        val baseColor = fallback?.color ?: DocumentColor.BLACK
        val baseStyle = fallback?.style ?: DocumentBorder.BorderStyle.SOLID
        
        return DocumentBorder(
            width = parseDouble(widthStr, baseWidth, "borderWidth", errors),
            color = parseColor(colorStr, baseColor, "borderColor", errors),
            style = parseEnum(styleStr, baseStyle, "borderStyle", errors)
        )
    }
}
