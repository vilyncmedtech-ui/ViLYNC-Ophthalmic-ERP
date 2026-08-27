package com.vilync.ophthalmicerp.core.document.engine

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType

/**
 * Supported statuses for dynamic field resolution.
 */
enum class BindingStatus {
    RESOLVED,
    MISSING,
    EMPTY,
    UNSUPPORTED,
    INVALID_PAYLOAD,
    ERROR
}

/**
 * Strongly typed, immutable representation of a dynamic field value.
 */
sealed class BindingValue {
    data class Text(val value: String) : BindingValue()
    data class Number(val value: Double, val format: String? = null) : BindingValue()
    data class Date(val timestamp: Long, val format: String? = null) : BindingValue()
    data class Image(val uri: String? = null, val path: String? = null) : BindingValue()
    data class Barcode(val content: String, val format: BarcodeFormat = BarcodeFormat.CODE128) : BindingValue()
    data class QrCode(val content: String) : BindingValue()
    data class Table(
        val headers: List<String>,
        val rows: List<List<BindingValue>>
    ) : BindingValue()
}

/**
 * Encapsulates the context required to resolve dynamic fields.
 */
data class DocumentBindingContext(
    val entityId: Long,
    val documentType: DesignerDocumentType,
    val extraParameters: Map<String, Any?> = emptyMap()
)

/**
 * Captures a single resolution attempt by a provider.
 */
data class ProviderResolutionTrace(
    val providerName: String,
    val totalFieldsRequested: Int,
    val resolvedCount: Int,
    val missingCount: Int,
    val unsupportedCount: Int,
    val executionTimeMs: Long
)

/**
 * Final diagnostic report for the binding operation.
 */
data class BindingReport(
    val traces: List<ProviderResolutionTrace>,
    val totalResolved: Int,
    val totalMissing: Int,
    val executionTimeMs: Long
)

/**
 * The final aggregate result of the data binding pipeline.
 */
data class BindingResult(
    val layout: RenderLayout, // This is the 'BoundRenderLayout'
    val report: BindingReport,
    val isSuccess: Boolean
)
