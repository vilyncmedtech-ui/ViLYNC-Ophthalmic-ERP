package com.vilync.ophthalmicerp.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.security.PasswordSecurity
import com.vilync.ophthalmicerp.data.entity.UserEntity
import com.vilync.ophthalmicerp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// =============================================================
// UI STATE
// =============================================================

data class FirstAdminSetupUiState(

    val displayName: String = "",

    val username: String = "",

    val password: String = "",

    val confirmPassword: String = "",

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

    val isCreatedSuccessfully: Boolean = false
)


// =============================================================
// VIEW MODEL
// =============================================================

class FirstAdminSetupViewModel(

    private val userRepository: UserRepository

) : ViewModel() {


    private val _uiState =
        MutableStateFlow(
            FirstAdminSetupUiState()
        )

    val uiState: StateFlow<FirstAdminSetupUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // FIELD UPDATES
    // =========================================================

    fun updateDisplayName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                displayName = value,
                errorMessage = null
            )
    }


    fun updateUsername(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                username = value,
                errorMessage = null
            )
    }


    fun updatePassword(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                password = value,
                errorMessage = null
            )
    }


    fun updateConfirmPassword(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                confirmPassword = value,
                errorMessage = null
            )
    }


    // =========================================================
    // CREATE FIRST ADMIN
    // =========================================================

    fun createFirstAdmin() {

        val state =
            _uiState.value


        if (state.isSaving) {
            return
        }


        val displayName =
            state.displayName.trim()

        val username =
            state.username
                .trim()
                .lowercase()

        val password =
            state.password

        val confirmPassword =
            state.confirmPassword


        // -----------------------------------------------------
        // BASIC VALIDATION
        // -----------------------------------------------------

        if (displayName.isBlank()) {

            setError(
                "Display name is required."
            )

            return
        }


        if (username.isBlank()) {

            setError(
                "Username is required."
            )

            return
        }


        if (username.length < 4) {

            setError(
                "Username must contain at least 4 characters."
            )

            return
        }


        if (password.length < 8) {

            setError(
                "Password must contain at least 8 characters."
            )

            return
        }


        if (password != confirmPassword) {

            setError(
                "Password and Confirm Password do not match."
            )

            return
        }


        // -----------------------------------------------------
        // DATABASE OPERATION
        // -----------------------------------------------------

        viewModelScope.launch {

            _uiState.value =
                state.copy(
                    isSaving = true,
                    errorMessage = null
                )


            try {

                // ---------------------------------------------
                // BOOTSTRAP PROTECTION
                // ---------------------------------------------
                //
                // First Admin can be created ONLY when there
                // are no existing ERP users.
                // ---------------------------------------------

                if (userRepository.hasAnyUser()) {

                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            errorMessage =
                                "ERP administrator has already been configured."
                        )

                    return@launch
                }


                // ---------------------------------------------
                // USERNAME CHECK
                // ---------------------------------------------

                if (
                    userRepository.usernameExists(
                        username
                    )
                ) {

                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            errorMessage =
                                "Username already exists."
                        )

                    return@launch
                }


                // ---------------------------------------------
                // SECURE PASSWORD CREDENTIALS
                // ---------------------------------------------

                val credentials =
                    PasswordSecurity.createCredentials(
                        password = password
                    )


                // ---------------------------------------------
                // CREATE ADMIN USER
                // ---------------------------------------------

                val admin =
                    UserEntity(
                        username = username,
                        displayName = displayName,
                        passwordHash =
                            credentials.passwordHash,
                        passwordSalt =
                            credentials.passwordSalt,
                        role = "ADMIN",
                        isActive = true
                    )


                userRepository.insertUser(
                    admin
                )


                // ---------------------------------------------
                // SUCCESS
                // ---------------------------------------------

                _uiState.value =
                    FirstAdminSetupUiState(
                        isCreatedSuccessfully = true
                    )


            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        errorMessage =
                            e.message
                                ?: "Unable to create administrator."
                    )
            }
        }
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
// VIEW MODEL FACTORY
// =============================================================

class FirstAdminSetupViewModelFactory(

    private val userRepository: UserRepository

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                FirstAdminSetupViewModel::class.java
            )
        ) {

            return FirstAdminSetupViewModel(
                userRepository = userRepository
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}