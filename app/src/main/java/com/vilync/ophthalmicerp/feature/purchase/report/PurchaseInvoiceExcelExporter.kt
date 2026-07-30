package com.vilync.ophthalmicerp.feature.purchase.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object PurchaseInvoiceExcelExporter {


    // =========================================================
    // EXPORT
    // =========================================================

    /**
     * Creates an Excel-compatible UTF-8 CSV file.
     *
     * Can be opened in:
     *
     * - Microsoft Excel
     * - Google Sheets
     * - LibreOffice Calc
     */
    fun export(
        context: Context,
        report: PurchaseInvoiceReport
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


            val safeInvoiceNumber =
                sanitizeFileName(
                    report.invoiceNumber
                )


            val fileName =

                if (safeInvoiceNumber.isNotBlank()) {

                    "Purchase_Invoice_${safeInvoiceNumber}_$timestamp.csv"

                } else {

                    "Purchase_Invoice_$timestamp.csv"
                }


            val file =
                File(
                    exportDirectory,
                    fileName
                )


            val csv =
                buildCsv(
                    report = report
                )


            // UTF-8 BOM improves Excel Unicode compatibility.
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
    // SHARE
    // =========================================================

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

                    type = "text/csv"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Purchase Invoice"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            val chooser =
                Intent.createChooser(
                    shareIntent,
                    "Export Purchase Invoice to Excel"
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
        report: PurchaseInvoiceReport
    ): Result<File> {

        return runCatching {

            val file =
                export(
                    context = context,
                    report = report
                )
                    .getOrThrow()


            share(
                context = context,
                file = file
            )
                .getOrThrow()


            file
        }
    }


    // =========================================================
    // BUILD CSV
    // =========================================================

    private fun buildCsv(
        report: PurchaseInvoiceReport
    ): String {

        return buildString {


            // =================================================
            // REPORT HEADER
            // =================================================

            appendCsvRow(
                "ViLYNC Ophthalmic ERP"
            )

            appendCsvRow(
                report.title
            )

            appendCsvRow(
                "Generated At",
                report.generatedAt
            )

            appendLine()


            // =================================================
            // VENDOR DETAILS
            // =================================================

            appendCsvRow(
                "VENDOR DETAILS"
            )

            appendCsvRow(
                "Vendor",
                report.supplierName
            )

            appendCsvRow(
                "GSTIN",
                report.supplierGstin
            )

            appendCsvRow(
                "Address",
                report.supplierFullAddress
            )

            appendLine()


            // =================================================
            // INVOICE DETAILS
            // =================================================

            appendCsvRow(
                "INVOICE DETAILS"
            )

            appendCsvRow(
                "Supplier Invoice No.",
                report.invoiceNumber
            )

            appendCsvRow(
                "Invoice Date",
                report.invoiceDate
            )

            appendCsvRow(
                "Received Date",
                report.receivedDate
            )

            appendCsvRow(
                "Purchase Type",
                report.purchaseType
            )

            appendCsvRow(
                "Payment Type",
                report.paymentType
            )

            appendCsvRow(
                "Credit Days",
                report.creditDays
            )

            appendCsvRow(
                "Reference / Remarks",
                report.reference
            )

            appendLine()


            // =================================================
            // PRODUCT TABLE
            // =================================================

            appendCsvRow(
                "PRODUCT DETAILS"
            )

            appendCsvRow(
                "Product",
                "Model",
                "Category",
                "HSN",
                "Power",
                "Batch",
                "Quantity",
                "Rate",
                "Discount %",
                "GST %",
                "Gross",
                "Discount Amount",
                "Taxable",
                "GST Amount",
                "Total"
            )


            report.items.forEach { item ->


                // ---------------------------------------------
                // ITEM CALCULATION
                // ---------------------------------------------

                val gross =
                    item.quantity *
                            item.purchaseRate


                val discountAmount =
                    gross *
                            (
                                    item.discountPercent /
                                            100.0
                                    )


                val taxableAmount =
                    gross -
                            discountAmount


                val gstAmount =
                    taxableAmount *
                            (
                                    item.gstPercent /
                                            100.0
                                    )


                val total =
                    taxableAmount +
                            gstAmount


                // ---------------------------------------------
                // PRODUCT ROW
                // ---------------------------------------------

                appendCsvRow(
                    item.productName,
                    item.model,
                    item.category,
                    item.hsnCode,
                    item.power,
                    item.batchNumber,
                    item.quantity.toString(),
                    money(item.purchaseRate),
                    money(item.discountPercent),
                    money(item.gstPercent),
                    money(gross),
                    money(discountAmount),
                    money(taxableAmount),
                    money(gstAmount),
                    money(total)
                )


                // =============================================
                // SERIAL / EXPIRY DETAILS
                // =============================================

                if (
                    item.lensDetails.isNotEmpty()
                ) {

                    appendCsvRow(
                        "",
                        "SERIAL / EXPIRY DETAILS"
                    )


                    appendCsvRow(
                        "",
                        "Unit",
                        "Serial Number",
                        "Expiry"
                    )


                    item.lensDetails.forEachIndexed {
                            index,
                            lens ->

                        appendCsvRow(
                            "",
                            "Unit ${index + 1}",
                            lens.serialNumber,
                            formatExpiry(
                                lens.expiryDate
                            )
                        )
                    }
                }
            }


            appendLine()


            // =================================================
            // QUANTITY SUMMARY
            // =================================================

            appendCsvRow(
                "TOTAL QUANTITY",
                report.totalQuantity.toString()
            )

            appendCsvRow(
                "TOTAL SERIAL UNITS",
                report.totalSerialUnits.toString()
            )

            appendLine()


            // =================================================
            // BILL SUMMARY
            // =================================================

            appendCsvRow(
                "BILL SUMMARY"
            )

            appendCsvRow(
                "Gross Amount",
                money(report.grossAmount)
            )

            appendCsvRow(
                "Discount",
                money(report.discountAmount)
            )

            appendCsvRow(
                "Taxable Amount",
                money(report.taxableAmount)
            )

            appendCsvRow(
                "GST",
                money(report.taxAmount)
            )

            appendCsvRow(
                "Adjustment",
                money(report.adjustmentAmount)
            )

            appendCsvRow(
                "Round Off",
                money(report.roundOffAmount)
            )

            appendCsvRow(
                "NET AMOUNT",
                money(report.netAmount)
            )

            appendCsvRow(
                "Paid Amount",
                money(report.paidAmount)
            )

            appendCsvRow(
                "DUE AMOUNT",
                money(report.dueAmount)
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
    // CSV ESCAPE
    // =========================================================

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


    // =========================================================
    // EXPIRY FORMAT
    // =========================================================

    /**
     * Database format:
     *
     * MMYY
     *
     * Example:
     *
     * 1029 -> 10-29
     */
    private fun formatExpiry(
        expiryDate: String
    ): String {

        val clean =
            expiryDate
                .trim()
                .filter {
                    it.isDigit()
                }


        return if (
            clean.length == 4
        ) {

            "${clean.substring(0, 2)}-${clean.substring(2, 4)}"

        } else {

            expiryDate.trim()
        }
    }


    // =========================================================
    // MONEY
    // =========================================================

    private fun money(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.2f",
            value
        )
    }


    // =========================================================
    // SAFE FILE NAME
    // =========================================================

    private fun sanitizeFileName(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex(
                    """[\\/:*?"<>|]"""
                ),
                "_"
            )
            .take(
                60
            )
    }
}