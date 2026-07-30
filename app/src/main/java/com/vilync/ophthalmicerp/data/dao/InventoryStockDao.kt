package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Query
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
}