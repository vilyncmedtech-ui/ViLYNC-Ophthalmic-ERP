package com.vilync.ophthalmicerp.feature.master.party.model


data class PartyMaster(

    val id: Long = 0L,


    // =========================================================
    // PARTY CLASSIFICATION
    // =========================================================

    val partyType: com.vilync.ophthalmicerp.feature.master.party.model.PartyType =
        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER,


    // =========================================================
    // BUSINESS INFORMATION
    // =========================================================

    /*
     * Main display name used throughout the ERP.
     *
     * For GST registered parties this can normally be
     * populated from Trade Name / Legal Name after GST lookup.
     */

    val partyName: String = "",


    /*
     * GST Registered Legal Name.
     *
     * Intended to be auto-filled through GSTIN lookup.
     */

    val legalName: String = "",


    /*
     * GST Trade Name.
     *
     * Intended to be auto-filled through GSTIN lookup.
     */

    val tradeName: String = "",


    // =========================================================
    // CONTACT INFORMATION
    // =========================================================

    val contactPerson: String = "",

    val mobileNumber: String = "",

    val alternateMobileNumber: String = "",

    val email: String = "",


    // =========================================================
    // TAX INFORMATION
    // =========================================================

    /*
     * 15-character GSTIN.
     */

    val gstin: String = "",


    /*
     * PAN can later be auto-derived from a valid GSTIN.
     */

    val panNumber: String = "",


    // =========================================================
    // ADDRESS
    // =========================================================

    /*
     * These fields are also ready to receive values
     * returned from GSTIN lookup.
     */

    val addressLine1: String = "",

    val addressLine2: String = "",

    val city: String = "",

    val district: String = "",

    val state: String = "",

    val pinCode: String = "",


    // =========================================================
    // COMMERCIAL TERMS
    // =========================================================

    val creditDays: Int = 0,

    val creditLimit: Double = 0.0,


    // =========================================================
    // STATUS
    // =========================================================

    val isActive: Boolean = true
)