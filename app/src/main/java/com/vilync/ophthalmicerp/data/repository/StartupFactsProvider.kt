package com.vilync.ophthalmicerp.data.repository

import android.util.Log
import com.vilync.ophthalmicerp.data.database.AppDatabase
import com.vilync.ophthalmicerp.feature.login.model.StartupFailure

/**
 * Executes business-level fact finding using verified Room DAOs.
 * Implements strict error propagation for production safety.
 */
class StartupFactsProvider(
    private val db: AppDatabase
) {
    companion object {
        private const val TAG = "STARTUP_FACTS"
    }

    /**
     * Positively confirms if an administrator exists.
     */
    suspend fun hasAdministrator(): Boolean {
        return try {
            Log.d(TAG, "Querying User Count...")
            db.userDao().getUserCount() > 0
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: UserDao count failed", e)
            throw StartupFailure.DaoFailure("UserDao", e)
        }
    }

    /**
     * Evaluates the existence of data across multiple business modules.
     */
    suspend fun hasAnyBusinessData(): Boolean {
        return try {
            Log.d(TAG, "Querying Business Data Existence...")
            val productCount = db.productDao().getProductCount()
            val partyCount = db.partyDao().getPartyCount()
            val saleCount = db.salesDao().getSalesCount()
            
            val hasData = productCount > 0 || partyCount > 0 || saleCount > 0
            Log.d(TAG, "Business Data Fact: $hasData (Products: $productCount, Parties: $partyCount, Sales: $saleCount)")
            hasData
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Business data fact finding failed", e)
            throw StartupFailure.DaoFailure("BusinessFacts", e)
        }
    }
}
