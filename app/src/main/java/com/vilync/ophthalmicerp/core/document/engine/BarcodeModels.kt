package com.vilync.ophthalmicerp.core.document.engine

/**
 * Supported barcode formats for the Document Designer.
 */
enum class BarcodeFormat {
    CODE128,
    CODE39,
    EAN13
}

/**
 * QR Code error correction levels.
 */
enum class QrErrorCorrection {
    LOW,
    MEDIUM,
    QUARTILE,
    HIGH
}

/**
 * Renderer-neutral representation of an encoded symbol (Barcode or QR).
 * 
 * This model abstracts the internal encoding (like a bit matrix) from the renderers.
 */
data class EncodedSymbol(
    val width: Int,
    val height: Int,
    val bits: BooleanArray, // Flattened bit matrix for performance
    val metadata: Map<String, String> = emptyMap()
) {
    fun getBit(x: Int, y: Int): Boolean {
        return bits[y * width + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedSymbol) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!bits.contentEquals(other.bits)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bits.contentHashCode()
        return result
    }
}

/**
 * Outcome of an encoding operation.
 */
sealed class EncodingResult {
    data class Success(val symbol: EncodedSymbol) : EncodingResult()
    data class Failure(val errorMessage: String) : EncodingResult()
}
