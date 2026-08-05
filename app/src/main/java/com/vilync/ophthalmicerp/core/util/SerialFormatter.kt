package com.vilync.ophthalmicerp.core.util

/**
 * Centralized Serial Formatting Engine.
 */
object SerialFormatter {

    /**
     * Formats a raw numeric serial with its prefix and standard padding.
     * 
     * Example:
     * Input: "125", Prefix: "LMDE"
     * Output: "LMDE000125"
     */
    fun format(prefix: String, serial: String, padding: Int = 6): String {
        val numericPart = serial.filter { it.isDigit() }
        if (numericPart.isEmpty()) return serial
        
        val padded = numericPart.padStart(padding, '0')
        return "$prefix$padded"
    }
}
