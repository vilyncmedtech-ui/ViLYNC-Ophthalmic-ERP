package com.vilync.ophthalmicerp.data.database

import android.content.Context
import androidx.room.Room


object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null


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
                        "vilync_ophthalmic_erp_database"
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
                            AppDatabase.MIGRATION_18_19
                        )
                        .fallbackToDestructiveMigration(false)
                        .build()

                INSTANCE = newInstance

                newInstance
            }
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