package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import java.util.Locale

/**
 * Stateless, deterministic implementation of the RenderingEngine.
 */
class DefaultRenderingEngine : RenderingEngine {

    override suspend fun process(layout: RenderLayout): RenderSession {
        val startTime = System.currentTimeMillis()
        val instructions = mutableListOf<RenderInstruction>()

        layout.pages.forEach { page ->
            page.objects.forEach { obj ->
                val instruction = mapObjectToInstruction(page.index, obj)
                if (instruction != null) {
                    instructions.add(instruction)
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        
        return RenderSession(
            instructions = instructions,
            diagnostics = RenderDiagnostics(
                executionTimeMs = duration,
                pageCount = layout.pages.size,
                instructionCount = instructions.size
            )
        )
    }

    private fun mapObjectToInstruction(pageIndex: Int, obj: RenderObject): RenderInstruction? {
        val id = "${obj.id}_inst"
        val metadata = mapOf("sourceObjectId" to obj.id)
        
        return when (obj) {
            is RenderText -> DrawTextInstruction(
                instructionId = id,
                pageIndex = pageIndex,
                position = obj.position,
                dimensions = obj.dimensions,
                rotation = obj.rotation,
                style = obj.style,
                metadata = metadata,
                text = obj.text
            )
            is RenderImage -> DrawImageInstruction(
                instructionId = id,
                pageIndex = pageIndex,
                position = obj.position,
                dimensions = obj.dimensions,
                rotation = obj.rotation,
                style = obj.style,
                metadata = metadata,
                sourceUri = obj.sourceUri,
                sourcePath = obj.sourcePath
            )
            is RenderShape -> {
                when (obj.shapeType) {
                    RenderShape.ShapeType.RECTANGLE -> DrawRectangleInstruction(
                        instructionId = id,
                        pageIndex = pageIndex,
                        position = obj.position,
                        dimensions = obj.dimensions,
                        rotation = obj.rotation,
                        style = obj.style,
                        metadata = metadata
                    )
                    RenderShape.ShapeType.CIRCLE -> DrawCircleInstruction(
                        instructionId = id,
                        pageIndex = pageIndex,
                        position = obj.position,
                        dimensions = obj.dimensions,
                        rotation = obj.rotation,
                        style = obj.style,
                        metadata = metadata,
                        radius = obj.dimensions.width / 2.0
                    )
                    RenderShape.ShapeType.LINE -> DrawLineInstruction(
                        instructionId = id,
                        pageIndex = pageIndex,
                        position = obj.position,
                        dimensions = obj.dimensions,
                        rotation = obj.rotation,
                        style = obj.style,
                        metadata = metadata,
                        endPosition = Point2D(
                            x = obj.position.x + obj.dimensions.width,
                            y = obj.position.y + obj.dimensions.height,
                            unit = obj.position.unit
                        )
                    )
                }
            }
            is RenderBarcode -> {
                val format = try {
                    BarcodeFormat.valueOf(obj.barcodeType.uppercase(Locale.getDefault()))
                } catch (e: Exception) {
                    BarcodeFormat.CODE128
                }
                DrawBarcodeInstruction(
                    instructionId = id,
                    pageIndex = pageIndex,
                    position = obj.position,
                    dimensions = obj.dimensions,
                    rotation = obj.rotation,
                    style = obj.style,
                    metadata = metadata,
                    content = obj.content,
                    format = format
                )
            }
            is RenderQrCode -> DrawQrCodeInstruction(
                instructionId = id,
                pageIndex = pageIndex,
                position = obj.position,
                dimensions = obj.dimensions,
                rotation = obj.rotation,
                style = obj.style,
                metadata = metadata,
                content = obj.content
            )
            is RenderTable -> DrawTableInstruction(
                instructionId = id,
                pageIndex = pageIndex,
                position = obj.position,
                dimensions = obj.dimensions,
                rotation = obj.rotation,
                style = obj.style,
                metadata = metadata,
                properties = obj.properties
            )
            is RenderDynamicField -> null
        }
    }
}
