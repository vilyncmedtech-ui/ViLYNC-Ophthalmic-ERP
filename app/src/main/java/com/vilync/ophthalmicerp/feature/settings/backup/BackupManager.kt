package com.vilync.ophthalmicerp.feature.settings.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Local Backup + Restore manager for ViLYNC Ophthalmic ERP.
 *
 * Responsibilities:
 *
 * BACKUP
 * - Create a consistent local backup of the current Room database.
 * - Keep backups in app-private storage.
 * - List backup history.
 * - Perform SQLite integrity validation.
 *
 * RESTORE
 * - Validate the selected backup before touching the live database.
 * - Check the SQLite database version.
 * - Create a pre-restore safety backup.
 * - Close the active Room database.
 * - Remove stale WAL / SHM sidecar files.
 * - Replace the live database using a temporary restore file.
 * - Reopen through the normal DatabaseProvider.
 * - Allow Room to perform registered migrations.
 * - Verify the restored database after Room opens it.
 *
 * The selected source backup is never modified.
 */
class BackupManager(
    context: Context
) {

    private val appContext: Context =
        context.applicationContext


    private val backupDirectory: File =
        File(
            appContext.filesDir,
            BACKUP_DIRECTORY_NAME
        )


    // =========================================================
    // MODELS
    // =========================================================

    data class BackupInfo(
        val file: File,
        val fileName: String,
        val createdAt: Long,
        val sizeBytes: Long,
        val checksumSha256: String?,
        val isValid: Boolean
    )


    data class BackupResult(
        val success: Boolean,
        val backup: BackupInfo? = null,
        val message: String
    )


    data class RestoreResult(
        val success: Boolean,
        val safetyBackup: BackupInfo? = null,
        val restoredFrom: BackupInfo? = null,
        val sourceDatabaseVersion: Int? = null,
        val finalDatabaseVersion: Int? = null,
        val message: String
    )


    // =========================================================
    // CREATE BACKUP
    // =========================================================

    suspend fun createBackup(): BackupResult =
        withContext(Dispatchers.IO) {

            createBackupInternal(
                filePrefix = BACKUP_FILE_PREFIX,
                successMessage = "Backup created successfully."
            )
        }


    // =========================================================
    // CREATE BACKUP INTERNAL
    // =========================================================

    private fun createBackupInternal(
        filePrefix: String,
        successMessage: String
    ): BackupResult {

        ensureBackupDirectory()


        val liveDatabaseFile =
            appContext.getDatabasePath(
                DATABASE_NAME
            )


        if (!liveDatabaseFile.exists()) {

            return BackupResult(
                success = false,
                message = "ERP database file was not found."
            )
        }


        val timestamp =
            SimpleDateFormat(
                BACKUP_TIMESTAMP_PATTERN,
                Locale.US
            ).format(Date())


        val finalBackupFile =
            createUniqueBackupFile(
                prefix = filePrefix,
                timestamp = timestamp
            )


        val temporaryBackupFile =
            File(
                backupDirectory,
                "${finalBackupFile.name}.tmp"
            )


        return runCatching {

            deleteIfExists(
                temporaryBackupFile
            )


            /*
             * Always use the application's existing Room database.
             */
            val roomDatabase =
                DatabaseProvider.getDatabase(
                    appContext
                )


            val sqliteDatabase =
                roomDatabase.openHelper
                    .writableDatabase


            /*
             * Room normally uses WAL.
             *
             * FULL checkpoint ensures committed WAL transactions
             * are moved into the main database file before copying.
             */
            sqliteDatabase.query(
                "PRAGMA wal_checkpoint(FULL)"
            ).use { cursor ->

                if (cursor.moveToFirst()) {

                    val busy =
                        cursor.getInt(0)


                    if (busy != 0) {

                        error(
                            "Database is busy. Backup was not created."
                        )
                    }
                }
            }


            copyFileSafely(
                source = liveDatabaseFile,
                destination = temporaryBackupFile
            )


            if (
                !temporaryBackupFile.exists() ||
                temporaryBackupFile.length() <= 0L
            ) {

                error(
                    "Backup file could not be created."
                )
            }


            if (
                !validateSQLiteDatabase(
                    temporaryBackupFile
                )
            ) {

                error(
                    "Backup integrity validation failed."
                )
            }


            moveTemporaryFileIntoPlace(
                temporaryFile = temporaryBackupFile,
                finalFile = finalBackupFile
            )


            val backupInfo =
                buildBackupInfo(
                    file = finalBackupFile,
                    validate = false
                ).copy(
                    isValid = true
                )


            BackupResult(
                success = true,
                backup = backupInfo,
                message = successMessage
            )

        }.getOrElse { throwable ->

            deleteIfExists(
                temporaryBackupFile
            )


            if (
                finalBackupFile.exists() &&
                finalBackupFile.length() <= 0L
            ) {

                deleteIfExists(
                    finalBackupFile
                )
            }


            BackupResult(
                success = false,
                message =
                    throwable.message
                        ?: "Backup could not be created."
            )
        }
    }


    // =========================================================
    // BACKUP HISTORY
    // =========================================================

    suspend fun getBackupHistory(
        validateFiles: Boolean = false
    ): List<BackupInfo> =
        withContext(Dispatchers.IO) {

            ensureBackupDirectory()


            backupDirectory
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter { file ->

                    file.isFile &&
                            isRecognisedBackupFile(
                                file
                            )
                }
                .sortedByDescending { file ->
                    file.lastModified()
                }
                .map { file ->

                    buildBackupInfo(
                        file = file,
                        validate = validateFiles
                    )
                }
                .toList()
        }


    // =========================================================
    // VALIDATE BACKUP
    // =========================================================

    suspend fun validateBackup(
        file: File
    ): Boolean =
        withContext(Dispatchers.IO) {

            if (
                !file.exists() ||
                !file.isFile ||
                file.length() <= 0L
            ) {

                return@withContext false
            }


            validateSQLiteDatabase(
                file
            )
        }


    // =========================================================
    // GET DATABASE VERSION
    // =========================================================

    suspend fun getDatabaseVersion(
        file: File
    ): Int? =
        withContext(Dispatchers.IO) {

            readDatabaseVersion(
                file
            )
        }


    // =========================================================
    // RESTORE BACKUP
    // =========================================================

    suspend fun restoreBackup(
        backupFile: File
    ): RestoreResult =
        withContext(Dispatchers.IO) {

            ensureBackupDirectory()


            /*
             * -------------------------------------------------
             * STEP 1
             * Validate selected source backup.
             * -------------------------------------------------
             */

            if (
                !backupFile.exists() ||
                !backupFile.isFile ||
                backupFile.length() <= 0L
            ) {

                return@withContext RestoreResult(
                    success = false,
                    message =
                        "Selected backup file was not found or is empty."
                )
            }


            if (
                !validateSQLiteDatabase(
                    backupFile
                )
            ) {

                return@withContext RestoreResult(
                    success = false,
                    message =
                        "Selected backup failed SQLite integrity validation."
                )
            }


            val sourceVersion =
                readDatabaseVersion(
                    backupFile
                )


            if (sourceVersion == null) {

                return@withContext RestoreResult(
                    success = false,
                    message =
                        "Selected backup database version could not be read."
                )
            }


            /*
             * A backup newer than the application database must never
             * be restored because Room cannot migrate backwards.
             *
             * Older backups are allowed because the normal
             * DatabaseProvider migration chain is responsible for
             * upgrading them.
             */
            if (
                sourceVersion >
                CURRENT_DATABASE_VERSION
            ) {

                return@withContext RestoreResult(
                    success = false,
                    sourceDatabaseVersion =
                        sourceVersion,
                    message =
                        "Backup database version $sourceVersion is newer " +
                                "than this ERP database version " +
                                "$CURRENT_DATABASE_VERSION."
                )
            }


            /*
             * -------------------------------------------------
             * STEP 2
             * Create mandatory pre-restore safety backup.
             * -------------------------------------------------
             */

            val safetyBackupResult =
                createBackupInternal(
                    filePrefix =
                        PRE_RESTORE_BACKUP_FILE_PREFIX,
                    successMessage =
                        "Pre-restore safety backup created."
                )


            if (!safetyBackupResult.success) {

                return@withContext RestoreResult(
                    success = false,
                    sourceDatabaseVersion =
                        sourceVersion,
                    message =
                        "Restore stopped because the current ERP database " +
                                "could not be backed up safely. " +
                                safetyBackupResult.message
                )
            }


            val safetyBackup =
                safetyBackupResult.backup


            /*
             * -------------------------------------------------
             * STEP 3
             * Determine live DB paths.
             * -------------------------------------------------
             */

            val liveDatabaseFile =
                appContext.getDatabasePath(
                    DATABASE_NAME
                )


            val liveDatabaseDirectory =
                liveDatabaseFile.parentFile


            if (
                liveDatabaseDirectory != null &&
                !liveDatabaseDirectory.exists()
            ) {

                check(
                    liveDatabaseDirectory.mkdirs() ||
                            liveDatabaseDirectory.exists()
                ) {
                    "Database directory could not be created."
                }
            }


            val walFile =
                File(
                    "${liveDatabaseFile.absolutePath}-wal"
                )


            val shmFile =
                File(
                    "${liveDatabaseFile.absolutePath}-shm"
                )


            val restoreTemporaryFile =
                File(
                    liveDatabaseFile.parentFile,
                    "${DATABASE_NAME}.restore_tmp"
                )


            var databaseWasClosed = false


            try {

                /*
                 * -------------------------------------------------
                 * STEP 4
                 * Close Room before touching physical DB files.
                 * -------------------------------------------------
                 */

                DatabaseProvider.closeDatabase()

                databaseWasClosed = true


                /*
                 * Stale WAL/SHM from the previous live DB must not
                 * survive into the restored database.
                 */
                deleteIfExists(
                    walFile
                )

                deleteIfExists(
                    shmFile
                )

                deleteIfExists(
                    restoreTemporaryFile
                )


                /*
                 * -------------------------------------------------
                 * STEP 5
                 * Copy selected backup to a temporary file first.
                 *
                 * The source backup itself is never modified.
                 * -------------------------------------------------
                 */

                copyFileSafely(
                    source = backupFile,
                    destination =
                        restoreTemporaryFile
                )


                if (
                    !restoreTemporaryFile.exists() ||
                    restoreTemporaryFile.length() <= 0L
                ) {

                    error(
                        "Temporary restore database could not be created."
                    )
                }


                if (
                    !validateSQLiteDatabase(
                        restoreTemporaryFile
                    )
                ) {

                    error(
                        "Temporary restore database failed integrity validation."
                    )
                }


                /*
                 * -------------------------------------------------
                 * STEP 6
                 * Replace live database.
                 * -------------------------------------------------
                 */

                if (liveDatabaseFile.exists()) {

                    if (!liveDatabaseFile.delete()) {

                        error(
                            "Current ERP database file could not be replaced."
                        )
                    }
                }


                moveTemporaryFileIntoPlace(
                    temporaryFile =
                        restoreTemporaryFile,
                    finalFile =
                        liveDatabaseFile
                )


                /*
                 * Ensure no sidecar files are present before Room
                 * opens the restored DB.
                 */
                deleteIfExists(
                    walFile
                )

                deleteIfExists(
                    shmFile
                )


                /*
                 * -------------------------------------------------
                 * STEP 7
                 * Reopen through normal DatabaseProvider.
                 *
                 * If source is v18 and current ERP is v19,
                 * registered Room MIGRATION_18_19 runs here.
                 * -------------------------------------------------
                 */

                val restoredRoomDatabase =
                    DatabaseProvider.getDatabase(
                        appContext
                    )


                val restoredSqliteDatabase =
                    restoredRoomDatabase
                        .openHelper
                        .writableDatabase


                /*
                 * Force database open and schema validation.
                 */
                restoredSqliteDatabase.query(
                    "SELECT 1"
                ).use { cursor ->

                    if (!cursor.moveToFirst()) {

                        error(
                            "Restored database could not be opened."
                        )
                    }
                }


                /*
                 * Flush migration/schema work into main DB.
                 */
                restoredSqliteDatabase.query(
                    "PRAGMA wal_checkpoint(FULL)"
                ).use { cursor ->

                    if (cursor.moveToFirst()) {

                        val busy =
                            cursor.getInt(0)


                        if (busy != 0) {

                            error(
                                "Restored database is busy after migration."
                            )
                        }
                    }
                }


                val finalVersion =
                    restoredSqliteDatabase.version


                if (
                    finalVersion !=
                    CURRENT_DATABASE_VERSION
                ) {

                    error(
                        "Restored database opened with version " +
                                "$finalVersion instead of expected " +
                                "$CURRENT_DATABASE_VERSION."
                    )
                }


                /*
                 * -------------------------------------------------
                 * STEP 8
                 * Final SQLite integrity check while Room owns DB.
                 * -------------------------------------------------
                 */

                restoredSqliteDatabase.query(
                    "PRAGMA integrity_check"
                ).use { cursor ->

                    val valid =
                        cursor.moveToFirst() &&
                                cursor.getString(0)
                                    .equals(
                                        "ok",
                                        ignoreCase = true
                                    )


                    if (!valid) {

                        error(
                            "Restored database failed final integrity validation."
                        )
                    }
                }


                /*
                 * Final checkpoint so migrated schema/data are
                 * safely represented in the main database file.
                 */
                restoredSqliteDatabase.query(
                    "PRAGMA wal_checkpoint(FULL)"
                ).use { }


                val restoredBackupInfo =
                    buildBackupInfo(
                        file = backupFile,
                        validate = false
                    ).copy(
                        isValid = true
                    )


                RestoreResult(
                    success = true,
                    safetyBackup =
                        safetyBackup,
                    restoredFrom =
                        restoredBackupInfo,
                    sourceDatabaseVersion =
                        sourceVersion,
                    finalDatabaseVersion =
                        finalVersion,
                    message =
                        "Backup restored successfully. " +
                                "Database version $sourceVersion was opened " +
                                "as ERP database version $finalVersion."
                )

            } catch (restoreThrowable: Throwable) {

                /*
                 * -------------------------------------------------
                 * RESTORE FAILURE RECOVERY
                 * -------------------------------------------------
                 *
                 * The pre-restore safety backup is used to put the
                 * live ERP database back exactly where it was before
                 * this restore attempt.
                 */

                runCatching {

                    if (!databaseWasClosed) {

                        DatabaseProvider.closeDatabase()

                        databaseWasClosed = true
                    } else {

                        DatabaseProvider.closeDatabase()
                    }


                    deleteIfExists(
                        restoreTemporaryFile
                    )

                    deleteIfExists(
                        walFile
                    )

                    deleteIfExists(
                        shmFile
                    )


                    if (
                        safetyBackup != null &&
                        safetyBackup.file.exists() &&
                        validateSQLiteDatabase(
                            safetyBackup.file
                        )
                    ) {

                        val rollbackTemporaryFile =
                            File(
                                liveDatabaseFile.parentFile,
                                "${DATABASE_NAME}.rollback_tmp"
                            )


                        deleteIfExists(
                            rollbackTemporaryFile
                        )


                        copyFileSafely(
                            source =
                                safetyBackup.file,
                            destination =
                                rollbackTemporaryFile
                        )


                        if (
                            !validateSQLiteDatabase(
                                rollbackTemporaryFile
                            )
                        ) {

                            error(
                                "Safety backup failed rollback validation."
                            )
                        }


                        if (
                            liveDatabaseFile.exists() &&
                            !liveDatabaseFile.delete()
                        ) {

                            error(
                                "Failed restore could not be rolled back."
                            )
                        }


                        moveTemporaryFileIntoPlace(
                            temporaryFile =
                                rollbackTemporaryFile,
                            finalFile =
                                liveDatabaseFile
                        )


                        deleteIfExists(
                            walFile
                        )

                        deleteIfExists(
                            shmFile
                        )


                        /*
                         * Reopen original database after rollback.
                         */
                        DatabaseProvider.getDatabase(
                            appContext
                        )
                            .openHelper
                            .writableDatabase
                    }

                }.onFailure { rollbackThrowable ->

                    return@withContext RestoreResult(
                        success = false,
                        safetyBackup =
                            safetyBackup,
                        sourceDatabaseVersion =
                            sourceVersion,
                        message =
                            "Restore failed: " +
                                    (
                                            restoreThrowable.message
                                                ?: "Unknown restore error."
                                            ) +
                                    " Automatic rollback also failed: " +
                                    (
                                            rollbackThrowable.message
                                                ?: "Unknown rollback error."
                                            ) +
                                    " Do not clear app data. " +
                                    "The pre-restore safety backup has been preserved."
                    )
                }


                return@withContext RestoreResult(
                    success = false,
                    safetyBackup =
                        safetyBackup,
                    sourceDatabaseVersion =
                        sourceVersion,
                    message =
                        "Restore failed and the previous ERP database " +
                                "was restored from the pre-restore safety backup. " +
                                (
                                        restoreThrowable.message
                                            ?: "Unknown restore error."
                                        )
                )
            }
        }


    // =========================================================
    // BACKUP DIRECTORY
    // =========================================================

    fun getBackupDirectory(): File {

        ensureBackupDirectory()

        return backupDirectory
    }


    // =========================================================
    // ENSURE BACKUP DIRECTORY
    // =========================================================

    private fun ensureBackupDirectory() {

        if (!backupDirectory.exists()) {

            check(
                backupDirectory.mkdirs() ||
                        backupDirectory.exists()
            ) {
                "Backup directory could not be created."
            }
        }
    }


    // =========================================================
    // RECOGNISED BACKUP FILE
    // =========================================================

    private fun isRecognisedBackupFile(
        file: File
    ): Boolean {

        if (
            !file.name.endsWith(
                BACKUP_FILE_EXTENSION
            )
        ) {

            return false
        }


        return file.name.startsWith(
            BACKUP_FILE_PREFIX
        ) ||
                file.name.startsWith(
                    PRE_RESTORE_BACKUP_FILE_PREFIX
                )
    }


    // =========================================================
    // CREATE UNIQUE BACKUP FILE
    // =========================================================

    private fun createUniqueBackupFile(
        prefix: String,
        timestamp: String
    ): File {

        var candidate =
            File(
                backupDirectory,
                "${prefix}${timestamp}${BACKUP_FILE_EXTENSION}"
            )


        if (!candidate.exists()) {

            return candidate
        }


        var counter = 1


        while (candidate.exists()) {

            candidate =
                File(
                    backupDirectory,
                    "${prefix}${timestamp}_${counter}${BACKUP_FILE_EXTENSION}"
                )

            counter++
        }


        return candidate
    }


    // =========================================================
    // COPY FILE SAFELY
    // =========================================================

    private fun copyFileSafely(
        source: File,
        destination: File
    ) {

        if (
            source.absolutePath ==
            destination.absolutePath
        ) {

            error(
                "Source and destination database files are the same."
            )
        }


        destination.parentFile?.let { parent ->

            if (!parent.exists()) {

                check(
                    parent.mkdirs() ||
                            parent.exists()
                ) {
                    "Destination directory could not be created."
                }
            }
        }


        FileInputStream(
            source
        ).use { input ->

            FileOutputStream(
                destination
            ).use { output ->

                input.copyTo(
                    output
                )

                output.fd.sync()
            }
        }
    }


    // =========================================================
    // MOVE TEMPORARY FILE INTO PLACE
    // =========================================================

    private fun moveTemporaryFileIntoPlace(
        temporaryFile: File,
        finalFile: File
    ) {

        if (finalFile.exists()) {

            error(
                "Destination file already exists: ${finalFile.name}"
            )
        }


        if (
            !temporaryFile.renameTo(
                finalFile
            )
        ) {

            copyFileSafely(
                source =
                    temporaryFile,
                destination =
                    finalFile
            )


            if (!temporaryFile.delete()) {

                temporaryFile.deleteOnExit()
            }
        }


        if (
            !finalFile.exists() ||
            finalFile.length() <= 0L
        ) {

            error(
                "Database file could not be moved into place."
            )
        }
    }


    // =========================================================
    // DELETE FILE IF PRESENT
    // =========================================================

    private fun deleteIfExists(
        file: File
    ) {

        if (
            file.exists() &&
            !file.delete()
        ) {

            error(
                "Could not remove temporary database file: ${file.name}"
            )
        }
    }


    // =========================================================
    // BUILD BACKUP INFO
    // =========================================================

    private fun buildBackupInfo(
        file: File,
        validate: Boolean
    ): BackupInfo {

        val valid =
            if (validate) {

                validateSQLiteDatabase(
                    file
                )

            } else {

                file.exists() &&
                        file.isFile &&
                        file.length() > 0L
            }


        val checksum =
            runCatching {

                sha256(
                    file
                )

            }.getOrNull()


        return BackupInfo(
            file = file,
            fileName = file.name,
            createdAt = file.lastModified(),
            sizeBytes = file.length(),
            checksumSha256 = checksum,
            isValid = valid
        )
    }


    // =========================================================
    // SQLITE INTEGRITY VALIDATION
    // =========================================================

    private fun validateSQLiteDatabase(
        file: File
    ): Boolean {

        var database: SQLiteDatabase? =
            null


        return try {

            database =
                SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )


            database.rawQuery(
                "PRAGMA integrity_check",
                null
            ).use { cursor ->

                cursor.moveToFirst() &&
                        cursor.getString(0)
                            .equals(
                                "ok",
                                ignoreCase = true
                            )
            }

        } catch (_: Exception) {

            false

        } finally {

            runCatching {

                database?.close()
            }
        }
    }


    // =========================================================
    // READ SQLITE DATABASE VERSION
    // =========================================================

    private fun readDatabaseVersion(
        file: File
    ): Int? {

        var database: SQLiteDatabase? =
            null


        return try {

            database =
                SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )


            database.version

        } catch (_: Exception) {

            null

        } finally {

            runCatching {

                database?.close()
            }
        }
    }


    // =========================================================
    // SHA-256
    // =========================================================

    private fun sha256(
        file: File
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )


        FileInputStream(
            file
        ).use { input ->

            val buffer =
                ByteArray(
                    DEFAULT_BUFFER_SIZE
                )


            while (true) {

                val count =
                    input.read(
                        buffer
                    )


                if (count <= 0) {

                    break
                }


                digest.update(
                    buffer,
                    0,
                    count
                )
            }
        }


        return digest
            .digest()
            .joinToString("") { byte ->

                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
    }


    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {

        const val DATABASE_NAME =
            "vilync_ophthalmic_erp_database"


        /*
         * Must match AppDatabase @Database(version = ...).
         */
        const val CURRENT_DATABASE_VERSION =
            19


        private const val BACKUP_DIRECTORY_NAME =
            "erp_backups"


        private const val BACKUP_FILE_PREFIX =
            "ViLYNC_ERP_"


        private const val PRE_RESTORE_BACKUP_FILE_PREFIX =
            "ViLYNC_ERP_PreRestore_"


        private const val BACKUP_FILE_EXTENSION =
            ".db"


        private const val BACKUP_TIMESTAMP_PATTERN =
            "yyyyMMdd_HHmmss"
    }
}