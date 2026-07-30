package com.vilync.ophthalmicerp.feature.gst.presentation

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GstHomeScreen(
    onBack: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onGstDashboardClick: () -> Unit = {},
    onGstReportClick: (String) -> Unit = {}
) {

    val modules =
        listOf(

            GstModuleItem(
                symbol = "G",
                title = "GST Dashboard",
                routeKey = "dashboard",
                background = Color(0xFFDDF3FF),
                accent = Color(0xFF2867B2)
            ),

            GstModuleItem(
                symbol = "1",
                title = "GSTR-1",
                routeKey = "gstr1",
                background = Color(0xFFDDF5E5),
                accent = Color(0xFF27945A)
            ),

            GstModuleItem(
                symbol = "3B",
                title = "GSTR-3B Working",
                routeKey = "gstr3b",
                background = Color(0xFFEDE4FF),
                accent = Color(0xFF7652C7)
            ),

            GstModuleItem(
                symbol = "S",
                title = "Sales GST Register",
                routeKey = "sales_register",
                background = Color(0xFFFFE8D3),
                accent = Color(0xFFD97A20)
            ),

            GstModuleItem(
                symbol = "P",
                title = "Purchase GST Register",
                routeKey = "purchase_register",
                background = Color(0xFFDDF4F5),
                accent = Color(0xFF159A9C)
            ),

            GstModuleItem(
                symbol = "H",
                title = "HSN Summary",
                routeKey = "hsn",
                background = Color(0xFFFFF1C9),
                accent = Color(0xFFB8860B)
            ),

            GstModuleItem(
                symbol = "B2B",
                title = "B2B Sales",
                routeKey = "b2b",
                background = Color(0xFFFFE0EA),
                accent = Color(0xFFC83E72)
            ),

            GstModuleItem(
                symbol = "B2C",
                title = "B2C Sales",
                routeKey = "b2c",
                background = Color(0xFFE0EDFF),
                accent = Color(0xFF3272B8)
            ),

            GstModuleItem(
                symbol = "R",
                title = "Returns / Adjustments",
                routeKey = "returns",
                background = Color(0xFFFFE3E3),
                accent = Color(0xFFC84B4B)
            ),

            GstModuleItem(
                symbol = "ITC",
                title = "ITC Summary",
                routeKey = "itc",
                background = Color(0xFFE2F4E6),
                accent = Color(0xFF318A4D)
            ),

            GstModuleItem(
                symbol = "₹",
                title = "Output Tax Liability",
                routeKey = "output_tax",
                background = Color(0xFFDDF3F4),
                accent = Color(0xFF178B92)
            ),

            GstModuleItem(
                symbol = "T",
                title = "CGST / SGST / IGST Summary",
                routeKey = "tax_heads",
                background = Color(0xFFECE4FF),
                accent = Color(0xFF694FB0)
            )
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF7F9FF)
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                )
        ) {

            GstHomeHeader(
                onBack = onBack,
                onDashboard = onDashboard
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text = "GST Module",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "GST compliance, registers and tax working",
                fontSize = 13.sp,
                color = Color(0xFF6D7480)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(4),

                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = modules,
                    key = { it.title }
                ) { item ->

                    GstModuleTile(
                        item = item,
                        onClick = {

                            if (
                                item.routeKey ==
                                "dashboard"
                            ) {

                                onGstDashboardClick()

                            } else {

                                onGstReportClick(
                                    item.routeKey
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GstHomeHeader(
    onBack: () -> Unit,
    onDashboard: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 9.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFE8F0FE),

                        contentColor =
                            Color(0xFF345FA8)
                    ),

                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "Back",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text = "GST",

                modifier =
                    Modifier.weight(1f),

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(0xFF20242C)
            )

            Button(
                onClick = onDashboard,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF456FB5),

                        contentColor =
                            Color.White
                    ),

                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "Dashboard",

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GstModuleTile(
    item: GstModuleItem,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .clickable(
                onClick = onClick
            ),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    item.background
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),

            verticalArrangement =
                Arrangement.Center,

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color =
                            item.accent.copy(
                                alpha = 0.12f
                            ),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = item.symbol,
                    color = item.accent,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(9.dp)
            )

            Text(
                text = item.title,
                color = Color(0xFF30343B),
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

private data class GstModuleItem(
    val symbol: String,
    val title: String,
    val routeKey: String,
    val background: Color,
    val accent: Color
)