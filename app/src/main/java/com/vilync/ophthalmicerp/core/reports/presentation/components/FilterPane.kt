package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.reports.domain.ReportFilterDescriptor

@Composable
fun FilterPane(
    modifier: Modifier = Modifier,
    filters: List<ReportFilterDescriptor>,
    currentValues: Map<String, Any?>,
    dynamicOptions: Map<String, List<String>> = emptyMap(),
    onFilterChange: (String, Any?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = "FILTERS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF345FA8))
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartFilterBar(
                    filters = filters,
                    currentValues = currentValues,
                    dynamicOptions = dynamicOptions,
                    onFilterChange = onFilterChange,
                    onApply = onApply
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF345FA8))
                ) {
                    Text("Apply")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}
