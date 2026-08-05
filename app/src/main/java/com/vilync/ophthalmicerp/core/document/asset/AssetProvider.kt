package com.vilync.ophthalmicerp.core.document.asset

import java.io.InputStream

/**
 * Contract for resolving binary assets (logos, signatures) required by a template.
 */
interface AssetProvider {
    /**
     * Retrieves the asset as a stream. 
     * Implementation decides if source is local file, assets, or cloud.
     */
    suspend fun getAssetStream(assetId: String): InputStream?
}
