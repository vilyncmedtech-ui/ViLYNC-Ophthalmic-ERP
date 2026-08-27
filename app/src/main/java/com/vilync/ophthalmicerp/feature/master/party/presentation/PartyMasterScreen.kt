package com.vilync.ophthalmicerp.feature.master.party.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType


@Composable
fun PartyMasterScreen(
    viewModel: com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterViewModel,
    onPartySaved: (Long) -> Unit = {},
    onPartyDeleted: () -> Unit = {},
    onSearchGstin: (String) -> Unit = {}
) {

    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val isSaving by
    viewModel.isSaving.collectAsStateWithLifecycle()


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Text(
            text =
                if (uiState.isEditMode) {
                    "EDIT PARTY"
                } else {
                    "PARTY MASTER"
                },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )


        Text(
            text =
                if (uiState.isEditMode) {
                    "Update or delete existing party information"
                } else {
                    "Add customer, vendor and business information"
                },
            fontSize = 13.sp,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        // =====================================================
        // PARTY INFORMATION
        // =====================================================

        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.SectionTitle(
            title = "PARTY INFORMATION"
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                ),
            verticalAlignment =
                Alignment.Top
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTypeField(
                selectedType =
                    uiState.partyType,

                onTypeSelected =
                    viewModel::updatePartyType,

                modifier =
                    Modifier.weight(
                        0.85f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.partyName,

                onValueChange =
                    viewModel::updatePartyName,

                label =
                    "Party / Business Name *",

                modifier =
                    Modifier.weight(
                        1.5f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.contactPerson,

                onValueChange =
                    viewModel::updateContactPerson,

                label =
                    "Contact Person",

                modifier =
                    Modifier.weight(
                        1.15f
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        // =====================================================
        // CONTACT & TAX INFORMATION
        // =====================================================

        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.SectionTitle(
            title =
                "CONTACT & TAX INFORMATION"
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.mobileNumber,

                onValueChange =
                    viewModel::updateMobileNumber,

                label =
                    "Mobile Number",

                keyboardType =
                    KeyboardType.Phone,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.alternateMobileNumber,

                onValueChange =
                    viewModel::updateAlternateMobileNumber,

                label =
                    "Alternate Mobile",

                keyboardType =
                    KeyboardType.Phone,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.email,

                onValueChange =
                    viewModel::updateEmail,

                label =
                    "Email",

                keyboardType =
                    KeyboardType.Email,

                modifier =
                    Modifier.weight(
                        1.35f
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.gstin,

                onValueChange =
                    viewModel::updateGstin,

                label =
                    "GSTIN",

                modifier =
                    Modifier.weight(
                        1.25f
                    )
            )


            Button(
                onClick = {

                    viewModel.startGstinLookup()

                    if (
                        uiState.gstin.length == 15
                    ) {

                        onSearchGstin(
                            uiState.gstin
                        )
                    }
                },

                enabled =
                    !uiState.isSearchingGstin &&
                            uiState.gstin.length == 15,

                modifier =
                    Modifier
                        .weight(
                            0.65f
                        )
                        .height(
                            56.dp
                        )
            ) {

                if (
                    uiState.isSearchingGstin
                ) {

                    CircularProgressIndicator()

                } else {

                    Text(
                        text =
                            "Search GSTIN"
                    )
                }
            }


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.panNumber,

                onValueChange =
                    viewModel::updatePanNumber,

                label =
                    "PAN",

                modifier =
                    Modifier.weight(
                        0.9f
                    )
            )
        }


        if (
            !uiState.gstinLookupMessage
                .isNullOrBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )


            Text(
                text =
                    uiState.gstinLookupMessage
                        ?: "",

                fontSize =
                    12.sp,

                color =
                    if (
                        uiState.gstinLookupSuccessful
                    ) {
                        Color(
                            0xFF2E7D32
                        )
                    } else {
                        MaterialTheme.colorScheme.error
                    }
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        // =====================================================
        // ADDRESS
        // =====================================================

        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.SectionTitle(
            title =
                "ADDRESS"
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.addressLine1,

                onValueChange =
                    viewModel::updateAddressLine1,

                label =
                    "Address Line 1",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.addressLine2,

                onValueChange =
                    viewModel::updateAddressLine2,

                label =
                    "Address Line 2",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.city,

                onValueChange =
                    viewModel::updateCity,

                label =
                    "City",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.district,

                onValueChange =
                    viewModel::updateDistrict,

                label =
                    "District",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.state,

                onValueChange =
                    viewModel::updateState,

                label =
                    "State",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.pinCode,

                onValueChange =
                    viewModel::updatePinCode,

                label =
                    "PIN Code",

                keyboardType =
                    KeyboardType.Number,

                modifier =
                    Modifier.weight(
                        0.7f
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        // =====================================================
        // COMMERCIAL TERMS
        // =====================================================

        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.SectionTitle(
            title =
                "COMMERCIAL TERMS"
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.CreditDaysField(
                value =
                    uiState.creditDays,

                onValueChange =
                    viewModel::updateCreditDays,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    uiState.creditLimit,

                onValueChange =
                    viewModel::updateCreditLimit,

                label =
                    "Credit Limit",

                keyboardType =
                    KeyboardType.Decimal,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            Row(
                modifier =
                    Modifier.weight(
                        1f
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Checkbox(
                    checked =
                        uiState.isActive,

                    onCheckedChange =
                        viewModel::updateIsActive
                )


                Text(
                    text =
                        if (uiState.isActive) {
                            "Active"
                        } else {
                            "Inactive"
                        },

                    fontWeight =
                        FontWeight.Medium
                )
            }
        }


        // =====================================================
        // ERROR
        // =====================================================

        if (
            !uiState.errorMessage
                .isNullOrBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(
                text =
                    uiState.errorMessage
                        ?: "",

                color =
                    MaterialTheme.colorScheme.error,

                fontSize =
                    13.sp
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        // =====================================================
        // ACTION BUTTONS
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {


            Button(
                onClick = {

                    viewModel.saveParty(
                        onSuccess =
                            onPartySaved
                    )
                },

                enabled =
                    !isSaving &&
                            !uiState.isLoadingParty,

                modifier =
                    Modifier
                        .weight(
                            3f
                        )
                        .height(
                            52.dp
                        )
            ) {

                if (isSaving) {

                    CircularProgressIndicator()

                } else {

                    Text(
                        text =
                            if (
                                uiState.isEditMode
                            ) {
                                "Update Party"
                            } else {
                                "Save Party"
                            },

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            if (
                uiState.isEditMode
            ) {

                Button(
                    onClick = {

                        viewModel.deleteParty(
                            onSuccess =
                                onPartyDeleted
                        )
                    },

                    enabled =
                        !isSaving,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(
                                    0xFFC62828
                                ),

                            contentColor =
                                Color.White
                        ),

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .height(
                                52.dp
                            )
                ) {

                    Text(
                        text =
                            "Delete Party",

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )
    }
}


// =============================================================
// SECTION TITLE
// =============================================================

@Composable
private fun SectionTitle(
    title: String
) {

    Text(
        text =
            title,

        fontSize =
            13.sp,

        fontWeight =
            FontWeight.Bold,

        color =
            MaterialTheme.colorScheme.primary
    )
}


// =============================================================
// COMMON TEXT FIELD
// =============================================================

@Composable
private fun PartyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType =
        KeyboardType.Text
) {

    OutlinedTextField(
        value =
            value,

        onValueChange =
            onValueChange,

        label = {
            Text(
                text =
                    label
            )
        },

        singleLine =
            true,

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    keyboardType
            ),

        modifier =
            modifier
                .height(
                    64.dp
                )
    )
}


// =============================================================
// PARTY TYPE DROPDOWN
// =============================================================

@Composable
private fun PartyTypeField(
    selectedType: com.vilync.ophthalmicerp.feature.master.party.model.PartyType,
    onTypeSelected: (com.vilync.ophthalmicerp.feature.master.party.model.PartyType) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by
    remember {
        mutableStateOf(
            false
        )
    }

    val typeColor =
        when (
            selectedType.displayName
                .trim()
                .lowercase()
        ) {
            "customer" ->
                Color(0xFF1565C0)

            "vendor" ->
                Color(0xFFEF6C00)

            else ->
                Color(0xFF6A1B9A)
        }

    Column(
        modifier =
            modifier
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },

            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor =
                        typeColor.copy(
                            alpha = 0.10f
                        ),
                    contentColor =
                        typeColor
                ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        64.dp
                    )
        ) {

            Text(
                text =
                    selectedType.displayName,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Text(
                text = "▼",
                color = typeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded =
                expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.entries.forEach {
                    partyType ->

                val optionColor =
                    when (
                        partyType.displayName
                            .trim()
                            .lowercase()
                    ) {
                        "customer" ->
                            Color(0xFF1565C0)

                        "vendor" ->
                            Color(0xFFEF6C00)

                        else ->
                            Color(0xFF6A1B9A)
                    }

                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                partyType.displayName,
                            color =
                                optionColor,
                            fontWeight =
                                if (
                                    partyType ==
                                    selectedType
                                ) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                }
                        )
                    },

                    onClick = {
                        onTypeSelected(
                            partyType
                        )

                        expanded = false
                    }
                )
            }
        }
    }
}


// =============================================================
// CREDIT DAYS DROPDOWN
// =============================================================

@Composable
private fun CreditDaysField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by
    remember {
        mutableStateOf(
            false
        )
    }

    var customMode by
    remember(
        value
    ) {
        mutableStateOf(
            value.isNotBlank() &&
                    value !in listOf(
                "0",
                "30",
                "60",
                "90"
            )
        )
    }

    if (customMode) {

        Row(
            modifier =
                modifier,
            horizontalArrangement =
                Arrangement.spacedBy(
                    6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyTextField(
                value =
                    value,
                onValueChange = { newValue ->

                    if (
                        newValue.all {
                            it.isDigit()
                        }
                    ) {
                        onValueChange(
                            newValue
                        )
                    }
                },
                label =
                    "Credit Days",
                keyboardType =
                    KeyboardType.Number,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            OutlinedButton(
                onClick = {
                    customMode = false
                    expanded = true
                },
                modifier =
                    Modifier.height(
                        64.dp
                    )
            ) {
                Text(
                    text = "▼",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    } else {

        Column(
            modifier =
                modifier
        ) {

            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            64.dp
                        )
            ) {

                Text(
                    text =
                        if (
                            value.isBlank()
                        ) {
                            "Credit Days"
                        } else {
                            "$value Days"
                        },
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                Text(
                    text = "▼",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            DropdownMenu(
                expanded =
                    expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                listOf(
                    "0",
                    "30",
                    "60",
                    "90"
                ).forEach {
                        days ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text =
                                    "$days Days"
                            )
                        },
                        onClick = {
                            onValueChange(
                                days
                            )
                            customMode = false
                            expanded = false
                        }
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                "Custom"
                        )
                    },
                    onClick = {
                        customMode = true
                        expanded = false

                        if (
                            value in listOf(
                                "0",
                                "30",
                                "60",
                                "90"
                            )
                        ) {
                            onValueChange(
                                ""
                            )
                        }
                    }
                )
            }
        }
    }
}
