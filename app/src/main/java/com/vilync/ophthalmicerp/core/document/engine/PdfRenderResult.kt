package com.vilync.ophthalmicerp.core.document.engine

import java.io.File

/**
 * specialized result for PDF rendering operations.
 */
data class PdfRenderResult(
    override val isSuccess: Boolean,
    override val errorCode: String? = null,
    override val message: String? = null,
    override val metadata: Map<String, String> = emptyMap(),
    val pdfFile: File? = null,
    val pageCount: Int = 0,
    val renderTimeMs: Long = 0,
    val warnings: List<String> = emptyList()
) : RenderResult
