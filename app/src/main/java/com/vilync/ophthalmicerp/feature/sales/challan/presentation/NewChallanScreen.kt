package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.ui.components.PartySearchField
import java.util.Calendar
import java.util.Locale

private val ChallanNavy = Color(0xFF071B33)
private val ChallanGold = Color(0xFFD4AF37)
private val ChallanSoftGreen = Color(0xFFEAF7EE)
private val ChallanSoftBlue = Color(0xFFEAF2FF)

@Composable
fun NewChallanScreen(
    viewModel: NewChallanViewModel,
    onBack: () -> Unit = {},
    onSavedToDetail: (Long) -> Unit = {}
) {

    val state by
    viewModel.uiState.collectAsState()

    val context =
        LocalContext.current

    androidx.compose.runtime.LaunchedEffect(state.savedChallanId) {
        if (state.savedChallanId != null) {
            onSavedToDetail(state.savedChallanId!!)
            viewModel.clearMessages()
        }
    }

    val calendar =
        Calendar.getInstance()

    val datePicker =
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateChallanDate(
                    String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%04d",
                        day,
                        month + 1,
                        year
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "New Challan",
                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,
                    color = ChallanNavy,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Physical stock issue without invoice",
                    color = Color.DarkGray
                )
            }

            OutlinedButton(
                onClick = onBack
            ) {
                Text("Back")
            }
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        ChallanSoftBlue
                ),
            shape =
                RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Challan Details",
                    fontWeight = FontWeight.Bold,
                    color = ChallanNavy
                )

                PartySearchField(
                    label = "Customer / Hospital *",
                    selectedParty = state.selectedCustomer,
                    allParties = state.customers,
                    onPartySelected = { viewModel.selectCustomer(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedTextField(
                        value = "Auto-generated",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Challan No. *")
                        },
                        modifier =
                            Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value =
                            state.challanDate,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Challan Date *")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    datePicker.show()
                                }
                            ) {
                                Text(
                                    text = "📅"
                                )
                            }
                        },
                        modifier =
                            Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value =
                        state.remarks,
                    onValueChange =
                        viewModel::updateRemarks,
                    label = {
                        Text("Remarks")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        ChallanSoftGreen
                ),
            shape =
                RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Add Physical Stock",
                    fontWeight = FontWeight.Bold,
                    color = ChallanNavy
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedTextField(
                        value =
                            state.serialQuery,
                        onValueChange =
                            viewModel::updateSerialQuery,
                        label = {
                            Text(
                                "Serial / Unique ID *"
                            )
                        },
                        modifier =
                            Modifier.weight(1.5f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value =
                            state.rate,
                        onValueChange =
                            viewModel::updateRate,
                        label = {
                            Text("Rate")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        modifier =
                            Modifier.weight(0.75f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value =
                            state.gstPercent,
                        onValueChange =
                            viewModel::updateGstPercent,
                        label = {
                            Text("GST %")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        modifier =
                            Modifier.weight(0.65f),
                        singleLine = true
                    )
                }

                Button(
                    onClick =
                        viewModel::searchSerial,
                    enabled =
                        !state.isSearchingSerial,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ChallanNavy
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isSearchingSerial) {
                            "Searching..."
                        } else {
                            "ADD SERIAL"
                        }
                    )
                }

                if (
                    state.serialMatches.isNotEmpty()
                ) {

                    Text(
                        text =
                            "Multiple serials matched. Select the correct physical unit:",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    state.serialMatches
                        .forEach { unit ->

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel
                                                .chooseSerialMatch(
                                                    unit
                                                )
                                        }
                            ) {

                                Text(
                                    text =
                                        "${unit.serialNumber}   |   ${unit.power}   |   Batch ${unit.batchNumber}",
                                    modifier =
                                        Modifier.padding(12.dp)
                                )
                            }
                        }
                }
            }
        }

        Text(
            text =
                "Selected Items (${state.items.size})",
            fontWeight = FontWeight.Bold,
            color = ChallanNavy
        )

        state.items
            .forEachIndexed { index, item ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(14.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text =
                                "${index + 1}. ${item.productName}",
                            fontWeight =
                                FontWeight.Bold,
                            color = ChallanNavy
                        )

                        Text(
                            "Serial: ${item.serialNumber}"
                        )

                        Text(
                            "Power: ${item.power.ifBlank { "-" }}   •   Batch: ${item.batchNumber.ifBlank { "-" }}"
                        )

                        Text(
                            "Rate: ${item.rate}   •   GST: ${item.gstPercent}%"
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.removeItem(
                                    item.inventoryUnitId
                                )
                            }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

        state.errorMessage
            ?.let { message ->

                Text(
                    text = message,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

        state.successMessage
            ?.let { message ->

                Text(
                    text = message,
                    color = Color(0xFF1B5E20),
                    fontWeight =
                        FontWeight.Bold
                )
            }

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Button(
            onClick =
                viewModel::saveChallan,
            enabled =
                !state.isSaving,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ChallanGold,
                    contentColor =
                        ChallanNavy
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (state.isSaving) {
                        "SAVING CHALLAN..."
                    } else {
                        "SAVE CHALLAN"
                    },
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
