package com.vilync.ophthalmicerp.feature.backup.model

/**
 * Represents the current authentication state for Google Backup operations.
 */
sealed class GoogleAuthState {
    data object Unauthenticated : GoogleAuthState()
    
    data object Authenticating : GoogleAuthState()
    
    data class Authenticated(
        val email: String,
        val displayName: String?,
        val idToken: String
    ) : GoogleAuthState()
    
    data class Error(val message: String) : GoogleAuthState()
}
