package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vilync.ophthalmicerp.data.dao.SerialStockRow
import com.vilync.ophthalmicerp.feature.inventory.report.SerialStockExcelExporter
import com.vilync.ophthalmicerp.feature.inventory.report.SerialStockPdfExporter
import com.vilync.ophthalmicerp.feature.inventory.report.SerialStockPrintAdapter


// =============================================================
// ViLYNC PREMIUM COMPACT PURCHASE UI
// PASTEL ERP COLOUR SYSTEM — NO GOLD
// =============================================================

private val SerialBackground =
    Color(0xFFF7F9FD)

private val SerialNavy =
    Color(0xFF071B33)

private val SerialNavyLight =
    Color(0xFF18275F)

// ViLYNC Premium Compact Purchase UI accent.
// Gold intentionally removed; this soft professional blue is used
// anywhere the previous screen used the old gold accent.
private val SerialGold =
    Color(0xFF4169A8)

private val SerialWhite =
    Color(0xFFFFFFFF)

private val SerialText =
    Color(0xFF172033)

private val SerialMuted =
    Color(0xFF747B89)

private val SerialBorder =
    Color(0xFFDDE4EE)

private val SerialGreenBackground =
    Color(0xFFE3F6EC)

private val SerialGreen =
    Color(0xFF16794D)

private val SerialBlueBackground =
    Color(0xFFE5EEFF)

private val SerialBlue =
    Color(0xFF315FA8)

private val SerialOrangeBackground =
    Color(0xFFFFEEDC)

private val SerialOrange =
    Color(0xFFA85B16)

private val SerialPurpleBackground =
    Color(0xFFEFE5FF)

private val SerialPurple =
    Color(0xFF7046B5)

private val SerialRedBackground =
    Color(0xFFFFE5E5)

private val SerialRed =
    Color(0xFFB63A3A)


// =============================================================
// SCREEN
// =============================================================

