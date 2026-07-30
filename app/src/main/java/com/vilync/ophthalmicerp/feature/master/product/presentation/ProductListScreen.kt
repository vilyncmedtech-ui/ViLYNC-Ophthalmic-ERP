package com.vilync.ophthalmicerp.feature.master.product.presentation

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.data.entity.ProductEntity


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onAddProductClick: () -> Unit,
    onEditProductClick: (Long) -> Unit
) {

    val products by
    viewModel.products.collectAsState()

    val searchQuery by
    viewModel.searchQuery.collectAsState()


    Scaffold(

        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "Product Master",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${products.size} Products",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
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
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
        ) {


            // =================================================
            // SEARCH
            // =================================================

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {
                    viewModel.updateSearchQuery(it)
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Search Product")
                },

                placeholder = {
                    Text(
                        "Name / Brand / Model / Category"
                    )
                },

                singleLine = true,

                trailingIcon = {

                    if (
                        searchQuery.isNotBlank()
                    ) {

                        TextButton(
                            onClick = {
                                viewModel.clearSearch()
                            }
                        ) {

                            Text("Clear")
                        }
                    }
                }
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // =================================================
            // ADD PRODUCT
            // =================================================

            Button(
                onClick = onAddProductClick,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("+ Add New Product")
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // COLUMN HEADINGS
            // =================================================

            ProductTableHeader()


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            // =================================================
            // EMPTY STATE
            // =================================================

            if (
                products.isEmpty()
            ) {

                Text(
                    text =
                        if (
                            searchQuery.isBlank()
                        ) {
                            "No products available"
                        } else {
                            "No matching products found"
                        },

                    modifier =
                        Modifier.padding(
                            top = 24.dp
                        ),

                    style =
                        MaterialTheme.typography.bodyLarge
                )

            } else {


                // =============================================
                // COMPACT PRODUCT LIST
                // =============================================

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {


                    items(
                        items = products,

                        key = {
                                product ->

                            product.id
                        }

                    ) {
                            product ->


                        CompactProductRow(

                            product =
                                product,

                            onClick = {

                                onEditProductClick(
                                    product.id
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
// TABLE HEADER
// =============================================================

@Composable
private fun ProductTableHeader() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {


            Text(
                text = "PRODUCT",

                modifier =
                    Modifier.weight(2.2f),

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography.labelMedium
            )


            Text(
                text = "CATEGORY",

                modifier =
                    Modifier.weight(1.1f),

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography.labelMedium
            )


            Text(
                text = "GST",

                modifier =
                    Modifier.weight(0.7f),

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography.labelMedium
            )


            Text(
                text = "MRP",

                modifier =
                    Modifier.weight(1f),

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography.labelMedium
            )
        }
    }
}


// =============================================================
// COMPACT PRODUCT ROW
// =============================================================

@Composable
private fun CompactProductRow(
    product: ProductEntity,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {


        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {


            // =================================================
            // MAIN ROW
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {


                // -------------------------------------------------
                // PRODUCT / BRAND
                // -------------------------------------------------

                Column(
                    modifier =
                        Modifier.weight(2.2f)
                ) {

                    Text(
                        text =
                            product.productName,

                        fontWeight =
                            FontWeight.SemiBold,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis,

                        style =
                            MaterialTheme.typography.bodyMedium
                    )


                    if (
                        product.brandName.isNotBlank()
                    ) {

                        Text(
                            text =
                                product.brandName,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis,

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }


                    if (
                        product.model.isNotBlank() &&
                        product.model != product.productName
                    ) {

                        Text(
                            text =
                                product.model,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis,

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }


                // -------------------------------------------------
                // CATEGORY
                // -------------------------------------------------

                Text(
                    text =
                        product.category,

                    modifier =
                        Modifier.weight(1.1f),

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis,

                    style =
                        MaterialTheme.typography.bodySmall
                )


                // -------------------------------------------------
                // GST
                // -------------------------------------------------

                Text(
                    text =
                        formatPercent(
                            product.gstPercent
                        ),

                    modifier =
                        Modifier.weight(0.7f),

                    maxLines = 1,

                    style =
                        MaterialTheme.typography.bodySmall
                )


                // -------------------------------------------------
                // MRP
                // -------------------------------------------------

                Text(
                    text =
                        formatMoney(
                            product.mrp
                        ),

                    modifier =
                        Modifier.weight(1f),

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis,

                    fontWeight =
                        FontWeight.Medium,

                    style =
                        MaterialTheme.typography.bodySmall
                )
            }


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            // =================================================
            // SECONDARY INFORMATION
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {


                if (
                    product.hsnCode.isNotBlank()
                ) {

                    Text(
                        text =
                            "HSN: ${product.hsnCode}",

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }


                Text(
                    text =
                        "Unit: ${product.unit}",

                    style =
                        MaterialTheme.typography.bodySmall
                )


                Text(
                    text =
                        "Purchase: ${
                            formatMoney(
                                product.purchasePrice
                            )
                        }",

                    style =
                        MaterialTheme.typography.bodySmall
                )


                Text(
                    text =
                        if (
                            product.isActive
                        ) {
                            "ACTIVE"
                        } else {
                            "INACTIVE"
                        },

                    fontWeight =
                        FontWeight.SemiBold,

                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


// =============================================================
// MONEY FORMAT
// =============================================================

private fun formatMoney(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {

        "₹${value.toLong()}"

    } else {

        "₹%.2f".format(
            value
        )
    }
}


// =============================================================
// PERCENT FORMAT
// =============================================================

private fun formatPercent(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {

        "${value.toInt()}%"

    } else {

        "$value%"
    }
}