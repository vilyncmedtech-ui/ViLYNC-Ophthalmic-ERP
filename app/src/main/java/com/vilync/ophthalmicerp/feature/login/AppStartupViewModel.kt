package com.vilync.ophthalmicerp.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.security.PersistentSessionStore
import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =============================================================
// STARTUP DESTINATION
// =============================================================

sealed class StartupDestination {

    data object Loading :
        StartupDestination()

    data object FirstAdminSetup :
        StartupDestination()

    data object Login :
        StartupDestination()

    data object Dashboard :
        StartupDestination()
}

// =============================================================
// APP STARTUP VIEWMODEL
// =============================================================

class AppStartupViewModel(

    private val userRepository: UserRepository,

    private val persistentSessionStore: PersistentSessionStore

) : ViewModel() {

    companion object {
        private const val TAG = "STARTUP_CHECK"
    }

    private val _destination =
        MutableStateFlow<StartupDestination>(
            StartupDestination.Loading
        )

    val destination: StateFlow<StartupDestination> =
        _destination.asStateFlow()

    init {
        checkStartupDestination()
    }

    // =========================================================
    // CHECK STARTUP DESTINATION
    // =========================================================

    private fun checkStartupDestination() {

        viewModelScope.launch {

            try {

                Log.d(TAG, "========================================")
                Log.d(TAG, "Startup verification started")

                val hasAnyUser =
                    userRepository.hasAnyUser()

                Log.d(TAG, "hasAnyUser = $hasAnyUser")

                val userCount =
                    userRepository.getUserCount()

                Log.d(TAG, "userCount = $userCount")

                if (!hasAnyUser) {

                    Log.d(
                        TAG,
                        "No ERP user found -> Opening FirstAdminSetup"
                    )

                    persistentSessionStore.clear()

                    SessionManager.clearSession()

                    _destination.value =
                        StartupDestination.FirstAdminSetup

                    return@launch
                }

                val rememberedUserId =
                    persistentSessionStore.getUserId()

                Log.d(
                    TAG,
                    "rememberedUserId = $rememberedUserId"
                )

                if (rememberedUserId == null) {

                    Log.d(
                        TAG,
                        "No remembered session -> Opening Login"
                    )

                    _destination.value =
                        StartupDestination.Login

                    return@launch
                }

                val rememberedUser =
                    userRepository.getUserById(
                        userId = rememberedUserId
                    )

                Log.d(
                    TAG,
                    "rememberedUser = $rememberedUser"
                )

                if (
                    rememberedUser != null &&
                    rememberedUser.isActive
                ) {

                    Log.d(
                        TAG,
                        "Valid active session -> Opening Dashboard"
                    )

                    SessionManager.startSession(
                        user = rememberedUser
                    )

                    _destination.value =
                        StartupDestination.Dashboard

                } else {

                    Log.d(
                        TAG,
                        "Remembered user invalid/inactive -> Opening Login"
                    )

                    persistentSessionStore.clear()

                    SessionManager.clearSession()

                    _destination.value =
                        StartupDestination.Login
                }

                Log.d(TAG, "Startup verification completed")
                Log.d(TAG, "========================================")

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Startup verification FAILED",
                    e
                )

                SessionManager.clearSession()

                _destination.value =
                    StartupDestination.Login
            }
        }
    }

    // =========================================================
    // ADMIN CREATED
    // =========================================================

    fun onFirstAdminCreated() {

        Log.d(
            TAG,
            "First Admin created successfully -> Login"
        )

        persistentSessionStore.clear()

        SessionManager.clearSession()

        _destination.value =
            StartupDestination.Login
    }
}

// =============================================================
// VIEWMODEL FACTORY
// =============================================================

class AppStartupViewModelFactory(

    private val userRepository: UserRepository,

    private val persistentSessionStore: PersistentSessionStore

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                AppStartupViewModel::class.java
            )
        ) {

            return AppStartupViewModel(
                userRepository =
                    userRepository,

                persistentSessionStore =
                    persistentSessionStore
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}