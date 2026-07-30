package com.vilync.ophthalmicerp.feature.purchase.model

/*
 * Represents one physical IOL.
 *
 * One physical IOL =
 * One unique Serial Number + One Expiry
 *
 * Expiry format: MMYY
 *
 * Example:
 * SN001 | 1229
 */
data class IolLensDetail(
    val serialNumber: String = "",
    val expiryDate: String = ""
)