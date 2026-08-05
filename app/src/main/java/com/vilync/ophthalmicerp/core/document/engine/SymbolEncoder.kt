package com.vilync.ophthalmicerp.core.document.engine

import android.util.Log

/**
 * Responsible for processing a RenderSession and encoding any pending Barcode or QR instructions.
 */
interface SymbolEncoder {
    /**
     * Resolves all encoded symbols in the session's instruction set.
     */
    fun encode(session: RenderSession): RenderSession
}

/**
 * Production implementation of the SymbolEncoder.
 * Integrates SymbolCache for performance optimization.
 */
class DefaultSymbolEncoder(
    private val barcodeService: BarcodeService,
    private val qrCodeService: QrCodeService
) : SymbolEncoder {

    companion object {
        private const val TAG = "SYMBOL_ENCODER"
    }

    override fun encode(session: RenderSession): RenderSession {
        val startTime = System.currentTimeMillis()
        
        val encodedInstructions = session.instructions.map { inst ->
            when (inst) {
                is DrawBarcodeInstruction -> {
                    if (inst.encodedSymbol != null) inst
                    else {
                        val cached = SymbolCache.get("BARCODE", inst.format.name, inst.content)
                        if (cached != null) {
                            inst.copy(encodedSymbol = cached)
                        } else {
                            val result = barcodeService.encode(inst.content, inst.format)
                            if (result is EncodingResult.Success) {
                                SymbolCache.put("BARCODE", inst.format.name, inst.content, result.symbol)
                                inst.copy(encodedSymbol = result.symbol)
                            } else {
                                Log.e(TAG, "Barcode encoding failed for ${inst.content}: ${(result as? EncodingResult.Failure)?.errorMessage}")
                                inst
                            }
                        }
                    }
                }
                is DrawQrCodeInstruction -> {
                    if (inst.encodedSymbol != null) inst
                    else {
                        val cached = SymbolCache.get("QR", "DEFAULT", inst.content)
                        if (cached != null) {
                            inst.copy(encodedSymbol = cached)
                        } else {
                            val result = qrCodeService.encode(inst.content)
                            if (result is EncodingResult.Success) {
                                SymbolCache.put("QR", "DEFAULT", inst.content, result.symbol)
                                inst.copy(encodedSymbol = result.symbol)
                            } else {
                                Log.e(TAG, "QR encoding failed for ${inst.content}: ${(result as? EncodingResult.Failure)?.errorMessage}")
                                inst
                            }
                        }
                    }
                }
                else -> inst
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val diagnostics = session.diagnostics.copy(
            executionTimeMs = session.diagnostics.executionTimeMs + duration
        )
        
        Log.d(TAG, "Encoding session complete. Duration: ${duration}ms. ${SymbolCache.getMetrics()}")
        
        return session.copy(instructions = encodedInstructions, diagnostics = diagnostics)
    }
}
