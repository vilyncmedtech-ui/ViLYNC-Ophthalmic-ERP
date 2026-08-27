package com.vilync.ophthalmicerp.feature.sales.proforma.export

import android.content.Context
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object ProformaInvoiceExportSuite {

    fun print(context: Context, proforma: ProformaInvoiceEntity, items: List<ProformaInvoiceItemEntity>) {
        val report = ExportReport(
            title = "PROFORMA INVOICE - ${proforma.proformaNumber}",
            headers = listOf("Product", "Power", "Qty", "Rate", "Taxable", "GST %", "Total"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.power,
                    it.quantity.toString(),
                    "₹ %.2f".format(it.rate),
                    "₹ %.2f".format(it.taxableAmount),
                    "${it.gstPercent}%",
                    "₹ %.2f".format(it.totalAmount)
                )
            } + listOf(
                listOf("DECLARATION", "This is a Proforma Invoice. This document is not a Tax Invoice. No tax liability arises from this document.", "", "", "", "", ""),
                listOf("SUMMARY", "Taxable: ₹ %.2f | GST: ₹ %.2f".format(proforma.taxableAmount, proforma.gstAmount), "", "", "", "", ""),
                listOf("GRAND TOTAL", "", "", "", "", "", "₹ %.2f".format(proforma.totalAmount))
            )
        )
        ReportExportUtils.print(context, report)
    }

    fun exportPdf(context: Context, uri: android.net.Uri, proforma: ProformaInvoiceEntity, items: List<ProformaInvoiceItemEntity>) {
        val report = ExportReport(
            title = "PROFORMA INVOICE - ${proforma.proformaNumber}",
            headers = listOf("Product", "Power", "Qty", "Rate", "Taxable", "GST %", "Total"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.power,
                    it.quantity.toString(),
                    "₹ %.2f".format(it.rate),
                    "₹ %.2f".format(it.taxableAmount),
                    "${it.gstPercent}%",
                    "₹ %.2f".format(it.totalAmount)
                )
            }
        )
        ReportExportUtils.exportPdf(context, uri, report)
    }

    fun sharePdf(context: Context, proforma: ProformaInvoiceEntity, items: List<ProformaInvoiceItemEntity>) {
        val file = File(context.cacheDir, "Proforma_${proforma.proformaNumber.replace("/", "_")}.pdf")
        val report = ExportReport(
            title = "PROFORMA INVOICE - ${proforma.proformaNumber}",
            headers = listOf("Product", "Power", "Qty", "Rate", "Taxable", "GST %", "Total"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.power,
                    it.quantity.toString(),
                    "₹ %.2f".format(it.rate),
                    "₹ %.2f".format(it.taxableAmount),
                    "${it.gstPercent}%",
                    "₹ %.2f".format(it.totalAmount)
                )
            }
        )
        ReportExportUtils.exportPdfToFile(context, file, report)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Proforma Invoice")
    }

    fun exportExcelAndShare(
        context: Context,
        proforma: ProformaInvoiceEntity,
        items: List<ProformaInvoiceItemEntity>
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "Proforma_${proforma.proformaNumber.replace("/", "_")}.xls")

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Proforma"><Table>""")
            
            fun row(vararg values: String) {
                w.write("<Row>")
                values.forEach { v ->
                    w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
                }
                w.write("</Row>")
            }

            row("Proforma Invoice", proforma.proformaNumber)
            row("Date", proforma.proformaDate)
            row("Customer", proforma.customerName)
            row("Valid Until", proforma.validUntilDate)
            row("Status", proforma.status)
            row()
            row("Product", "Power", "Qty", "Rate", "Taxable", "GST %", "Total")
            items.forEach { itm ->
                row(itm.productName, itm.power, itm.quantity.toString(), "%.2f".format(itm.rate), "%.2f".format(itm.taxableAmount), "${itm.gstPercent}%", "%.2f".format(itm.totalAmount))
            }
            row()
            row("Taxable Amount", "%.2f".format(proforma.taxableAmount))
            row("GST Amount", "%.2f".format(proforma.gstAmount))
            row("Grand Total", "%.2f".format(proforma.totalAmount))
            row()
            row("Remarks", proforma.remarks)
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Proforma Invoice Excel")
    }

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
