package com.vilync.ophthalmicerp.feature.designer.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores assets (logos, images) associated with templates.
 */
@Entity(
    tableName = "template_assets",
    indices = [
        Index(value = ["assetName"])
    ]
)
data class TemplateAssetEntity(
    @PrimaryKey
    val assetId: String, // UUID
    val assetName: String,
    val assetType: String, // "LOGO", "IMAGE", "SIGNATURE"
    val assetUri: String? = null,
    val assetPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