@Composable
fun SerialStockRegisterScreen(

    viewModel: SerialStockViewModel,

    onBack: () -> Unit,

    onDashboard: () -> Unit = {},

    onSerialClick: (Long) -> Unit = {}

) {

    val uiState by
    viewModel
        .uiState
        .collectAsStateWithLifecycle()


    val context =
        LocalContext.current


    var showExportDialog by
    remember {
        mutableStateOf(false)
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    SerialBackground
                )
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        SerialStockHeader(
            onBack = onBack,
            onDashboard = onDashboard,
            onExportClick = {
                showExportDialog = true
            }
        )


        // =====================================================
        // BODY
        // =====================================================

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize(),

            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 24.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    9.dp
                )
        ) {


            // =================================================
            // SUMMARY
            // =================================================

            item {

                SerialSummarySection(
                    totalUnits =
                        uiState.totalUnits,
                    inStockUnits =
                        uiState.inStockUnits,
                    issuedUnits =
                        uiState.issuedUnits
                )
            }


            // =================================================
            // SEARCH
            // =================================================

            item {

                SerialSearchBox(
                    query =
                        uiState.searchQuery,

                    onQueryChanged = {
                        viewModel
                            .onSearchQueryChanged(
                                it
                            )
                    },

                    onClear = {
                        viewModel.clearSearch()
                    }
                )
            }


            // =================================================
            // STATUS FILTER
            // =================================================

            item {

                SerialStatusFilters(
                    selectedStatus =
                        uiState.selectedStatus,

                    onStatusSelected = {
                        viewModel
                            .onStatusSelected(
                                it
                            )
                    }
                )
            }


            // =================================================
            // REGISTER TITLE
            // =================================================

            item {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 2.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(
                            text =
                                "Serial Stock",

                            fontSize =
                                19.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                SerialNavy
                        )

                        Text(
                            text =
                                "Tap any serial to view movement history",

                            fontSize =
                                10.sp,

                            color =
                                SerialMuted
                        )
                    }


                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color =
                                        SerialBlueBackground,
                                    shape =
                                        RoundedCornerShape(
                                            50.dp
                                        )
                                )
                                .padding(
                                    horizontal = 11.dp,
                                    vertical = 6.dp
                                )
                    ) {

                        Text(
                            text =
                                "${uiState.serialStock.size} Units",

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                SerialBlue
                        )
                    }
                }
            }


            // =================================================
            // LOADING
            // =================================================

            if (uiState.isLoading) {

                item {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    180.dp
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color =
                                SerialGold
                        )
                    }
                }
            }


            // =================================================
            // ERROR
            // =================================================

            else if (
                uiState.errorMessage != null
            ) {

                item {

                    SerialErrorCard(
                        message =
                            uiState.errorMessage
                                ?: "Unable to load Serial Stock.",

                        onRetry = {
                            viewModel.refresh()
                        }
                    )
                }
            }


            // =================================================
            // EMPTY
            // =================================================

            else if (
                uiState.serialStock.isEmpty()
            ) {

                item {

                    SerialEmptyState(
                        hasFilters =
                            uiState.searchQuery
                                .isNotBlank() ||
                                    uiState.selectedStatus !=
                                    "ALL",

                        onReset = {
                            viewModel.resetFilters()
                        }
                    )
                }
            }


            // =================================================
            // REGISTER
            // =================================================

            else {

                item {

                    SerialRegisterHeader()
                }


                items(
                    items =
                        uiState.serialStock,

                    key = {
                        it.inventoryUnitId
                    }
                ) { row ->

                    SerialRegisterRow(
                        row = row,

                        onClick = {
                            onSerialClick(
                                row.inventoryUnitId
                            )
                        }
                    )
                }
            }
        }
    }


    if (showExportDialog) {

        SerialStockExportDialog(
            hasData = uiState.serialStock.isNotEmpty(),
            onDismiss = {
                showExportDialog = false
            },
            onExcel = {
                showExportDialog = false
                SerialStockExcelExporter.exportAndShare(
                    context = context,
                    rows = uiState.serialStock,
                    searchQuery = uiState.searchQuery,
                    selectedStatus = uiState.selectedStatus
                )
            },
            onPdf = {
                showExportDialog = false
                SerialStockPdfExporter.exportAndShare(
                    context = context,
                    rows = uiState.serialStock,
                    searchQuery = uiState.searchQuery,
                    selectedStatus = uiState.selectedStatus
                )
            },
            onPrint = {
                showExportDialog = false
                SerialStockPrintAdapter.print(
                    context = context,
                    rows = uiState.serialStock,
                    searchQuery = uiState.searchQuery,
                    selectedStatus = uiState.selectedStatus
                )
            }
        )
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun SerialStockHeader(

    onBack: () -> Unit,

    onDashboard: () -> Unit,

    onExportClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SerialWhite
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    3.dp
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = SerialBorder
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            40.dp
                        )
                        .background(
                            color =
                                SerialBlueBackground,
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        )
                        .clickable {
                            onBack()
                        },

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "‹",
                    fontSize = 34.sp,
                    fontWeight =
                        FontWeight.Normal,
                    color =
                        SerialBlue
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        14.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "Serial Inventory",

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        SerialNavy
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            1.dp
                        )
                )


                Text(
                    text =
                        "Individual serial-wise inventory tracking",

                    fontSize =
                        11.sp,

                    color =
                        SerialMuted
                )
            }


            // =================================================
            // DASHBOARD ACTION
            // =================================================

            Box(
                modifier =
                    Modifier
                        .background(
                            color =
                                Color(0xFFF1F5F9),
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        )
                        .clickable {
                            onDashboard()
                        }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        )
            ) {

                Text(
                    text = "Dashboard",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SerialBlue
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        10.dp
                    )
            )


            Box(
                modifier =
                    Modifier
                        .background(
                            color =
                                SerialGold,
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        )
                        .clickable {
                            onExportClick()
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
            ) {

                Text(
                    text =
                        "EXPORT",

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        SerialWhite
                )
            }
        }
    }
}


