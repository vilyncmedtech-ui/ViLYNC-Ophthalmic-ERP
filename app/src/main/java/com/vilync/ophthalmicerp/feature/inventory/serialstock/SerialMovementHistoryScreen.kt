package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity


// =============================================================
// COLOURS
// =============================================================

private val PageBackground =
    Color(0xFFF7F9FD)

private val White =
    Color(0xFFFFFFFF)

private val Navy =
    Color(0xFF102A56)

private val NavyDark =
    Color(0xFF071B33)

private val Gold =
    Color(0xFFD4AF37)

private val TextPrimary =
    Color(0xFF17233B)

private val TextSecondary =
    Color(0xFF6F7C96)

private val Border =
    Color(0xFFE3E8F1)

private val LightBlue =
    Color(0xFFF0F5FF)

private val Blue =
    Color(0xFF3267C8)

private val LightGreen =
    Color(0xFFEDF9F2)

private val Green =
    Color(0xFF21834D)

private val LightOrange =
    Color(0xFFFFF5E9)

private val Orange =
    Color(0xFFC96A12)

private val LightPurple =
    Color(0xFFF4EEFF)

private val Purple =
    Color(0xFF7046B5)

private val LightRed =
    Color(0xFFFFEEEE)

private val Red =
    Color(0xFFB83A3A)

private val SoftGrey =
    Color(0xFFF6F8FC)


// =============================================================
// SCREEN
// =============================================================

@Composable
fun SerialMovementHistoryScreen(

    viewModel: SerialMovementHistoryViewModel,

    onBack: () -> Unit

) {

    val uiState by
    viewModel
        .uiState
        .collectAsStateWithLifecycle()


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    PageBackground
                )
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        MovementHistoryHeader(
            onBack = onBack
        )


        // =====================================================
        // CONTENT
        // =====================================================

        when {

            uiState.isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = Gold
                    )
                }
            }


            uiState.errorMessage != null -> {

                MovementHistoryError(
                    message =
                        uiState.errorMessage
                            ?: "Unable to load movement history.",

                    onRetry = {
                        viewModel.refresh()
                    }
                )
            }


            else -> {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            top = 18.dp,
                            bottom = 32.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        )
                ) {


                    // =========================================
                    // SERIAL SUMMARY
                    // =========================================

                    item {

                        SerialSummaryCard(
                            uiState = uiState
                        )
                    }


                    // =========================================
                    // MOVEMENT TITLE
                    // =========================================

                    item {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "Movement History",

                                fontSize =
                                    21.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    NavyDark
                            )


                            Text(
                                text =
                                    "${uiState.movementCount} Entries",

                                fontSize =
                                    12.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                color =
                                    TextSecondary
                            )
                        }
                    }


                    // =========================================
                    // EMPTY HISTORY
                    // =========================================

                    if (
                        uiState.movements.isEmpty()
                    ) {

                        item {

                            EmptyMovementHistory()
                        }
                    }


                    // =========================================
                    // MOVEMENT LIST
                    // =========================================

                    else {

                        items(
                            items =
                                uiState.movements,

                            key = {
                                it.id
                            }
                        ) { movement ->

                            MovementTimelineCard(
                                movement =
                                    movement
                            )
                        }
                    }
                }
            }
        }
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun MovementHistoryHeader(

    onBack: () -> Unit

) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 18.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // =================================================
            // BACK
            // =================================================

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            color =
                                White,

                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        )
                        .border(
                            width =
                                1.dp,

                            color =
                                Color(
                                    0xFFEBD99B
                                ),

                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        )
                        .clickable {
                            onBack()
                        },

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "‹",

                    fontSize =
                        34.sp,

                    color =
                        Color(
                            0xFFB98A00
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "Movement History",

                    fontSize =
                        23.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        NavyDark
                )


                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )


                Text(
                    text =
                        "Complete serial-wise stock trail",

                    fontSize =
                        13.sp,

                    color =
                        TextSecondary
                )
            }
        }
    }
}


// =============================================================
// SERIAL SUMMARY
// =============================================================

