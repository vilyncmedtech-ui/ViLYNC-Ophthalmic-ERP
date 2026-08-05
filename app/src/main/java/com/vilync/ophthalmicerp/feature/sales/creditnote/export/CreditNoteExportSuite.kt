package com.vilync.ophthalmicerp.feature.sales.creditnote.export

import android.content.Context
import android.net.Uri
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import java.io.File

object CreditNoteExportSuite {

    private fun buildReport(creditNote: SalesCreditNoteEntity, items: List<SalesCreditNoteItemEntity>, lenses: List<SalesCreditNoteLensEntity>): ExportReport {
        return ExportReport(
            title = "CREDIT NOTE - ${creditNote.creditNoteNumber}",
            headers = listOf("Product", "Serial", "Power", "Qty", "Rate", "GST %", "Total"),
            rows = items.map { item ->
                val itemLenses = lenses.filter { it.creditNoteItemId == item.id }
                listOf(
                    item.productName,
                    itemLenses.joinToString(", ") { it.serialNumber }.ifBlank { "-" },
                    item.power,
                    item.quantity.toString(),
                    "₹ %.2f".format(item.rate),
                    "${item.gstPercent}%",
                    "₹ %.2f".format(item.totalAmount)
                )
            } + listOf(
                listOf("REASON", creditNote.reason, "", "", "", "", ""),
                listOf("SUMMARY", "Taxable: ₹ %.2f | GST: ₹ %.2f".format(creditNote.taxableAmount, creditNote.gstAmount), "", "", "", "", ""),
                listOf("GRAND TOTAL", "", "", "", "", "", "₹ %.2f".format(creditNote.totalAmount))
            )
        )
    }

    fun print(context: Context, creditNote: SalesCreditNoteEntity, items: List<SalesCreditNoteItemEntity>, lenses: List<SalesCreditNoteLensEntity>) {
        ReportExportUtils.print(context, buildReport(creditNote, items, lenses))
    }

    fun exportPdf(context: Context, uri: Uri, creditNote: SalesCreditNoteEntity, items: List<SalesCreditNoteItemEntity>, lenses: List<SalesCreditNoteLensEntity>) {
        ReportExportUtils.exportPdf(context, uri, buildReport(creditNote, items, lenses))
    }

    fun sharePdf(context: Context, creditNote: SalesCreditNoteEntity, items: List<SalesCreditNoteItemEntity>, lenses: List<SalesCreditNoteLensEntity>) {
        val file = File(context.cacheDir, "CreditNote_${creditNote.creditNoteNumber.replace("/", "_")}.pdf")
        ReportExportUtils.exportPdfToFile(context, file, buildReport(creditNote, items, lenses))
        ShareUtils.shareFile(context, file, "application/pdf", "Share Credit Note")
    }
}
