package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.UserDao
import com.vilync.ophthalmicerp.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow


class UserRepository(
    private val userDao: UserDao
) {

    // =========================================================
    // CREATE USER
    // =========================================================

    suspend fun insertUser(
        user: UserEntity
    ): Long {

        return userDao.insertUser(
            user.copy(
                username = user.username
                    .trim()
                    .lowercase()
            )
        )
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    suspend fun updateUser(
        user: UserEntity
    ) {

        userDao.updateUser(
            user.copy(
                username = user.username
                    .trim()
                    .lowercase(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }


    // =========================================================
    // USER BY ID
    // =========================================================

    suspend fun getUserById(
        userId: Long
    ): UserEntity? {

        return userDao.getUserById(
            userId
        )
    }


    // =========================================================
    // USER BY USERNAME
    // =========================================================

    suspend fun getUserByUsername(
        username: String
    ): UserEntity? {

        val normalizedUsername =
            username
                .trim()
                .lowercase()

        if (normalizedUsername.isBlank()) {
            return null
        }

        return userDao.getUserByUsername(
            normalizedUsername
        )
    }


    // =========================================================
    // ACTIVE USER BY USERNAME
    // =========================================================

    suspend fun getActiveUserByUsername(
        username: String
    ): UserEntity? {

        val normalizedUsername =
            username
                .trim()
                .lowercase()

        if (normalizedUsername.isBlank()) {
            return null
        }

        return userDao.getActiveUserByUsername(
            normalizedUsername
        )
    }


    // =========================================================
    // USERNAME EXISTS
    // =========================================================

    suspend fun usernameExists(
        username: String
    ): Boolean {

        val normalizedUsername =
            username
                .trim()
                .lowercase()

        if (normalizedUsername.isBlank()) {
            return false
        }

        return userDao.usernameExists(
            normalizedUsername
        )
    }


    // =========================================================
    // USER COUNT
    // =========================================================

    suspend fun getUserCount(): Int {

        return userDao.getUserCount()
    }


    // =========================================================
    // FIRST USER / ADMIN SETUP CHECK
    // =========================================================

    /*
     * false:
     * No ERP user exists yet.
     * First Admin Setup should be shown.
     *
     * true:
     * At least one ERP user exists.
     * Normal Login flow should be shown.
     */
    suspend fun hasAnyUser(): Boolean {

        return userDao.hasAnyUser()
    }


    // =========================================================
    // ALL USERS
    // =========================================================

    fun getAllUsers():
            Flow<List<UserEntity>> {

        return userDao.getAllUsers()
    }


    // =========================================================
    // ACTIVE USERS
    // =========================================================

    fun getActiveUsers():
            Flow<List<UserEntity>> {

        return userDao.getActiveUsers()
    }
}