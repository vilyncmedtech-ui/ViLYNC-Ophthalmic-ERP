package com.vilync.ophthalmicerp.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


object PasswordSecurity {

    private const val SALT_LENGTH_BYTES = 16
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    private const val ALGORITHM =
        "PBKDF2WithHmacSHA256"


    // =========================================================
    // GENERATE SALT
    // =========================================================

    fun generateSalt(): String {

        val salt =
            ByteArray(
                SALT_LENGTH_BYTES
            )

        SecureRandom()
            .nextBytes(
                salt
            )

        return Base64
            .getEncoder()
            .encodeToString(
                salt
            )
    }


    // =========================================================
    // HASH PASSWORD
    // =========================================================

    fun hashPassword(
        password: String,
        saltBase64: String
    ): String {

        require(
            password.isNotEmpty()
        ) {
            "Password cannot be empty."
        }

        val salt =
            Base64
                .getDecoder()
                .decode(
                    saltBase64
                )

        val specification =
            PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH_BITS
            )

        return try {

            val factory =
                SecretKeyFactory
                    .getInstance(
                        ALGORITHM
                    )

            val hash =
                factory
                    .generateSecret(
                        specification
                    )
                    .encoded

            Base64
                .getEncoder()
                .encodeToString(
                    hash
                )

        } finally {

            specification.clearPassword()
        }
    }


    // =========================================================
    // CREATE PASSWORD CREDENTIALS
    // =========================================================

    fun createCredentials(
        password: String
    ): PasswordCredentials {

        val salt =
            generateSalt()

        val hash =
            hashPassword(
                password = password,
                saltBase64 = salt
            )

        return PasswordCredentials(
            passwordHash = hash,
            passwordSalt = salt
        )
    }


    // =========================================================
    // VERIFY PASSWORD
    // =========================================================

    fun verifyPassword(
        password: String,
        expectedHashBase64: String,
        saltBase64: String
    ): Boolean {

        if (
            password.isEmpty() ||
            expectedHashBase64.isBlank() ||
            saltBase64.isBlank()
        ) {
            return false
        }

        return try {

            val actualHash =
                Base64
                    .getDecoder()
                    .decode(
                        hashPassword(
                            password = password,
                            saltBase64 = saltBase64
                        )
                    )

            val expectedHash =
                Base64
                    .getDecoder()
                    .decode(
                        expectedHashBase64
                    )

            MessageDigest.isEqual(
                actualHash,
                expectedHash
            )

        } catch (_: Exception) {

            false
        }
    }
}


// =============================================================
// PASSWORD CREDENTIALS
// =============================================================

data class PasswordCredentials(

    val passwordHash: String,

    val passwordSalt: String
)