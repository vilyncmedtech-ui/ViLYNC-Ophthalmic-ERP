package com.vilync.ophthalmicerp.feature.gst.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.feature.gst.model.GstDocumentRow
import com.vilync.ophthalmicerp.feature.gst.model.GstHsnRow
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GstPdfExporter {

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val LEFT_MARGIN = 26f
    private const val RIGHT_MARGIN = 26f
    private const val TOP_MARGIN = 26f
    private const val BOTTOM_MARGIN = 28f
    private const val ROW_HEIGHT = 22f

    fun export(
        context: Context,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ): Result<File> = runCatching {
        val exportDirectory = File(context.cacheDir, "exports")
        if (!exportDirectory.exists() && !exportDirectory.mkdirs() && !exportDirectory.exists()) {
            error("Unable to create export directory.")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeTitle = reportType.title
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "GST_Report" }

        val file = File(exportDirectory, "${safeTitle}_$timestamp.pdf")
        val document = PdfDocument()

        try {
            PdfWriter(
                document = document,
                reportType = reportType,
                snapshot = snapshot,
                financialYearDisplayName = financialYearDisplayName
            ).write()

            file.outputStream().use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        file
    }

    fun share(
        context: Context,
        file: File,
        reportType: GstReportType
    ): Result<Unit> = runCatching {
        require(file.exists()) { "GST PDF file does not exist." }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, reportType.title)
            putExtra(Intent.EXTRA_TEXT, "ViLYNC ERP - ${reportType.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export ${reportType.title} PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun exportAndShare(
        context: Context,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ): Result<File> = runCatching {
        val file = export(
            context = context,
            reportType = reportType,
            snapshot = snapshot,
            financialYearDisplayName = financialYearDisplayName
        ).getOrThrow()

        share(
            context = context,
            file = file,
            reportType = reportType
        ).getOrThrow()

        file
    }

    private class PdfWriter(
        private val document: PdfDocument,
        private val reportType: GstReportType,
        private val snapshot: GstReportSnapshot,
        private val financialYearDisplayName: String
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = TOP_MARGIN

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 45, 60)
            textSize = 8f
            typeface = Typeface.DEFAULT
        }

        private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 48, 82)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(210, 216, 226)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        fun write() {
            startPage()
            drawHeader()

            when (reportType) {
                GstReportType.GSTR1 -> {
                    drawSummary(
                        listOf(
                            "Taxable Outward Supplies" to money(snapshot.outputTaxable),
                            "Output GST" to money(snapshot.outputGst),
                            "Credit Note GST" to money(snapshot.creditNoteGst),
                            "Adjusted Output GST" to money(snapshot.adjustedOutputGst)
                        )
                    )
                    drawDocumentTable("B2B INVOICES", snapshot.b2bSales)
                    drawDocumentTable("B2C INVOICES", snapshot.b2cSales)
                }

                GstReportType.GSTR3B -> {
                    drawSummary(
                        listOf(
                            "Outward Taxable" to money(snapshot.outputTaxable),
                            "Output GST" to money(snapshot.outputGst),
                            "Input GST / ITC" to money(snapshot.inputGst),
                            "Net GST Position" to money(snapshot.netGstPosition)
                        )
                    )
                    drawNote(
                        "Purchase ITC is shown from persisted purchase GST totals. " +
                                "Historical purchase GST is not reclassified into tax heads."
                    )
                }

                GstReportType.SALES_REGISTER -> {
                    drawSummary(
                        listOf(
                            "Posted Sales" to snapshot.sales.size.toString(),
                            "Taxable Value" to money(snapshot.outputTaxable),
                            "Output GST" to money(snapshot.outputGst)
                        )
                    )
                    drawDocumentTable("SALES GST REGISTER", snapshot.sales)
                }

                GstReportType.PURCHASE_REGISTER -> {
                    drawSummary(
                        listOf(
                            "Purchases" to snapshot.purchases.size.toString(),
                            "Taxable Value" to money(snapshot.inputTaxable),
                            "Input GST" to money(snapshot.inputGst)
                        )
                    )
                    drawDocumentTable("PURCHASE GST REGISTER", snapshot.purchases)
                }

                GstReportType.HSN -> drawHsnTable(snapshot.hsnRows)

                GstReportType.B2B -> {
                    drawSummary(
                        listOf(
                            "B2B Invoices" to snapshot.b2bSales.size.toString(),
                            "Taxable Value" to money(snapshot.b2bSales.sumOf { it.taxableAmount }),
                            "GST" to money(snapshot.b2bSales.sumOf { it.totalGst })
                        )
                    )
                    drawDocumentTable("B2B SALES", snapshot.b2bSales)
                }

                GstReportType.B2C -> {
                    drawSummary(
                        listOf(
                            "B2C Invoices" to snapshot.b2cSales.size.toString(),
                            "Taxable Value" to money(snapshot.b2cSales.sumOf { it.taxableAmount }),
                            "GST" to money(snapshot.b2cSales.sumOf { it.totalGst })
                        )
                    )
                    drawDocumentTable("B2C SALES", snapshot.b2cSales)
                }

                GstReportType.RETURNS -> {
                    drawSummary(
                        listOf(
                            "Posted Sales Credit Notes" to snapshot.creditNotes.size.toString(),
                            "Taxable Adjustment" to money(snapshot.creditNoteTaxable),
                            "GST Adjustment" to money(snapshot.creditNoteGst)
                        )
                    )
                    drawDocumentTable("RETURNS / ADJUSTMENTS", snapshot.creditNotes)
                    drawNote(
                        "Purchase Returns remain in the existing Purchase Return Register " +
                                "and are not silently netted into ITC here."
                    )
                }

                GstReportType.ITC -> {
                    drawSummary(
                        listOf(
                            "Purchase Taxable Value" to money(snapshot.inputTaxable),
                            "Total Input GST / ITC" to money(snapshot.inputGst)
                        )
                    )
                    drawNote(
                        "Input GST is read from persisted Purchase GST. " +
                                "No historical CGST/SGST/IGST reclassification is performed."
                    )
                    drawDocumentTable("ITC PURCHASE DETAILS", snapshot.purchases)
                }

                GstReportType.OUTPUT_TAX -> {
                    drawSummary(
                        listOf(
                            "Output CGST" to money(snapshot.outputCgst),
                            "Output SGST" to money(snapshot.outputSgst),
                            "Output IGST" to money(snapshot.outputIgst),
                            "Total Output GST" to money(snapshot.outputGst)
                        )
                    )
                }

                GstReportType.TAX_HEADS -> {
                    drawSummary(
                        listOf(
                            "CGST" to money(snapshot.outputCgst),
                            "SGST" to money(snapshot.outputSgst),
                            "IGST" to money(snapshot.outputIgst),
                            "Total" to money(snapshot.outputGst)
                        )
                    )
                    drawNote("Tax-head summary uses persisted POSTED Sales tax heads only.")
                }
            }

            finishPage()
        }

        private fun startPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()
            page = document.startPage(info)
            y = TOP_MARGIN
            drawFooter()
        }

        private fun finishPage() {
            page?.let(document::finishPage)
            page = null
        }

        private fun newPage() {
            finishPage()
            startPage()
            drawContinuationHeader()
        }

        private fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - BOTTOM_MARGIN - 16f) {
                newPage()
            }
        }

        private fun canvas() = requireNotNull(page).canvas

        private fun drawHeader() {
            fillPaint.color = Color.rgb(25, 48, 82)
            canvas().drawRoundRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 58f,
                8f,
                8f,
                fillPaint
            )

            val whiteTitle = Paint(textPaint).apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val whiteSub = Paint(textPaint).apply {
                color = Color.WHITE
                textSize = 10f
            }

            canvas().drawText("ViLYNC OPHTHALMIC ERP", LEFT_MARGIN + 14f, y + 22f, whiteTitle)
            canvas().drawText(reportType.title, LEFT_MARGIN + 14f, y + 42f, whiteSub)

            val fy = "Financial Year: $financialYearDisplayName"
            canvas().drawText(
                fy,
                PAGE_WIDTH - RIGHT_MARGIN - 14f - whiteSub.measureText(fy),
                y + 25f,
                whiteSub
            )

            val generated = "Generated: ${displayDateTime()}"
            canvas().drawText(
                generated,
                PAGE_WIDTH - RIGHT_MARGIN - 14f - whiteSub.measureText(generated),
                y + 42f,
                whiteSub
            )

            y += 70f
        }

        private fun drawContinuationHeader() {
            boldPaint.textSize = 10f
            canvas().drawText(
                "${reportType.title} - Continued",
                LEFT_MARGIN,
                y + 10f,
                boldPaint
            )
            y += 22f
        }

        private fun drawSummary(values: List<Pair<String, String>>) {
            sectionTitle("SUMMARY")
            values.forEach { (label, value) ->
                ensureSpace(20f)
                canvas().drawText(label, LEFT_MARGIN + 8f, y + 13f, textPaint)

                val valuePaint = Paint(boldPaint).apply { textSize = 8.5f }
                canvas().drawText(
                    value,
                    PAGE_WIDTH - RIGHT_MARGIN - 8f - valuePaint.measureText(value),
                    y + 13f,
                    valuePaint
                )
                canvas().drawLine(
                    LEFT_MARGIN,
                    y + 18f,
                    PAGE_WIDTH - RIGHT_MARGIN,
                    y + 18f,
                    borderPaint
                )
                y += 20f
            }
            y += 6f
        }

        private fun drawDocumentTable(title: String, rows: List<GstDocumentRow>) {
            ensureSpace(55f)
            sectionTitle(title)

            if (rows.isEmpty()) {
                drawNote("No records found.")
                return
            }

            drawDocumentHeader()

            rows.forEach { row ->
                ensureSpace(ROW_HEIGHT)
                drawDocumentRow(row)
            }
            y += 8f
        }

        private fun drawDocumentHeader() {
            fillPaint.color = Color.rgb(229, 238, 250)
            canvas().drawRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + ROW_HEIGHT,
                fillPaint
            )

            val headers = listOf(
                "Document", "Date", "Party", "GSTIN",
                "Taxable", "CGST", "SGST", "IGST", "Total"
            )
            val widths = documentWidths()
            var x = LEFT_MARGIN

            headers.forEachIndexed { index, header ->
                canvas().drawRect(x, y, x + widths[index], y + ROW_HEIGHT, borderPaint)
                canvas().drawText(
                    fitText(header, boldPaint, widths[index] - 8f),
                    x + 4f,
                    y + 14f,
                    boldPaint
                )
                x += widths[index]
            }
            y += ROW_HEIGHT
        }

        private fun drawDocumentRow(row: GstDocumentRow) {
            val values = listOf(
                row.documentNumber,
                row.documentDate,
                row.partyName,
                row.gstin,
                money(row.taxableAmount),
                money(row.cgstAmount),
                money(row.sgstAmount),
                money(row.igstAmount),
                money(row.totalAmount)
            )
            val widths = documentWidths()
            var x = LEFT_MARGIN

            values.forEachIndexed { index, value ->
                canvas().drawRect(x, y, x + widths[index], y + ROW_HEIGHT, borderPaint)
                canvas().drawText(
                    fitText(value, textPaint, widths[index] - 8f),
                    x + 4f,
                    y + 14f,
                    textPaint
                )
                x += widths[index]
            }
            y += ROW_HEIGHT
        }

        private fun documentWidths(): List<Float> =
            listOf(82f, 66f, 132f, 98f, 82f, 70f, 70f, 70f, 94f)

        private fun drawHsnTable(rows: List<GstHsnRow>) {
            sectionTitle("HSN SUMMARY")

            if (rows.isEmpty()) {
                drawNote("No HSN transaction data found for this Financial Year.")
                return
            }

            val widths = listOf(105f, 270f, 70f, 150f, 145f)
            val headers = listOf("HSN", "Description", "Qty", "Taxable", "GST")

            fillPaint.color = Color.rgb(255, 248, 223)
            canvas().drawRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + ROW_HEIGHT,
                fillPaint
            )

            var x = LEFT_MARGIN
            headers.forEachIndexed { index, header ->
                canvas().drawRect(x, y, x + widths[index], y + ROW_HEIGHT, borderPaint)
                canvas().drawText(header, x + 4f, y + 14f, boldPaint)
                x += widths[index]
            }
            y += ROW_HEIGHT

            rows.forEach { row ->
                ensureSpace(ROW_HEIGHT)
                val values = listOf(
                    row.hsnCode,
                    row.description,
                    row.quantity.toString(),
                    money(row.taxableAmount),
                    money(row.gstAmount)
                )
                x = LEFT_MARGIN
                values.forEachIndexed { index, value ->
                    canvas().drawRect(x, y, x + widths[index], y + ROW_HEIGHT, borderPaint)
                    canvas().drawText(
                        fitText(value, textPaint, widths[index] - 8f),
                        x + 4f,
                        y + 14f,
                        textPaint
                    )
                    x += widths[index]
                }
                y += ROW_HEIGHT
            }
        }

        private fun sectionTitle(title: String) {
            ensureSpace(30f)
            fillPaint.color = Color.rgb(242, 246, 252)
            canvas().drawRoundRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 24f,
                5f,
                5f,
                fillPaint
            )
            val paint = Paint(boldPaint).apply { textSize = 9.5f }
            canvas().drawText(title, LEFT_MARGIN + 8f, y + 16f, paint)
            y += 30f
        }

        private fun drawNote(message: String) {
            val lines = wrapText(
                text = message,
                paint = textPaint,
                maxWidth = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN - 20f
            )
            ensureSpace(lines.size * 13f + 18f)

            fillPaint.color = Color.rgb(247, 249, 255)
            val height = lines.size * 13f + 14f
            canvas().drawRoundRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + height,
                6f,
                6f,
                fillPaint
            )

            lines.forEachIndexed { index, line ->
                canvas().drawText(
                    line,
                    LEFT_MARGIN + 10f,
                    y + 15f + index * 13f,
                    textPaint
                )
            }
            y += height + 8f
        }

        private fun drawFooter() {
            val footer = Paint(textPaint).apply {
                color = Color.DKGRAY
                textSize = 7.5f
            }
            canvas().drawText(
                "Generated by ViLYNC ERP",
                LEFT_MARGIN,
                PAGE_HEIGHT - 11f,
                footer
            )
            val pageText = "Page $pageNumber"
            canvas().drawText(
                pageText,
                PAGE_WIDTH - RIGHT_MARGIN - footer.measureText(pageText),
                PAGE_HEIGHT - 11f,
                footer
            )
        }

        private fun fitText(text: String, paint: Paint, maximumWidth: Float): String {
            if (paint.measureText(text) <= maximumWidth) return text
            val ellipsis = "..."
            var end = text.length
            while (end > 0) {
                val candidate = text.substring(0, end) + ellipsis
                if (paint.measureText(candidate) <= maximumWidth) return candidate
                end--
            }
            return ellipsis
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val words = text.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = ""

            words.forEach { word ->
                val candidate = if (current.isBlank()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotBlank()) lines.add(current)
                    current = word
                }
            }

            if (current.isNotBlank()) lines.add(current)
            return lines
        }
    }

    private fun money(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)

    private fun displayDateTime(): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
}
