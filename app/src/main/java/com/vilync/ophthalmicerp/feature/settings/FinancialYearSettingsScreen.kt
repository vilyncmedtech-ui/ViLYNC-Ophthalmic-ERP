package com.vilync.ophthalmicerp.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.financialyear.FinancialYear
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearViewModel

@Composable
fun FinancialYearSettingsScreen(
    viewModel: FinancialYearViewModel,
    onBack: () -> Unit
) {

    val activeFinancialYear by
    viewModel.activeFinancialYear.collectAsState()

    val availableFinancialYears =
        viewModel.availableFinancialYears

    var selectedFinancialYear by remember(
        activeFinancialYear
    ) {
        mutableStateOf(activeFinancialYear)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            )
    ) {

        // =====================================================
        // HEADER + ACTIVE FY
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Financial Year",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select ERP working financial year",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }


            // ACTIVE FY COMPACT CARD

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F0FE)
                )
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Active FY",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = activeFinancialYear.displayName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3455A4)
                    )
                }
            }


            Spacer(
                modifier = Modifier.width(14.dp)
            )


            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(44.dp)
            ) {

                Text(
                    text = "Back",
                    fontSize = 14.sp
                )
            }
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // AVAILABLE FY TITLE
        // =====================================================

        Text(
            text = "Available Financial Years",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // =====================================================
        // FINANCIAL YEARS - HORIZONTAL
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            availableFinancialYears
                .sortedByDescending {
                    it.startYear
                }
                .forEach { financialYear ->

                    FinancialYearOption(
                        financialYear = financialYear,
                        selected =
                            financialYear ==
                                    selectedFinancialYear,
                        onClick = {
                            selectedFinancialYear =
                                financialYear
                        }
                    )
                }
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // BOTTOM ACTION AREA
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF4F5F8)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Selected Financial Year",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Text(
                        text =
                            selectedFinancialYear.displayName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3455A4)
                    )
                }


                Text(
                    text = "Indian FY: 1 April - 31 March",
                    fontSize = 12.sp,
                    color = Color.Gray
                )


                Spacer(
                    modifier = Modifier.width(20.dp)
                )


                Button(
                    onClick = {

                        viewModel.changeFinancialYear(
                            selectedFinancialYear
                        )
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3455A4),
                        contentColor = Color.White
                    )
                ) {

                    Text(
                        text = "Apply Financial Year",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
private fun FinancialYearOption(
    financialYear: FinancialYear,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .width(190.dp)
            .height(70.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    Color(0xFFE8F0FE)
                } else {
                    Color(0xFFE7E8ED)
                }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = onClick
            )


            Spacer(
                modifier = Modifier.width(6.dp)
            )


            Text(
                text = financialYear.displayName,
                fontSize = 16.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
            )


            if (selected) {

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3455A4)
                )
            }
        }
    }
}