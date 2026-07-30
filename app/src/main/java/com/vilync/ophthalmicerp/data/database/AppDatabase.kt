package com.vilync.ophthalmicerp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vilync.ophthalmicerp.data.dao.AuditTrailDao
import com.vilync.ophthalmicerp.data.dao.ChallanDao
import com.vilync.ophthalmicerp.data.dao.InventoryDao
import com.vilync.ophthalmicerp.data.dao.InventoryStockDao
import com.vilync.ophthalmicerp.data.dao.SerialStockDao
import com.vilync.ophthalmicerp.data.dao.ProductDao
import com.vilync.ophthalmicerp.data.dao.PurchaseDao
import com.vilync.ophthalmicerp.data.dao.PurchaseReturnDao
import com.vilync.ophthalmicerp.data.dao.SalesDao
import com.vilync.ophthalmicerp.data.dao.SalesCreditNoteDao
import com.vilync.ophthalmicerp.data.dao.ProformaInvoiceDao
import com.vilync.ophthalmicerp.data.dao.SampleIssueDao
import com.vilync.ophthalmicerp.data.dao.StockMovementDao
import com.vilync.ophthalmicerp.data.dao.UserDao
import com.vilync.ophthalmicerp.data.entity.AuditTrailEntity
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SaleItemEntity
import com.vilync.ophthalmicerp.data.entity.SaleLensEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceEntity
import com.vilync.ophthalmicerp.data.entity.ProformaInvoiceItemEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import com.vilync.ophthalmicerp.data.entity.StockMovementEntity
import com.vilync.ophthalmicerp.data.entity.UserEntity
import com.vilync.ophthalmicerp.master.party.data.PartyDao
import com.vilync.ophthalmicerp.master.party.data.PartyEntity
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity


