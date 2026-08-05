package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.reports.domain.ReportSchema
import com.vilync.ophthalmicerp.core.reports.export.UniversalExportSuite
import com.vilync.ophthalmicerp.core.reports.presentation.UniversalReportUiState

@Composable
fun ReportHeader(
    schema: ReportSchema,
    uiState: UniversalReportUiState,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onSaveFavourite: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back
            Card(
                modifier = Modifier.size(40.dp).clickable { onBack() },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color(0xFF344054))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center: Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(text = schema.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                Text(text = schema.subtitle, fontSize = 12.sp, color = Color(0xFF667085))
            }

            // Right: Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSaveFavourite,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D5DD)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save as Favorite", color = Color(0xFF344054), fontSize = 13.sp)
                }
                
                FilledTonalButton(
                    onClick = onDashboard,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEEF4FF)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF3538CD))
                    Spacer(Modifier.width(8.dp))
                    Text("Dashboard", color = Color(0xFF3538CD), fontSize = 13.sp)
                }

                Box {
                    Button(
                        onClick = { showExportMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14233C)),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.width(160.dp)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Print", fontSize = 14.sp) },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.print(context, schema, uiState.filteredRows, uiState.visibleColumnIds)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("PDF", fontSize = 14.sp) },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.exportPdfAndShare(context, schema, uiState.filteredRows, uiState.visibleColumnIds)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Excel", fontSize = 14.sp) },
                            onClick = {
                                showExportMenu = false
                                UniversalExportSuite.exportExcelAndShare(context, schema, uiState.filteredRows, uiState.visibleColumnIds)
                            }
                        )
                    }
                }

                IconButton(onClick = { /* More Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF667085))
                }
            }
        }
    }
}
