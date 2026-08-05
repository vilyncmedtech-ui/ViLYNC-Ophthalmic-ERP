package com.vilync.ophthalmicerp.core.reports.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import java.io.File
import java.io.FileOutputStream

object UniversalExportEngine {

    fun exportToPdf(
        context: Context,
        schema: ReportSchema,
        rows: List<ReportRowData>,
        summaries: Map<String, String>
    ): File {
        val file = File(context.cacheDir, "${schema.title.replace(" ", "_")}.pdf")
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 9f }
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        canvas.drawText(schema.title, 40f, y, titlePaint)
        y += 30f

        // Draw Summary Cards Data
        schema.summaries.forEach { config ->
            val value = summaries[config.id] ?: "0"
            canvas.drawText("${config.label}: ${config.prefix}$value${config.suffix}", 40f, y, textPaint)
            y += 15f
        }
        y += 10f

        // Draw Table Headers
        val visibleColumns = schema.columns.filter { it.isVisible }
        var x = 40f
        val colWidth = 515f / visibleColumns.size.coerceAtLeast(1)
        visibleColumns.forEach { col ->
            canvas.drawText(col.displayName, x, y, textPaint.apply { isFakeBoldText = true })
            x += colWidth
        }
        y += 20f

        // Draw Rows
        rows.forEach { row ->
            if (y > 800f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            x = 40f
            visibleColumns.forEach { col ->
                val value = row.values[col.id]?.toString() ?: "-"
                canvas.drawText(value.take(20), x, y, textPaint.apply { isFakeBoldText = false })
                x += colWidth
            }
            y += 15f
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
