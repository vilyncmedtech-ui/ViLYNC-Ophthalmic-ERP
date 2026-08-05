package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vilync.ophthalmicerp.core.reports.presentation.components.MasterDetailReportLayout

@Composable
fun FinancialReportScreen(
    viewModel: FinancialReportViewModel,
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
