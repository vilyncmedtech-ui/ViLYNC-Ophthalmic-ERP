package com.vilync.ophthalmicerp.feature.backup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.backup.data.BackupMetadataEntity
import com.vilync.ophthalmicerp.feature.backup.data.BackupRepository
import com.vilync.ophthalmicerp.feature.backup.domain.BackupUseCase
import com.vilync.ophthalmicerp.feature.backup.logic.GoogleAuthManager
import com.vilync.ophthalmicerp.feature.backup.model.GoogleAuthState
import com.vilync.ophthalmicerp.feature.backup.logic.BackupIntegrityVerifier
import com.vilync.ophthalmicerp.feature.backup.logic.BackupSettingsManager
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Intent

sealed class BackupInitializationState {
    data object Idle : BackupInitializationState()
    data object Initializing : BackupInitializationState()
    data object Ready : BackupInitializationState()
    data class Failed(val message: String) : BackupInitializationState()
}

data class GoogleBackupUiState(
    val authState: GoogleAuthState = GoogleAuthState.Unauthenticated,
    val initializationState: BackupInitializationState = BackupInitializationState.Idle,
    val lastSuccess: BackupMetadataEntity? = null,
    val lastFailed: BackupMetadataEntity? = null,
    val cloudBackups: List<BackupMetadataEntity> = emptyList(),
    val isOperationInProgress: Boolean = false,
    val statusMessage: String? = null,
    val databaseVersion: Int = 28,
    val restoreSummary: BackupIntegrityVerifier.RestoreSummary? = null,
    val selectedBackupForRestore: BackupMetadataEntity? = null,
    val showRollbackMessage: Boolean = false,
    val backupFolderName: String = "ViLYNC ERP Backup",
    val pendingIntent: android.content.Intent? = null
)

