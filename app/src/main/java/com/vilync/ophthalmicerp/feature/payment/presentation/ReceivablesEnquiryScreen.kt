package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.payment.domain.PartyReceivableSummary

@Composable
fun ReceivablesEnquiryScreen(
    viewModel: ReceivablesViewModel,
    onBack: () -> Unit,
    onPartyClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FD))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Receivables Enquiry",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
        }

        // Summary Cards
        OverallTotalsRow(uiState)

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Party Name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFD9DEE8)
            )
        )

        // List
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No receivables found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.filteredSummaries) { summary ->
                    ReceivablePartyCard(summary = summary, onClick = { onPartyClick(summary.partyId) })
                }
            }
        }
    }
}

@Composable
private fun OverallTotalsRow(uiState: ReceivablesUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14233C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TotalItem("Total Due", uiState.totalDue, Color.White.copy(alpha = 0.7f))
            }
            VerticalDivider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.height(32.dp).width(1.dp)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TotalItem("Total Received", uiState.totalReceived, Color.White.copy(alpha = 0.7f))
            }
            VerticalDivider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.height(32.dp).width(1.dp)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TotalItem("Adjustment\n(Rebate/Credit Note)", uiState.totalAdjustments, Color.White.copy(alpha = 0.7f))
            }
            VerticalDivider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.height(32.dp).width(1.dp)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TotalItem("Outstanding", uiState.totalOutstanding, Color(0xFF4ADE80), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TotalItem(label: String, amount: Double, color: Color, fontWeight: FontWeight = FontWeight.Normal) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp
        )
        Text("₹ %.2f".format(amount), color = color, fontSize = 16.sp, fontWeight = fontWeight, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ReceivablePartyCard(summary: PartyReceivableSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(summary.partyName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF14233C))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SummaryField("Total Due", summary.totalDue)
                }
                VerticalDivider(
                    color = Color(0xFFF2F4F7),
                    modifier = Modifier.height(24.dp).width(1.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SummaryField("Total Received", summary.totalReceived)
                }
                VerticalDivider(
                    color = Color(0xFFF2F4F7),
                    modifier = Modifier.height(24.dp).width(1.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SummaryField("Adjustment\n(Rebate/Credit Note)", summary.totalAdjustments)
                }
                VerticalDivider(
                    color = Color(0xFFF2F4F7),
                    modifier = Modifier.height(24.dp).width(1.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SummaryField(
                        label = "Outstanding",
                        value = summary.balanceOutstanding,
                        valueColor = if (summary.balanceOutstanding > 0) Color(0xFFB42318) else Color(0xFF027A48),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryField(
    label: String,
    value: Double,
    valueColor: Color = Color(0xFF14233C),
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF667085),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp
        )
        Text(
            text = "₹ %.2f".format(value),
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
