package com.vilync.ophthalmicerp.feature.sales.register

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF071B33)
private val Gold = Color(0xFFD4AF37)
private val Page = Color(0xFFF7F9FC)

@Composable
fun SalesRegisterScreen(
    viewModel: SalesRegisterViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onViewInvoice: (Long) -> Unit = {},
    onEditInvoice: (Long) -> Unit = {}
) {
    val state = viewModel.uiState.collectAsState().value
    val actionMessage = viewModel.actionMessage.collectAsState().value
    val isActionRunning = viewModel.isActionRunning.collectAsState().value

    val context = LocalContext.current
    var exportOpen by remember { mutableStateOf(false) }
    var selectedRow by remember { mutableStateOf<SalesRegisterRow?>(null) }
    var showRowActions by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancellationReason by remember { mutableStateOf("") }

    val money = remember {
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
            ).show()

            viewModel.clearActionMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            Text(
                viewModel.registerType.screenTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Button(
                        onClick = {
                            exportOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy
                        )
                    ) {
                        Text("EXPORT ▾")
                    }

                    DropdownMenu(
                        expanded = exportOpen,
                        onDismissRequest = {
                            exportOpen = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Print")
                            },
                            onClick = {
                                exportOpen = false

                                SalesRegisterExportSuite
                                    .print(
                                        context,
                                        viewModel.registerType.screenTitle,
                                        state.rows
                                    )
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "Unable to print.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("PDF")
                            },
                            onClick = {
                                exportOpen = false

                                SalesRegisterExportSuite
                                    .exportPdfAndShare(
                                        context,
                                        viewModel.registerType.screenTitle,
                                        state.rows
                                    )
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "Unable to export PDF.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Excel")
                            },
                            onClick = {
                                exportOpen = false

                                SalesRegisterExportSuite
                                    .exportExcelAndShare(
                                        context,
                                        viewModel.registerType.screenTitle,
                                        state.rows
                                    )
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "Unable to export Excel.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDashboard
                ) {
                    Text("⌂ Dashboard")
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                label = {
                    Text("Search document / customer / date / status")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.82f)
            )

            OutlinedButton(
                onClick = viewModel::refresh
            ) {
                Text("Refresh")
            }
        }

        val statuses = remember(
            state.rows,
            state.statusFilter
        ) {
            listOf("ALL") +
                    state.rows
                        .map { it.status }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statuses.forEach { status ->
                FilterChip(
                    selected =
                        state.statusFilter == status,
                    onClick = {
                        viewModel.updateStatusFilter(
                            status
                        )
                    },
                    label = {
                        Text(
                            status.replace(
                                '_',
                                ' '
                            )
                        )
                    }
                )
            }
        }

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> {
                Text(
                    state.errorMessage!!,
                    color =
                        MaterialTheme.colorScheme.error
                )
            }

            state.rows.isEmpty() -> {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Page
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            Gold.copy(alpha = .45f)
                        )
                ) {
                    Text(
                        "No records found.",
                        Modifier.padding(20.dp),
                        color = Navy
                    )
                }
            }

            else -> {
                state.rows.forEach { row ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (
                                            viewModel.registerType ==
                                            SalesRegisterType.INVOICE
                                        ) {
                                            onViewInvoice(row.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectedRow = row
                                        showRowActions = true
                                    }
                                ),
                        shape =
                            RoundedCornerShape(14.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            ),
                        border =
                            BorderStroke(
                                1.dp,
                                Color(0xFFDDE3EC)
                            )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column(
                                Modifier.fillMaxWidth(0.72f),
                                verticalArrangement =
                                    Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    row.documentNumber,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = Navy
                                )

                                Text(
                                    "${row.documentDate}  •  ${row.customerName}"
                                )

                                if (
                                    row.secondaryInfo
                                        .isNotBlank()
                                ) {
                                    Text(
                                        row.secondaryInfo,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {
                                Text(
                                    row.status.replace(
                                        '_',
                                        ' '
                                    ),
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    color = Navy
                                )

                                row.amount?.let {
                                    Text(
                                        money.format(it),
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (
        showRowActions &&
        selectedRow != null
    ) {
        val row = selectedRow!!

        AlertDialog(
            onDismissRequest = {
                showRowActions = false
                selectedRow = null
            },
            title = {
                Text(row.documentNumber)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${row.customerName}\n${row.documentDate}",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (
                        viewModel.registerType ==
                        SalesRegisterType.INVOICE
                    ) {
                        TextButton(
                            enabled =
                                !isActionRunning &&
                                        !row.status.trim().equals(
                                            "CANCELLED",
                                            ignoreCase = true
                                        ),
                            onClick = {
                                showRowActions = false
                                selectedRow = null
                                onEditInvoice(row.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Invoice")
                        }

                        TextButton(
                            enabled =
                                !isActionRunning &&
                                        !row.status.trim().equals(
                                            "CANCELLED",
                                            ignoreCase = true
                                        ),
                            onClick = {
                                showRowActions = false
                                cancellationReason = ""
                                showCancelDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Invoice")
                        }

                        TextButton(
                            onClick = {
                                showRowActions = false
                                selectedRow = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        Text(
                            "Edit/Delete actions for this document type will be connected with its own transaction workflow."
                        )

                        TextButton(
                            onClick = {
                                showRowActions = false
                                selectedRow = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (
        showCancelDialog &&
        selectedRow != null
    ) {
        val row = selectedRow!!

        AlertDialog(
            onDismissRequest = {
                if (!isActionRunning) {
                    showCancelDialog = false
                    selectedRow = null
                    cancellationReason = ""
                }
            },
            title = {
                Text(
                    "Delete ${row.documentNumber}?"
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "This safely removes the invoice from active posting without destroying audit history. The invoice will remain as CANCELLED, and physical serials will return from SOLD to IN_STOCK only if the complete reversal is safe."
                    )

                    OutlinedTextField(
                        value =
                            cancellationReason,
                        onValueChange = {
                            cancellationReason = it
                        },
                        label = {
                            Text(
                                "Cancellation reason"
                            )
                        },
                        enabled =
                            !isActionRunning,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    if (isActionRunning) {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled =
                        !isActionRunning &&
                                cancellationReason
                                    .trim()
                                    .isNotBlank(),
                    onClick = {
                        viewModel.cancelInvoice(
                            row = row,
                            reason =
                                cancellationReason
                        )

                        showCancelDialog = false
                        selectedRow = null
                        cancellationReason = ""
                    }
                ) {
                    Text("Delete Invoice")
                }
            },
            dismissButton = {
                TextButton(
                    enabled =
                        !isActionRunning,
                    onClick = {
                        showCancelDialog = false
                        selectedRow = null
                        cancellationReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
