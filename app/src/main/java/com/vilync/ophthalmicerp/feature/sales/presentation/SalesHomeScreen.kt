package com.vilync.ophthalmicerp.feature.sales.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// =============================================================
// SALES HOME
// =============================================================
//
// ViLYNC ERP module-menu standard:
//
// - Same visual language as Purchase Home
// - Two-column pastel tile grid
// - White icon block
// - Dark navy typography
// - Soft borders and rounded corners
//
// Navigation callbacks are intentionally unchanged.
// =============================================================

private val SalesPageBackground =
    Color(0xFFF7F8FC)

private val SalesNavy =
    Color(0xFF16213E)

private val SalesSecondaryText =
    Color(0xFF667085)

private val SalesTileBorder =
    Color(0xFFD6DCE8)

private val SalesBlue =
    Color(0xFFDCE8FA)

private val SalesGreen =
    Color(0xFFDDF3E6)

private val SalesPeach =
    Color(0xFFFFE4C7)

private val SalesLavender =
    Color(0xFFECE2FA)

private val SalesYellow =
    Color(0xFFFFF1C9)


@Composable
fun SalesHomeScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onInvoice: () -> Unit,
    onChallan: () -> Unit,
    onCreditNote: () -> Unit,
    onProformaInvoice: () -> Unit,
    onSampleIssue: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 28.dp,
                vertical = 18.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // =====================================================
        // TOP NAVIGATION
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            SalesTopButton(
                text = "← Back",
                onClick = onBack
            )

            SalesTopButton(
                text = "⌂ Dashboard",
                onClick = onDashboard
            )
        }


        // =====================================================
        // HEADER
        // =====================================================

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text = "Sales",
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight =
                FontWeight.Bold,
            color =
                SalesNavy
        )

        Text(
            text =
                "Manage sales transactions and customer issue documents",
            fontSize =
                14.sp,
            color =
                SalesSecondaryText
        )

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // =====================================================
        // ROW 1
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            SalesModuleTile(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Invoice",
                subtitle =
                    "New Invoice and Invoice Register",
                symbol =
                    "+",
                background =
                    SalesBlue,
                onClick =
                    onInvoice
            )

            SalesModuleTile(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Challan",
                subtitle =
                    "New Challan and Challan Register",
                symbol =
                    "▤",
                background =
                    SalesGreen,
                onClick =
                    onChallan
            )
        }


        // =====================================================
        // ROW 2
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            SalesModuleTile(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Credit Note",
                subtitle =
                    "New Credit Note and Credit Note Register",
                symbol =
                    "↩",
                background =
                    SalesPeach,
                onClick =
                    onCreditNote
            )

            SalesModuleTile(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Proforma Invoice",
                subtitle =
                    "New Proforma and Proforma Register",
                symbol =
                    "▦",
                background =
                    SalesLavender,
                onClick =
                    onProformaInvoice
            )
        }


        // =====================================================
        // ROW 3
        // =====================================================
        //
        // Five Sales actions currently exist, therefore the
        // fifth tile remains half-width on the left so the same
        // two-column Purchase-style grid rhythm is preserved.
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            SalesModuleTile(
                modifier =
                    Modifier.weight(1f),
                title =
                    "Sample Issue",
                subtitle =
                    "New Sample Issue and Sample Issue Register",
                symbol =
                    "S",
                background =
                    SalesYellow,
                onClick =
                    onSampleIssue
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}


// =============================================================
// TOP BUTTON
// =============================================================

@Composable
private fun SalesTopButton(
    text: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier.clickable(
                onClick = onClick
            ),
        shape =
            RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = SalesTileBorder
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                ),
            fontSize =
                14.sp,
            fontWeight =
                FontWeight.Medium,
            color =
                SalesNavy
        )
    }
}


// =============================================================
// SALES MODULE TILE
// =============================================================

@Composable
private fun SalesModuleTile(
    modifier: Modifier,
    title: String,
    subtitle: String,
    symbol: String,
    background: Color,
    onClick: () -> Unit
) {

    Card(
        modifier =
            modifier
                .height(146.dp)
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = SalesTileBorder
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 20.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // -------------------------------------------------
            // WHITE ICON BLOCK
            // -------------------------------------------------

            Card(
                modifier =
                    Modifier
                        .height(64.dp)
                        .fillMaxWidth(0.14f),
                shape =
                    RoundedCornerShape(14.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(
                                alpha = 0.88f
                            )
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = symbol,
                        fontSize =
                            24.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            SalesNavy
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth(0.035f)
            )


            // -------------------------------------------------
            // LABELS
            // -------------------------------------------------

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = title,
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        SalesNavy
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = subtitle,
                    fontSize =
                        13.sp,
                    lineHeight =
                        18.sp,
                    color =
                        SalesSecondaryText
                )
            }
        }
    }
}
