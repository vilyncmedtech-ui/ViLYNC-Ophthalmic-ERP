package com.vilync.ophthalmicerp

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryQueryTest {
    @Test
    fun querySerial() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseProvider.getDatabase(context)
        val dao = db.inventoryDao()
        
        Log.i("QUERY_FACTS", "Starting query for 252279")
        
        // Try search by suffix
        val units = dao.findBySerialSuffix("252279")
        Log.i("QUERY_FACTS", "Units found by suffix: ${units.size}")
        units.forEach { unit ->
            val sn = unit.serialNumber
            Log.i("QUERY_FACTS", "FACT_VALUE: '$sn'")
            Log.i("QUERY_FACTS", "FACT_LENGTH: ${sn.length}")
            Log.i("QUERY_FACTS", "FACT_STARTS_WITH_LMDE: ${sn.startsWith("LMDE")}")
        }
        
        // Try search by ID just in case
        val unitById = dao.getById(252279L)
        if (unitById != null) {
            Log.i("QUERY_FACTS", "Unit found by ID 252279")
            val sn = unitById.serialNumber
            Log.i("QUERY_FACTS", "FACT_VALUE_ID: '$sn'")
            Log.i("QUERY_FACTS", "FACT_LENGTH_ID: ${sn.length}")
            Log.i("QUERY_FACTS", "FACT_STARTS_WITH_LMDE_ID: ${sn.startsWith("LMDE")}")
        }
    }
}
