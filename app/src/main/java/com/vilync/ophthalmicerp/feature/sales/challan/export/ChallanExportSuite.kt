package com.vilync.ophthalmicerp.feature.sales.challan.export

import android.content.Context
import android.net.Uri
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import java.io.File

object ChallanExportSuite {

    private fun buildReport(challan: ChallanEntity, items: List<ChallanItemEntity>): ExportReport {
        return ExportReport(
            title = "DELIVERY CHALLAN - ${challan.challanNumber}",
            headers = listOf("Product", "Serial", "Power", "Batch", "Expiry"),
            rows = items.map {
                listOf(
                    it.productName,
                    it.serialNumber,
                    it.power,
                    it.batchNumber,
                    it.expiryDate
                )
            } + listOf(
                listOf("DECLARATION", "This is a Delivery Challan. This document is not a Tax Invoice.", "", "", ""),
                listOf("REMARKS", challan.remarks, "", "", "")
            )
        )
    }

    fun print(context: Context, challan: ChallanEntity, items: List<ChallanItemEntity>) {
        ReportExportUtils.print(context, buildReport(challan, items))
    }

    fun exportPdf(context: Context, uri: Uri, challan: ChallanEntity, items: List<ChallanItemEntity>) {
        ReportExportUtils.exportPdf(context, uri, buildReport(challan, items))
    }

    fun sharePdf(context: Context, challan: ChallanEntity, items: List<ChallanItemEntity>) {
        val file = File(context.cacheDir, "Challan_${challan.challanNumber.replace("/", "_")}.pdf")
        ReportExportUtils.exportPdfToFile(context, file, buildReport(challan, items))
        ShareUtils.shareFile(context, file, "application/pdf", "Share Delivery Challan")
    }
}
