package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object OpeningStockExportSuite {

    fun exportPdfAndShare(
        context: Context,
        rows: List<OpeningStockEntity>
    ): Result<Unit> = runCatching {
        val file = File(context.cacheDir, "Opening_Stock_Register.pdf")
        writePdf(file, rows)
        share(context, file, "application/pdf")
    }

    fun exportExcelAndShare(
        context: Context,
        rows: List<OpeningStockEntity>
    ): Result<Unit> = runCatching {
        val file = File(context.cacheDir, "Opening_Stock_Register.xls")
        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Register"><Table>""")
            fun cell(v: String) {
                w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
            }
            w.write("<Row>")
            listOf("Entry No.","Date","Status").forEach(::cell)
            w.write("</Row>")
            rows.forEach { r ->
                w.write("<Row>")
                listOf(r.entryNumber, r.entryDate, r.status).forEach(::cell)
                w.write("</Row>")
            }
            w.write("</Table></Worksheet></Workbook>")
        }
        share(context, file, "application/vnd.ms-excel")
    }

    fun print(
        context: Context,
        rows: List<OpeningStockEntity>
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            "Opening Stock Register",
            RegisterPrintAdapter(rows),
            PrintAttributes.Builder().build()
        )
    }

    private fun writePdf(file: File, rows: List<OpeningStockEntity>) {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 10f }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var y = 45f
        
        page.canvas.drawText("Opening Stock Register", 32f, y, titlePaint); y += 28f
        page.canvas.drawText("Entry No. | Date | Status", 32f, y, paint); y += 20f
        
        rows.forEach { r ->
            if (y > 800f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                y = 45f
                page.canvas.drawText("Opening Stock Register (Cont.)", 32f, y, titlePaint); y += 28f
            }
            val line = "${r.entryNumber} | ${r.entryDate} | ${r.status}"
            page.canvas.drawText(line, 32f, y, paint)
            y += 17f
        }
        document.finishPage(page)
        FileOutputStream(file).use(document::writeTo)
        document.close()
    }

    private fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private class RegisterPrintAdapter(private val rows: List<OpeningStockEntity>) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("opening_stock_register.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback
        ) {
            runCatching {
                val temp = File.createTempFile("opening_stock_", ".pdf")
                writePdf(temp, rows)
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
