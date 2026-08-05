package com.vilync.ophthalmicerp.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase


object DatabaseProvider {

    private const val TAG = "DATABASE_HEALTH"
    private const val DB_NAME = "vilync_ophthalmic_erp_database"

    @Volatile
    private var INSTANCE: AppDatabase? = null


    // =========================================================
    // HEALTH REPORT MODEL
    // =========================================================

    enum class HealthScore {
        HEALTHY,
        WARNING,
        CRITICAL
    }

    data class DatabaseHealthReport(
        val fileExists: Boolean,
        val canOpen: Boolean,
        val versionOnDisk: Int,
        val expectedVersion: Int,
        val usersCount: Int = 0,
        val productsCount: Int = 0,
        val partiesCount: Int = 0,
        val salesCount: Int = 0,
        val purchasesCount: Int = 0,
        val companyProfileCount: Int = 0,
        val errorMessage: String? = null
    ) {
        val hasBusinessData: Boolean
            get() = productsCount > 0 || partiesCount > 0 || salesCount > 0 || purchasesCount > 0 || companyProfileCount > 0

        val healthScore: HealthScore
            get() = when {
                !fileExists -> HealthScore.HEALTHY // Fresh install is healthy
                !canOpen || versionOnDisk != expectedVersion -> HealthScore.CRITICAL
                usersCount == 0 && hasBusinessData -> HealthScore.CRITICAL // Accidental overwrite risk
                usersCount == 0 && !hasBusinessData -> HealthScore.WARNING // Setup required
                else -> HealthScore.HEALTHY
            }

        val isHealthy: Boolean
            get() = healthScore == HealthScore.HEALTHY || (!fileExists && usersCount == 0 && !hasBusinessData)
    }


    // =========================================================
    // GET DATABASE
    // =========================================================

    fun getDatabase(
        context: Context
    ): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            val existingInstance = INSTANCE

            if (existingInstance != null) {

                existingInstance

            } else {

                val newInstance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .addMigrations(
                            AppDatabase.MIGRATION_1_2,
                            AppDatabase.MIGRATION_2_3,
                            AppDatabase.MIGRATION_3_4,
                            AppDatabase.MIGRATION_4_5,
                            AppDatabase.MIGRATION_5_6,
                            AppDatabase.MIGRATION_6_7,
                            AppDatabase.MIGRATION_7_8,
                            AppDatabase.MIGRATION_8_9,
                            AppDatabase.MIGRATION_9_10,
                            AppDatabase.MIGRATION_10_11,
                            AppDatabase.MIGRATION_11_12,
                            AppDatabase.MIGRATION_12_13,
                            AppDatabase.MIGRATION_13_14,
                            AppDatabase.MIGRATION_14_15,
                            AppDatabase.MIGRATION_15_16,
                            AppDatabase.MIGRATION_16_17,
                            AppDatabase.MIGRATION_17_18,
                            AppDatabase.MIGRATION_18_19,
                            AppDatabase.MIGRATION_19_20,
                            AppDatabase.MIGRATION_20_21,
                            AppDatabase.MIGRATION_21_22,
                            AppDatabase.MIGRATION_22_23,
                            AppDatabase.MIGRATION_23_24,
                            AppDatabase.MIGRATION_24_25,
                            AppDatabase.MIGRATION_25_26
                        )
                        .fallbackToDestructiveMigration(false)
                        .build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }


    // =========================================================
    // VERIFY DATABASE HEALTH
    // =========================================================

    fun verifyDatabaseHealth(
        context: Context
    ): DatabaseHealthReport {

        val dbFile = context.getDatabasePath(DB_NAME)
        val fileExists = dbFile.exists()

        if (!fileExists) {
            return DatabaseHealthReport(
                fileExists = false,
                canOpen = false,
                versionOnDisk = 0,
                expectedVersion = 26
            )
        }

        var canOpen = false
        var versionOnDisk = -1
        var usersCount = 0
        var productsCount = 0
        var partiesCount = 0
        var salesCount = 0
        var purchasesCount = 0
        var companyProfileCount = 0
        var error: String? = null

        try {
            // 1. Force Room initialization to run migrations
            val roomDb = getDatabase(context)
            val sqliteDb = roomDb.openHelper.readableDatabase

            canOpen = true
            versionOnDisk = sqliteDb.version

            // 2. Perform counts for business detection
            usersCount = getTableCount(sqliteDb, "users")
            productsCount = getTableCount(sqliteDb, "products")
            partiesCount = getTableCount(sqliteDb, "parties")
            salesCount = getTableCount(sqliteDb, "sales")
            purchasesCount = getTableCount(sqliteDb, "purchases")
            companyProfileCount = getTableCount(sqliteDb, "company_profile")

            Log.d(TAG, "Database Health Check: Version=$versionOnDisk, Users=$usersCount, HasBusinessData=${productsCount > 0}")

        } catch (e: Exception) {
            Log.e(TAG, "Database Health Check FAILED", e)
            error = e.message ?: "Unknown SQLite error."
        }

        return DatabaseHealthReport(
            fileExists = true,
            canOpen = canOpen,
            versionOnDisk = versionOnDisk,
            expectedVersion = 26,
            usersCount = usersCount,
            productsCount = productsCount,
            partiesCount = partiesCount,
            salesCount = salesCount,
            purchasesCount = purchasesCount,
            companyProfileCount = companyProfileCount,
            errorMessage = error
        )
    }

    private fun getTableCount(
        db: SupportSQLiteDatabase,
        tableName: String
    ): Int {
        return try {
            db.query("SELECT COUNT(*) FROM $tableName").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }


    // =========================================================
    // CLOSE DATABASE
    // =========================================================
    //
    // Required by the controlled Backup Restore flow.
    //
    // Restore must NEVER replace the physical SQLite database
    // file while Room still owns an open connection to it.
    //
    // After this method:
    //
    // 1. The current Room instance is closed.
    // 2. The singleton reference is cleared.
    // 3. The next getDatabase() call creates a fresh Room
    //    instance against the restored physical database.
    //
    // Normal ERP operation does not need to call this method.
    // =========================================================

    fun closeDatabase() {

        synchronized(this) {

            val currentInstance = INSTANCE

            if (currentInstance != null) {

                if (currentInstance.isOpen) {
                    currentInstance.close()
                }

                INSTANCE = null
            }
        }
    }
}
