package com.vilync.ophthalmicerp.core.document.template

import com.google.gson.Gson
import com.vilync.ophthalmicerp.core.document.domain.TemplateLayout

/**
 * GSON-backed implementation of the TemplateSerializer.
 */
class GsonTemplateSerializer(
    private val gson: Gson = Gson()
) : TemplateSerializer {

    override fun serialize(layout: TemplateLayout): String {
        return gson.toJson(layout)
    }

    override fun deserialize(data: String): TemplateLayout {
        return try {
            gson.fromJson(data, TemplateLayout::class.java)
        } catch (e: Exception) {
            android.util.Log.e("GsonTemplateSerializer", "Failed to deserialize layout", e)
            TemplateLayout() // Fallback to empty layout
        }
    }
}
