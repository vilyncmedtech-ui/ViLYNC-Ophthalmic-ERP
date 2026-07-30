package com.vilync.ophthalmicerp.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


/**
 * Protects ERP screens that require an authenticated user.
 *
 * Behaviour:
 *
 * 1. Active session exists:
 *    Protected screen content is displayed.
 *
 * 2. No active session:
 *    Protected content is NOT displayed and the caller is
 *    instructed to navigate back to Login.
 *
 * IMPORTANT:
 *
 * Authentication itself is performed by AuthRepository.
 * This guard only checks the currently authenticated session.
 */
@Composable
fun SessionGuard(
    onSessionExpired: () -> Unit,
    content: @Composable () -> Unit
) {

    val currentUser by
    SessionManager
        .currentUser
        .collectAsState()


    // =========================================================
    // NO ACTIVE SESSION
    // =========================================================

    if (currentUser == null) {

        LaunchedEffect(Unit) {

            onSessionExpired()
        }

        /*
         * Do not compose protected ERP content while there is
         * no authenticated user.
         */
        return
    }


    // =========================================================
    // AUTHENTICATED SESSION
    // =========================================================

    content()
}