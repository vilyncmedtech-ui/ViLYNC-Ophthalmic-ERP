package com.vilync.ophthalmicerp.feature.purchase

import androidx.compose.foundation.BorderStroke
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


// =============================================================
// ViLYNC PASTEL ERP THEME
// =============================================================

private val PageBackground =
    Color(0xFFF7F9FD)

private val NavyText =
    Color(0xFF18233A)

private val SecondaryText =
    Color(0xFF697386)

private val BorderColor =
    Color(0xFFDDE3EC)

private val PastelBlue =
    Color(0xFFDDE9FA)

private val PastelGreen =
    Color(0xFFDDF3E5)

private val PastelPeach =
    Color(0xFFFFE7CF)

private val PastelLavender =
    Color(0xFFECE4FA)


// =============================================================
// PURCHASE HOME SCREEN
// =============================================================

@Composable
fun PurchaseHomeScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onNewPurchase: () -> Unit,
    onPurchaseRegister: () -> Unit,
    onPurchaseReturn: () -> Unit,
    onPurchaseReturnRegister: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(
                horizontal = 24.dp,
                vertical = 20.dp
            )
    ) {

        // =====================================================
        // TOP NAVIGATION
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(
                    1.dp,
                    BorderColor
                )
            ) {
                Text(
                    text = "← Back",
                    color = NavyText,
                    fontWeight = FontWeight.Medium
                )
            }


            OutlinedButton(
                onClick = onDashboard,
                border = BorderStroke(
                    1.dp,
                    BorderColor
                )
            ) {
                Text(
                    text = "⌂ Dashboard",
                    color = NavyText,
                    fontWeight = FontWeight.Medium
                )
            }
        }


        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // =====================================================
        // PAGE TITLE
        // =====================================================

        Text(
            text = "Purchase",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NavyText
        )


        Spacer(
            modifier = Modifier.height(4.dp)
        )


        Text(
            text = "Manage purchase transactions and returns",
            fontSize = 15.sp,
            color = SecondaryText
        )


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // =====================================================
        // FIRST ROW
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            PurchaseActionCard(
                title = "New Purchase",
                subtitle = "Create a new purchase invoice",
                symbol = "+",
                backgroundColor = PastelBlue,
                onClick = onNewPurchase,
                modifier = Modifier.weight(1f)
            )


            PurchaseActionCard(
                title = "Purchase Register",
                subtitle = "View and manage purchase invoices",
                symbol = "▤",
                backgroundColor = PastelGreen,
                onClick = onPurchaseRegister,
                modifier = Modifier.weight(1f)
            )
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =====================================================
        // SECOND ROW
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            PurchaseActionCard(
                title = "Purchase Return",
                subtitle = "Create a purchase return",
                symbol = "↩",
                backgroundColor = PastelPeach,
                onClick = onPurchaseReturn,
                modifier = Modifier.weight(1f)
            )


            PurchaseActionCard(
                title = "Purchase Return Register",
                subtitle = "View and manage purchase returns",
                symbol = "▦",
                backgroundColor = PastelLavender,
                onClick = onPurchaseReturnRegister,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


// =============================================================
// PURCHASE ACTION CARD
// =============================================================

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
        modifier = modifier
            .height(116.dp)
            .clickable(
                onClick = onClick
            ),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        border =
            BorderStroke(
                1.dp,
                BorderColor
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // =================================================
            // ICON BOX
            // =================================================

            Box(
                modifier = Modifier
                    .height(52.dp)
                    .weight(0.14f)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.78f
                            ),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = symbol,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyText
                )
            }


            Spacer(
                modifier =
                    Modifier.weight(
                        0.04f
                    )
            )


            // =================================================
            // CARD TEXT
            // =================================================

            Column(
                modifier =
                    Modifier.weight(
                        0.82f
                    ),
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color = NavyText
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }
        }
    }
}