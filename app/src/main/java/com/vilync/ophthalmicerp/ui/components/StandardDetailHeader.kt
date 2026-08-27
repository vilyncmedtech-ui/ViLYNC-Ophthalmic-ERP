package com.vilync.ophthalmicerp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Standard Header for Document Detail Screens.
 * 
 * Provides a consistent layout: [Back] [Title] ... [Dashboard] [Export ▼] [Edit]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardDetailHeader(
    title: String,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onPrint: () -> Unit,
    onPdf: () -> Unit,
    onExcel: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null, // Null if not editable
    isEditable: Boolean = true
) {
    var exportExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Standard action sequence
            IconButton(onClick = onDashboard) {
                Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
            }

            // Consolidated Export Menu
            Box {
                TextButton(onClick = { exportExpanded = true }) {
                    Text(
                        text = "Export ▼", 
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF071B33)
                    )
                }

                DropdownMenu(
                    expanded = exportExpanded,
                    onDismissRequest = { exportExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Print") },
                        leadingIcon = { Icon(Icons.Default.Print, null) },
                        onClick = {
                            exportExpanded = false
                            onPrint()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                        onClick = {
                            exportExpanded = false
                            onPdf()
                        }
                    )
                    if (onExcel != null) {
                        DropdownMenuItem(
                            text = { Text("Excel") },
                            leadingIcon = { Icon(Icons.Default.TableChart, null) },
                            onClick = {
                                exportExpanded = false
                                onExcel()
                            }
                        )
                    }
                }
            }
          // Edit action if supported
            if (onEdit != null && isEditable) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color(0xFF071B33),
            navigationIconContentColor = Color(0xFF071B33),
            actionIconContentColor = Color(0xFF071B33)
        )
    )
}
