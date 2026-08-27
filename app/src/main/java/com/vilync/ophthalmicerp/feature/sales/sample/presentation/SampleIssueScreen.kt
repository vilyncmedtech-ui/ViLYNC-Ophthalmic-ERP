package com.vilync.ophthalmicerp.feature.sales.sample.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import com.vilync.ophthalmicerp.feature.sales.sample.export.SampleIssueExportSuite
import com.vilync.ophthalmicerp.ui.components.PartySearchField
import com.vilync.ophthalmicerp.ui.components.PrintExportActionBar
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleIssueScreen(
    viewModel: SampleIssueViewModel,
    onBack: () -> Unit,
    onSavedToDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(uiState.isSaved, uiState.savedSampleId) {
        if (uiState.isSaved && uiState.savedSampleId != null) {
            onSavedToDetail(uiState.savedSampleId!!)
            viewModel.clearMessage()
        }
    }

    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d -> viewModel.updateDate("${d}-${m + 1}-$y") },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val returnDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> viewModel.updateExpectedReturnDate("${d}-${m + 1}-$y") },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved && !uiState.isEditMode) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Sample: ${uiState.sampleNumber}" else "New Sample") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (uiState.isSaved) {
                        PrintExportActionBar(
                            onPrint = {
                                val sample = SampleIssueEntity(
                                    sampleIssueNumber = uiState.sampleNumber,
                                    normalizedSampleIssueNumber = uiState.sampleNumber.uppercase(),
                                    sampleIssueDate = uiState.issueDate,
                                    expectedReturnDate = uiState.expectedReturnDate,
                                    sampleType = uiState.sampleType,
                                    remarks = uiState.remarks,
                                    financialYearStart = uiState.financialYearStart,
                                    customerId = uiState.selectedCustomer?.id ?: 0, 
                                    customerName = uiState.selectedCustomer?.partyName ?: "", 
                                    createdAt = 0, updatedAt = 0
                                )
                                val items = uiState.items.map {
                                    SampleIssueItemEntity(
                                        sampleIssueId = 0, inventoryUnitId = it.inventoryUnitId, productId = it.productId,
                                        productName = it.productName, power = it.power, serialNumber = it.serialNumber,
                                        batchNumber = it.batchNumber, expiryDate = it.expiryDate, createdAt = 0, updatedAt = 0
                                    )
                                }
                                SampleIssueExportSuite.print(context, sample, items)
                            },
                            onExportPdf = {
                                val sample = SampleIssueEntity(
                                    sampleIssueNumber = uiState.sampleNumber,
                                    normalizedSampleIssueNumber = uiState.sampleNumber.uppercase(),
                                    sampleIssueDate = uiState.issueDate,
                                    expectedReturnDate = uiState.expectedReturnDate,
                                    sampleType = uiState.sampleType,
                                    remarks = uiState.remarks,
                                    financialYearStart = uiState.financialYearStart,
                                    customerId = uiState.selectedCustomer?.id ?: 0, 
                                    customerName = uiState.selectedCustomer?.partyName ?: "", 
                                    createdAt = 0, updatedAt = 0
                                )
                                val items = uiState.items.map {
                                    SampleIssueItemEntity(
                                        sampleIssueId = 0, inventoryUnitId = it.inventoryUnitId, productId = it.productId,
                                        productName = it.productName, power = it.power, serialNumber = it.serialNumber,
                                        batchNumber = it.batchNumber, expiryDate = it.expiryDate, createdAt = 0, updatedAt = 0
                                    )
                                }
                                SampleIssueExportSuite.exportPdfAndShare(context, sample, items)
                            },
                            onExportExcel = {
                                val sample = SampleIssueEntity(
                                    sampleIssueNumber = uiState.sampleNumber,
                                    normalizedSampleIssueNumber = uiState.sampleNumber.uppercase(),
                                    sampleIssueDate = uiState.issueDate,
                                    expectedReturnDate = uiState.expectedReturnDate,
                                    sampleType = uiState.sampleType,
                                    remarks = uiState.remarks,
                                    financialYearStart = uiState.financialYearStart,
                                    customerId = uiState.selectedCustomer?.id ?: 0, 
                                    customerName = uiState.selectedCustomer?.partyName ?: "", 
                                    createdAt = 0, updatedAt = 0
                                )
                                val items = uiState.items.map {
                                    SampleIssueItemEntity(
                                        sampleIssueId = 0, inventoryUnitId = it.inventoryUnitId, productId = it.productId,
                                        productName = it.productName, power = it.power, serialNumber = it.serialNumber,
                                        batchNumber = it.batchNumber, expiryDate = it.expiryDate, createdAt = 0, updatedAt = 0
                                    )
                                }
                                SampleIssueExportSuite.exportExcelAndShare(context, sample, items)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SampleNumberField(
                            label = "Sample Number",
                            prefix = if (uiState.isEditMode) "" else "Auto-generated:",
                            number = if (uiState.isEditMode) uiState.sampleNumber else uiState.sampleNumberPreview.ifBlank { "..." },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.issueDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f).height(56.dp).clickable { if (!uiState.isSaved) datePicker.show() },
                            enabled = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 16.sp),
                            shape = RoundedCornerShape(9.dp)
                        )
                    }

                    PartySearchField(
                        label = "Hospital / Doctor *",
                        selectedParty = uiState.selectedCustomer,
                        allParties = uiState.customers,
                        onPartySelected = { viewModel.selectCustomer(it) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        readOnly = uiState.isSaved,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    )

                    var typeExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = uiState.sampleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Purpose") },
                            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { if (!uiState.isSaved) typeExpanded = true },
                            enabled = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 16.sp),
                            shape = RoundedCornerShape(9.dp)
                        )
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            listOf("Evaluation", "Surgical Trial", "Demonstration", "Training", "Exhibition", "Marketing", "Replacement Sample", "Other").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = { viewModel.updateSampleType(type); typeExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.expectedReturnDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expected Return Date") },
                        modifier = Modifier.fillMaxWidth().height(56.dp).clickable { if (!uiState.isSaved) returnDatePicker.show() },
                        enabled = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 16.sp),
                        shape = RoundedCornerShape(9.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Declaration
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "The above material has been supplied strictly for evaluation / demonstration. No commercial sale has taken place. This document is NOT a Tax Invoice.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }

                // Serial Search
                if (!uiState.isSaved) {
                    OutlinedTextField(
                        value = uiState.serialQuery,
                        onValueChange = viewModel::updateSerialQuery,
                        label = { Text("Search Serial (Numeric)") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(9.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        ),
                        trailingIcon = { IconButton(onClick = { viewModel.searchSerial() }) { Icon(Icons.Default.Search, "Search") } }
                    )

                    if (uiState.serialMatches.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                            uiState.serialMatches.forEach { match ->
                                ListItem(
                                    headlineContent = { Text(match.serialNumber) },
                                    supportingContent = { Text("Power: ${match.power}") },
                                    modifier = Modifier.clickable { viewModel.addItem(match) }
                                )
                            }
                        }
                    }
                }

                // Items
                Text("ISSUED SERIALS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                uiState.items.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item.serialNumber) },
                        supportingContent = { Text(item.productName + " • Power: " + item.power) },
                        trailingContent = {
                            if (!uiState.isSaved) {
                                IconButton(onClick = { viewModel.removeItem(index) }) {
                                    Icon(Icons.Default.Delete, "Remove", tint = Color.Red)
                                    }
                                }
                            }
                        )
                    }

                OutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = viewModel::updateRemarks,
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(9.dp),
                    readOnly = uiState.isSaved
                )

                if (!uiState.isSaved) {
                    Button(
                        onClick = { viewModel.save() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(9.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) CircularProgressIndicator(color = Color.White)
                        else Text("SAVE SAMPLE DISTRIBUTION")
                    }
                }
                
                uiState.successMessage?.let {
                    Text(it, color = Color(0xFF28A745), fontWeight = FontWeight.Bold)
                }
                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SampleNumberField(
    label: String,
    prefix: String,
    number: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        if (prefix.isNotBlank()) {
            withStyle(
                style = SpanStyle(
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            ) {
                append(prefix)
            }
            append(" ")
        }
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                fontSize = if (prefix.isNotBlank()) 16.sp else 13.sp,
                color = if (prefix.isNotBlank()) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
            )
        ) {
            append(number)
        }
    }

    OutlinedTextField(
        value = annotatedString.text,
        onValueChange = {},
        readOnly = true,
        enabled = true,
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 16.sp),
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(9.dp),
        visualTransformation = {
            TransformedText(annotatedString, OffsetMapping.Identity)
        }
    )
}
