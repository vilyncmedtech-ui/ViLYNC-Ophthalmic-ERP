package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface SerialStockDao {


    // =========================================================
    // COMPLETE SERIAL STOCK REGISTER
    // =========================================================

    /**
     * Returns every physical serial-tracked inventory unit.
     *
     * inventory_units is the source of the current physical
     * serial inventory state.
     *
     * Product Master is joined only for product display
     * information.
     */
    @Query(
        """
        SELECT

            iu.id AS inventoryUnitId,

            iu.productId AS productId,

            COALESCE(
                p.productName,
                ''
            ) AS productName,

            COALESCE(
                p.brandName,
                ''
            ) AS brandName,

            COALESCE(
                p.model,
                ''
            ) AS model,

            COALESCE(
                p.category,
                ''
            ) AS category,

            COALESCE(
                p.serialPrefix,
                ''
            ) AS serialPrefix,

            COALESCE(
                iu.power,
                ''
            ) AS power,

            COALESCE(
                iu.serialNumber,
                ''
            ) AS serialNumber,

            COALESCE(
                iu.batchNumber,
                ''
            ) AS batchNumber,

            COALESCE(
                iu.expiryDate,
                ''
            ) AS expiryDate,

            COALESCE(
                iu.receivedDate,
                ''
            ) AS receivedDate,

            COALESCE(
                iu.supplierName,
                ''
            ) AS supplierName,

            COALESCE(
                iu.purchaseInvoiceNumber,
                ''
            ) AS purchaseInvoiceNumber,

            COALESCE(
                iu.status,
                ''
            ) AS status

        FROM inventory_units iu

        INNER JOIN products p
            ON p.id = iu.productId

        ORDER BY
            p.productName COLLATE NOCASE ASC,
            p.model COLLATE NOCASE ASC,
            iu.power COLLATE NOCASE ASC,
            iu.serialNumber COLLATE NOCASE ASC
        """
    )
    fun getSerialStockRegister():
            Flow<List<SerialStockRow>>


    // =========================================================
    // SERIAL SEARCH
    // =========================================================

    /**
     * Search Serial Stock Register using:
     *
     * Serial Number
     * Product Name
     * Brand
     * Model
     * Power
     * Batch Number
     * Supplier
     * Purchase Invoice
     */
    @Query(
        """
        SELECT

            iu.id AS inventoryUnitId,

            iu.productId AS productId,

            COALESCE(
                p.productName,
                ''
            ) AS productName,

            COALESCE(
                p.brandName,
                ''
            ) AS brandName,

            COALESCE(
                p.model,
                ''
            ) AS model,

            COALESCE(
                p.category,
                ''
            ) AS category,

            COALESCE(
                p.serialPrefix,
                ''
            ) AS serialPrefix,

            COALESCE(
                iu.power,
                ''
            ) AS power,

            COALESCE(
                iu.serialNumber,
                ''
            ) AS serialNumber,

            COALESCE(
                iu.batchNumber,
                ''
            ) AS batchNumber,

            COALESCE(
                iu.expiryDate,
                ''
            ) AS expiryDate,

            COALESCE(
                iu.receivedDate,
                ''
            ) AS receivedDate,

            COALESCE(
                iu.supplierName,
                ''
            ) AS supplierName,

            COALESCE(
                iu.purchaseInvoiceNumber,
                ''
            ) AS purchaseInvoiceNumber,

            COALESCE(
                iu.status,
                ''
            ) AS status

        FROM inventory_units iu

        INNER JOIN products p
            ON p.id = iu.productId

        WHERE

            iu.serialNumber LIKE '%' || :query || '%'

            OR p.productName LIKE '%' || :query || '%'

            OR p.brandName LIKE '%' || :query || '%'

            OR p.model LIKE '%' || :query || '%'

            OR iu.power LIKE '%' || :query || '%'

            OR iu.batchNumber LIKE '%' || :query || '%'

            OR iu.supplierName LIKE '%' || :query || '%'

            OR iu.purchaseInvoiceNumber LIKE '%' || :query || '%'

        ORDER BY
            p.productName COLLATE NOCASE ASC,
            p.model COLLATE NOCASE ASC,
            iu.power COLLATE NOCASE ASC,
            iu.serialNumber COLLATE NOCASE ASC
        """
    )
    fun searchSerialStock(
        query: String
    ): Flow<List<SerialStockRow>>


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
    @Query(
        """
        SELECT

            iu.id AS inventoryUnitId,

            iu.productId AS productId,

            COALESCE(
                p.productName,
                ''
            ) AS productName,

            COALESCE(
                p.brandName,
                ''
            ) AS brandName,

            COALESCE(
                p.model,
                ''
            ) AS model,

            COALESCE(
                p.category,
                ''
            ) AS category,

            COALESCE(
                p.serialPrefix,
                ''
            ) AS serialPrefix,

            COALESCE(
                iu.power,
                ''
            ) AS power,

            COALESCE(
                iu.serialNumber,
                ''
            ) AS serialNumber,

            COALESCE(
                iu.batchNumber,
                ''
            ) AS batchNumber,

            COALESCE(
                iu.expiryDate,
                ''
            ) AS expiryDate,

            COALESCE(
                iu.receivedDate,
                ''
            ) AS receivedDate,

            COALESCE(
                iu.supplierName,
                ''
            ) AS supplierName,

            COALESCE(
                iu.purchaseInvoiceNumber,
                ''
            ) AS purchaseInvoiceNumber,

            COALESCE(
                iu.status,
                ''
            ) AS status

        FROM inventory_units iu

        INNER JOIN products p
            ON p.id = iu.productId

        WHERE
            iu.status = :status

        ORDER BY
            p.productName COLLATE NOCASE ASC,
            p.model COLLATE NOCASE ASC,
            iu.power COLLATE NOCASE ASC,
            iu.serialNumber COLLATE NOCASE ASC
        """
    )
    fun getSerialStockByStatus(
        status: String
    ): Flow<List<SerialStockRow>>


    // =========================================================
    // GET ONE SERIAL
    // =========================================================

    /**
     * Used later for Serial Detail / Movement History screen.
     */
    @Query(
        """
        SELECT

            iu.id AS inventoryUnitId,

            iu.productId AS productId,

            COALESCE(
                p.productName,
                ''
            ) AS productName,

            COALESCE(
                p.brandName,
                ''
            ) AS brandName,

            COALESCE(
                p.model,
                ''
            ) AS model,

            COALESCE(
                p.category,
                ''
            ) AS category,

            COALESCE(
                p.serialPrefix,
                ''
            ) AS serialPrefix,

            COALESCE(
                iu.power,
                ''
            ) AS power,

            COALESCE(
                iu.serialNumber,
                ''
            ) AS serialNumber,

            COALESCE(
                iu.batchNumber,
                ''
            ) AS batchNumber,

            COALESCE(
                iu.expiryDate,
                ''
            ) AS expiryDate,

            COALESCE(
                iu.receivedDate,
                ''
            ) AS receivedDate,

            COALESCE(
                iu.supplierName,
                ''
            ) AS supplierName,

            COALESCE(
                iu.purchaseInvoiceNumber,
                ''
            ) AS purchaseInvoiceNumber,

            COALESCE(
                iu.status,
                ''
            ) AS status

        FROM inventory_units iu

        INNER JOIN products p
            ON p.id = iu.productId

        WHERE
            iu.id = :inventoryUnitId

        LIMIT 1
        """
    )
    suspend fun getSerialStockById(
        inventoryUnitId: Long
    ): SerialStockRow?
}