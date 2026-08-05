package com.vilync.ophthalmicerp.core.reports.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.core.reports.domain.ReportRowData
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.util.ShareUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object UniversalExportSuite {

    private fun getExportFile(context: Context, fileName: String, extension: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "$fileName.$extension")
    }

    fun exportPdfAndShare(
        context: Context,
        schema: ReportSchema,
        rows: List<ReportRowData>,
        visibleColumnIds: Set<String>? = null
    ): Result<Unit> = runCatching {
        val file = getExportFile(context, safeName(schema.title), "pdf")
        
        // Filter based on UI visibility if provided, else use schema default
        val visibleColumns = schema.columns.filter { col ->
            val isUiVisible = visibleColumnIds?.contains(col.id) ?: col.isVisible
            isUiVisible && when (col.id) {
                "cgst" -> rows.any { (it.values["cgst"] as? Double ?: 0.0) > 0.0 }
                "sgst" -> rows.any { (it.values["sgst"] as? Double ?: 0.0) > 0.0 }
                "igst" -> rows.any { (it.values["igst"] as? Double ?: 0.0) > 0.0 }
                else -> true
            }
        }

        writePdf(file, schema, rows, visibleColumns)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Report")
    }

    fun exportExcelAndShare(
        context: Context,
        schema: ReportSchema,
        rows: List<ReportRowData>,
        visibleColumnIds: Set<String>? = null
    ): Result<Unit> = runCatching {
        val file = getExportFile(context, safeName(schema.title), "xls")
        
        val visibleColumns = schema.columns.filter { col ->
            val isUiVisible = visibleColumnIds?.contains(col.id) ?: col.isVisible
            isUiVisible && when (col.id) {
                "cgst" -> rows.any { (it.values["cgst"] as? Double ?: 0.0) > 0.0 }
                "sgst" -> rows.any { (it.values["sgst"] as? Double ?: 0.0) > 0.0 }
                "igst" -> rows.any { (it.values["igst"] as? Double ?: 0.0) > 0.0 }
                else -> true
            }
        }

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Report"><Table>""")
            
            fun cell(v: String) {
                w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
            }

            // Headers
            w.write("<Row>")
            visibleColumns.forEach { cell(it.displayName) }
            w.write("</Row>")

            // Rows
            rows.forEach { r ->
                w.write("<Row>")
                visibleColumns.forEach { col ->
                    cell(r.values[col.id]?.toString() ?: "")
                }
                w.write("</Row>")
            }
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Report Excel")
    }

    fun print(
        context: Context,
        schema: ReportSchema,
        rows: List<ReportRowData>,
        visibleColumnIds: Set<String>? = null
    ): Result<Unit> = runCatching {
        val visibleColumns = schema.columns.filter { col ->
            val isUiVisible = visibleColumnIds?.contains(col.id) ?: col.isVisible
            isUiVisible && when (col.id) {
                "cgst" -> rows.any { (it.values["cgst"] as? Double ?: 0.0) > 0.0 }
                "sgst" -> rows.any { (it.values["sgst"] as? Double ?: 0.0) > 0.0 }
                "igst" -> rows.any { (it.values["igst"] as? Double ?: 0.0) > 0.0 }
                else -> true
            }
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            schema.title,
            ReportPrintAdapter(schema, rows, visibleColumns),
            PrintAttributes.Builder().build()
        )
    }

    private fun writePdf(file: File, schema: ReportSchema, rows: List<ReportRowData>, visibleColumns: List<com.vilync.ophthalmicerp.core.reports.domain.ReportColumn>) {
        val document = PdfDocument()
        
        // Decide orientation: Landscape if many columns
        val isLandscape = visibleColumns.size > 7
        val pageWidth = if (isLandscape) 842 else 595
        val pageHeight = if (isLandscape) 595 else 842
        val margin = 32f
        val contentWidth = pageWidth - (margin * 2)
        
        // Font scaling
        val baseFontSize = if (visibleColumns.size > 10) 7f else 8f
        
        val paint = Paint().apply { textSize = baseFontSize }
        val headerPaint = Paint().apply { textSize = baseFontSize; isFakeBoldText = true }
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val linePaint = Paint().apply { strokeWidth = 0.5f }
        
        val totalWeight = visibleColumns.sumOf { it.weight.toDouble() }.toFloat()
        val colWidths = visibleColumns.map { (it.weight / totalWeight) * contentWidth }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var y = 45f

        fun drawHeaders() {
            page.canvas.drawText(schema.title, margin, y, titlePaint); y += 22f
            page.canvas.drawText(schema.subtitle, margin, y, paint); y += 18f
            
            var x = margin
            visibleColumns.forEachIndexed { index, col ->
                page.canvas.drawText(col.displayName, x, y, headerPaint)
                x += colWidths[index]
            }
            y += 8f
            page.canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 15f
        }

        drawHeaders()

        rows.forEach { r ->
            // Pre-calculate row height based on maximum number of lines in any cell
            val rowData = visibleColumns.map { col ->
                (r.values[col.id]?.toString() ?: "-").split("\n")
            }
            val maxLinesInRow = rowData.maxOf { it.size }.coerceAtLeast(1)
            val rowHeight = maxLinesInRow * 14f

            if (y + rowHeight > pageHeight - 40f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                y = 45f
                drawHeaders()
            }
            
            var x = margin
            visibleColumns.forEachIndexed { index, col ->
                val lines = rowData[index]
                val availableWidth = colWidths[index] - 5f // padding
                
                lines.forEachIndexed { lineIndex, line ->
                    val lineY = y + (lineIndex * 14f)
                    
                    // Smart truncation for each individual line
                    val truncatedText = if (paint.measureText(line) > availableWidth) {
                        val count = paint.breakText(line, true, availableWidth - paint.measureText("..."), null)
                        line.take(count) + "..."
                    } else {
                        line
                    }
                    
                    page.canvas.drawText(truncatedText, x, lineY, paint)
                }
                x += colWidths[index]
            }
            y += rowHeight
        }
        
        document.finishPage(page)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        document.close()
    }

    private fun safeName(value: String) =
        value.replace(Regex("[^A-Za-z0-9._-]+"), "_")

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private class ReportPrintAdapter(
        private val schema: ReportSchema,
        private val rows: List<ReportRowData>,
        private val visibleColumns: List<com.vilync.ophthalmicerp.core.reports.domain.ReportColumn>
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("${schema.title}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            runCatching {
                val temp = File.createTempFile("universal_report_", ".pdf")
                writePdf(temp, schema, rows, visibleColumns)
                temp.inputStream().use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                temp.delete()
            }.onSuccess {
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }.onFailure {
                callback.onWriteFailed(it.message)
            }
        }
    }
}
