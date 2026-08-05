package com.vilync.ophthalmicerp.core.document.engine

/**
 * Service for generating 1D Barcode symbols.
 */
interface BarcodeService {
    /**
     * Encodes a string into a barcode symbol of the specified format.
     */
    fun encode(content: String, format: BarcodeFormat): EncodingResult
}
