package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Production-grade database integrity verification.
 * 
 * Performs PRAGMA integrity_check and validates presence of critical ERP tables.
 */
class BackupIntegrityVerifier(private val context: Context) {

    companion object {
        private const val TAG = "BACKUP_INTEGRITY"
        private const val DB_NAME = "vilync_ophthalmic_erp_database"
    }

    data class IntegrityReport(
        val isSuccess: Boolean,
        val message: String,
        val tableCheckPassed: Boolean,
        val sqliteCheckPassed: Boolean
    )

    fun verify(dbFile: File): IntegrityReport {
        if (!dbFile.exists()) {
            return IntegrityReport(false, "Source database file does not exist.", false, false)
        }

        var sqliteCheckPassed = false
        var tableCheckPassed = false
        var error: String? = null

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            // 1. SQLite PRAGMA Check
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    sqliteCheckPassed = result.equals("ok", ignoreCase = true)
                }
            }

            if (!sqliteCheckPassed) {
                return IntegrityReport(false, "SQLite integrity check failed.", false, false)
            }

            // 2. Critical Table Presence Check
            val criticalTables = listOf(
                "products", "sales", "purchases", "parties", "company_profile", "users", "audit_trail"
            )
            
            tableCheckPassed = criticalTables.all { tableName ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
                    cursor.count > 0
                }
            }

            if (!tableCheckPassed) {
                return IntegrityReport(false, "Critical ERP tables are missing in source.", false, true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Integrity verification error", e)
            error = e.message
        } finally {
            db?.close()
        }

        return IntegrityReport(
            isSuccess = sqliteCheckPassed && tableCheckPassed,
            message = error ?: if (sqliteCheckPassed && tableCheckPassed) "Integrity verified successfully." else "Verification failed.",
            tableCheckPassed = tableCheckPassed,
            sqliteCheckPassed = sqliteCheckPassed
        )
    }

    data class RestoreSummary(
        val companyName: String = "Unknown",
        val companyGst: String = "N/A",
        val dbVersion: Int = 0,
        val productsCount: Int = 0,
        val partiesCount: Int = 0,
        val salesCount: Int = 0,
        val purchasesCount: Int = 0,
        val usersCount: Int = 0
    )

    fun getRestoreSummary(dbFile: File): Result<RestoreSummary> {
        Log.i(TAG, "RESTORE_FORENSIC_START: Path=${dbFile.absolutePath} | Size=${dbFile.length()} bytes")
        return runCatching {
            var summary = RestoreSummary()
            // Open specifically in READONLY mode
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val version = db.version
                Log.i(TAG, "RESTORE_FORENSIC: PRAGMA user_version=$version")
                
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        Log.i(TAG, "RESTORE_FORENSIC: PRAGMA integrity_check=${cursor.getString(0)}")
                    }
                }

                logTableInventory(db)
                
                val existingTables = getExistingTables(db)
                
                // Detailed row counting
                val users = if (existingTables.contains("users")) getCount(db, "users") else -1
                val products = if (existingTables.contains("products")) getCount(db, "products") else -1
                val sales = if (existingTables.contains("sales")) getCount(db, "sales") else -1
                val purchases = if (existingTables.contains("purchases")) getCount(db, "purchases") else -1
                val parties = if (existingTables.contains("parties")) getCount(db, "parties") else -1
                val profile = if (existingTables.contains("company_profile")) getCount(db, "company_profile") else -1

                Log.i(TAG, "RESTORE_FORENSIC Counts: Users=$users, Products=$products, Sales=$sales, Purchases=$purchases, Parties=$parties, ProfileRows=$profile")

                summary = summary.copy(
                    dbVersion = version,
                    usersCount = users.coerceAtLeast(0),
                    productsCount = products.coerceAtLeast(0),
                    partiesCount = parties.coerceAtLeast(0),
                    salesCount = sales.coerceAtLeast(0),
                    purchasesCount = purchases.coerceAtLeast(0)
                )
                
                if (existingTables.contains("company_profile")) {
                    try {
                        db.rawQuery("SELECT legalName, gstin FROM company_profile LIMIT 1", null).use { cursor ->
                            if (cursor.moveToFirst()) {
                                val legalName = cursor.getString(0) ?: "Unknown"
                                val gstin = cursor.getString(1) ?: "N/A"
                                Log.i(TAG, "RESTORE_FORENSIC Profile: Name=$legalName, GST=$gstin")
                                summary = summary.copy(
                                    companyName = legalName,
                                    companyGst = gstin
                                )
                            } else {
                                Log.w(TAG, "RESTORE_FORENSIC: company_profile table is empty")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "RESTORE_FORENSIC: Failed to read company_profile row", e)
                    }
                }
            }
            Log.i(TAG, "RESTORE_FORENSIC_FINISH: Summary calculated: $summary")
            summary
        }.onFailure { e ->
            Log.e(TAG, "RESTORE_FORENSIC: getRestoreSummary CRITICAL FAILURE", e)
        }
    }

    private fun getExistingTables(db: SQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        try {
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list tables", e)
        }
        return tables
    }

    fun validatePostRestore(dbFile: File): Result<Unit> {
        Log.d(TAG, "validatePostRestore() for ${dbFile.absolutePath}")
        val integrity = verify(dbFile)
        if (!integrity.isSuccess) {
            Log.e(TAG, "Post-restore integrity check failed: ${integrity.message}")
            return Result.failure(Exception(integrity.message))
        }
        
        return runCatching {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val tables = getExistingTables(db)
                val users = if (tables.contains("users")) getCount(db, "users") else 0
                val products = if (tables.contains("products")) getCount(db, "products") else 0
                val sales = if (tables.contains("sales")) getCount(db, "sales") else 0
                val purchases = if (tables.contains("purchases")) getCount(db, "purchases") else 0
                
                Log.i(TAG, "RESTORE_FORENSIC Post-restore Verify: Users=$users, Products=$products, Sales=$sales, Purchases=$purchases")
                
                if (users == 0) {
                    throw Exception("Restore Aborted: The selected backup contains no valid user accounts.")
                }
                // company_profile rows check removed as confirmed optional.
            }
            Unit
        }.onFailure { e ->
            Log.e(TAG, "validatePostRestore() Logic validation failed", e)
        }
    }

    private fun getCount(db: SQLiteDatabase, table: String): Int {
        return try {
            db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCount failed for table: $table", e)
            0
        }
    }

    private fun logTableInventory(db: SQLiteDatabase) {
        try {
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                val tables = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
                Log.i(TAG, "RESTORE_FORENSIC Table Inventory: ${tables.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inventory tables", e)
        }
    }
}
