package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.database.AppDatabase
import java.util.Calendar

enum class DocumentType(val defaultPrefix: String) {
    INVOICE("VMINV"),
    CHALLAN("VMCH"),
    CREDIT_NOTE("VMCN"),
    DEBIT_NOTE("VMDN"),
    PROFORMA("VMPI"),
    SAMPLE_ISSUE("VMSN")
}

/**
 * Centralized Document Numbering Engine.
 * 
 * Provides atomic, sequence-based numbering with support for:
 * - Independent series per document type and financial year
 * - Automatic sequence reset on FY change
 * - Atomic database-level locking
 * - Flexible formatting for future configuration
 */
class DocumentNumberingRepository(
    private val database: AppDatabase
) {

    /**
     * Generates and commits the next sequence number for the given document.
     * 
     * IMPORTANT: This method increments the last used sequence in the database.
     * It must only be called when the document is being Saved or Posted.
     */
    suspend fun getNextDocumentNumber(type: DocumentType, financialYearStart: Int): String {
        val dao = database.documentNumberingDao()
        
        // Atomic fetch & increment inside a database transaction
        val sequence = dao.getNextSequenceNumber(
            documentType = type.name,
            financialYearStart = financialYearStart,
            defaultPrefix = type.defaultPrefix
        )
        
        // Fetch the updated series to respect prefix/padding configuration
        val series = dao.getSeries(type.name, financialYearStart)
            ?: throw IllegalStateException("Failed to retrieve series after generation.")
            
        return formatDocumentNumber(
            prefix = series.prefix,
            financialYearStart = financialYearStart,
            sequence = sequence,
            padding = series.padding
        )
    }

    /**
     * Formats the document number string.
     * 
     * Architecture allows for future override of this logic via Company Settings.
     */
    private fun formatDocumentNumber(
        prefix: String,
        financialYearStart: Int,
        sequence: Int,
        padding: Int
    ): String {
        val fyCode = formatFyCode(financialYearStart)
        val paddedSequence = sequence.toString().padStart(padding, '0')
        
        return "$prefix/$fyCode/$paddedSequence"
    }

    private fun formatFyCode(startYear: Int): String {
        val startYearShort = startYear % 100
        val endYearShort = (startYear + 1) % 100
        return "%02d%02d".format(startYearShort, endYearShort)
    }

    /**
     * Helper to derive the Financial Year Start for a given date.
     */
    fun getFinancialYearStart(calendar: Calendar): Int {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) // 0-indexed (April = 3)
        return if (month >= Calendar.APRIL) year else year - 1
    }
}
