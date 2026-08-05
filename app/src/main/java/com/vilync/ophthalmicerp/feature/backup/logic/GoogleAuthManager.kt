package com.vilync.ophthalmicerp.feature.backup.logic

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vilync.ophthalmicerp.BuildConfig
import com.vilync.ophthalmicerp.feature.backup.model.GoogleAuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production-grade Authentication Manager for Google Identity.
 * 
 * Uses the modern Android Credential Manager API.
 */
class GoogleAuthManager(private val context: Context) {

    private val TAG = "GOOGLE_AUTH"
    private val credentialManager = CredentialManager.create(context)
    
    private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Unauthenticated)
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    /**
     * Attempts to restore an existing Google session silently.
     */
    suspend fun checkExistingSession() {
        Log.d(TAG, "checkExistingSession() initiated")
        
        try {
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (serverClientId.isBlank()) return

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true) // Only existing authorized accounts
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true) // Attempt silent sign-in
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // For silent check, we don't use activity context to avoid showing UI
            // but Credential Manager might still throw if it needs UI.
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            handleSignInResult(result)
            Log.d(TAG, "Existing session restored successfully")

        } catch (e: Exception) {
            Log.d(TAG, "No existing session found or silent restore failed: ${e.message}")
            // Silently fail, keep Unauthenticated state
        }
    }

    /**
     * Triggers the Google Sign-In flow.
     */
    suspend fun signIn(activityContext: Context) {
        Log.d(TAG, "signIn() initiated")
        _authState.value = GoogleAuthState.Authenticating
        
        try {
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (serverClientId.isBlank()) {
                throw Exception("Google Web Client ID is missing. Check local.properties.")
            }

            Log.d(TAG, "Server Client ID: $serverClientId")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts on device
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false) // Force Account Picker
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Requesting credentials...")
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            Log.d(TAG, "Credential received successfully")
            handleSignInResult(result)

        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.type} - ${e.message}")
            val message = when (e.type) {
                "androidx.credentials.TYPE_USER_CANCELED_ERROR" -> "Sign-in cancelled by user."
                "androidx.credentials.TYPE_NO_CREDENTIAL_ERROR" -> "No Google accounts found on this device."
                "androidx.credentials.TYPE_INTERRUPTED_ERROR" -> "Authentication interrupted. Please try again."
                else -> "Authentication failed: ${e.message ?: "Unknown error"}"
            }
            _authState.value = GoogleAuthState.Error(message)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Exception during signIn", e)
            val msg = e.message ?: ""
            val userFriendly = when {
                msg.contains("Network", true) -> "Network failure. Please check your internet connection."
                msg.contains("Play Services", true) -> "Google Play Services is missing or needs update."
                msg.contains("Developer", true) -> "Invalid OAuth Configuration (SHA-1/Client ID mismatch)."
                else -> "Connection failed: ${e.message}"
            }
            _authState.value = GoogleAuthState.Error(userFriendly)
        }
    }

    /**
     * Reverses the sign-in state and clears credentials.
     */
    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _authState.value = GoogleAuthState.Unauthenticated
        } catch (e: Exception) {
            _authState.value = GoogleAuthState.Error("Sign out failed: ${e.message}")
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        Log.d(TAG, "Handling result. Type: ${credential.type}")
        
        when (credential) {
            is GoogleIdTokenCredential -> {
                Log.d(TAG, "Received GoogleIdTokenCredential directly")
                _authState.value = GoogleAuthState.Authenticated(
                    email = credential.id,
                    displayName = credential.displayName,
                    idToken = credential.idToken
                )
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        Log.d(TAG, "Parsing GoogleIdTokenCredential from CustomCredential")
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        _authState.value = GoogleAuthState.Authenticated(
                            email = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName,
                            idToken = googleIdTokenCredential.idToken
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Google ID Token from CustomCredential", e)
                        _authState.value = GoogleAuthState.Error("Failed to parse identity token: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Unexpected CustomCredential type: ${credential.type}")
                    _authState.value = GoogleAuthState.Error("Unexpected credential type: ${credential.type}")
                }
            }
            is PasswordCredential -> {
                Log.d(TAG, "Received PasswordCredential - not supported for Google Backup")
                _authState.value = GoogleAuthState.Error("Google Account selection is required, but a password was provided.")
            }
            is PublicKeyCredential -> {
                Log.d(TAG, "Received PublicKeyCredential - not supported for Google Backup")
                _authState.value = GoogleAuthState.Error("Google Account selection is required, but a passkey was provided.")
            }
            else -> {
                Log.e(TAG, "Unknown credential type received: ${credential.type}")
                _authState.value = GoogleAuthState.Error("Unsupported authentication method: ${credential.type}")
            }
        }
    }

    /**
     * Provides the current authenticated user if available.
     */
    fun getAuthenticatedUser(): GoogleAuthState.Authenticated? {
        return _authState.value as? GoogleAuthState.Authenticated
    }
}
