package com.vilync.ophthalmicerp.core.document.engine

/**
 * Service for generating 2D QR Code symbols.
 */
interface QrCodeService {
    /**
     * Encodes a string into a QR Code symbol with optional error correction level.
     */
    fun encode(
        content: String, 
        errorCorrection: QrErrorCorrection = QrErrorCorrection.MEDIUM
    ): EncodingResult
}
