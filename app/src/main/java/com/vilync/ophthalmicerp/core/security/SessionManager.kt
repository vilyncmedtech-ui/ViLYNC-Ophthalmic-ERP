package com.vilync.ophthalmicerp.core.security

import com.vilync.ophthalmicerp.data.entity.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object SessionManager {

    // =========================================================
    // CURRENT LOGGED-IN USER
    // =========================================================

    private val _currentUser =
        MutableStateFlow<UserEntity?>(
            null
        )

    val currentUser: StateFlow<UserEntity?> =
        _currentUser.asStateFlow()


    // =========================================================
    // LOGIN
    // =========================================================

    fun startSession(
        user: UserEntity
    ) {

        require(user.isActive) {
            "Inactive user cannot start a session."
        }

        _currentUser.value =
            user
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    fun clearSession() {

        _currentUser.value =
            null
    }


    // =========================================================
    // SESSION STATE
    // =========================================================

    fun isLoggedIn(): Boolean {

        return _currentUser.value != null
    }


    // =========================================================
    // CURRENT USER HELPERS
    // =========================================================

    fun currentUserId(): Long? {

        return _currentUser.value?.id
    }


    fun currentUsername(): String? {

        return _currentUser.value?.username
    }


    fun currentDisplayName(): String? {

        return _currentUser.value?.displayName
    }


    fun currentRole(): String? {

        return _currentUser.value?.role
    }


    // =========================================================
    // ROLE CHECK
    // =========================================================

    fun hasRole(
        role: String
    ): Boolean {

        return _currentUser
            .value
            ?.role
            ?.equals(
                role,
                ignoreCase = true
            )
            ?: false
    }
}