package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StockRegisterExcelExporter {

    // =========================================================
    // EXPORT
    // =========================================================

    /**
     * Creates an Excel-compatible UTF-8 CSV file from the
     * Stock Register report.
     *
     * The generated file can be opened directly in:
     *
     * - Microsoft Excel
     * - Google Sheets
     * - LibreOffice Calc
     *
     * No external Excel library is required.
     */
    fun export(
        context: Context,
        report: StockRegisterReport
    ): Result<File> {

        return runCatching {

            val exportDirectory =
                File(
                    context.cacheDir,
                    "exports"
                )

            if (!exportDirectory.exists()) {
                exportDirectory.mkdirs()
            }


            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(
                    Date()
                )


            val file =
                File(
                    exportDirectory,
                    "Stock_Register_$timestamp.csv"
                )


            val csv =
                buildCsv(
                    report = report
                )


            /*
             * UTF-8 BOM is intentionally written at the beginning.
             *
             * This improves compatibility with Microsoft Excel,
             * especially when product/customer text may contain
             * Indian-language or other Unicode characters.
             */
            file.outputStream().use { outputStream ->

                outputStream.write(
                    byteArrayOf(
                        0xEF.toByte(),
                        0xBB.toByte(),
                        0xBF.toByte()
                    )
                )

                outputStream.write(
                    csv.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
            }


            file
        }
    }


    // =========================================================
    // SHARE / OPEN
    // =========================================================

    /**
     * Opens Android's share sheet for the generated
     * Excel-compatible CSV file.
     *
     * IMPORTANT:
     *
     * FileProvider must exist in AndroidManifest before this
     * function is connected to the UI.
     */
    fun share(
        context: Context,
        file: File
    ): Result<Unit> {

        return runCatching {

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )


            val shareIntent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        "text/csv"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Stock Register"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    shareIntent,
                    "Export Stock Register to Excel"
                )


            chooser.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )


            context.startActivity(
                chooser
            )
        }
    }


    // =========================================================
    // EXPORT AND SHARE
    // =========================================================

    fun exportAndShare(
        context: Context,
        report: StockRegisterReport
    ): Result<File> {

        return runCatching {

            val file =
                export(
                    context = context,
                    report = report
                ).getOrThrow()


            share(
                context = context,
                file = file
            ).getOrThrow()


            file
        }
    }


    // =========================================================
    // CSV BUILDER
    // =========================================================

    private fun buildCsv(
        report: StockRegisterReport
    ): String {

        return buildString {

            // =================================================
            // REPORT HEADER
            // =================================================

            appendCsvRow(
                "ViLYNC ERP"
            )

            appendCsvRow(
                report.title
            )

            appendCsvRow(
                "Generated At",
                report.generatedAt
            )


            if (
                report.searchQuery.isNotBlank()
            ) {

                appendCsvRow(
                    "Search / Filter",
                    report.searchQuery
                )
            }


            appendLine()


            // =================================================
            // COLUMN HEADINGS
            // =================================================

            appendCsvRow(
                "Product",
                "Model",
                "Category",
                "Power",
                "Purchased",
                "Returned",
                "Available"
            )


            // =================================================
            // STOCK ROWS
            // =================================================

            report.rows.forEach { row ->

                appendCsvRow(
                    row.productName,
                    row.model,
                    row.category,
                    row.power,
                    row.purchasedQuantity.toString(),
                    row.returnedQuantity.toString(),
                    row.availableQuantity.toString()
                )
            }


            appendLine()


            // =================================================
            // TOTALS
            // =================================================

            appendCsvRow(
                "TOTAL",
                "",
                "",
                "",
                report.totalPurchased.toString(),
                report.totalReturned.toString(),
                report.totalAvailable.toString()
            )
        }
    }


    // =========================================================
    // CSV ROW
    // =========================================================

    private fun StringBuilder.appendCsvRow(
        vararg values: String
    ) {

        append(
            values.joinToString(
                separator = ","
            ) { value ->

                escapeCsvValue(
                    value
                )
            }
        )

        appendLine()
    }


    // =========================================================
    // CSV ESCAPING
    // =========================================================

    /**
     * Always quotes CSV values.
     *
     * Embedded quotes are escaped according to CSV rules:
     *
     * ABC "Lens"
     *
     * becomes:
     *
     * "ABC ""Lens"""
     */
    private fun escapeCsvValue(
        value: String
    ): String {

        val escaped =
            value.replace(
                "\"",
                "\"\""
            )

        return "\"$escaped\""
    }
}