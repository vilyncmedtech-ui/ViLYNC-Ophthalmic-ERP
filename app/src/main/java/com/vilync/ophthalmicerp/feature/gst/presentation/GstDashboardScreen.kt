package com.vilync.ophthalmicerp.feature.gst.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.gst.model.GstSummary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GstDashboardScreen(
    viewModel: GstDashboardViewModel,
    financialYearStart: Int,
    financialYearDisplayName: String,
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(financialYearStart) {
        viewModel.loadFinancialYear(financialYearStart)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {

        GstDashboardHeader(
            onBack = onBack,
            onDashboard = onDashboard
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GST Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20242C)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Financial Year: $financialYearDisplayName",
            fontSize = 13.sp,
            color = Color(0xFF6D7480)
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {

            financialYearStart <= 0 -> {
                GstMessageCard(
                    message =
                        "Valid Financial Year is required. " +
                                "Please select the active Financial Year from Settings."
                )
            }

            uiState.isLoading && uiState.summary == null -> {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null &&
                    uiState.summary == null -> {

                GstMessageCard(
                    message =
                        uiState.errorMessage
                            ?: "Unable to load GST summary."
                )
            }

            else -> {

                val summary = uiState.summary

                if (summary != null) {
                    GstSummaryContent(
                        summary = summary
                    )
                }
            }
        }
    }
}

@Composable
private fun GstSummaryContent(
    summary: GstSummary
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        GstAmountCard(
            modifier = Modifier.weight(1f),
            title = "Output GST",
            amount = summary.outputGst,
            subtitle =
                "${summary.postedSalesCount} posted sales",
            background = Color(0xFFDDF3FF)
        )

        GstAmountCard(
            modifier = Modifier.weight(1f),
            title = "Input GST / ITC",
            amount = summary.inputGst,
            subtitle =
                "${summary.purchaseCount} purchases",
            background = Color(0xFFDDF5E5)
        )

        GstAmountCard(
            modifier = Modifier.weight(1f),
            title = "Net GST Position",
            amount = summary.netGstPosition,
            subtitle = "Output GST - Input GST",
            background = Color(0xFFEDE4FF)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        GstDetailCard(
            modifier = Modifier.weight(1f),
            title = "Output Tax Liability",
            background = Color(0xFFFFE8D3),
            rows = listOf(
                "Taxable Value" to
                        summary.outputTaxableValue,

                "CGST" to
                        summary.outputCgst,

                "SGST" to
                        summary.outputSgst,

                "IGST" to
                        summary.outputIgst,

                "Total Output GST" to
                        summary.outputGst
            )
        )

        GstDetailCard(
            modifier = Modifier.weight(1f),
            title = "Input GST / Purchase Tax",
            background = Color(0xFFDDF4F5),
            rows = listOf(
                "Purchase Taxable Value" to
                        summary.inputTaxableValue,

                "Total Input GST" to
                        summary.inputGst
            ),
            note =
                "Historical purchase tax is shown as " +
                        "total Input GST. CGST/SGST/IGST " +
                        "head-wise ITC is not reclassified " +
                        "or recalculated."
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    GstMessageCard(
        message =
            "Read-only GST summary. Existing Sales, " +
                    "Purchase, Inventory and historical " +
                    "financial records are not modified."
    )
}

@Composable
private fun GstDashboardHeader(
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 9.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFE8F0FE),

                        contentColor =
                            Color(0xFF345FA8)
                    ),
                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "Back",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Text(
                text = "GST",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Button(
                onClick = onDashboard,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF456FB5),

                        contentColor =
                            Color.White
                    ),
                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "Dashboard",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GstAmountCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    subtitle: String,
    background: Color
) {

    Card(
        modifier =
            modifier.height(120.dp),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = background
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color = Color(0xFF4C535E)
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text = formatMoney(amount),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6D7480)
            )
        }
    }
}

@Composable
private fun GstDetailCard(
    modifier: Modifier,
    title: String,
    background: Color,
    rows: List<Pair<String, Double>>,
    note: String? = null
) {

    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = background
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            rows.forEach { row ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = row.first,
                        fontSize = 13.sp,
                        color = Color(0xFF525A66)
                    )

                    Text(
                        text =
                            formatMoney(row.second),

                        fontSize = 13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color(0xFF20242C)
                    )
                }
            }

            if (!note.isNullOrBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text = note,
                    fontSize = 11.sp,
                    color = Color(0xFF6D7480)
                )
            }
        }
    }
}

@Composable
private fun GstMessageCard(
    message: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Text(
            text = message,
            modifier =
                Modifier.padding(16.dp),
            fontSize = 13.sp,
            color = Color(0xFF5F6670)
        )
    }
}

private fun formatMoney(
    value: Double
): String {

    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    return formatter.format(value)
}