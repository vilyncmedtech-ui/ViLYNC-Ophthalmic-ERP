package com.vilync.ophthalmicerp.feature.sales.reports.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.feature.sales.reports.data.SalesTransactionDetail
import com.vilync.ophthalmicerp.feature.sales.reports.data.ProductWiseSummary

@Composable
fun SalesTransactionRow(detail: SalesTransactionDetail) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = detail.invoiceNo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = detail.customer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = detail.date, fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "₹${"%.2f".format(detail.amount)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (detail.status == "POSTED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = detail.status,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = if (detail.status == "POSTED") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductSummaryRow(summary: ProductWiseSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "Qty: ${summary.qty}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "₹${"%.2f".format(summary.amount)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
