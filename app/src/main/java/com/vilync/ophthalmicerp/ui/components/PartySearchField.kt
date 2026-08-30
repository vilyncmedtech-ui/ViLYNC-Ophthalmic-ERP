package com.vilync.ophthalmicerp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster

/**
 * Reusable Party Auto Search component for ERP documents.
 */
@Composable
fun PartySearchField(
    label: String,
    selectedParty: PartyMaster?,
    allParties: List<PartyMaster>,
    onPartySelected: (PartyMaster) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    onAddNewLabel: String = "+ New Party",
    onAddNew: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(selectedParty?.partyName ?: "") }

    // Sync search query with external selection
    LaunchedEffect(selectedParty) {
        if (selectedParty != null && searchQuery != selectedParty.partyName) {
            searchQuery = selectedParty.partyName
        } else if (selectedParty == null) {
            searchQuery = ""
        }
    }

    Box(modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                if (!readOnly) {
                    searchQuery = it
                    expanded = it.isNotBlank()
                }
            },
            label = { Text(label, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            textStyle = textStyle,
            trailingIcon = {
                if (!readOnly) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(9.dp)
        )

        val filtered = remember(searchQuery, allParties) {
            if (searchQuery.isBlank()) allParties.take(20)
            else allParties.filter { 
                it.partyName.contains(searchQuery, ignoreCase = true) ||
                it.legalName.contains(searchQuery, ignoreCase = true) ||
                it.gstin.contains(searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(searchQuery, ignoreCase = true)
            }.take(10)
        }

        if (expanded && (filtered.isNotEmpty() || onAddNew != null) && !readOnly) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false)
            ) {
                if (onAddNew != null) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = onAddNewLabel,
                                    color = Color(0xFF476EA8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onAddNew()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }

                filtered.forEach { party ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(party.partyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (party.legalName.isNotBlank() && party.legalName != party.partyName) {
                                    Text(party.legalName, fontSize = 12.sp, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (party.gstin.isNotBlank()) {
                                        Text("GST: ${party.gstin}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    if (party.city.isNotBlank()) {
                                        Text("City: ${party.city}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        },
                        onClick = {
                            onPartySelected(party)
                            searchQuery = party.partyName
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
