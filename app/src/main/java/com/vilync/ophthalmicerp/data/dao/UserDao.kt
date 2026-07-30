package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface UserDao {

    // =========================================================
    // CREATE USER
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertUser(
        user: UserEntity
    ): Long


    // =========================================================
    // UPDATE USER
    // =========================================================

    @Update
    suspend fun updateUser(
        user: UserEntity
    )


    // =========================================================
    // USER BY ID
    // =========================================================

    @Query(
        """
        SELECT * FROM users
        WHERE id = :userId
        LIMIT 1
        """
    )
    suspend fun getUserById(
        userId: Long
    ): UserEntity?


    // =========================================================
    // USER BY USERNAME
    // =========================================================

    @Query(
        """
        SELECT * FROM users
        WHERE LOWER(TRIM(username)) =
              LOWER(TRIM(:username))
        LIMIT 1
        """
    )
    suspend fun getUserByUsername(
        username: String
    ): UserEntity?


    // =========================================================
    // ACTIVE USER BY USERNAME
    // =========================================================

    @Query(
        """
        SELECT * FROM users
        WHERE LOWER(TRIM(username)) =
              LOWER(TRIM(:username))
          AND isActive = 1
        LIMIT 1
        """
    )
    suspend fun getActiveUserByUsername(
        username: String
    ): UserEntity?


    // =========================================================
    // USERNAME EXISTS
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM users
            WHERE LOWER(TRIM(username)) =
                  LOWER(TRIM(:username))
            LIMIT 1
        )
        """
    )
    suspend fun usernameExists(
        username: String
    ): Boolean


    // =========================================================
    // USER COUNT
    // =========================================================

    /*
     * Used during application bootstrap.
     *
     * If count = 0:
     * No ERP user exists yet and the First Admin Setup
     * screen must be shown.
     *
     * If count > 0:
     * Normal Login flow must be shown.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM users
        """
    )
    suspend fun getUserCount(): Int


    // =========================================================
    // HAS ANY USER
    // =========================================================

    /*
     * Convenience method for First Admin Setup detection.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM users
            LIMIT 1
        )
        """
    )
    suspend fun hasAnyUser(): Boolean


    // =========================================================
    // ALL USERS
    // =========================================================

    /*
     * Inactive users are intentionally included.
     *
     * Historical Audit Trail records may reference an inactive
     * user, therefore inactive users must not be hard-deleted.
     */
    @Query(
        """
        SELECT * FROM users
        ORDER BY displayName COLLATE NOCASE ASC,
                 username COLLATE NOCASE ASC
        """
    )
    fun getAllUsers():
            Flow<List<UserEntity>>


    // =========================================================
    // ACTIVE USERS
    // =========================================================

    @Query(
        """
        SELECT * FROM users
        WHERE isActive = 1
        ORDER BY displayName COLLATE NOCASE ASC,
                 username COLLATE NOCASE ASC
        """
    )
    fun getActiveUsers():
            Flow<List<UserEntity>>
}