// =============================================================
// EXPORT DIALOG
// =============================================================

@Composable
private fun SerialStockExportDialog(
    hasData: Boolean,
    onDismiss: () -> Unit,
    onExcel: () -> Unit,
    onPdf: () -> Unit,
    onPrint: () -> Unit
) {

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = SerialWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Export Serial Stock",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = SerialNavy
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = "Current search and status filter will be used.",
                            fontSize = 11.sp,
                            color = SerialMuted
                        )
                    }

                    Text(
                        text = "×",
                        modifier =
                            Modifier
                                .clickable { onDismiss() }
                                .padding(8.dp),
                        fontSize = 25.sp,
                        color = SerialMuted
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (!hasData) {
                    Text(
                        text = "No serial stock rows are available to export.",
                        fontSize = 12.sp,
                        color = SerialOrange
                    )
                    Spacer(Modifier.height(14.dp))
                }

                SerialExportAction(
                    title = "Excel / CSV",
                    subtitle = "Export the visible serial stock register",
                    enabled = hasData,
                    onClick = onExcel
                )

                Spacer(Modifier.height(10.dp))

                SerialExportAction(
                    title = "PDF",
                    subtitle = "Create and share an A4 landscape report",
                    enabled = hasData,
                    onClick = onPdf
                )

                Spacer(Modifier.height(10.dp))

                SerialExportAction(
                    title = "Print",
                    subtitle = "Open Android print / Save as PDF",
                    enabled = hasData,
                    onClick = onPrint
                )
            }
        }
    }
}


@Composable
private fun SerialExportAction(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    onClick()
                },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (enabled) SerialBackground
                else Color(0xFFF1F2F5)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .background(
                            color =
                                if (enabled) SerialBlueBackground
                                else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(10.dp)
                        ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        when (title) {
                            "Excel / CSV" -> "XLS"
                            "PDF" -> "PDF"
                            else -> "PRN"
                        },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) SerialBlue else SerialMuted
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) SerialNavy else SerialMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = SerialMuted
                )
            }

            Text(
                text = "›",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) SerialGold else SerialBorder
            )
        }
    }
}


// =============================================================
// SUMMARY
// =============================================================

@Composable
private fun SerialSummarySection(

    totalUnits: Int,

    inStockUnits: Int,

    issuedUnits: Int

) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        SerialSummaryCard(
            modifier =
                Modifier.weight(
                    1f
                ),
            title =
                "Total",
            value =
                totalUnits.toString(),
            backgroundColor =
                SerialBlueBackground,
            valueColor =
                SerialBlue
        )


        SerialSummaryCard(
            modifier =
                Modifier.weight(
                    1f
                ),
            title =
                "In Stock",
            value =
                inStockUnits.toString(),
            backgroundColor =
                SerialGreenBackground,
            valueColor =
                SerialGreen
        )


        SerialSummaryCard(
            modifier =
                Modifier.weight(
                    1f
                ),
            title =
                "Issued",
            value =
                issuedUnits.toString(),
            backgroundColor =
                SerialOrangeBackground,
            valueColor =
                SerialOrange
        )
    }
}


@Composable
private fun SerialSummaryCard(

    modifier: Modifier,

    title: String,

    value: String,

    backgroundColor: Color,

    valueColor: Color

) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                12.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text =
                    title,

                fontSize =
                    10.sp,

                fontWeight =
                    FontWeight.Medium,

                color =
                    SerialMuted
            )


            Text(
                text =
                    value,

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    valueColor
            )
        }
    }
}


// =============================================================
// SEARCH
// =============================================================

