package com.vilync.ophthalmicerp.core.document.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.style.DocumentColor
import com.vilync.ophthalmicerp.core.document.domain.style.FontStyle
import com.vilync.ophthalmicerp.core.document.domain.style.HorizontalAlignment
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation of the PdfRenderer.
 * Translates RenderInstructions into PdfDocument operations.
 */
class DefaultPdfRenderer(
    private val context: Context
) : PdfRenderer {

    private val mmToPoint = 72.0 / 25.4
    private val imageCache = mutableMapOf<String, Bitmap>()

    override suspend fun render(request: DocumentRequest, context: RenderContext): RenderResult {
        return PdfRenderResult(
            isSuccess = false,
            errorCode = "ERR_NOT_IMPLEMENTED",
            message = "Use specialized renderSession method or implement pipeline integration."
        )
    }

    /**
     * Physical rendering of a session into a PDF file.
     */
    suspend fun renderSession(session: RenderSession, outputFile: File): PdfRenderResult {
        val startTime = System.currentTimeMillis()
        val document = PdfDocument()
        val warnings = mutableListOf<String>()

        try {
            val pages = session.instructions.groupBy { it.pageIndex }
            val sortedPageIndices = pages.keys.sorted()

            sortedPageIndices.forEach { pageIndex ->
                val pageInstructions = pages[pageIndex] ?: return@forEach
                
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                pageInstructions.forEach { instruction ->
                    CanvasInstructionDispatcher.dispatch(canvas, instruction, warnings)
                }

                document.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }

            val duration = System.currentTimeMillis() - startTime
            return PdfRenderResult(
                isSuccess = true,
                pdfFile = outputFile,
                pageCount = sortedPageIndices.size,
                renderTimeMs = duration,
                warnings = warnings
            )

        } catch (e: Exception) {
            return PdfRenderResult(
                isSuccess = false,
                errorCode = "ERR_PDF_GEN",
                message = "PDF generation failed: ${e.message}"
            )
        } finally {
            document.close()
            CanvasInstructionDispatcher.clearCache()
        }
    }
}
