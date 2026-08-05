package com.vilync.ophthalmicerp.core.security

import android.content.Context

class PersistentSessionStore(
    private val context: Context
) {

    fun getContext(): Context = context

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun saveUserId(
        userId: Long
    ) {
        preferences.edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(): Long? {
        if (!preferences.contains(KEY_USER_ID)) {
            return null
        }

        val userId =
            preferences.getLong(
                KEY_USER_ID,
                NO_USER_ID
            )

        return if (userId > 0L) {
            userId
        } else {
            null
        }
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "vilync_erp_session"

        private const val KEY_USER_ID =
            "authenticated_user_id"

        private const val NO_USER_ID =
            -1L
    }
}
