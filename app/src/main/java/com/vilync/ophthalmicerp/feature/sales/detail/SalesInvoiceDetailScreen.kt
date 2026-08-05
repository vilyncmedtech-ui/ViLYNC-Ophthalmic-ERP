package com.vilync.ophthalmicerp.feature.sales.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.ui.components.StandardDetailHeader
import java.text.NumberFormat
import java.util.Locale

private val Navy = Color(0xFF071B33)
private val Gold = Color(0xFFD4AF37)
private val Page = Color(0xFFF7F9FC)
private val SoftBlue = Color(0xFFEAF2FD)
private val SoftLavender = Color(0xFFF1ECFA)
private val SoftMint = Color(0xFFEAF7F0)
private val SoftAmber = Color(0xFFFFF5DA)
private val SoftRed = Color(0xFFFDECEC)
private val Border = Color(0xFFDDE3EC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesInvoiceDetailScreen(
    viewModel: SalesInvoiceDetailViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    val money = remember {
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )
    }

    Scaffold(
        topBar = {
            StandardDetailHeader(
                title = "Sales Invoice",
                onBack = onBack,
                onDashboard = onDashboard,
                onPrint = {
                    state.sale?.let {
                        SalesInvoiceDetailExportSuite.print(context, it, state.lines, state.companyProfile)
                    }
                },
                onPdf = {
                    state.sale?.let {
                        SalesInvoiceDetailExportSuite.exportPdfAndShare(context, it, state.lines, state.companyProfile)
                    }
                },
                onShare = {
                    state.sale?.let {
                        SalesInvoiceDetailExportSuite.exportPdfAndShare(context, it, state.lines, state.companyProfile)
                    }
                },
                onEdit = { state.sale?.let { onEdit(it.id) } },
                isEditable = state.sale?.status != "CANCELLED"
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null -> {
                    DetailCard(
                        title = "Unable to open invoice",
                        background = SoftRed
                    ) {
                        Text(
                            state.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = viewModel::refresh
                        ) {
                            Text("Retry")
                        }
                    }
                }

                state.sale != null -> {
                    val sale = state.sale

                    InvoiceHeaderCard(
                        sale = sale,
                        money = money
                    )

                    DetailCard(
                        title = "Customer / Hospital",
                        background = SoftBlue
                    ) {
                        CompactInfoRow(
                            leftLabel = "Party",
                            leftValue = sale.customerName.ifBlank {
                                sale.billToLegalName
                            },
                            rightLabel = "GSTIN",
                            rightValue = sale.billToGstin
                        )
                        CompactInfoRow(
                            leftLabel = "Legal Name",
                            leftValue = sale.billToLegalName,
                            rightLabel = "State",
                            rightValue = sale.billToState
                        )
                        CompactInfoRow(
                            leftLabel = "Address",
                            leftValue = sale.billToAddress,
                            rightLabel = "Place of Supply",
                            rightValue = sale.placeOfSupplyState
                        )
                    }

                    if (
                        !sale.sameAsBillTo ||
                        sale.shipToName.isNotBlank() ||
                        sale.shipToAddress.isNotBlank() ||
                        sale.shipToGstin.isNotBlank() ||
                        sale.shipToState.isNotBlank()
                    ) {
                        DetailCard(
                            title = "Ship To",
                            background = SoftLavender
                        ) {
                            CompactInfoRow(
                                leftLabel = "Name",
                                leftValue = sale.shipToName,
                                rightLabel = "GSTIN",
                                rightValue = sale.shipToGstin
                            )
                            CompactInfoRow(
                                leftLabel = "Address",
                                leftValue = sale.shipToAddress,
                                rightLabel = "State",
                                rightValue = sale.shipToState
                            )
                        }
                    }

                    DetailCard(
                        title = "Invoice / GST Details",
                        background = SoftAmber
                    ) {
                        CompactInfoRow(
                            leftLabel = "PO Number",
                            leftValue = sale.poNumber,
                            rightLabel = "PO Date",
                            rightValue = sale.poDate
                        )
                        CompactInfoRow(
                            leftLabel = "Place of Supply",
                            leftValue = sale.placeOfSupplyState,
                            rightLabel = "GST Supply Type",
                            rightValue = sale.gstSupplyType.replace('_', ' ')
                        )
                    }

                    DetailCard(
                        title = "Products / Serial Details",
                        background = Color.White
                    ) {
                        if (state.lines.isEmpty()) {
                            Text("No invoice items found.")
                        } else {
                            state.lines.forEachIndexed { index, line ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Page
                                    ),
                                    border = BorderStroke(1.dp, Border)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                "${index + 1}. ${line.item.productName}",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                color = Navy
                                            )
                                            if (line.item.hsnCode.isNotBlank()) {
                                                Text(
                                                    "HSN: ${line.item.hsnCode}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Navy
                                                )
                                            }
                                        }

                                        CompactTableRow(
                                            cells = listOf(
                                                "Power" to line.item.power,
                                                "Qty" to line.item.quantity.toString(),
                                                "Rate" to money.format(line.item.rate),
                                                "Discount" to "${formatNumber(line.item.discountPercent)}% (${money.format(line.item.discountAmount)})",
                                                "Taxable" to money.format(line.item.taxableAmount),
                                                "GST" to "${formatNumber(line.item.gstPercent)}% (${money.format(line.item.gstAmount)})",
                                                "Total" to money.format(line.item.totalAmount)
                                            )
                                        )

                                        if (
                                            line.item.batchNumber.isNotBlank() ||
                                            line.item.lotNumber.isNotBlank()
                                        ) {
                                            CompactInfoRow(
                                                leftLabel = "Batch",
                                                leftValue = line.item.batchNumber,
                                                rightLabel = "Lot",
                                                rightValue = line.item.lotNumber
                                            )
                                        }

                                        if (line.lenses.isNotEmpty()) {
                                            Text(
                                                "Serial Numbers",
                                                fontWeight = FontWeight.SemiBold,
                                                color = Navy
                                            )

                                            line.lenses.forEach { lens ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = SoftLavender
                                                    ),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        Gold.copy(alpha = .25f)
                                                    )
                                                ) {
                                                    CompactTableRow(
                                                        modifier = Modifier.padding(8.dp),
                                                        cells = listOf(
                                                            "Serial Number" to lens.serialNumber,
                                                            "Power" to lens.power,
                                                            "Batch" to lens.batchNumber,
                                                            "Expiry" to lens.expiryDate
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DetailCard(
                        title = "Financial Summary",
                        background = SoftMint
                    ) {
                        CompactMoneyRow(
                            cells = listOf(
                                "Sub Total" to sale.subTotal,
                                "Discount" to sale.discountAmount,
                                "Taxable" to sale.taxableAmount
                            ),
                            money = money
                        )

                        val taxCells = buildList {
                            if (sale.cgstAmount != 0.0) {
                                add("CGST" to sale.cgstAmount)
                            }
                            if (sale.sgstAmount != 0.0) {
                                add("SGST" to sale.sgstAmount)
                            }
                            if (sale.igstAmount != 0.0) {
                                add("IGST" to sale.igstAmount)
                            }
                            add("GST Total" to sale.gstAmount)
                        }

                        CompactMoneyRow(
                            cells = taxCells,
                            money = money
                        )

                        if (
                            sale.adjustment != 0.0 ||
                            sale.roundOff != 0.0
                        ) {
                            CompactMoneyRow(
                                cells = buildList {
                                    if (sale.adjustment != 0.0) {
                                        add("Adjustment" to sale.adjustment)
                                    }
                                    if (sale.roundOff != 0.0) {
                                        add("Round Off" to sale.roundOff)
                                    }
                                },
                                money = money
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = .72f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                Gold.copy(alpha = .35f)
                            )
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GRAND TOTAL",
                                    fontWeight = FontWeight.Bold,
                                    color = Navy
                                )
                                Text(
                                    money.format(sale.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy
                                )
                            }
                        }
                    }

                    if (sale.remarks.isNotBlank()) {
                        DetailCard(
                            title = "Remarks",
                            background = Page
                        ) {
                            Text(sale.remarks)
                        }
                    }

                    if (sale.cancellationReason.isNotBlank()) {
                        DetailCard(
                            title = "Cancellation",
                            background = SoftRed
                        ) {
                            DetailRow(
                                "Reason",
                                sale.cancellationReason
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceHeaderCard(
    sale: SaleEntity,
    money: NumberFormat
) {
    val cancelled =
        sale.status.trim().equals(
            "CANCELLED",
            ignoreCase = true
        )

    DetailCard(
        title = sale.invoiceNumber,
        background =
            if (cancelled) SoftRed else SoftBlue
    ) {
        CompactInfoRow(
            leftLabel = "Invoice Date",
            leftValue = sale.invoiceDate,
            rightLabel = "Status",
            rightValue = sale.status.replace('_', ' ')
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Grand Total",
                fontWeight = FontWeight.SemiBold,
                color = Navy
            )
            Text(
                money.format(sale.totalAmount),
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    background: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
        ),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            content()
        }
    }
}

@Composable
private fun CompactInfoRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    if (leftValue.isBlank() && rightValue.isBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        CompactInfoCell(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            value = leftValue
        )
        CompactInfoCell(
            modifier = Modifier.weight(1f),
            label = rightLabel,
            value = rightValue
        )
    }
}

@Composable
private fun CompactInfoCell(
    modifier: Modifier,
    label: String,
    value: String
) {
    if (value.isBlank()) {
        Box(modifier = modifier)
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "$label:",
            fontWeight = FontWeight.SemiBold,
            color = Navy
        )
        Text(
            value,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactTableRow(
    cells: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val visibleCells = cells.filter { it.second.isNotBlank() }
    if (visibleCells.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        visibleCells.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy.copy(alpha = .75f)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight =
                        if (label == "Serial Number") {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    color =
                        if (label == "Serial Number") Navy
                        else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CompactMoneyRow(
    cells: List<Pair<String, Double>>,
    money: NumberFormat
) {
    if (cells.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        cells.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                )
                Text(
                    money.format(value),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(0.34f),
            fontWeight = FontWeight.SemiBold,
            color = Navy
        )
        Text(
            value,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
