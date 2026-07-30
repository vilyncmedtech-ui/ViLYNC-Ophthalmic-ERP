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

private val GoldSoft =
    Color(0xFFFFFBF0)

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
                22.dp
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
                    .padding(18.dp)
        ) {

            // =================================================
            // PRODUCT + STATUS
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            uiState.productName
                                .ifBlank {
                                    "Serial Stock"
                                },

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            NavyDark,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    val secondary =
                        listOf(
                            uiState.brandName,
                            uiState.model,
                            uiState.category
                        )
                            .filter {
                                it.isNotBlank()
                            }
                            .joinToString(
                                separator =
                                    "  •  "
                            )


                    if (
                        secondary.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )


                        Text(
                            text =
                                secondary,

                            fontSize =
                                12.sp,

                            color =
                                TextSecondary,

                            maxLines =
                                1,

                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                HistoryStatusBadge(
                    status =
                        uiState.currentStatus
                )
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // =================================================
            // SERIAL + POWER
            // =================================================

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color =
                                GoldSoft,

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                )
                        )
                        .border(
                            width =
                                1.dp,

                            color =
                                Color(
                                    0xFFE4C76B
                                ),

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                )
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "SERIAL NUMBER",

                            fontSize =
                                9.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            color =
                                TextSecondary
                        )


                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )


                        Text(
                            text =
                                uiState.serialNumber
                                    .ifBlank {
                                        "—"
                                    },

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                NavyDark
                        )
                    }


                    if (
                        uiState.power.isNotBlank()
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {

                            Text(
                                text =
                                    "POWER",

                                fontSize =
                                    9.sp,

                                fontWeight =
                                    FontWeight.SemiBold,

                                color =
                                    TextSecondary
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )


                            Text(
                                text =
                                    uiState.power,

                                fontSize =
                                    18.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Blue
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // =================================================
            // BATCH / EXPIRY / RECEIVED
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                HistoryInfoBox(
                    modifier =
                        Modifier.weight(1f),

                    label =
                        "Batch",

                    value =
                        uiState.batchNumber
                            .ifBlank {
                                "—"
                            }
                )


                HistoryInfoBox(
                    modifier =
                        Modifier.weight(1f),

                    label =
                        "Expiry",

                    value =
                        formatExpiryForDisplay(
                            uiState.expiryDate
                        )
                )


                HistoryInfoBox(
                    modifier =
                        Modifier.weight(1f),

                    label =
                        "Received",

                    value =
                        uiState.receivedDate
                            .ifBlank {
                                "—"
                            }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            HorizontalDivider(
                color =
                    Border
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            HistoryDetailLine(
                label =
                    "Supplier",

                value =
                    uiState.supplierName
                        .ifBlank {
                            "—"
                        }
            )


            Spacer(
                modifier =
                    Modifier.height(9.dp)
            )


            HistoryDetailLine(
                label =
                    "Purchase Invoice",

                value =
                    uiState.purchaseInvoiceNumber
                        .ifBlank {
                            "—"
                        }
            )
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
                18.dp
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
                    .padding(16.dp),

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
                            .size(16.dp)
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
                                    3.dp,

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
                                top = 5.dp
                            )
                            .width(2.dp)
                            .height(74.dp)
                            .background(
                                Border
                            )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )


            // =================================================
            // MOVEMENT INFORMATION
            // =================================================

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(
                        text =
                            movementTitle,

                        modifier =
                            Modifier.weight(1f),

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            movementColour(
                                movement.movementType
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Text(
                        text =
                            movement.movementDate
                                .ifBlank {
                                    "—"
                                },

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            TextSecondary
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // =============================================
                // STATUS TRANSITION
                // =============================================

                if (
                    movement.fromStatus.isNotBlank() ||
                    movement.toStatus.isNotBlank()
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

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
                                11.sp,

                            color =
                                TextSecondary
                        )


                        Text(
                            text =
                                "  →  ",

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Gold
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
                                11.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                TextPrimary
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }


                if (
                    movement.partyName.isNotBlank()
                ) {

                    MovementDetail(
                        label =
                            "Party",

                        value =
                            movement.partyName
                    )
                }


                if (
                    movement.referenceNumber.isNotBlank()
                ) {

                    MovementDetail(
                        label =
                            "Reference",

                        value =
                            movement.referenceNumber
                    )
                }


                if (
                    movement.remarks.isNotBlank()
                ) {

                    MovementDetail(
                        label =
                            "Remarks",

                        value =
                            movement.remarks
                    )
                }
            }
        }
    }
}


// =============================================================
// MOVEMENT DETAIL
// =============================================================

@Composable
private fun MovementDetail(

    label: String,

    value: String

) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 3.dp
                )
    ) {

        Text(
            text =
                "$label:",

            modifier =
                Modifier.width(78.dp),

            fontSize =
                11.sp,

            color =
                TextSecondary
        )


        Text(
            text =
                value,

            modifier =
                Modifier.weight(1f),

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Medium,

            color =
                TextPrimary
        )
    }
}


// =============================================================
// INFO BOX
// =============================================================

@Composable
private fun HistoryInfoBox(

    modifier: Modifier,

    label: String,

    value: String

) {

    Box(
        modifier =
            modifier
                .background(
                    color =
                        SoftGrey,

                    shape =
                        RoundedCornerShape(
                            13.dp
                        )
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                )
    ) {

        Column {

            Text(
                text =
                    label,

                fontSize =
                    9.sp,

                color =
                    TextSecondary
            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            Text(
                text =
                    value,

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    TextPrimary,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// DETAIL LINE
// =============================================================

@Composable
private fun HistoryDetailLine(

    label: String,

    value: String

) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                "$label:",

            modifier =
                Modifier.width(
                    125.dp
                ),

            fontSize =
                11.sp,

            color =
                TextSecondary
        )


        Text(
            text =
                value,

            modifier =
                Modifier.weight(1f),

            fontSize =
                12.sp,

            fontWeight =
                FontWeight.SemiBold,

            color =
                TextPrimary,

            maxLines =
                1,

            overflow =
                TextOverflow.Ellipsis
        )
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