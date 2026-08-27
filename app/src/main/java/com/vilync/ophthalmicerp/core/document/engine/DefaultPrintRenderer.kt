package com.vilync.ophthalmicerp.core.document.engine

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import java.io.FileOutputStream

/**
 * Android implementation of the PrintRenderer.
 * Wraps RenderInstructions into a PrintDocumentAdapter for the system print spooler.
 */
class DefaultPrintRenderer(
    private val context: Context
) : PrintRenderer {

    override suspend fun render(request: DocumentRequest, context: RenderContext): RenderResult {
        return PrintRenderResult(
            isSuccess = false,
            errorCode = "ERR_NOT_IMPLEMENTED",
            message = "Use createPrintAdapter to generate a PrintDocumentAdapter."
        )
    }

    /**
     * Creates an adapter for the system printing flow using the provided rendering context.
     */
    fun createPrintAdapter(session: RenderSession, context: RenderContext, documentTitle: String): PrintRenderResult {
        val adapter = DesignerPrintAdapter(session, context, documentTitle)
        return PrintRenderResult(
            isSuccess = true,
            printAdapter = adapter,
            instructionCount = session.instructions.size
        )
    }

    private inner class DesignerPrintAdapter(
        private val session: RenderSession,
        private val context: RenderContext,
        private val title: String
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }

            val pages = session.instructions.groupBy { it.pageIndex }.size
            val info = PrintDocumentInfo.Builder("$title.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pages)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }

            val document = PdfDocument()
            val warnings = mutableListOf<String>()

            try {
                val groupedInstructions = session.instructions.groupBy { it.pageIndex }
                val sortedPageIndices = groupedInstructions.keys.sorted()

                // Resolve physical size in points (72 DPI)
                val pointsSize = context.pageSize.convertTo(MeasurementUnit.POINT)

                sortedPageIndices.forEach { pageIndex ->
                    val pageInstructions = groupedInstructions[pageIndex] ?: return@forEach
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pointsSize.width.toInt(),
                        pointsSize.height.toInt(),
                        pageIndex + 1
                    ).create()
                    
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    pageInstructions.forEach { instruction ->
                        CanvasInstructionDispatcher.dispatch(canvas, instruction, warnings)
                    }

                    document.finishPage(page)
                }

                FileOutputStream(destination.fileDescriptor).use { output ->
                    document.writeTo(output)
                }

                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                document.close()
                CanvasInstructionDispatcher.clearCache()
            }
        }
    }
}
