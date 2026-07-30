package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.ui.components.PrintExportActionBar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PurchaseReturnSavedRegisterScreen(
    viewModel: PurchaseReturnSavedRegisterViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onReturnClick: (Long) -> Unit = {},
    onEditReturn: (Long) -> Unit = {}
) {

    val query by viewModel.searchQuery.collectAsState()
    val returns by viewModel.returns.collectAsState()
    val isCancelling by viewModel.isCancelling.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    val context = LocalContext.current

    var actionReturn by remember {
        mutableStateOf<PurchaseReturnEntity?>(null)
    }

    var deleteReturn by remember {
        mutableStateOf<PurchaseReturnEntity?>(null)
    }

    var cancellationReason by remember {
        mutableStateOf("")
    }


    // =========================================================
    // EXPORT REPORT
    // =========================================================

    val registerReport = remember(returns) {

        ExportReport(
            title = "Purchase Return Register",
            headers = listOf(
                "Debit Note",
                "Date",
                "Supplier",
                "Original Invoice",
                "Amount",
                "Status"
            ),
            rows = returns.map { purchaseReturn ->

                listOf(
                    purchaseReturn.creditNoteNumber,
                    purchaseReturn.creditNoteDate,
                    purchaseReturn.supplierName,
                    purchaseReturn.originalInvoiceNumber,
                    purchaseReturn.totalAmount.toString(),
                    purchaseReturn.status
                )
            }
        )
    }


    val pdfLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { uri ->

            uri?.let {
                ReportExportUtils.exportPdf(
                    context,
                    it,
                    registerReport
                )
            }
        }


    val excelLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "text/csv"
            )
        ) { uri ->

            uri?.let {
                ReportExportUtils.exportExcelCsv(
                    context,
                    it,
                    registerReport
                )
            }
        }


    // =========================================================
    // MAIN SCREEN
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            OutlinedButton(
                onClick = onDashboard
            ) {
                Text("⌂ Dashboard")
            }
        }


        Spacer(
            Modifier.height(12.dp)
        )


        Text(
            text = "Purchase Return Register",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap to view • Long press for Edit / Delete",
            fontSize = 14.sp,
            color = Color.Gray
        )


        Spacer(
            Modifier.height(14.dp)
        )


        PrintExportActionBar(
            onPrint = {
                ReportExportUtils.print(
                    context,
                    registerReport
                )
            },
            onExportPdf = {
                pdfLauncher.launch(
                    "Purchase_Return_Register.pdf"
                )
            },
            onExportExcel = {
                excelLauncher.launch(
                    "Purchase_Return_Register.csv"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            Modifier.height(12.dp)
        )


        OutlinedTextField(
            value = query,
            onValueChange =
                viewModel::updateSearchQuery,
            label = {
                Text(
                    "Search Debit Note / Invoice / Supplier"
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true
        )


        Spacer(
            Modifier.height(14.dp)
        )


        if (returns.isEmpty()) {

            Text(
                text =
                    if (query.isBlank()) {
                        "No purchase returns saved yet."
                    } else {
                        "No purchase return found."
                    },
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    returns,
                    key = { it.id }
                ) { purchaseReturn ->

                    PurchaseReturnRegisterCard(
                        purchaseReturn =
                            purchaseReturn,

                        onClick = {
                            onReturnClick(
                                purchaseReturn.id
                            )
                        },

                        onLongClick = {
                            actionReturn =
                                purchaseReturn
                        }
                    )
                }
            }
        }
    }


    // =========================================================
    // LONG-PRESS ACTION DIALOG
    // =========================================================

    actionReturn?.let { selectedReturn ->

        AlertDialog(
            onDismissRequest = {
                actionReturn = null
            },

            title = {
                Text(
                    "Purchase Return ${selectedReturn.creditNoteNumber}"
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        selectedReturn.supplierName,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        "Select an action."
                    )

                    if (
                        selectedReturn.status ==
                        PurchaseReturnEntity.STATUS_CANCELLED
                    ) {

                        Text(
                            "Status: CANCELLED",
                            color =
                                MaterialTheme.colorScheme.error,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled =
                        selectedReturn.status !=
                                PurchaseReturnEntity.STATUS_CANCELLED,

                    onClick = {

                        val id =
                            selectedReturn.id

                        actionReturn =
                            null

                        onEditReturn(id)
                    }
                ) {
                    Text("Edit")
                }
            },

            dismissButton = {

                Row {

                    TextButton(
                        enabled =
                            selectedReturn.status !=
                                    PurchaseReturnEntity.STATUS_CANCELLED,

                        onClick = {

                            deleteReturn =
                                selectedReturn

                            cancellationReason =
                                ""

                            actionReturn =
                                null
                        }
                    ) {
                        Text("Delete")
                    }


                    TextButton(
                        onClick = {
                            actionReturn = null
                        }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }


    // =========================================================
    // DELETE / CANCEL CONFIRMATION
    // =========================================================

    deleteReturn?.let { selectedReturn ->

        AlertDialog(
            onDismissRequest = {

                if (!isCancelling) {
                    deleteReturn = null
                    cancellationReason = ""
                }
            },

            title = {
                Text("Delete Purchase Return?")
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        "Debit Note: ${selectedReturn.creditNoteNumber}"
                    )

                    Text(
                        "This entry will be marked CANCELLED rather than permanently removed. This preserves accounting and IOL serial history."
                    )

                    OutlinedTextField(
                        value =
                            cancellationReason,

                        onValueChange = {
                            cancellationReason = it
                        },

                        label = {
                            Text(
                                "Cancellation Reason"
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled =
                            !isCancelling,

                        minLines = 2
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled =
                        !isCancelling &&
                                cancellationReason
                                    .trim()
                                    .isNotBlank(),

                    onClick = {

                        viewModel.cancelPurchaseReturn(
                            purchaseReturnId =
                                selectedReturn.id,

                            reason =
                                cancellationReason
                        )

                        deleteReturn =
                            null

                        cancellationReason =
                            ""
                    }
                ) {

                    Text(
                        if (isCancelling) {
                            "Deleting..."
                        } else {
                            "Confirm Delete"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled =
                        !isCancelling,

                    onClick = {
                        deleteReturn = null
                        cancellationReason = ""
                    }
                ) {
                    Text("Back")
                }
            }
        )
    }


    // =========================================================
    // RESULT MESSAGE
    // =========================================================

    actionMessage?.let { message ->

        AlertDialog(
            onDismissRequest = {
                viewModel.clearActionMessage()
            },

            title = {
                Text("Purchase Return")
            },

            text = {
                Text(message)
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.clearActionMessage()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}


// =============================================================
// PURCHASE RETURN REGISTER CARD
// =============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PurchaseReturnRegisterCard(
    purchaseReturn: PurchaseReturnEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {

    val money =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme
                        .surfaceVariant
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        purchaseReturn.creditNoteNumber,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        17.sp
                )


                Text(
                    text =
                        money.format(
                            purchaseReturn.totalAmount
                        ),

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.height(5.dp)
            )


            Text(
                text =
                    purchaseReturn.supplierName,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    "Original Invoice: ${purchaseReturn.originalInvoiceNumber}",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )


            Text(
                text =
                    "Debit Note Date: ${purchaseReturn.creditNoteDate}",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )


            Spacer(
                Modifier.height(4.dp)
            )


            Text(
                text =
                    "Status: ${purchaseReturn.status}",

                color =
                    if (
                        purchaseReturn.status ==
                        PurchaseReturnEntity.STATUS_CANCELLED
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    },

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}