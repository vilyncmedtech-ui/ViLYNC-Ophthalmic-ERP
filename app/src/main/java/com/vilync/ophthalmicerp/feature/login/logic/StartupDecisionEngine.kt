package com.vilync.ophthalmicerp.feature.login.logic

import android.util.Log
import com.vilync.ophthalmicerp.feature.login.StartupDestination
import com.vilync.ophthalmicerp.feature.login.model.StartupFacts
import com.vilync.ophthalmicerp.feature.login.model.StartupFailure

/**
 * Stateless logic engine that implements the hardened startup decision table.
 */
object StartupDecisionEngine {

    private const val TAG = "STARTUP_DECISION"

    /**
     * Analyzes startup facts and determines the safe application destination.
     */
    fun decide(facts: StartupFacts): StartupDestination {
        Log.d(TAG, "Analyzing Pipeline Facts: $facts")

        // 1. INFRASTRUCTURE FAILURE
        // Includes: Database Open Error, Migration Failure, Thread Hangs
        if (!facts.infrastructureHealthy || facts.failure != null) {
            val failure = facts.failure ?: StartupFailure.InfrastructureFailure("Undefined health violation")
            Log.e(TAG, "DECISION: RUNTIME_ERROR (Infrastructure)")
            return StartupDestination.RuntimeError(
                message = failure.message,
                diagnostics = "Code: ${failure.code}, DiskVersion: ${facts.versionOnDisk}",
                report = failure
            )
        }

        // 2. ADMINISTRATOR DETECTION
        if (!facts.administratorExists) {
            if (facts.businessDataExists) {
                // DATA INTEGRITY PROTECTION: Business data found with no owner.
                Log.e(TAG, "DECISION: RUNTIME_ERROR (Overwrite Protection Triggered)")
                return StartupDestination.RuntimeError(
                    message = "Existing business data detected without an administrator account. Setup blocked to prevent data loss.",
                    diagnostics = "IntegrityViolation: ORPHAN_DATA",
                    report = StartupFailure.OverwriteProtectionError()
                )
            } else {
                // POSITIVE CONFIRMATION: Database is healthy, open, and genuinely empty.
                Log.d(TAG, "DECISION: FIRST_ADMIN_SETUP")
                return StartupDestination.FirstAdminSetup
            }
        }

        // 3. HEALTHY OPERATING STATE
        Log.d(TAG, "DECISION: NORMAL_OPERATION")
        return StartupDestination.Login
    }
}
