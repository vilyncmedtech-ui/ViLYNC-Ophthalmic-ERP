package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.core.security.PasswordSecurity
import com.vilync.ophthalmicerp.data.entity.UserEntity


class AuthRepository(
    private val userRepository: UserRepository
) {

    // =========================================================
    // AUTHENTICATE USER
    // =========================================================

    suspend fun authenticate(
        username: String,
        password: String
    ): AuthResult {

        val normalizedUsername =
            username
                .trim()
                .lowercase()

        if (
            normalizedUsername.isBlank() ||
            password.isBlank()
        ) {
            return AuthResult.Failure(
                message = "Username and password are required."
            )
        }


        // -----------------------------------------------------
        // FIND ACTIVE USER
        // -----------------------------------------------------

        val user =
            userRepository.getActiveUserByUsername(
                normalizedUsername
            )
                ?: return AuthResult.Failure(
                    message = "Invalid username or password."
                )


        // -----------------------------------------------------
        // VERIFY PASSWORD
        // -----------------------------------------------------

        val passwordValid =
            PasswordSecurity.verifyPassword(
                password = password,
                expectedHashBase64 = user.passwordHash,
                saltBase64 = user.passwordSalt
            )


        if (!passwordValid) {

            return AuthResult.Failure(
                message = "Invalid username or password."
            )
        }


        // -----------------------------------------------------
        // SUCCESS
        // -----------------------------------------------------

        return AuthResult.Success(
            user = user
        )
    }
}


// =============================================================
// AUTH RESULT
// =============================================================

sealed class AuthResult {

    data class Success(
        val user: UserEntity
    ) : AuthResult()


    data class Failure(
        val message: String
    ) : AuthResult()
}