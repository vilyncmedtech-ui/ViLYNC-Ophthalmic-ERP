package com.vilync.ophthalmicerp.feature.login

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


    private val _destination =
        MutableStateFlow<StartupDestination>(
            StartupDestination.Loading
        )


    val destination:
            StateFlow<StartupDestination> =
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

                val hasAnyUser =
                    userRepository.hasAnyUser()


                if (!hasAnyUser) {

                    persistentSessionStore.clear()

                    SessionManager.clearSession()

                    _destination.value =
                        StartupDestination.FirstAdminSetup

                    return@launch
                }


                val rememberedUserId =
                    persistentSessionStore.getUserId()


                if (rememberedUserId == null) {

                    _destination.value =
                        StartupDestination.Login

                    return@launch
                }


                val rememberedUser =
                    userRepository.getUserById(
                        userId = rememberedUserId
                    )


                if (
                    rememberedUser != null &&
                    rememberedUser.isActive
                ) {

                    SessionManager.startSession(
                        user = rememberedUser
                    )

                    _destination.value =
                        StartupDestination.Dashboard

                } else {

                    persistentSessionStore.clear()

                    SessionManager.clearSession()

                    _destination.value =
                        StartupDestination.Login
                }

            } catch (_: Exception) {

                /*
                 * Fail closed.
                 *
                 * If the remembered session cannot be verified
                 * against Room, require normal login.
                 */

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
