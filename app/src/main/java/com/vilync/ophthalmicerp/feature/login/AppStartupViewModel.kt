package com.vilync.ophthalmicerp.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.security.PersistentSessionStore
import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
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

    data class RuntimeError(
        val message: String,
        val diagnostics: String,
        val report: DatabaseProvider.DatabaseHealthReport
    ) : StartupDestination()
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
        private const val APP_VERSION = "1.0.0 (Production Hardened)"
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
    // CHECK STARTUP DESTINATION (STATE MACHINE)
    // =========================================================

    private fun checkStartupDestination() {

        viewModelScope.launch {
            var finalDestination = "UNKNOWN"
            var healthReport = DatabaseProvider.DatabaseHealthReport(false, false, 0, 23)
            
            try {
                Log.d(TAG, "STATE: LOADING")

                // 1. STATE: DATABASE_HEALTH_CHECK (Includes OPEN & MIGRATION)
                // ---------------------------------------------
                Log.d(TAG, "STATE: DATABASE_HEALTH_CHECK")
                healthReport = DatabaseProvider.verifyDatabaseHealth(
                    context = persistentSessionStore.getContext()
                )
                Log.d(TAG, "Health Report: $healthReport")

                if (!healthReport.canOpen && healthReport.fileExists) {
                    throw Exception("Database exists but could not be opened: ${healthReport.errorMessage}")
                }

                if (healthReport.canOpen && healthReport.versionOnDisk != healthReport.expectedVersion) {
                    throw Exception("Migration Failure: Version mismatch (OnDisk: ${healthReport.versionOnDisk}, Expected: ${healthReport.expectedVersion})")
                }

                // 2. STATE: BUSINESS_VALIDATION
                // ---------------------------------------------
                Log.d(TAG, "STATE: BUSINESS_VALIDATION")
                if (healthReport.usersCount == 0 && healthReport.hasBusinessData) {
                    Log.e(TAG, "CRITICAL: Business data detected with NO admin user.")
                    val errorMsg = "Business data exists but no administrator account was found. System initialization has been blocked to prevent accidental data overwrite."
                    
                    _destination.value = StartupDestination.RuntimeError(
                        message = errorMsg,
                        diagnostics = "Users: 0, BusinessData: TRUE, Version: ${healthReport.versionOnDisk}",
                        report = healthReport
                    )
                    finalDestination = "RUNTIME_ERROR (Overwrite Protection)"
                    return@launch
                }

                // 3. STATE: USER_VALIDATION
                // ---------------------------------------------
                Log.d(TAG, "STATE: USER_VALIDATION")
                if (healthReport.usersCount == 0) {
                    Log.d(TAG, "Fresh Install / Empty State detected.")
                    _destination.value = StartupDestination.FirstAdminSetup
                    finalDestination = "FIRST_ADMIN_SETUP"
                    return@launch
                }

                // 4. STATE: FINAL_ROUTING (Session Check)
                // ---------------------------------------------
                Log.d(TAG, "STATE: FINAL_ROUTING")
                val rememberedUserId = persistentSessionStore.getUserId()
                
                if (rememberedUserId == null) {
                    _destination.value = StartupDestination.Login
                    finalDestination = "LOGIN"
                    return@launch
                }

                val rememberedUser = userRepository.getUserById(rememberedUserId)
                if (rememberedUser != null && rememberedUser.isActive) {
                    SessionManager.startSession(user = rememberedUser)
                    _destination.value = StartupDestination.Dashboard
                    finalDestination = "DASHBOARD"
                } else {
                    persistentSessionStore.clear()
                    SessionManager.clearSession()
                    _destination.value = StartupDestination.Login
                    finalDestination = "LOGIN (Session Expired)"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Startup State Machine FAILED at state", e)
                _destination.value = StartupDestination.RuntimeError(
                    message = "A critical error occurred during application bootstrap.",
                    diagnostics = e.message ?: "Unknown Error",
                    report = healthReport
                )
                finalDestination = "RUNTIME_ERROR (Exception)"
            } finally {
                // PERSIST DIAGNOSTIC LOG
                StartupLogger.logStartup(
                    context = persistentSessionStore.getContext(),
                    appVersion = APP_VERSION,
                    report = healthReport,
                    destination = finalDestination
                )
            }
        }
    }

    fun onFirstAdminCreated() {
        Log.d(TAG, "First Admin created successfully -> Login")
        persistentSessionStore.clear()
        SessionManager.clearSession()
        _destination.value = StartupDestination.Login
    }
}

class AppStartupViewModelFactory(
    private val userRepository: UserRepository,
    private val persistentSessionStore: PersistentSessionStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppStartupViewModel::class.java)) {
            return AppStartupViewModel(
                userRepository = userRepository,
                persistentSessionStore = persistentSessionStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
