package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.vilync.ophthalmicerp.feature.inventory.logic.ProductPowerStock
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryStockDao {

    // =========================================================
    // RECONCILED STOCK REGISTER (PHASE 2)
    // =========================================================

    /**
     * Transaction-reconciled current stock.
     *
     * USES AGGREGATED SUBQUERIES TO PREVENT DOUBLE-COUNTING.
     *
     * AVAILABLE (Serial): Physical count of IN_STOCK units.
     * AVAILABLE (Qty): OS + Purchased - PurcReturn - NetSold - OtherOut.
     */
    @Query(
        """
        WITH power_map AS (
            SELECT productId, power FROM purchase_items
            UNION SELECT productId, power FROM opening_stock_items
            UNION SELECT productId, power FROM sale_items
            UNION SELECT productId, power FROM sales_credit_note_items
            UNION SELECT productId, power FROM challan_items
            UNION SELECT productId, power FROM inventory_units
        ),
        agg_purchase AS (
            SELECT pi.productId, pi.power, SUM(pi.quantity) as qty 
            FROM purchase_items pi
            INNER JOIN purchases p ON p.id = pi.purchaseId
            WHERE p.status = 'POSTED'
            GROUP BY pi.productId, pi.power
        ),
        agg_opening AS (
            SELECT osi.productId, osi.power, SUM(osi.quantity) as qty 
            FROM opening_stock_items osi
            INNER JOIN opening_stocks os ON os.id = osi.openingStockId
            WHERE os.status = 'POSTED'
            GROUP BY osi.productId, osi.power
        ),
        agg_purc_return AS (
            SELECT pi.productId, pi.power, SUM(pri.quantity) as qty
            FROM purchase_return_items pri
            INNER JOIN purchase_items pi ON pi.id = pri.originalPurchaseItemId
            INNER JOIN purchase_returns pr ON pr.id = pri.purchaseReturnId
            WHERE pr.status = 'POSTED'
            GROUP BY pi.productId, pi.power
        ),
        agg_sales AS (
            SELECT si.productId, si.power, SUM(si.quantity) as qty
            FROM sale_items si
            INNER JOIN sales s ON s.id = si.saleId
            WHERE s.status = 'POSTED'
            GROUP BY si.productId, si.power
        ),
        agg_sales_return AS (
            SELECT scni.productId, scni.power, SUM(scni.quantity) as qty
            FROM sales_credit_note_items scni
            INNER JOIN sales_credit_notes scn ON scn.id = scni.creditNoteId
            WHERE scn.status = 'POSTED'
            GROUP BY scni.productId, scni.power
        ),
        agg_other_out AS (
            -- ON_CHALLAN and SAMPLE are considered 'Out' from warehouse but not Sold.
            SELECT productId, power, COUNT(*) as qty
            FROM inventory_units
            WHERE status IN ('ON_CHALLAN', 'SAMPLE')
            GROUP BY productId, power
        ),
        agg_phys_avail AS (
            -- The ultimate proof for Serial items.
            SELECT productId, power, COUNT(*) as qty
            FROM inventory_units
            WHERE status = 'IN_STOCK'
            GROUP BY productId, power
        )
        SELECT
            p.id AS productId,
            p.productName,
            p.model,
            p.category,
            pm.power,
            
            CAST(COALESCE(ap.qty, 0) + COALESCE(ao.qty, 0) AS INTEGER) AS purchasedQuantity,
            CAST(COALESCE(apr.qty, 0) AS INTEGER) AS purchaseReturnQuantity,
            CAST(COALESCE(asl.qty, 0) - COALESCE(asr.qty, 0) AS INTEGER) AS soldQuantity,
            CAST(COALESCE(aot.qty, 0) AS INTEGER) AS otherOutQuantity,
            
            CAST(
                CASE 
                    WHEN p.trackingType = 'SERIAL' THEN COALESCE(apa.qty, 0)
                    ELSE COALESCE(ao.qty, 0) + COALESCE(ap.qty, 0) - COALESCE(apr.qty, 0) - (COALESCE(asl.qty, 0) - COALESCE(asr.qty, 0)) - COALESCE(aot.qty, 0)
                END AS INTEGER
            ) AS availableQuantity

        FROM products p
        INNER JOIN power_map pm ON p.id = pm.productId
        LEFT JOIN agg_purchase ap ON p.id = ap.productId AND pm.power = ap.power
        LEFT JOIN agg_opening ao ON p.id = ao.productId AND pm.power = ao.power
        LEFT JOIN agg_purc_return apr ON p.id = apr.productId AND pm.power = apr.power
        LEFT JOIN agg_sales asl ON p.id = asl.productId AND pm.power = asl.power
        LEFT JOIN agg_sales_return asr ON p.id = asr.productId AND pm.power = asr.power
        LEFT JOIN agg_other_out aot ON p.id = aot.productId AND pm.power = aot.power
        LEFT JOIN agg_phys_avail apa ON p.id = apa.productId AND pm.power = apa.power

        ORDER BY
            p.productName COLLATE NOCASE ASC,
            p.model COLLATE NOCASE ASC,
            pm.power COLLATE NOCASE ASC
        """
    )
    fun getStockRegister(): Flow<List<InventoryStockRow>>

    @Query(
        """
        WITH power_map AS (
            SELECT productId, power FROM purchase_items
            UNION SELECT productId, power FROM opening_stock_items
            UNION SELECT productId, power FROM sale_items
            UNION SELECT productId, power FROM sales_credit_note_items
            UNION SELECT productId, power FROM challan_items
            UNION SELECT productId, power FROM inventory_units
        ),
        agg_purchase AS (
            SELECT pi.productId, pi.power, SUM(pi.quantity) as qty 
            FROM purchase_items pi
            INNER JOIN purchases p ON p.id = pi.purchaseId
            WHERE p.status = 'POSTED'
            GROUP BY pi.productId, pi.power
        ),
        agg_opening AS (
            SELECT osi.productId, osi.power, SUM(osi.quantity) as qty 
            FROM opening_stock_items osi
            INNER JOIN opening_stocks os ON os.id = osi.openingStockId
            WHERE os.status = 'POSTED'
            GROUP BY osi.productId, osi.power
        ),
        agg_purc_return AS (
            SELECT pi.productId, pi.power, SUM(pri.quantity) as qty
            FROM purchase_return_items pri
            INNER JOIN purchase_items pi ON pi.id = pri.originalPurchaseItemId
            INNER JOIN purchase_returns pr ON pr.id = pri.purchaseReturnId
            WHERE pr.status = 'POSTED'
            GROUP BY pi.productId, pi.power
        ),
        agg_sales AS (
            SELECT si.productId, si.power, SUM(si.quantity) as qty
            FROM sale_items si
            INNER JOIN sales s ON s.id = si.saleId
            WHERE s.status = 'POSTED'
            GROUP BY si.productId, si.power
        ),
        agg_sales_return AS (
            SELECT scni.productId, scni.power, SUM(scni.quantity) as qty
            FROM sales_credit_note_items scni
            INNER JOIN sales_credit_notes scn ON scn.id = scni.creditNoteId
            WHERE scn.status = 'POSTED'
            GROUP BY scni.productId, scni.power
        ),
        agg_other_out AS (
            -- ON_CHALLAN and SAMPLE are considered 'Out' from warehouse but not Sold.
            SELECT productId, power, COUNT(*) as qty
            FROM inventory_units
            WHERE status IN ('ON_CHALLAN', 'SAMPLE')
            GROUP BY productId, power
        ),
        agg_phys_avail AS (
            -- The ultimate proof for Serial items.
            SELECT productId, power, COUNT(*) as qty
            FROM inventory_units
            WHERE status = 'IN_STOCK'
            GROUP BY productId, power
        )
        SELECT SUM(
            CAST(
                CASE 
                    WHEN p.trackingType = 'SERIAL' THEN COALESCE(apa.qty, 0)
                    ELSE COALESCE(ao.qty, 0) + COALESCE(ap.qty, 0) - COALESCE(apr.qty, 0) - (COALESCE(asl.qty, 0) - COALESCE(asr.qty, 0)) - COALESCE(aot.qty, 0)
                END AS INTEGER
            ) * p.purchasePrice
        )
        FROM products p
        INNER JOIN power_map pm ON p.id = pm.productId
        LEFT JOIN agg_purchase ap ON p.id = ap.productId AND pm.power = ap.power
        LEFT JOIN agg_opening ao ON p.id = ao.productId AND pm.power = ao.power
        LEFT JOIN agg_purc_return apr ON p.id = apr.productId AND pm.power = apr.power
        LEFT JOIN agg_sales asl ON p.id = asl.productId AND pm.power = asl.power
        LEFT JOIN agg_sales_return asr ON p.id = asr.productId AND pm.power = asr.power
        LEFT JOIN agg_other_out aot ON p.id = aot.productId AND pm.power = aot.power
        LEFT JOIN agg_phys_avail apa ON p.id = apa.productId AND pm.power = apa.power
        """
    )
    suspend fun calculateClosingStockValueTotal(): Double?

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
        SELECT pi.productId, pi.power, SUM(pri.quantity) as quantity
        FROM purchase_return_items pri
        INNER JOIN purchase_returns pr ON pr.id = pri.purchaseReturnId
        INNER JOIN purchase_items pi ON pi.id = pri.originalPurchaseItemId
        INNER JOIN products prod ON prod.id = pi.productId
        WHERE pr.status = 'POSTED' AND prod.trackingType != 'SERIAL'
        GROUP BY pi.productId, pi.power
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
