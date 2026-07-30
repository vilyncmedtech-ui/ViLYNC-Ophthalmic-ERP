package com.vilync.ophthalmicerp.feature.purchase.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
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
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.core.export.ExportReport
import com.vilync.ophthalmicerp.core.export.ReportExportUtils
import com.vilync.ophthalmicerp.ui.components.PrintExportActionBar
import java.text.NumberFormat
import java.util.Locale

private val AccentBlue = Color(0xFF4169A8)
private val PageBackground = Color(0xFFF7F9FD)
private val PastelBlue = Color(0xFFF1F6FD)
private val PastelLavender = Color(0xFFF2EEFC)
private val CardBorder = Color(0xFFDDE4EE)
private val NavyText = Color(0xFF18233A)
private val MutedText = Color(0xFF667085)

@Composable
fun PurchaseDetailScreen(
    viewModel: PurchaseDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEditPurchase: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.errorMessage != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "Purchase Detail",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                uiState.errorMessage ?: "Unable to load purchase details.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("← Back")
                }
                OutlinedButton(onClick = onDashboard) {
                    Text("⌂ Dashboard")
                }
            }
        }
        return
    }

    val purchase = uiState.purchase ?: return
    val context = LocalContext.current
    val detailReport = remember(purchase, uiState.items) {
        ExportReport(
            title = "Purchase Invoice ${purchase.invoiceNumber}",
            headers = listOf("Product", "Serial", "Power", "Qty", "Rate", "GST", "Total"),
            rows = uiState.items.map { detailItem ->
                val item = detailItem.purchaseItem
                listOf(
                    detailItem.productName,
                    detailItem.lenses.joinToString(", ") { it.serialNumber }.ifBlank { "-" },
                    item.power.ifBlank { "-" },
                    item.quantity.toString(),
                    item.purchaseRate.toString(),
                    "${formatPercent(item.gstPercent)}%",
                    item.lineTotal.toString()
                )
            }
        )
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { ReportExportUtils.exportPdf(context, it, detailReport) } }
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { ReportExportUtils.exportExcelCsv(context, it, detailReport) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Purchase Detail",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${purchase.invoiceNumber}  •  ${purchase.supplierName}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onEditPurchase(purchase.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Purchase", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = onBack) { Text("← Back") }
                    OutlinedButton(onClick = onDashboard) { Text("⌂ Dashboard") }
                }

                PrintExportActionBar(
                    onPrint = { ReportExportUtils.print(context, detailReport) },
                    onExportPdf = { pdfLauncher.launch("Purchase_${purchase.invoiceNumber}.pdf") },
                    onExportExcel = { excelLauncher.launch("Purchase_${purchase.invoiceNumber}.csv") }
                )
            }
        }

        Spacer(Modifier.height(7.dp))

        CompactCard(title = "Invoice Information") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CompactField("Invoice No.", purchase.invoiceNumber, Modifier.weight(1.35f))
                CompactField("Invoice Date", purchase.invoiceDate, Modifier.weight(1f))
                CompactField("Received Date", purchase.receivedDate, Modifier.weight(1f))
                CompactField("Purchase Type", purchase.purchaseType, Modifier.weight(1f))
                CompactField("Payment Type", purchase.paymentType, Modifier.weight(1f))
                CompactField(
                    "Credit",
                    if (purchase.creditDays > 0) "${purchase.creditDays} Days" else "-",
                    Modifier.weight(.8f)
                )
            }

            if (purchase.reference.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reference: ${purchase.reference}",
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        CompactCard(title = "Supplier / Vendor") {
            Text(
                purchase.supplierName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Products",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        if (uiState.items.isEmpty()) {
            Text("No products found in this purchase.", color = Color.Gray)
        } else {
            uiState.items.forEachIndexed { index, detailItem ->
                CompactPurchaseItem(
                    itemNumber = index + 1,
                    detailItem = detailItem,
                    money = money
                )
                if (index < uiState.items.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        CompactCard(title = "Financial Summary") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CompactAmount("Sub Total", money.format(purchase.subtotal), Modifier.weight(1f))
                CompactAmount("Discount", money.format(purchase.discountAmount), Modifier.weight(1f))
                CompactAmount("Taxable", money.format(purchase.taxableAmount), Modifier.weight(1f))

                if (purchase.igstAmount != 0.0) {
                    CompactAmount("IGST", money.format(purchase.igstAmount), Modifier.weight(1f))
                } else {
                    CompactAmount("CGST", money.format(purchase.cgstAmount), Modifier.weight(1f))
                    CompactAmount("SGST", money.format(purchase.sgstAmount), Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Grand Total", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        money.format(purchase.grandTotal),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CompactPurchaseItem(
    itemNumber: Int,
    detailItem: PurchaseDetailItem,
    money: NumberFormat
) {
    val item = detailItem.purchaseItem

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PastelLavender),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(2.15f)) {
                    Text(
                        "$itemNumber. ${detailItem.productName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyText,
                        maxLines = 1
                    )
                    val subTitle = listOf(detailItem.brand, detailItem.model)
                        .filter { it.isNotBlank() }.joinToString(" • ")
                    if (subTitle.isNotBlank()) {
                        Text(subTitle, fontSize = 10.sp, color = MutedText, maxLines = 1)
                    }
                }

                CompactField("Power", item.power.ifBlank { "-" }, Modifier.weight(.62f))
                CompactField("HSN", detailItem.hsnCode.ifBlank { "-" }, Modifier.weight(.92f))
                CompactField("Qty", item.quantity.toString(), Modifier.weight(.45f))
                CompactField("Rate", money.format(item.purchaseRate), Modifier.weight(.9f))
                CompactField("Disc.", "${formatPercent(item.discountPercent)}%", Modifier.weight(.55f))
                CompactField("GST", "${formatPercent(item.gstPercent)}%", Modifier.weight(.5f))
                CompactField("Batch", item.batchNumber.ifBlank { "-" }, Modifier.weight(.8f))
                CompactField("Total", money.format(item.lineTotal), Modifier.weight(.95f), valueBold = true)
            }

            HorizontalDivider(color = CardBorder, thickness = 0.7.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactMeta("Category", detailItem.category.ifBlank { "-" }, Modifier.weight(.8f))
                CompactMeta("Gross", money.format(item.grossAmount), Modifier.weight(1f))
                CompactMeta("Discount", money.format(item.discountAmount), Modifier.weight(1f))
                CompactMeta("Taxable", money.format(item.taxableAmount), Modifier.weight(1f))
                CompactMeta("GST Amt.", money.format(item.gstAmount), Modifier.weight(1f))
            }

            if (detailItem.lenses.isNotEmpty()) {
                HorizontalDivider(color = CardBorder, thickness = 0.7.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SN / EXP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        modifier = Modifier.width(58.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        detailItem.lenses.chunked(5).forEach { rowLenses ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                rowLenses.forEach { lens ->
                                    CompactLens(lens = lens, modifier = Modifier.weight(1f))
                                }
                                repeat(5 - rowLenses.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else if (item.expiryDate.isNotBlank()) {
                Text(
                    "Expiry: ${formatExpiry(item.expiryDate)}",
                    fontSize = 10.sp,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
private fun CompactLens(
    lens: PurchaseLensEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                lens.serialNumber.ifBlank { "-" },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = NavyText,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatExpiry(lens.expiryDate),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = PastelBlue
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NavyText
            )
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueBold: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 1
        )
        Text(
            value.ifBlank { "-" },
            fontSize = 12.sp,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2
        )
    }
}

@Composable
private fun CompactMeta(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(
            value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactAmount(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatExpiry(expiry: String): String {
    val clean = expiry.trim()

    if (clean.length == 4 && clean.all { it.isDigit() }) {
        val month = clean.substring(0, 2)
        val year = clean.substring(2, 4)
        return "$month/20$year"
    }

    return clean
}

private fun formatPercent(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}
