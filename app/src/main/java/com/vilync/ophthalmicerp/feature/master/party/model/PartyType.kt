package com.vilync.ophthalmicerp.feature.master.party.model

enum class PartyType(
    val displayName: String
) {
    CUSTOMER("Customer"),
    VENDOR("Vendor"),
    BOTH("Customer & Vendor")
}