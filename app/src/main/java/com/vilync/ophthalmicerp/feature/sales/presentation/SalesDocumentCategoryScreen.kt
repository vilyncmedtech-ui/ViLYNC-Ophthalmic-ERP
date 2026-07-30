package com.vilync.ophthalmicerp.feature.sales.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CategoryNavy = Color(0xFF16213E)
private val CategorySecondary = Color(0xFF667085)
private val CategoryBorder = Color(0xFFD6DCE8)

@Composable
fun SalesDocumentCategoryScreen(
    title: String,
    subtitle: String,
    newTitle: String,
    newSubtitle: String,
    registerTitle: String,
    registerSubtitle: String,
    newSymbol: String,
    registerSymbol: String = "▤",
    newBackground: Color,
    registerBackground: Color,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onNewDocument: () -> Unit,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryTopButton("← Back", onBack)
            CategoryTopButton("⌂ Dashboard", onDashboard)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CategoryNavy
        )

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = CategorySecondary
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CategoryTile(
                modifier = Modifier.weight(1f),
                title = newTitle,
                subtitle = newSubtitle,
                symbol = newSymbol,
                background = newBackground,
                onClick = onNewDocument
            )

            CategoryTile(
                modifier = Modifier.weight(1f),
                title = registerTitle,
                subtitle = registerSubtitle,
                symbol = registerSymbol,
                background = registerBackground,
                onClick = onRegister
            )
        }
    }
}

@Composable
private fun CategoryTopButton(
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CategoryBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
            color = CategoryNavy,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryTile(
    modifier: Modifier,
    title: String,
    subtitle: String,
    symbol: String,
    background: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, CategoryBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = symbol,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    color = CategoryNavy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = CategoryNavy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = CategorySecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
