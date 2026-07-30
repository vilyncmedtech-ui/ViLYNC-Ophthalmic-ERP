package com.vilync.ophthalmicerp.feature.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.inventory.model.StockRegisterRow
import com.vilync.ophthalmicerp.feature.inventory.report.StockRegisterExcelExporter
import com.vilync.ophthalmicerp.feature.inventory.report.StockRegisterPdfExporter
import com.vilync.ophthalmicerp.feature.inventory.report.StockRegisterPrintAdapter
import com.vilync.ophthalmicerp.feature.inventory.report.createStockRegisterReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun StockRegisterScreen(
    viewModel: StockRegisterViewModel,
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {}
) {

    val context =
        LocalContext.current

    val stockRows by
    viewModel.stockRows.collectAsState()

    val searchQuery by
    viewModel.searchQuery.collectAsState()

    val totalAvailableUnits by
    viewModel.totalAvailableUnits.collectAsState()

    val totalStockRows by
    viewModel.totalStockRows.collectAsState()


    var exportMenuExpanded by
    remember {
        mutableStateOf(false)
    }


    // =========================================================
    // COLOURS
    // =========================================================

    val backgroundColor =
        Color(0xFFF5F7FF)

    val headerBlue =
        Color(0xFFE4EEFF)

    val summaryBlue =
        Color(0xFFDDEBFF)

    val summaryPurple =
        Color(0xFFECE4FF)

    val positiveGreen =
        Color(0xFFDDF5E8)

    val returnOrange =
        Color(0xFFFFEBD8)

    val textPrimary =
        Color(0xFF252A33)

    val textSecondary =
        Color(0xFF6D7480)

    val blueAccent =
        Color(0xFF345FA8)


    // =========================================================
    // REPORT SNAPSHOT CREATOR
    // =========================================================

    fun createCurrentReport() =
        createStockRegisterReport(
            stockRows = stockRows,
            generatedAt =
                SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                ).format(
                    Date()
                ),
            searchQuery = searchQuery
        )


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // =================================================
                // BACK
                // =================================================

                Card(
                    modifier =
                        Modifier.size(48.dp),
                    shape =
                        RoundedCornerShape(14.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                headerBlue
                        )
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        IconButton(
                            onClick = onBack
                        ) {

                            Text(
                                text = "←",
                                fontSize = 27.sp,
                                fontWeight =
                                    FontWeight.Medium,
                                color = blueAccent
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                // =================================================
                // TITLE
                // =================================================

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Stock Register",
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = textPrimary
                    )

                    Text(
                        text =
                            "Product and power-wise current stock",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }


                // =================================================
                // EXPORT / PRINT
                // =================================================

                Box {

                    Card(
                        modifier =
                            Modifier.clickable {
                                exportMenuExpanded =
                                    true
                            },
                        shape =
                            RoundedCornerShape(14.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFE8F1FF)
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Export / Print",
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color = blueAccent
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            Text(
                                text = "▾",
                                fontSize = 13.sp,
                                color = blueAccent
                            )
                        }
                    }


                    DropdownMenu(
                        expanded =
                            exportMenuExpanded,
                        onDismissRequest = {
                            exportMenuExpanded =
                                false
                        }
                    ) {

                        // =========================================
                        // EXCEL
                        // =========================================

                        DropdownMenuItem(
                            text = {

                                Column {

                                    Text(
                                        text = "Excel",
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Text(
                                        text =
                                            "Open in Excel or Sheets",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            },
                            onClick = {

                                exportMenuExpanded =
                                    false


                                val result =
                                    StockRegisterExcelExporter
                                        .exportAndShare(
                                            context =
                                                context,
                                            report =
                                                createCurrentReport()
                                        )


                                if (result.isFailure) {

                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()
                                            ?.message
                                            ?: "Unable to export Excel file.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )


                        // =========================================
                        // PDF
                        // =========================================

                        DropdownMenuItem(
                            text = {

                                Column {

                                    Text(
                                        text = "PDF",
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Text(
                                        text =
                                            "Create and share PDF report",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            },
                            onClick = {

                                exportMenuExpanded =
                                    false


                                val result =
                                    StockRegisterPdfExporter
                                        .exportAndShare(
                                            context =
                                                context,
                                            report =
                                                createCurrentReport()
                                        )


                                if (result.isFailure) {

                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()
                                            ?.message
                                            ?: "Unable to export PDF.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )


                        // =========================================
                        // PRINT
                        // =========================================

                        DropdownMenuItem(
                            text = {

                                Column {

                                    Text(
                                        text = "Print",
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Text(
                                        text =
                                            "Printer or Save as PDF",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            },
                            onClick = {

                                exportMenuExpanded =
                                    false


                                val result =
                                    StockRegisterPrintAdapter
                                        .print(
                                            context =
                                                context,
                                            report =
                                                createCurrentReport()
                                        )


                                if (result.isFailure) {

                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()
                                            ?.message
                                            ?: "Unable to start printing.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                // =================================================
                // DASHBOARD - NOW ACTUALLY CLICKABLE
                // =================================================

                Card(
                    modifier =
                        Modifier.clickable(
                            onClick =
                                onDashboard
                        ),
                    shape =
                        RoundedCornerShape(14.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                headerBlue
                        )
                ) {

                    Box(
                        modifier =
                            Modifier.padding(
                                horizontal = 18.dp,
                                vertical = 14.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "Dashboard",
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color = blueAccent
                        )
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // =====================================================
        // SUMMARY
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SummaryCard(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Current Units",
                value =
                    totalAvailableUnits.toString(),
                backgroundColor =
                    positiveGreen,
                valueColor =
                    Color(0xFF168457)
            )


            SummaryCard(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Stock Rows",
                value =
                    totalStockRows.toString(),
                backgroundColor =
                    summaryBlue,
                valueColor =
                    blueAccent
            )


            SummaryCard(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Stock Status",
                value =
                    if (
                        totalAvailableUnits > 0
                    ) {
                        "Available"
                    } else {
                        "No Stock"
                    },
                backgroundColor =
                    summaryPurple,
                valueColor =
                    Color(0xFF7652C7)
            )
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // =====================================================
        // SEARCH
        // =====================================================

        OutlinedTextField(
            value =
                searchQuery,
            onValueChange = {
                viewModel.updateSearchQuery(
                    it
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine =
                true,
            shape =
                RoundedCornerShape(16.dp),
            placeholder = {

                Text(
                    text =
                        "Search product, model, category or power...",
                    color =
                        textSecondary
                )
            },
            trailingIcon = {

                if (
                    searchQuery.isNotBlank()
                ) {

                    IconButton(
                        onClick = {
                            viewModel.clearSearch()
                        }
                    ) {

                        Text(
                            text = "×",
                            fontSize = 24.sp,
                            color = textSecondary
                        )
                    }
                }
            },
            colors =
                OutlinedTextFieldDefaults
                    .colors(
                        focusedContainerColor =
                            Color.White,
                        unfocusedContainerColor =
                            Color.White,
                        focusedBorderColor =
                            blueAccent,
                        unfocusedBorderColor =
                            Color(0xFFD9DFEA)
                    )
        )


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // =====================================================
        // SECTION TITLE
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Current Stock",
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                color = textPrimary
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            Text(
                text =
                    "(${stockRows.size})",
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Medium,
                color = textSecondary
            )
        }


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        // =====================================================
        // STOCK LIST
        // =====================================================

        if (
            stockRows.isEmpty()
        ) {

            EmptyStockCard(
                hasSearch =
                    searchQuery.isNotBlank()
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(9.dp)
            ) {

                items(
                    items =
                        stockRows,
                    key = { row ->

                        "${row.productId}_${row.power}_${row.model}"
                    }
                ) { row ->

                    StockRowCard(
                        row = row,
                        positiveGreen =
                            positiveGreen,
                        returnOrange =
                            returnOrange,
                        textPrimary =
                            textPrimary,
                        textSecondary =
                            textSecondary
                    )
                }


                item {

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )
                }
            }
        }
    }
}


// =============================================================
// SUMMARY CARD
// =============================================================

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    backgroundColor: Color,
    valueColor: Color
) {

    Card(
        modifier =
            modifier.height(82.dp),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                color =
                    Color(0xFF626B78),
                maxLines = 1
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// STOCK ROW
// =============================================================

@Composable
private fun StockRowCard(
    row: StockRegisterRow,
    positiveGreen: Color,
    returnOrange: Color,
    textPrimary: Color,
    textSecondary: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // =================================================
            // PRODUCT
            // =================================================

            Column(
                modifier =
                    Modifier.weight(2.2f)
            ) {

                Text(
                    text =
                        row.productName
                            .ifBlank {
                                "Unnamed Product"
                            },
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )


                val productDetails =
                    buildString {

                        if (
                            row.model.isNotBlank()
                        ) {
                            append(
                                row.model
                            )
                        }


                        if (
                            row.model.isNotBlank() &&
                            row.category.isNotBlank()
                        ) {
                            append(
                                "  •  "
                            )
                        }


                        if (
                            row.category.isNotBlank()
                        ) {
                            append(
                                row.category
                            )
                        }
                    }


                Text(
                    text =
                        productDetails
                            .ifBlank {
                                "Product"
                            },
                    fontSize = 11.sp,
                    color =
                        textSecondary,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }


            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )


            // =================================================
            // POWER
            // =================================================

            Column(
                modifier =
                    Modifier.weight(0.8f),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Power",
                    fontSize = 10.sp,
                    color =
                        textSecondary
                )


                Text(
                    text =
                        row.power.ifBlank {
                            "—"
                        },
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        textPrimary,
                    maxLines = 1
                )
            }


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            // =================================================
            // PURCHASED
            // =================================================

            QuantityBox(
                modifier =
                    Modifier.weight(0.9f),
                title =
                    "Purchased",
                quantity =
                    row.purchasedQuantity,
                backgroundColor =
                    Color(0xFFE4EEFF),
                quantityColor =
                    Color(0xFF345FA8)
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            // =================================================
            // RETURNED
            // =================================================

            QuantityBox(
                modifier =
                    Modifier.weight(0.9f),
                title =
                    "Returned",
                quantity =
                    row.purchaseReturnQuantity,
                backgroundColor =
                    returnOrange,
                quantityColor =
                    Color(0xFFC86B17)
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            // =================================================
            // AVAILABLE
            // =================================================

            QuantityBox(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Available",
                quantity =
                    row.availableQuantity,
                backgroundColor =
                    positiveGreen,
                quantityColor =
                    Color(0xFF168457)
            )
        }
    }
}


// =============================================================
// QUANTITY BOX
// =============================================================

@Composable
private fun QuantityBox(
    modifier: Modifier = Modifier,
    title: String,
    quantity: Int,
    backgroundColor: Color,
    quantityColor: Color
) {

    Card(
        modifier =
            modifier.height(58.dp),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.Center,
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                fontSize = 9.sp,
                color =
                    Color(0xFF68717D),
                maxLines = 1
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            Text(
                text =
                    quantity.toString(),
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    quantityColor
            )
        }
    }
}


// =============================================================
// EMPTY STATE
// =============================================================

@Composable
private fun EmptyStockCard(
    hasSearch: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFEAF1FF)
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.Center,
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "▥",
                fontSize = 30.sp,
                color =
                    Color(0xFF5272AD)
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text =
                    if (hasSearch) {
                        "No matching stock found"
                    } else {
                        "No stock available"
                    },
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    Color(0xFF2C3850)
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(
                text =
                    if (hasSearch) {
                        "Try another product, model or power."
                    } else {
                        "Purchase stock will appear here automatically."
                    },
                fontSize = 12.sp,
                color =
                    Color(0xFF6D7480)
            )
        }
    }
}