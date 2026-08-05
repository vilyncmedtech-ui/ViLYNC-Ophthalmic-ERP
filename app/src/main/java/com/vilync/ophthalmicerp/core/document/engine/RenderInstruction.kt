package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.style.ResolvedStyle
import com.vilync.ophthalmicerp.core.document.engine.BarcodeFormat
import com.vilync.ophthalmicerp.core.document.engine.EncodedSymbol

/**
 * Platform-independent drawing command.
 * 
 * All coordinates and dimensions are in MILLIMETER.
 * Instructions are immutable and order-preserved.
 */
sealed class RenderInstruction {
    abstract val instructionId: String
    abstract val pageIndex: Int
    abstract val position: Point2D
    abstract val dimensions: Dimensions
    abstract val rotation: Float
    abstract val style: ResolvedStyle
    abstract val metadata: Map<String, String>
}

data class DrawTextInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val text: String
) : RenderInstruction()

data class DrawImageInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val sourceUri: String?,
    val sourcePath: String?
) : RenderInstruction()

data class DrawLineInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val endPosition: Point2D // Start position is inherited from 'position'
) : RenderInstruction()

data class DrawRectangleInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap()
) : RenderInstruction()

data class DrawCircleInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val radius: Double
) : RenderInstruction()

data class DrawBarcodeInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val content: String,
    val format: BarcodeFormat,
    val encodedSymbol: EncodedSymbol? = null // Resolved before rendering
) : RenderInstruction()

data class DrawQrCodeInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val content: String,
    val encodedSymbol: EncodedSymbol? = null // Resolved before rendering
) : RenderInstruction()

data class DrawTableInstruction(
    override val instructionId: String,
    override val pageIndex: Int,
    override val position: Point2D,
    override val dimensions: Dimensions,
    override val rotation: Float,
    override val style: ResolvedStyle,
    override val metadata: Map<String, String> = emptyMap(),
    val properties: Map<String, Any>
) : RenderInstruction()
