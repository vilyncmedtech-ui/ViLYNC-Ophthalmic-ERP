package com.vilync.ophthalmicerp.data.repository

import androidx.room.withTransaction
import com.vilync.ophthalmicerp.core.util.SerialFormatter
import com.vilync.ophthalmicerp.data.dao.InventoryDao
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import kotlinx.coroutines.flow.Flow


class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val database: AppDatabase? = null
) {

    // =========================================================
    // CREATE INVENTORY UNIT
    // =========================================================

    suspend fun insertInventoryUnit(
        unit: InventoryUnitEntity
    ): Long {

        return inventoryDao.insertInventoryUnit(
            unit
        )
    }


    // =========================================================
    // UPDATE INVENTORY UNIT
    // =========================================================

    suspend fun updateInventoryUnit(
        unit: InventoryUnitEntity
    ) {

        inventoryDao.updateInventoryUnit(
            unit
        )
    }


    // =========================================================
    // ALL INVENTORY UNITS
    // =========================================================

    fun getAllInventoryUnits():
            Flow<List<InventoryUnitEntity>> {

        return inventoryDao.getAllInventoryUnits()
    }


    // =========================================================
    // EXACT SERIAL SEARCH
    // =========================================================
    //
    // Existing public method retained so current ERP code
    // depending on this method continues to work.
    // =========================================================

    suspend fun getBySerialNumber(
        serialNumber: String
    ): InventoryUnitEntity? {

        val normalizedSerial =
            serialNumber.trim()

        if (normalizedSerial.isBlank()) {
            return null
        }

        return inventoryDao.getBySerialNumber(
            normalizedSerial
        )
    }


    // =========================================================
    // ERP-WIDE SMART SERIAL SEARCH
    // =========================================================
    //
    // SEARCH PRIORITY:
    //
    // 1. Exact full serial
    // 2. Serial suffix
    //
    // Example:
    //
    // Stored:
    // LMDE232123
    //
    // Search:
    // LMDE232123
    //
    // -> Exact result returned immediately.
    //
    //
    // Search:
    // 232123
    //
    // -> Suffix search.
    //
    //
    // If suffix matches:
    //
    // LMDE232123
    // LMMS232123
    //
    // BOTH are returned.
    //
    // UI must ask the user to select the correct physical unit.
    //
    // ERP must never guess between multiple matches.
    // =========================================================

    suspend fun smartSearchSerial(
        serialQuery: String
    ): List<InventoryUnitEntity> {

        val query =
            serialQuery.trim()

        if (query.isBlank()) {
            return emptyList()
        }


        // -----------------------------------------------------
        // EXACT MATCH HAS HIGHEST PRIORITY
        // -----------------------------------------------------

        val exactMatch =
            inventoryDao.getBySerialNumber(
                query
            )

        if (exactMatch != null) {

            return listOf(
                exactMatch
            )
        }


        // -----------------------------------------------------
        // OTHERWISE SEARCH BY SERIAL SUFFIX
        // -----------------------------------------------------

        return inventoryDao.findBySerialSuffix(
            serialSuffix = query
        )
    }


    // =========================================================
    // SMART SERIAL SEARCH BY INVENTORY STATUS
    // =========================================================
    //
    // This is the preferred method for transactional workflows.
    //
    // Examples:
    //
    // NORMAL SALES
    //
    // smartSearchSerialByStatus(
    //     serialQuery = "232123",
    //     status = "IN_STOCK"
    // )
    //
    //
    // CHALLAN INVENTORY
    //
    // smartSearchSerialByStatus(
    //     serialQuery = "232123",
    //     status = "ON_CHALLAN"
    // )
    //
    //
    // Exact match still receives highest priority.
    // =========================================================

    suspend fun smartSearchSerialByStatus(
        serialQuery: String,
        status: String
    ): List<InventoryUnitEntity> {

        val query =
            serialQuery.trim()

        val normalizedStatus =
            status
                .trim()
                .uppercase()

        if (
            query.isBlank() ||
            normalizedStatus.isBlank()
        ) {
            return emptyList()
        }


        // -----------------------------------------------------
        // EXACT MATCH FIRST
        // -----------------------------------------------------

        val exactMatch =
            inventoryDao.findExactSerialByStatus(
                serialNumber = query,
                status = normalizedStatus
            )

        if (exactMatch != null) {

            return listOf(
                exactMatch
            )
        }


        // -----------------------------------------------------
        // SUFFIX MATCH SECOND
        // -----------------------------------------------------

        return inventoryDao.findBySerialSuffixAndStatus(
            serialSuffix = query,
            status = normalizedStatus
        )
    }


    // =========================================================
    // NORMAL SALES SMART SERIAL SEARCH
    // =========================================================
    //
    // Convenience method.
    //
    // Normal Sale must only search currently available stock.
    // ON_CHALLAN or SOLD units must never appear here.
    // =========================================================

    suspend fun smartSearchInStockSerial(
        serialQuery: String
    ): List<InventoryUnitEntity> {

        return smartSearchSerialByStatus(
            serialQuery = serialQuery,
            status = "IN_STOCK"
        )
    }


    // =========================================================
    // ON-CHALLAN INVENTORY SMART SEARCH
    // =========================================================
    //
    // Generic inventory-level ON_CHALLAN search.
    //
    // IMPORTANT:
    //
    // New Sales Invoice -> Settle Challan must NOT use this
    // method directly because settlement search must additionally
    // be restricted to the selected Bill To customer.
    //
    // Customer-restricted Challan search is handled by
    // ChallanRepository.
    // =========================================================

    suspend fun smartSearchOnChallanSerial(
        serialQuery: String
    ): List<InventoryUnitEntity> {

        return smartSearchSerialByStatus(
            serialQuery = serialQuery,
            status = "ON_CHALLAN"
        )
    }


    // =========================================================
    // INVENTORY UNIT BY ID
    // =========================================================

    suspend fun getById(
        unitId: Long
    ): InventoryUnitEntity? {

        return inventoryDao.getById(
            unitId
        )
    }


    // =========================================================
    // DISTINCT POWERS
    // =========================================================

    suspend fun getDistinctPowers(): List<String> {
        return inventoryDao.getDistinctPowers()
    }

    suspend fun getAvailableUnitsByProductAndPower(
        productId: Long,
        power: String
    ): List<InventoryUnitEntity> {
        return inventoryDao.getAvailableUnitsByProductAndPower(productId, power)
    }

    /**
     * Finds an inventory unit by serial number, attempting to match
     * regardless of whether the "LMDE" prefix is stored in the database.
     */
    suspend fun getUnitBySerialAgnostic(input: String): InventoryUnitEntity? {
        val trimmed = input.trim()
        
        // 1. Try exact match (handles LMDE prefix if stored, or raw if entered raw)
        val exact = inventoryDao.getBySerialNumber(trimmed)
        if (exact != null) return exact
        
        // 2. Try with LMDE prefix if input is numeric
        if (trimmed.all { it.isDigit() }) {
            val prefixed = SerialFormatter.format("LMDE", trimmed)
            val unit = inventoryDao.getBySerialNumber(prefixed)
            if (unit != null) return unit
        }
        
        // 3. Try raw numeric if input has LMDE prefix
        if (trimmed.startsWith("LMDE", ignoreCase = true)) {
            val raw = trimmed.substring(4)
            val unit = inventoryDao.getBySerialNumber(raw)
            if (unit != null) return unit
        }
        
        return null
    }


    suspend fun <R> withTransaction(block: suspend () -> R): R {
        return if (database != null) {
            database.withTransaction(block)
        } else {
            block()
        }
    }


    // =========================================================
    // INVENTORY UNITS BY PRODUCT
    // =========================================================

    fun getUnitsByProduct(
        productId: Long
    ): Flow<List<InventoryUnitEntity>> {

        return inventoryDao.getUnitsByProduct(
            productId
        )
    }


    // =========================================================
    // CURRENT AVAILABLE STOCK
    // =========================================================

    fun getInStockUnits():
            Flow<List<InventoryUnitEntity>> {

        return inventoryDao.getInStockUnits()
    }


    // =========================================================
    // INVENTORY BY STATUS
    // =========================================================

    fun getUnitsByStatus(
        status: String
    ): Flow<List<InventoryUnitEntity>> {

        return inventoryDao.getUnitsByStatus(
            status = status
                .trim()
                .uppercase()
        )
    }


    // =========================================================
    // UPDATE INVENTORY STATUS
    // =========================================================

    suspend fun updateStatus(
        unitId: Long,
        newStatus: String
    ) {

        inventoryDao.updateStatus(
            unitId = unitId,

            newStatus =
                newStatus
                    .trim()
                    .uppercase()
        )
    }


    // =========================================================
    // SERIAL NUMBER EXISTS
    // =========================================================

    suspend fun serialNumberExists(
        serialNumber: String
    ): Boolean {

        val normalizedSerial =
            serialNumber.trim()

        if (normalizedSerial.isBlank()) {
            return false
        }

        return inventoryDao.serialNumberExists(
            normalizedSerial
        )
    }
}