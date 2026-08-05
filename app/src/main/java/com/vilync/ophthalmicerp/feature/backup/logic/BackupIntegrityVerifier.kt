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
        return runCatching {
            var summary = RestoreSummary()
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                summary = summary.copy(
                    dbVersion = db.version,
                    usersCount = getCount(db, "users"),
                    productsCount = getCount(db, "products"),
                    partiesCount = getCount(db, "parties"),
                    salesCount = getCount(db, "sales"),
                    purchasesCount = getCount(db, "purchases")
                )
                
                db.rawQuery("SELECT legalName, gstin FROM company_profile LIMIT 1", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        summary = summary.copy(
                            companyName = cursor.getString(0) ?: "Unknown",
                            companyGst = cursor.getString(1) ?: "N/A"
                        )
                    }
                }
            }
            summary
        }
    }

    /**
     * Performs a post-restore validation to ensure the database is functional.
     */
    fun validatePostRestore(dbFile: File): Result<Unit> {
        val integrity = verify(dbFile)
        if (!integrity.isSuccess) return Result.failure(Exception(integrity.message))
        
        return runCatching {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val users = getCount(db, "users")
                val products = getCount(db, "products")
                val companyProfile = getCount(db, "company_profile")
                val sales = getCount(db, "sales")
                val purchases = getCount(db, "purchases")
                
                if (users == 0) throw Exception("Post-restore validation failed: No user accounts found.")
                if (companyProfile == 0) throw Exception("Post-restore validation failed: Company Profile is missing.")
                
                Log.d(TAG, "Post-restore summary: Users=$users, Products=$products, Profile=$companyProfile, Sales=$sales, Purchases=$purchases")
            }
            Unit
        }
    }

    private fun getCount(db: SQLiteDatabase, table: String): Int {
        return try {
            db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }
}
