package com.vilync.ophthalmicerp.core.document.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.style.DocumentColor
import com.vilync.ophthalmicerp.core.document.domain.style.FontStyle
import com.vilync.ophthalmicerp.core.document.domain.style.HorizontalAlignment

/**
 * Shared logic for translating RenderInstructions into Android Canvas operations.
 * Used by both PDF and Print renderers to ensure visual parity.
 */
object CanvasInstructionDispatcher {

    private val mmToPoint = 72.0 / 25.4
    private val imageCache = mutableMapOf<String, Bitmap>()

    fun dispatch(canvas: Canvas, instruction: RenderInstruction, warnings: MutableList<String>) {
        when (instruction) {
            is DrawTextInstruction -> handleText(canvas, instruction)
            is DrawRectangleInstruction -> handleRectangle(canvas, instruction)
            is DrawLineInstruction -> handleLine(canvas, instruction)
            is DrawCircleInstruction -> handleCircle(canvas, instruction)
            is DrawImageInstruction -> handleImage(canvas, instruction)
            is DrawBarcodeInstruction -> {
                if (instruction.encodedSymbol != null) {
                    drawEncodedSymbol(canvas, instruction, instruction.encodedSymbol, quietZoneModules = 10)
                } else {
                    handlePlaceholder(canvas, instruction, "BARCODE: ${instruction.format.name}")
                }
            }
            is DrawQrCodeInstruction -> {
                if (instruction.encodedSymbol != null) {
                    drawEncodedSymbol(canvas, instruction, instruction.encodedSymbol, quietZoneModules = 4)
                } else {
                    handlePlaceholder(canvas, instruction, "QR CODE")
                }
            }
            is DrawTableInstruction -> warnings.add("Table instruction not yet decomposed: ${instruction.instructionId}")
        }
    }

