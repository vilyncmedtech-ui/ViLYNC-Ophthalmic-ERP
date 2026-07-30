package com.vilync.ophthalmicerp.feature.sales.register

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object SalesRegisterExportSuite {

    fun exportPdfAndShare(
        context: Context,
        title: String,
        rows: List<SalesRegisterRow>
    ): Result<Unit> = runCatching {
        val file = File(context.cacheDir, safeName(title) + ".pdf")
        writePdf(file, title, rows)
        share(context, file, "application/pdf")
    }

    fun exportExcelAndShare(
        context: Context,
        title: String,
        rows: List<SalesRegisterRow>
    ): Result<Unit> = runCatching {
        // SpreadsheetML is an Excel-readable workbook without adding a third-party dependency.
        val file = File(context.cacheDir, safeName(title) + ".xls")
        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Register"><Table>""")
            fun cell(v: String) {
                w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
            }
            w.write("<Row>")
            listOf("Document No.","Date","Customer","Status","Amount","Details").forEach(::cell)
            w.write("</Row>")
            rows.forEach { r ->
                w.write("<Row>")
                listOf(
                    r.documentNumber, r.documentDate, r.customerName, r.status,
                    r.amount?.let { "%.2f".format(it) }.orEmpty(), r.secondaryInfo
                ).forEach(::cell)
                w.write("</Row>")
            }
            w.write("</Table></Worksheet></Workbook>")
        }
        share(context, file, "application/vnd.ms-excel")
    }

    fun print(
        context: Context,
        title: String,
        rows: List<SalesRegisterRow>
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print(
            title,
            RegisterPrintAdapter(title, rows),
            PrintAttributes.Builder().build()
        )
    }

    private fun writePdf(file: File, title: String, rows: List<SalesRegisterRow>) {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 10f }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var y = 45f
        fun header() {
            page.canvas.drawText(title, 32f, y, titlePaint); y += 28f
            page.canvas.drawText("No. | Date | Customer | Status | Amount | Details", 32f, y, paint); y += 20f
        }
        header()
        rows.forEach { r ->
            if (y > 800f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                y = 45f
                header()
            }
            val amount = r.amount?.let { "%.2f".format(it) }.orEmpty()
            val line = "${r.documentNumber} | ${r.documentDate} | ${r.customerName} | ${r.status} | $amount | ${r.secondaryInfo}"
            page.canvas.drawText(line.take(95), 32f, y, paint)
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

    private fun safeName(value: String) =
        value.replace(Regex("[^A-Za-z0-9._-]+"), "_")

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private class RegisterPrintAdapter(
        private val title: String,
        private val rows: List<SalesRegisterRow>
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("$title.pdf")
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
            runCatching {
                val temp = File.createTempFile("sales_register_", ".pdf")
                writePdf(temp, title, rows)
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
