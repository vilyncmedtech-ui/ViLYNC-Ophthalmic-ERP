package com.vilync.ophthalmicerp.core.document.engine

/**
 * Aggregate result of a rendering engine process.
 */
data class RenderSession(
    val instructions: List<RenderInstruction>,
    val diagnostics: RenderDiagnostics
)

/**
 * Performance and complexity metrics for a rendering operation.
 */
data class RenderDiagnostics(
    val executionTimeMs: Long,
    val pageCount: Int,
    val instructionCount: Int
)
