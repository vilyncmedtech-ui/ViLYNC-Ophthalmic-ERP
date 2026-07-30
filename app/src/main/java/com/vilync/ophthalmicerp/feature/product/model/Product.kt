package com.vilync.ophthalmicerp.feature.product.model

data class Product(

    val id: Long = 0,

    val productName: String = "",

    val brandName: String = "",

    val model: String = "",

    val category: ProductCategory = ProductCategory.OTHER,

    val trackingType: TrackingType = TrackingType.QUANTITY,

    val powerApplicable: Boolean = false,

    val batchApplicable: Boolean = false,

    val expiryApplicable: Boolean = false,

    val serialNumberRequired: Boolean = false,

    val isActive: Boolean = true
)