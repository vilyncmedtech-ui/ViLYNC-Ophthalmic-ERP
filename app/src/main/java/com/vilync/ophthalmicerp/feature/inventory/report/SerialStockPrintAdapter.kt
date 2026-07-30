package com.vilync.ophthalmicerp.feature.inventory.report

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.vilync.ophthalmicerp.data.dao.SerialStockRow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


object SerialStockPrintAdapter {


    // =========================================================
    // PRINT SERIAL STOCK REGISTER
    // =========================================================

    /**
     * Generates the Serial Stock Register PDF using the same
     * verified SerialStockPdfExporter used by the PDF export
     * feature.
     *
     * The generated PDF is then passed to Android's native
     * Print Framework.
     *
     * Android Print Framework allows the user to:
     *
     * - Select an available printer
     * - Change printer settings
     * - Select page range
     * - Save as PDF
     *
     * The report is printed in A4 Landscape because the
     * Serial Stock Register contains multiple inventory
     * columns and may contain 1000+ individual serial units.
     */
    fun print(
        context: Context,
        rows: List<SerialStockRow>,
        searchQuery: String = "",
        selectedStatus: String = "ALL"
    ): Result<Unit> {

        return runCatching {


            // =================================================
            // 1. VALIDATE DATA
            // =================================================

            require(
                rows.isNotEmpty()
            ) {
                "No Serial Stock data is available to print."
            }


            // =================================================
            // 2. GENERATE VERIFIED SERIAL STOCK PDF
            // =================================================

            val pdfFile =
                SerialStockPdfExporter
                    .export(
                        context = context,
                        rows = rows,
                        searchQuery = searchQuery,
                        selectedStatus = selectedStatus
                    )
                    .getOrThrow()


            // =================================================
            // 3. VALIDATE GENERATED PDF
            // =================================================

            require(
                pdfFile.exists() &&
                        pdfFile.length() > 0L
            ) {
                "Serial Stock PDF could not be prepared."
            }


            // =================================================
            // 4. GET ANDROID PRINT MANAGER
            // =================================================

            val printManager =
                context.getSystemService(
                    Context.PRINT_SERVICE
                ) as PrintManager


            // =================================================
            // 5. CREATE PRINT DOCUMENT ADAPTER
            // =================================================

            val adapter =
                PdfFilePrintDocumentAdapter(
                    pdfFile = pdfFile
                )


            // =================================================
            // 6. OPEN ANDROID PRINT UI
            // =================================================

            printManager.print(

                "ViLYNC ERP - Serial Stock Register",

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

    /**
     * Sends an already-generated PDF file to Android's
     * Print Framework.
     *
     * This keeps PDF Export and Print based on exactly the
     * same Serial Stock report.
     */
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
            // PRINT CANCELLED
            // -------------------------------------------------

            if (
                cancellationSignal.isCanceled
            ) {

                callback.onLayoutCancelled()

                return
            }


            // -------------------------------------------------
            // PDF VALIDATION
            // -------------------------------------------------

            if (
                !pdfFile.exists() ||
                pdfFile.length() <= 0L
            ) {

                callback.onLayoutFailed(
                    "Serial Stock Register PDF could not be prepared."
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
            // LAYOUT COMPLETE
            // -------------------------------------------------

            callback.onLayoutFinished(
                documentInfo,
                oldAttributes != newAttributes
            )
        }


        // =====================================================
        // WRITE
        // =====================================================

        override fun onWrite(

            pages: Array<out PageRange>,

            destination: ParcelFileDescriptor,

            cancellationSignal: CancellationSignal,

            callback: WriteResultCallback

        ) {

            try {


                // -------------------------------------------------
                // CHECK CANCELLATION BEFORE STARTING
                // -------------------------------------------------

                if (
                    cancellationSignal.isCanceled
                ) {

                    callback.onWriteCancelled()

                    return
                }


                // -------------------------------------------------
                // COPY GENERATED PDF TO PRINT DESTINATION
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
                            // CHECK CANCELLATION DURING COPY
                            // -------------------------------------

                            if (
                                cancellationSignal.isCanceled
                            ) {

                                callback.onWriteCancelled()

                                return
                            }


                            // -------------------------------------
                            // READ PDF DATA
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
                            // WRITE PDF DATA
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
                // PRINT DATA READY
                // -------------------------------------------------

                callback.onWriteFinished(
                    arrayOf(
                        PageRange.ALL_PAGES
                    )
                )


            } catch (
                exception: Exception
            ) {


                callback.onWriteFailed(
                    exception.message
                        ?: "Unable to print Serial Stock Register."
                )
            }
        }
    }
}