@Composable
private fun SerialSearchBox(

    query: String,

    onQueryChanged: (String) -> Unit,

    onClear: () -> Unit

) {

    OutlinedTextField(

        value =
            query,

        onValueChange =
            onQueryChanged,

        modifier =
            Modifier
                .fillMaxWidth(),

        singleLine =
            true,

        placeholder = {

            Text(
                text =
                    "Search serial, product, power, batch...",
                fontSize =
                    12.sp
            )
        },

        trailingIcon = {

            if (
                query.isNotBlank()
            ) {

                Text(
                    text =
                        "Clear",

                    modifier =
                        Modifier
                            .clickable {
                                onClear()
                            }
                            .padding(
                                8.dp
                            ),

                    color =
                        SerialBlue,

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        },

        shape =
            RoundedCornerShape(
                12.dp
            ),

        colors =
            OutlinedTextFieldDefaults.colors(

                focusedContainerColor =
                    SerialWhite,

                unfocusedContainerColor =
                    SerialWhite,

                focusedBorderColor =
                    SerialGold,

                unfocusedBorderColor =
                    SerialBorder,

                focusedTextColor =
                    SerialText,

                unfocusedTextColor =
                    SerialText,

                cursorColor =
                    SerialGold
            ),

        keyboardOptions =
            KeyboardOptions(
                imeAction =
                    ImeAction.Search
            ),

        keyboardActions =
            KeyboardActions(
                onSearch = {}
            )
    )
}


// =============================================================
// STATUS FILTER
// =============================================================

@Composable
private fun SerialStatusFilters(

    selectedStatus: String,

    onStatusSelected: (String) -> Unit

) {

    val statuses =
        remember {

            listOf(
                "ALL" to "All",
                "IN_STOCK" to "In Stock",
                "SAMPLE" to "Sample",
                "DEMO" to "Demo",
                "APPROVAL" to "Approval",
                "SOLD" to "Sold",
                "RETURNED" to "Returned"
            )
        }


    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),

        horizontalArrangement =
            Arrangement.spacedBy(
                7.dp
            )
    ) {

        statuses.forEach { status ->

            val statusCode =
                status.first

            val label =
                status.second

            val selected =
                selectedStatus.equals(
                    other = statusCode,
                    ignoreCase = true
                )


            Box(
                modifier =
                    Modifier
                        .background(
                            color =
                                if (selected)
                                    SerialBlueBackground
                                else
                                    SerialWhite,

                            shape =
                                RoundedCornerShape(
                                    50.dp
                                )
                        )
                        .border(
                            width =
                                1.dp,

                            color =
                                if (selected)
                                    SerialBlue
                                else
                                    SerialBorder,

                            shape =
                                RoundedCornerShape(
                                    50.dp
                                )
                        )
                        .clickable {

                            onStatusSelected(
                                statusCode
                            )
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
            ) {

                Text(
                    text =
                        label,

                    fontSize =
                        11.sp,

                    fontWeight =
                        if (selected)
                            FontWeight.Bold
                        else
                            FontWeight.Medium,

                    color =
                        if (selected)
                            SerialBlue
                        else
                            SerialText
                )
            }
        }
    }
}


// =============================================================
// REGISTER HEADER
// =============================================================

@Composable
private fun SerialRegisterHeader() {

    Card(
        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SerialBlueBackground
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "SERIAL NO.",

                modifier =
                    Modifier.weight(
                        1.20f
                    ),

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialBlue
            )


            Text(
                text =
                    "PRODUCT / MODEL",

                modifier =
                    Modifier.weight(
                        1.65f
                    ),

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialBlue
            )


            Text(
                text =
                    "POWER",

                modifier =
                    Modifier.weight(
                        0.65f
                    ),

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialBlue
            )


            Text(
                text =
                    "EXPIRY",

                modifier =
                    Modifier.weight(
                        0.65f
                    ),

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialBlue
            )


            Text(
                text =
                    "STATUS",

                modifier =
                    Modifier.weight(
                        0.90f
                    ),

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialBlue
            )


            Spacer(
                modifier =
                    Modifier.width(
                        14.dp
                    )
            )
        }
    }
}


// =============================================================
// COMPACT REGISTER ROW
// =============================================================

