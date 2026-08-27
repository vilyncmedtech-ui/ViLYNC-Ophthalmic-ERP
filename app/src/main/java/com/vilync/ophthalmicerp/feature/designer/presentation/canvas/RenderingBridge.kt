package com.vilync.ophthalmicerp.feature.designer.presentation.canvas

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import com.vilync.ophthalmicerp.core.document.engine.PreviewRenderResult
import com.vilync.ophthalmicerp.core.document.engine.DefaultPreviewRenderer
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache for pre-rendered page bitmaps.
 */
class PreviewCache {
    private val bitmaps = ConcurrentHashMap<Int, ImageBitmap>()

    fun get(pageIndex: Int): ImageBitmap? = bitmaps[pageIndex]

    fun put(pageIndex: Int, bitmap: ImageBitmap) {
        bitmaps[pageIndex] = bitmap
    }

    fun invalidate() {
        bitmaps.clear()
    }
}

/**
 * Orchestrates background rendering of document pages to prevent UI thread blocking.
 */
class RenderingBridge(
    private val previewRenderer: DefaultPreviewRenderer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val cache = PreviewCache()
    private var lastResult: PreviewRenderResult? = null

    /**
     * Updates the rendering intent and invalidates cache if document has changed.
     */
    fun updateResult(result: PreviewRenderResult?) {
        if (result != lastResult) {
            cache.invalidate()
            lastResult = result
        }
    }

    /**
     * Retrieves a cached page or triggers a background render.
     */
    fun getPage(pageIndex: Int, dpi: Int): ImageBitmap? {
        val cached = cache.get(pageIndex)
        if (cached != null) return cached

        val result = lastResult ?: return null
        val dimensions = result.pageDimensions[pageIndex] ?: return null

        // Trigger background render
        scope.launch {
            renderPageToCache(pageIndex, result, dimensions, dpi)
        }

        return null // Return null to show placeholder while rendering
    }

    private suspend fun renderPageToCache(
        pageIndex: Int,
        result: PreviewRenderResult,
        dimMm: Dimensions,
        dpi: Int
    ) {
        // Calculate pixel size for the bitmap
        val mmToInch = 1.0 / 25.4
        val widthPx = (dimMm.width * mmToInch * dpi).toInt()
        val heightPx = (dimMm.height * mmToInch * dpi).toInt()

        if (widthPx <= 0 || heightPx <= 0) return

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Execute renderer on background thread
        val success = result.renderPage(
            pageIndex, 
            DefaultPreviewRenderer.CanvasPreviewOutput(canvas)
        )

        if (success) {
            cache.put(pageIndex, bitmap.asImageBitmap())
        }
    }

    fun dispose() {
        scope.cancel()
        cache.invalidate()
    }
}
