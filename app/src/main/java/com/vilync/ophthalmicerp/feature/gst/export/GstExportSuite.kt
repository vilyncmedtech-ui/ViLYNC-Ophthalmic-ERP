package com.vilync.ophthalmicerp.feature.gst.export

import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.feature.gst.model.GstDocumentRow
import com.vilync.ophthalmicerp.feature.gst.model.GstReportSnapshot
import com.vilync.ophthalmicerp.feature.gst.model.GstReportType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GstExportSuite {

    fun print(
        context: Context,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ): Result<Unit> = runCatching {
        val printManager =
            context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager

        printManager.print(
            reportType.title,
            GstPrintAdapter(
                context = context,
                reportType = reportType,
                snapshot = snapshot,
                financialYearDisplayName = financialYearDisplayName
            ),
            PrintAttributes.Builder().build()
        )
    }

    fun exportPdfAndShare(
        context: Context,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ): Result<Unit> = runCatching {
        GstPdfExporter.exportAndShare(
            context = context,
            reportType = reportType,
            snapshot = snapshot,
            financialYearDisplayName = financialYearDisplayName
        ).getOrThrow()
    }

    fun exportExcelAndShare(
        context: Context,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ): Result<Unit> = runCatching {
        val exportDirectory = File(context.cacheDir, "exports")
        if (!exportDirectory.exists() && !exportDirectory.mkdirs() && !exportDirectory.exists()) {
            error("Unable to create export directory.")
        }

        val timestamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val file = File(
            exportDirectory,
            "${safeName(reportType.title)}_$timestamp.xls"
        )

        writeExcel(
            file = file,
            reportType = reportType,
            snapshot = snapshot,
            financialYearDisplayName = financialYearDisplayName
        )

        share(
            context = context,
            file = file,
            mime = "application/vnd.ms-excel",
            chooserTitle = "Export ${reportType.title} Excel"
        )
    }

    private fun writeExcel(
        file: File,
        reportType: GstReportType,
        snapshot: GstReportSnapshot,
        financialYearDisplayName: String
    ) {
        OutputStreamWriter(
            FileOutputStream(file),
            Charsets.UTF_8
        ).use { writer ->

            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.write("""<?mso-application progid="Excel.Sheet"?>""")
            writer.write(
                """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" """ +
                        """xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
            )

            writer.write(
                """<Worksheet ss:Name="${xml(sheetName(reportType.title))}"><Table>"""
            )

            row(writer, listOf("ViLYNC OPHTHALMIC ERP"))
            row(writer, listOf(reportType.title))
            row(writer, listOf("Financial Year", financialYearDisplayName))
            row(writer, listOf("Generated", displayDateTime()))
            emptyRow(writer)

            when (reportType) {
                GstReportType.GSTR1 -> {
                    summary(
                        writer,
                        listOf(
                            "Taxable Outward Supplies" to snapshot.outputTaxable,
                            "Output GST" to snapshot.outputGst,
                            "Credit Note GST" to snapshot.creditNoteGst,
                            "Adjusted Output GST" to snapshot.adjustedOutputGst
                        )
                    )
                    documentSection(writer, "B2B Invoices", snapshot.b2bSales)
                    documentSection(writer, "B2C Invoices", snapshot.b2cSales)
                }

                GstReportType.GSTR3B -> {
                    summary(
                        writer,
                        listOf(
                            "Outward Taxable" to snapshot.outputTaxable,
                            "Output GST" to snapshot.outputGst,
                            "Input GST / ITC" to snapshot.inputGst,
                            "Net GST Position" to snapshot.netGstPosition
                        )
                    )
                    note(
                        writer,
                        "Purchase ITC is shown from persisted purchase GST totals. " +
                                "Historical purchase GST is not reclassified into tax heads."
                    )
                }

                GstReportType.SALES_REGISTER -> {
                    summary(
                        writer,
                        listOf(
                            "Posted Sales" to snapshot.sales.size.toDouble(),
                            "Taxable Value" to snapshot.outputTaxable,
                            "Output GST" to snapshot.outputGst
                        ),
                        countLabels = setOf("Posted Sales")
                    )
                    documentSection(writer, "Sales GST Register", snapshot.sales)
                }

                GstReportType.PURCHASE_REGISTER -> {
                    summary(
                        writer,
                        listOf(
                            "Purchases" to snapshot.purchases.size.toDouble(),
                            "Taxable Value" to snapshot.inputTaxable,
                            "Input GST" to snapshot.inputGst
                        ),
                        countLabels = setOf("Purchases")
                    )
                    documentSection(writer, "Purchase GST Register", snapshot.purchases)
                }

                GstReportType.HSN -> {
                    row(writer, listOf("HSN Summary"))
                    row(
                        writer,
                        listOf("HSN", "Description", "Quantity", "Taxable Amount", "GST %", "GST Amount", "Total Amount")
                    )
                    snapshot.hsnRows.forEach { item ->
                        row(
                            writer,
                            listOf(
                                item.hsnCode,
                                item.description,
                                item.quantity.toString(),
                                decimal(item.taxableAmount),
                                decimal(item.gstPercent) + "%",
                                decimal(item.gstAmount),
                                decimal(item.totalAmount)
                            )
                        )
                    }
                    if (snapshot.hsnRows.isEmpty()) {
                        row(writer, listOf("No HSN transaction data found."))
                    }
                }

                GstReportType.B2B -> {
                    summary(
                        writer,
                        listOf(
                            "B2B Invoices" to snapshot.b2bSales.size.toDouble(),
                            "Taxable Value" to snapshot.b2bSales.sumOf { it.taxableAmount },
                            "GST" to snapshot.b2bSales.sumOf { it.totalGst }
                        ),
                        countLabels = setOf("B2B Invoices")
                    )
                    documentSection(writer, "B2B Sales", snapshot.b2bSales)
                }

                GstReportType.B2C -> {
                    summary(
                        writer,
                        listOf(
                            "B2C Invoices" to snapshot.b2cSales.size.toDouble(),
                            "Taxable Value" to snapshot.b2cSales.sumOf { it.taxableAmount },
                            "GST" to snapshot.b2cSales.sumOf { it.totalGst }
                        ),
                        countLabels = setOf("B2C Invoices")
                    )
                    documentSection(writer, "B2C Sales", snapshot.b2cSales)
                }

                GstReportType.RETURNS -> {
                    summary(
                        writer,
                        listOf(
                            "Posted Sales Credit Notes" to snapshot.creditNotes.size.toDouble(),
                            "Taxable Adjustment" to snapshot.creditNoteTaxable,
                            "GST Adjustment" to snapshot.creditNoteGst
                        ),
                        countLabels = setOf("Posted Sales Credit Notes")
                    )
                    documentSection(writer, "Returns / Adjustments", snapshot.creditNotes)
                    note(
                        writer,
                        "Purchase Returns remain in the existing Purchase Return Register " +
                                "and are not silently netted into ITC here."
                    )
                }

                GstReportType.ITC -> {
                    summary(
                        writer,
                        listOf(
                            "Purchase Taxable Value" to snapshot.inputTaxable,
                            "Total Input GST / ITC" to snapshot.inputGst
                        )
                    )
                    note(
                        writer,
                        "Input GST is read from persisted Purchase GST. " +
                                "No historical CGST/SGST/IGST reclassification is performed."
                    )
                    documentSection(writer, "ITC Purchase Details", snapshot.purchases)
                }

                GstReportType.OUTPUT_TAX -> {
                    summary(
                        writer,
                        listOf(
                            "Output CGST" to snapshot.outputCgst,
                            "Output SGST" to snapshot.outputSgst,
                            "Output IGST" to snapshot.outputIgst,
                            "Total Output GST" to snapshot.outputGst
                        )
                    )
                }

                GstReportType.TAX_HEADS -> {
                    summary(
                        writer,
                        listOf(
                            "CGST" to snapshot.outputCgst,
                            "SGST" to snapshot.outputSgst,
                            "IGST" to snapshot.outputIgst,
                            "Total" to snapshot.outputGst
                        )
                    )
                    note(writer, "Tax-head summary uses persisted POSTED Sales tax heads only.")
                }
            }

            writer.write("</Table></Worksheet></Workbook>")
        }
    }

    private fun summary(
        writer: OutputStreamWriter,
        values: List<Pair<String, Double>>,
        countLabels: Set<String> = emptySet()
    ) {
        row(writer, listOf("Summary"))
        row(writer, listOf("Particular", "Value"))

        values.forEach { (label, value) ->
            row(
                writer,
                listOf(
                    label,
                    if (label in countLabels) value.toInt().toString() else decimal(value)
                )
            )
        }

        emptyRow(writer)
    }

    private fun documentSection(
        writer: OutputStreamWriter,
        title: String,
        rows: List<GstDocumentRow>
    ) {
        row(writer, listOf(title))
        row(
            writer,
            listOf(
                "Document No.",
                "Date",
                "Party",
                "GSTIN",
                "Taxable Amount",
                "CGST",
                "SGST",
                "IGST",
                "Total GST",
                "Total Amount"
            )
        )

        rows.forEach { item ->
            row(
                writer,
                listOf(
                    item.documentNumber,
                    item.documentDate,
                    item.partyName,
                    item.gstin,
                    decimal(item.taxableAmount),
                    decimal(item.cgstAmount),
                    decimal(item.sgstAmount),
                    decimal(item.igstAmount),
                    decimal(item.totalGst),
                    decimal(item.totalAmount)
                )
            )
        }

        if (rows.isEmpty()) {
            row(writer, listOf("No records found."))
        }

        emptyRow(writer)
    }

    private fun note(
        writer: OutputStreamWriter,
        message: String
    ) {
        row(writer, listOf("Note", message))
        emptyRow(writer)
    }

    private fun row(
        writer: OutputStreamWriter,
        values: List<String>
    ) {
        writer.write("<Row>")
        values.forEach { value ->
            writer.write(
                "<Cell><Data ss:Type=\"String\">${xml(value)}</Data></Cell>"
            )
        }
        writer.write("</Row>")
    }

    private fun emptyRow(writer: OutputStreamWriter) {
        writer.write("<Row></Row>")
    }

    private fun share(
        context: Context,
        file: File,
        mime: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, chooserTitle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun safeName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "GST_Report" }

    private fun sheetName(value: String): String =
        value.replace(Regex("""[\\/:*?\[\]]"""), " ")
            .trim()
            .take(31)
            .ifBlank { "GST Report" }

    private fun xml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun decimal(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun displayDateTime(): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())

    private class GstPrintAdapter(
        private val context: Context,
        private val reportType: GstReportType,
        private val snapshot: GstReportSnapshot,
        private val financialYearDisplayName: String
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
                PrintDocumentInfo.Builder("${safeName(reportType.title)}.pdf")
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

            var tempFile: File? = null

            runCatching {
                tempFile = GstPdfExporter.export(
                    context = context,
                    reportType = reportType,
                    snapshot = snapshot,
                    financialYearDisplayName = financialYearDisplayName
                ).getOrThrow()

                tempFile!!.inputStream().use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
            }.onSuccess {
                callback.onWriteFinished(
                    arrayOf(android.print.PageRange.ALL_PAGES)
                )
            }.onFailure { error ->
                callback.onWriteFailed(
                    error.message ?: "Unable to print GST report."
                )
            }
        }
    }
}
