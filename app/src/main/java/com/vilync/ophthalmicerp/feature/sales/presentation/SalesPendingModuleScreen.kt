package com.vilync.ophthalmicerp.feature.sales.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SalesPendingModuleScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("← Back")
        }

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF16213E)
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF7F8FC)
            ),
            border = BorderStroke(
                1.dp,
                Color(0xFFD6DCE8)
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(20.dp),
                color = Color(0xFF667085)
            )
        }
    }
}
