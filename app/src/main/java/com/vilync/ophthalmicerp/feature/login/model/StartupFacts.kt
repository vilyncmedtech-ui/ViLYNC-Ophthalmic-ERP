package com.vilync.ophthalmicerp.feature.login.model

/**
 * Immutable semantic facts about the application state during startup.
 * Evaluated by StartupDecisionEngine to determine navigation.
 */
data class StartupFacts(
    val fileExists: Boolean,
    val canOpen: Boolean,
    val versionOnDisk: Int,
    val expectedVersion: Int,
    val schemaHealthy: Boolean,
    val migrationsCompleted: Boolean,
    val administratorExists: Boolean,
    val businessDataExists: Boolean,
    val infrastructureHealthy: Boolean,
    val failure: StartupFailure? = null
)
