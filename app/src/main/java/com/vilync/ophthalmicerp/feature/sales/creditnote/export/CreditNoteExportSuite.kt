package com.vilync.ophthalmicerp.feature.sales.creditnote.export

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
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale

object CreditNoteExportSuite {

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

    private fun getExportFile(context: Context, fileName: String, extension: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "$fileName.$extension")
    }

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
            letterSpacing = 0.00f
            isSubpixelText = true
            isLinearText = true
            isFakeBoldText = false
        }

    private fun headerPaint(size: Float, color: Int = NAVY, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

    fun print(
        context: Context,
        creditNote: SalesCreditNoteEntity,
        items: List<SalesCreditNoteItemEntity>,
        lenses: List<SalesCreditNoteLensEntity>,
        companyProfile: CompanyProfileEntity?
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            "Credit Note ${creditNote.creditNoteNumber}",
            CreditNotePrintAdapter(context, creditNote, items, lenses, companyProfile),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    fun sharePdf(
        context: Context,
        creditNote: SalesCreditNoteEntity,
        items: List<SalesCreditNoteItemEntity>,
        lenses: List<SalesCreditNoteLensEntity>,
        companyProfile: CompanyProfileEntity?
    ) {
        val file = getExportFile(context, "CreditNote_${creditNote.creditNoteNumber.replace("/", "_")}", "pdf")
        writePdf(context, file, creditNote, items, lenses, companyProfile)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Credit Note")
    }

    private fun writePdf(
        context: Context,
        file: File,
        creditNote: SalesCreditNoteEntity,
        items: List<SalesCreditNoteItemEntity>,
        lenses: List<SalesCreditNoteLensEntity>,
        companyProfile: CompanyProfileEntity?
    ) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            renderFullPage(context, page.canvas, creditNote, companyProfile, items, lenses)
            document.finishPage(page)
            FileOutputStream(file).use { document.writeTo(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }

    private data class PrintRow(
        val serialIndex: Int,
        val productName: String,
        val hsn: String,
        val power: String,
        val quantity: Int,
        val rate: Double,
        val discountPercent: Double,
        val gstPercent: Double,
        val totalAmount: Double,
        val groupLenses: List<SalesCreditNoteLensEntity>
    )

    private fun buildRows(items: List<SalesCreditNoteItemEntity>, lenses: List<SalesCreditNoteLensEntity>): List<PrintRow> {
        // ALIGNED GROUPING KEY: ProductName + Power + Rate + Discount% + GST%
        // Note: CN Items don't have HSN in the DB entity yet, so we group by existing commercial keys.
        val grouped = items.groupBy { "${it.productName}|${it.power}|${it.rate}|${it.discountPercent}|${it.gstPercent}" }
        val rows = mutableListOf<PrintRow>()
        var serialIndex = 1
        grouped.values.forEach { sameTypeItems ->
            val first = sameTypeItems.first()
            val groupLenses = lenses.filter { lens -> 
                sameTypeItems.any { it.id == lens.creditNoteItemId }
            }
            val totalQty = sameTypeItems.sumOf { it.quantity }
            val totalAmount = sameTypeItems.sumOf { it.totalAmount }
            rows += PrintRow(
                serialIndex = serialIndex++,
                productName = first.productName,
                hsn = "", // Placeholder since it's not in the entity
                power = first.power,
                quantity = totalQty,
                rate = first.rate,
                discountPercent = first.discountPercent,
                gstPercent = first.gstPercent,
                totalAmount = totalAmount,
                groupLenses = groupLenses
            )
        }
        return rows
    }

    private fun formatSerial(sn: String): String {
        val trimmed = sn.trim()
        return if (trimmed.isEmpty()) "" 
               else if (trimmed.startsWith("LMDE", ignoreCase = true)) trimmed 
               else "LMDE$trimmed"
    }

    private fun formatExpiry(v: String): String {
        val trimmed = v.trim()
        return if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
            "${trimmed.substring(0, 2)}/${trimmed.substring(2, 4)}"
        } else trimmed
    }

    private fun calculateLines(lenses: List<SalesCreditNoteLensEntity>, widthLimit: Float): Int {
        if (lenses.isEmpty()) return 0
        val tp = bodyPaint(7f)
        var lineCount = 1
        val prefixW = tp.measureText("SN: ")
        var currentLineW = prefixW
        lenses.forEachIndexed { index, lens ->
            val snStr = formatSerial(lens.serialNumber)
            val expStr = formatExpiry(lens.expiryDate)
            val isLastLens = index == lenses.size - 1
            val commaStr = if (isLastLens) "" else ", "
            val pairText = "$snStr $expStr$commaStr"
            val w = tp.measureText(pairText)
            if (currentLineW + w > widthLimit && currentLineW > prefixW) { 
                lineCount++
                currentLineW = w 
            } else {
                currentLineW += w
            }
        }
        return lineCount
    }

    private fun renderFullPage(
        context: Context,
        canvas: Canvas,
        cn: SalesCreditNoteEntity,
        company: CompanyProfileEntity?,
        items: List<SalesCreditNoteItemEntity>,
        lenses: List<SalesCreditNoteLensEntity>
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
            var res = v; while (res.length > 1 && tp.measureText("$res…") > mw) res = res.dropLast(1)
            return "$res…"
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
        val logoBitmap = if (company?.logoUri != null && company.logoUri.isNotBlank()) resolveBitmap(context, company.logoUri) else null
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
        
        canvas.drawText("CREDIT NOTE", 440f, 43f, headerPaint(15f, ORANGE, true))
        canvas.drawText("Page 1 of 1", 440f, 55f, headerPaint(8f, NAVY))
        if (!company?.gstin.isNullOrBlank()) canvas.drawText("GSTIN: ${company?.gstin}", 440f, 67f, headerPaint(8f, NAVY))
        if (!company?.drugLicenceNo1.isNullOrBlank()) canvas.drawText("DL No.: ${company?.drugLicenceNo1}", 440f, 79f, headerPaint(8f, NAVY))

        var y = 104f
        // 2. Document Details
        val gap = 7f; val boxW = (CONTENT_W - gap * 2) / 3f; val boxH = 110f; val xs = listOf(MARGIN, MARGIN + boxW + gap, MARGIN + (boxW + gap) * 2); val titles = listOf("CREDIT TO", "", "DOCUMENT DETAILS")
        xs.forEachIndexed { i, x -> if (i != 1) rect(x, y, boxW, boxH, LIGHT, BORDER, 4f); if (i != 1) bodyText(titles[i], x + 9f, y + 15f, 9.5f, NAVY, "BOLD") }
        
        var by = y + 36f; bodyText(fit(cn.billToLegalName.ifBlank { cn.customerName }, boxW - 18f, 9.5f, "BOLD"), xs[0] + 9f, by, 9.5f, NAVY, "BOLD"); by += 15f
        if (cn.billToGstin.isNotBlank()) { bodyText("GSTIN: ${cn.billToGstin}", xs[0] + 9f, by, 8.5f, NAVY, "MEDIUM"); by += 14f }
        wrapped(cn.billToAddress, boxW - 18f, 8.5f, "REGULAR", 3).forEach { bodyText(it, xs[0] + 9f, by, 8.5f); by += 12f }
        if (cn.billToState.isNotBlank()) bodyText(fit(cn.billToState, boxW - 18f, 8.5f), xs[0] + 9f, y + boxH - 12f, 8.5f)
        
        val dx = xs[2] + 9f; var dy = y + 36f
        fun dtl(l: String, v: String) { bodyText(l, dx, dy, 8f, MUTED, "BOLD"); bodyText(fit(v, boxW - 80f, 9f, "BOLD"), dx + 65f, dy, 9f, NAVY, "BOLD"); dy += 15f }
        dtl("CN No.", cn.creditNoteNumber); dtl("CN Date", cn.creditNoteDate); dtl("Ref Inv No.", cn.originalInvoiceNumber); dtl("Ref Inv Date", cn.originalInvoiceDate); dtl("Supply Type", cn.gstSupplyType.replace('_', ' '))
        
        y += boxH + 8f

        // 3. Product Table
        val tableX = MARGIN; val widths = floatArrayOf(24f, 136f, 42f, 50f, 42f, 60f, 73f, 55f, 65f)
        val headers = arrayOf("#", "PRODUCT", "HSN", "POWER", "QTY", "RATE", "DISC%", "GST%", "AMOUNT")
        val aligns = arrayOf(Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)
        rect(tableX, y, CONTENT_W, 22f, Color.rgb(234, 243, 255), null, 3f)
        var tx0 = tableX
        headers.forEachIndexed { i, h ->
            val cx = if (aligns[i] == Paint.Align.CENTER) tx0 + widths[i] / 2f else if (aligns[i] == Paint.Align.RIGHT) tx0 + widths[i] - 3f else tx0 + 3f
            bodyText(h, cx, y + 14f, 8f, NAVY, "BOLD", aligns[i])
            tx0 += widths[i]
        }
        y += 22f

        val rows = buildRows(items, lenses)
        rows.forEachIndexed { idx, row ->
            val detailAreaW = widths[1] + widths[2] + widths[3] + widths[4] 
            val wrappedLines = calculateLines(row.groupLenses, detailAreaW)
            val rowH = 21f + (if (row.groupLenses.isNotEmpty()) wrappedLines * 12f else 0f)
            
            rect(tableX, y, CONTENT_W, rowH, Color.WHITE, BORDER)
            var rx0 = tableX
            val values = arrayOf((idx + 1).toString(), row.productName, row.hsn, row.power, row.quantity.toString(), amount(row.rate), amount(row.discountPercent), amount(row.gstPercent), amount(row.totalAmount))
            values.forEachIndexed { i, v ->
                val cx = if (aligns[i] == Paint.Align.CENTER) rx0 + widths[i] / 2f else if (aligns[i] == Paint.Align.RIGHT) rx0 + widths[i] - 3f else rx0 + 3f
                bodyText(fit(v, widths[i] - 6f, 8.5f, if(i==1) "BOLD" else "REGULAR"), cx, y + 14f, 8.5f, NAVY, if(i==1) "BOLD" else "REGULAR", aligns[i])
                rx0 += widths[i]
            }
            
            if (row.groupLenses.isNotEmpty()) {
                var ly = y + 26f; val tp7 = bodyPaint(7f); val dX = MARGIN + widths[0] + 3f
                bodyText("SN: ", dX, ly, 7f, NAVY, "REGULAR")
                var cW = tp7.measureText("SN: ")
                row.groupLenses.forEachIndexed { i, l ->
                    val sStr = formatSerial(l.serialNumber); val eStr = formatExpiry(l.expiryDate)
                    val isLastL = i == row.groupLenses.size - 1
                    val cStr = if (isLastL) "" else ", "; val pStr = "$sStr $eStr$cStr"; val w = tp7.measureText(pStr)
                    if (cW + w > detailAreaW && cW > tp7.measureText("SN: ")) { ly += 12f; cW = 0f }
                    val sW = tp7.measureText(sStr); rect(dX + cW - 1f, ly - 7f, sW + 2f, 9f, Color.rgb(244, 249, 255), null, 1.5f)
                    bodyText(sStr, dX + cW, ly, 7f, NAVY, "REGULAR"); bodyText(" $eStr$cStr", dX + cW + sW, ly, 7f, NAVY, "REGULAR")
                    cW += w
                }
            }
            y += rowH
        }

        // 4. Summary and Tax
        y += 10f
        val leftW = 214f; val rightW = CONTENT_W - leftW - 8f; val finH = 78f
        rect(MARGIN, y, leftW, finH, Color.WHITE, BORDER, 4f)
        bodyText("TAX BREAKUP", MARGIN + 10f, y + 15f, 9.5f, NAVY, "BOLD")
        val isInter = cn.gstSupplyType == "INTER_STATE"
        val tL = if (isInter) listOf("Taxable Amt", "IGST", "GST Total") else listOf("Taxable Amt", "CGST", "SGST", "GST Total")
        val tV = if (isInter) listOf(cn.taxableAmount, cn.igstAmount, cn.gstAmount) else listOf(cn.taxableAmount, cn.cgstAmount, cn.sgstAmount, cn.gstAmount)
        val useW = leftW - 10f; var fS = 9f
        fun getW(f: Float) = tL.zip(tV).map { (l, v) -> val lw = bodyPaint(8f, weight = "BOLD").measureText(l); val vw = bodyPaint(f, weight = "BOLD").measureText("₹ ${amount(v)}"); maxOf(lw, vw) + 6f }.toFloatArray()
        var rWs = getW(fS); while (rWs.sum() > useW && fS > 6.5f) { fS -= 0.5f; rWs = getW(fS) }
        val scale = useW / rWs.sum(); val fWs = rWs.map { it * scale }.toFloatArray()
        canvas.drawLine(MARGIN, y + 42f, MARGIN + leftW, y + 42f, Paint().apply { color = BORDER; strokeWidth = 0.5f })
        var cX = MARGIN + 5f
        tL.forEachIndexed { i, l ->
            val cw = fWs[i]; val cx = cX + cw / 2f
            bodyText(l, cx, y + 36f, 8f, MUTED, "BOLD", Paint.Align.CENTER)
            bodyText("₹ ${amount(tV[i])}", cx, y + 58f, fS, NAVY, "BOLD", Paint.Align.CENTER)
            if (i > 0) canvas.drawLine(cX, y + 25f, cX, y + finH, Paint().apply { color = BORDER; strokeWidth = 0.5f })
            cX += cw
        }

        rect(MARGIN + leftW + 8f, y, rightW, finH, Color.WHITE, BORDER, 4f)
        bodyText("FINANCIAL SUMMARY", MARGIN + leftW + 18f, y + 15f, 9.5f, NAVY, "BOLD")
        val colM = MARGIN + leftW + (rightW / 2f) + 4f; val fX1 = MARGIN + leftW + 18f
        canvas.drawLine(colM, y + 25f, colM, y + 64f, Paint().apply { color = BORDER; strokeWidth = 0.5f })
        fun sumR(l1: String, v1: String, l2: String, v2: String, y0: Float) {
            bodyText(l1, fX1, y0, 8.5f, MUTED, "MEDIUM"); bodyText(v1, colM - 8f, y0, 9f, NAVY, "BOLD", Paint.Align.RIGHT)
            bodyText(l2, colM + 10f, y0, 8.5f, MUTED, "MEDIUM"); bodyText(v2, MARGIN + CONTENT_W - 10f, y0, 9f, NAVY, "BOLD", Paint.Align.RIGHT)
            canvas.drawLine(MARGIN + leftW + 12f, y0 + 4f, MARGIN + CONTENT_W - 8f, y0 + 4f, Paint().apply { color = BORDER; strokeWidth = 0.5f })
        }
        sumR("Total Item", rows.size.toString(), "Discount", "₹ ${amount(cn.discountAmount)}", y + 32f)
        sumR("Total Qty", rows.sumOf { it.quantity }.toString(), "Round Off", "₹ ${amount(cn.roundOff)}", y + 44f)
        sumR("Sub Total", "₹ ${amount(cn.subTotal)}", "GST Total", "₹ ${amount(cn.gstAmount)}", y + 56f)
        rect(MARGIN + leftW + 8.1f, y + 64f, rightW - 0.2f, 13.8f, Color.rgb(234, 243, 255), null, 0f)
        bodyText("GRAND TOTAL", MARGIN + leftW + 18f, y + 74f, 10f, NAVY, "BOLD")
        bodyText("₹ ${amount(cn.totalAmount)}", MARGIN + CONTENT_W - 10f, y + 74f, 11f, NAVY, "BOLD", Paint.Align.RIGHT)
        y += finH + 12f

        // 5. Bottom Amount Sections
        val bottomSectionH = 34f
        val wordsW = CONTENT_W - 140f
        rect(MARGIN, y, wordsW, bottomSectionH, LIGHT, BORDER, 3f)
        bodyText("CREDIT AMOUNT", MARGIN + 10f, y + 12f, 8f, MUTED, "BOLD")
        bodyText(fit(amountInWords(cn.totalAmount), wordsW - 20f, 9.5f, "BOLD"), MARGIN + 10f, y + 25f, 9.5f, NAVY, "BOLD")
        
        val amtBoxW = CONTENT_W - wordsW - 8f
        val amtBoxCenterX = MARGIN + wordsW + 8f + amtBoxW / 2f
        rect(MARGIN + wordsW + 8f, y, amtBoxW, bottomSectionH, Color.WHITE, NAVY, 3f)
        bodyText("CREDIT AMOUNT", amtBoxCenterX, y + 12f, 7f, MUTED, "BOLD", Paint.Align.CENTER)
        bodyText("₹ ${amount(cn.totalAmount)}", amtBoxCenterX, y + 26f, 12f, NAVY, "BOLD", Paint.Align.CENTER)
        
        y += bottomSectionH + 12f

        // 6. Signatory
        val sigX = MARGIN + CONTENT_W - 190f
        val compLegal = company?.legalName ?: "ViLync Medtech (OPC) Private Limited"
        val signLines = wrapped("For $compLegal", 180f, 9.5f, "BOLD", 2)
        signLines.forEachIndexed { i, line -> bodyText(line, sigX + 9f, y + 18f + (i * 10f), 9.5f, NAVY, "BOLD") }
        val signatory = company?.authorisedSignatoryName.orEmpty()
        if (signatory.isNotBlank()) bodyText(fit(signatory, 180f, 9.5f, "BOLD"), sigX + 9f, y + 68f, 9.5f, NAVY, "BOLD")
        bodyText("Authorised Signatory", sigX + 9f, y + 84f, 9.5f, NAVY, "BOLD")

        canvas.drawLine(MARGIN, PAGE_H - 25f, MARGIN + CONTENT_W, PAGE_H - 25f, Paint().apply { color = BORDER; strokeWidth = 0.6f })
        bodyText("Computer generated document", MARGIN, PAGE_H - 13f, 7.5f, MUTED, "REGULAR")
        bodyText("Page 1 of 1", PAGE_W - 90f, PAGE_H - 13f, 7.5f, MUTED, "BOLD", Paint.Align.RIGHT)
    }

    private fun resolveBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }
    }

    private fun amount(value: Double): String = String.format(Locale.US, "%,.2f", value)

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

    fun exportExcelAndShare(
        context: Context,
        creditNote: SalesCreditNoteEntity,
        items: List<SalesCreditNoteItemEntity>,
        lenses: List<SalesCreditNoteLensEntity>
    ): Result<Unit> = runCatching {
        val file = getExportFile(context, "CreditNote_${creditNote.creditNoteNumber.replace("/", "_")}", "xls")
        FileOutputStream(file).use { fos ->
            val w = OutputStreamWriter(fos, Charsets.UTF_8)
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="CreditNote"><Table>""")
            fun row(vararg values: String) { w.write("<Row>"); values.forEach { v -> w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>") }; w.write("</Row>") }
            row("Sales Credit Note", creditNote.creditNoteNumber); row("Date", creditNote.creditNoteDate); row("Customer", creditNote.customerName); row("Original Invoice", creditNote.originalInvoiceNumber); row("Status", creditNote.status)
            row(); row("Product", "Serial", "Power", "Qty", "Rate", "GST %", "Total")
            items.forEach { item -> val itL = lenses.filter { it.creditNoteItemId == item.id }; row(item.productName, itL.joinToString(", ") { it.serialNumber }, item.power, item.quantity.toString(), "%.2f".format(item.rate), "${item.gstPercent}%", "%.2f".format(item.totalAmount)) }
            row(); row("Taxable Amount", "%.2f".format(creditNote.taxableAmount)); row("GST Amount", "%.2f".format(creditNote.gstAmount)); row("Grand Total", "%.2f".format(creditNote.totalAmount)); row(); row("Remarks", creditNote.remarks)
            w.write("</Table></Worksheet></Workbook>"); w.flush()
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Credit Note Excel")
    }

    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private class CreditNotePrintAdapter(
        private val context: Context,
        private val cn: SalesCreditNoteEntity,
        private val items: List<SalesCreditNoteItemEntity>,
        private val lenses: List<SalesCreditNoteLensEntity>,
        private val company: CompanyProfileEntity?
    ) : PrintDocumentAdapter() {
        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, signal: CancellationSignal, callback: LayoutResultCallback, extras: android.os.Bundle?) {
            if (signal.isCanceled) { callback.onLayoutCancelled(); return }
            callback.onLayoutFinished(PrintDocumentInfo.Builder("CreditNote_${cn.creditNoteNumber}.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }
        override fun onWrite(pages: Array<out android.print.PageRange>, destination: ParcelFileDescriptor, signal: CancellationSignal, callback: WriteResultCallback) {
            if (signal.isCanceled) { callback.onWriteCancelled(); return }
            runCatching {
                val temp = File.createTempFile("credit_note_", ".pdf")
                try {
                    writePdf(context, temp, cn, items, lenses, company)
                    temp.inputStream().use { input -> FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) } }
                } finally { temp.delete() }
            }.onSuccess { callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES)) }.onFailure { callback.onWriteFailed(it.message) }
        }
    }
}
