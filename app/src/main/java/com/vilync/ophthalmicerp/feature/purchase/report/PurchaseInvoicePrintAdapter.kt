package com.vilync.ophthalmicerp.feature.purchase.report

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


object PurchaseInvoicePrintAdapter {


    // =========================================================
    // PRINT PURCHASE INVOICE
    // =========================================================

    /**
     * Generates the Purchase Invoice PDF using the same
     * verified PDF exporter used by Export PDF.
     *
     * The generated PDF is then sent to Android's
     * native Print Framework.
     *
     * User can:
     *
     * - Select available printer
     * - Change printer settings
     * - Select page range
     * - Save as PDF
     */
    fun print(
        context: Context,
        report: PurchaseInvoiceReport
    ): Result<Unit> {

        return runCatching {


            // -------------------------------------------------
            // 1. GENERATE PDF
            // -------------------------------------------------

            val pdfFile =
                PurchaseInvoicePdfExporter
                    .export(
                        context = context,
                        report = report
                    )
                    .getOrThrow()


            // -------------------------------------------------
            // 2. PRINT MANAGER
            // -------------------------------------------------

            val printManager =
                context.getSystemService(
                    Context.PRINT_SERVICE
                ) as PrintManager


            // -------------------------------------------------
            // 3. PRINT DOCUMENT ADAPTER
            // -------------------------------------------------

            val adapter =
                PdfFilePrintDocumentAdapter(
                    pdfFile = pdfFile
                )


            // -------------------------------------------------
            // 4. PRINT JOB NAME
            // -------------------------------------------------

            val invoiceNumber =
                report.invoiceNumber
                    .trim()
                    .ifBlank {
                        "Purchase Invoice"
                    }


            val printJobName =
                "ViLYNC ERP - $invoiceNumber"


            // -------------------------------------------------
            // 5. START ANDROID PRINT UI
            // -------------------------------------------------

            printManager.print(
                printJobName,
                adapter,
                PrintAttributes
                    .Builder()
                    .setMediaSize(
                        PrintAttributes
                            .MediaSize
                            .ISO_A4
                            .asLandscape()
                    )
                    .setColorMode(
                        PrintAttributes
                            .COLOR_MODE_COLOR
                    )
                    .build()
            )
        }
    }


    // =========================================================
    // PDF FILE PRINT DOCUMENT ADAPTER
    // =========================================================

    private class PdfFilePrintDocumentAdapter(

        private val pdfFile: File

    ) : PrintDocumentAdapter() {


        // =====================================================
        // LAYOUT
        // =====================================================

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {


            // -------------------------------------------------
            // CANCELLED
            // -------------------------------------------------

            if (
                cancellationSignal.isCanceled
            ) {

                callback.onLayoutCancelled()

                return
            }


            // -------------------------------------------------
            // VERIFY PDF
            // -------------------------------------------------

            if (
                !pdfFile.exists() ||
                pdfFile.length() <= 0L
            ) {

                callback.onLayoutFailed(
                    "Purchase Invoice PDF could not be prepared."
                )

                return
            }


            // -------------------------------------------------
            // DOCUMENT INFORMATION
            // -------------------------------------------------

            val documentInfo =
                PrintDocumentInfo
                    .Builder(
                        pdfFile.name
                    )
                    .setContentType(
                        PrintDocumentInfo
                            .CONTENT_TYPE_DOCUMENT
                    )
                    .setPageCount(
                        PrintDocumentInfo
                            .PAGE_COUNT_UNKNOWN
                    )
                    .build()


            // -------------------------------------------------
            // FINISH LAYOUT
            // -------------------------------------------------

            callback.onLayoutFinished(
                documentInfo,
                oldAttributes != newAttributes
            )
        }


        // =====================================================
        // WRITE PDF TO PRINT DESTINATION
        // =====================================================

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {

            try {


                // -------------------------------------------------
                // CANCELLED BEFORE WRITE
                // -------------------------------------------------

                if (
                    cancellationSignal.isCanceled
                ) {

                    callback.onWriteCancelled()

                    return
                }


                // -------------------------------------------------
                // COPY GENERATED PDF
                // -------------------------------------------------

                FileInputStream(
                    pdfFile
                ).use { inputStream ->


                    FileOutputStream(
                        destination.fileDescriptor
                    ).use { outputStream ->


                        val buffer =
                            ByteArray(
                                DEFAULT_BUFFER_SIZE
                            )


                        while (true) {


                            // -------------------------------------
                            // CANCELLED DURING WRITE
                            // -------------------------------------

                            if (
                                cancellationSignal.isCanceled
                            ) {

                                callback.onWriteCancelled()

                                return
                            }


                            // -------------------------------------
                            // READ
                            // -------------------------------------

                            val bytesRead =
                                inputStream.read(
                                    buffer
                                )


                            if (
                                bytesRead <= 0
                            ) {

                                break
                            }


                            // -------------------------------------
                            // WRITE
                            // -------------------------------------

                            outputStream.write(
                                buffer,
                                0,
                                bytesRead
                            )
                        }


                        outputStream.flush()
                    }
                }


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                callback.onWriteFinished(
                    arrayOf(
                        PageRange.ALL_PAGES
                    )
                )


            } catch (
                exception: Exception
            ) {


                // -------------------------------------------------
                // FAILED
                // -------------------------------------------------

                callback.onWriteFailed(
                    exception.message
                        ?: "Unable to print Purchase Invoice."
                )
            }
        }
    }
}