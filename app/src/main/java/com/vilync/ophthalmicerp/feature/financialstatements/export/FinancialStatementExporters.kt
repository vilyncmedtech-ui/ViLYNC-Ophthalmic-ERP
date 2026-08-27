package com.vilync.ophthalmicerp.feature.financialstatements.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.feature.financialstatements.domain.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object FinancialStatementExporters {

    fun exportToCsv(context: Context, title: String, rows: List<List<String>>): Result<File> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "${title.replace(" ", "_")}_$timestamp.csv")

            file.outputStream().use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // BOM
                rows.forEach { row ->
                    val line = row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n"
                    out.write(line.toByteArray(StandardCharsets.UTF_8))
                }
            }
            file
        }
    }

    fun shareFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "csv") "text/csv" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun getTrialBalanceRows(report: TrialBalanceReport): List<List<String>> {
        val header = listOf("Account", "Opening Dr", "Opening Cr", "Period Dr", "Period Cr", "Closing Dr", "Closing Cr")
        val data = report.rows.map { 
            listOf(it.accountName, it.openingDebit.toString(), it.openingCredit.toString(), it.periodDebit.toString(), it.periodCredit.toString(), it.closingDebit.toString(), it.closingCredit.toString())
        }
        val footer = listOf("TOTAL", report.totalOpeningDebit.toString(), report.totalOpeningCredit.toString(), report.totalPeriodDebit.toString(), report.totalPeriodCredit.toString(), report.totalClosingDebit.toString(), report.totalClosingCredit.toString())
        return listOf(header) + data + listOf(footer)
    }

    fun getProfitLossRows(report: ProfitLossReport): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Profit & Loss Report"))
        rows.add(listOf("Revenue"))
        report.incomeSection.items.forEach { rows.add(listOf(it.label, it.amount.toString())) }
        rows.add(listOf("Gross Profit", report.grossProfit.toString()))
        rows.add(listOf("Direct Costs"))
        report.directCostSection.items.forEach { rows.add(listOf(it.label, it.amount.toString())) }
        rows.add(listOf("Operating Expenses"))
        report.operatingExpenseSection.items.forEach { rows.add(listOf(it.label, it.amount.toString())) }
        rows.add(listOf("Net Profit", report.netProfit.toString()))
        return rows
    }

    fun getBalanceSheetRows(report: BalanceSheetReport): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Balance Sheet Report"))
        rows.add(listOf("LIABILITIES & EQUITY"))
        report.liabilitiesSection.items.forEach { rows.add(listOf(it.label, it.amount.toString())) }
        rows.add(listOf("TOTAL LIABILITIES", report.totalLiabilitiesAndEquity.toString()))
        rows.add(listOf(""))
        rows.add(listOf("ASSETS"))
        report.assetsSection.items.forEach { rows.add(listOf(it.label, it.amount.toString())) }
        rows.add(listOf("TOTAL ASSETS", report.totalAssets.toString()))
        return rows
    }
}
