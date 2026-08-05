package com.vilync.ophthalmicerp.feature.sales.sample.export

import android.content.Context
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import java.io.File

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

    fun exportPdf(context: Context, uri: android.net.Uri, sample: SampleIssueEntity, items: List<SampleIssueItemEntity>) {
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
        ReportExportUtils.exportPdf(context, uri, report)
    }

    fun sharePdf(context: Context, sample: SampleIssueEntity, items: List<SampleIssueItemEntity>) {
        val file = File(context.cacheDir, "Sample_${sample.sampleIssueNumber.replace("/", "_")}.pdf")
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
        ShareUtils.shareFile(context, file, "application/pdf", "Share Sample Distribution")
    }
}
