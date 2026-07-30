package com.vilync.ophthalmicerp.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DashboardScreen(

    // =========================================================
    // SALES
    // =========================================================

    onSalesClick: () -> Unit = {},

    onNewSaleClick: () -> Unit = {},


    // =========================================================
    // PURCHASE
    // =========================================================

    onPurchaseClick: () -> Unit = {},

    onNewPurchaseClick: () -> Unit = {},


    // =========================================================
    // OTHER MODULES
    // =========================================================

    onInventoryClick: () -> Unit = {},

    onMasterClick: () -> Unit = {},

    onGstClick: () -> Unit = {},

    onSettingsClick: () -> Unit = {},

    onLogoutClick: () -> Unit = {},

    activeFinancialYear: String = ""
) {


    // =========================================================
    // MODULES
    // =========================================================

    val modules =
        listOf(

            ModuleItem(
                "₹",
                "Sales",
                Color(0xFFD7F7E4),
                Color(0xFF14945A)
            ),

            ModuleItem(
                "↓",
                "Purchase",
                Color(0xFFFFECD8),
                Color(0xFFE57A1F)
            ),

            ModuleItem(
                "▣",
                "Inventory",
                Color(0xFFEDE6FF),
                Color(0xFF7D57D1)
            ),

            ModuleItem(
                "M",
                "Master",
                Color(0xFFE7F0FF),
                Color(0xFF3455A4)
            ),

            ModuleItem(
                "₹",
                "Payments",
                Color(0xFFFFF2C7),
                Color(0xFFC98A00)
            ),

            ModuleItem(
                "G",
                "GST",
                Color(0xFFFFE2EB),
                Color(0xFFD9346B)
            ),

            ModuleItem(
                "▥",
                "Reports",
                Color(0xFFE4F6E7),
                Color(0xFF348A4A)
            )
        )


    // =========================================================
    // BUSINESS OVERVIEW
    // =========================================================

    val overview =
        listOf(

            OverviewItem(
                "Outstanding",
                "₹0"
            ),

            OverviewItem(
                "Pending Challans",
                "0"
            ),

            OverviewItem(
                "Pending Samples",
                "0"
            ),

            OverviewItem(
                "Low Stock",
                "0"
            ),

            OverviewItem(
                "Expiry Alerts",
                "0"
            )
        )


    // =========================================================
    // QUICK ACTIONS
    // =========================================================

    val quickActions =
        listOf(

            QuickActionItem(
                "New Purchase"
            ),

            QuickActionItem(
                "New Invoice"
            ),

            QuickActionItem(
                "New Sample"
            ),

            QuickActionItem(
                "New Challan"
            ),

            QuickActionItem(
                "Global Search"
            )
        )


    // =========================================================
    // PAGE
    // =========================================================

    BoxWithConstraints(

        modifier =
            Modifier
                .fillMaxSize()
                .background(

                    Brush.verticalGradient(

                        colors =
                            listOf(
                                Color(0xFFF8FAFF),
                                Color(0xFFF4F7FF)
                            )
                    )
                )
    ) {


        val isTabletLandscape =
            maxWidth >= 900.dp


        val moduleColumns =
            if (isTabletLandscape) {
                4
            } else {
                3
            }


        val overviewColumns =
            if (isTabletLandscape) {
                3
            } else {
                2
            }


        val quickActionColumns =
            if (isTabletLandscape) {
                3
            } else {
                2
            }


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
        ) {


            // =================================================
            // HEADER
            // =================================================

            HeaderCard(

                activeFinancialYear =
                    activeFinancialYear,

                onSettingsClick =
                    onSettingsClick,

                onLogoutClick =
                    onLogoutClick
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            // =================================================
            // MODULES
            // =================================================

            SectionTitle(
                "Modules"
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            GridRows(

                items =
                    modules,

                columns =
                    moduleColumns,

                horizontalGap =
                    10.dp,

                verticalGap =
                    8.dp

            ) { module ->


                ModuleCard(

                    item =
                        module,

                    onClick = {

                        when (
                            module.title
                        ) {


                            // =================================
                            // SALES
                            // =================================

                            "Sales" -> {

                                onSalesClick()
                            }


                            // =================================
                            // PURCHASE
                            // =================================

                            "Purchase" -> {

                                onPurchaseClick()
                            }


                            // =================================
                            // INVENTORY
                            // =================================

                            "Inventory" -> {

                                onInventoryClick()
                            }


                            // =================================
                            // MASTER
                            // =================================

                            "Master" -> {

                                onMasterClick()
                            }


                            // =================================
                            // GST
                            // =================================

                            "GST" -> {

                                onGstClick()
                            }
                        }
                    }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            // =================================================
            // BUSINESS OVERVIEW
            // =================================================

            SectionTitle(
                "Business Overview"
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            GridRows(

                items =
                    overview,

                columns =
                    overviewColumns,

                horizontalGap =
                    10.dp,

                verticalGap =
                    8.dp

            ) { item ->


                OverviewCard(
                    item =
                        item
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            // =================================================
            // QUICK ACTIONS
            // =================================================

            SectionTitle(
                "Quick Actions"
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            GridRows(

                items =
                    quickActions,

                columns =
                    quickActionColumns,

                horizontalGap =
                    10.dp,

                verticalGap =
                    8.dp

            ) { item ->


                QuickActionCard(

                    title =
                        item.title,

                    onClick = {

                        when (
                            item.title
                        ) {


                            // =================================
                            // NEW PURCHASE
                            // =================================

                            "New Purchase" -> {

                                onNewPurchaseClick()
                            }


                            // =================================
                            // NEW SALES INVOICE
                            // =================================

                            "New Invoice" -> {

                                onNewSaleClick()
                            }
                        }
                    }
                )
            }
        }
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun HeaderCard(

    activeFinancialYear: String,

    onSettingsClick: () -> Unit,

    onLogoutClick: () -> Unit
) {


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    Color.White.copy(
                        alpha = 0.95f
                    )
            ),

        elevation =
            CardDefaults.cardElevation(

                defaultElevation =
                    2.dp
            )
    ) {


        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {


                Text(

                    text =
                        "ViLYNC Ophthalmic ERP",

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(
                            0xFF1F2530
                        )
                )


                Text(

                    text =
                        "Smart billing, purchase and stock control",

                    fontSize =
                        12.sp,

                    color =
                        Color(
                            0xFF6B7280
                        )
                )
            }


            // =================================================
            // ACTIVE FINANCIAL YEAR
            // =================================================

            if (
                activeFinancialYear
                    .isNotBlank()
            ) {


                Card(

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),

                    colors =
                        CardDefaults
                            .cardColors(

                                containerColor =
                                    Color(
                                        0xFFE8F0FE
                                    )
                            )
                ) {


                    Text(

                        text =
                            "FY $activeFinancialYear",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight
                                .SemiBold,

                        color =
                            Color(
                                0xFF3455A4
                            ),

                        modifier =
                            Modifier.padding(

                                horizontal =
                                    12.dp,

                                vertical =
                                    8.dp
                            )
                    )
                }


                Spacer(

                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )
            }


            // =================================================
            // SETTINGS
            // =================================================

            IconButton(

                onClick =
                    onSettingsClick
            ) {


                Text(

                    text =
                        "⚙",

                    fontSize =
                        22.sp,

                    color =
                        Color(
                            0xFF4B5563
                        )
                )
            }


            Spacer(

                modifier =
                    Modifier.width(
                        2.dp
                    )
            )


            // =================================================
            // LOGOUT
            // =================================================

            Button(

                onClick =
                    onLogoutClick,

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                Color(
                                    0xFF4569AB
                                ),

                            contentColor =
                                Color.White
                        ),

                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            ) {


                Text(

                    text =
                        "Logout",

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight
                            .SemiBold
                )
            }
        }
    }
}


// =============================================================
// MODELS
// =============================================================

data class ModuleItem(

    val icon: String,

    val title: String,

    val backgroundColor: Color,

    val iconColor: Color
)


data class OverviewItem(

    val title: String,

    val value: String
)


data class QuickActionItem(

    val title: String
)


// =============================================================
// SECTION TITLE
// =============================================================

@Composable
fun SectionTitle(
    title: String
) {


    Text(

        text =
            title,

        fontSize =
            17.sp,

        fontWeight =
            FontWeight
                .SemiBold,

        color =
            Color(
                0xFF252A33
            )
    )
}


// =============================================================
// GENERIC GRID
// =============================================================

@Composable
private fun <T> GridRows(

    items: List<T>,

    columns: Int,

    horizontalGap: Dp,

    verticalGap: Dp,

    itemContent:
    @Composable (T) -> Unit
) {


    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                verticalGap
            )
    ) {


        items
            .chunked(
                columns
            )
            .forEach {
                    rowItems ->


                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement
                            .spacedBy(
                                horizontalGap
                            )
                ) {


                    rowItems
                        .forEach {
                                item ->


                            Box(

                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {


                                itemContent(
                                    item
                                )
                            }
                        }


                    repeat(
                        columns -
                                rowItems.size
                    ) {


                        Spacer(

                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
                        )
                    }
                }
            }
    }
}


// =============================================================
// MODULE CARD
// =============================================================

@Composable
fun ModuleCard(

    item: ModuleItem,

    onClick: () -> Unit = {}
) {


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    72.dp
                )
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults
                .cardColors(

                    containerColor =
                        item.backgroundColor
                ),

        elevation =
            CardDefaults
                .cardElevation(

                    defaultElevation =
                        2.dp
                )
    ) {


        Row(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp
                    ),

            verticalAlignment =
                Alignment
                    .CenterVertically
        ) {


            Box(

                modifier =
                    Modifier
                        .size(
                            38.dp
                        )
                        .background(

                            color =
                                Color.White
                                    .copy(
                                        alpha =
                                            0.82f
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

                    text =
                        item.icon,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        item.iconColor
                )
            }


            Spacer(

                modifier =
                    Modifier.width(
                        10.dp
                    )
            )


            Text(

                text =
                    item.title,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight
                        .SemiBold,

                color =
                    Color(
                        0xFF252A33
                    ),

                maxLines =
                    1
            )
        }
    }
}


