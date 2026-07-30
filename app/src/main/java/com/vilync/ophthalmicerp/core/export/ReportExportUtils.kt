package com.vilync.ophthalmicerp.core.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream

data class ExportReport(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

object ReportExportUtils {

    fun exportPdf(context: Context, uri: Uri, report: ExportReport) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            val document = createPdf(report)
            try {
                document.writeTo(output)
            } finally {
                document.close()
            }
        }
    }

    fun exportExcelCsv(context: Context, uri: Uri, report: ExportReport) {
        val csv = buildString {
            appendCsvRow(report.headers)
            report.rows.forEach { row ->
                appendCsvRow(row)
            }
        }
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(csv)
        }
    }

    fun print(context: Context, report: ExportReport) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            report.title,
            ReportPrintAdapter(report),
            PrintAttributes.Builder().build()
        )
    }

    private fun StringBuilder.appendCsvRow(values: List<String>) {
        append(values.joinToString(",") { value ->
            "\"${value.replace("\"", "\"\"")}\""
        })
        appendLine()
    }

    private fun createPdf(report: ExportReport): PdfDocument {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 8.5f }
        val headerPaint = Paint().apply { textSize = 8.5f; isFakeBoldText = true }
        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(184, 184, 184)
            strokeWidth = 0.7f
        }
        val headerBackgroundPaint = Paint().apply {
            color = android.graphics.Color.rgb(232, 236, 246)
        }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentWidth = pageWidth - (margin * 2)
        val columnWidths = tableColumnWidths(report.headers, contentWidth)
        val rowHeight = 23f
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun drawFittedText(text: String, x: Float, maxWidth: Float, baseline: Float, paint: Paint) {
            var result = text
            while (result.isNotEmpty() && paint.measureText(result) > maxWidth - 8f) {
                result = result.dropLast(1)
            }
            if (result != text && result.length > 3) result = result.dropLast(3) + "..."
            canvas.drawText(result, x + 4f, baseline, paint)
        }

        fun drawTableHeader() {
            canvas.drawRect(margin, y, margin + columnWidths.sum(), y + rowHeight, headerBackgroundPaint)
            var x = margin
            report.headers.forEachIndexed { index, header ->
                drawFittedText(header, x, columnWidths[index], y + 15f, headerPaint)
                canvas.drawLine(x, y, x, y + rowHeight, linePaint)
                x += columnWidths[index]
            }
            canvas.drawLine(margin + columnWidths.sum(), y, margin + columnWidths.sum(), y + rowHeight, linePaint)
            canvas.drawLine(margin, y, margin + columnWidths.sum(), y, linePaint)
            canvas.drawLine(margin, y + rowHeight, margin + columnWidths.sum(), y + rowHeight, linePaint)
            y += rowHeight
        }

        fun drawReportHeading() {
            canvas.drawText(report.title, margin, y + 18f, titlePaint)
            y += 31f
            drawTableHeader()
        }

        fun nextPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
            drawReportHeading()
        }

        drawReportHeading()
        report.rows.forEach { row ->
            if (y + rowHeight > pageHeight - margin) nextPage()
            var x = margin
            report.headers.indices.forEach { index ->
                val value = row.getOrNull(index).orEmpty()
                drawFittedText(value, x, columnWidths[index], y + 15f, textPaint)
                canvas.drawLine(x, y, x, y + rowHeight, linePaint)
                x += columnWidths[index]
            }
            canvas.drawLine(margin + columnWidths.sum(), y, margin + columnWidths.sum(), y + rowHeight, linePaint)
            canvas.drawLine(margin, y + rowHeight, margin + columnWidths.sum(), y + rowHeight, linePaint)
            y += rowHeight
        }
        document.finishPage(page)
        return document
    }

    private fun tableColumnWidths(headers: List<String>, contentWidth: Float): List<Float> {
        if (headers == listOf("Product", "Serial", "Power", "Qty", "Rate", "GST", "Total")) {
            return listOf(112f, 94f, 44f, 34f, 62f, 40f, 70f)
        }
        return List(headers.size.coerceAtLeast(1)) { contentWidth / headers.size.coerceAtLeast(1) }
    }

    private class ReportPrintAdapter(
        private val report: ExportReport
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: android.os.Bundle?
        ) {
            callback?.onLayoutFinished(
                PrintDocumentInfo.Builder("${report.title}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            if (destination == null) {
                callback?.onWriteFailed("Print destination unavailable")
                return
            }
            try {
                val document = createPdf(report)
                try {
                    FileOutputStream(destination.fileDescriptor).use(document::writeTo)
                } finally {
                    document.close()
                }
                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (error: Exception) {
                callback?.onWriteFailed(error.message)
            }
        }
    }
}
