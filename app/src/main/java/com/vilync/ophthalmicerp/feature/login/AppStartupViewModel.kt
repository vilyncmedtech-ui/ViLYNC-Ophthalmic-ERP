package com.vilync.ophthalmicerp.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.security.PersistentSessionStore
import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.data.repository.StartupRepository
import com.vilync.ophthalmicerp.data.repository.UserRepository
import com.vilync.ophthalmicerp.feature.login.logic.StartupDecisionEngine
import com.vilync.ophthalmicerp.feature.login.model.StartupFacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =============================================================
// STARTUP DESTINATION
// =============================================================

sealed class StartupDestination {

    data object Loading : StartupDestination()
    data object FirstAdminSetup : StartupDestination()
    data object Login : StartupDestination()
    data object Dashboard : StartupDestination()

    data class RuntimeError(
        val message: String,
        val diagnostics: String,
        val report: Any
    ) : StartupDestination()
}

// =============================================================
// APP STARTUP VIEWMODEL
// =============================================================

class AppStartupViewModel(
    private val userRepository: UserRepository,
    private val startupRepository: StartupRepository,
    private val persistentSessionStore: PersistentSessionStore
) : ViewModel() {

    companion object {
        private const val TAG = "STARTUP_CHECK"
        private const val APP_VERSION = "1.0.0 (Hardened Architecture)"
    }

    private val _destination = MutableStateFlow<StartupDestination>(StartupDestination.Loading)
    val destination: StateFlow<StartupDestination> = _destination.asStateFlow()

    init {
        runHardenedStartupPipeline()
    }

    private fun runHardenedStartupPipeline() {
        viewModelScope.launch {
            Log.d(TAG, "Starting Hardened Application Bootstrap...")
            
            // 1. COLLECT FACTS (FAIL-CLOSED)
            val facts = startupRepository.getStartupFacts()
            
            // 2. EVALUATE NAVIGATION
            val target = StartupDecisionEngine.decide(facts)
            
            // 3. RESOLVE FINAL ROUTE
            if (target == StartupDestination.Login) {
                executeSessionRouting()
            } else {
                _destination.value = target
            }

            // 4. PERSIST DIAGNOSTICS
            StartupLogger.logStartup(
                context = persistentSessionStore.getContext(),
                appVersion = APP_VERSION,
                report = facts,
                destination = _destination.value.toString()
            )
        }
    }

    private suspend fun executeSessionRouting() {
        Log.d(TAG, "Evaluating Persistent Session...")
        val rememberedUserId = persistentSessionStore.getUserId()
        
        if (rememberedUserId == null) {
            _destination.value = StartupDestination.Login
            return
        }

        try {
            val rememberedUser = userRepository.getUserById(rememberedUserId)
            if (rememberedUser != null && rememberedUser.isActive) {
                SessionManager.startSession(user = rememberedUser)
                _destination.value = StartupDestination.Dashboard
            } else {
                persistentSessionStore.clear()
                SessionManager.clearSession()
                _destination.value = StartupDestination.Login
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session restoration CRITICAL FAILURE", e)
            _destination.value = StartupDestination.Login
        }
    }

    fun onFirstAdminCreated() {
        Log.d(TAG, "Bootstrap: First Admin confirmed -> Login")
        persistentSessionStore.clear()
        SessionManager.clearSession()
        _destination.value = StartupDestination.Login
    }
}

class AppStartupViewModelFactory(
    private val userRepository: UserRepository,
    private val startupRepository: StartupRepository,
    private val persistentSessionStore: PersistentSessionStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppStartupViewModel::class.java)) {
            return AppStartupViewModel(
                userRepository = userRepository,
                startupRepository = startupRepository,
                persistentSessionStore = persistentSessionStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