// =============================================================
// OVERVIEW CARD
// =============================================================

@Composable
fun OverviewCard(
    item: OverviewItem
) {


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    82.dp
                ),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults
                .cardColors(

                    containerColor =
                        Color(
                            0xFFE6ECFF
                        )
                ),

        elevation =
            CardDefaults
                .cardElevation(

                    defaultElevation =
                        1.dp
                )
    ) {


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        12.dp
                    ),

            verticalArrangement =
                Arrangement.Center
        ) {


            Text(

                text =
                    item.title,

                fontSize =
                    12.sp,

                color =
                    Color(
                        0xFF506080
                    ),

                maxLines =
                    1
            )


            Spacer(

                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(

                text =
                    item.value,

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(
                        0xFF1F2F57
                    )
            )
        }
    }
}


// =============================================================
// QUICK ACTION CARD
// =============================================================

@Composable
fun QuickActionCard(

    title: String,

    onClick: () -> Unit = {}
) {


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    74.dp
                )
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults
                .cardColors(

                    containerColor =
                        Color(
                            0xFFFFE8CC
                        )
                ),

        elevation =
            CardDefaults
                .cardElevation(

                    defaultElevation =
                        1.dp
                )
    ) {


        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp
                    ),

            contentAlignment =
                Alignment
                    .CenterStart
        ) {


            Text(

                text =
                    title,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight
                        .SemiBold,

                color =
                    Color(
                        0xFF6E4318
                    ),

                maxLines =
                    1
            )
        }
    }
}