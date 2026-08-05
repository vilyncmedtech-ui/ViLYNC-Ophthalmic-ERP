package com.vilync.ophthalmicerp.feature.sales.reports.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.sales.reports.domain.SalesReportType
import com.vilync.ophthalmicerp.feature.sales.reports.presentation.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    viewModel: SalesReportViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val datePickerState = rememberDatePickerState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Colours from StockRegisterScreen
    val backgroundColor = Color(0xFFF5F7FF)
    val headerBlue = Color(0xFFE4EEFF)
    val summaryBlue = Color(0xFFDDEBFF)
    val summaryGreen = Color(0xFFDDF5E8)
    val textPrimary = Color(0xFF252A33)
    val textSecondary = Color(0xFF6D7480)
    val blueAccent = Color(0xFF345FA8)

    if (showStartPicker || showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { 
                showStartPicker = false
                showEndPicker = false
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val date = sdf.format(Date(ms))
                        if (showStartPicker) {
                            viewModel.updateDateRange(date, uiState.endDate)
                        } else {
                            viewModel.updateDateRange(uiState.startDate, date)
                        }
                    }
                    showStartPicker = false
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header (Premium Card Style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(48.dp).clickable { onBack() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = headerBlue)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "←", fontSize = 27.sp, fontWeight = FontWeight.Medium, color = blueAccent)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sales Report", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = uiState.reportTitle, fontSize = 12.sp, color = textSecondary)
                }

                IconButton(onClick = { /* TODO: Export */ }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = blueAccent)
                }

                Card(
                    modifier = Modifier.clickable { onDashboard() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = headerBlue)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Dashboard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = blueAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Count",
                value = uiState.totalCount.toString(),
                backgroundColor = summaryGreen,
                valueColor = Color(0xFF168457)
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Amount",
                value = "₹${"%.2f".format(uiState.totalAmount)}",
                backgroundColor = summaryBlue,
                valueColor = blueAccent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Section
        FilterDropdown(
            selectedType = uiState.selectedReportType,
            onTypeSelected = viewModel::updateReportType
        )

        DatePickerRow(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onStartDateClick = { showStartPicker = true },
            onEndDateClick = { showEndPicker = true },
            onApplyClick = viewModel::loadReport
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Report List
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                val rowsExist = if (uiState.selectedReportType == SalesReportType.SUMMARY) {
                    uiState.productSummary.isNotEmpty()
                } else {
                    uiState.salesList.isNotEmpty()
                }

                if (!rowsExist) {
                    Text(
                        text = "No records found for the selected period",
                        modifier = Modifier.align(Alignment.Center),
                        color = textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        if (uiState.selectedReportType == SalesReportType.SUMMARY) {
                            items(uiState.productSummary) { summary ->
                                ProductSummaryRow(summary)
                            }
                        } else {
                            items(uiState.salesList) { detail ->
                                SalesTransactionRow(detail)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    backgroundColor: Color,
    valueColor: Color
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF626B78), maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
