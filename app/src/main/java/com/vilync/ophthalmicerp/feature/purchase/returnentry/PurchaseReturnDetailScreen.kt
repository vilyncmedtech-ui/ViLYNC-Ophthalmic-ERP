package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseReturnDetailScreen(
    viewModel: PurchaseReturnDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val report = remember(uiState.purchaseReturn, uiState.items, uiState.lensesByItemId) {
        val purchaseReturn = uiState.purchaseReturn ?: return@remember null
        ExportReport(
            title = "Purchase Return Debit Note ${purchaseReturn.creditNoteNumber}",
            headers = listOf("Product", "Serial", "Qty", "Rate", "GST", "Total"),
            rows = uiState.items.map { item ->
                listOf(
                    item.productName,
                    uiState.lensesByItemId[item.id].orEmpty().joinToString(", ") { it.serialNumber }.ifBlank { "-" },
                    item.quantity.toString(),
                    "₹ %.2f".format(item.rate),
                    "${item.gstPercent}%",
                    "₹ %.2f".format(item.totalAmount)
                )
            }
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { report?.let { r -> ReportExportUtils.exportPdf(context, it, r) } }
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Purchase Return (Debit Note)",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = { report?.let { ReportExportUtils.print(context, it) } },
                onPdf = { uiState.purchaseReturn?.let { pdfLauncher.launch("DebitNote_${it.creditNoteNumber.replace("/", "_")}.pdf") } },
                onShare = {
                    uiState.purchaseReturn?.let { cn ->
                        report?.let { r ->
                            val file = File(context.cacheDir, "DebitNote_${cn.creditNoteNumber.replace("/", "_")}.pdf")
                            ReportExportUtils.exportPdfToFile(context, file, r)
                            ShareUtils.shareFile(context, file, "application/pdf", "Share Debit Note")
                        }
                    }
                },
                onEdit = { uiState.purchaseReturn?.let { onEdit(it.id) } },
                isEditable = uiState.purchaseReturn?.status != "CANCELLED"
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                uiState.errorMessage != null -> {
                    ErrorState(message = uiState.errorMessage!!, onBack = onBack)
                }
                uiState.purchaseReturn != null -> {
                    PurchaseReturnDetailContent(
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseReturnDetailContent(
    uiState: PurchaseReturnDetailUiState
) {
    val purchaseReturn = uiState.purchaseReturn ?: return
    val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Suppliers / Original Purchase
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

        // Returned Products
        item {
            Text(
                text = "Returned Products",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            items = uiState.items,
            key = { it.id }
        ) { item ->
            ReturnItemCard(
                item = item,
                serials = uiState.lensesByItemId[item.id].orEmpty().joinToString(", ") { it.serialNumber },
                money = money
            )
        }

        // Financial Summary
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
                        text = money.format(purchaseReturn.totalAmount),
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Qty: ${item.quantity}  |  Rate: ${money.format(item.rate)}  |  GST: ${item.gstPercent}%"
                )
                Text(
                    text = money.format(item.totalAmount),
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp)
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = onBack) {
            Text("← Back")
        }
    }
}
