package com.vilync.ophthalmicerp.core.document.engine.binding

import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates multiple DynamicFieldProviders to resolve all placeholders for a document.
 */
class PlaceholderResolver(
    private val providers: List<DynamicFieldProvider>
) {

    /**
     * Aggregates field data from all eligible providers.
     */
    suspend fun resolveAll(context: DocumentBindingContext): Map<String, BindingValue> = coroutineScope {
        val eligibleProviders = providers.filter { it.supportedTypes.contains(context.documentType) }
        
        val deferredResults = eligibleProviders.map { provider ->
            async {
                try {
                    provider.getFields(context)
                } catch (e: Exception) {
                    // Log error and return empty to prevent total failure
                    android.util.Log.e("PlaceholderResolver", "Provider ${provider.name} failed", e)
                    emptyMap<String, BindingValue>()
                }
            }
        }

        val results = deferredResults.awaitAll()
        val finalMap = mutableMapOf<String, BindingValue>()
        
        // Merge results, later providers override earlier ones if keys collide (standard priority)
        results.forEach { finalMap.putAll(it) }
        
        finalMap
    }
}
