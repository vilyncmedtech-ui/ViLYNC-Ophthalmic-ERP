package com.vilync.ophthalmicerp.feature.master.party.gst


data class GstinLookupResult(

    // =========================================================
    // GST IDENTIFICATION
    // =========================================================

    val gstin: String = "",

    val legalName: String = "",

    val tradeName: String = "",

    val registrationStatus: String = "",


    // =========================================================
    // BUSINESS INFORMATION
    // =========================================================

    val taxpayerType: String = "",

    val constitutionOfBusiness: String = "",

    val registrationDate: String = "",


    // =========================================================
    // ADDRESS
    // =========================================================

    val addressLine1: String = "",

    val addressLine2: String = "",

    val city: String = "",

    val district: String = "",

    val state: String = "",

    val pinCode: String = ""
)