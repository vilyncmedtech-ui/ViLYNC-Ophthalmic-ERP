package com.vilync.ophthalmicerp.core.document.engine

import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import com.vilync.ophthalmicerp.core.document.domain.style.ResolvedStyle
import com.vilync.ophthalmicerp.core.document.domain.style.FontStyle

/**
 * Service to measure text height for dynamic row calculation.
 */
object TextMeasurementService {

    private val mmToPoint = 72.0 / 25.4

    /**
     * Calculates the height (in mm) required for text to fit within a given width.
     */
    fun measureHeightMm(text: String, widthMm: Double, style: ResolvedStyle): Double {
        if (text.isEmpty()) return 0.0

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            // SPRINT 27 ALIGNMENT: Use font size directly as points.
            textSize = style.fontSize.toFloat()
            typeface = when (style.fontStyle) {
                FontStyle.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                FontStyle.ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                FontStyle.BOLD_ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                else -> Typeface.DEFAULT
            }
        }

        val widthPx = (widthMm * mmToPoint).toInt()
        
        // Simple multiline measurement using Android's StaticLayout
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        return layout.height / mmToPoint
    }
}
