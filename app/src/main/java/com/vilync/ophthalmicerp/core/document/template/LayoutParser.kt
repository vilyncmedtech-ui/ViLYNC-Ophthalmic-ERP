package com.vilync.ophthalmicerp.core.document.template

/**
 * Responsible for converting a LoadedTemplate (raw data) into a TemplateLayout (domain model).
 */
interface LayoutParser {
    /**
     * Parses the raw data into a structured layout. 
     * Independent of specific JSON libraries.
     */
    fun parse(loaded: LoadedTemplate): ParseResult
}
