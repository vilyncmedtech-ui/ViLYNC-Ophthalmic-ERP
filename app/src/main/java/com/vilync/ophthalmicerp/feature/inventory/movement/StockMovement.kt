package com.vilync.ophthalmicerp.feature.inventory.movement

data class StockMovement(

    val id: Long = 0,

    val serialNumber: String = "",

    val productName: String = "",

    val model: String = "",

    val power: String = "",

    val movementType: MovementType = MovementType.PURCHASE_IN,

    val movementDate: String = "",

    val partyName: String = "",

    val referenceNumber: String = "",

    val remarks: String = ""
)