package com.vilync.ophthalmicerp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Standard Header for Document Detail Screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardDetailHeader(
    title: String,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onPrint: () -> Unit,
    onPdf: () -> Unit,
    onShare: () -> Unit,
    onEdit: (() -> Unit)? = null, // Null if not editable
    isEditable: Boolean = true
) {
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
            IconButton(onClick = onDashboard) {
                Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
            }
            IconButton(onClick = onPrint) {
                Icon(Icons.Default.Print, contentDescription = "Print")
            }
            IconButton(onClick = onPdf) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
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
