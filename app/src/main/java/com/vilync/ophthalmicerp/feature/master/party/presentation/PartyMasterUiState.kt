package com.vilync.ophthalmicerp.feature.master.party.presentation

import com.vilync.ophthalmicerp.feature.master.party.model.PartyType


data class PartyMasterUiState(

    // =========================================================
    // EDIT MODE
    // =========================================================

    val partyId: Long = 0L,

    val isEditMode: Boolean = false,

    val isLoadingParty: Boolean = false,


    // =========================================================
    // PARTY CLASSIFICATION
    // =========================================================

    val partyType: com.vilync.ophthalmicerp.feature.master.party.model.PartyType =
        _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER,


    // =========================================================
    // BUSINESS INFORMATION
    // =========================================================

    val partyName: String = "",

    val legalName: String = "",

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

    val gstin: String = "",

    val panNumber: String = "",


    // =========================================================
    // GSTIN LOOKUP STATE
    // =========================================================
    //
    // These fields prepare the UI for GSTIN Search.
    //
    // Actual online GST API integration will be connected
    // separately.
    // =========================================================

    val isSearchingGstin: Boolean = false,

    val gstinLookupSuccessful: Boolean = false,

    val gstinLookupMessage: String? = null,


    // =========================================================
    // ADDRESS
    // =========================================================

    val addressLine1: String = "",

    val addressLine2: String = "",

    val city: String = "",

    val district: String = "",

    val state: String = "",

    val pinCode: String = "",


    // =========================================================
    // COMMERCIAL TERMS
    // =========================================================

    val creditDays: String = "0",

    val creditLimit: String = "0",


    // =========================================================
    // STATUS
    // =========================================================

    val isActive: Boolean = true,


    // =========================================================
    // VALIDATION / GENERAL ERROR
    // =========================================================

    val errorMessage: String? = null
)