package com.vilync.ophthalmicerp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Common ERP report actions.
 *
 * Print opens the Android print flow. Export opens PDF and Excel choices.
 * The screen using this component supplies the actual export callbacks.
 */
@Composable
fun PrintExportActionBar(
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var exportExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onPrint) {
            Text("Print")
        }

        Box {
            Button(onClick = { exportExpanded = true }) {
                Text("Export ▾")
            }

            DropdownMenu(
                expanded = exportExpanded,
                onDismissRequest = { exportExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export PDF") },
                    onClick = {
                        exportExpanded = false
                        onExportPdf()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export Excel") },
                    onClick = {
                        exportExpanded = false
                        onExportExcel()
                    }
                )
            }
        }
    }
}
