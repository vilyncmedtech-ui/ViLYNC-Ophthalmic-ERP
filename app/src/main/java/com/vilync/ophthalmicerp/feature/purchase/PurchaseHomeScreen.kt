package com.vilync.ophthalmicerp.feature.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PurchaseHomeScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onNewPurchase: () -> Unit, // Purchase Order
    onPurchaseInvoice: () -> Unit,
    onPurchaseRegister: () -> Unit,
    onPurchaseReturn: () -> Unit,
    onPurchaseReturnRegister: () -> Unit,
    onPurchaseOrderRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack, border = BorderStroke(1.dp, BorderColor)) {
                Text(text = "← Back", color = NavyText, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(onClick = onDashboard, border = BorderStroke(1.dp, BorderColor)) {
                Text(text = "⌂ Dashboard", color = NavyText, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text(text = "Purchase", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NavyText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Manage purchase orders and invoices", fontSize = 15.sp, color = SecondaryText)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PurchaseActionCard(
                title = "New Purchase",
                subtitle = "Receive stock & pay",
                symbol = "+",
                backgroundColor = PastelBlue,
                onClick = onPurchaseInvoice,
                modifier = Modifier.weight(1f)
            )
            PurchaseActionCard(
                title = "Purchase Register",
                subtitle = "View posted invoices",
                symbol = "▦",
                backgroundColor = PastelGreen,
                onClick = onPurchaseRegister,
                modifier = Modifier.weight(1f)
            )
        }

        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PurchaseActionCard(
                title = "New Purchase Order",
                subtitle = "Commitment to buy",
                symbol = "📋",
                backgroundColor = PastelLavender,
                onClick = onNewPurchase,
                modifier = Modifier.weight(1f)
            )
            PurchaseActionCard(
                title = "Order Register",
                subtitle = "Manage pending orders",
                symbol = "▤",
                backgroundColor = Color(0xFFFDF2F2),
                onClick = onPurchaseOrderRegister,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PurchaseActionCard(
                title = "Purchase Return",
                subtitle = "Return stock to vendor",
                symbol = "↩",
                backgroundColor = PastelPeach,
                onClick = onPurchaseReturn,
                modifier = Modifier.weight(1f)
            )
            PurchaseActionCard(
                title = "Purchase Return Register",
                subtitle = "View manage returns",
                symbol = "↪",
                backgroundColor = Color(0xFFF0F0F0),
                onClick = onPurchaseReturnRegister,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PurchaseActionCard(
    title: String,
    subtitle: String,
    symbol: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(116.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(color = Color.White.copy(alpha = 0.78f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = symbol, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NavyText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NavyText)
                Text(text = subtitle, fontSize = 12.sp, color = SecondaryText, lineHeight = 14.sp)
            }
        }
    }
}

private val PageBackground = Color(0xFFF7F9FD)
private val NavyText = Color(0xFF18233A)
private val SecondaryText = Color(0xFF697386)
private val BorderColor = Color(0xFFDDE3EC)
private val PastelBlue = Color(0xFFDDE9FA)
private val PastelGreen = Color(0xFFDDF3E5)
private val PastelPeach = Color(0xFFFFE7CF)
private val PastelLavender = Color(0xFFECE4FA)