@Database(
    entities = [
        ProductEntity::class,
        InventoryUnitEntity::class,
        StockMovementEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        PurchaseLensEntity::class,
        PurchaseReturnEntity::class,
        PurchaseReturnItemEntity::class,
        PurchaseReturnLensEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SaleLensEntity::class,
        PartyEntity::class,
        UserEntity::class,
        AuditTrailEntity::class,
        ChallanEntity::class,
        ChallanItemEntity::class,
        SalesCreditNoteEntity::class,
        SalesCreditNoteItemEntity::class,
        SalesCreditNoteLensEntity::class,
        ProformaInvoiceEntity::class,
        ProformaInvoiceItemEntity::class,
        SampleIssueEntity::class,
        SampleIssueItemEntity::class,
        CompanyProfileEntity::class
    ],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    // =========================================================
    // PRODUCT
    // =========================================================

    abstract fun productDao(): ProductDao


    // =========================================================
    // INVENTORY
    // =========================================================

    abstract fun inventoryDao(): InventoryDao

    abstract fun inventoryStockDao(): InventoryStockDao

    abstract fun serialStockDao(): SerialStockDao


    // =========================================================
    // STOCK MOVEMENT
    // =========================================================

    abstract fun stockMovementDao(): StockMovementDao


    // =========================================================
    // PURCHASE
    // =========================================================

    abstract fun purchaseDao(): PurchaseDao


    // =========================================================
    // PURCHASE RETURN
    // =========================================================

    abstract fun purchaseReturnDao(): PurchaseReturnDao


    // =========================================================
    // SALES
    // =========================================================

    abstract fun salesDao(): SalesDao

    abstract fun challanDao(): ChallanDao

    abstract fun salesCreditNoteDao(): SalesCreditNoteDao

    abstract fun proformaInvoiceDao(): ProformaInvoiceDao

    abstract fun sampleIssueDao(): SampleIssueDao


    // =========================================================
    // PARTY MASTER
    // =========================================================

    abstract fun partyDao(): PartyDao


    // =========================================================
    // USER / AUTHENTICATION
    // =========================================================

    abstract fun userDao(): UserDao


    // =========================================================
    // CENTRAL AUDIT TRAIL
    // =========================================================

    abstract fun auditTrailDao(): AuditTrailDao


    // =========================================================
    // COMPANY PROFILE
    // =========================================================

    abstract fun companyProfileDao(): CompanyProfileDao


    companion object {


        // =====================================================
        // MIGRATION 1 -> 2
        // =====================================================
        //
        // Product Master fields added.
        //
        // Existing product data remains preserved.
        // =====================================================

        val MIGRATION_1_2 =
            object : Migration(
                1,
                2
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {


                    // ==========================================
                    // UNIT
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'
                        """.trimIndent()
                    )


                    // ==========================================
                    // TAX INFORMATION
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN hsnCode TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )


                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN gstPercent REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    // ==========================================
                    // PURCHASE PRICING
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN purchaseGstAmount REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN netPurchasePrice REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    // ==========================================
                    // RETAIL PRICING
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN retailPrice REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN retailGstAmount REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN netRetailPrice REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )


                    // ==========================================
                    // MRP
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN mrp REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 2 -> 3
        // =====================================================
        //
        // Party Master introduced.
        //
        // Creates:
        // parties table
        // partyName index
        // mobileNumber index
        // gstin index
        //
        // Existing Product / Purchase / Inventory data
        // remains untouched.
        // =====================================================

        val MIGRATION_2_3 =
            object : Migration(
                2,
                3
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {


                    // ==========================================
                    // CREATE PARTIES TABLE
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS parties (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            partyType TEXT NOT NULL,
                            partyName TEXT NOT NULL,
                            contactPerson TEXT NOT NULL,
                            mobileNumber TEXT NOT NULL,
                            alternateMobileNumber TEXT NOT NULL,
                            email TEXT NOT NULL,
                            gstin TEXT NOT NULL,
                            panNumber TEXT NOT NULL,
                            addressLine1 TEXT NOT NULL,
                            addressLine2 TEXT NOT NULL,
                            city TEXT NOT NULL,
                            district TEXT NOT NULL,
                            state TEXT NOT NULL,
                            pinCode TEXT NOT NULL,
                            creditDays INTEGER NOT NULL,
                            creditLimit REAL NOT NULL,
                            isActive INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )


                    // ==========================================
                    // PARTY NAME INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_parties_partyName
                        ON parties(partyName)
                        """.trimIndent()
                    )


                    // ==========================================
                    // MOBILE NUMBER INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_parties_mobileNumber
                        ON parties(mobileNumber)
                        """.trimIndent()
                    )


                    // ==========================================
                    // GSTIN INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_parties_gstin
                        ON parties(gstin)
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 3 -> 4
        // =====================================================
        //
        // GSTIN Lookup preparation.
        //
        // Adds:
        // legalName
        // tradeName
        //
        // Existing Party records remain preserved.
        // =====================================================

        val MIGRATION_3_4 =
            object : Migration(
                3,
                4
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {


                    // ==========================================
                    // LEGAL NAME
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE parties
                        ADD COLUMN legalName TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )


                    // ==========================================
                    // TRADE NAME
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE parties
                        ADD COLUMN tradeName TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 4 -> 5
        // =====================================================
        //
        // Introduces physical IOL lens-level persistence.
        //
        // One row in purchase_lenses =
        // one physical IOL serial number + its exact expiry.
        //
        // Existing purchase_items.expiryDate is intentionally
        // retained for backward compatibility.
        // =====================================================

        val MIGRATION_4_5 =
            object : Migration(
                4,
                5
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // CREATE PURCHASE LENSES TABLE
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS purchase_lenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            purchaseItemId INTEGER NOT NULL,
                            serialNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            FOREIGN KEY(purchaseItemId)
                                REFERENCES purchase_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )


                    // ==========================================
                    // PURCHASE ITEM INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_lenses_purchaseItemId
                        ON purchase_lenses(purchaseItemId)
                        """.trimIndent()
                    )


                    // ==========================================
                    // UNIQUE SERIAL NUMBER INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_purchase_lenses_serialNumber
                        ON purchase_lenses(serialNumber)
                        """.trimIndent()
                    )


                    // ==========================================
                    // EXPIRY INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_lenses_expiryDate
                        ON purchase_lenses(expiryDate)
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 5 -> 6
        // =====================================================
        //
        // Adds stable supplierId to purchases.
        //
        // Existing purchase rows are mapped to Party Master
        // using supplierName -> partyName where an exact
        // case-insensitive trimmed match exists.
        //
        // Legacy/unmatched rows remain supplierId = 0.
        //
        // Duplicate protection for NEW mapped purchases:
        // supplierId + normalized invoiceNumber must be unique.
        //
        // Existing duplicate historical rows are NOT deleted.
        // If duplicates already exist for a mapped supplier,
        // only one row keeps the mapped supplierId; additional
        // duplicate legacy rows remain supplierId = 0 so that
        // migration can preserve all historical data safely.
        // =====================================================

        val MIGRATION_5_6 =
            object : Migration(
                5,
                6
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // ADD SUPPLIER ID
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE purchases
                        ADD COLUMN supplierId INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )


                    // ==========================================
                    // BACKFILL SAFE LEGACY PURCHASES
                    // ==========================================
                    //
                    // Map only the first historical row for each
                    // Party + normalized Invoice combination.
                    // This preserves old duplicate rows without
                    // deleting or merging them.
                    // ==========================================

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET supplierId = COALESCE(
                            (
                                SELECT p.id
                                FROM parties p
                                WHERE UPPER(TRIM(p.partyName)) =
                                      UPPER(TRIM(purchases.supplierName))
                                ORDER BY p.id ASC
                                LIMIT 1
                            ),
                            0
                        )
                        WHERE id IN (
                            SELECT MIN(pr.id)
                            FROM purchases pr
                            GROUP BY
                                UPPER(TRIM(pr.supplierName)),
                                UPPER(TRIM(pr.invoiceNumber))
                        )
                        """.trimIndent()
                    )


                    // ==========================================
                    // SUPPLIER ID INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchases_supplierId
                        ON purchases(supplierId)
                        """.trimIndent()
                    )


                    // ==========================================
                    // UNIQUE VENDOR + INVOICE INDEX
                    // ==========================================
                    //
                    // Partial unique index intentionally excludes
                    // supplierId = 0 legacy/unmatched rows.
                    //
                    // COLLATE NOCASE blocks invoice case variants.
                    // New save flow also stores trimmed/uppercase
                    // invoice numbers for consistent persistence.
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_purchases_supplierId_invoiceNumber
                        ON purchases(
                            supplierId,
                            invoiceNumber COLLATE NOCASE
                        )
                        WHERE supplierId > 0
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 6 -> 7
        // =====================================================
        //
        // Production duplicate-invoice protection.
        //
        // Adds normalizedInvoiceNumber as the canonical
        // duplicate key.
        //
        // New business rule:
        //
        // supplierId + normalizedInvoiceNumber = UNIQUE
        //
        // Existing purchase rows are NEVER deleted.
        //
        // For valid historical supplierId > 0 rows:
        // the oldest row keeps the real normalized invoice key.
        // Any already-existing historical duplicate gets a
        // migration-only internal marker so migration succeeds
        // without deleting accounting history.
        //
        // Legacy supplierId <= 0 rows also receive an internal
        // marker because they cannot safely participate in the
        // vendor-level uniqueness rule.
        // =====================================================

        val MIGRATION_6_7 =
            object : Migration(
                6,
                7
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // REMOVE OLD V6 COMPOSITE INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        DROP INDEX IF EXISTS
                        index_purchases_supplierId_invoiceNumber
                        """.trimIndent()
                    )


                    // ==========================================
                    // ADD CANONICAL INVOICE KEY
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE purchases
                        ADD COLUMN normalizedInvoiceNumber
                        TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )


                    // ==========================================
                    // INITIAL NORMALIZATION
                    // ==========================================

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET normalizedInvoiceNumber =
                            UPPER(TRIM(invoiceNumber))
                        """.trimIndent()
                    )


                    // ==========================================
                    // PROTECT LEGACY / UNMATCHED SUPPLIERS
                    // ==========================================
                    //
                    // supplierId <= 0 is not a valid new purchase.
                    // Keep those historical rows unique internally.
                    // ==========================================

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET normalizedInvoiceNumber =
                            '__LEGACY_UNMATCHED__' || id || '__' ||
                            UPPER(TRIM(invoiceNumber))
                        WHERE supplierId <= 0
                        """.trimIndent()
                    )


                    // ==========================================
                    // PRESERVE PRE-EXISTING HISTORICAL DUPLICATES
                    // ==========================================
                    //
                    // For each valid supplier + normalized invoice,
                    // the oldest purchase keeps the real key.
                    //
                    // Additional historical duplicate rows are
                    // preserved but receive an internal marker.
                    // Their visible invoiceNumber is NOT changed.
                    // ==========================================

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET normalizedInvoiceNumber =
                            '__LEGACY_DUPLICATE__' || id || '__' ||
                            normalizedInvoiceNumber
                        WHERE supplierId > 0
                          AND id NOT IN (
                              SELECT MIN(p2.id)
                              FROM purchases p2
                              WHERE p2.supplierId > 0
                              GROUP BY
                                  p2.supplierId,
                                  p2.normalizedInvoiceNumber
                          )
                        """.trimIndent()
                    )


                    // ==========================================
                    // FINAL DATABASE-LEVEL UNIQUE CONSTRAINT
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_purchases_supplierId_normalizedInvoiceNumber
                        ON purchases(
                            supplierId,
                            normalizedInvoiceNumber
                        )
                        """.trimIndent()
                    )
                }
            }

        // =====================================================
        // MIGRATION 7 -> 8
        // =====================================================
        //
        // Adds lightweight Indian Financial Year key to purchases.
        //
        // Purchase invoiceDate format: dd/MM/yyyy
        //
        // 15/03/2026 -> financialYearStart = 2025
        // 01/04/2026 -> financialYearStart = 2026
        //
        // Existing purchase rows are preserved.
        // Invalid/unexpected legacy dates remain 0 so they can
        // be corrected explicitly rather than assigned wrongly.
        // =====================================================

        val MIGRATION_7_8 =
            object : Migration(
                7,
                8
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // ADD FINANCIAL YEAR START
                    // ==========================================

                    database.execSQL(
                        """
                        ALTER TABLE purchases
                        ADD COLUMN financialYearStart
                        INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )


                    // ==========================================
                    // BACKFILL FROM INVOICE DATE
                    // ==========================================
                    //
                    // Expected format: dd/MM/yyyy
                    // Month = characters 4-5
                    // Year  = characters 7-10
                    //
                    // April-December -> FY starts same year
                    // January-March  -> FY starts previous year
                    // ==========================================

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET financialYearStart =
                            CASE
                                WHEN LENGTH(TRIM(invoiceDate)) = 10
                                 AND substr(TRIM(invoiceDate), 3, 1) = '/'
                                 AND substr(TRIM(invoiceDate), 6, 1) = '/'
                                 AND CAST(substr(TRIM(invoiceDate), 4, 2) AS INTEGER)
                                     BETWEEN 1 AND 12
                                 AND CAST(substr(TRIM(invoiceDate), 7, 4) AS INTEGER) > 0
                                THEN
                                    CASE
                                        WHEN CAST(
                                            substr(TRIM(invoiceDate), 4, 2)
                                            AS INTEGER
                                        ) >= 4
                                        THEN CAST(
                                            substr(TRIM(invoiceDate), 7, 4)
                                            AS INTEGER
                                        )
                                        ELSE CAST(
                                            substr(TRIM(invoiceDate), 7, 4)
                                            AS INTEGER
                                        ) - 1
                                    END
                                ELSE 0
                            END
                        """.trimIndent()
                    )


                    // ==========================================
                    // FINANCIAL YEAR INDEX
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchases_financialYearStart
                        ON purchases(financialYearStart)
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 8 -> 9
        // =====================================================
        //
        // Repairs legacy Purchase Financial Year values that
        // remained 0 because older backfill expected only:
        //
        // dd/MM/yyyy
        //
        // Purchase Entry later standardized dates as:
        //
        // dd-MM-yyyy
        //
        // This migration accepts BOTH separators and updates
        // ONLY rows where financialYearStart = 0.
        //
        // Existing valid FY values and all Purchase data remain
        // untouched.
        // =====================================================

        val MIGRATION_8_9 =
            object : Migration(
                8,
                9
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        UPDATE purchases
                        SET financialYearStart =
                            CASE
                                WHEN LENGTH(TRIM(invoiceDate)) = 10
                                 AND substr(TRIM(invoiceDate), 3, 1)
                                     IN ('/', '-')
                                 AND substr(TRIM(invoiceDate), 6, 1)
                                     IN ('/', '-')
                                 AND substr(TRIM(invoiceDate), 3, 1) =
                                     substr(TRIM(invoiceDate), 6, 1)
                                 AND CAST(
                                     substr(TRIM(invoiceDate), 1, 2)
                                     AS INTEGER
                                 ) BETWEEN 1 AND 31
                                 AND CAST(
                                     substr(TRIM(invoiceDate), 4, 2)
                                     AS INTEGER
                                 ) BETWEEN 1 AND 12
                                 AND CAST(
                                     substr(TRIM(invoiceDate), 7, 4)
                                     AS INTEGER
                                 ) > 0
                                THEN
                                    CASE
                                        WHEN CAST(
                                            substr(TRIM(invoiceDate), 4, 2)
                                            AS INTEGER
                                        ) >= 4
                                        THEN CAST(
                                            substr(TRIM(invoiceDate), 7, 4)
                                            AS INTEGER
                                        )
                                        ELSE CAST(
                                            substr(TRIM(invoiceDate), 7, 4)
                                            AS INTEGER
                                        ) - 1
                                    END
                                ELSE 0
                            END
                        WHERE financialYearStart = 0
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 9 -> 10
        // =====================================================
        //
        // Introduces ERP User Identity foundation.
        //
        // Existing Purchase, Inventory, Party, Product and
        // Financial Year data remain untouched.
        // =====================================================

        val MIGRATION_9_10 =
            object : Migration(
                9,
                10
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            username TEXT NOT NULL,
                            displayName TEXT NOT NULL,
                            passwordHash TEXT NOT NULL,
                            passwordSalt TEXT NOT NULL,
                            role TEXT NOT NULL,
                            isActive INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_users_username
                        ON users(username)
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 10 -> 11
        // =====================================================
        //
        // Introduces the central, append-only ERP Audit Trail.
        //
        // Existing Purchase, Inventory, Party, Product, User
        // and Financial Year data remain untouched.
        // =====================================================

        val MIGRATION_10_11 =
            object : Migration(
                10,
                11
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // CREATE AUDIT TRAIL TABLE
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS audit_trail (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            module TEXT NOT NULL,
                            action TEXT NOT NULL,
                            recordId INTEGER,
                            referenceNumber TEXT,
                            userId INTEGER,
                            username TEXT,
                            userDisplayName TEXT,
                            userRole TEXT,
                            financialYear TEXT,
                            description TEXT,
                            fieldName TEXT,
                            oldValue TEXT,
                            newValue TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )


                    // ==========================================
                    // AUDIT TRAIL INDEXES
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_module
                        ON audit_trail(module)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_action
                        ON audit_trail(action)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_recordId
                        ON audit_trail(recordId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_userId
                        ON audit_trail(userId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_financialYear
                        ON audit_trail(financialYear)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_audit_trail_createdAt
                        ON audit_trail(createdAt)
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 11 -> 12
        // =====================================================
        //
        // Introduces Purchase Return / Supplier Credit Note
        // persistence with exact original Purchase Item and
        // physical IOL serial-level traceability.
        //
        // Existing Purchase, Inventory, Party, Product, User,
        // Audit Trail and Financial Year data remain untouched.
        // =====================================================

        val MIGRATION_11_12 =
            object : Migration(
                11,
                12
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // PURCHASE RETURN HEADER
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS purchase_returns (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            originalPurchaseId INTEGER NOT NULL,
                            supplierId INTEGER NOT NULL,
                            supplierName TEXT NOT NULL,
                            originalInvoiceNumber TEXT NOT NULL,
                            creditNoteNumber TEXT NOT NULL,
                            creditNoteDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            subTotal REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            roundOff REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            remarks TEXT NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_originalPurchaseId
                        ON purchase_returns(originalPurchaseId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_supplierId
                        ON purchase_returns(supplierId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_creditNoteNumber
                        ON purchase_returns(creditNoteNumber)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_financialYearStart
                        ON purchase_returns(financialYearStart)
                        """.trimIndent()
                    )


                    // ==========================================
                    // PURCHASE RETURN ITEMS
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS purchase_return_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            purchaseReturnId INTEGER NOT NULL,
                            originalPurchaseItemId INTEGER NOT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            rate REAL NOT NULL,
                            gstPercent REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            batchNumber TEXT NOT NULL,
                            lotNumber TEXT NOT NULL,
                            FOREIGN KEY(purchaseReturnId)
                                REFERENCES purchase_returns(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(originalPurchaseItemId)
                                REFERENCES purchase_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_return_items_purchaseReturnId
                        ON purchase_return_items(purchaseReturnId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_return_items_originalPurchaseItemId
                        ON purchase_return_items(originalPurchaseItemId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_return_items_productId
                        ON purchase_return_items(productId)
                        """.trimIndent()
                    )


                    // ==========================================
                    // RETURNED PHYSICAL IOL LENSES
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS purchase_return_lenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            purchaseReturnItemId INTEGER NOT NULL,
                            originalPurchaseLensId INTEGER NOT NULL,
                            serialNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            FOREIGN KEY(purchaseReturnItemId)
                                REFERENCES purchase_return_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(originalPurchaseLensId)
                                REFERENCES purchase_lenses(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_return_lenses_purchaseReturnItemId
                        ON purchase_return_lenses(purchaseReturnItemId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_purchase_return_lenses_originalPurchaseLensId
                        ON purchase_return_lenses(originalPurchaseLensId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_return_lenses_serialNumber
                        ON purchase_return_lenses(serialNumber)
                        """.trimIndent()
                    )
                }
            }
        // =====================================================
        // MIGRATION 12 -> 13
        // =====================================================
        //
        // Adds Purchase Return lifecycle / cancellation fields.
        // Existing Purchase Return data remains preserved.
        // Existing rows are treated as POSTED.
        // =====================================================

        val MIGRATION_12_13 =
            object : Migration(
                12,
                13
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE purchase_returns
                        ADD COLUMN status TEXT NOT NULL DEFAULT 'POSTED'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE purchase_returns
                        ADD COLUMN cancelledAt INTEGER DEFAULT NULL
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE purchase_returns
                        ADD COLUMN cancellationReason TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE purchase_returns
                        ADD COLUMN reversedByPurchaseReturnId INTEGER DEFAULT NULL
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE purchase_returns
                        ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    // Preserve meaningful timestamp for existing returns.
                    database.execSQL(
                        """
                        UPDATE purchase_returns
                        SET updatedAt = createdAt
                        WHERE updatedAt = 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_status
                        ON purchase_returns(status)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_purchase_returns_reversedByPurchaseReturnId
                        ON purchase_returns(reversedByPurchaseReturnId)
                        """.trimIndent()
                    )
                }
            }

        // =====================================================
        // MIGRATION 13 -> 14
        // =====================================================
        //
        // Introduces Sales Invoice persistence with:
        //
        // sales
        // sale_items
        // sale_lenses
        //
        // Physical IOL Sales lenses retain an exact reference
        // to inventory_units through inventoryUnitId.
        //
        // Existing Purchase, Purchase Return, Inventory, Party,
        // Product, User and Audit Trail data remain untouched.
        // =====================================================

        val MIGRATION_13_14 =
            object : Migration(
                13,
                14
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // SALES HEADER
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sales (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            customerName TEXT NOT NULL,
                            invoiceNumber TEXT NOT NULL,
                            normalizedInvoiceNumber TEXT NOT NULL,
                            invoiceDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            subTotal REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            adjustment REAL NOT NULL,
                            roundOff REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            remarks TEXT NOT NULL,
                            status TEXT NOT NULL,
                            cancelledAt INTEGER DEFAULT NULL,
                            cancellationReason TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sales_customerId
                        ON sales(customerId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sales_invoiceNumber
                        ON sales(invoiceNumber)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sales_financialYearStart
                        ON sales(financialYearStart)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sales_status
                        ON sales(status)
                        """.trimIndent()
                    )


                    // ==========================================
                    // SALES ITEMS
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sale_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            saleId INTEGER NOT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            power TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            rate REAL NOT NULL,
                            discountPercent REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstPercent REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            batchNumber TEXT NOT NULL,
                            lotNumber TEXT NOT NULL,
                            FOREIGN KEY(saleId)
                                REFERENCES sales(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sale_items_saleId
                        ON sale_items(saleId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sale_items_productId
                        ON sale_items(productId)
                        """.trimIndent()
                    )


                    // ==========================================
                    // SOLD PHYSICAL IOL LENSES
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sale_lenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            saleItemId INTEGER NOT NULL,
                            inventoryUnitId INTEGER NOT NULL,
                            serialNumber TEXT NOT NULL,
                            power TEXT NOT NULL,
                            batchNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            FOREIGN KEY(saleItemId)
                                REFERENCES sale_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(inventoryUnitId)
                                REFERENCES inventory_units(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sale_lenses_saleItemId
                        ON sale_lenses(saleItemId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sale_lenses_inventoryUnitId
                        ON sale_lenses(inventoryUnitId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_sale_lenses_serialNumber
                        ON sale_lenses(serialNumber)
                        """.trimIndent()
                    )
                }
            }


        val MIGRATION_14_15 =
            object : Migration(14, 15) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN billToLegalName TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN billToGstin TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN billToAddress TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN billToState TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN sameAsBillTo INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE sales ADD COLUMN shipToName TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN shipToGstin TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN shipToAddress TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN shipToState TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN poNumber TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN poDate TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN placeOfSupplyState TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN gstSupplyType TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sales ADD COLUMN cgstAmount REAL NOT NULL DEFAULT 0.0")
                    database.execSQL("ALTER TABLE sales ADD COLUMN sgstAmount REAL NOT NULL DEFAULT 0.0")
                    database.execSQL("ALTER TABLE sales ADD COLUMN igstAmount REAL NOT NULL DEFAULT 0.0")
                }
            }

        val MIGRATION_15_16 =
            object : Migration(15, 16) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS challans (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            customerName TEXT NOT NULL,
                            challanNumber TEXT NOT NULL,
                            normalizedChallanNumber TEXT NOT NULL,
                            challanDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            remarks TEXT NOT NULL,
                            status TEXT NOT NULL,
                            cancelledAt INTEGER DEFAULT NULL,
                            cancellationReason TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challans_customerId ON challans(customerId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challans_challanNumber ON challans(challanNumber)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challans_normalizedChallanNumber ON challans(normalizedChallanNumber)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challans_financialYearStart ON challans(financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challans_status ON challans(status)")

                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS challan_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            challanId INTEGER NOT NULL,
                            inventoryUnitId INTEGER NOT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            serialNumber TEXT NOT NULL,
                            power TEXT NOT NULL,
                            batchNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            rate REAL NOT NULL,
                            gstPercent REAL NOT NULL,
                            settlementStatus TEXT NOT NULL,
                            saleId INTEGER DEFAULT NULL,
                            settledAt INTEGER DEFAULT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            FOREIGN KEY(challanId) REFERENCES challans(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(inventoryUnitId) REFERENCES inventory_units(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
                            FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                        )
                    """.trimIndent())
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challan_items_challanId ON challan_items(challanId)")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_challan_items_inventoryUnitId ON challan_items(inventoryUnitId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challan_items_productId ON challan_items(productId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challan_items_serialNumber ON challan_items(serialNumber)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challan_items_settlementStatus ON challan_items(settlementStatus)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_challan_items_saleId ON challan_items(saleId)")
                }
            }


        // =====================================================
        // MIGRATION 16 -> 17
        // =====================================================
        //
        // Adds the remaining Sales Documents foundation:
        //
        // 1. Sales Credit Note + item + physical lens traceability
        // 2. Proforma Invoice + items
        // 3. Sample Issue + physical inventory items
        //
        // Existing Sales Invoice, Challan, Purchase, Inventory,
        // Party and other application data remain untouched.
        // =====================================================

        val MIGRATION_16_17 =
            object : Migration(16, 17) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // ==========================================
                    // SALES CREDIT NOTE HEADER
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sales_credit_notes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            customerName TEXT NOT NULL,
                            billToLegalName TEXT NOT NULL,
                            billToGstin TEXT NOT NULL,
                            billToAddress TEXT NOT NULL,
                            billToState TEXT NOT NULL,
                            creditNoteNumber TEXT NOT NULL,
                            normalizedCreditNoteNumber TEXT NOT NULL,
                            creditNoteDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            originalSaleId INTEGER DEFAULT NULL,
                            originalInvoiceNumber TEXT NOT NULL,
                            originalInvoiceDate TEXT NOT NULL,
                            placeOfSupplyState TEXT NOT NULL,
                            gstSupplyType TEXT NOT NULL,
                            creditNoteType TEXT NOT NULL,
                            subTotal REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            cgstAmount REAL NOT NULL,
                            sgstAmount REAL NOT NULL,
                            igstAmount REAL NOT NULL,
                            adjustment REAL NOT NULL,
                            roundOff REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            reason TEXT NOT NULL,
                            remarks TEXT NOT NULL,
                            status TEXT NOT NULL,
                            cancelledAt INTEGER DEFAULT NULL,
                            cancellationReason TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_notes_customerId ON sales_credit_notes(customerId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_notes_creditNoteNumber ON sales_credit_notes(creditNoteNumber)")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_credit_notes_normalizedCreditNoteNumber_financialYearStart ON sales_credit_notes(normalizedCreditNoteNumber, financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_notes_financialYearStart ON sales_credit_notes(financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_notes_originalSaleId ON sales_credit_notes(originalSaleId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_notes_status ON sales_credit_notes(status)")


                    // ==========================================
                    // SALES CREDIT NOTE ITEMS
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sales_credit_note_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            creditNoteId INTEGER NOT NULL,
                            originalSaleItemId INTEGER DEFAULT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            power TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            rate REAL NOT NULL,
                            discountPercent REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstPercent REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            batchNumber TEXT NOT NULL,
                            lotNumber TEXT NOT NULL,
                            FOREIGN KEY(creditNoteId)
                                REFERENCES sales_credit_notes(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(originalSaleItemId)
                                REFERENCES sale_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_items_creditNoteId ON sales_credit_note_items(creditNoteId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_items_originalSaleItemId ON sales_credit_note_items(originalSaleItemId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_items_productId ON sales_credit_note_items(productId)")


                    // ==========================================
                    // SALES CREDIT NOTE PHYSICAL LENSES
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sales_credit_note_lenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            creditNoteItemId INTEGER NOT NULL,
                            originalSaleLensId INTEGER DEFAULT NULL,
                            inventoryUnitId INTEGER NOT NULL,
                            serialNumber TEXT NOT NULL,
                            power TEXT NOT NULL,
                            batchNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            returnToStock INTEGER NOT NULL,
                            FOREIGN KEY(creditNoteItemId)
                                REFERENCES sales_credit_note_items(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(originalSaleLensId)
                                REFERENCES sale_lenses(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION,
                            FOREIGN KEY(inventoryUnitId)
                                REFERENCES inventory_units(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_lenses_creditNoteItemId ON sales_credit_note_lenses(creditNoteItemId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_lenses_originalSaleLensId ON sales_credit_note_lenses(originalSaleLensId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_lenses_inventoryUnitId ON sales_credit_note_lenses(inventoryUnitId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_credit_note_lenses_serialNumber ON sales_credit_note_lenses(serialNumber)")


                    // ==========================================
                    // PROFORMA INVOICE HEADER
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS proforma_invoices (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            customerName TEXT NOT NULL,
                            billToLegalName TEXT NOT NULL,
                            billToGstin TEXT NOT NULL,
                            billToAddress TEXT NOT NULL,
                            billToState TEXT NOT NULL,
                            sameAsBillTo INTEGER NOT NULL,
                            shipToName TEXT NOT NULL,
                            shipToGstin TEXT NOT NULL,
                            shipToAddress TEXT NOT NULL,
                            shipToState TEXT NOT NULL,
                            proformaNumber TEXT NOT NULL,
                            normalizedProformaNumber TEXT NOT NULL,
                            proformaDate TEXT NOT NULL,
                            validUntilDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            poNumber TEXT NOT NULL,
                            poDate TEXT NOT NULL,
                            placeOfSupplyState TEXT NOT NULL,
                            gstSupplyType TEXT NOT NULL,
                            subTotal REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            cgstAmount REAL NOT NULL,
                            sgstAmount REAL NOT NULL,
                            igstAmount REAL NOT NULL,
                            adjustment REAL NOT NULL,
                            roundOff REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            remarks TEXT NOT NULL,
                            status TEXT NOT NULL,
                            convertedSaleId INTEGER DEFAULT NULL,
                            convertedAt INTEGER DEFAULT NULL,
                            cancelledAt INTEGER DEFAULT NULL,
                            cancellationReason TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoices_customerId ON proforma_invoices(customerId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoices_proformaNumber ON proforma_invoices(proformaNumber)")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_proforma_invoices_normalizedProformaNumber_financialYearStart ON proforma_invoices(normalizedProformaNumber, financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoices_financialYearStart ON proforma_invoices(financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoices_status ON proforma_invoices(status)")


                    // ==========================================
                    // PROFORMA INVOICE ITEMS
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS proforma_invoice_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            proformaInvoiceId INTEGER NOT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            power TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            rate REAL NOT NULL,
                            discountPercent REAL NOT NULL,
                            discountAmount REAL NOT NULL,
                            taxableAmount REAL NOT NULL,
                            gstPercent REAL NOT NULL,
                            gstAmount REAL NOT NULL,
                            totalAmount REAL NOT NULL,
                            batchNumber TEXT NOT NULL,
                            lotNumber TEXT NOT NULL,
                            FOREIGN KEY(proformaInvoiceId)
                                REFERENCES proforma_invoices(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoice_items_proformaInvoiceId ON proforma_invoice_items(proformaInvoiceId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_proforma_invoice_items_productId ON proforma_invoice_items(productId)")


                    // ==========================================
                    // SAMPLE ISSUE HEADER
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sample_issues (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customerId INTEGER NOT NULL,
                            customerName TEXT NOT NULL,
                            customerGstin TEXT NOT NULL,
                            customerAddress TEXT NOT NULL,
                            customerState TEXT NOT NULL,
                            recipientName TEXT NOT NULL,
                            sampleIssueNumber TEXT NOT NULL,
                            normalizedSampleIssueNumber TEXT NOT NULL,
                            sampleIssueDate TEXT NOT NULL,
                            financialYearStart INTEGER NOT NULL,
                            sampleType TEXT NOT NULL,
                            expectedReturnDate TEXT NOT NULL,
                            remarks TEXT NOT NULL,
                            status TEXT NOT NULL,
                            cancelledAt INTEGER DEFAULT NULL,
                            cancellationReason TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issues_customerId ON sample_issues(customerId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issues_sampleIssueNumber ON sample_issues(sampleIssueNumber)")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sample_issues_normalizedSampleIssueNumber_financialYearStart ON sample_issues(normalizedSampleIssueNumber, financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issues_financialYearStart ON sample_issues(financialYearStart)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issues_status ON sample_issues(status)")


                    // ==========================================
                    // SAMPLE ISSUE PHYSICAL ITEMS
                    // ==========================================

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sample_issue_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            sampleIssueId INTEGER NOT NULL,
                            inventoryUnitId INTEGER NOT NULL,
                            productId INTEGER NOT NULL,
                            productName TEXT NOT NULL,
                            power TEXT NOT NULL,
                            serialNumber TEXT NOT NULL,
                            batchNumber TEXT NOT NULL,
                            expiryDate TEXT NOT NULL,
                            referenceRate REAL NOT NULL,
                            settlementStatus TEXT NOT NULL,
                            returnedAt INTEGER DEFAULT NULL,
                            consumedAt INTEGER DEFAULT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            FOREIGN KEY(sampleIssueId)
                                REFERENCES sample_issues(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(inventoryUnitId)
                                REFERENCES inventory_units(id)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issue_items_sampleIssueId ON sample_issue_items(sampleIssueId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issue_items_inventoryUnitId ON sample_issue_items(inventoryUnitId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issue_items_productId ON sample_issue_items(productId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issue_items_serialNumber ON sample_issue_items(serialNumber)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sample_issue_items_settlementStatus ON sample_issue_items(settlementStatus)")
                }
            }


        // =====================================================
        // MIGRATION 17 -> 18
        // =====================================================
        //
        // Product Master serial prefix added.
        // Existing product and transaction data remains preserved.
        // Existing products receive a blank prefix until configured.
        // =====================================================

        val MIGRATION_17_18 =
            object : Migration(
                17,
                18
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE products
                        ADD COLUMN serialPrefix TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )
                }
            }


        // =====================================================
        // MIGRATION 18 -> 19
        // =====================================================
        //
        // Adds Sales Invoice Due Date and E-Way Number.
        // Existing sales and all other ERP data remain preserved.
        // Existing invoices receive blank values until explicitly set.
        // =====================================================

        val MIGRATION_18_19 =
            object : Migration(
                18,
                19
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        "ALTER TABLE sales ADD COLUMN dueDate TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL(
                        "ALTER TABLE sales ADD COLUMN eWayNumber TEXT NOT NULL DEFAULT ''"
                    )
                }
            }

    }
}
