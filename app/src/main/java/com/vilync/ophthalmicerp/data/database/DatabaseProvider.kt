package com.vilync.ophthalmicerp.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.vilync.ophthalmicerp.feature.login.model.StartupFailure
import com.vilync.ophthalmicerp.feature.login.model.StartupFacts

/**
 * Infrastructure component responsible for managing the physical SQLite database.
 * No business logic or navigation determination allowed here.
 */
object DatabaseProvider {

    private const val TAG = "DATABASE_PROVIDER"
    private const val DB_NAME = "vilync_ophthalmic_erp_database"
    private const val EXPECTED_VERSION = 34

    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * Obtains the singleton Room database instance.
     */
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val existingInstance = INSTANCE
            if (existingInstance != null) {
                existingInstance
            } else {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(
                    AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
                    AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13,
                    AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16,
                    AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
                    AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22,
                    AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25,
                    AppDatabase.MIGRATION_25_26, AppDatabase.MIGRATION_26_27, AppDatabase.MIGRATION_27_28,
                    AppDatabase.MIGRATION_28_29, AppDatabase.MIGRATION_29_30, AppDatabase.MIGRATION_30_31,
                    AppDatabase.MIGRATION_31_32, AppDatabase.MIGRATION_32_33, AppDatabase.MIGRATION_33_34
                )
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = newInstance
                newInstance
            }
        }
    }

    /**
     * Verifies physical database connectivity and schema version.
     * Replaces previous verifyDatabaseHealth with infrastructure-only reporting.
     */
    fun checkInfrastructure(context: Context): StartupFacts {
        val dbFile = context.getDatabasePath(DB_NAME)
        val fileExists = dbFile.exists()

        if (!fileExists) {
            return createFreshInstallFacts()
        }

        return try {
            Log.d(TAG, "Gating Startup: Verifying Database Infrastructure...")
            
            // Force synchronous initialization and migration check via writable handle
            val roomDb = getDatabase(context)
            val sqliteDb = roomDb.openHelper.writableDatabase
            val actualVersion = sqliteDb.version

            if (actualVersion != EXPECTED_VERSION) {
                Log.e(TAG, "MIGRATION_FAILURE: Expected $EXPECTED_VERSION, Found $actualVersion")
                return createMigrationFailureFacts(actualVersion)
            }

            StartupFacts(
                fileExists = true,
                canOpen = true,
                versionOnDisk = actualVersion,
                expectedVersion = EXPECTED_VERSION,
                schemaHealthy = true,
                migrationsCompleted = true,
                administratorExists = false, // Fact finding deferred to Repository
                businessDataExists = false,   // Fact finding deferred to Repository
                infrastructureHealthy = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Database infrastructure check CRITICAL FAILURE", e)
            createFailureFacts(e)
        }
    }

    private fun createFreshInstallFacts() = StartupFacts(
        fileExists = false,
        canOpen = false,
        versionOnDisk = 0,
        expectedVersion = EXPECTED_VERSION,
        schemaHealthy = true,
        migrationsCompleted = true,
        administratorExists = false,
        businessDataExists = false,
        infrastructureHealthy = true
    )

    private fun createMigrationFailureFacts(actual: Int) = StartupFacts(
        fileExists = true,
        canOpen = true,
        versionOnDisk = actual,
        expectedVersion = EXPECTED_VERSION,
        schemaHealthy = false,
        migrationsCompleted = false,
        administratorExists = false,
        businessDataExists = false,
        infrastructureHealthy = false,
        failure = StartupFailure.MigrationFailure(actual, EXPECTED_VERSION)
    )

    private fun createFailureFacts(e: Exception) = StartupFacts(
        fileExists = true,
        canOpen = false,
        versionOnDisk = -1,
        expectedVersion = EXPECTED_VERSION,
        schemaHealthy = false,
        migrationsCompleted = false,
        administratorExists = false,
        businessDataExists = false,
        infrastructureHealthy = false,
        failure = StartupFailure.DatabaseOpenFailure(e)
    )

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

    /**
     * Forces a full checkpoint of the Write-Ahead Log (WAL) into the main database file.
     * This is critical before performing a file-based backup to ensure data completeness.
     */
    fun checkpoint(context: Context) {
        try {
            val db = getDatabase(context)
            if (db.isOpen) {
                val sqliteDb = db.openHelper.writableDatabase
                sqliteDb.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                    if (cursor.moveToFirst()) {
                        val busy = cursor.getInt(0)
                        val log = cursor.getInt(1)
                        val checkpointed = cursor.getInt(2)
                        Log.i(TAG, "WAL Checkpoint SUCCESS: Busy=$busy, LogFrames=$log, Checkpointed=$checkpointed")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WAL Checkpoint FAILED", e)
        }
    }
}
