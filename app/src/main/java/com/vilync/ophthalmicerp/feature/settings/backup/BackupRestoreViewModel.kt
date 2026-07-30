package com.vilync.ophthalmicerp.feature.settings.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val isLoading: Boolean = true,
    val isCreatingBackup: Boolean = false,
    val backups: List<BackupManager.BackupInfo> = emptyList(),
    val lastSuccessfulBackup: BackupManager.BackupInfo? = null,
    val message: String? = null,
    val isError: Boolean = false
)

class BackupRestoreViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val backupManager =
        BackupManager(
            context = application.applicationContext
        )

    private val _uiState =
        MutableStateFlow(
            BackupRestoreUiState()
        )

    val uiState: StateFlow<BackupRestoreUiState> =
        _uiState.asStateFlow()

    init {
        refreshBackupHistory()
    }

    fun createBackup() {

        if (_uiState.value.isCreatingBackup) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isCreatingBackup = true,
                    message = null,
                    isError = false
                )

            val result =
                backupManager.createBackup()

            if (result.success) {

                val history =
                    backupManager.getBackupHistory(
                        validateFiles = false
                    )

                _uiState.value =
                    BackupRestoreUiState(
                        isLoading = false,
                        isCreatingBackup = false,
                        backups = history,
                        lastSuccessfulBackup =
                            history.firstOrNull(),
                        message =
                            result.message,
                        isError = false
                    )

            } else {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isCreatingBackup = false,
                        message =
                            result.message,
                        isError = true
                    )
            }
        }
    }

    fun refreshBackupHistory(
        validateFiles: Boolean = false
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    message = null,
                    isError = false
                )

            runCatching {

                backupManager.getBackupHistory(
                    validateFiles = validateFiles
                )

            }.onSuccess { history ->

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        backups = history,
                        lastSuccessfulBackup =
                            history.firstOrNull(),
                        isError = false
                    )

            }.onFailure { throwable ->

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        message =
                            throwable.message
                                ?: "Backup history could not be loaded.",
                        isError = true
                    )
            }
        }
    }

    fun validateBackup(
        backup: BackupManager.BackupInfo
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    message = null,
                    isError = false
                )

            val valid =
                backupManager.validateBackup(
                    backup.file
                )

            val refreshedBackups =
                _uiState.value.backups.map { item ->

                    if (
                        item.file.absolutePath ==
                        backup.file.absolutePath
                    ) {
                        item.copy(
                            isValid = valid
                        )
                    } else {
                        item
                    }
                }

            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    backups = refreshedBackups,
                    lastSuccessfulBackup =
                        refreshedBackups.firstOrNull(),
                    message =
                        if (valid) {
                            "Backup integrity check passed."
                        } else {
                            "Backup integrity check failed."
                        },
                    isError = !valid
                )
        }
    }

    fun clearMessage() {

        _uiState.value =
            _uiState.value.copy(
                message = null,
                isError = false
            )
    }
}
