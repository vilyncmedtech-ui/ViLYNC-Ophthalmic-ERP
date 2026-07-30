package com.vilync.ophthalmicerp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InventoryHomeScreen(
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onStockRegisterClick: () -> Unit = {},
    onSerialStockRegisterClick: () -> Unit = {},
    onOpeningStockClick: () -> Unit = {},
    onStockAdjustmentClick: () -> Unit = {},
    onStockReconciliationClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFF),
                        Color(0xFFF4F7FF)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {

            // =====================================================
            // HEADER
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Card(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                onBack()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFE8F0FE)
                        )
                    ) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "←",
                                fontSize = 22.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(0xFF3455A4)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Inventory",
                            fontSize = 21.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                Color(0xFF1F2530)
                        )

                        Text(
                            text =
                                "Stock control and inventory management",
                            fontSize = 12.sp,
                            color =
                                Color(0xFF6B7280)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .clickable {
                                onDashboard()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFE8F0FE)
                        )
                    ) {

                        Text(
                            text = "Dashboard",
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            ),
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                Color(0xFF3455A4)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =====================================================
            // CURRENT STOCK
            // =====================================================

            InventorySectionTitle(
                title = "Current Stock"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                InventoryMenuCard(
                    modifier = Modifier.weight(1f),
                    icon = "▥",
                    title = "Stock Register",
                    subtitle =
                        "Product and power-wise current stock",
                    backgroundColor =
                        Color(0xFFE7F0FF),
                    iconColor =
                        Color(0xFF3455A4),
                    onClick =
                        onStockRegisterClick
                )

                InventoryMenuCard(
                    modifier = Modifier.weight(1f),
                    icon = "#",
                    title = "Serial Stock Register",
                    subtitle =
                        "Track individual IOL serial numbers",
                    backgroundColor =
                        Color(0xFFEDE6FF),
                    iconColor =
                        Color(0xFF7D57D1),
                    onClick =
                        onSerialStockRegisterClick
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =====================================================
            // OPENING STOCK
            // =====================================================

            InventorySectionTitle(
                title = "Opening Stock"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            InventoryMenuCard(
                modifier = Modifier.fillMaxWidth(),
                icon = "+",
                title = "Opening Stock Entry",
                subtitle =
                    "Record controlled opening inventory",
                backgroundColor =
                    Color(0xFFDFF6E8),
                iconColor =
                    Color(0xFF14945A),
                onClick =
                    onOpeningStockClick
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =====================================================
            // STOCK CONTROL
            // =====================================================

            InventorySectionTitle(
                title = "Stock Control"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                InventoryMenuCard(
                    modifier = Modifier.weight(1f),
                    icon = "±",
                    title = "Stock Adjustment",
                    subtitle =
                        "Shortage, damaged or destroyed stock",
                    backgroundColor =
                        Color(0xFFFFECD8),
                    iconColor =
                        Color(0xFFE57A1F),
                    onClick =
                        onStockAdjustmentClick
                )

                InventoryMenuCard(
                    modifier = Modifier.weight(1f),
                    icon = "✓",
                    title = "Stock Reconciliation",
                    subtitle =
                        "Compare system and physical stock",
                    backgroundColor =
                        Color(0xFFFFF2C7),
                    iconColor =
                        Color(0xFFC98A00),
                    onClick =
                        onStockReconciliationClick
                )
            }
        }
    }
}


@Composable
private fun InventorySectionTitle(
    title: String
) {

    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF252A33)
    )
}


@Composable
private fun InventoryMenuCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(94.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.84f
                            ),
                        shape =
                            RoundedCornerShape(12.dp)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = icon,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color = iconColor
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        Color(0xFF252A33),
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color =
                        Color(0xFF667085),
                    maxLines = 2
                )
            }
        }
    }
}