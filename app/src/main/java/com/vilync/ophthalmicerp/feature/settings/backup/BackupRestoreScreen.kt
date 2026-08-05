package com.vilync.ophthalmicerp.feature.settings.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.vilync.ophthalmicerp.feature.backup.model.GoogleAuthState
import com.vilync.ophthalmicerp.feature.backup.presentation.GoogleBackupUiState
import com.vilync.ophthalmicerp.feature.backup.presentation.GoogleBackupViewModel
import com.vilync.ophthalmicerp.feature.backup.logic.BackupIntegrityVerifier
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    viewModel: GoogleBackupViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.showRollbackMessage) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRollbackMessage() },
            title = { Text("System Recovery") },
            text = { Text("A previous restore operation failed validation. The previous production database has been automatically restored successfully.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRollbackMessage() }) { Text("OK") }
            }
        )
    }

    if (uiState.restoreSummary != null) {
        RestoreSummaryDialog(
            summary = uiState.restoreSummary!!,
            onConfirm = {
                uiState.cloudBackups.find { it.driveFileId != null }?.let {
                    viewModel.executeRestore(it) {
                        exitProcess(0)
                    }
                }
            },
            onDismiss = { viewModel.dismissSummary() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cloud Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isOperationInProgress) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountStatusCard(
                authState = uiState.authState,
                backupFolderName = uiState.backupFolderName,
                onSignIn = { 
                    val activity = context.findActivity()
                    if (activity != null) {
                        viewModel.signIn(activity)
                    }
                }
            )

            BackupStatusOverview(uiState)

            if (uiState.authState is GoogleAuthState.Authenticated) {
                CloudBackupsSection(
                    backups = uiState.cloudBackups,
                    onRestoreClick = { viewModel.prepareRestore(it) }
                )
            }

            TechnicalDetailsCard(uiState.databaseVersion)

            Spacer(modifier = Modifier.weight(1f))

            ActionSection(
                isOperationInProgress = uiState.isOperationInProgress,
                isAuth = uiState.authState is GoogleAuthState.Authenticated,
                onBackupClick = { viewModel.performBackupNow() },
                onChangeAccount = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        viewModel.signIn(activity)
                    }
                },
                onDisconnect = { viewModel.signOut() }
            )
        }
    }
}

@Composable
private fun CloudBackupsSection(
    backups: List<com.vilync.ophthalmicerp.feature.backup.data.BackupMetadataEntity>,
    onRestoreClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AVAILABLE CLOUD BACKUPS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF345FA8)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            if (backups.isEmpty()) {
                Text("No cloud backups found.", fontSize = 13.sp, color = Color.Gray)
            } else {
                backups.forEach { backup ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = formatTime(backup.timestamp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Size: ${formatFileSize(backup.fileSize)} • DB v${backup.dbVersion}", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(onClick = { backup.driveFileId?.let { onRestoreClick(it) } }) {
                            Text("RESTORE")
                        }
                    }
                    if (backup != backups.last()) HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun RestoreSummaryDialog(
    summary: BackupIntegrityVerifier.RestoreSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Confirmation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Please verify the backup content before overwriting your current data:")
                
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        SummaryDetail("Company", summary.companyName)
                        SummaryDetail("GST", summary.companyGst)
                        SummaryDetail("DB Version", summary.dbVersion.toString())
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryDetail("Users", summary.usersCount.toString())
                        SummaryDetail("Products", summary.productsCount.toString())
                        SummaryDetail("Sales", summary.salesCount.toString())
                        SummaryDetail("Purchases", summary.purchasesCount.toString())
                    }
                }
                
                Text(
                    "WARNING: All current unsaved work will be lost. The app will restart after restore.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))) {
                Text("RESTORE & RESTART")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
private fun SummaryDetail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccountStatusCard(
    authState: GoogleAuthState,
    backupFolderName: String,
    onSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (authState is GoogleAuthState.Authenticated) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = if (authState is GoogleAuthState.Authenticated) Color(0xFF4CAF50) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = when (authState) {
                            is GoogleAuthState.Authenticated -> authState.email
                            is GoogleAuthState.Authenticating -> "Connecting..."
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (authState !is GoogleAuthState.Authenticated) {
                    Button(
                        onClick = onSignIn,
                        enabled = authState !is GoogleAuthState.Authenticating,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }
            
            if (authState is GoogleAuthState.Authenticated) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Backup Folder:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = backupFolderName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BackupStatusOverview(uiState: GoogleBackupUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BACKUP STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF345FA8)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            StatusRow(
                label = "Current Sync Status",
                value = if (uiState.isOperationInProgress) "Syncing..." else "Idle",
                icon = Icons.Default.Sync,
                iconColor = if (uiState.isOperationInProgress) Color(0xFF2196F3) else Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

            StatusRow(
                label = "Last Successful Backup",
                value = uiState.lastSuccess?.let { formatTime(it.timestamp) } ?: "Never",
                icon = Icons.Default.CheckCircle,
                iconColor = if (uiState.lastSuccess != null) Color(0xFF4CAF50) else Color.Gray
            )

            if (uiState.lastSuccess != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(start = 30.dp)) {
                    Text(text = "Size: ${formatFileSize(uiState.lastSuccess.fileSize)}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StatusRow(
                label = "Last Failed Attempt",
                value = uiState.lastFailed?.let { formatTime(it.timestamp) } ?: "None",
                icon = Icons.Default.Error,
                iconColor = if (uiState.lastFailed != null) Color(0xFFF44336) else Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

            StatusRow(
                label = "Next Automatic Backup",
                value = "Daily at night",
                icon = Icons.Default.Schedule,
                iconColor = Color.Gray
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconColor)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TechnicalDetailsCard(dbVersion: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Database Version: $dbVersion", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Cloud Sync: ENABLED", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionSection(
    isOperationInProgress: Boolean,
    isAuth: Boolean,
    onBackupClick: () -> Unit,
    onChangeAccount: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isAuth) {
            Button(
                onClick = onBackupClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isOperationInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF345FA8))
            ) {
                if (isOperationInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Backup Now", fontWeight = FontWeight.Bold)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { /* Restore is handled by list items, but we can make this button scroll to them */ },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isOperationInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF027A48))
                ) {
                    Text("Restore")
                }

                OutlinedButton(
                    onClick = onChangeAccount,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isOperationInProgress
                ) {
                    Text("Change Account")
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isOperationInProgress,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Disconnect")
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
