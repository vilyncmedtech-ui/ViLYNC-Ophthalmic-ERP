package com.vilync.ophthalmicerp.feature.master.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun MasterScreen(
    onProductMasterClick: () -> Unit = {},
    onPartyMasterClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // =====================================================
        // TITLE
        // =====================================================

        Text(
            text = "Master",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Manage products, customers and vendors",
            style = MaterialTheme.typography.bodyMedium
        )


        // =====================================================
        // PRODUCT MASTER
        // =====================================================

        MasterMenuCard(
            iconText = "P",
            title = "Product Master",
            description =
                "Manage products, HSN, GST, pricing, units and inventory settings",
            onClick = onProductMasterClick
        )


        // =====================================================
        // CUSTOMER / VENDOR MASTER
        // =====================================================

        MasterMenuCard(
            iconText = "C/V",
            title = "Customer / Vendor Master",
            description =
                "Manage customers, suppliers, GST details, addresses and credit terms",
            onClick = onPartyMasterClick
        )
    }
}


// =============================================================
// MASTER MENU CARD
// =============================================================

@Composable
private fun MasterMenuCard(
    iconText: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // =================================================
            // SIMPLE ICON BOX
            // =================================================

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {

                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F0FE)
                    )
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = iconText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3455A4)
                        )
                    }
                }
            }


            // =================================================
            // TITLE + DESCRIPTION
            // =================================================

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}