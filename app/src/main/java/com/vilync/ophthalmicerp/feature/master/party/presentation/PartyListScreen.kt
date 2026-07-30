package com.vilync.ophthalmicerp.feature.master.party.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType


@Composable
fun PartyListScreen(
    viewModel: com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModel,
    onAddParty: () -> Unit,
    onEditParty: (Long) -> Unit
) {

    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "CUSTOMER / VENDOR MASTER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Manage customers, vendors and business parties",
                    fontSize = 13.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }


            Button(
                onClick = onAddParty
            ) {

                Text(
                    text = "+ Add Party",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        // =====================================================
        // SEARCH
        // =====================================================

        OutlinedTextField(
            value = uiState.searchQuery,

            onValueChange =
                viewModel::updateSearchQuery,

            label = {
                Text(
                    text =
                        "Search by Party Name, Mobile, GSTIN, City..."
                )
            },

            singleLine = true,

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================================
        // PARTY TYPE FILTER
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            FilterChip(
                selected =
                    uiState.selectedPartyType == null,

                onClick = {
                    viewModel.updatePartyTypeFilter(
                        null
                    )
                },

                label = {
                    Text("All")
                }
            )


            FilterChip(
                selected =
                    uiState.selectedPartyType ==
                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER,

                onClick = {
                    viewModel.updatePartyTypeFilter(
                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER
                    )
                },

                label = {
                    Text("Customers")
                }
            )


            FilterChip(
                selected =
                    uiState.selectedPartyType ==
                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.VENDOR,

                onClick = {
                    viewModel.updatePartyTypeFilter(
                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.VENDOR
                    )
                },

                label = {
                    Text("Vendors")
                }
            )


            FilterChip(
                selected =
                    uiState.selectedPartyType ==
                            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH,

                onClick = {
                    viewModel.updatePartyTypeFilter(
                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.BOTH
                    )
                },

                label = {
                    Text("Customer & Vendor")
                }
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================================
        // TABLE HEADER
        // =====================================================

        Surface(
            color =
                MaterialTheme.colorScheme
                    .surfaceVariant,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "Party Name",
                    modifier =
                        Modifier.weight(2.2f)
                )

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "Type",
                    modifier =
                        Modifier.weight(1.2f)
                )

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "Mobile",
                    modifier =
                        Modifier.weight(1.2f)
                )

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "City",
                    modifier =
                        Modifier.weight(1.2f)
                )

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "GSTIN",
                    modifier =
                        Modifier.weight(1.7f)
                )

                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.HeaderText(
                    text = "Status",
                    modifier =
                        Modifier.weight(0.8f)
                )
            }
        }


        // =====================================================
        // CONTENT
        // =====================================================

        when {

            uiState.isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }


            !uiState.errorMessage.isNullOrBlank() -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            uiState.errorMessage
                                ?: "Unable to load parties.",

                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }


            uiState.parties.isEmpty() -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "No parties found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Add your first customer or vendor using + Add Party.",

                            fontSize = 13.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }


            else -> {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                ) {

                    items(
                        items = uiState.parties,
                        key = {
                            it.id
                        }
                    ) { party ->

                        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyRow(
                            party = party,

                            onClick = {
                                onEditParty(
                                    party.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// TABLE HEADER TEXT
// =============================================================

@Composable
private fun HeaderText(
    text: String,
    modifier: Modifier = Modifier
) {

    Text(
        text = text,

        modifier = modifier,

        fontSize = 12.sp,

        fontWeight =
            FontWeight.Bold,

        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}


// =============================================================
// PARTY ROW
// =============================================================

@Composable
private fun PartyRow(
    party: com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster,
    onClick: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 13.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            // PARTY NAME

            Column(
                modifier =
                    Modifier.weight(2.2f)
            ) {

                Text(
                    text =
                        party.partyName.ifBlank {
                            "-"
                        },

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                if (
                    party.contactPerson
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            party.contactPerson,

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            // TYPE

            Text(
                text =
                    party.partyType.displayName,

                modifier =
                    Modifier.weight(1.2f),

                fontSize = 13.sp
            )


            // MOBILE

            Text(
                text =
                    party.mobileNumber
                        .ifBlank {
                            "-"
                        },

                modifier =
                    Modifier.weight(1.2f),

                fontSize = 13.sp
            )


            // CITY

            Text(
                text =
                    party.city
                        .ifBlank {
                            "-"
                        },

                modifier =
                    Modifier.weight(1.2f),

                fontSize = 13.sp
            )


            // GSTIN

            Text(
                text =
                    party.gstin
                        .ifBlank {
                            "-"
                        },

                modifier =
                    Modifier.weight(1.7f),

                fontSize = 12.sp
            )


            // STATUS

            Text(
                text =
                    if (party.isActive) {
                        "Active"
                    } else {
                        "Inactive"
                    },

                modifier =
                    Modifier.weight(0.8f),

                fontSize = 12.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    if (party.isActive) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
            )
        }
    }
}