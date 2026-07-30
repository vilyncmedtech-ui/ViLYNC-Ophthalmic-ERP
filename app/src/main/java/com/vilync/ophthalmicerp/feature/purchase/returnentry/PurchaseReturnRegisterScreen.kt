package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnSerialSearchRow

@Composable
fun PurchaseReturnRegisterScreen(
    viewModel: PurchaseReturnRegisterViewModel,
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onCreateReturn: (Long) -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Text(
                text = "Purchase Return",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onDashboard) { Text("⌂ Dashboard") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Search IOL Serial No.") },
            supportingText = { Text("Enter at least 2 characters") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (query.trim().length < 2) {
            Text(
                text = "Search a received IOL serial to create a return.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (results.isEmpty()) {
            Text(
                text = "No available IOL serial found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.purchaseLensId }) { row ->
                    SerialSearchCard(
                        row = row,
                        onClick = { onCreateReturn(row.purchaseId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SerialSearchCard(
    row: PurchaseReturnSerialSearchRow,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = row.serialNumber,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Invoice: ${row.invoiceNumber}  |  Date: ${row.invoiceDate}")
            Text("Supplier: ${row.supplierName}")
            Text("Power: ${row.power.ifBlank { "-" }}  |  Expiry: ${row.expiryDate}")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to create return",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
