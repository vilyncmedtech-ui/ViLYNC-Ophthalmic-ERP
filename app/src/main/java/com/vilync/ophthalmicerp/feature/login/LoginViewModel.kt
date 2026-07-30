package com.vilync.ophthalmicerp.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.security.PersistentSessionStore
import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.data.repository.AuditTrailRepository
import com.vilync.ophthalmicerp.data.repository.AuthRepository
import com.vilync.ophthalmicerp.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoggingIn: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)


class LoginViewModel(

    private val authRepository: AuthRepository,

    private val auditTrailRepository: AuditTrailRepository,

    private val persistentSessionStore: PersistentSessionStore

) : ViewModel() {


    companion object {

        private const val TAG =
            "ViLYNC_LOGIN"
    }


    private val _uiState =
        MutableStateFlow(
            LoginUiState()
        )


    val uiState: StateFlow<LoginUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // USERNAME
    // =========================================================

    fun updateUsername(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                username = value,
                errorMessage = null
            )
    }


    // =========================================================
    // PASSWORD
    // =========================================================

    fun updatePassword(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                password = value,
                errorMessage = null
            )
    }


    // =========================================================
    // LOGIN
    // =========================================================

    fun login() {

        val state =
            _uiState.value


        if (state.isLoggingIn) {
            return
        }


        val username =
            state.username.trim()

        val password =
            state.password


        if (username.isBlank()) {

            setError(
                "Username is required."
            )

            return
        }


        if (password.isBlank()) {

            setError(
                "Password is required."
            )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoggingIn = true,
                    errorMessage = null,
                    isLoginSuccessful = false
                )


            try {

                Log.d(
                    TAG,
                    "Login started for username: $username"
                )


                val result =
                    authRepository.authenticate(
                        username = username,
                        password = password
                    )


                when (result) {

                    is AuthResult.Success -> {

                        Log.d(
                            TAG,
                            "Authentication successful. User ID: ${result.user.id}"
                        )


                        // =====================================
                        // START IN-MEMORY SESSION
                        // =====================================

                        SessionManager.startSession(
                            user = result.user
                        )


                        Log.d(
                            TAG,
                            "SessionManager session started."
                        )


                        // =====================================
                        // SAVE PERSISTENT SESSION
                        // =====================================

                        persistentSessionStore.saveUserId(
                            userId = result.user.id
                        )


                        Log.d(
                            TAG,
                            "Persistent session saved."
                        )


                        // =====================================
                        // AUDIT LOGIN
                        // =====================================

                        try {

                            auditTrailRepository.recordEvent(
                                module = "SECURITY",
                                action = "LOGIN",
                                recordId = result.user.id,
                                description =
                                    "User logged in successfully"
                            )


                            Log.d(
                                TAG,
                                "Login audit event saved."
                            )

                        } catch (auditException: Exception) {

                            /*
                             * Audit failure must NOT invalidate
                             * a successful login.
                             */

                            Log.e(
                                TAG,
                                "Audit logging failed during login.",
                                auditException
                            )
                        }


                        // =====================================
                        // LOGIN SUCCESS
                        // =====================================

                        _uiState.value =
                            _uiState.value.copy(
                                password = "",
                                isLoggingIn = false,
                                errorMessage = null,
                                isLoginSuccessful = true
                            )


                        Log.d(
                            TAG,
                            "Login completed successfully."
                        )
                    }


                    is AuthResult.Failure -> {

                        Log.w(
                            TAG,
                            "Authentication failed: ${result.message}"
                        )


                        _uiState.value =
                            _uiState.value.copy(
                                password = "",
                                isLoggingIn = false,
                                errorMessage =
                                    result.message,
                                isLoginSuccessful = false
                            )
                    }
                }

            } catch (exception: Exception) {

                /*
                 * IMPORTANT:
                 *
                 * Never log:
                 * - password
                 * - password hash
                 * - password salt
                 *
                 * The exception itself is logged so we can
                 * identify the actual login failure.
                 */

                Log.e(
                    TAG,
                    "Unexpected exception during login.",
                    exception
                )


                _uiState.value =
                    _uiState.value.copy(
                        password = "",
                        isLoggingIn = false,
                        errorMessage =
                            "Unable to sign in. Please try again.",
                        isLoginSuccessful = false
                    )
            }
        }
    }


    // =========================================================
    // LOGIN SUCCESS CONSUMED
    // =========================================================

    fun consumeLoginSuccess() {

        _uiState.value =
            _uiState.value.copy(
                isLoginSuccessful = false
            )
    }


    // =========================================================
    // ERROR
    // =========================================================

    private fun setError(
        message: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = message
            )
    }
}


// =============================================================
// LOGIN VIEWMODEL FACTORY
// =============================================================

class LoginViewModelFactory(

    private val authRepository: AuthRepository,

    private val auditTrailRepository: AuditTrailRepository,

    private val persistentSessionStore: PersistentSessionStore

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                LoginViewModel::class.java
            )
        ) {

            return LoginViewModel(
                authRepository =
                    authRepository,

                auditTrailRepository =
                    auditTrailRepository,

                persistentSessionStore =
                    persistentSessionStore
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}