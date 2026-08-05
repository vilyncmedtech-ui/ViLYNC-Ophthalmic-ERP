package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context

/**
 * Persistent settings for the Backup Module.
 */
class BackupSettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGoogleDriveFolderId(folderId: String) {
        prefs.edit().putString(KEY_FOLDER_ID, folderId).apply()
    }

    fun getGoogleDriveFolderId(): String? {
        return prefs.getString(KEY_FOLDER_ID, null)
    }

    fun saveGoogleAccountEmail(email: String?) {
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    fun getGoogleAccountEmail(): String? {
        return prefs.getString(KEY_ACCOUNT_EMAIL, null)
    }

    fun setRollbackOccurred(occurred: Boolean) {
        prefs.edit().putBoolean(KEY_ROLLBACK_OCCURRED, occurred).apply()
    }

    fun didRollbackOccur(): Boolean {
        return prefs.getBoolean(KEY_ROLLBACK_OCCURRED, false)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "vilync_erp_backup_settings"
        private const val KEY_FOLDER_ID = "google_drive_folder_id"
        private const val KEY_ACCOUNT_EMAIL = "google_account_email"
        private const val KEY_ROLLBACK_OCCURRED = "rollback_occurred"
    }
}
