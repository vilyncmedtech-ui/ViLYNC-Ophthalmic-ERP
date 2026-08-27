package com.vilync.ophthalmicerp

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vilync.ophthalmicerp.data.database.DatabaseProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FindSafeContextTest {
    @Test
    fun findSafeContext() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseProvider.getDatabase(context)
        val inventoryDao = db.inventoryDao()
        val productDao = db.productDao()
        
        Log.i("TEST_CONTEXT", "Searching for safe single-unit test context...")
        
        // Get all in-stock units
        val inStockUnits = inventoryDao.getInStockUnits().first()
        
        // Group by (productId, power)
        val grouped = inStockUnits.groupBy { it.productId to it.power }
        
        // Find a group with exactly one unit, and that unit ID is not 2 or 3
        val safeContext = grouped.entries.find { (key, units) ->
            units.size == 1 && units[0].id != 2L && units[0].id != 3L
        }
        
        if (safeContext != null) {
            val unit = safeContext.value[0]
            val product = productDao.getProductById(unit.productId)
            
            Log.i("TEST_CONTEXT", "RESULT: SUCCESS")
            Log.i("TEST_CONTEXT", "Product Name: ${product?.productName}")
            Log.i("TEST_CONTEXT", "Power: ${unit.power}")
            Log.i("TEST_CONTEXT", "Serial Number: ${unit.serialNumber}")
            Log.i("TEST_CONTEXT", "Unit ID: ${unit.id}")
            Log.i("TEST_CONTEXT", "In-Stock Count: ${safeContext.value.size}")
        } else {
            Log.i("TEST_CONTEXT", "RESULT: NO SAFE SINGLE-UNIT TEST CONTEXT AVAILABLE")
        }
    }
}
