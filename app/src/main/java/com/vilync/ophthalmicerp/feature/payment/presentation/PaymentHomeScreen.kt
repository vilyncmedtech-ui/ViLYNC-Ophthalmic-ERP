package com.vilync.ophthalmicerp.feature.payment.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentHomeScreen(
    onBack: () -> Unit,
    onReceiptClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onReceiptRegisterClick: () -> Unit,
    onPaymentRegisterClick: () -> Unit,
    onReceivablesEnquiryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FD))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }
            Text(
                "Payments & Receipts",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14233C)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Text("MONEY IN (RECEIPTS)", fontWeight = FontWeight.Bold, color = Color(0xFF667085), fontSize = 12.sp)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMenuCard(
                title = "Customer Receipt",
                subtitle = "Record payments from customers",
                icon = "↙",
                color = Color(0xFFDDF6E8),
                onClick = onReceiptClick,
                modifier = Modifier.weight(1f)
            )
            PaymentMenuCard(
                title = "Receipt Register",
                subtitle = "View all money received",
                icon = "▥",
                color = Color(0xFFEFF8FF),
                onClick = onReceiptRegisterClick,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMenuCard(
                title = "Receivables Enquiry",
                subtitle = "Party-wise outstanding status",
                icon = "📋",
                color = Color(0xFFF9F5FF),
                onClick = onReceivablesEnquiryClick,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("MONEY OUT (PAYMENTS)", fontWeight = FontWeight.Bold, color = Color(0xFF667085), fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMenuCard(
                title = "Supplier Payment",
                subtitle = "Record payments to vendors",
                icon = "↗",
                color = Color(0xFFFFF2E4),
                onClick = onPaymentClick,
                modifier = Modifier.weight(1f)
            )
            PaymentMenuCard(
                title = "Payment Register",
                subtitle = "View all money paid out",
                icon = "▥",
                color = Color(0xFFF5F2FF),
                onClick = onPaymentRegisterClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PaymentMenuCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF667085))
            }
        }
    }
}
