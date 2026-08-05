package com.vilync.ophthalmicerp.core.reports.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign

/**
 * Master configuration for the Universal Report Engine.
 */
data class ReportSchema(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val columns: List<ReportColumn>,
    val filters: List<ReportFilterDescriptor>,
    val summaries: List<SummaryCardConfig>,
    val showCheckbox: Boolean = true
)

/**
 * Definition of a single data column in the report table.
 */
data class ReportColumn(
    val id: String,
    val displayName: String,
    val weight: Float = 1f,
    val textAlign: TextAlign = TextAlign.Start,
    val isVisible: Boolean = true,
    val isDefaultVisible: Boolean = true,
    val sortable: Boolean = true,
    val type: ColumnType = ColumnType.TEXT
)

enum class SortDirection {
    ASC, DESC
}

enum class ColumnType {
    TEXT,
    DATE,
    CURRENCY,
    NUMBER,
    STATUS
}

/**
 * Configuration for dynamic filter UI generation.
 */
data class ReportFilterDescriptor(
    val id: String,
    val label: String,
    val type: FilterType,
    val defaultValue: Any? = null,
    val options: List<String>? = null,
    val icon: ImageVector? = null,
    val section: String? = null,
    val weight: Float = 1f,
    val enableSearch: Boolean = false
)

enum class FilterType {
    DATE_RANGE,
    SEARCH_BAR,
    DROPDOWN,
    TOGGLE,
    AUTOCOMPLETE
}

/**
 * Configuration for top-level summary cards.
 */
data class SummaryCardConfig(
    val id: String,
    val label: String,
    val backgroundColor: Color,
    val valueColor: Color,
    val prefix: String = "",
    val suffix: String = "",
    val icon: ImageVector? = null
)

/**
 * Generic row data consumed by the Universal Engine.
 */
data class ReportRowData(
    val id: Long,
    val values: Map<String, Any?>
)

data class DateRange(
    val startDate: String,
    val endDate: String
)
