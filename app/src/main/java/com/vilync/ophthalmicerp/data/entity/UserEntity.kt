package com.vilync.ophthalmicerp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "users",
    indices = [
        Index(
            value = ["username"],
            unique = true
        )
    ]
)
data class UserEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // =========================================================
    // USER IDENTITY
    // =========================================================

    val username: String,

    val displayName: String = "",


    // =========================================================
    // AUTHENTICATION
    // =========================================================

    /*
     * IMPORTANT:
     *
     * Plain-text password must NEVER be stored here.
     *
     * passwordHash stores the derived password hash.
     * passwordSalt stores the unique salt used for this user.
     */
    val passwordHash: String,

    val passwordSalt: String,


    // =========================================================
    // ROLE
    // =========================================================

    /*
     * Initial supported roles:
     *
     * ADMIN
     * MANAGER
     * USER
     *
     * Later this can be connected to a more detailed
     * permission system without changing Audit ownership.
     */
    val role: String = "USER",


    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    /*
     * Disabled users remain in the database so their old
     * Audit Trail references continue to remain valid.
     */
    val isActive: Boolean = true,


    // =========================================================
    // AUDIT / SYSTEM METADATA
    // =========================================================

    /*
     * Epoch milliseconds.
     *
     * Example:
     * System.currentTimeMillis()
     */
    val createdAt: Long =
        System.currentTimeMillis(),

    val updatedAt: Long =
        System.currentTimeMillis()
)