package com.vilync.ophthalmicerp.feature.settings.audit

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.data.entity.AuditTrailEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// =============================================================
// AUDIT TRAIL SCREEN
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditTrailScreen(
    viewModel: AuditTrailViewModel,
    onBack: () -> Unit
) {

    val uiState by
    viewModel.uiState.collectAsState()


    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Audit Trail")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 12.dp
                )
        ) {

            // =================================================
            // SEARCH
            // =================================================

            OutlinedTextField(

                value =
                    uiState.searchQuery,

                onValueChange = {
                    viewModel.updateSearchQuery(it)
                },

                label = {
                    Text("Search audit trail")
                },

                placeholder = {
                    Text(
                        "User, action, module, reference..."
                    )
                },

                singleLine = true,

                trailingIcon = {

                    if (
                        uiState.searchQuery.isNotBlank()
                    ) {

                        TextButton(
                            onClick = {
                                viewModel.clearSearch()
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                },

                modifier = Modifier.fillMaxWidth()
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // =================================================
            // LOADING
            // =================================================

            if (uiState.isLoading) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text("Loading audit trail...")
                }

                return@Column
            }


            // =================================================
            // ERROR
            // =================================================

            uiState.errorMessage?.let { message ->

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = message,
                        color =
                            MaterialTheme.colorScheme.error
                    )

                    if (
                        uiState.searchQuery.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.clearSearch()
                            }
                        ) {
                            Text("Clear Search")
                        }
                    }
                }

                return@Column
            }


            // =================================================
            // EMPTY
            // =================================================

            if (uiState.records.isEmpty()) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        if (
                            uiState.searchQuery.isBlank()
                        ) {
                            "No audit records found."
                        } else {
                            "No matching audit records found."
                        }
                    )
                }

                return@Column
            }


            // =================================================
            // RECORD COUNT
            // =================================================

            Text(
                text =
                    "${uiState.records.size} record(s)",
                style =
                    MaterialTheme.typography.labelMedium
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            // =================================================
            // COMPACT AUDIT LIST
            // =================================================

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                items(
                    items = uiState.records,
                    key = { audit ->
                        audit.id
                    }
                ) { audit ->

                    CompactAuditRow(
                        audit = audit
                    )

                    HorizontalDivider()
                }
            }
        }
    }
}


// =============================================================
// COMPACT AUDIT ROW
// =============================================================

@Composable
private fun CompactAuditRow(
    audit: AuditTrailEntity
) {

    var expanded by
    remember(audit.id) {
        mutableStateOf(false)
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
            }
            .padding(
                vertical = 9.dp,
                horizontal = 4.dp
            )
    ) {

        // =====================================================
        // PRIMARY LINE
        // =====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // TIME

            Text(
                text =
                    formatAuditTime(
                        audit.createdAt
                    ),
                modifier =
                    Modifier.weight(0.17f),
                style =
                    MaterialTheme.typography.bodyMedium,
                fontWeight =
                    FontWeight.SemiBold
            )


            // USER

            Text(
                text =
                    compactUserName(audit),
                modifier =
                    Modifier.weight(0.31f),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.bodyMedium
            )


            // ACTION

            Text(
                text =
                    audit.action,
                modifier =
                    Modifier.weight(0.25f),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.bodyMedium,
                fontWeight =
                    FontWeight.Bold
            )


            // MODULE

            Text(
                text =
                    audit.module,
                modifier =
                    Modifier.weight(0.22f),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.labelMedium
            )


            Text(
                text =
                    if (expanded) "▲" else "▼",
                style =
                    MaterialTheme.typography.labelSmall
            )
        }


        // =====================================================
        // SECOND LINE
        // =====================================================

        compactSecondLine(audit)
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let { secondLine ->

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text = secondLine,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


        // =====================================================
        // EXPANDED DETAILS
        // =====================================================

        if (expanded) {

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    DetailRow(
                        "Date / Time",
                        formatAuditDateTime(
                            audit.createdAt
                        )
                    )


                    DetailRow(
                        "User",
                        audit.userDisplayName
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "System"
                    )


                    audit.username
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            DetailRow(
                                "Username",
                                it
                            )
                        }


                    audit.userRole
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            DetailRow(
                                "Role",
                                it
                            )
                        }


                    DetailRow(
                        "Module",
                        audit.module
                    )


                    DetailRow(
                        "Action",
                        audit.action
                    )


                    audit.referenceNumber
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            DetailRow(
                                "Reference",
                                it
                            )
                        }


                    audit.recordId
                        ?.let {
                            DetailRow(
                                "Record ID",
                                it.toString()
                            )
                        }


                    audit.financialYear
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            DetailRow(
                                "Financial Year",
                                it
                            )
                        }


                    audit.description
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text = it,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }


                    audit.fieldName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { field ->

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            HorizontalDivider()

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            Text(
                                text =
                                    "Changed Field: $field",
                                fontWeight =
                                    FontWeight.SemiBold,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )


                            audit.oldValue?.let {

                                DetailRow(
                                    "Old",
                                    it.ifBlank {
                                        "(empty)"
                                    }
                                )
                            }


                            audit.newValue?.let {

                                DetailRow(
                                    "New",
                                    it.ifBlank {
                                        "(empty)"
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
}


// =============================================================
// COMPACT USER NAME
// =============================================================

private fun compactUserName(
    audit: AuditTrailEntity
): String {

    return audit.userDisplayName
        ?.takeIf {
            it.isNotBlank()
        }
        ?: audit.username
            ?.takeIf {
                it.isNotBlank()
            }
        ?: "System"
}


// =============================================================
// SECOND LINE
// =============================================================

private fun compactSecondLine(
    audit: AuditTrailEntity
): String? {

    val reference =
        audit.referenceNumber
            ?.takeIf {
                it.isNotBlank()
            }


    val description =
        audit.description
            ?.takeIf {
                it.isNotBlank()
            }


    return when {

        reference != null &&
                description != null -> {

            "$reference • $description"
        }


        reference != null -> {

            reference
        }


        description != null -> {

            description
        }


        else -> null
    }
}


// =============================================================
// DETAIL ROW
// =============================================================

@Composable
private fun DetailRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 2.dp
            )
    ) {

        Text(
            text = "$label:",
            modifier =
                Modifier.weight(0.35f),
            fontWeight =
                FontWeight.SemiBold,
            style =
                MaterialTheme.typography.bodySmall
        )


        Text(
            text = value,
            modifier =
                Modifier.weight(0.65f),
            style =
                MaterialTheme.typography.bodySmall
        )
    }
}


// =============================================================
// TIME ONLY
// =============================================================

private fun formatAuditTime(
    timestamp: Long
): String {

    return try {

        SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(
            Date(timestamp)
        )

    } catch (_: Exception) {

        "-"
    }
}


// =============================================================
// FULL DATE / TIME
// =============================================================

private fun formatAuditDateTime(
    timestamp: Long
): String {

    return try {

        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        ).format(
            Date(timestamp)
        )

    } catch (_: Exception) {

        timestamp.toString()
    }
}