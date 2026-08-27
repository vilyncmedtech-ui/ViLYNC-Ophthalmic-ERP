package com.vilync.ophthalmicerp.feature.purchase.register

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import java.text.NumberFormat
import java.util.Locale


// ViLYNC Premium Compact Purchase UI — Pastel ERP, NO GOLD
private val RegisterAccentBlue = Color(0xFF4169A8)
private val RegisterPage = Color(0xFFF7F9FD)
private val RegisterNavy = Color(0xFF18233A)
private val RegisterMuted = Color(0xFF667085)
private val RegisterBorder = Color(0xFFDDE4EE)
private val RegisterPastelBlue = Color(0xFFEAF2FF)
private val RegisterPastelGreen = Color(0xFFECF8F1)
private val RegisterWhite = Color(0xFFFFFFFF)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PurchaseRegisterScreen(
    viewModel: PurchaseRegisterViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onNewPurchase: () -> Unit,
    onPurchaseClick: (Long) -> Unit = {},
    onEditPurchase: (Long) -> Unit = {},
    title: String = "Purchase Register"
) {

    val uiState by
    viewModel.uiState.collectAsState()

    val deleteResult by
    viewModel.deleteResult.collectAsState()


    val context =
        LocalContext.current


    // =========================================================
    // LONG PRESS / DELETE DIALOG STATE
    // =========================================================

    var selectedPurchase by
    remember {
        mutableStateOf<PurchaseEntity?>(null)
    }


    var purchasePendingDelete by
    remember {
        mutableStateOf<PurchaseEntity?>(null)
    }

    var exportExpanded by remember { mutableStateOf(false) }

    // =========================================================
    // EXPORT REPORT
    // =========================================================

    val registerReport =
        remember(uiState.purchases) {

            ExportReport(

                title =
                    title,

                headers =
                    listOf(
                        "Invoice No.",
                        "Invoice Date",
                        "Supplier",
                        "Type",
                        "Payment",
                        "Amount"
                    ),

                rows =
                    uiState.purchases.map { purchase ->

                        listOf(
                            purchase.invoiceNumber,
                            purchase.invoiceDate,
                            purchase.supplierName,
                            purchase.purchaseType,
                            purchase.paymentType,
                            purchase.grandTotal.toString()
                        )
                    }
            )
        }


    val financialYearLabel =
        formatFinancialYear(
            uiState.financialYearStart
        )


    // =========================================================
    // MAIN SCREEN
    // =========================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(RegisterPage)
                .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {

        // =====================================================
        // TOP ACTION ROW
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("← Back")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDashboard) {
                    Text("⌂ Dashboard")
                }

                Box {
                    Button(onClick = { exportExpanded = true }) {
                        Text("Export ▼")
                    }

                    DropdownMenu(
                        expanded = exportExpanded,
                        onDismissRequest = { exportExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Print") },
                            onClick = {
                                exportExpanded = false
                                ReportExportUtils.print(context, registerReport)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("PDF") },
                            onClick = {
                                exportExpanded = false
                                ReportExportUtils.exportPdfAndShare(context, registerReport)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Excel") },
                            onClick = {
                                exportExpanded = false
                                ReportExportUtils.exportExcelAndShare(context, registerReport)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // =====================================================
        // TITLE AREA
        // =====================================================

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = RegisterNavy
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Tap to view • Long press for Edit / Delete",
                fontSize = 12.sp,
                color = RegisterMuted
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RegisterAccentBlue.copy(alpha = 0.10f)
            ) {
                Text(
                    text = "Working FY: $financialYearLabel",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RegisterAccentBlue
                )
            }
        }


        Spacer(
            Modifier.height(16.dp)
        )


        // =====================================================
        // SEARCH + NEW PURCHASE
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value =
                    uiState.searchQuery,

                onValueChange =
                    viewModel::updateSearchQuery,

                label = {
                    Text(
                        "Search Invoice No. / Vendor"
                    )
                },

                singleLine =
                    true,

                modifier =
                    Modifier.weight(1f)
            )


            Button(
                onClick =
                    onNewPurchase,

                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(min = 56.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            RegisterAccentBlue,

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        if (title.contains("Order")) "New PO" else "New Purchase",

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }


        Spacer(
            Modifier.height(8.dp)
        )


        // =====================================================
        // PURCHASE LIST
        // =====================================================

        if (
            uiState.purchases.isEmpty()
        ) {

            Text(
                text =
                    if (
                        uiState.searchQuery.isBlank()
                    ) {

                        "No purchase invoices found in FY $financialYearLabel."

                    } else {

                        "No purchase invoice found."
                    },

                color =
                    Color.Gray
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                items(
                    items =
                        uiState.purchases,

                    key = {
                        it.id
                    }
                ) { purchase ->

                    PurchaseRegisterCard(

                        purchase =
                            purchase,

                        onClick = {

                            onPurchaseClick(
                                purchase.id
                            )
                        },

                        onLongClick = {

                            selectedPurchase =
                                purchase
                        },
                        title = title
                    )
                }
            }
        }
    }


    // =========================================================
    // LONG PRESS ACTION DIALOG
    // =========================================================

    selectedPurchase?.let { purchase ->

        AlertDialog(

            onDismissRequest = {
                selectedPurchase =
                    null
            },

            title = {

                Text(
                    text =
                        "Purchase ${purchase.invoiceNumber}"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            purchase.supplierName,

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        Modifier.height(8.dp)
                    )


                    Text(
                        text =
                            "Select an action."
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        selectedPurchase =
                            null

                        onEditPurchase(
                            purchase.id
                        )
                    }
                ) {

                    Text(
                        "Edit"
                    )
                }
            },

            dismissButton = {

                Row {

                    TextButton(
                        onClick = {

                            selectedPurchase =
                                null

                            purchasePendingDelete =
                                purchase
                        }
                    ) {

                        Text(
                            "Delete"
                        )
                    }


                    TextButton(
                        onClick = {

                            selectedPurchase =
                                null
                        }
                    ) {

                        Text(
                            "Close"
                        )
                    }
                }
            }
        )
    }


    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    purchasePendingDelete?.let { purchase ->

        AlertDialog(

            onDismissRequest = {

                purchasePendingDelete =
                    null
            },

            title = {

                Text(
                    text =
                        "Delete Purchase?"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Invoice: ${purchase.invoiceNumber}",

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        Modifier.height(6.dp)
                    )


                    Text(
                        text =
                            purchase.supplierName
                    )


                    Spacer(
                        Modifier.height(12.dp)
                    )


                    Text(
                        text =
                            "This Purchase will be deleted only if it is not linked with a protected downstream transaction."
                    )


                    Spacer(
                        Modifier.height(8.dp)
                    )


                    Text(
                        text =
                            "If a Purchase Return is already linked, deletion will be blocked."
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        purchasePendingDelete =
                            null

                        viewModel.deletePurchase(
                            purchaseId =
                                purchase.id
                        )
                    }
                ) {

                    Text(
                        "Delete"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        purchasePendingDelete =
                            null
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }


    // =========================================================
    // DELETE RESULT
    // =========================================================

    deleteResult?.let { result ->

        val title =
            when (result) {

                is PurchaseDeleteResult.Success ->
                    "Purchase Deleted"

                is PurchaseDeleteResult.Blocked ->
                    "Delete Not Allowed"

                is PurchaseDeleteResult.Error ->
                    "Unable to Delete"
            }


        val message =
            when (result) {

                is PurchaseDeleteResult.Success ->
                    result.message

                is PurchaseDeleteResult.Blocked ->
                    result.message

                is PurchaseDeleteResult.Error ->
                    result.message
            }


        AlertDialog(

            onDismissRequest = {
                viewModel.clearDeleteResult()
            },

            title = {

                Text(
                    text =
                        title
                )
            },

            text = {

                Text(
                    text =
                        message
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.clearDeleteResult()
                    }
                ) {

                    Text(
                        "OK"
                    )
                }
            }
        )
    }
}


// =============================================================
// PURCHASE REGISTER CARD
// =============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PurchaseRegisterCard(
    purchase: PurchaseEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    title: String
) {
    val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            RegisterBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RegisterCell(
                label = if (title.contains("Order")) "PO NO." else "INVOICE",
                value = purchase.invoiceNumber.ifBlank { "-" },
                modifier = Modifier.weight(1.15f),
                strong = true
            )

            RegisterCell(
                label = "DATE",
                value = purchase.invoiceDate.ifBlank { "-" },
                modifier = Modifier.weight(.85f)
            )

            RegisterCell(
                label = "VENDOR",
                value = purchase.supplierName.ifBlank { "-" },
                modifier = Modifier.weight(2.6f),
                strong = true
            )

            RegisterCell(
                label = "TYPE",
                value = purchase.purchaseType.ifBlank { "-" },
                modifier = Modifier.weight(.8f)
            )

            RegisterCell(
                label = "PAYMENT",
                value = purchase.paymentType.ifBlank { "-" },
                modifier = Modifier.weight(.8f)
            )

            RegisterCell(
                label = "AMOUNT",
                value = money.format(purchase.grandTotal),
                modifier = Modifier.weight(1f),
                strong = true
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RegisterPastelBlue
            ) {
                Text(
                    text = "VIEW ›",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RegisterAccentBlue
                )
            }
        }
    }
}

@Composable
private fun RegisterCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = RegisterMuted,
            maxLines = 1
        )

        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            color = RegisterNavy,
            maxLines = 1
        )
    }
}


// =============================================================
// FINANCIAL YEAR LABEL
// =============================================================

private fun formatFinancialYear(
    startYear: Int
): String {

    if (
        startYear <= 0
    ) {
        return "-"
    }


    val endYear =
        startYear + 1


    return String.format(
        Locale.US,
        "%04d-%02d",
        startYear,
        endYear % 100
    )
}