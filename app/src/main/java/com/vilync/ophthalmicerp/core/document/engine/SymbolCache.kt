package com.vilync.ophthalmicerp.core.document.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe global cache for encoded symbols (Barcodes/QR).
 * Prevents redundant encoding operations during multi-page document generation.
 */
object SymbolCache {
    private val cache = ConcurrentHashMap<String, EncodedSymbol>()
    
    // Statistics
    private var hits = 0
    private var misses = 0
    private var encodedCount = 0

    private const val MAX_ENTRIES = 500

    /**
     * Retrieves a cached symbol.
     */
    fun get(type: String, format: String, content: String): EncodedSymbol? {
        val key = createKey(type, format, content)
        val symbol = cache[key]
        if (symbol != null) {
            hits++
        } else {
            misses++
        }
        return symbol
    }

    /**
     * Caches a newly encoded symbol.
     */
    fun put(type: String, format: String, content: String, symbol: EncodedSymbol) {
        // Simple safety: clear if limit reached to prevent memory leak
        if (cache.size >= MAX_ENTRIES) {
            cache.clear()
        }
        
        val key = createKey(type, format, content)
        if (cache.putIfAbsent(key, symbol) == null) {
            encodedCount++
        }
    }

    private fun createKey(type: String, format: String, content: String): String {
        return "$type:$format:$content"
    }

    /**
     * Resets statistics for a new measurement session.
     */
    fun resetStats() {
        hits = 0
        misses = 0
        encodedCount = 0
    }

    /**
     * Returns a snapshot of cache performance.
     */
    fun getMetrics(): Map<String, Any> {
        val total = hits + misses
        val ratio = if (total > 0) hits.toDouble() / total else 0.0
        return mapOf(
            "cacheHits" to hits,
            "cacheMisses" to misses,
            "symbolsEncoded" to encodedCount,
            "symbolsReused" to hits,
            "hitRatio" to ratio,
            "currentSize" to cache.size
        )
    }

    /**
     * Clears all cached symbols.
     */
    fun clear() {
        cache.clear()
        resetStats()
    }
}
