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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoogleBackupUiState(
    val authState: GoogleAuthState = GoogleAuthState.Unauthenticated,
    val lastSuccess: BackupMetadataEntity? = null,
    val lastFailed: BackupMetadataEntity? = null,
    val cloudBackups: List<BackupMetadataEntity> = emptyList(),
    val isOperationInProgress: Boolean = false,
    val statusMessage: String? = null,
    val databaseVersion: Int = 25,
    val restoreSummary: BackupIntegrityVerifier.RestoreSummary? = null,
    val showRollbackMessage: Boolean = false,
    val backupFolderName: String = "ViLYNC ERP Backup"
)

class GoogleBackupViewModel(
    private val authManager: GoogleAuthManager,
    private val backupUseCase: BackupUseCase,
    private val backupRepository: BackupRepository,
    private val settingsManager: BackupSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleBackupUiState())
    val uiState: StateFlow<GoogleBackupUiState> = _uiState.asStateFlow()

    init {
        // Observe Authentication State
        authManager.authState
            .onEach { state ->
                _uiState.update { it.copy(authState = state) }
                when (state) {
                    is GoogleAuthState.Authenticated -> {
                        loadCloudBackups()
                    }
                    is GoogleAuthState.Error -> {
                        _uiState.update { it.copy(statusMessage = state.message) }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)

        // Attempt to restore existing session
        viewModelScope.launch {
            authManager.checkExistingSession()
        }

        // Load Latest Backup Info
        backupRepository.getBackupHistory()
            .onEach { history ->
                val success = history.firstOrNull { it.status == "COMPLETED" }
                val failed = history.firstOrNull { it.status == "FAILED" }
                _uiState.update { 
                    it.copy(
                        lastSuccess = success,
                        lastFailed = failed
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

    fun signIn(context: android.content.Context) {
        viewModelScope.launch {
            authManager.signIn(context)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun loadCloudBackups() {
        viewModelScope.launch {
            backupUseCase.listBackups().onSuccess { backups ->
                _uiState.update { it.copy(cloudBackups = backups) }
            }
        }
    }

    fun performBackupNow() {
        if (_uiState.value.isOperationInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Initializing Backup...") }
            
            val user = authManager.getAuthenticatedUser()
            if (user == null) {
                _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Error: Not signed in.") }
                return@launch
            }

            backupUseCase.initializeDriveService(user.email)
            
            backupUseCase.performBackupWorkflow()
                .onSuccess { record ->
                    _uiState.update { it.copy(statusMessage = "Uploading to Google Drive...") }
                    backupUseCase.uploadBackup(record.id)
                        .onSuccess {
                            _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Backup Completed Successfully.") }
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Upload Failed: ${e.message}") }
                        }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Backup Failed: ${e.message}") }
                }
        }
    }

    fun prepareRestore(driveFileId: String) {
        if (_uiState.value.isOperationInProgress) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Downloading for verification...") }
            backupUseCase.prepareRestoreSummary(driveFileId)
                .onSuccess { summary ->
                    _uiState.update { it.copy(isOperationInProgress = false, restoreSummary = summary, statusMessage = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Verification Failed: ${e.message}") }
                }
        }
    }

    fun executeRestore(metadata: BackupMetadataEntity, onRestart: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Performing atomic restore...") }
            backupUseCase.executeRestore(metadata)
                .onSuccess {
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Restore successful. Restarting...") }
                    onRestart()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Restore Failed: ${e.message}") }
                }
        }
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