class GoogleBackupViewModel(
    private val authManager: GoogleAuthManager,
    private val backupUseCase: BackupUseCase,
    private val backupRepository: BackupRepository,
    private val settingsManager: BackupSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleBackupUiState())
    val uiState: StateFlow<GoogleBackupUiState> = _uiState.asStateFlow()

    // INDEPENDENT SCOPE: To survive the Session-Guard Trap during Restore
    private val restoreScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    init {
        // Observe Authentication State
        authManager.authState
            .onEach { state ->
                _uiState.update { it.copy(authState = state) }
                when (state) {
                    is GoogleAuthState.Authenticated -> {
                        // When authenticated, trigger Drive Service initialization
                        initializeCloudService(state.email)
                    }
                    is GoogleAuthState.Unauthenticated -> {
                        _uiState.update { it.copy(initializationState = BackupInitializationState.Idle, cloudBackups = emptyList()) }
                        backupUseCase.resetSession()
                    }
                    is GoogleAuthState.Error -> {
                        _uiState.update { it.copy(statusMessage = state.message, initializationState = BackupInitializationState.Failed(state.message)) }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)

        // Attempt to restore existing session
        viewModelScope.launch {
            authManager.checkExistingSession()
        }

        // Load Latest Backup Info from Local Database
        // Note: cloudBackups list fetched from Drive is now the primary source for 'Last Success'
        // to ensure the status card and the list are always in sync.
        backupRepository.getBackupHistory()
            .catch { e ->
                // HARDENING: Prevent database closure from cancelling the entire scope
                android.util.Log.e("BackupViewModel", "RESTORE_CANCELLATION_TRACE: Backup history flow failed!", e)
            }
            .onEach { history ->
                _uiState.update { state ->
                    // Derive Last Success/Failed from the cloud backups list if available, 
                    // otherwise fallback to local history.
                    val latestCloudSuccess = state.cloudBackups.firstOrNull { it.status == "COMPLETED" }
                    val latestLocalSuccess = history.firstOrNull { it.status == "COMPLETED" }
                    
                    val effectiveSuccess = latestCloudSuccess ?: latestLocalSuccess
                    val effectiveFailed = history.firstOrNull { it.status == "FAILED" }

                    state.copy(
                        lastSuccess = effectiveSuccess,
                        lastFailed = effectiveFailed,
                        cloudBackups = if (state.cloudBackups.isEmpty()) history else state.cloudBackups
                    )
                }
            }
            .launchIn(viewModelScope)

        // Check for rollback recovery message
        if (settingsManager.didRollbackOccur()) {
            _uiState.update { it.copy(showRollbackMessage = true) }
            settingsManager.setRollbackOccurred(false)
        }
    }

    private fun initializeCloudService(email: String) {
        if (_uiState.value.initializationState == BackupInitializationState.Initializing) return

        viewModelScope.launch {
            _uiState.update { it.copy(initializationState = BackupInitializationState.Initializing, statusMessage = "Checking account access...") }
            
            backupUseCase.initializeDriveService(email)
                .onSuccess {
                    _uiState.update { it.copy(initializationState = BackupInitializationState.Ready, statusMessage = null) }
                    loadCloudBackups() // Only load backups once service is ready
                }
                .onFailure { e ->
                    handleCloudError("Initialization Failed", e)
                }
        }
    }

    private fun handleCloudError(prefix: String, e: Throwable) {
        android.util.Log.e("BackupViewModel", "CLOUD_ERROR [$prefix]", e)
        val exceptionName = e.javaClass.name
        val cause = e.cause
        val causeName = cause?.javaClass?.name ?: ""

        // 1. Detect Coroutine Cancellation (usually triggered by a deeper failure)
        if (e is kotlinx.coroutines.CancellationException && cause != null) {
            handleCloudError(prefix, cause)
            return
        }

        // 2. Detect User Recoverable Auth Issues (Common in modern Android for OAuth scopes)
        if (exceptionName.contains("UserRecoverableAuth") || causeName.contains("UserRecoverableAuth")) {
            try {
                // Use reflection to get Intent to avoid heavy dependency on legacy auth libs in UI code
                val target = if (exceptionName.contains("UserRecoverableAuth")) e else cause!!
                val intentMethod = target.javaClass.getMethod("getIntent")
                val intent = intentMethod.invoke(target) as? android.content.Intent
                if (intent != null) {
                    _uiState.update { 
                        it.copy(
                            isOperationInProgress = false,
                            initializationState = BackupInitializationState.Idle,
                            pendingIntent = intent,
                            statusMessage = "$prefix: Google Account access required."
                        ) 
                    }
                    return
                }
            } catch (ex: Exception) {
                android.util.Log.e("BackupViewModel", "Failed to extract recovery intent", ex)
            }
        }

        // 3. Generic Error Handling
        val displayMessage = when {
            e.message != null && !e.message!!.contains("Job was cancelled") -> e.message
            cause?.message != null -> cause.message
            exceptionName.contains("IOException") -> "Network or Drive API error."
            else -> "Operation failed: ${e.javaClass.simpleName}"
        }

        _uiState.update { 
            it.copy(
                isOperationInProgress = false,
                initializationState = if (prefix.contains("Init")) BackupInitializationState.Failed(displayMessage!!) else it.initializationState,
                statusMessage = "$prefix: $displayMessage"
            ) 
        }
    }

    /**
     * Resets the pending intent after it has been consumed by the UI.
     */
    fun consumePendingIntent() {
        _uiState.update { it.copy(pendingIntent = null) }
    }

    fun signIn(context: android.content.Context) {
        viewModelScope.launch {
            authManager.signIn(context)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            backupUseCase.resetSession()
            authManager.signOut()
        }
    }

    fun loadCloudBackups() {
        val auth = _uiState.value.authState
        if (auth is GoogleAuthState.Authenticated) {
            if (_uiState.value.initializationState != BackupInitializationState.Ready) {
                initializeCloudService(auth.email)
                return
            }
        } else {
            return
        }

        viewModelScope.launch {
            backupUseCase.listBackups()
                .onSuccess { backups ->
                    _uiState.update { state ->
                        val latestSuccess = backups.firstOrNull { it.status == "COMPLETED" }
                        state.copy(
                            cloudBackups = backups, 
                            lastSuccess = latestSuccess ?: state.lastSuccess,
                            statusMessage = null
                        ) 
                    }
                }
                .onFailure { e ->
                    handleCloudError("Listing Failed", e)
                }
        }
    }

    fun performBackupNow() {
        if (_uiState.value.isOperationInProgress) return
        if (_uiState.value.initializationState != BackupInitializationState.Ready) {
            _uiState.update { it.copy(statusMessage = "Cloud service not ready. Please wait.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Initializing Backup...") }
            
            try {
                val user = authManager.getAuthenticatedUser()
                if (user == null) {
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Error: Not signed in.") }
                    return@launch
                }

                backupUseCase.performBackupWorkflow()
                    .onSuccess { record ->
                        _uiState.update { it.copy(statusMessage = "Uploading to Google Drive...") }
                        backupUseCase.uploadBackup(record.id)
                            .onSuccess {
                                _uiState.update { it.copy(statusMessage = "Backup Completed Successfully.") }
                                loadCloudBackups() // Refresh list
                            }
                            .onFailure { e ->
                                handleCloudError("Upload Failed", e)
                            }
                    }
                    .onFailure { e ->
                        handleCloudError("Backup Failed", e)
                    }
            } catch (e: Exception) {
                handleCloudError("Backup Crash", e)
            } finally {
                _uiState.update { it.copy(isOperationInProgress = false) }
            }
        }
    }

    fun prepareRestore(metadata: BackupMetadataEntity) {
        if (_uiState.value.isOperationInProgress) return
        if (_uiState.value.initializationState != BackupInitializationState.Ready) {
            _uiState.update { it.copy(statusMessage = "Cloud service not ready.") }
            return
        }
        
        val driveFileId = metadata.driveFileId ?: return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isOperationInProgress = true, 
                    statusMessage = "Downloading for verification...",
                    selectedBackupForRestore = metadata
                ) 
            }
            try {
                backupUseCase.prepareRestoreSummary(driveFileId)
                    .onSuccess { summary ->
                        _uiState.update { it.copy(isOperationInProgress = false, restoreSummary = summary, statusMessage = null) }
                    }
                    .onFailure { e ->
                        handleCloudError("Verification Failed", e)
                    }
            } catch (e: Exception) {
                handleCloudError("Verification Crash", e)
            } finally {
                _uiState.update { it.copy(isOperationInProgress = false) }
            }
        }
    }

    fun executeRestore(metadata: BackupMetadataEntity, onRestart: () -> Unit) {
        if (_uiState.value.initializationState != BackupInitializationState.Ready) return

        // Use independent restoreScope to survive Session-Guard navigation
        restoreScope.launch {
            val restoreJob = coroutineContext[kotlinx.coroutines.Job]
            android.util.Log.i("BackupViewModel", "EXECUTE_RESTORE_START: Job=$restoreJob | DriveID=${metadata.driveFileId}")
            
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Performing atomic restore...") }
            try {
                // Ensure we use a direct result capture to prevent unhandled exceptions from cancelling the job
                val result = backupUseCase.executeRestore(metadata)
                
                if (result.isSuccess) {
                    // HANDOFF PROTECTION: Success is confirmed. App MUST reach restart.
                    val summary = _uiState.value.restoreSummary
                    android.util.Log.i("BackupViewModel", "RESTORE_SUCCESS_READY_FOR_RESTART | DriveID=${metadata.driveFileId} | Size=${metadata.fileSize} | Users=${summary?.usersCount} | Products=${summary?.productsCount} | Sales=${summary?.salesCount} | Purchases=${summary?.purchasesCount} | Verification=SUCCESS")
                    
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Restore successful. Restarting...") }
                    onRestart()
                } else {
                    val error = result.exceptionOrNull()
                    android.util.Log.e("BackupViewModel", "RESTORE_FAILURE_REPORTED: ${error?.message} | JobState=${restoreJob?.isActive}")
                    handleCloudError("Restore Failed", error ?: Exception("Unknown restore error"))
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "EXECUTE_RESTORE_CRASH: Catch block reached | JobState=${restoreJob?.isActive}", e)
                handleCloudError("Restore Crash", e)
            } finally {
                android.util.Log.i("BackupViewModel", "EXECUTE_RESTORE_FINISH: Operation flag reset for ID=${metadata.driveFileId}")
                _uiState.update { it.copy(isOperationInProgress = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // DO NOT cancel restoreScope here. It must survive ViewModel destruction 
        // to complete the processExit/restart handoff.
    }

    fun dismissSummary() {
        _uiState.update { it.copy(restoreSummary = null) }
    }

    fun dismissRollbackMessage() {
        _uiState.update { it.copy(showRollbackMessage = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

class GoogleBackupViewModelFactory(
    private val authManager: GoogleAuthManager,
    private val backupUseCase: BackupUseCase,
    private val backupRepository: BackupRepository,
    private val settingsManager: BackupSettingsManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoogleBackupViewModel(authManager, backupUseCase, backupRepository, settingsManager) as T
    }
}
