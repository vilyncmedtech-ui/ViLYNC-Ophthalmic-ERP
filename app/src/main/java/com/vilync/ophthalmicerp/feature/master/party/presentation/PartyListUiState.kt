package com.vilync.ophthalmicerp.feature.master.party.presentation

import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType


data class PartyListUiState(

    // =========================================================
    // PARTY DATA
    // =========================================================

    val parties: List<com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster> =
        emptyList(),


    // =========================================================
    // SEARCH
    // =========================================================

    val searchQuery: String =
        "",


    // =========================================================
    // PARTY TYPE FILTER
    // =========================================================
    //
    // null = All Parties
    // CUSTOMER = Customers
    // VENDOR = Vendors
    // BOTH = Customer & Vendor
    // =========================================================

    val selectedPartyType: com.vilync.ophthalmicerp.feature.master.party.model.PartyType? =
        null,


    // =========================================================
    // ACTIVE FILTER
    // =========================================================
    //
    // null  = All
    // true  = Active
    // false = Inactive
    // =========================================================

    val selectedActiveStatus: Boolean? =
        null,


    // =========================================================
    // LOADING
    // =========================================================

    val isLoading: Boolean =
        false,


    // =========================================================
    // ERROR
    // =========================================================

    val errorMessage: String? =
        null
)