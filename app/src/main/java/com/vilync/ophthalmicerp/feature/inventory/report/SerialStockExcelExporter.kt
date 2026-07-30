package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vilync.ophthalmicerp.data.dao.SerialStockRow
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object SerialStockExcelExporter {


    // =========================================================
    // EXPORT
    // =========================================================

    /**
     * Creates an Excel-compatible UTF-8 CSV file containing
     * the complete Serial Stock Register.
     *
     * One physical serial-tracked inventory unit = one row.
     *
     * Compatible with:
     *
     * - Microsoft Excel
     * - Google Sheets
     * - LibreOffice Calc
     *
     * No external Excel library is required.
     */
    fun export(
        context: Context,
        rows: List<SerialStockRow>,
        searchQuery: String = "",
        selectedStatus: String = "ALL"
    ): Result<File> {

        return runCatching {

            val exportDirectory =
                File(
                    context.cacheDir,
                    "exports"
                )


            if (!exportDirectory.exists()) {

                val created =
                    exportDirectory.mkdirs()

                if (
                    !created &&
                    !exportDirectory.exists()
                ) {

                    error(
                        "Unable to create export directory."
                    )
                }
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
                    "Serial_Stock_Register_$timestamp.csv"
                )


            val csv =
                buildCsv(
                    rows = rows,
                    searchQuery = searchQuery,
                    selectedStatus =
                        selectedStatus
                )


            // =================================================
            // WRITE UTF-8 BOM + CSV
            // =================================================
            //
            // UTF-8 BOM improves Microsoft Excel compatibility
            // when data contains Unicode text.
            // =================================================

            file.outputStream().use {
                    outputStream ->

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
    // SHARE
    // =========================================================

    /**
     * Opens Android share sheet for the generated
     * Excel-compatible CSV file.
     *
     * Uses the same FileProvider architecture as the existing
     * Stock Register exporter.
     */
    fun share(
        context: Context,
        file: File
    ): Result<Unit> {

        return runCatching {

            require(
                file.exists()
            ) {
                "Serial Stock export file does not exist."
            }


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
                        "Serial Stock Register"
                    )


                    putExtra(
                        Intent.EXTRA_TEXT,
                        "ViLYNC ERP - Serial Stock Register"
                    )


                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    shareIntent,
                    "Export Serial Stock Register to Excel"
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
        rows: List<SerialStockRow>,
        searchQuery: String = "",
        selectedStatus: String = "ALL"
    ): Result<File> {

        return runCatching {

            val file =
                export(
                    context = context,
                    rows = rows,
                    searchQuery =
                        searchQuery,
                    selectedStatus =
                        selectedStatus
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
        rows: List<SerialStockRow>,
        searchQuery: String,
        selectedStatus: String
    ): String {

        return buildString {


            // =================================================
            // REPORT HEADER
            // =================================================

            appendCsvRow(
                "ViLYNC ERP"
            )


            appendCsvRow(
                "Serial Stock Register"
            )


            appendCsvRow(
                "Generated At",
                currentDisplayDateTime()
            )


            appendCsvRow(
                "Total Serial Units",
                rows.size.toString()
            )


            appendCsvRow(
                "In Stock",
                rows.count {

                    it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    )
                }.toString()
            )


            appendCsvRow(
                "Issued / Other Status",
                rows.count {

                    !it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    )
                }.toString()
            )


            if (
                searchQuery.isNotBlank()
            ) {

                appendCsvRow(
                    "Search",
                    searchQuery.trim()
                )
            }


            if (
                selectedStatus.isNotBlank() &&
                !selectedStatus.equals(
                    other = "ALL",
                    ignoreCase = true
                )
            ) {

                appendCsvRow(
                    "Status Filter",
                    formatStatus(
                        selectedStatus
                    )
                )
            }


            appendLine()


            // =================================================
            // COLUMN HEADINGS
            // =================================================

            appendCsvRow(
                "Serial Number",
                "Product",
                "Brand",
                "Model",
                "Category",
                "Power",
                "Batch Number",
                "Expiry",
                "Received Date",
                "Supplier",
                "Purchase Invoice",
                "Current Status"
            )


            // =================================================
            // SERIAL STOCK ROWS
            // =================================================

            rows.forEach { row ->

                appendCsvRow(

                    row.serialNumber,

                    row.productName,

                    row.brandName,

                    row.model,

                    row.category,

                    row.power,

                    row.batchNumber,

                    formatExpiry(
                        row.expiryDate
                    ),

                    row.receivedDate,

                    row.supplierName,

                    row.purchaseInvoiceNumber,

                    formatStatus(
                        row.status
                    )
                )
            }


            // =================================================
            // FOOTER
            // =================================================

            appendLine()


            appendCsvRow(
                "TOTAL SERIAL UNITS",
                rows.size.toString()
            )


            appendCsvRow(
                "IN STOCK",
                rows.count {

                    it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    )
                }.toString()
            )


            appendCsvRow(
                "OTHER / ISSUED",
                rows.count {

                    !it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    )
                }.toString()
            )
        }
    }


    // =========================================================
    // CURRENT DISPLAY DATE / TIME
    // =========================================================

    private fun currentDisplayDateTime():
            String {

        return SimpleDateFormat(
            "dd-MM-yyyy hh:mm a",
            Locale.US
        ).format(
            Date()
        )
    }


    // =========================================================
    // EXPIRY FORMATTER
    // =========================================================

    /**
     * Converts common stored expiry formats into MM-YY.
     *
     * Examples:
     *
     * 1029       -> 10-29
     * 0528       -> 05-28
     * 10/2029    -> 10-29
     * 10-2029    -> 10-29
     * 2029-10    -> 10-29
     * 2029-10-31 -> 10-29
     */
    private fun formatExpiry(
        rawExpiry: String
    ): String {

        val value =
            rawExpiry.trim()


        if (
            value.isBlank()
        ) {

            return ""
        }


        // -----------------------------------------------------
        // MMYY
        // -----------------------------------------------------

        if (
            value.length == 4 &&
            value.all {
                it.isDigit()
            }
        ) {

            return value.substring(
                0,
                2
            ) +
                    "-" +
                    value.substring(
                        2,
                        4
                    )
        }


        // -----------------------------------------------------
        // MM/YYYY
        // -----------------------------------------------------

        val slashParts =
            value.split(
                "/"
            )


        if (
            slashParts.size == 2
        ) {

            val month =
                slashParts[0]
                    .trim()


            val year =
                slashParts[1]
                    .trim()


            if (
                month.length in 1..2 &&
                year.length >= 2 &&
                month.all {
                    it.isDigit()
                } &&
                year.all {
                    it.isDigit()
                }
            ) {

                return month
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        year.takeLast(
                            2
                        )
            }
        }


        // -----------------------------------------------------
        // DASH FORMATS
        // -----------------------------------------------------

        val dashParts =
            value.split(
                "-"
            )


        // MM-YYYY
        if (
            dashParts.size == 2
        ) {

            val first =
                dashParts[0]
                    .trim()


            val second =
                dashParts[1]
                    .trim()


            if (
                first.length in 1..2 &&
                second.length == 4 &&
                first.all {
                    it.isDigit()
                } &&
                second.all {
                    it.isDigit()
                }
            ) {

                return first
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        second.takeLast(
                            2
                        )
            }
        }


        // YYYY-MM or YYYY-MM-DD
        if (
            dashParts.size >= 2
        ) {

            val year =
                dashParts[0]
                    .trim()


            val month =
                dashParts[1]
                    .trim()


            if (
                year.length == 4 &&
                month.length in 1..2 &&
                year.all {
                    it.isDigit()
                } &&
                month.all {
                    it.isDigit()
                }
            ) {

                return month
                    .padStart(
                        2,
                        '0'
                    ) +
                        "-" +
                        year.takeLast(
                            2
                        )
            }
        }


        // -----------------------------------------------------
        // FALLBACK
        // -----------------------------------------------------

        return value
    }


    // =========================================================
    // STATUS FORMATTER
    // =========================================================

    private fun formatStatus(
        status: String
    ): String {

        val normalized =
            status
                .trim()
                .uppercase()


        return when (
            normalized
        ) {

            "IN_STOCK" ->
                "IN STOCK"

            "SAMPLE" ->
                "SAMPLE"

            "DEMO" ->
                "DEMO"

            "APPROVAL" ->
                "APPROVAL"

            "SOLD" ->
                "SOLD"

            "RETURNED" ->
                "RETURNED"

            else ->
                normalized
                    .replace(
                        "_",
                        " "
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
     * Example:
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