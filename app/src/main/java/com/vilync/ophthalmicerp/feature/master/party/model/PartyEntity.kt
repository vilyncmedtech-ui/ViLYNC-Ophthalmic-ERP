package com.vilync.ophthalmicerp.master.party.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "parties",

    indices = [
        Index(
            value = ["partyName"]
        ),
        Index(
            value = ["mobileNumber"]
        ),
        Index(
            value = ["gstin"]
        )
    ]
)
data class PartyEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,


    // =========================================================
    // PARTY CLASSIFICATION
    // =========================================================

    val partyType: String,


    // =========================================================
    // BUSINESS INFORMATION
    // =========================================================

    val partyName: String,

    /*
     * GST registered legal business name.
     * Can be populated through GSTIN lookup.
     */

    val legalName: String,

    /*
     * GST registered trade name.
     * Can be populated through GSTIN lookup.
     */

    val tradeName: String,


    // =========================================================
    // CONTACT INFORMATION
    // =========================================================

    val contactPerson: String,

    val mobileNumber: String,

    val alternateMobileNumber: String,

    val email: String,


    // =========================================================
    // TAX INFORMATION
    // =========================================================

    val gstin: String,

    val panNumber: String,


    // =========================================================
    // ADDRESS
    // =========================================================

    val addressLine1: String,

    val addressLine2: String,

    val city: String,

    val district: String,

    val state: String,

    val pinCode: String,


    // =========================================================
    // COMMERCIAL TERMS
    // =========================================================

    val creditDays: Int,

    val creditLimit: Double,


    // =========================================================
    // STATUS
    // =========================================================

    val isActive: Boolean = true
)