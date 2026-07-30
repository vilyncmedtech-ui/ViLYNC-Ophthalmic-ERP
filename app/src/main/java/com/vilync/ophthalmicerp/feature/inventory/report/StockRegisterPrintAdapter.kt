package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object StockRegisterPrintAdapter {

    // =========================================================
    // PRINT STOCK REGISTER
    // =========================================================

    /**
     * Generates the Stock Register PDF using the same verified
     * PDF exporter used by the Export PDF feature, and then
     * sends that PDF to Android's native Print Framework.
     *
     * Android Print Framework allows the user to:
     *
     * - Select an available printer
     * - Change print settings
     * - Select page range
     * - Save as PDF
     */
    fun print(
        context: Context,
        report: StockRegisterReport
    ): Result<Unit> {

        return runCatching {

            // -------------------------------------------------
            // 1. GENERATE VERIFIED PDF
            // -------------------------------------------------

            val pdfFile =
                StockRegisterPdfExporter
                    .export(
                        context = context,
                        report = report
                    )
                    .getOrThrow()


            // -------------------------------------------------
            // 2. GET ANDROID PRINT MANAGER
            // -------------------------------------------------

            val printManager =
                context.getSystemService(
                    Context.PRINT_SERVICE
                ) as PrintManager


            // -------------------------------------------------
            // 3. CREATE PRINT ADAPTER
            // -------------------------------------------------

            val adapter =
                PdfFilePrintDocumentAdapter(
                    pdfFile = pdfFile
                )


            // -------------------------------------------------
            // 4. START ANDROID PRINT UI
            // -------------------------------------------------

            printManager.print(
                "ViLYNC ERP - Stock Register",
                adapter,
                PrintAttributes
                    .Builder()
                    .setMediaSize(
                        PrintAttributes.MediaSize
                            .ISO_A4
                            .asLandscape()
                    )
                    .setColorMode(
                        PrintAttributes.COLOR_MODE_COLOR
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

            if (
                cancellationSignal.isCanceled
            ) {

                callback.onLayoutCancelled()

                return
            }


            if (
                !pdfFile.exists() ||
                pdfFile.length() <= 0L
            ) {

                callback.onLayoutFailed(
                    "Stock Register PDF could not be prepared."
                )

                return
            }


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


            callback.onLayoutFinished(
                documentInfo,
                oldAttributes != newAttributes
            )
        }


        // =====================================================
        // WRITE
        // =====================================================

        override fun onWrite(
            pages: Array<out android.print.PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {

            try {

                if (
                    cancellationSignal.isCanceled
                ) {

                    callback.onWriteCancelled()

                    return
                }


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

                            if (
                                cancellationSignal.isCanceled
                            ) {

                                callback.onWriteCancelled()

                                return
                            }


                            val bytesRead =
                                inputStream.read(
                                    buffer
                                )


                            if (
                                bytesRead <= 0
                            ) {
                                break
                            }


                            outputStream.write(
                                buffer,
                                0,
                                bytesRead
                            )
                        }


                        outputStream.flush()
                    }
                }


                callback.onWriteFinished(
                    arrayOf(
                        android.print.PageRange.ALL_PAGES
                    )
                )

            } catch (
                exception: Exception
            ) {

                callback.onWriteFailed(
                    exception.message
                        ?: "Unable to print Stock Register."
                )
            }
        }
    }
}