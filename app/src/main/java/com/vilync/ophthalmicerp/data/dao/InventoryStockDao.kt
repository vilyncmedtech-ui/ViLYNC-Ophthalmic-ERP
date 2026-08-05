package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.vilync.ophthalmicerp.feature.inventory.logic.ProductPowerStock
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryStockDao {

    // =========================================================
    // STOCK REGISTER
    // =========================================================

    /**
     * Transaction-derived current stock.
     *
     * CURRENT STOCK SOURCES:
     *
     * PURCHASE
     *     increases stock
     *
     * ACTIVE PURCHASE RETURN
     *     decreases stock
     *
     * CANCELLED PURCHASE RETURN
     *     does NOT decrease stock
     *
     * Stock is grouped by:
     *
     * Product
     * Model
     * Category
     * Power
     *
     * IMPORTANT:
     *
     * Current stock is calculated from transactions.
     * It is not manually stored as a separate duplicate
     * source of truth.
     *
     * Future stock-affecting transactions such as:
     *
     * - Opening Stock
     * - Stock Adjustment
     * - Sales
     * - Sales Return
     *
     * can later be incorporated into this inventory
     * reporting layer.
     */
    @Query(
        """
        SELECT
            pi.productId AS productId,

            COALESCE(
                p.productName,
                ''
            ) AS productName,

            COALESCE(
                p.model,
                ''
            ) AS model,

            COALESCE(
                p.category,
                ''
            ) AS category,

            COALESCE(
                pi.power,
                ''
            ) AS power,

            CAST(
                COALESCE(
                    SUM(pi.quantity),
                    0
                )
                AS INTEGER
            ) AS purchasedQuantity,

            CAST(
                COALESCE(
                    SUM(
                        (
                            SELECT
                                COALESCE(
                                    SUM(pri.quantity),
                                    0
                                )

                            FROM purchase_return_items pri

                            INNER JOIN purchase_returns pr
                                ON pr.id = pri.purchaseReturnId

                            WHERE
                                pri.originalPurchaseItemId = pi.id

                                AND pr.status != 'CANCELLED'
                        )
                    ),
                    0
                )
                AS INTEGER
            ) AS purchaseReturnQuantity,

            CAST(
                COALESCE(
                    SUM(pi.quantity),
                    0
                )
                -
                COALESCE(
                    SUM(
                        (
                            SELECT
                                COALESCE(
                                    SUM(pri.quantity),
                                    0
                                )

                            FROM purchase_return_items pri

                            INNER JOIN purchase_returns pr
                                ON pr.id = pri.purchaseReturnId

                            WHERE
                                pri.originalPurchaseItemId = pi.id

                                AND pr.status != 'CANCELLED'
                        )
                    ),
                    0
                )
                AS INTEGER
            ) AS availableQuantity

        FROM purchase_items pi

        INNER JOIN products p
            ON p.id = pi.productId

        GROUP BY
            pi.productId,
            p.productName,
            p.model,
            p.category,
            pi.power

        ORDER BY
            p.productName COLLATE NOCASE ASC,
            p.model COLLATE NOCASE ASC,
            pi.power COLLATE NOCASE ASC
        """
    )
    fun getStockRegister():
            Flow<List<InventoryStockRow>>

    // =========================================================
    // MODULAR STOCK AGGREGATION (PHASE 1)
    // =========================================================

    /**
     * Physical count of units currently in warehouse (Serial Tracked).
     */
    @Query(
        """
        SELECT productId, power, COUNT(*) as quantity
        FROM inventory_units
        WHERE status = 'IN_STOCK'
        GROUP BY productId, power
        """
    )
    suspend fun getInStockSerialCounts(): List<ProductPowerStock>

    /**
     * Total quantity purchased (Non-Serial items).
     */
    @Query(
        """
        SELECT pi.productId, pi.power, SUM(pi.quantity) as quantity
        FROM purchase_items pi
        INNER JOIN products prod ON prod.id = pi.productId
        WHERE prod.trackingType != 'SERIAL'
        GROUP BY pi.productId, pi.power
        """
    )
    suspend fun getPurchaseQuantityStock(): List<ProductPowerStock>

    /**
     * Total quantity returned to supplier (Non-Serial items).
     */
    @Query(
        """
        SELECT pri.productId, pi.power, SUM(pri.quantity) as quantity
        FROM purchase_return_items pri
        INNER JOIN purchase_returns pr ON pr.id = pri.purchaseReturnId
        INNER JOIN purchase_items pi ON pi.id = pri.originalPurchaseItemId
        INNER JOIN products prod ON prod.id = pri.productId
        WHERE pr.status = 'POSTED' AND prod.trackingType != 'SERIAL'
        GROUP BY pri.productId, pi.power
        """
    )
    suspend fun getPurchaseReturnQuantityStock(): List<ProductPowerStock>

    /**
     * Total quantity sold (Non-Serial items).
     */
    @Query(
        """
        SELECT si.productId, si.power, SUM(si.quantity) as quantity
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.saleId
        INNER JOIN products prod ON prod.id = si.productId
        WHERE s.status = 'POSTED' AND prod.trackingType != 'SERIAL'
        GROUP BY si.productId, si.power
        """
    )
    suspend fun getSaleQuantityStock(): List<ProductPowerStock>

    /**
     * Total quantity returned from customer (Non-Serial items).
     */
    @Query(
        """
        SELECT scni.productId, scni.power, SUM(scni.quantity) as quantity
        FROM sales_credit_note_items scni
        INNER JOIN sales_credit_notes scn ON scn.id = scni.creditNoteId
        INNER JOIN products prod ON prod.id = scni.productId
        WHERE scn.status = 'POSTED' AND prod.trackingType != 'SERIAL'
        GROUP BY scni.productId, scni.power
        """
    )
    suspend fun getSalesReturnQuantityStock(): List<ProductPowerStock>

    /**
     * Total quantity entered as Opening Stock (Non-Serial items).
     */
    @Query(
        """
        SELECT osi.productId, osi.power, SUM(osi.quantity) as quantity
        FROM opening_stock_items osi
        INNER JOIN opening_stocks os ON os.id = osi.openingStockId
        INNER JOIN products prod ON prod.id = osi.productId
        WHERE os.status = 'POSTED' AND prod.trackingType != 'SERIAL'
        GROUP BY osi.productId, osi.power
        """
    )
    suspend fun getOpeningStockQuantityStock(): List<ProductPowerStock>
}