@Composable
private fun SerialRegisterRow(

    row: SerialStockRow,

    onClick: () -> Unit

) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(
                8.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SerialWhite
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                // =============================================
                // SERIAL
                // =============================================

                val displaySerial =
                    if (
                        row.serialPrefix.isNotBlank() &&
                        !row.serialNumber.startsWith(
                            row.serialPrefix,
                            ignoreCase = true
                        )
                    ) {
                        "${row.serialPrefix} ${row.serialNumber}"
                    } else {
                        row.serialNumber
                            .ifBlank {
                                "—"
                            }
                    }


                Text(
                    text =
                        displaySerial,

                    modifier =
                        Modifier.weight(
                            1.20f
                        ),

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        SerialNavy,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                // =============================================
                // PRODUCT / MODEL
                // =============================================

                Column(
                    modifier =
                        Modifier.weight(
                            1.65f
                        )
                ) {

                    val displayProductName =
                        if (
                            row.model.isNotBlank()
                        ) {
                            "${row.productName} (${row.model})"
                        } else {
                            row.productName
                                .ifBlank {
                                    "Unnamed Product"
                                }
                        }


                    Text(
                        text =
                            displayProductName,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            SerialText,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }


                // =============================================
                // POWER
                // =============================================

                Text(
                    text =
                        row.power
                            .ifBlank {
                                "—"
                            },

                    modifier =
                        Modifier.weight(
                            0.65f
                        ),

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        SerialBlue,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                // =============================================
                // EXPIRY
                // =============================================

                Text(
                    text =
                        formatCompactExpiry(
                            row.expiryDate
                        ),

                    modifier =
                        Modifier.weight(
                            0.65f
                        ),

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        SerialText,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                // =============================================
                // STATUS
                // =============================================

                Box(
                    modifier =
                        Modifier.weight(
                            0.90f
                        ),

                    contentAlignment =
                        Alignment.CenterStart
                ) {

                    CompactStatusBadge(
                        status =
                            row.status
                    )
                }


                // =============================================
                // MOVEMENT HISTORY ARROW
                // =============================================

                Text(
                    text =
                        "›",

                    modifier =
                        Modifier.width(
                            14.dp
                        ),

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        SerialBlue
                )
            }


            HorizontalDivider(
                thickness =
                    0.5.dp,

                color =
                    SerialBorder
            )
        }
    }
}


// =============================================================
// COMPACT STATUS BADGE
// =============================================================

@Composable
private fun CompactStatusBadge(

    status: String

) {

    val normalized =
        status
            .trim()
            .uppercase()


    val backgroundColor =
        when (normalized) {

            "IN_STOCK" ->
                SerialGreenBackground

            "SAMPLE" ->
                SerialBlueBackground

            "DEMO" ->
                SerialPurpleBackground

            "APPROVAL" ->
                SerialOrangeBackground

            "SOLD" ->
                SerialRedBackground

            "RETURNED" ->
                Color(
                    0xFFE9ECF1
                )

            else ->
                Color(
                    0xFFE9ECF1
                )
        }


    val foregroundColor =
        when (normalized) {

            "IN_STOCK" ->
                SerialGreen

            "SAMPLE" ->
                SerialBlue

            "DEMO" ->
                SerialPurple

            "APPROVAL" ->
                SerialOrange

            "SOLD" ->
                SerialRed

            else ->
                SerialMuted
        }


    val displayText =
        when (normalized) {

            "IN_STOCK" ->
                "STOCK"

            "APPROVAL" ->
                "APPR."

            "RETURNED" ->
                "RETURN"

            else ->
                normalized
                    .replace(
                        "_",
                        " "
                    )
                    .ifBlank {
                        "—"
                    }
        }


    Box(
        modifier =
            Modifier
                .background(
                    color =
                        backgroundColor,

                    shape =
                        RoundedCornerShape(
                            50.dp
                        )
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 4.dp
                )
    ) {

        Text(
            text =
                displayText,

            fontSize =
                7.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                foregroundColor,

            maxLines =
                1
        )
    }
}


// =============================================================
// EXPIRY FORMAT
// =============================================================

private fun formatCompactExpiry(

    rawExpiry: String

): String {

    val value =
        rawExpiry
            .trim()

    if (
        value.isBlank()
    ) {
        return "—"
    }


    // ---------------------------------------------------------
    // MMYY
    // Example:
    // 1029 -> 10-29
    // 0528 -> 05-28
    // ---------------------------------------------------------

    if (
        value.length == 4 &&
        value.all {
            it.isDigit()
        }
    ) {

        return value
            .substring(
                0,
                2
            ) +
                "-" +
                value.substring(
                    2,
                    4
                )
    }


    // ---------------------------------------------------------
    // MM/YYYY
    // Example:
    // 10/2029 -> 10-29
    // ---------------------------------------------------------

    val slashParts =
        value.split(
            "/"
        )

    if (
        slashParts.size == 2
    ) {

        val month =
            slashParts[0]
                .trim()

        val year =
            slashParts[1]
                .trim()

        if (
            month.length in 1..2 &&
            year.length >= 2 &&
            month.all {
                it.isDigit()
            } &&
            year.all {
                it.isDigit()
            }
        ) {

            return month
                .padStart(
                    2,
                    '0'
                ) +
                    "-" +
                    year.takeLast(
                        2
                    )
        }
    }


    // ---------------------------------------------------------
    // MM-YYYY
    // Example:
    // 10-2029 -> 10-29
    // ---------------------------------------------------------

    val dashParts =
        value.split(
            "-"
        )

    if (
        dashParts.size == 2
    ) {

        val first =
            dashParts[0]
                .trim()

        val second =
            dashParts[1]
                .trim()

        if (
            first.length in 1..2 &&
            second.length == 4 &&
            first.all {
                it.isDigit()
            } &&
            second.all {
                it.isDigit()
            }
        ) {

            return first
                .padStart(
                    2,
                    '0'
                ) +
                    "-" +
                    second.takeLast(
                        2
                    )
        }
    }


    // ---------------------------------------------------------
    // YYYY-MM / YYYY-MM-DD
    // Example:
    // 2029-10 -> 10-29
    // 2029-10-31 -> 10-29
    // ---------------------------------------------------------

    if (
        dashParts.size >= 2
    ) {

        val year =
            dashParts[0]
                .trim()

        val month =
            dashParts[1]
                .trim()

        if (
            year.length == 4 &&
            month.length in 1..2 &&
            year.all {
                it.isDigit()
            } &&
            month.all {
                it.isDigit()
            }
        ) {

            return month
                .padStart(
                    2,
                    '0'
                ) +
                    "-" +
                    year.takeLast(
                        2
                    )
        }
    }


    // ---------------------------------------------------------
    // FALLBACK
    // ---------------------------------------------------------

    return value
}


// =============================================================
// EMPTY STATE
// =============================================================

@Composable
private fun SerialEmptyState(

    hasFilters: Boolean,

    onReset: () -> Unit

) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SerialWhite
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        30.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    if (hasFilters)
                        "No Matching Serial Found"
                    else
                        "No Serial Stock Available",

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialNavy
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    if (hasFilters)
                        "Try another search or reset the selected filters."
                    else
                        "Serial-tracked stock will appear here after Purchase entry.",

                fontSize =
                    12.sp,

                color =
                    SerialMuted
            )


            if (hasFilters) {

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                Button(
                    onClick =
                        onReset,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                SerialBlue,
                            contentColor =
                                SerialWhite
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                ) {

                    Text(
                        text =
                            "Reset Filters",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// =============================================================
// ERROR STATE
// =============================================================

@Composable
private fun SerialErrorCard(

    message: String,

    onRetry: () -> Unit

) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SerialRedBackground
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        22.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "Unable to Load Serial Stock",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    SerialRed
            )


            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )


            Text(
                text =
                    message,

                fontSize =
                    12.sp,

                color =
                    SerialText
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Button(
                onClick =
                    onRetry,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            SerialBlue,
                        contentColor =
                            SerialWhite
                    )
            ) {

                Text(
                    text =
                        "Retry",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}