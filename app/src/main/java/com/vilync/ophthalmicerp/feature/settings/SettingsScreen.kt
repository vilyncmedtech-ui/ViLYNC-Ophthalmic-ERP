package com.vilync.ophthalmicerp.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


// =============================================================
// SETTINGS SCREEN
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(

    onBack: () -> Unit,

    onCompanyProfileClick: () -> Unit,

    onFinancialYearClick: () -> Unit,

    onAuditTrailClick: () -> Unit,

    onBackupRestoreClick: () -> Unit

) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Settings"
                    )
                },

                navigationIcon = {

                    TextButton(
                        onClick = onBack
                    ) {

                        Text(
                            text = "Back"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)

        ) {


            // =================================================
            // HEADING
            // =================================================

            Text(

                text = "ERP Settings",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    "Manage company profile, financial year, security and ERP administration.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // =================================================
            // COMPANY PROFILE
            // =================================================

            SettingsOptionCard(

                title =
                    "Company Profile",

                description =
                    "Manage business, GST, contact, bank and printable company details.",

                onClick =
                    onCompanyProfileClick
            )


            // =================================================
            // FINANCIAL YEAR
            // =================================================

            SettingsOptionCard(

                title =
                    "Financial Year",

                description =
                    "Manage the active financial year and financial year settings.",

                onClick =
                    onFinancialYearClick
            )


            // =================================================
            // AUDIT TRAIL
            // =================================================

            SettingsOptionCard(

                title =
                    "Audit Trail",

                description =
                    "View user activity, login/logout events and ERP transaction history.",

                onClick =
                    onAuditTrailClick
            )


            // =================================================
            // BACKUP & RESTORE
            // =================================================

            SettingsOptionCard(

                title =
                    "Backup & Restore",

                description =
                    "Create, verify and manage local ERP safety backups.",

                onClick =
                    onBackupRestoreClick
            )
        }
    }
}


// =============================================================
// SETTINGS OPTION CARD
// =============================================================

@Composable
private fun SettingsOptionCard(

    title: String,

    description: String,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text = title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(

                    text = description,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            Text(

                text = "›",

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )
        }
    }
}