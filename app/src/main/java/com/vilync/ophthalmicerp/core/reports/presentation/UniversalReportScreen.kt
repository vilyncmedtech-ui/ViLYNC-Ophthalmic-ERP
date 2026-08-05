package com.vilync.ophthalmicerp.core.reports.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.vilync.ophthalmicerp.core.reports.presentation.components.MasterDetailReportLayout

@Composable
fun UniversalReportScreen(
    viewModel: UniversalReportViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MasterDetailReportLayout(
        uiState = uiState,
        schema = viewModel.schema,
        dynamicOptions = uiState.dynamicOptions,
        onFilterChange = { id, value -> viewModel.updateFilter(id, value) },
        onApply = { viewModel.loadData() },
        onReset = { viewModel.resetFilters() },
        onBack = onBack,
        onDashboard = onDashboard
    )
}
