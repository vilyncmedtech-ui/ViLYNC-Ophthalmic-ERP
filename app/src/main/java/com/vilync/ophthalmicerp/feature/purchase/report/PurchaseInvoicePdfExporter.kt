package com.vilync.ophthalmicerp.feature.purchase.report

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object PurchaseInvoicePdfExporter {

    // =========================================================
    // PAGE SETTINGS - A4 LANDSCAPE
    // =========================================================

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    private const val LEFT_MARGIN = 28f
    private const val RIGHT_MARGIN = 28f
    private const val TOP_MARGIN = 28f
    private const val BOTTOM_MARGIN = 28f

    private const val CONTENT_WIDTH =
        PAGE_WIDTH - 56


    // =========================================================
    // EXPORT
    // =========================================================

    fun export(
        context: Context,
        report: PurchaseInvoiceReport
    ): Result<File> {

        return runCatching {

            val exportDirectory =
                File(
                    context.cacheDir,
                    "exports"
                )

            if (!exportDirectory.exists()) {
                exportDirectory.mkdirs()
            }


            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())


            val safeInvoiceNumber =
                sanitizeFileName(
                    report.invoiceNumber
                )


            val fileName =
                if (safeInvoiceNumber.isNotBlank()) {

                    "Purchase_Invoice_${safeInvoiceNumber}_$timestamp.pdf"

                } else {

                    "Purchase_Invoice_$timestamp.pdf"
                }


            val file =
                File(
                    exportDirectory,
                    fileName
                )


            val document =
                PdfDocument()


            try {

                PdfWriter(
                    document = document,
                    report = report
                ).write()


                FileOutputStream(file).use {
                        outputStream ->

                    document.writeTo(
                        outputStream
                    )
                }

            } finally {

                document.close()
            }


            file
        }
    }


    // =========================================================
    // SHARE
    // =========================================================

    fun share(
        context: Context,
        file: File
    ): Result<Unit> {

        return runCatching {

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )


            val intent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type = "application/pdf"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Purchase Invoice"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    intent,
                    "Export Purchase Invoice PDF"
                )


            chooser.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )


            context.startActivity(
                chooser
            )
        }
    }


    // =========================================================
    // EXPORT AND SHARE
    // =========================================================

    fun exportAndShare(
        context: Context,
        report: PurchaseInvoiceReport
    ): Result<File> {

        return runCatching {

            val file =
                export(
                    context = context,
                    report = report
                ).getOrThrow()


            share(
                context = context,
                file = file
            ).getOrThrow()


            file
        }
    }


    // =========================================================
    // PDF WRITER
    // =========================================================

    private class PdfWriter(

        private val document: PdfDocument,

        private val report: PurchaseInvoiceReport

    ) {

        private var pageNumber = 0

        private var page:
                PdfDocument.Page? = null

        private var y = TOP_MARGIN


        // -----------------------------------------------------
        // PAINT
        // -----------------------------------------------------

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color = Color.rgb(
                    25,
                    40,
                    65
                )

                textSize = 9f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
            }


        private val linePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        210,
                        216,
                        226
                    )

                strokeWidth = 1f

                style =
                    Paint.Style.STROKE
            }


        private val fillPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.FILL
            }


        // =====================================================
        // WRITE COMPLETE DOCUMENT
        // =====================================================

        fun write() {

            startPage()

            drawReportHeader()

            drawVendorInvoiceDetails()

            drawProductTable()

            drawBillSummary()

            finishPage()
        }


        // =====================================================
        // PAGE
        // =====================================================

        private fun startPage() {

            pageNumber++


            val pageInfo =
                PdfDocument.PageInfo
                    .Builder(
                        PAGE_WIDTH,
                        PAGE_HEIGHT,
                        pageNumber
                    )
                    .create()


            page =
                document.startPage(
                    pageInfo
                )


            y = TOP_MARGIN


            drawPageIdentity()
        }


        private fun finishPage() {

            page?.let {
                document.finishPage(it)
            }

            page = null
        }


        private fun newPage() {

            finishPage()

            startPage()
        }


        private fun ensureSpace(
            requiredHeight: Float
        ) {

            if (
                y + requiredHeight >
                PAGE_HEIGHT - BOTTOM_MARGIN
            ) {

                newPage()
            }
        }


        private fun canvas() =
            requireNotNull(
                page
            ).canvas


        // =====================================================
        // PAGE IDENTITY
        // =====================================================

        private fun drawPageIdentity() {

            val canvas =
                canvas()


            paint.apply {

                textSize = 8f

                color =
                    Color.rgb(
                        105,
                        115,
                        130
                    )

                typeface =
                    Typeface.DEFAULT
            }


            canvas.drawText(
                "ViLYNC OPHTHALMIC ERP",
                LEFT_MARGIN,
                PAGE_HEIGHT - 12f,
                paint
            )


            val pageText =
                "Page $pageNumber"


            val pageTextWidth =
                paint.measureText(
                    pageText
                )


            canvas.drawText(
                pageText,
                PAGE_WIDTH -
                        RIGHT_MARGIN -
                        pageTextWidth,
                PAGE_HEIGHT - 12f,
                paint
            )
        }


        // =====================================================
        // HEADER
        // =====================================================

        private fun drawReportHeader() {

            val canvas =
                canvas()


            // -------------------------------------------------
            // NAVY HEADER
            // -------------------------------------------------

            fillPaint.color =
                Color.rgb(
                    25,
                    48,
                    82
                )


            canvas.drawRoundRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 54f,
                8f,
                8f,
                fillPaint
            )


            paint.apply {

                color =
                    Color.WHITE

                textSize = 17f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


            canvas.drawText(
                "ViLYNC OPHTHALMIC ERP",
                LEFT_MARGIN + 16f,
                y + 22f,
                paint
            )


            paint.apply {

                textSize = 11f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
            }


            canvas.drawText(
                "PURCHASE INVOICE",
                LEFT_MARGIN + 16f,
                y + 41f,
                paint
            )


            paint.apply {

                textSize = 8f
            }


            val generatedText =
                "Generated: ${report.generatedAt}"


            val generatedWidth =
                paint.measureText(
                    generatedText
                )


            canvas.drawText(
                generatedText,
                PAGE_WIDTH -
                        RIGHT_MARGIN -
                        16f -
                        generatedWidth,
                y + 33f,
                paint
            )


            y += 66f
        }


        // =====================================================
        // VENDOR + INVOICE DETAILS
        // =====================================================

        private fun drawVendorInvoiceDetails() {

            ensureSpace(
                105f
            )


            sectionTitle(
                "VENDOR & INVOICE DETAILS"
            )


            val leftX =
                LEFT_MARGIN + 10f

            val rightX =
                LEFT_MARGIN +
                        CONTENT_WIDTH / 2f +
                        10f


            val startY =
                y


            drawLabelValue(
                x = leftX,
                label = "Vendor",
                value = report.supplierName,
                maxWidth = 340f
            )


            drawLabelValue(
                x = rightX,
                label = "Invoice No.",
                value = report.invoiceNumber,
                maxWidth = 320f
            )


            y += 18f


            drawLabelValue(
                x = leftX,
                label = "GSTIN",
                value = report.supplierGstin,
                maxWidth = 340f
            )


            drawLabelValue(
                x = rightX,
                label = "Invoice Date",
                value = report.invoiceDate,
                maxWidth = 320f
            )


            y += 18f


            drawLabelValue(
                x = leftX,
                label = "Address",
                value = report.supplierFullAddress,
                maxWidth = 340f
            )


            drawLabelValue(
                x = rightX,
                label = "Received",
                value = report.receivedDate,
                maxWidth = 320f
            )


            y += 18f


            drawLabelValue(
                x = leftX,
                label = "Payment",
                value = report.paymentType,
                maxWidth = 340f
            )


            drawLabelValue(
                x = rightX,
                label = "Purchase Type",
                value = report.purchaseType,
                maxWidth = 320f
            )


            y += 18f


            if (
                report.reference.isNotBlank()
            ) {

                drawLabelValue(
                    x = leftX,
                    label = "Reference",
                    value = report.reference,
                    maxWidth = CONTENT_WIDTH - 40f
                )

                y += 18f
            }


            if (y < startY + 80f) {
                y = startY + 80f
            }


            y += 6f
        }


        // =====================================================
        // PRODUCT TABLE
        // =====================================================

        private fun drawProductTable() {

            ensureSpace(
                80f
            )


            sectionTitle(
                "PRODUCT DETAILS"
            )


            drawProductHeader()


            report.items.forEach {
                    item ->


                ensureSpace(
                    46f +
                            (
                                    item.lensDetails.size *
                                            13f
                                    )
                )


                val gross =
                    item.quantity *
                            item.purchaseRate


                val discount =
                    gross *
                            (
                                    item.discountPercent /
                                            100.0
                                    )


                val taxable =
                    gross -
                            discount


                val gst =
                    taxable *
                            (
                                    item.gstPercent /
                                            100.0
                                    )


                val total =
                    taxable +
                            gst


                val rowValues =
                    listOf(
                        item.productName,
                        item.model,
                        item.hsnCode,
                        item.power,
                        item.batchNumber,
                        item.quantity.toString(),
                        money(item.purchaseRate),
                        "${money(item.discountPercent)}%",
                        "${money(item.gstPercent)}%",
                        money(total)
                    )


                drawProductRow(
                    rowValues
                )


                if (
                    item.lensDetails.isNotEmpty()
                ) {

                    item.lensDetails.forEachIndexed {
                            index,
                            lens ->


                        ensureSpace(
                            15f
                        )


                        paint.apply {

                            color =
                                Color.rgb(
                                    80,
                                    90,
                                    105
                                )

                            textSize = 7.5f

                            typeface =
                                Typeface.DEFAULT
                        }


                        val serialText =
                            "Unit ${index + 1}: " +
                                    "${lens.serialNumber}  |  " +
                                    "Expiry ${formatExpiry(lens.expiryDate)}"


                        canvas().drawText(
                            fitText(
                                serialText,
                                CONTENT_WIDTH - 35f,
                                paint
                            ),
                            LEFT_MARGIN + 18f,
                            y + 10f,
                            paint
                        )


                        y += 13f
                    }
                }


                y += 4f
            }


            y += 8f
        }


        private fun drawProductHeader() {

            ensureSpace(
                25f
            )


            val canvas =
                canvas()


            fillPaint.color =
                Color.rgb(
                    232,
                    239,
                    249
                )


            canvas.drawRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 22f,
                fillPaint
            )


            val headings =
                listOf(
                    "Product",
                    "Model",
                    "HSN",
                    "Power",
                    "Batch",
                    "Qty",
                    "Rate",
                    "Disc",
                    "GST",
                    "Total"
                )


            val widths =
                productColumnWidths()


            var x =
                LEFT_MARGIN


            paint.apply {

                color =
                    Color.rgb(
                        25,
                        48,
                        82
                    )

                textSize = 7.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


            headings.forEachIndexed {
                    index,
                    heading ->

                canvas.drawText(
                    heading,
                    x + 4f,
                    y + 14f,
                    paint
                )

                x +=
                    widths[index]
            }


            y += 22f
        }


        private fun drawProductRow(
            values: List<String>
        ) {

            val canvas =
                canvas()


            val widths =
                productColumnWidths()


            val rowHeight =
                26f


            var x =
                LEFT_MARGIN


            paint.apply {

                color =
                    Color.rgb(
                        35,
                        45,
                        60
                    )

                textSize = 7f

                typeface =
                    Typeface.DEFAULT
            }


            values.forEachIndexed {
                    index,
                    value ->


                canvas.drawRect(
                    x,
                    y,
                    x + widths[index],
                    y + rowHeight,
                    linePaint
                )


                canvas.drawText(
                    fitText(
                        value,
                        widths[index] - 8f,
                        paint
                    ),
                    x + 4f,
                    y + 16f,
                    paint
                )


                x +=
                    widths[index]
            }


            y += rowHeight
        }


        private fun productColumnWidths():
                List<Float> {

            return listOf(
                142f, // Product
                86f,  // Model
                60f,  // HSN
                54f,  // Power
                72f,  // Batch
                38f,  // Qty
                70f,  // Rate
                58f,  // Discount
                52f,  // GST
                154f  // Total
            )
        }


        // =====================================================
        // BILL SUMMARY
        // =====================================================

        private fun drawBillSummary() {

            ensureSpace(
                180f
            )


            sectionTitle(
                "BILL SUMMARY"
            )


            val summaryWidth =
                320f


            val summaryX =
                PAGE_WIDTH -
                        RIGHT_MARGIN -
                        summaryWidth


            drawSummaryLine(
                summaryX,
                "Gross Amount",
                report.grossAmount
            )


            drawSummaryLine(
                summaryX,
                "Discount",
                report.discountAmount
            )


            drawSummaryLine(
                summaryX,
                "Taxable Amount",
                report.taxableAmount
            )


            drawSummaryLine(
                summaryX,
                "GST",
                report.taxAmount
            )


            drawSummaryLine(
                summaryX,
                "Adjustment",
                report.adjustmentAmount
            )


            drawSummaryLine(
                summaryX,
                "Round Off",
                report.roundOffAmount
            )


            // -------------------------------------------------
            // NET AMOUNT
            // -------------------------------------------------

            fillPaint.color =
                Color.rgb(
                    224,
                    244,
                    232
                )


            canvas().drawRoundRect(
                summaryX,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 28f,
                6f,
                6f,
                fillPaint
            )


            paint.apply {

                color =
                    Color.rgb(
                        20,
                        50,
                        35
                    )

                textSize = 10f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


            canvas().drawText(
                "NET AMOUNT",
                summaryX + 10f,
                y + 18f,
                paint
            )


            val netText =
                "Rs. ${money(report.netAmount)}"


            canvas().drawText(
                netText,
                PAGE_WIDTH -
                        RIGHT_MARGIN -
                        10f -
                        paint.measureText(netText),
                y + 18f,
                paint
            )


            y += 36f


            drawSummaryLine(
                summaryX,
                "Paid Amount",
                report.paidAmount
            )


            drawSummaryLine(
                summaryX,
                "Due Amount",
                report.dueAmount,
                bold = true
            )


            y += 10f


            paint.apply {

                color =
                    Color.rgb(
                        90,
                        100,
                        115
                    )

                textSize = 8f

                typeface =
                    Typeface.DEFAULT
            }


            canvas().drawText(
                "Total Quantity: ${report.totalQuantity}    |    " +
                        "Serial Units: ${report.totalSerialUnits}",
                LEFT_MARGIN,
                y,
                paint
            )
        }


        // =====================================================
        // SECTION TITLE
        // =====================================================

        private fun sectionTitle(
            title: String
        ) {

            ensureSpace(
                28f
            )


            fillPaint.color =
                Color.rgb(
                    242,
                    245,
                    250
                )


            canvas().drawRoundRect(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + 22f,
                5f,
                5f,
                fillPaint
            )


            paint.apply {

                color =
                    Color.rgb(
                        25,
                        48,
                        82
                    )

                textSize = 9f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


            canvas().drawText(
                title,
                LEFT_MARGIN + 8f,
                y + 15f,
                paint
            )


            y += 29f
        }


        // =====================================================
        // LABEL VALUE
        // =====================================================

        private fun drawLabelValue(
            x: Float,
            label: String,
            value: String,
            maxWidth: Float
        ) {

            val canvas =
                canvas()


            paint.apply {

                color =
                    Color.rgb(
                        100,
                        110,
                        125
                    )

                textSize = 7.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


            canvas.drawText(
                "$label:",
                x,
                y + 10f,
                paint
            )


            val labelWidth =
                paint.measureText(
                    "$label:"
                )


            paint.apply {

                color =
                    Color.rgb(
                        30,
                        40,
                        55
                    )

                typeface =
                    Typeface.DEFAULT
            }


            canvas.drawText(
                fitText(
                    value.ifBlank { "-" },
                    maxWidth -
                            labelWidth -
                            10f,
                    paint
                ),
                x + labelWidth + 6f,
                y + 10f,
                paint
            )
        }


        // =====================================================
        // SUMMARY LINE
        // =====================================================

        private fun drawSummaryLine(
            x: Float,
            label: String,
            amount: Double,
            bold: Boolean = false
        ) {

            ensureSpace(
                22f
            )


            paint.apply {

                color =
                    Color.rgb(
                        40,
                        50,
                        65
                    )

                textSize = 8.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        if (bold) {
                            Typeface.BOLD
                        } else {
                            Typeface.NORMAL
                        }
                    )
            }


            canvas().drawText(
                label,
                x + 8f,
                y + 12f,
                paint
            )


            val amountText =
                "Rs. ${money(amount)}"


            canvas().drawText(
                amountText,
                PAGE_WIDTH -
                        RIGHT_MARGIN -
                        8f -
                        paint.measureText(amountText),
                y + 12f,
                paint
            )


            y += 20f
        }


        // =====================================================
        // FIT TEXT
        // =====================================================

        private fun fitText(
            value: String,
            maxWidth: Float,
            paint: Paint
        ): String {

            if (
                paint.measureText(value) <=
                maxWidth
            ) {

                return value
            }


            val ellipsis =
                "..."


            var result =
                value


            while (
                result.isNotEmpty() &&
                paint.measureText(
                    result + ellipsis
                ) > maxWidth
            ) {

                result =
                    result.dropLast(1)
            }


            return result +
                    ellipsis
        }
    }


    // =========================================================
    // EXPIRY FORMAT
    // =========================================================

    private fun formatExpiry(
        expiryDate: String
    ): String {

        val clean =
            expiryDate
                .trim()
                .filter {
                    it.isDigit()
                }


        return if (
            clean.length == 4
        ) {

            "${clean.substring(0, 2)}-${clean.substring(2, 4)}"

        } else {

            expiryDate.trim()
        }
    }


    // =========================================================
    // MONEY
    // =========================================================

    private fun money(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.2f",
            value
        )
    }


    // =========================================================
    // SAFE FILE NAME
    // =========================================================

    private fun sanitizeFileName(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex(
                    """[\\/:*?"<>|]"""
                ),
                "_"
            )
            .take(60)
    }
}