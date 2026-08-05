package com.vilync.ophthalmicerp.feature.sales.proforma.export

import android.content.Context
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import java.io.File

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
}
