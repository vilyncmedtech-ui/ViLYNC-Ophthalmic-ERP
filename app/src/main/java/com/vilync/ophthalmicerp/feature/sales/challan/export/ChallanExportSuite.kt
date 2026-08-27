package com.vilync.ophthalmicerp.feature.sales.challan.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.sales.challan.presentation.ChallanGroupedRow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.EnumMap
import java.util.Locale
import kotlin.math.round

object ChallanExportSuite {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 24f
    private const val CONTENT_W = PAGE_W - (MARGIN * 2f)

    private val NAVY = Color.rgb(7, 27, 51)
    private val ORANGE = Color.rgb(255, 128, 0)
    private val LIGHT = Color.rgb(247, 249, 252)
    private val BORDER = Color.rgb(215, 222, 232)
    private val MUTED = Color.rgb(90, 101, 115)

    private val BODY_REGULAR = Typeface.create("sans-serif", Typeface.NORMAL)
    private val BODY_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val BODY_BOLD = Typeface.create("sans-serif", Typeface.BOLD)

    private fun bodyPaint(size: Float, color: Int = NAVY, weight: String = "REGULAR"): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = when (weight) {
                "MEDIUM" -> BODY_MEDIUM
                "BOLD" -> BODY_BOLD
                else -> BODY_REGULAR
            }
            textScaleX = 1.0f
        }

    private fun headerPaint(size: Float, color: Int = NAVY, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

    fun print(
        context: Context,
        challan: ChallanEntity,
        lines: List<ChallanGroupedRow>,
        companyProfile: CompanyProfileEntity?,
        customer: PartyMaster?
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            "Delivery Challan ${challan.challanNumber}",
            ChallanPrintAdapter(context, challan, lines, companyProfile, customer),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    fun sharePdf(
        context: Context,
        challan: ChallanEntity,
        lines: List<ChallanGroupedRow>,
        companyProfile: CompanyProfileEntity?,
        customer: PartyMaster?
    ) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "Challan_${challan.challanNumber.replace("/", "_")}.pdf")
        writePdf(context, file, challan, lines, companyProfile, customer)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Delivery Challan")
    }

    private fun writePdf(
        context: Context,
        file: File,
        challan: ChallanEntity,
        lines: List<ChallanGroupedRow>,
        companyProfile: CompanyProfileEntity?,
        customer: PartyMaster?
    ) {
        val document = PdfDocument()
        try {
            val rows = buildPrintRows(lines)
            val pageRowGroups = mutableListOf<List<PrintRow>>()
            var cursor = 0
            val headerH = 104f; val custInfoH = 110f; val tableHeaderH = 22f; val summaryBlockH = 280f
            val footerLimit = PAGE_H - 35f
            val detailAreaW = 328f

            while (cursor < rows.size) {
                val isFirst = pageRowGroups.isEmpty()
                val currentY = if (isFirst) headerH + custInfoH + tableHeaderH else headerH + tableHeaderH
                val pageRows = mutableListOf<PrintRow>()
                var totalRowH = 0f
                while (cursor < rows.size) {
                    val row = rows[cursor]
                    val wrappedLines = calculateLines(row.items, detailAreaW)
                    val calculatedRowH = 21f + (wrappedLines * 12f)
                    val needed = calculatedRowH + (if (cursor == rows.size - 1) summaryBlockH else 0f)
                    if (currentY + totalRowH + needed <= footerLimit) {
                        pageRows.add(row); totalRowH += calculatedRowH; cursor++
                    } else break
                }
                if (pageRows.isEmpty() && cursor < rows.size) { pageRows.add(rows[cursor]); cursor++ }
                pageRowGroups.add(pageRows)
            }
            if (pageRowGroups.isEmpty()) pageRowGroups.add(emptyList())

            val totals = calculateTotalsFromLines(lines)

            pageRowGroups.forEachIndexed { pageIndex, pageRows ->
                val pageNumber = pageIndex + 1
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
                renderFullPage(context, page.canvas, challan, totals, companyProfile, customer, pageRows, pageNumber, pageRowGroups.size, pageIndex == 0, pageIndex == pageRowGroups.size - 1)
                document.finishPage(page)
            }
            FileOutputStream(file).use { document.writeTo(it) }
        } finally { document.close() }
    }

    private fun renderFullPage(
        context: Context,
        canvas: Canvas,
        challan: ChallanEntity,
        totals: ChallanTotals,
        company: CompanyProfileEntity?,
        customer: PartyMaster?,
        rows: List<PrintRow>,
        pageNumber: Int,
        pageCount: Int,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun rect(x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int? = null, radius: Float = 0f) {
            p.style = Paint.Style.FILL; p.color = fill; canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, p)
            if (stroke != null) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.7f; p.color = stroke; canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, p) }
        }
        fun bodyText(v: String, x: Float, y: Float, s: Float, c: Int = NAVY, w: String = "REGULAR", a: Paint.Align = Paint.Align.LEFT) {
            if (v.isNotBlank()) { val tp = bodyPaint(s, c, w).apply { textAlign = a }; canvas.drawText(v, x, y, tp) }
        }
        fun fit(v: String, mw: Float, s: Float, w: String = "REGULAR"): String {
            if (v.isBlank()) return ""; val tp = bodyPaint(s, NAVY, w)
            if (tp.measureText(v) <= mw) return v
            var res = v; while (res.length > 1 && tp.measureText("$res\u2026") > mw) res = res.dropLast(1)
            return "$res\u2026"
        }
        fun wrapped(v: String, mw: Float, s: Float, weightStr: String = "REGULAR", ml: Int = 3): List<String> {
            if (v.isBlank()) return emptyList(); val tp = bodyPaint(s, weight = weightStr); val words = v.trim().split(Regex("\\s+"))
            val out = mutableListOf<String>(); var current = ""
            for (word in words) {
                val test = if (current.isBlank()) word else "$current $word"
                if (tp.measureText(test) <= mw) current = test else { if (current.isNotBlank()) out += current; current = word; if (out.size == ml - 1) break }
            }
            if (current.isNotBlank() && out.size < ml) out += fit(current, mw, s, weightStr)
            return out
        }

        // 1. Header
        val logoBitmap = if (company?.logoUri != null && company.logoUri.isNotBlank()) resolveBitmapInternal(context, company.logoUri) else null
        val logoSize = 75f; val headerTextX = if (logoBitmap != null) 38f + 80f else 38f
        if (logoBitmap != null) {
            val ratio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat(); val dw: Float; val dh: Float
            if (ratio > 1f) { dw = logoSize; dh = logoSize / ratio } else { dh = logoSize; dw = logoSize * ratio }
            canvas.drawBitmap(logoBitmap, null, RectF(38f, 25f, 38f + dw, 25f + dh), null)
            p.style = Paint.Style.STROKE; p.strokeWidth = 0.5f; p.color = BORDER; canvas.drawLine(115.5f, 25f, 115.5f, 92f, p)
        }
        val compName = company?.legalName?.ifBlank { "ViLync Medtech (OPC) Private Limited" }.orEmpty()
        canvas.drawText(compName, headerTextX, 48f, headerPaint(16f, NAVY, true))
        val line1 = listOfNotNull(company?.addressLine1?.takeIf { it.isNotBlank() }, company?.addressLine2?.takeIf { it.isNotBlank() }, company?.city?.takeIf { it.isNotBlank() }).joinToString(", ")
        val line2 = listOfNotNull(company?.state?.takeIf { it.isNotBlank() }, company?.pinCode?.takeIf { it.isNotBlank() }).joinToString(" ")
        canvas.drawText(fit(line1, 320f, 10f), headerTextX, 63f, headerPaint(10f, NAVY))
        canvas.drawText(fit(line2, 320f, 10f), headerTextX, 75f, headerPaint(10f, NAVY))
        canvas.drawText("+91 8188886788 | vilyncmedtech@gmail.com", headerTextX, 88f, headerPaint(9f, NAVY))
        
        // Metadata alignment
        val metaX = 410f
        canvas.drawText("DELIVERY CHALLAN", metaX, 43f, headerPaint(15f, ORANGE, true))
        canvas.drawText("Page $pageNumber of $pageCount", metaX, 55f, headerPaint(8f, NAVY))
        if (!company?.gstin.isNullOrBlank()) canvas.drawText("GSTIN: ${company?.gstin}", metaX, 67f, headerPaint(8f, NAVY))
        if (!company?.drugLicenceNo1.isNullOrBlank()) canvas.drawText("DL No.: ${company?.drugLicenceNo1}", metaX, 79f, headerPaint(8f, NAVY))

        var y = 104f
        if (isFirst) {
            val gap = 7f; val boxW = (CONTENT_W - gap * 2) / 3f; val boxH = 110f; val xs = listOf(MARGIN, MARGIN + boxW + gap, MARGIN + (boxW + gap) * 2); val titles = listOf("BILL TO", "SHIP TO", "CHALLAN DETAILS")
            xs.forEachIndexed { i, x -> rect(x, y, boxW, boxH, LIGHT, BORDER, 4f); bodyText(titles[i], x + 9f, y + 15f, 9.5f, NAVY, "BOLD") }
            
            // BILL TO (No GSTIN)
            var by = y + 36f; bodyText(fit(customer?.partyName ?: challan.customerName, boxW - 18f, 9.5f, "BOLD"), xs[0] + 9f, by, 9.5f, NAVY, "BOLD"); by += 15f
            val fullAddress = listOfNotNull(customer?.addressLine1, customer?.addressLine2, customer?.city, customer?.district, customer?.state, customer?.pinCode).filter { it.isNotBlank() }.joinToString(", ")
            wrapped(fullAddress, boxW - 18f, 8.5f, "REGULAR", 3).forEach { bodyText(it, xs[0] + 9f, by, 8.5f); by += 12f }
            
            // SHIP TO (No GSTIN)
            var sy = y + 36f; bodyText(fit(customer?.partyName ?: challan.customerName, boxW - 18f, 9.5f, "BOLD"), xs[1] + 9f, sy, 9.5f, NAVY, "BOLD"); sy += 15f
            wrapped(fullAddress, boxW - 18f, 8.5f, "REGULAR", 3).forEach { bodyText(it, xs[1] + 9f, sy, 8.5f); sy += 12f }

            // CHALLAN DETAILS
            val dx = xs[2] + 9f; var dy = y + 36f
            fun dtl(l: String, v: String) { bodyText(l, dx, dy, 8f, MUTED, "BOLD"); bodyText(fit(v, boxW - 80f, 9f, "BOLD"), dx + 65f, dy, 9f, NAVY, "BOLD"); dy += 15f }
            dtl("Challan No.", challan.challanNumber); dtl("Challan Date", challan.challanDate); dtl("Status", challan.status.replace('_', ' '))
            y += boxH + 8f
        } else { bodyText("Challan ${challan.challanNumber} \u2014 Continued", MARGIN, y + 10f, 10f, NAVY, "BOLD"); y += 22f }

        // 3. Product Table (Removed GST%)
        val tableX = MARGIN; val widths = floatArrayOf(24f, 186f, 52f, 50f, 42f, 90f, 35f, 68f)
        val tableHeaders = arrayOf("#", "PRODUCT", "HSN", "POWER", "QTY", "RATE", "DISC%", "AMOUNT")
        val aligns = arrayOf(Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)
        rect(tableX, y, CONTENT_W, 22f, Color.rgb(234, 243, 255), null, 3f)
        var tx0 = tableX
        tableHeaders.forEachIndexed { i, h ->
            val cx = if (aligns[i] == Paint.Align.CENTER) tx0 + widths[i] / 2f else if (aligns[i] == Paint.Align.RIGHT) tx0 + widths[i] - 3f else tx0 + 3f
            bodyText(h, cx, y + 14f, 8f, NAVY, "BOLD", aligns[i])
            tx0 += widths[i]
        }
        y += 22f

        rows.forEach { row ->
            val detailAreaW = 328f
            val wrappedLines = calculateLines(row.items, detailAreaW)
            val calculatedRowH = 21f + (wrappedLines * 12f)
            rect(tableX, y, CONTENT_W, calculatedRowH, Color.WHITE, BORDER)
            var rx0 = tableX
            
            // GST-inclusive calculations
            val inclusiveRate = row.rate * (1 + row.gstPercent / 100.0)
            val rowAmt = row.qty * inclusiveRate
            
            val values = arrayOf(row.serialIndex.toString(), row.productName, row.hsn, row.power, row.qty.toString(), amount(inclusiveRate), "0.00", amount(rowAmt))
            values.forEachIndexed { i, v ->
                val cx = if (aligns[i] == Paint.Align.CENTER) rx0 + widths[i] / 2f else if (aligns[i] == Paint.Align.RIGHT) rx0 + widths[i] - 3f else rx0 + 3f
                bodyText(fit(v, widths[i] - 6f, 8.5f, if(i==1) "BOLD" else "REGULAR"), cx, y + 14f, 8.5f, NAVY, if(i==1) "BOLD" else "REGULAR", aligns[i])
                rx0 += widths[i]
            }
            
            if (row.items.isNotEmpty()) {
                var ly = y + 26f
                val tp = bodyPaint(7f)
                val detailX = MARGIN + widths[0] + 3f
                bodyText("SN: ", detailX, ly, 7f, NAVY, "REGULAR")
                val prefixW = tp.measureText("SN: ")
                var currentLineW = prefixW

                row.items.forEachIndexed { index, item ->
                    val snStr = item.serialNumber.trim()
                    val expStr = item.expiryDate.trim()
                    val formattedExp = if (expStr.length == 4 && expStr.all { it.isDigit() }) "${expStr.substring(0, 2)}/${expStr.substring(2, 4)}" else expStr
                    val isLast = index == row.items.size - 1
                    val comma = if (isLast) "" else ", "
                    val pairStr = "$snStr $formattedExp$comma"
                    val w = tp.measureText(pairStr)
                    if (currentLineW + w > detailAreaW && currentLineW > 0f) { ly += 12f; currentLineW = 0f }
                    val snW = tp.measureText(snStr)
                    rect(detailX + currentLineW - 1f, ly - 7f, snW + 2f, 9f, Color.rgb(244, 249, 255), null, 1.5f)
                    bodyText(snStr, detailX + currentLineW, ly, 7f, NAVY, "REGULAR")
                    bodyText(" $formattedExp$comma", detailX + currentLineW + snW, ly, 7f, NAVY, "REGULAR")
                    currentLineW += w
                }
            }
            y += calculatedRowH
        }

        if (isLast) {
            y += 10f; val financialH = 78f; val leftW = 214f; val rightW = CONTENT_W - leftW - 8f
            
            // Box 1: DECLARATION & REMARKS
            rect(MARGIN, y, leftW, financialH, Color.WHITE, BORDER, 4f)
            bodyText("DECLARATION & REMARKS", MARGIN + 10f, y + 15f, 9.5f, NAVY, "BOLD")
            canvas.drawLine(MARGIN, y + 22f, MARGIN + leftW, y + 22f, p.apply { color = BORDER; style = Paint.Style.STROKE; strokeWidth = 0.5f })
            var ry0 = y + 34f
            bodyText("This is a Delivery Challan. Not a Tax Invoice.", MARGIN + 10f, ry0, 7.5f, MUTED, "MEDIUM"); ry0 += 12f
            if (challan.remarks.isNotBlank()) {
                bodyText("REMARKS:", MARGIN + 10f, ry0, 7.5f, MUTED, "BOLD"); ry0 += 10f
                wrapped(challan.remarks, leftW - 20f, 7.5f, "MEDIUM", 2).forEach { bodyText(it, MARGIN + 10f, ry0, 7.5f); ry0 += 9f }
            }

            // Box 2: FINANCIAL SUMMARY (Removed GST line, Sub Total inclusive)
            rect(MARGIN + leftW + 8f, y, rightW, financialH, Color.WHITE, BORDER, 4f)
            bodyText("FINANCIAL SUMMARY", MARGIN + leftW + 18f, y + 15f, 9.5f, NAVY, "BOLD")
            val fx1 = MARGIN + leftW + 18f; val colMid = MARGIN + leftW + (rightW / 2f) + 4f
            canvas.drawLine(colMid, y + 25f, colMid, y + 64f, p.apply { color = BORDER; style = Paint.Style.STROKE; strokeWidth = 0.5f })
            fun summaryRow(l1: String, v1: String, l2: String, v2: String, y0: Float) {
                bodyText(l1, fx1, y0, 8.5f, MUTED, "MEDIUM"); bodyText(v1, colMid - 8f, y0, 9f, NAVY, "BOLD", Paint.Align.RIGHT)
                bodyText(l2, colMid + 10f, y0, 8.5f, MUTED, "MEDIUM"); bodyText(v2, MARGIN + CONTENT_W - 10f, y0, 9f, NAVY, "BOLD", Paint.Align.RIGHT)
            }
            summaryRow("Total Item", rows.size.toString(), "Discount", "₹ 0.00", y + 32f)
            summaryRow("Total Qty", String.format(Locale.US, "%.0f", totals.totalQty), "Round Off", "₹ ${amount(totals.roundOff)}", y + 44f)
            summaryRow("Sub Total", "₹ ${amount(totals.inclusiveSubTotal)}", "", "", y + 56f)
            rect(MARGIN + leftW + 8.1f, y + 64f, rightW - 0.2f, 13.8f, Color.rgb(234, 243, 255), null, 0f)
            bodyText("GRAND TOTAL", MARGIN + leftW + 18f, y + 74f, 10f, NAVY, "BOLD")
            bodyText("₹ ${amount(totals.grandTotal)}", MARGIN + CONTENT_W - 10f, y + 74f, 11f, NAVY, "BOLD", Paint.Align.RIGHT)
            y += financialH + 4f

            // Amount in Words
            rect(MARGIN, y, CONTENT_W, 28f, LIGHT, BORDER, 3f); bodyText("AMOUNT IN WORDS", MARGIN + 10f, y + 18f, 8.5f, MUTED, "BOLD")
            bodyText(fit(amountInWords(totals.grandTotal), CONTENT_W - 130f, 9.5f, "BOLD"), MARGIN + 120f, y + 18f, 9.5f, NAVY, "BOLD")
            y += 34f

            val bottomH = 140f
            val bankW = 240f; val payW = 105f; val signW = 190f; val gap = 6f
            val bankX = MARGIN; val payX = bankX + bankW + gap; val signX = payX + payW + gap
            rect(bankX, y, bankW, bottomH, Color.WHITE, BORDER, 4f)
            rect(payX, y, payW, bottomH, LIGHT, BORDER, 4f)
            rect(signX, y, signW, bottomH, Color.WHITE, BORDER, 4f)
            
            bodyText("BANK DETAILS", bankX + 9f, y + 15f, 9.5f, NAVY, "BOLD")
            var bankY = y + 33f
            fun bank(l: String, v: String) {
                bodyText(l, bankX + 9f, bankY, 8f, MUTED, "BOLD")
                val lines = wrapped(v, bankW - 75f, 8.5f, "BOLD", 2)
                lines.forEachIndexed { i, line -> bodyText(line, bankX + 68f, bankY + (i * 9f), 8.5f, NAVY, "BOLD") }
                bankY += if (lines.size > 1) 18f else 13f
            }
            bank("A/C Name", company?.accountHolderName.orEmpty())
            bank("Bank", company?.bankName.orEmpty())
            bank("A/C No.", company?.accountNumber.orEmpty())
            bank("IFSC", company?.ifscCode.orEmpty())
            bank("Branch", company?.bankBranch.orEmpty())

            bodyText("SCAN TO PAY", payX + payW / 2f, y + 15f, 9.5f, NAVY, "BOLD", Paint.Align.CENTER)
            val upiId = company?.upiId?.trim().orEmpty()
            val companyLegal = company?.legalName?.ifBlank { "Company Name" } ?: "Company Name"
            val qrSize = 68f; val qrY = y + 22f
            if (upiId.contains("@")) {
                val uri = "upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(companyLegal)}&am=${String.format(Locale.US, "%.2f", totals.grandTotal)}&cu=INR&tn=${Uri.encode(challan.challanNumber)}"
                val qrX = payX + (payW - qrSize) / 2f
                rect(qrX - 4f, qrY - 4f, qrSize + 8f, qrSize + 8f, Color.WHITE, BORDER, 4f)
                drawQrCode(canvas, uri, qrX, qrY, qrSize)
            } else rect(payX + (payW - qrSize) / 2f, qrY, qrSize, qrSize, Color.WHITE, NAVY, 2f)
            
            val upiTextY = qrY + qrSize + 10f
            bodyText("UPI ID: $upiId", payX + payW / 2f, upiTextY, 6.5f, NAVY, "BOLD", Paint.Align.CENTER)
            p.style = Paint.Style.STROKE; p.strokeWidth = 0.5f; p.color = BORDER
            canvas.drawLine(payX + 10f, upiTextY + 6f, payX + payW - 10f, upiTextY + 6f, p)
            val payAmtLabelY = upiTextY + 18f; bodyText("Pay Amount", payX + payW / 2f, payAmtLabelY, 7.5f, MUTED, "BOLD", Paint.Align.CENTER)
            val grandTotalY = payAmtLabelY + 14f; bodyText("₹ ${amount(totals.grandTotal)}", payX + payW / 2f, grandTotalY, 10f, NAVY, "BOLD", Paint.Align.CENTER)
            canvas.drawLine(payX + 10f, grandTotalY + 6f, payX + payW - 10f, grandTotalY + 6f, p)

            val signLines = wrapped("For $companyLegal", signW - 18f, 9.5f, "BOLD", 2)
            signLines.forEachIndexed { i, line -> bodyText(line, signX + 9f, y + 18f + (i * 10f), 9.5f, NAVY, "BOLD") }
            val signatory = company?.authorisedSignatoryName.orEmpty()
            if (signatory.isNotBlank()) bodyText(fit(signatory, signW - 18f, 9.5f, "BOLD"), signX + 9f, y + bottomH - 28f, 9.5f, NAVY, "BOLD")
            bodyText("Authorised Signatory", signX + 9f, y + bottomH - 12f, 9.5f, NAVY, "BOLD")
        }
        canvas.drawLine(MARGIN, PAGE_H - 25f, MARGIN + CONTENT_W, PAGE_H - 25f, Paint().apply { color = BORDER; strokeWidth = 0.6f })
        bodyText("Computer generated Delivery Challan", MARGIN, PAGE_H - 13f, 7.5f, MUTED, "REGULAR")
        bodyText("Page $pageNumber of $pageCount", PAGE_W - 90f, PAGE_H - 13f, 7.5f, MUTED, "BOLD", Paint.Align.RIGHT)
    }

    private data class ChallanTotals(
        val totalQty: Double,
        val inclusiveSubTotal: Double,
        val grandTotal: Double,
        val roundOff: Double
    )

    private fun calculateTotalsFromLines(lines: List<ChallanGroupedRow>): ChallanTotals {
        val qty = lines.sumOf { it.items.size }.toDouble()
        val inclusiveSum = lines.sumOf { row ->
            row.items.sumOf { it.rate * (1 + it.gstPercent / 100.0) }
        }
        val rounded = round(inclusiveSum)
        return ChallanTotals(
            totalQty = qty,
            inclusiveSubTotal = inclusiveSum,
            grandTotal = rounded,
            roundOff = rounded - inclusiveSum
        )
    }

    private data class PrintRow(
        val serialIndex: Int,
        val productName: String,
        val hsn: String,
        val power: String,
        val qty: Int,
        val rate: Double,
        val gstPercent: Double,
        val items: List<ChallanItemEntity>
    )

    private fun buildPrintRows(lines: List<ChallanGroupedRow>): List<PrintRow> {
        var index = 1
        return lines.map { line ->
            val first = line.item
            PrintRow(
                serialIndex = index++,
                productName = first.productName,
                hsn = line.hsn,
                power = first.power,
                qty = line.items.size,
                rate = first.rate,
                gstPercent = first.gstPercent,
                items = line.items
            )
        }
    }

    private fun calculateLines(items: List<ChallanItemEntity>, widthLimit: Float): Int {
        if (items.isEmpty()) return 0
        val tp = bodyPaint(7f)
        var count = 1
        val prefixW = tp.measureText("SN: ")
        var currentW = prefixW
        items.forEachIndexed { index, item ->
            val snStr = item.serialNumber.trim()
            val expStr = item.expiryDate.trim()
            val formattedExp = if (expStr.length == 4 && expStr.all { it.isDigit() }) "${expStr.substring(0, 2)}/${expStr.substring(2, 4)}" else expStr
            val isLast = index == items.size - 1
            val comma = if (isLast) "" else ", "
            val pair = "$snStr $formattedExp$comma"
            val w = tp.measureText(pair)
            if (currentW + w > widthLimit && currentW > prefixW) { count++; currentW = w } else currentW += w
        }
        return count
    }

    private fun drawQrCode(canvas: Canvas, content: String, x: Float, y: Float, size: Float) {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 0
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val w = bitMatrix.width; val moduleSize = size / w
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val orangeRed = Color.rgb(255, 87, 34); val skyBlue = Color.rgb(33, 150, 243); val forestGreen = Color.rgb(76, 175, 80)
            val skipS = (w * 0.22f).toInt().coerceAtLeast(1); val skipStart = (w - skipS) / 2; val skipEnd = skipStart + skipS
            for (row in 0 until bitMatrix.height) {
                for (col in 0 until bitMatrix.width) {
                    if (bitMatrix.get(col, row)) {
                        if (row in skipStart until skipEnd && col in skipStart until skipEnd) continue
                        p.color = when {
                            row < 7 && col < 7 -> orangeRed
                            row < 7 && col >= w - 7 -> skyBlue
                            row >= bitMatrix.height - 7 && col < 7 -> forestGreen
                            else -> Color.BLACK
                        }
                        canvas.drawRect(x + col * moduleSize, y + row * moduleSize, x + (col + 1) * moduleSize, y + (row + 1) * moduleSize, p)
                    }
                }
            }
            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = NAVY; textSize = size * 0.16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText("UPI", x + size / 2f, y + size / 2f + (cp.textSize / 2.5f), cp)
        } catch (e: Exception) { canvas.drawRect(x, y, x + size, y + size, Paint().apply { color = Color.LTGRAY }) }
    }

    private fun resolveBitmapInternal(context: Context, uriString: String): Bitmap? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }
    }

    private fun amountInWords(value: Double): String {
        val rupees = value.toLong(); val paise = ((value - rupees) * 100.0 + 0.5).toInt().coerceIn(0, 99)
        val base = "Rupees ${indianNumberToWords(rupees)}"
        return if (paise > 0) "$base and ${indianNumberToWords(paise.toLong())} Paise Only" else "$base Only"
    }

    private fun indianNumberToWords(number: Long): String {
        if (number == 0L) return "Zero"
        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        fun underHundred(n: Int): String = if (n < 20) ones[n] else (tens[n / 10] + if (n % 10 != 0) " ${ones[n % 10]}" else "")
        fun underThousand(n: Int): String {
            val h = n / 100; val r = n % 100
            return buildString { if (h > 0) append("${ones[h]} Hundred"); if (r > 0) { if (isNotEmpty()) append(" "); append(underHundred(r)) } }
        }
        var n = number; val parts = mutableListOf<String>()
        val crore = n / 10_000_000; if (crore > 0) { parts += "${indianNumberToWords(crore)} Crore"; n %= 10_000_000 }
        val lakh = n / 100_000; if (lakh > 0) { parts += "${underHundred(lakh.toInt())} Lakh"; n %= 100_000 }
        val thousand = n / 1_000; if (thousand > 0) { parts += "${underHundred(thousand.toInt())} Thousand"; n %= 1_000 }
        if (n > 0) parts += underThousand(n.toInt())
        return parts.joinToString(" ")
    }

    private fun amount(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private class ChallanPrintAdapter(
        private val context: Context,
        private val challan: ChallanEntity,
        private val lines: List<ChallanGroupedRow>,
        private val companyProfile: CompanyProfileEntity?,
        private val customer: PartyMaster?
    ) : PrintDocumentAdapter() {
        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, signal: CancellationSignal, callback: LayoutResultCallback, extras: android.os.Bundle?) {
            if (signal.isCanceled) { callback.onLayoutCancelled(); return }
            callback.onLayoutFinished(PrintDocumentInfo.Builder("Challan_${challan.challanNumber}.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }
        override fun onWrite(pages: Array<out android.print.PageRange>, destination: ParcelFileDescriptor, signal: CancellationSignal, callback: WriteResultCallback) {
            if (signal.isCanceled) { callback.onWriteCancelled(); return }
            runCatching {
                val temp = File.createTempFile("challan_", ".pdf")
                try {
                    writePdf(context, temp, challan, lines, companyProfile, customer)
                    temp.inputStream().use { input -> FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) } }
                } finally { temp.delete() }
            }.onSuccess { callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES)) }.onFailure { callback.onWriteFailed(it.message) }
        }
    }

    fun exportExcelAndShare(
        context: Context,
        challan: ChallanEntity,
        items: List<ChallanItemEntity>
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "Challan_${challan.challanNumber.replace("/", "_")}.xls")

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Challan"><Table>""")
            
            fun row(vararg values: String) {
                w.write("<Row>")
                values.forEach { v ->
                    w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
                }
                w.write("</Row>")
            }

            row("Delivery Challan", challan.challanNumber)
            row("Date", challan.challanDate)
            row("Customer", challan.customerName)
            row("Status", challan.status)
            row()
            row("Product", "Serial", "Power", "Batch", "Expiry", "Settlement")
            items.forEach { itm ->
                row(itm.productName, itm.serialNumber, itm.power, itm.batchNumber, itm.expiryDate, itm.settlementStatus)
            }
            row()
            row("Remarks", challan.remarks)
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Delivery Challan Excel")
    }

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

}
