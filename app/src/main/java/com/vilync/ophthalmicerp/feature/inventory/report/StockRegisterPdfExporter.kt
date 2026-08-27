package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StockRegisterPdfExporter {

    // =========================================================
    // PAGE CONFIGURATION
    // =========================================================

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    private const val LEFT_MARGIN = 32f
    private const val RIGHT_MARGIN = 32f
    private const val TOP_MARGIN = 30f
    private const val BOTTOM_MARGIN = 30f

    private const val ROW_HEIGHT = 25f

    private const val TABLE_START_X = LEFT_MARGIN

    private const val PRODUCT_WIDTH = 170f
    private const val MODEL_WIDTH = 100f
    private const val CATEGORY_WIDTH = 80f
    private const val POWER_WIDTH = 60f
    private const val PURCHASED_WIDTH = 80f
    private const val SOLD_WIDTH = 80f
    private const val RETURNED_WIDTH = 80f
    private const val AVAILABLE_WIDTH = 80f


    // =========================================================
    // EXPORT
    // =========================================================

    fun export(
        context: Context,
        report: StockRegisterReport
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
                ).format(
                    Date()
                )


            val file =
                File(
                    exportDirectory,
                    "Stock_Register_$timestamp.pdf"
                )


            val document =
                PdfDocument()

            try {

                writeReport(
                    document = document,
                    report = report
                )

                file.outputStream().use { outputStream ->

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
                        "Stock Register"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    intent,
                    "Export Stock Register PDF"
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
        report: StockRegisterReport
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
    // REPORT WRITER
    // =========================================================

    private fun writeReport(
        document: PdfDocument,
        report: StockRegisterReport
    ) {

        var pageNumber = 1
        var rowIndex = 0


        /*
         * At least one page is generated even if the
         * Stock Register contains no rows.
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
                    report = report,
                    pageNumber = pageNumber
                )


            currentY =
                drawTableHeader(
                    canvas = canvas,
                    top = currentY
                )


            while (
                rowIndex < report.rows.size &&
                currentY + ROW_HEIGHT <
                PAGE_HEIGHT - BOTTOM_MARGIN - 35f
            ) {

                val row =
                    report.rows[rowIndex]


                drawStockRow(
                    canvas = canvas,
                    top = currentY,
                    row = row
                )


                currentY +=
                    ROW_HEIGHT


                rowIndex++
            }


            /*
             * Totals are drawn only on the final page.
             */
            if (
                rowIndex >= report.rows.size
            ) {

                currentY += 8f


                if (
                    currentY + ROW_HEIGHT <
                    PAGE_HEIGHT - BOTTOM_MARGIN
                ) {

                    drawTotals(
                        canvas = canvas,
                        top = currentY,
                        report = report
                    )
                }
            }


            drawFooter(
                canvas = canvas,
                pageNumber = pageNumber
            )


            document.finishPage(
                page
            )


            pageNumber++

        } while (
            rowIndex < report.rows.size
        )
    }


    // =========================================================
    // PAGE HEADER
    // =========================================================

    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        report: StockRegisterReport,
        pageNumber: Int
    ): Float {

        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 22f

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

                textSize = 13f

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

                textSize = 10f
            }


        var y =
            TOP_MARGIN


        canvas.drawText(
            "ViLYNC ERP",
            LEFT_MARGIN,
            y,
            titlePaint
        )


        y += 24f


        canvas.drawText(
            report.title,
            LEFT_MARGIN,
            y,
            subtitlePaint
        )


        y += 18f


        canvas.drawText(
            "Generated At: ${report.generatedAt}",
            LEFT_MARGIN,
            y,
            normalPaint
        )


        if (
            report.searchQuery.isNotBlank()
        ) {

            y += 15f

            canvas.drawText(
                "Search / Filter: ${report.searchQuery}",
                LEFT_MARGIN,
                y,
                normalPaint
            )
        }


        canvas.drawText(
            "Page $pageNumber",
            PAGE_WIDTH - RIGHT_MARGIN - 55f,
            TOP_MARGIN,
            normalPaint
        )


        return y + 18f
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

                /*
                 * Soft professional blue-grey.
                 */
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

                strokeWidth = 1f
            }


        val textPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 9f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val bottom =
            top + ROW_HEIGHT


        canvas.drawRect(
            TABLE_START_X,
            top,
            PAGE_WIDTH - RIGHT_MARGIN,
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
            top + 16f


        drawCellText(
            canvas,
            "Product",
            TABLE_START_X,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Model",
            TABLE_START_X + PRODUCT_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Category",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Power",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Purchased",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Sold",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Returned",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH +
                    SOLD_WIDTH,
            textY,
            textPaint
        )

        drawCellText(
            canvas,
            "Available",
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH +
                    SOLD_WIDTH +
                    RETURNED_WIDTH,
            textY,
            textPaint
        )


        return bottom
    }


    // =========================================================
    // STOCK ROW
    // =========================================================

    private fun drawStockRow(
        canvas: android.graphics.Canvas,
        top: Float,
        row: StockRegisterReportRow
    ) {

        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    android.graphics.Color.LTGRAY

                style =
                    Paint.Style.STROKE

                strokeWidth = 1f
            }


        val textPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 9f
            }


        val bottom =
            top + ROW_HEIGHT


        drawTableGrid(
            canvas = canvas,
            top = top,
            bottom = bottom,
            paint = borderPaint
        )


        val textY =
            top + 16f


        drawCellText(
            canvas,
            row.productName,
            TABLE_START_X,
            textY,
            textPaint,
            PRODUCT_WIDTH
        )

        drawCellText(
            canvas,
            row.model,
            TABLE_START_X + PRODUCT_WIDTH,
            textY,
            textPaint,
            MODEL_WIDTH
        )

        drawCellText(
            canvas,
            row.category,
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH,
            textY,
            textPaint,
            CATEGORY_WIDTH
        )

        drawCellText(
            canvas,
            row.power,
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH,
            textY,
            textPaint,
            POWER_WIDTH
        )

        drawCellText(
            canvas,
            row.purchasedQuantity.toString(),
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH,
            textY,
            textPaint,
            PURCHASED_WIDTH
        )

        drawCellText(
            canvas,
            row.soldQuantity.toString(),
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH,
            textY,
            textPaint,
            SOLD_WIDTH
        )

        drawCellText(
            canvas,
            row.returnedQuantity.toString(),
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH +
                    SOLD_WIDTH,
            textY,
            textPaint,
            RETURNED_WIDTH
        )

        drawCellText(
            canvas,
            row.availableQuantity.toString(),
            TABLE_START_X +
                    PRODUCT_WIDTH +
                    MODEL_WIDTH +
                    CATEGORY_WIDTH +
                    POWER_WIDTH +
                    PURCHASED_WIDTH +
                    SOLD_WIDTH +
                    RETURNED_WIDTH,
            textY,
            textPaint,
            AVAILABLE_WIDTH
        )
    }


    // =========================================================
    // TOTALS
    // =========================================================

    private fun drawTotals(
        canvas: android.graphics.Canvas,
        top: Float,
        report: StockRegisterReport
    ) {

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 10f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val summary =
            "Purc: ${report.totalPurchased}" +
                    " | Sold: ${report.totalSold}" +
                    " | Ret: ${report.totalReturned}" +
                    " | Net Stock: ${report.totalAvailable}"


        canvas.drawText(
            summary,
            LEFT_MARGIN,
            top + 16f,
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

                textSize = 8f

                color =
                    android.graphics.Color.DKGRAY
            }


        canvas.drawText(
            "Generated by ViLYNC ERP",
            LEFT_MARGIN,
            PAGE_HEIGHT - 15f,
            paint
        )


        canvas.drawText(
            "Page $pageNumber",
            PAGE_WIDTH - RIGHT_MARGIN - 45f,
            PAGE_HEIGHT - 15f,
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
                        PRODUCT_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        CATEGORY_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        CATEGORY_WIDTH +
                        POWER_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        CATEGORY_WIDTH +
                        POWER_WIDTH +
                        PURCHASED_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        CATEGORY_WIDTH +
                        POWER_WIDTH +
                        PURCHASED_WIDTH +
                        SOLD_WIDTH,

                TABLE_START_X +
                        PRODUCT_WIDTH +
                        MODEL_WIDTH +
                        CATEGORY_WIDTH +
                        POWER_WIDTH +
                        PURCHASED_WIDTH +
                        SOLD_WIDTH +
                        RETURNED_WIDTH,

                PAGE_WIDTH -
                        RIGHT_MARGIN
            )


        canvas.drawLine(
            TABLE_START_X,
            top,
            PAGE_WIDTH - RIGHT_MARGIN,
            top,
            paint
        )


        canvas.drawLine(
            TABLE_START_X,
            bottom,
            PAGE_WIDTH - RIGHT_MARGIN,
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
        width: Float = 100f
    ) {

        val safeText =
            fitText(
                text = text,
                paint = paint,
                maximumWidth = width - 10f
            )


        canvas.drawText(
            safeText,
            startX + 5f,
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
            paint.measureText(text) <= maximumWidth
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
            ellipsisWidth >= maximumWidth
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
                ) + ellipsis


            if (
                paint.measureText(candidate) <= maximumWidth
            ) {

                return candidate
            }


            end--
        }


        return ellipsis
    }
}