@Composable
private fun SerialSummaryCard(

    uiState: SerialMovementHistoryUiState

) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    )
        ) {

            // =================================================
            // ROW 1: PRODUCT NAME (MODEL) + STATUS
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                val displayName =
                    if (
                        uiState.model.isNotBlank()
                    ) {
                        "${uiState.productName} (${uiState.model})"
                    } else {
                        uiState.productName
                            .ifBlank {
                                "Serial Inventory"
                            }
                    }


                Text(
                    text =
                        displayName,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        NavyDark,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Text(
                    text = "  ·  ",
                    fontSize = 14.sp,
                    color = Border
                )


                HistoryStatusBadge(
                    status =
                        uiState.currentStatus
                )
            }


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // =================================================
            // ROW 2: SERIAL · INVOICE · POWER
            // =================================================

            val displaySerial =
                if (
                    uiState.serialPrefix.isNotBlank() &&
                    !uiState.serialNumber.startsWith(
                        uiState.serialPrefix,
                        ignoreCase = true
                    )
                ) {
                    "${uiState.serialPrefix} ${uiState.serialNumber}"
                } else {
                    uiState.serialNumber
                        .ifBlank {
                            "—"
                        }
                }


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Serial: ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text(
                    text = displaySerial,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )


                Text(
                    text = "  ·  ",
                    fontSize = 12.sp,
                    color = Border
                )


                Text(
                    text = "Invoice: ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text(
                    text =
                        uiState.purchaseInvoiceNumber
                            .ifBlank {
                                "—"
                            },

                    modifier =
                        Modifier.weight(1f, fill = false),

                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )


                Text(
                    text = "  ·  ",
                    fontSize = 12.sp,
                    color = Border
                )


                Text(
                    text = "Power: ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text(
                    text =
                        uiState.power
                            .ifBlank {
                                "—"
                            },

                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue
                )
            }


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            // =================================================
            // ROW 3: BATCH · EXPIRY · RECEIVED
            // =================================================

            val row3Text =
                listOf(
                    "Batch: ${uiState.batchNumber.ifBlank { "—" }}",
                    "Expiry: ${formatExpiryForDisplay(uiState.expiryDate)}",
                    "Received: ${uiState.receivedDate.ifBlank { "—" }}"
                ).joinToString(
                    separator = "  ·  "
                )


            Text(
                text =
                    row3Text,

                fontSize =
                    12.sp,

                color =
                    TextPrimary,

                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            HorizontalDivider(
                color =
                    Border,

                thickness =
                    0.5.dp
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // =================================================
            // ROW 4: SUPPLIER
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Supplier: ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Text(
                    text =
                        uiState.supplierName
                            .ifBlank {
                                "—"
                            },

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        TextPrimary
                )
            }
        }
    }
}


// =============================================================
// MOVEMENT TIMELINE CARD
// =============================================================

