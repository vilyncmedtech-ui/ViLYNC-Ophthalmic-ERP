package com.vilync.ophthalmicerp.data.repository

import android.content.Context
import android.util.Log
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import com.vilync.ophthalmicerp.feature.login.model.StartupFacts
import com.vilync.ophthalmicerp.feature.login.model.StartupFailure

/**
 * Enterprise gateway for application bootstrap state.
 * Aggregates infrastructure and business facts into a single immutable model.
 */
class StartupRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "STARTUP_REPO"
    }

    /**
     * Performs a full diagnostic check of the application state.
     * Strictly fail-closed: any exception prevents setup from being shown.
     */
    suspend fun getStartupFacts(): StartupFacts {
        // 1. INFRASTRUCTURE CHECK
        val infraFacts = DatabaseProvider.checkInfrastructure(context)
        
        if (!infraFacts.infrastructureHealthy || infraFacts.failure != null) {
            return infraFacts
        }

        // Fresh install (no file) implies no users and no business data
        if (!infraFacts.fileExists) {
            return infraFacts.copy(
                administratorExists = false,
                businessDataExists = false
            )
        }

        // 2. BUSINESS FACT COLLECTION (DAO DRIVEN)
        return try {
            Log.d(TAG, "Initializing Business Fact Provider...")
            val db = DatabaseProvider.getDatabase(context)
            val provider = StartupFactsProvider(db)
            
            val hasAdmin = provider.hasAdministrator()
            val hasBusinessData = provider.hasAnyBusinessData()
            
            infraFacts.copy(
                administratorExists = hasAdmin,
                businessDataExists = hasBusinessData
            )
        } catch (e: StartupFailure) {
            Log.e(TAG, "Startup fact collection failed with structured error", e)
            infraFacts.copy(
                infrastructureHealthy = false,
                failure = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during fact collection", e)
            infraFacts.copy(
                infrastructureHealthy = false,
                failure = StartupFailure.UnknownFailure(e)
            )
        }
    }
}
