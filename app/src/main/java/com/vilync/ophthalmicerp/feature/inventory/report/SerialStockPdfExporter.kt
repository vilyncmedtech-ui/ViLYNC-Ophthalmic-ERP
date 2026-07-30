package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.data.dao.SerialStockRow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object SerialStockPdfExporter {


    // =========================================================
    // PAGE CONFIGURATION - A4 LANDSCAPE
    // =========================================================

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    private const val LEFT_MARGIN = 24f
    private const val RIGHT_MARGIN = 24f
    private const val TOP_MARGIN = 24f
    private const val BOTTOM_MARGIN = 25f

    /*
     * Compact row height is intentional.
     *
     * Serial Stock may contain 1000+ individual units,
     * therefore the PDF is designed as a high-density
     * inventory register rather than a product-detail report.
     */
    private const val ROW_HEIGHT = 19f

    private const val TABLE_START_X = LEFT_MARGIN


    // =========================================================
    // COLUMN WIDTHS
    // =========================================================

    private const val SERIAL_WIDTH = 105f
    private const val PRODUCT_WIDTH = 150f
    private const val MODEL_WIDTH = 90f
    private const val POWER_WIDTH = 55f
    private const val BATCH_WIDTH = 80f
    private const val EXPIRY_WIDTH = 55f
    private const val SUPPLIER_WIDTH = 145f
    private const val INVOICE_WIDTH = 95f
    private const val STATUS_WIDTH = 74f


    // =========================================================
    // EXPORT
    // =========================================================

    fun export(
        context: Context,
        rows: List<SerialStockRow>,
        searchQuery: String = "",
        selectedStatus: String = "ALL"
    ): Result<File> {

        return runCatching {

            val exportDirectory =
                File(
                    context.cacheDir,
                    "exports"
                )


            if (!exportDirectory.exists()) {

                val created =
                    exportDirectory.mkdirs()

                if (
                    !created &&
                    !exportDirectory.exists()
                ) {

                    error(
                        "Unable to create export directory."
                    )
                }
            }


            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(
                    Date()
                )


            val file =
                File(
                    exportDirectory,
                    "Serial_Stock_Register_$timestamp.pdf"
                )


            val document =
                PdfDocument()


            try {

                writeReport(
                    document = document,
                    rows = rows,
                    searchQuery = searchQuery,
                    selectedStatus =
                        selectedStatus
                )


                file.outputStream().use {
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
    // SHARE PDF
    // =========================================================

    fun share(
        context: Context,
        file: File
    ): Result<Unit> {

        return runCatching {

            require(
                file.exists()
            ) {
                "Serial Stock PDF file does not exist."
            }


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

                    type =
                        "application/pdf"


                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )


                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Serial Stock Register"
                    )


                    putExtra(
                        Intent.EXTRA_TEXT,
                        "ViLYNC ERP - Serial Stock Register"
                    )


                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    intent,
                    "Export Serial Stock Register PDF"
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
    // EXPORT + SHARE
    // =========================================================

    fun exportAndShare(
        context: Context,
        rows: List<SerialStockRow>,
        searchQuery: String = "",
        selectedStatus: String = "ALL"
    ): Result<File> {

        return runCatching {

            val file =
                export(
                    context = context,
                    rows = rows,
                    searchQuery = searchQuery,
                    selectedStatus =
                        selectedStatus
                ).getOrThrow()


            share(
                context = context,
                file = file
            ).getOrThrow()


            file
        }
    }


    // =========================================================
    // REPORT WRITER
    // =========================================================

    private fun writeReport(
        document: PdfDocument,
        rows: List<SerialStockRow>,
        searchQuery: String,
        selectedStatus: String
    ) {

        var pageNumber = 1

        var rowIndex = 0


        /*
         * At least one page is generated even when there are
         * currently no matching Serial Stock rows.
         */
        do {

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()


            val page =
                document.startPage(
                    pageInfo
                )


            val canvas =
                page.canvas


            var currentY =
                drawPageHeader(
                    canvas = canvas,
                    rows = rows,
                    searchQuery =
                        searchQuery,
                    selectedStatus =
                        selectedStatus,
                    pageNumber =
                        pageNumber
                )


            currentY =
                drawTableHeader(
                    canvas = canvas,
                    top = currentY
                )


            while (
                rowIndex < rows.size &&
                currentY + ROW_HEIGHT <
                PAGE_HEIGHT -
                BOTTOM_MARGIN -
                28f
            ) {

                val row =
                    rows[
                        rowIndex
                    ]


                drawSerialRow(
                    canvas = canvas,
                    top = currentY,
                    row = row
                )


                currentY +=
                    ROW_HEIGHT


                rowIndex++
            }


            // =================================================
            // FINAL PAGE SUMMARY
            // =================================================

            if (
                rowIndex >= rows.size
            ) {

                currentY +=
                    7f


                if (
                    currentY + 20f <
                    PAGE_HEIGHT -
                    BOTTOM_MARGIN
                ) {

                    drawTotals(
                        canvas = canvas,
                        top = currentY,
                        rows = rows
                    )
                }
            }


            drawFooter(
                canvas = canvas,
                pageNumber =
                    pageNumber
            )


            document.finishPage(
                page
            )


            pageNumber++

        } while (
            rowIndex < rows.size
        )
    }


    // =========================================================
    // PAGE HEADER
    // =========================================================

    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        rows: List<SerialStockRow>,
        searchQuery: String,
        selectedStatus: String,
        pageNumber: Int
    ): Float {

        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    19f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val subtitlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    12f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val normalPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    8.5f

                color =
                    android.graphics.Color.DKGRAY
            }


        var y =
            TOP_MARGIN


        // =====================================================
        // COMPANY
        // =====================================================

        canvas.drawText(
            "ViLYNC ERP",
            LEFT_MARGIN,
            y,
            titlePaint
        )


        // =====================================================
        // PAGE NUMBER
        // =====================================================

        canvas.drawText(
            "Page $pageNumber",
            PAGE_WIDTH -
                    RIGHT_MARGIN -
                    45f,
            y,
            normalPaint
        )


        y +=
            21f


        // =====================================================
        // REPORT TITLE
        // =====================================================

        canvas.drawText(
            "Serial Stock Register",
            LEFT_MARGIN,
            y,
            subtitlePaint
        )


        y +=
            15f


        // =====================================================
        // GENERATED DATE
        // =====================================================

        canvas.drawText(
            "Generated At: ${currentDisplayDateTime()}",
            LEFT_MARGIN,
            y,
            normalPaint
        )


        // =====================================================
        // TOTAL UNITS
        // =====================================================

        canvas.drawText(
            "Total Units: ${rows.size}",
            LEFT_MARGIN + 210f,
            y,
            normalPaint
        )


        // =====================================================
        // IN STOCK
        // =====================================================

        val inStock =
            rows.count {

                it.status.equals(
                    other = "IN_STOCK",
                    ignoreCase = true
                )
            }


        canvas.drawText(
            "In Stock: $inStock",
            LEFT_MARGIN + 310f,
            y,
            normalPaint
        )


        // =====================================================
        // OTHER / ISSUED
        // =====================================================

        val issued =
            rows.size -
                    inStock


        canvas.drawText(
            "Issued / Other: $issued",
            LEFT_MARGIN + 390f,
            y,
            normalPaint
        )


        // =====================================================
        // FILTER INFORMATION
        // =====================================================

        val filterParts =
            mutableListOf<String>()


        if (
            searchQuery.isNotBlank()
        ) {

            filterParts.add(
                "Search: ${searchQuery.trim()}"
            )
        }


        if (
            selectedStatus.isNotBlank() &&
            !selectedStatus.equals(
                other = "ALL",
                ignoreCase = true
            )
        ) {

            filterParts.add(
                "Status: ${
                    formatStatus(
                        selectedStatus
                    )
                }"
            )
        }


        if (
            filterParts.isNotEmpty()
        ) {

            y +=
                14f


            val filterText =
                filterParts.joinToString(
                    separator = "    |    "
                )


            canvas.drawText(
                fitText(
                    text = filterText,
                    paint = normalPaint,
                    maximumWidth =
                        PAGE_WIDTH -
                                LEFT_MARGIN -
                                RIGHT_MARGIN
                ),
                LEFT_MARGIN,
                y,
                normalPaint
            )
        }


        return y + 13f
    }


    // =========================================================
    // TABLE HEADER
    // =========================================================

    private fun drawTableHeader(
        canvas: android.graphics.Canvas,
        top: Float
    ): Float {

        val fillPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    android.graphics.Color.rgb(
                        229,
                        238,
                        250
                    )

                style =
                    Paint.Style.FILL
            }


        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    android.graphics.Color.LTGRAY

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    0.8f
            }


        val textPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    7.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val bottom =
            top +
                    ROW_HEIGHT


        canvas.drawRect(
            TABLE_START_X,
            top,
            PAGE_WIDTH -
                    RIGHT_MARGIN,
            bottom,
            fillPaint
        )


        drawTableGrid(
            canvas = canvas,
            top = top,
            bottom = bottom,
            paint = borderPaint
        )


        val textY =
            top +
                    12.5f


        var x =
            TABLE_START_X


        drawCellText(
            canvas = canvas,
            text = "Serial No.",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = SERIAL_WIDTH
        )

        x +=
            SERIAL_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Product",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = PRODUCT_WIDTH
        )

        x +=
            PRODUCT_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Model",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = MODEL_WIDTH
        )

        x +=
            MODEL_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Power",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = POWER_WIDTH
        )

        x +=
            POWER_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Batch",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = BATCH_WIDTH
        )

        x +=
            BATCH_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Expiry",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = EXPIRY_WIDTH
        )

        x +=
            EXPIRY_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Supplier",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = SUPPLIER_WIDTH
        )

        x +=
            SUPPLIER_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Invoice",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = INVOICE_WIDTH
        )

        x +=
            INVOICE_WIDTH


        drawCellText(
            canvas = canvas,
            text = "Status",
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = STATUS_WIDTH
        )


        return bottom
    }


    // =========================================================
    // SERIAL ROW
    // =========================================================

    private fun drawSerialRow(
        canvas: android.graphics.Canvas,
        top: Float,
        row: SerialStockRow
    ) {

        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    android.graphics.Color.LTGRAY

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    0.6f
            }


        val textPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    7.3f
            }


        val serialPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    7.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val bottom =
            top +
                    ROW_HEIGHT


        drawTableGrid(
            canvas = canvas,
            top = top,
            bottom = bottom,
            paint = borderPaint
        )


        val textY =
            top +
                    12.5f


        var x =
            TABLE_START_X


        // =====================================================
        // SERIAL
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.serialNumber,
            startX = x,
            baselineY = textY,
            paint = serialPaint,
            width = SERIAL_WIDTH
        )

        x +=
            SERIAL_WIDTH


        // =====================================================
        // PRODUCT
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.productName,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = PRODUCT_WIDTH
        )

        x +=
            PRODUCT_WIDTH


        // =====================================================
        // MODEL
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.model,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = MODEL_WIDTH
        )

        x +=
            MODEL_WIDTH


        // =====================================================
        // POWER
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.power,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = POWER_WIDTH
        )

        x +=
            POWER_WIDTH


        // =====================================================
        // BATCH
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.batchNumber,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = BATCH_WIDTH
        )

        x +=
            BATCH_WIDTH


        // =====================================================
        // EXPIRY
        // =====================================================

        drawCellText(
            canvas = canvas,
            text =
                formatExpiry(
                    row.expiryDate
                ),
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = EXPIRY_WIDTH
        )

        x +=
            EXPIRY_WIDTH


        // =====================================================
        // SUPPLIER
        // =====================================================

        drawCellText(
            canvas = canvas,
            text = row.supplierName,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = SUPPLIER_WIDTH
        )

        x +=
            SUPPLIER_WIDTH


        // =====================================================
        // INVOICE
        // =====================================================

        drawCellText(
            canvas = canvas,
            text =
                row.purchaseInvoiceNumber,
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = INVOICE_WIDTH
        )

        x +=
            INVOICE_WIDTH


        // =====================================================
        // STATUS
        // =====================================================

        drawCellText(
            canvas = canvas,
            text =
                formatStatus(
                    row.status
                ),
            startX = x,
            baselineY = textY,
            paint = textPaint,
            width = STATUS_WIDTH
        )
    }


    // =========================================================
    // TOTALS
    // =========================================================

    private fun drawTotals(
        canvas: android.graphics.Canvas,
        top: Float,
        rows: List<SerialStockRow>
    ) {

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    9f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val inStock =
            rows.count {

                it.status.equals(
                    other = "IN_STOCK",
                    ignoreCase = true
                )
            }


        val other =
            rows.size -
                    inStock


        val summary =
            "Total Serial Units: ${rows.size}" +
                    "    |    " +
                    "In Stock: $inStock" +
                    "    |    " +
                    "Issued / Other: $other"


        canvas.drawText(
            summary,
            LEFT_MARGIN,
            top + 13f,
            paint
        )
    }


    // =========================================================
    // FOOTER
    // =========================================================

    private fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNumber: Int
    ) {

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize =
                    7.5f

                color =
                    android.graphics.Color.DKGRAY
            }


        canvas.drawText(
            "Generated by ViLYNC ERP",
            LEFT_MARGIN,
            PAGE_HEIGHT - 12f,
            paint
        )


        canvas.drawText(
            "Page $pageNumber",
            PAGE_WIDTH -
                    RIGHT_MARGIN -
                    42f,
            PAGE_HEIGHT - 12f,
            paint
        )
    }


    // =========================================================
    // TABLE GRID
    // =========================================================

    private fun drawTableGrid(
        canvas: android.graphics.Canvas,
        top: Float,
        bottom: Float,
        paint: Paint
    ) {

        val positions =
            floatArrayOf(

                TABLE_START_X,

                TABLE_START_X +
                        SERIAL_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        POWER_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        POWER_WIDTH +
                        BATCH_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        POWER_WIDTH +
                        BATCH_WIDTH +
                        EXPIRY_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        POWER_WIDTH +
                        BATCH_WIDTH +
                        EXPIRY_WIDTH +
                        SUPPLIER_WIDTH,

                TABLE_START_X +
                        SERIAL_WIDTH +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        POWER_WIDTH +
                        BATCH_WIDTH +
                        EXPIRY_WIDTH +
                        SUPPLIER_WIDTH +
                        INVOICE_WIDTH,

                PAGE_WIDTH -
                        RIGHT_MARGIN
            )


        canvas.drawLine(
            TABLE_START_X,
            top,
            PAGE_WIDTH -
                    RIGHT_MARGIN,
            top,
            paint
        )


        canvas.drawLine(
            TABLE_START_X,
            bottom,
            PAGE_WIDTH -
                    RIGHT_MARGIN,
            bottom,
            paint
        )


        positions.forEach { x ->

            canvas.drawLine(
                x,
                top,
                x,
                bottom,
                paint
            )
        }
    }


    // =========================================================
    // CELL TEXT
    // =========================================================

    private fun drawCellText(
        canvas: android.graphics.Canvas,
        text: String,
        startX: Float,
        baselineY: Float,
        paint: Paint,
        width: Float
    ) {

        val safeText =
            fitText(
                text =
                    text.ifBlank {
                        "—"
                    },
                paint = paint,
                maximumWidth =
                    width - 7f
            )


        canvas.drawText(
            safeText,
            startX + 3.5f,
            baselineY,
            paint
        )
    }


    // =========================================================
    // TEXT FITTING
    // =========================================================

    private fun fitText(
        text: String,
        paint: Paint,
        maximumWidth: Float
    ): String {

        if (
            paint.measureText(
                text
            ) <= maximumWidth
        ) {

            return text
        }


        val ellipsis =
            "..."


        val ellipsisWidth =
            paint.measureText(
                ellipsis
            )


        if (
            ellipsisWidth >=
            maximumWidth
        ) {

            return ellipsis
        }


        var end =
            text.length


        while (
            end > 0
        ) {

            val candidate =
                text.substring(
                    0,
                    end
                ) +
                        ellipsis


            if (
                paint.measureText(
                    candidate
                ) <= maximumWidth
            ) {

                return candidate
            }


            end--
        }


        return ellipsis
    }


    // =========================================================
    // EXPIRY FORMAT
    // =========================================================

    private fun formatExpiry(
        rawExpiry: String
    ): String {

        val value =
            rawExpiry.trim()


        if (
            value.isBlank()
        ) {

            return "—"
        }


        // MMYY
        if (
            value.length == 4 &&
            value.all {
                it.isDigit()
            }
        ) {

            return value.substring(
                0,
                2
            ) +
                    "-" +
                    value.substring(
                        2,
                        4
                    )
        }


        // MM/YYYY
        val slashParts =
            value.split(
                "/"
            )


        if (
            slashParts.size == 2
        ) {

            val month =
                slashParts[0]
                    .trim()

            val year =
                slashParts[1]
                    .trim()


            if (
                month.length in 1..2 &&
                year.length >= 2 &&
                month.all {
                    it.isDigit()
                } &&
                year.all {
                    it.isDigit()
                }
            ) {

                return month
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        year.takeLast(
                            2
                        )
            }
        }


        // Dash formats
        val dashParts =
            value.split(
                "-"
            )


        // MM-YYYY
        if (
            dashParts.size == 2
        ) {

            val first =
                dashParts[0]
                    .trim()

            val second =
                dashParts[1]
                    .trim()


            if (
                first.length in 1..2 &&
                second.length == 4 &&
                first.all {
                    it.isDigit()
                } &&
                second.all {
                    it.isDigit()
                }
            ) {

                return first
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        second.takeLast(
                            2
                        )
            }
        }


        // YYYY-MM / YYYY-MM-DD
        if (
            dashParts.size >= 2
        ) {

            val year =
                dashParts[0]
                    .trim()

            val month =
                dashParts[1]
                    .trim()


            if (
                year.length == 4 &&
                month.length in 1..2 &&
                year.all {
                    it.isDigit()
                } &&
                month.all {
                    it.isDigit()
                }
            ) {

                return month
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        year.takeLast(
                            2
                        )
            }
        }


        return value
    }


    // =========================================================
    // STATUS FORMAT
    // =========================================================

    private fun formatStatus(
        status: String
    ): String {

        return when (
            status
                .trim()
                .uppercase()
        ) {

            "IN_STOCK" ->
                "IN STOCK"

            "SAMPLE" ->
                "SAMPLE"

            "DEMO" ->
                "DEMO"

            "APPROVAL" ->
                "APPROVAL"

            "SOLD" ->
                "SOLD"

            "RETURNED" ->
                "RETURNED"

            else ->
                status
                    .trim()
                    .uppercase()
                    .replace(
                        "_",
                        " "
                    )
        }
    }


    // =========================================================
    // CURRENT DATE / TIME
    // =========================================================

    private fun currentDisplayDateTime():
            String {

        return SimpleDateFormat(
            "dd-MM-yyyy hh:mm a",
            Locale.US
        ).format(
            Date()
        )
    }
}