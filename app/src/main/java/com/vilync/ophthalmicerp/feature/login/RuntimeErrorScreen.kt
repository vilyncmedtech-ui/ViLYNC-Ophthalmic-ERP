package com.vilync.ophthalmicerp.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.database.DatabaseProvider

@Composable
fun RuntimeErrorScreen(
    message: String,
    diagnostics: String,
    report: DatabaseProvider.DatabaseHealthReport,
    onRetry: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val healthColor = when (report.healthScore) {
        DatabaseProvider.HealthScore.HEALTHY -> Color(0xFF4CAF50)
        DatabaseProvider.HealthScore.WARNING -> Color(0xFFFFC107)
        DatabaseProvider.HealthScore.CRITICAL -> Color(0xFFF44336)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = healthColor,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SYSTEM PROTECTION FAULT",
            style = MaterialTheme.typography.titleMedium,
            color = healthColor,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Health Status: ${report.healthScore}",
            style = MaterialTheme.typography.labelSmall,
            color = healthColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Technical Diagnostic Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DiagnosticRow("Database File", if (report.fileExists) "EXISTS" else "MISSING")
                DiagnosticRow("Connection", if (report.canOpen) "SUCCESS" else "FAILED")
                DiagnosticRow("Actual Version", report.versionOnDisk.toString())
                DiagnosticRow("Expected Version", report.expectedVersion.toString())
                DiagnosticRow("Users Found", report.usersCount.toString())
                DiagnosticRow("Business Records", if (report.hasBusinessData) "YES" else "NONE")
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "RAW DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = diagnostics,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )

                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(diagnostics)) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = healthColor)
        ) {
            Text(text = "RETRY BOOTSTRAP")
        }

        OutlinedButton(
            onClick = { /* Export Logic would go here */ },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = false // Future feature
        ) {
            Text(text = "EXPORT DIAGNOSTIC LOG")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Contact support immediately. Do not clear storage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
