package com.vilync.ophthalmicerp.core.document.template

import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout

/**
 * Interface for serializing and deserializing TemplateLayout objects.
 * Keeps the domain model decoupled from specific JSON libraries.
 */
interface TemplateSerializer {
    /**
     * Converts a TemplateLayout to a storage-friendly string.
     */
    fun serialize(layout: TemplateLayout): String

    /**
     * Reconstructs a TemplateLayout from a stored string.
     */
    fun deserialize(data: String): TemplateLayout
}
