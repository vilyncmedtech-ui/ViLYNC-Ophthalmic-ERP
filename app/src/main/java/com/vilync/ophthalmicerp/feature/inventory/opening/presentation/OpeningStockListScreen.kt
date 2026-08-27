package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningStockListScreen(
    viewModel: OpeningStockViewModel,
    onBack: () -> Unit,
    onNewEntry: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRegister()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opening Stock Register") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            onClick = {
                                showExportMenu = false
                                val result = OpeningStockExportSuite.exportPdfAndShare(context, uiState.openingStocks)
                                if (result.isFailure) Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Excel") },
                            onClick = {
                                showExportMenu = false
                                val result = OpeningStockExportSuite.exportExcelAndShare(context, uiState.openingStocks)
                                if (result.isFailure) Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print") },
                            onClick = {
                                showExportMenu = false
                                val result = OpeningStockExportSuite.print(context, uiState.openingStocks)
                                if (result.isFailure) Toast.makeText(context, "Print Failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewEntry) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FD))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.isRegisterLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.openingStocks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No opening stock entries found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.openingStocks) { stock ->
                        OpeningStockCard(stock = stock, onClick = { onEditEntry(stock.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OpeningStockCard(
    stock: OpeningStockEntity,
    onClick: () -> Unit
) {
    val isDraft = stock.status.uppercase() == "DRAFT"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DEE8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stock.entryNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF14233C)
                )
                Text(
                    text = "Date: ${stock.entryDate}",
                    fontSize = 13.sp,
                    color = Color(0xFF667085)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(status = stock.status)
                
                Icon(
                    imageVector = if (isDraft) Icons.Default.Edit else Icons.Default.Visibility,
                    contentDescription = if (isDraft) "Edit" else "View",
                    tint = if (isDraft) Color(0xFF175CD3) else Color(0xFF667085),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "POSTED" -> Color(0xFFECFDF3) to Color(0xFF027A48)
        "DRAFT" -> Color(0xFFEFF8FF) to Color(0xFF175CD3)
        "CANCELLED" -> Color(0xFFFEF3F2) to Color(0xFFB42318)
        else -> Color.LightGray to Color.DarkGray
    }
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
