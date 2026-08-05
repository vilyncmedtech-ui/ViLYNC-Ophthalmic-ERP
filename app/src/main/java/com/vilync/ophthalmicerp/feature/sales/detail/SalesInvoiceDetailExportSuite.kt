package com.vilync.ophthalmicerp.feature.sales.detail

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale

object SalesInvoiceDetailExportSuite {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 24f
    private const val CONTENT_W = PAGE_W - (MARGIN * 2f)

    private val NAVY = Color.rgb(7, 27, 51)
    private val GOLD = Color.rgb(212, 175, 55)
    private val LIGHT = Color.rgb(247, 249, 252)
    private val BORDER = Color.rgb(215, 222, 232)
    private val MUTED = Color.rgb(90, 101, 115)

    private fun getExportFile(context: Context, fileName: String, extension: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "$fileName.$extension")
    }

    fun exportPdfAndShare(
        context: Context,
        sale: SaleEntity,
        lines: List<SalesInvoiceDetailLine>,
        companyProfile: CompanyProfileEntity?
    ): Result<Unit> = runCatching {
        val file = getExportFile(
            context,
            safeName("Sales_Invoice_${sale.invoiceNumber}"),
            "pdf"
        )
        writePdf(file, sale, lines, companyProfile)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Sales Invoice")
    }

    fun exportExcelAndShare(
        context: Context,
        sale: SaleEntity,
        lines: List<SalesInvoiceDetailLine>
    ): Result<Unit> = runCatching {
        val file = getExportFile(
            context,
            safeName("Sales_Invoice_${sale.invoiceNumber}"),
            "xls"
        )

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { writer ->
            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.write("""<?mso-application progid="Excel.Sheet"?>""")
            writer.write(
                """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
            )

            fun openSheet(name: String) {
                writer.write("<Worksheet ss:Name=\"${xml(name)}\"><Table>")
            }

            fun closeSheet() {
                writer.write("</Table></Worksheet>")
            }

            fun row(vararg values: String) {
                writer.write("<Row>")
                values.forEach { value ->
                    writer.write("<Cell><Data ss:Type=\"String\">${xml(value)}</Data></Cell>")
                }
                writer.write("</Row>")
            }

            openSheet("Invoice")
            row("Sales Invoice", sale.invoiceNumber)
            row("Date", sale.invoiceDate)
            row("Status", sale.status)
            row("Customer / Hospital", sale.customerName)
            row("Bill To Legal Name", sale.billToLegalName)
            row("Bill To GSTIN", sale.billToGstin)
            row("Bill To Address", sale.billToAddress)
            row("Bill To State", sale.billToState)
            row("Ship To Name", sale.shipToName)
            row("Ship To GSTIN", sale.shipToGstin)
            row("Ship To Address", sale.shipToAddress)
            row("Ship To State", sale.shipToState)
            row("PO Number", sale.poNumber)
            row("PO Date", sale.poDate)
            row("Place of Supply", sale.placeOfSupplyState)
            row("GST Supply Type", sale.gstSupplyType)
            row("Sub Total", amount(sale.subTotal))
            row("Discount", amount(sale.discountAmount))
            row("Taxable Amount", amount(sale.taxableAmount))
            row("CGST", amount(sale.cgstAmount))
            row("SGST", amount(sale.sgstAmount))
            row("IGST", amount(sale.igstAmount))
            row("GST Total", amount(sale.gstAmount))
            row("Adjustment", amount(sale.adjustment))
            row("Round Off", amount(sale.roundOff))
            row("Grand Total", amount(sale.totalAmount))
            row("Remarks", sale.remarks)
            if (sale.cancellationReason.isNotBlank()) {
                row("Cancellation Reason", sale.cancellationReason)
            }
            closeSheet()

            openSheet("Items")
            row(
                "Product", "HSN", "Power", "Qty", "Rate", "Discount %",
                "Discount Amount", "Taxable", "GST %", "GST Amount", "Total",
                "Batch", "Lot", "Serial Numbers"
            )
            lines.forEach { line ->
                row(
                    line.item.productName,
                    line.item.hsnCode,
                    line.item.power,
                    line.item.quantity.toString(),
                    amount(line.item.rate),
                    amount(line.item.discountPercent),
                    amount(line.item.discountAmount),
                    amount(line.item.taxableAmount),
                    amount(line.item.gstPercent),
                    amount(line.item.gstAmount),
                    amount(line.item.totalAmount),
                    line.item.batchNumber,
                    line.item.lotNumber,
                    line.lenses.joinToString(", ") { it.serialNumber }
                )
            }
            closeSheet()

            openSheet("Serials")
            row("Product", "Serial Number", "Power", "Batch", "Expiry Date", "Inventory Unit ID")
            lines.forEach { line ->
                line.lenses.forEach { lens ->
                    row(
                        line.item.productName,
                        lens.serialNumber,
                        lens.power,
                        lens.batchNumber,
                        lens.expiryDate,
                        lens.inventoryUnitId.toString()
                    )
                }
            }
            closeSheet()
            writer.write("</Workbook>")
        }

        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Sales Invoice Excel")
    }

    fun print(
        context: Context,
        sale: SaleEntity,
        lines: List<SalesInvoiceDetailLine>,
        companyProfile: CompanyProfileEntity?
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            "Sales Invoice ${sale.invoiceNumber}",
            InvoicePrintAdapter(sale, lines, companyProfile),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    private data class PrintRow(
        val serialIndex: Int,
        val product: String,
        val hsn: String,
        val power: String,
        val serial: String,
        val qty: String,
        val rate: Double,
        val discountPercent: Double,
        val gstPercent: Double,
        val amount: Double
    )

    private fun buildRows(lines: List<SalesInvoiceDetailLine>): List<PrintRow> {
        val rows = mutableListOf<PrintRow>()
        var serialIndex = 1
        lines.forEach { line ->
            if (line.lenses.isNotEmpty()) {
                val unitAmount = if (line.item.quantity > 0) {
                    line.item.totalAmount / line.item.quantity.toDouble()
                } else {
                    line.item.totalAmount
                }
                line.lenses.forEach { lens ->
                    rows += PrintRow(
                        serialIndex = serialIndex++,
                        product = line.item.productName,
                        hsn = line.item.hsnCode,
                        power = lens.power.ifBlank { line.item.power },
                        serial = lens.serialNumber,
                        qty = "1",
                        rate = line.item.rate,
                        discountPercent = line.item.discountPercent,
                        gstPercent = line.item.gstPercent,
                        amount = unitAmount
                    )
                }
            } else {
                rows += PrintRow(
                    serialIndex = serialIndex++,
                    product = line.item.productName,
                    hsn = line.item.hsnCode,
                    power = line.item.power,
                    serial = "",
                    qty = line.item.quantity.toString(),
                    rate = line.item.rate,
                    discountPercent = line.item.discountPercent,
                    gstPercent = line.item.gstPercent,
                    amount = line.item.totalAmount
                )
            }
        }
        return rows
    }

    private fun writePdf(
        file: File,
        sale: SaleEntity,
        lines: List<SalesInvoiceDetailLine>,
        companyProfile: CompanyProfileEntity?
    ) {
        val document = PdfDocument()
        try {
            val rows = buildRows(lines)
            val firstPageCapacity = 8
            val continuationCapacity = 13
            val pages = mutableListOf<List<PrintRow>>()

            if (rows.size <= firstPageCapacity) {
                pages.add(rows)
            } else {
                pages.add(rows.take(firstPageCapacity))
                var cursor = firstPageCapacity
                while (cursor < rows.size) {
                    val end = minOf(cursor + continuationCapacity, rows.size)
                    pages.add(rows.subList(cursor, end))
                    cursor = end
                }
            }
            if (pages.isEmpty()) pages.add(emptyList())

            pages.forEachIndexed { pageIndex, pageRows ->
                val pageNumber = pageIndex + 1
                val isFirst = pageIndex == 0
                val isLast = pageIndex == pages.lastIndex
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
                )
                val canvas = page.canvas
                drawPage(
                    canvas = canvas,
                    sale = sale,
                    company = companyProfile,
                    rows = pageRows,
                    pageNumber = pageNumber,
                    pageCount = pages.size,
                    isFirst = isFirst,
                    isLast = isLast
                )
                document.finishPage(page)
            }

            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun drawPage(
        canvas: Canvas,
        sale: SaleEntity,
        company: CompanyProfileEntity?,
        rows: List<PrintRow>,
        pageNumber: Int,
        pageCount: Int,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        canvas.drawColor(Color.WHITE)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun paint(size: Float, color: Int = NAVY, bold: Boolean = false): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }

        fun rect(x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int? = null, radius: Float = 0f) {
            p.style = Paint.Style.FILL
            p.color = fill
            canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, p)
            if (stroke != null) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.8f
                p.color = stroke
                canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, p)
            }
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = BORDER, width: Float = 0.7f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = width
            p.color = color
            canvas.drawLine(x1, y1, x2, y2, p)
        }

        fun text(value: String, x: Float, y: Float, size: Float = 7.5f, color: Int = NAVY, bold: Boolean = false) {
            if (value.isNotBlank()) canvas.drawText(value, x, y, paint(size, color, bold))
        }

        fun fit(value: String, maxWidth: Float, size: Float, bold: Boolean = false): String {
            if (value.isBlank()) return ""
            val tp = paint(size, NAVY, bold)
            if (tp.measureText(value) <= maxWidth) return value
            var v = value
            while (v.length > 1 && tp.measureText("$v…") > maxWidth) v = v.dropLast(1)
            return "$v…"
        }

        fun wrapped(value: String, maxWidth: Float, size: Float, maxLines: Int): List<String> {
            if (value.isBlank()) return emptyList()
            val tp = paint(size)
            val words = value.trim().split(Regex("\\s+"))
            val out = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val test = if (current.isBlank()) word else "$current $word"
                if (tp.measureText(test) <= maxWidth) current = test
                else {
                    if (current.isNotBlank()) out += current
                    current = word
                    if (out.size == maxLines - 1) break
                }
            }
            if (current.isNotBlank() && out.size < maxLines) out += fit(current, maxWidth, size)
            return out
        }

        // Header
        rect(MARGIN, 22f, CONTENT_W, 72f, NAVY, null, 5f)
        val companyName = company?.legalName?.ifBlank { company?.tradeName.orEmpty() }.orEmpty()
            .ifBlank { "ViLYNC Medtech (OPC) Pvt. Ltd." }
        text(companyName, 38f, 48f, 15f, Color.WHITE, true)
        val address = listOfNotNull(
            company?.addressLine1?.takeIf { it.isNotBlank() },
            company?.addressLine2?.takeIf { it.isNotBlank() },
            listOfNotNull(company?.city?.takeIf { it.isNotBlank() }, company?.state?.takeIf { it.isNotBlank() }, company?.pinCode?.takeIf { it.isNotBlank() }).joinToString(" ").takeIf { it.isNotBlank() }
        ).joinToString(", ")
        text(fit(address, 330f, 6.8f), 38f, 64f, 6.8f, Color.WHITE)
        val contact = listOfNotNull(
            company?.phone?.takeIf { it.isNotBlank() },
            company?.email?.takeIf { it.isNotBlank() },
            company?.website?.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        text(fit(contact, 330f, 6.5f), 38f, 78f, 6.5f, Color.WHITE)
        text("TAX INVOICE", 454f, 49f, 13f, GOLD, true)
        text("Page $pageNumber of $pageCount", 474f, 68f, 7f, Color.WHITE)
        if (!company?.gstin.isNullOrBlank()) text("GSTIN: ${company?.gstin}", 404f, 82f, 6.5f, Color.WHITE)

        var y = 104f
        if (isFirst) {
            // Billing / shipping / invoice details
            val gap = 7f
            val boxW = (CONTENT_W - gap * 2) / 3f
            val boxH = 105f
            val xs = listOf(MARGIN, MARGIN + boxW + gap, MARGIN + (boxW + gap) * 2)
            val titles = listOf("BILL TO", "SHIP TO", "INVOICE DETAILS")
            xs.forEachIndexed { i, x ->
                rect(x, y, boxW, boxH, Color.WHITE, BORDER, 4f)
                rect(x, y, boxW, 20f, LIGHT, null, 4f)
                text(titles[i], x + 9f, y + 14f, 7.2f, NAVY, true)
            }

            var by = y + 34f
            text(fit(sale.billToLegalName.ifBlank { sale.customerName }, boxW - 18f, 7.4f, true), xs[0] + 9f, by, 7.4f, NAVY, true)
            by += 13f
            if (sale.billToGstin.isNotBlank()) { text("GSTIN: ${sale.billToGstin}", xs[0] + 9f, by, 6.6f); by += 12f }
            wrapped(sale.billToAddress, boxW - 18f, 6.5f, 3).forEach { text(it, xs[0] + 9f, by, 6.5f); by += 10f }
            if (sale.billToState.isNotBlank()) text(fit(sale.billToState, boxW - 18f, 6.5f), xs[0] + 9f, y + boxH - 10f, 6.5f)

            val shipName = sale.shipToName.ifBlank { if (sale.sameAsBillTo) sale.billToLegalName.ifBlank { sale.customerName } else "" }
            val shipGstin = sale.shipToGstin.ifBlank { if (sale.sameAsBillTo) sale.billToGstin else "" }
            val shipAddress = sale.shipToAddress.ifBlank { if (sale.sameAsBillTo) sale.billToAddress else "" }
            val shipState = sale.shipToState.ifBlank { if (sale.sameAsBillTo) sale.billToState else "" }
            var sy = y + 34f
            text(fit(shipName, boxW - 18f, 7.4f, true), xs[1] + 9f, sy, 7.4f, NAVY, true); sy += 13f
            if (shipGstin.isNotBlank()) { text("GSTIN: $shipGstin", xs[1] + 9f, sy, 6.6f); sy += 12f }
            wrapped(shipAddress, boxW - 18f, 6.5f, 3).forEach { text(it, xs[1] + 9f, sy, 6.5f); sy += 10f }
            if (shipState.isNotBlank()) text(fit(shipState, boxW - 18f, 6.5f), xs[1] + 9f, y + boxH - 10f, 6.5f)

            val dx = xs[2] + 9f
            var dy = y + 34f
            fun detail(label: String, value: String) {
                if (value.isBlank()) return
                text(label, dx, dy, 6.2f, MUTED, true)
                text(fit(value, boxW - 74f, 6.5f, true), dx + 62f, dy, 6.5f, NAVY, true)
                dy += 13f
            }
            detail("Invoice No.", sale.invoiceNumber)
            detail("Invoice Date", sale.invoiceDate)
            detail("PO No.", sale.poNumber)
            detail("PO Date", sale.poDate)
            detail("Place of Supply", sale.placeOfSupplyState)
            detail("Supply Type", sale.gstSupplyType.replace('_', ' '))
            y += boxH + 10f
        } else {
            text("Invoice ${sale.invoiceNumber} — Continued", MARGIN, y + 9f, 8f, NAVY, true)
            y += 20f
        }

        // Item table
        val tableX = MARGIN
        val widths = floatArrayOf(24f, 136f, 42f, 42f, 78f, 28f, 50f, 36f, 34f, 77f)
        val headers = arrayOf("#", "PRODUCT", "HSN", "POWER", "SERIAL NO.", "QTY", "RATE", "DISC%", "GST%", "AMOUNT")
        val headerH = 22f
        val rowH = 22f
        rect(tableX, y, CONTENT_W, headerH, NAVY, null, 3f)
        var x = tableX
        headers.forEachIndexed { i, h ->
            text(h, x + 3f, y + 14f, 5.8f, Color.WHITE, true)
            x += widths[i]
        }
        y += headerH

        rows.forEach { row ->
            rect(tableX, y, CONTENT_W, rowH, Color.WHITE, BORDER)
            x = tableX
            val values = arrayOf(
                row.serialIndex.toString(), row.product, row.hsn, row.power, row.serial, row.qty,
                amount(row.rate), amount(row.discountPercent), amount(row.gstPercent), amount(row.amount)
            )
            values.forEachIndexed { i, value ->
                text(fit(value, widths[i] - 6f, 6.1f, i == 1), x + 3f, y + 14f, 6.1f, NAVY, i == 1)
                x += widths[i]
            }
            y += rowH
        }

        if (isLast) {
            y += 8f

            // Amount in words
            rect(MARGIN, y, CONTENT_W, 25f, LIGHT, BORDER, 3f)
            text("AMOUNT IN WORDS", MARGIN + 8f, y + 10f, 5.8f, MUTED, true)
            text(fit(amountInWords(sale.totalAmount), CONTENT_W - 105f, 7f, true), MARGIN + 100f, y + 16f, 7f, NAVY, true)
            y += 32f

            // Full-width horizontal tax breakup
            val taxH = 44f
            rect(MARGIN, y, CONTENT_W, taxH, Color.WHITE, BORDER, 3f)
            text("TAX BREAKUP", MARGIN + 8f, y + 13f, 6.5f, NAVY, true)
            val taxLabels = listOf(
                "Taxable" to sale.taxableAmount,
                "CGST" to sale.cgstAmount,
                "SGST" to sale.sgstAmount,
                "IGST" to sale.igstAmount,
                "GST Total" to sale.gstAmount
            )
            val taxStart = MARGIN + 90f
            val taxW = (CONTENT_W - 98f) / taxLabels.size
            taxLabels.forEachIndexed { i, pair ->
                val tx = taxStart + i * taxW
                if (i > 0) line(tx, y + 7f, tx, y + taxH - 7f)
                text(pair.first, tx + 7f, y + 14f, 5.8f, MUTED, true)
                text("₹ ${amount(pair.second)}", tx + 7f, y + 30f, 7.2f, NAVY, true)
            }
            y += taxH + 8f

            // Financial section gets the larger area requested in final design
            val financialH = 94f
            val leftW = 325f
            val rightW = CONTENT_W - leftW - 8f
            rect(MARGIN, y, leftW, financialH, Color.WHITE, BORDER, 4f)
            rect(MARGIN + leftW + 8f, y, rightW, financialH, Color.WHITE, BORDER, 4f)
            text("FINANCIAL SUMMARY", MARGIN + 10f, y + 16f, 7f, NAVY, true)

            val fx1 = MARGIN + 10f
            val fx2 = MARGIN + 165f
            var fy = y + 34f
            fun moneyLine(label: String, value: Double, x0: Float) {
                text(label, x0, fy, 6.3f, MUTED)
                text("₹ ${amount(value)}", x0 + 82f, fy, 6.8f, NAVY, true)
            }
            moneyLine("Sub Total", sale.subTotal, fx1)
            moneyLine("Discount", sale.discountAmount, fx2)
            fy += 16f
            moneyLine("Taxable Amount", sale.taxableAmount, fx1)
            moneyLine("GST Total", sale.gstAmount, fx2)
            fy += 16f
            moneyLine("Adjustment", sale.adjustment, fx1)
            moneyLine("Round Off", sale.roundOff, fx2)
            fy += 16f
            text("GRAND TOTAL", fx1, fy, 8f, NAVY, true)
            text("₹ ${amount(sale.totalAmount)}", fx1 + 96f, fy, 9f, NAVY, true)

            val rx = MARGIN + leftW + 18f
            text("PAYMENT DETAILS", rx, y + 16f, 7f, NAVY, true)
            text("Grand Total Payable", rx, y + 34f, 6.2f, MUTED)
            text("₹ ${amount(sale.totalAmount)}", rx, y + 50f, 10f, NAVY, true)
            if (!company?.upiId.isNullOrBlank()) {
                text("UPI ID", rx, y + 68f, 5.8f, MUTED, true)
                text(fit(company?.upiId.orEmpty(), rightW - 28f, 6.5f, true), rx, y + 82f, 6.5f, NAVY, true)
            }
            y += financialH + 8f

            // Bank / scan-to-pay / signature
            val bottomH = 91f
            val bankW = 245f
            val payW = 125f
            val signW = CONTENT_W - bankW - payW - 12f
            rect(MARGIN, y, bankW, bottomH, Color.WHITE, BORDER, 4f)
            rect(MARGIN + bankW + 6f, y, payW, bottomH, LIGHT, BORDER, 4f)
            rect(MARGIN + bankW + payW + 12f, y, signW, bottomH, Color.WHITE, BORDER, 4f)

            text("BANK DETAILS", MARGIN + 9f, y + 15f, 6.8f, NAVY, true)
            var bankY = y + 31f
            fun bank(label: String, value: String) {
                if (value.isBlank()) return
                text(label, MARGIN + 9f, bankY, 5.7f, MUTED, true)
                text(fit(value, bankW - 88f, 6.1f, true), MARGIN + 78f, bankY, 6.1f, NAVY, true)
                bankY += 12f
            }
            bank("A/C Name", company?.accountHolderName.orEmpty())
            bank("Bank", company?.bankName.orEmpty())
            bank("A/C No.", company?.accountNumber.orEmpty())
            bank("IFSC", company?.ifscCode.orEmpty())
            bank("Branch", company?.bankBranch.orEmpty())

            val payX = MARGIN + bankW + 6f
            text("SCAN TO PAY", payX + 10f, y + 15f, 6.8f, NAVY, true)
            // QR area is intentionally reserved in the finalized layout. A standards-compliant
            // UPI QR requires an encoder dependency; until that dependency is added, the UPI ID
            // and exact payable amount remain printed instead of drawing a fake/unscannable QR.
            rect(payX + 10f, y + 24f, 52f, 52f, Color.WHITE, NAVY, 2f)
            text("UPI", payX + 26f, y + 54f, 8f, NAVY, true)
            text(fit(company?.upiId.orEmpty(), payW - 78f, 5.3f), payX + 68f, y + 40f, 5.3f, NAVY, true)
            text("₹ ${amount(sale.totalAmount)}", payX + 68f, y + 56f, 6.2f, NAVY, true)

            val signX = MARGIN + bankW + payW + 12f
            val signCompany = companyName
            text(fit("For $signCompany", signW - 18f, 6.5f, true), signX + 9f, y + 17f, 6.5f, NAVY, true)
            // Clean blank signing space: no signature lines.
            val signatory = company?.authorisedSignatoryName.orEmpty()
            if (signatory.isNotBlank()) text(fit(signatory, signW - 18f, 6.1f), signX + 9f, y + 66f, 6.1f, NAVY, true)
            text("Authorised Signatory", signX + 9f, y + 82f, 6.4f, NAVY, true)
            y += bottomH + 7f

            // Two compact horizontal terms lines at the very bottom
            val terms = company?.defaultTermsAndConditions.orEmpty()
            val termLines = wrapped(terms, CONTENT_W - 18f, 5.3f, 2)
            if (termLines.isNotEmpty()) {
                line(MARGIN, y, MARGIN + CONTENT_W, y, GOLD, 0.8f)
                text("Terms & Conditions:", MARGIN, y + 10f, 5.3f, NAVY, true)
                termLines.forEachIndexed { i, t ->
                    text(t, MARGIN + 82f, y + 10f + (i * 8f), 5.3f, MUTED)
                }
            }
        }

        // Fixed page footer
        line(MARGIN, PAGE_H - 25f, MARGIN + CONTENT_W, PAGE_H - 25f, BORDER, 0.6f)
        text("Computer generated invoice", MARGIN, PAGE_H - 13f, 5.2f, MUTED)
        text("Page $pageNumber of $pageCount", PAGE_W - 78f, PAGE_H - 13f, 5.2f, MUTED, true)
    }

    private fun amountInWords(value: Double): String {
        val rupees = value.toLong()
        val paise = ((value - rupees) * 100.0 + 0.5).toInt().coerceIn(0, 99)
        val base = "Rupees ${indianNumberToWords(rupees)}"
        return if (paise > 0) "$base and ${indianNumberToWords(paise.toLong())} Paise Only" else "$base Only"
    }

    private fun indianNumberToWords(number: Long): String {
        if (number == 0L) return "Zero"
        if (number < 0L) return "Minus ${indianNumberToWords(-number)}"
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        fun underHundred(n: Int): String =
            if (n < 20) ones[n] else (tens[n / 10] + if (n % 10 != 0) " ${ones[n % 10]}" else "")

        fun underThousand(n: Int): String {
            val h = n / 100
            val r = n % 100
            return buildString {
                if (h > 0) append("${ones[h]} Hundred")
                if (r > 0) {
                    if (isNotEmpty()) append(" ")
                    append(underHundred(r))
                }
            }
        }

        var n = number
        val parts = mutableListOf<String>()
        val crore = n / 10_000_000
        if (crore > 0) { parts += "${indianNumberToWords(crore)} Crore"; n %= 10_000_000 }
        val lakh = n / 100_000
        if (lakh > 0) { parts += "${underHundred(lakh.toInt())} Lakh"; n %= 100_000 }
        val thousand = n / 1_000
        if (thousand > 0) { parts += "${underHundred(thousand.toInt())} Thousand"; n %= 1_000 }
        if (n > 0) parts += underThousand(n.toInt())
        return parts.joinToString(" ")
    }

    private fun amount(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private fun safeName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]+"), "_")

    private fun xml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private class InvoicePrintAdapter(
        private val sale: SaleEntity,
        private val lines: List<SalesInvoiceDetailLine>,
        private val companyProfile: CompanyProfileEntity?
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("Sales_Invoice_${sale.invoiceNumber}.pdf")
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
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            runCatching {
                val temp = File.createTempFile("sales_invoice_", ".pdf")
                try {
                    writePdf(temp, sale, lines, companyProfile)
                    temp.inputStream().use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    temp.delete()
                }
            }.onSuccess {
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }.onFailure {
                callback.onWriteFailed(it.message)
            }
        }
    }
}
