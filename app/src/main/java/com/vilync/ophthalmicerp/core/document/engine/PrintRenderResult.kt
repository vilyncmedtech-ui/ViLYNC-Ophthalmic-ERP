package com.vilync.ophthalmicerp.core.document.engine

import android.print.PrintDocumentAdapter

/**
 * specialized result for Android Print rendering operations.
 */
data class PrintRenderResult(
    override val isSuccess: Boolean,
    override val errorCode: String? = null,
    override val message: String? = null,
    override val metadata: Map<String, String> = emptyMap(),
    val printAdapter: PrintDocumentAdapter? = null,
    val instructionCount: Int = 0,
    val renderTimeMs: Long = 0,
    val warnings: List<String> = emptyList()
) : RenderResult