    private fun handleText(canvas: Canvas, inst: DrawTextInstruction) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inst.style.textColor.toAndroidColor()
            textSize = (inst.style.fontSize * (inst.dimensions.unit.mmFactor / MeasurementUnit.POINT.mmFactor)).toFloat()
            typeface = when (inst.style.fontStyle) {
                FontStyle.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                FontStyle.ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                FontStyle.BOLD_ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                else -> Typeface.DEFAULT
            }
        }

        val x = (inst.position.x * mmToPoint).toFloat()
        val y = (inst.position.y * mmToPoint).toFloat()

        canvas.save()
        if (inst.rotation != 0f) {
            canvas.rotate(inst.rotation, x, y)
        }

        val xAdjusted = when (inst.style.horizontalAlignment) {
            HorizontalAlignment.CENTER -> x + (inst.dimensions.width * mmToPoint / 2.0).toFloat()
            HorizontalAlignment.RIGHT -> x + (inst.dimensions.width * mmToPoint).toFloat()
            else -> x
        }

        if (inst.style.horizontalAlignment == HorizontalAlignment.CENTER) {
            paint.textAlign = Paint.Align.CENTER
        } else if (inst.style.horizontalAlignment == HorizontalAlignment.RIGHT) {
            paint.textAlign = Paint.Align.RIGHT
        }

        canvas.drawText(inst.text, xAdjusted, y + paint.textSize, paint)
        canvas.restore()
    }

    private fun handleRectangle(canvas: Canvas, inst: DrawRectangleInstruction) {
        val paint = Paint().apply {
            color = inst.style.backgroundColor.toAndroidColor()
            style = Paint.Style.FILL
        }
        
        val left = (inst.position.x * mmToPoint).toFloat()
        val top = (inst.position.y * mmToPoint).toFloat()
        val right = left + (inst.dimensions.width * mmToPoint).toFloat()
        val bottom = top + (inst.dimensions.height * mmToPoint).toFloat()

        canvas.drawRect(left, top, right, bottom, paint)

        inst.style.border?.let { border ->
            val strokePaint = Paint().apply {
                color = border.color.toAndroidColor()
                strokeWidth = (border.width * mmToPoint).toFloat()
                style = Paint.Style.STROKE
            }
            canvas.drawRect(left, top, right, bottom, strokePaint)
        }
    }

    private fun handleLine(canvas: Canvas, inst: DrawLineInstruction) {
        val paint = Paint().apply {
            color = inst.style.textColor.toAndroidColor()
            strokeWidth = (inst.style.border?.width?.let { it * mmToPoint } ?: 1.0).toFloat()
        }
        val startX = (inst.position.x * mmToPoint).toFloat()
        val startY = (inst.position.y * mmToPoint).toFloat()
        val endX = (inst.endPosition.x * mmToPoint).toFloat()
        val endY = (inst.endPosition.y * mmToPoint).toFloat()

        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    private fun handleCircle(canvas: Canvas, inst: DrawCircleInstruction) {
        val paint = Paint().apply {
            color = inst.style.backgroundColor.toAndroidColor()
            style = Paint.Style.FILL
        }
        val cx = (inst.position.x * mmToPoint).toFloat()
        val cy = (inst.position.y * mmToPoint).toFloat()
        val radius = (inst.radius * mmToPoint).toFloat()

        canvas.drawCircle(cx, cy, radius, paint)

        inst.style.border?.let { border ->
            val strokePaint = Paint().apply {
                color = border.color.toAndroidColor()
                strokeWidth = (border.width * mmToPoint).toFloat()
                style = Paint.Style.STROKE
            }
            canvas.drawCircle(cx, cy, radius, strokePaint)
        }
    }

    private fun handleImage(canvas: Canvas, inst: DrawImageInstruction) {
        val bitmap = resolveBitmap(inst.sourcePath, inst.sourceUri) ?: return
        
        val left = (inst.position.x * mmToPoint).toFloat()
        val top = (inst.position.y * mmToPoint).toFloat()
        val right = left + (inst.dimensions.width * mmToPoint).toFloat()
        val bottom = top + (inst.dimensions.height * mmToPoint).toFloat()

        canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, right, bottom), null)
    }

    private fun drawEncodedSymbol(canvas: Canvas, inst: RenderInstruction, symbol: EncodedSymbol, quietZoneModules: Int) {
        val paint = Paint().apply {
            color = inst.style.textColor.toAndroidColor()
            style = Paint.Style.FILL
        }

        val left = (inst.position.x * mmToPoint).toFloat()
        val top = (inst.position.y * mmToPoint).toFloat()
        val totalWidth = (inst.dimensions.width * mmToPoint).toFloat()
        val totalHeight = (inst.dimensions.height * mmToPoint).toFloat()

        // Calculate available area after reserving Quiet Zones
        val effectiveWidthModules = symbol.width + (quietZoneModules * 2)
        val effectiveHeightModules = symbol.height + (quietZoneModules * 2)

        val bitWidth = totalWidth / effectiveWidthModules
        val bitHeight = totalHeight / effectiveHeightModules

        // Start drawing offset by the quiet zone
        val startXOffset = left + (quietZoneModules * bitWidth)
        val startYOffset = top + (quietZoneModules * bitHeight)

        for (y in 0 until symbol.height) {
            for (x in 0 until symbol.width) {
                if (symbol.getBit(x, y)) {
                    val rLeft = startXOffset + (x * bitWidth)
                    val rTop = startYOffset + (y * bitHeight)
                    canvas.drawRect(rLeft, rTop, rLeft + bitWidth, rTop + bitHeight, paint)
                }
            }
        }
    }

    private fun handlePlaceholder(canvas: Canvas, inst: RenderInstruction, label: String) {
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
        val left = (inst.position.x * mmToPoint).toFloat()
        val top = (inst.position.y * mmToPoint).toFloat()
        val right = left + (inst.dimensions.width * mmToPoint).toFloat()
        val bottom = top + (inst.dimensions.height * mmToPoint).toFloat()

        canvas.drawRect(left, top, right, bottom, paint)
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, left + (inst.dimensions.width * mmToPoint / 2.0).toFloat(), top + (inst.dimensions.height * mmToPoint / 2.0).toFloat(), textPaint)
    }

    private fun resolveBitmap(path: String?, uri: String?): Bitmap? {
        val key = path ?: uri ?: return null
        if (imageCache.containsKey(key)) return imageCache[key]

        return try {
            val bitmap = if (path != null) {
                BitmapFactory.decodeFile(path)
            } else {
                null 
            }
            if (bitmap != null) imageCache[key] = bitmap
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        imageCache.values.forEach { it.recycle() }
        imageCache.clear()
    }

    private fun DocumentColor.toAndroidColor(): Int {
        return Color.argb(alpha, red, green, blue)
    }
}
