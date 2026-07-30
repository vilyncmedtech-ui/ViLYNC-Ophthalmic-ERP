package com.vilync.ophthalmicerp.feature.inventory.serialstock

import com.vilync.ophthalmicerp.data.dao.SerialStockRow


data class SerialStockUiState(

    // =========================================================
    // LOADING
    // =========================================================

    val isLoading: Boolean = true,


    // =========================================================
    // SEARCH
    // =========================================================

    val searchQuery: String = "",


    // =========================================================
    // STATUS FILTER
    // =========================================================

    val selectedStatus: String = "ALL",


    // =========================================================
    // SERIAL STOCK DATA
    // =========================================================

    val serialStock: List<SerialStockRow> = emptyList(),


    // =========================================================
    // ERROR
    // =========================================================

    val errorMessage: String? = null

) {


    // =========================================================
    // TOTAL SERIAL UNITS
    // =========================================================

    val totalUnits: Int
        get() =
            serialStock.size


    // =========================================================
    // CURRENTLY IN STOCK
    // =========================================================

    val inStockUnits: Int
        get() =
            serialStock.count { row ->

                row.status.equals(
                    other = "IN_STOCK",
                    ignoreCase = true
                )
            }


    // =========================================================
    // UNITS CURRENTLY OUT OF STOCK / ISSUED
    // =========================================================

    val issuedUnits: Int
        get() =
            serialStock.count { row ->

                !row.status.equals(
                    other = "IN_STOCK",
                    ignoreCase = true
                )
            }
}