@Composable
private fun MovementTimelineCard(

    movement: StockMovementEntity

) {

    val movementTitle =
        movement.movementType
            .trim()
            .replace(
                "_",
                " "
            )
            .ifBlank {
                "STOCK MOVEMENT"
            }


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    1.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.Top
        ) {


            // =================================================
            // TIMELINE INDICATOR
            // =================================================

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .background(
                                color =
                                    movementColour(
                                        movement.movementType
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        50.dp
                                    )
                            )
                            .border(
                                width =
                                    2.dp,

                                color =
                                    movementBackgroundColour(
                                        movement.movementType
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        50.dp
                                    )
                            )
                )


                Box(
                    modifier =
                        Modifier
                            .padding(
                                top = 4.dp
                            )
                            .width(1.5.dp)
                            .height(30.dp)
                            .background(
                                Border
                            )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )


            // =================================================
            // COMPACT MOVEMENT INFORMATION
            // =================================================

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                // ---------------------------------------------
                // LINE 1: TITLE · DATE · STATUS
                // ---------------------------------------------

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            movementTitle,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            movementColour(
                                movement.movementType
                            )
                    )


                    Text(
                        text =
                            "  ·  ",

                        fontSize =
                            12.sp,

                        color =
                            Border
                    )


                    Text(
                        text =
                            movement.movementDate,

                        fontSize =
                            11.sp,

                        color =
                            TextSecondary
                    )


                    if (
                        movement.fromStatus.isNotBlank() ||
                        movement.toStatus.isNotBlank()
                    ) {

                        Text(
                            text =
                                "  ·  ",

                            fontSize =
                                12.sp,

                            color =
                                Border
                        )


                        Text(
                            text =
                                movement.fromStatus
                                    .replace(
                                        "_",
                                        " "
                                    )
                                    .ifBlank {
                                        "NEW"
                                    },

                            fontSize =
                                10.sp,

                            color =
                                TextSecondary
                        )


                        Text(
                            text = " → ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )


                        Text(
                            text =
                                movement.toStatus
                                    .replace(
                                        "_",
                                        " "
                                    )
                                    .ifBlank {
                                        "—"
                                    },

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                TextPrimary
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                // ---------------------------------------------
                // LINE 2: PARTY · REF · REMARKS
                // ---------------------------------------------

                val details =
                    mutableListOf<String>()


                if (
                    movement.partyName.isNotBlank()
                ) {
                    details.add(
                        "Party: ${movement.partyName}"
                    )
                }


                if (
                    movement.referenceNumber.isNotBlank()
                ) {
                    details.add(
                        "Ref: ${movement.referenceNumber}"
                    )
                }


                if (
                    movement.remarks.isNotBlank()
                ) {
                    details.add(
                        "Remarks: ${movement.remarks}"
                    )
                }


                if (
                    details.isNotEmpty()
                ) {

                    Text(
                        text =
                            details.joinToString(
                                separator = "  ·  "
                            ),

                        fontSize =
                            11.sp,

                        color =
                            TextSecondary,

                        maxLines =
                            2,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// =============================================================
// CURRENT STATUS BADGE
// =============================================================

@Composable
private fun HistoryStatusBadge(

    status: String

) {

    val normalized =
        status
            .trim()
            .uppercase()


    val background =
        movementBackgroundColour(
            normalized
        )


    val foreground =
        movementColour(
            normalized
        )


    Box(
        modifier =
            Modifier
                .background(
                    color =
                        background,

                    shape =
                        RoundedCornerShape(
                            50.dp
                        )
                )
                .padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                )
    ) {

        Text(
            text =
                normalized
                    .replace(
                        "_",
                        " "
                    )
                    .ifBlank {
                        "UNKNOWN"
                    },

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                foreground
        )
    }
}


// =============================================================
// EMPTY HISTORY
// =============================================================

@Composable
private fun EmptyMovementHistory() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    LightBlue
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 24.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "No Movement History",

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    NavyDark
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            Text(
                text =
                    "No stock movement entries are recorded for this serial yet.",

                fontSize =
                    12.sp,

                color =
                    TextSecondary
            )
        }
    }
}


// =============================================================
// ERROR
// =============================================================

@Composable
private fun MovementHistoryError(

    message: String,

    onRetry: () -> Unit

) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(18.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        LightRed
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        "Unable to Load History",

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Red
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        message,

                    fontSize =
                        12.sp,

                    color =
                        TextPrimary
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Button(
                    onClick =
                        onRetry,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Navy,

                            contentColor =
                                White
                        )
                ) {

                    Text(
                        text =
                            "Retry",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// =============================================================
// EXPIRY DISPLAY FORMAT
// =============================================================

/**
 * Database format:
 *
 * 1029 -> October 2029
 * 1229 -> December 2029
 *
 * UI format:
 *
 * 10-29
 * 12-29
 */
private fun formatExpiryForDisplay(

    expiryDate: String

): String {

    val clean =
        expiryDate
            .trim()
            .replace(
                "-",
                ""
            )
            .replace(
                "/",
                ""
            )


    if (
        clean.length == 4 &&
        clean.all {
            it.isDigit()
        }
    ) {

        return "${clean.substring(0, 2)}-${clean.substring(2, 4)}"
    }


    return expiryDate
        .trim()
        .ifBlank {
            "—"
        }
}


// =============================================================
// MOVEMENT COLOUR
// =============================================================

private fun movementColour(

    movementType: String

): Color {

    val normalized =
        movementType
            .trim()
            .uppercase()


    return when {

        normalized.contains(
            "PURCHASE"
        ) ->
            Green

        normalized.contains(
            "RETURN"
        ) ->
            Purple

        normalized.contains(
            "SAMPLE"
        ) ->
            Blue

        normalized.contains(
            "DEMO"
        ) ->
            Purple

        normalized.contains(
            "APPROVAL"
        ) ->
            Orange

        normalized.contains(
            "SOLD"
        ) ->
            Red

        normalized == "IN_STOCK" ->
            Green

        else ->
            Navy
    }
}


// =============================================================
// MOVEMENT BACKGROUND COLOUR
// =============================================================

private fun movementBackgroundColour(

    movementType: String

): Color {

    val normalized =
        movementType
            .trim()
            .uppercase()


    return when {

        normalized.contains(
            "PURCHASE"
        ) ->
            LightGreen

        normalized.contains(
            "RETURN"
        ) ->
            LightPurple

        normalized.contains(
            "SAMPLE"
        ) ->
            LightBlue

        normalized.contains(
            "DEMO"
        ) ->
            LightPurple

        normalized.contains(
            "APPROVAL"
        ) ->
            LightOrange

        normalized.contains(
            "SOLD"
        ) ->
            LightRed

        normalized == "IN_STOCK" ->
            LightGreen

        else ->
            SoftGrey
    }
}