package com.vilync.ophthalmicerp.feature.designer.data

import androidx.room.TypeConverter
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType
import com.vilync.ophthalmicerp.feature.designer.domain.model.TemplateStatus

/**
 * Type converters for Document Designer enums.
 */
class DesignerTypeConverters {
    @TypeConverter
    fun fromDocumentType(value: DesignerDocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DesignerDocumentType = DesignerDocumentType.valueOf(value)

    @TypeConverter
    fun fromTemplateStatus(value: TemplateStatus): String = value.name

    @TypeConverter
    fun toTemplateStatus(value: String): TemplateStatus = TemplateStatus.valueOf(value)
}
