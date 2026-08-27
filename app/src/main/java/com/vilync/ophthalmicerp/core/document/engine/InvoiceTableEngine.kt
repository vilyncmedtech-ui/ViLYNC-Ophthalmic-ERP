package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.style.*

/**
 * Decomposes a RenderInvoiceTable into primitive RenderInstructions.
 */
object InvoiceTableEngine {

    fun decompose(pageIndex: Int, table: RenderInvoiceTable): List<RenderInstruction> {
        val instructions = mutableListOf<RenderInstruction>()
        val config = table.config
        val data = table.data ?: return emptyList()

        var currentY = table.position.y

        // 1. Draw Header
        val headerResult = drawHeader(pageIndex, table.position.x, currentY, table)
        instructions.addAll(headerResult.instructions)
        currentY += config.headerHeightMm

        // 2. Draw Data Rows
        data.rows.forEachIndexed { rowIndex, rowData ->
            val rowHeight = calculateRowHeight(rowData, table)
            
            // Check for page overflow (placeholder logic for future pagination)
            // if (currentY + rowHeight > pageMaxY) { ... start new page ... }
            
            instructions.addAll(drawRow(pageIndex, rowIndex, table.position.x, currentY, rowHeight, rowData, table))
            currentY += rowHeight
        }

        // 3. Draw Vertical Column Dividers
        if (config.showBorders) {
            val tableHeight = currentY - table.position.y
            instructions.addAll(drawColumnDividers(pageIndex, table.position.x, table.position.y, tableHeight, table))
            
            // Bottom Horizontal Border
            instructions.add(DrawLineInstruction(
                instructionId = "${table.id}_bottom_border",
                pageIndex = pageIndex,
                position = Point2D(table.position.x, currentY),
                dimensions = Dimensions(table.dimensions.width, 0.0),
                rotation = 0f,
                style = table.style,
                endPosition = Point2D(table.position.x + table.dimensions.width, currentY),
                metadata = emptyMap()
            ))
        }

        return instructions
    }

    private data class HeaderResult(val instructions: List<RenderInstruction>)

    private fun drawHeader(pageIndex: Int, startX: Double, startY: Double, table: RenderInvoiceTable): HeaderResult {
        val instructions = mutableListOf<RenderInstruction>()
        val config = table.config
        var currentX = startX

        config.columns.forEach { col ->
            val cellPos = Point2D(currentX + 1.0, startY) // ADDED: Horizontal padding for alignment parity
            val cellDim = Dimensions(col.widthMm - 2.0, config.headerHeightMm)
            
            instructions.add(DrawTextInstruction(
                instructionId = "${table.id}_header_${col.id}",
                pageIndex = pageIndex,
                position = cellPos,
                dimensions = cellDim,
                rotation = 0f,
                style = table.style.copy(
                    fontStyle = FontStyle.BOLD, 
                    horizontalAlignment = if (col.id == "product") HorizontalAlignment.LEFT else HorizontalAlignment.CENTER,
                    verticalAlignment = VerticalAlignment.MIDDLE
                ),
                text = col.header,
                metadata = emptyMap()
            ))
            
            currentX += col.widthMm
        }
        
        if (config.showBorders) {
            // Header Bottom Line
            instructions.add(DrawLineInstruction(
                instructionId = "${table.id}_header_sep",
                pageIndex = pageIndex,
                position = Point2D(startX, startY + config.headerHeightMm),
                dimensions = Dimensions(table.dimensions.width, 0.0),
                rotation = 0f,
                style = table.style,
                endPosition = Point2D(startX + table.dimensions.width, startY + config.headerHeightMm),
                metadata = emptyMap()
            ))
            
            // Top Line
            instructions.add(DrawLineInstruction(
                instructionId = "${table.id}_top_border",
                pageIndex = pageIndex,
                position = Point2D(startX, startY),
                dimensions = Dimensions(table.dimensions.width, 0.0),
                rotation = 0f,
                style = table.style,
                endPosition = Point2D(startX + table.dimensions.width, startY),
                metadata = emptyMap()
            ))
        }

        return HeaderResult(instructions)
    }

    private fun drawRow(
        pageIndex: Int, rowIndex: Int, startX: Double, startY: Double, 
        rowHeight: Double, rowData: List<BindingValue>, table: RenderInvoiceTable
    ): List<RenderInstruction> {
        val instructions = mutableListOf<RenderInstruction>()
        var currentX = startX

        table.config.columns.forEachIndexed { colIndex, col ->
            val value = rowData.getOrNull(colIndex)
            val textValue = formatValue(value)
            
            // PIXEL-PERFECT ALIGNMENT: 
            // 1. Horizontal: Start at currentX + 1.0 with width col.widthMm - 2.0 to match header and provide padding.
            // 2. Vertical: Use full rowHeight and VerticalAlignment.MIDDLE for consistent centering across all columns.
            instructions.add(DrawTextInstruction(
                instructionId = "${table.id}_row_${rowIndex}_col_${col.id}",
                pageIndex = pageIndex,
                position = Point2D(currentX + 1.0, startY), 
                dimensions = Dimensions(col.widthMm - 2.0, rowHeight),
                rotation = 0f,
                style = table.style.copy(
                    horizontalAlignment = col.alignment,
                    verticalAlignment = VerticalAlignment.MIDDLE
                ),
                text = textValue,
                metadata = emptyMap()
            ))
            currentX += col.widthMm
        }

        return instructions
    }

    private fun drawColumnDividers(
        pageIndex: Int, startX: Double, startY: Double, 
        tableHeight: Double, table: RenderInvoiceTable
    ): List<RenderInstruction> {
        val instructions = mutableListOf<RenderInstruction>()
        var currentX = startX

        // Left-most border
        instructions.add(createDivider(pageIndex, table.id, "left", currentX, startY, tableHeight, table.style))

        table.config.columns.forEachIndexed { index, col ->
            currentX += col.widthMm
            val id = "col_${index}"
            instructions.add(createDivider(pageIndex, table.id, id, currentX, startY, tableHeight, table.style))
        }

        return instructions
    }

    private fun createDivider(pageIndex: Int, tableId: String, suffix: String, x: Double, y: Double, height: Double, style: ResolvedStyle): RenderInstruction {
        return DrawLineInstruction(
            instructionId = "${tableId}_div_${suffix}",
            pageIndex = pageIndex,
            position = Point2D(x, y),
            dimensions = Dimensions(0.0, height),
            rotation = 0f,
            style = style,
            endPosition = Point2D(x, y + height),
            metadata = emptyMap()
        )
    }

    private fun calculateRowHeight(rowData: List<BindingValue>, table: RenderInvoiceTable): Double {
        var maxHeight = table.config.minRowHeightMm
        
        table.config.columns.forEachIndexed { index, col ->
            val value = rowData.getOrNull(index)
            val text = formatValue(value)
            // Use cell width minus padding for accurate wrapping measurement
            val height = TextMeasurementService.measureHeightMm(text, col.widthMm - 2.0, table.style) + 2.0
            if (height > maxHeight) maxHeight = height
        }
        
        return maxHeight
    }

    private fun formatValue(value: BindingValue?): String {
        return when (value) {
            is BindingValue.Text -> value.value
            is BindingValue.Number -> "%.2f".format(value.value)
            else -> value?.toString() ?: ""
        }
    }
}
