package com.vilync.ophthalmicerp.feature.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    viewModel: BackupRestoreViewModel = viewModel()
) {

    val uiState by
    viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Restore"
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

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text = "ERP Data Protection",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Create and verify local safety backups of the complete ViLYNC ERP database.",
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

            item {

                BackupStatusCard(
                    uiState = uiState
                )
            }

            item {

                Button(
                    onClick = {
                        viewModel.createBackup()
                    },
                    enabled =
                        !uiState.isCreatingBackup,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (uiState.isCreatingBackup) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            text = "BACKUP NOW"
                        )
                    }
                }
            }

            uiState.message?.let { message ->

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Text(
                                text =
                                    if (uiState.isError) {
                                        "Backup Error"
                                    } else {
                                        "Backup Status"
                                    },
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text = message,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )

                            TextButton(
                                onClick = {
                                    viewModel.clearMessage()
                                }
                            ) {
                                Text(
                                    text = "Dismiss"
                                )
                            }
                        }
                    }
                }
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Backup History",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            viewModel.refreshBackupHistory()
                        },
                        enabled =
                            !uiState.isLoading &&
                                    !uiState.isCreatingBackup
                    ) {
                        Text(
                            text = "Refresh"
                        )
                    }
                }
            }

            if (
                uiState.isLoading &&
                uiState.backups.isEmpty()
            ) {

                item {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

            } else if (
                uiState.backups.isEmpty()
            ) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "No local ERP backups have been created yet.",
                            modifier =
                                Modifier.padding(16.dp),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }

            } else {

                items(
                    items = uiState.backups,
                    key = { backup ->
                        backup.file.absolutePath
                    }
                ) { backup ->

                    BackupHistoryCard(
                        backup = backup,
                        onValidate = {
                            viewModel.validateBackup(
                                backup
                            )
                        },
                        validationEnabled =
                            !uiState.isLoading &&
                                    !uiState.isCreatingBackup
                    )
                }
            }

            item {

                RestoreFoundationCard()
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BackupStatusCard(
    uiState: BackupRestoreUiState
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Backup Status",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            val latest =
                uiState.lastSuccessfulBackup

            if (latest == null) {

                Text(
                    text = "Last successful backup: None"
                )

                Text(
                    text =
                        "Database version: ${BackupManager.CURRENT_DATABASE_VERSION}"
                )

            } else {

                Text(
                    text =
                        "Last successful backup: ${formatDateTime(latest.createdAt)}"
                )

                Text(
                    text =
                        "File: ${latest.fileName}"
                )

                Text(
                    text =
                        "Size: ${formatFileSize(latest.sizeBytes)}"
                )

                Text(
                    text =
                        "Database version: ${BackupManager.CURRENT_DATABASE_VERSION}"
                )
            }
        }
    }
}

@Composable
private fun BackupHistoryCard(
    backup: BackupManager.BackupInfo,
    onValidate: () -> Unit,
    validationEnabled: Boolean
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = backup.fileName,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    formatDateTime(
                        backup.createdAt
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    "Size: ${formatFileSize(backup.sizeBytes)}",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    if (backup.isValid) {
                        "Status: Available"
                    } else {
                        "Status: Validation required"
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            backup.checksumSha256
                ?.takeIf { checksum ->
                    checksum.isNotBlank()
                }
                ?.let { checksum ->

                    Text(
                        text =
                            "SHA-256: ${checksum.take(16)}…",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

            OutlinedButton(
                onClick = onValidate,
                enabled =
                    validationEnabled
            ) {
                Text(
                    text = "VERIFY BACKUP"
                )
            }
        }
    }
}

@Composable
private fun RestoreFoundationCard() {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Restore",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Restore will be activated after Backup Now and backup integrity are runtime verified.",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Text(
                text =
                    "The restore phase will validate the selected backup and create a pre-restore safety backup before replacing current ERP data.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            OutlinedButton(
                onClick = {},
                enabled = false
            ) {
                Text(
                    text = "RESTORE BACKUP"
                )
            }
        }
    }
}

private fun formatDateTime(
    timestamp: Long
): String {

    return DateFormat
        .getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )
        .format(
            Date(timestamp)
        )
}

private fun formatFileSize(
    bytes: Long
): String {

    if (bytes < 1024L) {
        return "$bytes B"
    }

    val kilobytes =
        bytes / 1024.0

    if (kilobytes < 1024.0) {
        return String.format(
            "%.1f KB",
            kilobytes
        )
    }

    val megabytes =
        kilobytes / 1024.0

    return String.format(
        "%.2f MB",
        megabytes
    )
}
