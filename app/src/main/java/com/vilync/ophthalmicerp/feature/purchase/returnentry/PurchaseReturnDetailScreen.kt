package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.ui.components.PrintExportActionBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PurchaseReturnDetailScreen(
    viewModel: PurchaseReturnDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            BoxLoading()
        }

        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage
                    ?: "Unable to load purchase return.",
                onBack = onBack
            )
        }

        uiState.purchaseReturn != null -> {
            PurchaseReturnDetailContent(
                uiState = uiState,
                onBack = onBack,
                onDashboard = onDashboard,
                onEdit = onEdit
            )
        }
    }
}

@Composable
private fun PurchaseReturnDetailContent(
    uiState: PurchaseReturnDetailUiState,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val purchaseReturn = uiState.purchaseReturn ?: return
    val context = LocalContext.current

    val money = NumberFormat.getCurrencyInstance(
        Locale("en", "IN")
    )

    val report = remember(
        purchaseReturn,
        uiState.items,
        uiState.lensesByItemId
    ) {
        ExportReport(
            title = "Purchase Return Debit Note ${purchaseReturn.creditNoteNumber}",
            headers = listOf(
                "Product",
                "Serial",
                "Qty",
                "Rate",
                "GST",
                "Total"
            ),
            rows = uiState.items.map { item ->

                listOf(
                    item.productName,

                    uiState.lensesByItemId[item.id]
                        .orEmpty()
                        .joinToString(", ") {
                            it.serialNumber
                        }
                        .ifBlank {
                            "-"
                        },

                    item.quantity.toString(),
                    item.rate.toString(),
                    "${item.gstPercent}%",
                    item.totalAmount.toString()
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
                    report
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
                    report
                )
            }
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column {

                    Text(
                        text = "Purchase Return Detail",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Debit Note: ${purchaseReturn.creditNoteNumber}",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // =========================================
                        // EDIT / CORRECT RETURN
                        // =========================================

                        OutlinedButton(
                            onClick = {
                                onEdit(purchaseReturn.id)
                            }
                        ) {
                            Text("✎ Edit")
                        }

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

                    PrintExportActionBar(

                        onPrint = {
                            ReportExportUtils.print(
                                context,
                                report
                            )
                        },

                        onExportPdf = {
                            pdfLauncher.launch(
                                "Debit_Note_${purchaseReturn.creditNoteNumber}.pdf"
                            )
                        },

                        onExportExcel = {
                            excelLauncher.launch(
                                "Debit_Note_${purchaseReturn.creditNoteNumber}.csv"
                            )
                        }
                    )
                }
            }
        }

        // =====================================================
        // SUPPLIER / ORIGINAL PURCHASE
        // =====================================================

        item {

            DetailCard(
                title = "Supplier / Original Purchase"
            ) {

                Text(
                    text = purchaseReturn.supplierName,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Original Invoice: ${purchaseReturn.originalInvoiceNumber}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Text(
                    text = "Debit Note Date: ${purchaseReturn.creditNoteDate}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        // =====================================================
        // RETURNED PRODUCTS
        // =====================================================

        item {

            Text(
                text = "Returned Products",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            items = uiState.items,
            key = {
                it.id
            }
        ) { item ->

            ReturnItemCard(

                item = item,

                serials = uiState
                    .lensesByItemId[item.id]
                    .orEmpty()
                    .joinToString(", ") {
                        it.serialNumber
                    },

                money = money
            )
        }

        // =====================================================
        // FINANCIAL SUMMARY
        // =====================================================

        item {

            DetailCard(
                title = "Financial Summary"
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Total Amount",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = money.format(
                            purchaseReturn.totalAmount
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun ReturnItemCard(
    item: PurchaseReturnItemEntity,
    serials: String,
    money: NumberFormat
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = item.productName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (serials.isNotBlank()) {

                Text(
                    text = "Serial No.: $serials",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "Qty: ${item.quantity}  |  " +
                                "Rate: ${money.format(item.rate)}  |  " +
                                "GST: ${item.gstPercent}%"
                )

                Text(
                    text = money.format(
                        item.totalAmount
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun DetailCard(
    title: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            content()
        }
    }
}


@Composable
private fun BoxLoading() {

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator()
    }
}


@Composable
private fun ErrorState(
    message: String,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = onBack
        ) {

            Text("← Back")
        }
    }
}