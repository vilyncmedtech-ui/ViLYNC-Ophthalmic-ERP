package com.vilync.ophthalmicerp.feature.purchase.returnentry

import android.content.Context
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object PurchaseReturnExportSuite {

    fun exportExcelAndShare(
        context: Context,
        purchaseReturn: PurchaseReturnEntity,
        items: List<PurchaseReturnItemEntity>,
        lenses: Map<Long, List<PurchaseReturnLensEntity>>
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "PurchaseReturn_${purchaseReturn.creditNoteNumber.replace("/", "_")}.xls")

        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Return"><Table>""")
            
            fun row(vararg values: String) {
                w.write("<Row>")
                values.forEach { v ->
                    val escaped = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
                    w.write("<Cell><Data ss:Type=\"String\">$escaped</Data></Cell>")
                }
                w.write("</Row>")
            }

            row("Purchase Return (Debit Note)", purchaseReturn.creditNoteNumber)
            row("Date", purchaseReturn.creditNoteDate)
            row("Vendor", purchaseReturn.supplierName)
            row("Original Invoice", purchaseReturn.originalInvoiceNumber)
            row("Status", purchaseReturn.status)
            row()
            row("Product", "Serial", "Qty", "Rate", "GST %", "Total")
            items.forEach { itm ->
                val itmLenses = lenses[itm.id].orEmpty()
                row(
                    itm.productName,
                    itmLenses.joinToString(", ") { it.serialNumber },
                    itm.quantity.toString(),
                    "%.2f".format(itm.rate),
                    "${itm.gstPercent}%",
                    "%.2f".format(itm.totalAmount)
                )
            }
            row()
            row("Grand Total", "%.2f".format(purchaseReturn.totalAmount))
            row()
            row("Remarks", purchaseReturn.remarks)
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Purchase Return Excel")
    }
}
