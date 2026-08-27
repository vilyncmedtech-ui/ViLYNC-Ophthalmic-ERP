package com.vilync.ophthalmicerp.feature.financialstatements.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vilync.ophthalmicerp.feature.financialstatements.domain.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FinancialStatementPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f

    fun exportTrialBalance(context: Context, report: TrialBalanceReport, from: String, to: String): Result<File> {
        return runCatching {
            val document = PdfDocument()
            val paint = Paint()
            var pageNumber = 1
            
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            var y = MARGIN
            
            // Header (on first page)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("ViLYNC ERP - Trial Balance", MARGIN, y, paint)
            y += 25f
            
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Period: $from to $to", MARGIN, y, paint)
            y += 35f

            // Table Header Columns
            val colAccountX = MARGIN
            val colOpeningX = MARGIN + 230f
            val colPeriodX = MARGIN + 325f
            val colClosingX = MARGIN + 420f
            val colWidth = 220f // Account column width

            fun drawTableHeader(c: Canvas, currentY: Float): Float {
                var cy = currentY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                c.drawText("Account", colAccountX, cy, paint)
                c.drawText("Opening", colOpeningX, cy, paint)
                c.drawText("Period", colPeriodX, cy, paint)
                c.drawText("Closing", colClosingX, cy, paint)
                cy += 10f
                c.drawLine(MARGIN, cy, PAGE_WIDTH - MARGIN, cy, paint)
                return cy + 15f
            }

            y = drawTableHeader(canvas, y)

            // Data Rows
            report.rows.forEach { row ->
                // Estimate row height based on account name wrapping
                paint.textSize = 9f
                if (row.isGroup) paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                else paint.typeface = Typeface.DEFAULT
                
                val indent = row.level * 12f
                val accountLines = wrapText(row.accountName, paint, colWidth - indent)
                val rowHeight = (accountLines.size * 12f).coerceAtLeast(15f)

                // Check for page break
                if (y + rowHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = MARGIN
                    y = drawTableHeader(canvas, y)
                }

                // Draw Account Name (wrapped)
                var textY = y
                accountLines.forEach { line ->
                    canvas.drawText(line, colAccountX + indent, textY, paint)
                    textY += 12f
                }
                
                // Draw Amounts (aligned to top of wrapped row)
                paint.typeface = Typeface.DEFAULT
                val op = if (row.openingDebit > 0) "Dr %.2f".format(row.openingDebit) else if (row.openingCredit > 0) "Cr %.2f".format(row.openingCredit) else "0.00"
                canvas.drawText(op, colOpeningX, y, paint)
                
                val p = "Dr %.2f / Cr %.2f".format(row.periodDebit, row.periodCredit)
                canvas.drawText(p, colPeriodX, y, paint)
                
                val cl = if (row.closingDebit > 0) "Dr %.2f".format(row.closingDebit) else if (row.closingCredit > 0) "Cr %.2f".format(row.closingCredit) else "0.00"
                canvas.drawText(cl, colClosingX, y, paint)
                
                y += rowHeight + 5f
            }

            // TOTAL Section
            val totalHeight = 40f
            if (y + totalHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            y += 10f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
            y += 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("TOTAL", MARGIN, y, paint)

            // Stacking Dr/Cr for totals to match screen more closely and ensure fit
            paint.textSize = 9f
            canvas.drawText("Dr %.0f".format(report.totalOpeningDebit), colOpeningX, y, paint)
            canvas.drawText("Cr %.0f".format(report.totalOpeningCredit), colOpeningX, y + 12f, paint)

            canvas.drawText("Dr %.0f".format(report.totalPeriodDebit), colPeriodX, y, paint)
            canvas.drawText("Cr %.0f".format(report.totalPeriodCredit), colPeriodX, y + 12f, paint)

            canvas.drawText("Dr %.0f".format(report.totalClosingDebit), colClosingX, y, paint)
            canvas.drawText("Cr %.0f".format(report.totalClosingCredit), colClosingX, y + 12f, paint)

            document.finishPage(page)
            
            val file = File(context.cacheDir, "exports/Trial_Balance_${System.currentTimeMillis()}.pdf")
            file.parentFile?.mkdirs()
            file.outputStream().use { document.writeTo(it) }
            document.close()
            file
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) "" else " ").append(word)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    // Similar implementations for P&L and Balance Sheet...
    fun exportProfitLoss(context: Context, report: ProfitLossReport, from: String, to: String): Result<File> {
        return runCatching {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            var y = MARGIN
            
            // Header
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("ViLYNC ERP - Profit & Loss", MARGIN, y, paint)
            y += 25f
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Period: $from to $to", MARGIN, y, paint)
            y += 40f

            // Income
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("INCOME", MARGIN, y, paint)
            y += 20f
            paint.typeface = Typeface.DEFAULT
            report.incomeSection.items.forEach {
                canvas.drawText(it.label, MARGIN + 10, y, paint)
                canvas.drawText("%.2f".format(it.amount), PAGE_WIDTH - MARGIN - 100, y, paint)
                y += 15f
            }
            y += 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GROSS PROFIT", MARGIN, y, paint)
            canvas.drawText("%.2f".format(report.grossProfit), PAGE_WIDTH - MARGIN - 100, y, paint)
            y += 30f
            
            canvas.drawText("NET PROFIT", MARGIN, y, paint)
            canvas.drawText("%.2f".format(report.netProfit), PAGE_WIDTH - MARGIN - 100, y, paint)

            document.finishPage(page)
            val file = File(context.cacheDir, "exports/Profit_Loss_${System.currentTimeMillis()}.pdf")
            file.parentFile?.mkdirs()
            file.outputStream().use { document.writeTo(it) }
            document.close()
            file
        }
    }

    fun exportBalanceSheet(context: Context, report: BalanceSheetReport, asOn: String): Result<File> {
        return runCatching {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            var y = MARGIN
            
            // Header
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("ViLYNC ERP - Balance Sheet", MARGIN, y, paint)
            y += 25f
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("As on: $asOn", MARGIN, y, paint)
            y += 40f

            // Liabilities
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("LIABILITIES & EQUITY", MARGIN, y, paint)
            y += 20f
            paint.typeface = Typeface.DEFAULT
            report.liabilitiesSection.items.forEach {
                canvas.drawText(it.label, MARGIN + 10, y, paint)
                canvas.drawText("%.2f".format(it.amount), PAGE_WIDTH - MARGIN - 100, y, paint)
                y += 15f
            }
            y += 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL LIABILITIES", MARGIN, y, paint)
            canvas.drawText("%.2f".format(report.totalLiabilitiesAndEquity), PAGE_WIDTH - MARGIN - 100, y, paint)
            y += 35f

            // Assets
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ASSETS", MARGIN, y, paint)
            y += 20f
            paint.typeface = Typeface.DEFAULT
            report.assetsSection.items.forEach {
                canvas.drawText(it.label, MARGIN + 10, y, paint)
                canvas.drawText("%.2f".format(it.amount), PAGE_WIDTH - MARGIN - 100, y, paint)
                y += 15f
            }
            y += 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL ASSETS", MARGIN, y, paint)
            canvas.drawText("%.2f".format(report.totalAssets), PAGE_WIDTH - MARGIN - 100, y, paint)

            document.finishPage(page)
            val file = File(context.cacheDir, "exports/Balance_Sheet_${System.currentTimeMillis()}.pdf")
            file.parentFile?.mkdirs()
            file.outputStream().use { document.writeTo(it) }
            document.close()
            file
        }
    }
}
