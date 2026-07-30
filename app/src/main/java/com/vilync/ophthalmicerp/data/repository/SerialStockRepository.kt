package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.SerialStockDao
import com.vilync.ophthalmicerp.data.dao.SerialStockRow
import kotlinx.coroutines.flow.Flow


class SerialStockRepository(

    private val serialStockDao: SerialStockDao

) {


    // =========================================================
    // COMPLETE SERIAL STOCK REGISTER
    // =========================================================

    fun getSerialStockRegister():
            Flow<List<SerialStockRow>> {

        return serialStockDao
            .getSerialStockRegister()
    }


    // =========================================================
    // SEARCH SERIAL STOCK
    // =========================================================

    /**
     * Search supported by DAO:
     *
     * Serial Number
     * Product Name
     * Brand
     * Model
     * Power
     * Batch
     * Supplier
     * Purchase Invoice
     */
    fun searchSerialStock(
        query: String
    ): Flow<List<SerialStockRow>> {

        val normalizedQuery =
            query.trim()

        if (normalizedQuery.isBlank()) {

            return serialStockDao
                .getSerialStockRegister()
        }

        return serialStockDao
            .searchSerialStock(
                query = normalizedQuery
            )
    }


    // =========================================================
    // FILTER BY CURRENT STATUS
    // =========================================================

    /**
     * Examples:
     *
     * IN_STOCK
     * SAMPLE
     * DEMO
     * APPROVAL
     * SOLD
     * RETURNED
     */
    fun getSerialStockByStatus(
        status: String
    ): Flow<List<SerialStockRow>> {

        return serialStockDao
            .getSerialStockByStatus(
                status = status.trim()
            )
    }


    // =========================================================
    // GET ONE SERIAL STOCK UNIT
    // =========================================================

    suspend fun getSerialStockById(
        inventoryUnitId: Long
    ): SerialStockRow? {

        require(
            inventoryUnitId > 0L
        ) {
            "Valid Inventory Unit ID is required."
        }

        return serialStockDao
            .getSerialStockById(
                inventoryUnitId =
                    inventoryUnitId
            )
    }
}