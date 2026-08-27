package com.vilync.ophthalmicerp.feature.sales.sample.export

import android.content.Context
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object SampleIssueExportSuite {

    fun print(context: Context, sample: SampleIssueEntity, items: List<SampleIssueItemEntity>) {
        val report = ExportReport(
            title = "SAMPLE DISTRIBUTION - ${sample.sampleIssueNumber}",
            headers = listOf("Product", "Power", "Serial Number", "Batch", "Expiry"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.power,
                    it.serialNumber,
                    it.batchNumber,
                    it.expiryDate
                )
            } + listOf(
                listOf("DECLARATION", "This material is supplied strictly for evaluation. This document is NOT a Tax Invoice.", "", "", ""),
                listOf("PURPOSE", sample.sampleType, "", "", ""),
                listOf("EXPECTED RETURN", sample.expectedReturnDate, "", "", ""),
                listOf("REMARKS", sample.remarks, "", "", "")
            )
        )
        ReportExportUtils.print(context, report)
    }

    fun exportPdfAndShare(context: Context, sample: SampleIssueEntity, items: List<SampleIssueItemEntity>) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "Sample_${sample.sampleIssueNumber.replace("/", "_")}.pdf")

        val report = ExportReport(
            title = "SAMPLE DISTRIBUTION - ${sample.sampleIssueNumber}",
            headers = listOf("Product", "Power", "Serial Number", "Batch", "Expiry"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.power,
                    it.serialNumber,
                    it.batchNumber,
                    it.expiryDate
                )
            }
        )
        ReportExportUtils.exportPdfToFile(context, file, report)
        ShareUtils.shareFile(context, file, "application/pdf", "Share Sample Distribution PDF")
    }

    fun exportExcelAndShare(
        context: Context,
        sample: SampleIssueEntity,
        items: List<SampleIssueItemEntity>
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "Sample_${sample.sampleIssueNumber.replace("/", "_")}.xlsx")

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Sample"><Table>""")
            
            fun row(vararg values: String) {
                w.write("<Row>")
                values.forEach { v ->
                    w.write("<Cell><Data ss:Type=\"String\">${xml(v)}</Data></Cell>")
                }
                w.write("</Row>")
            }

            row("Sample Issue", sample.sampleIssueNumber)
            row("Date", sample.sampleIssueDate)
            row("Hospital / Doctor", sample.customerName)
            row("Purpose", sample.sampleType)
            row("Expected Return", sample.expectedReturnDate)
            row("Status", sample.status)
            row()
            row("Product", "Power", "Serial Number", "Batch", "Expiry", "Status")
            items.forEach { itm ->
                row(itm.productName, itm.power, itm.serialNumber, itm.batchNumber, itm.expiryDate, itm.settlementStatus)
            }
            row()
            row("Remarks", sample.remarks)
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(
            context, 
            file, 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
            "Share Sample Distribution Excel"
        )
    }

    private fun xml(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
