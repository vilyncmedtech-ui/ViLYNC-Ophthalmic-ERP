package com.vilync.ophthalmicerp.feature.companyprofile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(

    /*
     * ViLYNC ERP currently maintains one own-company profile.
     *
     * Keeping a fixed primary key makes the record behave like
     * application-level company settings rather than a list/master.
     */
    @PrimaryKey
    val id: Long = COMPANY_PROFILE_ID,

    // =========================================================
    // BUSINESS IDENTITY
    // =========================================================

    val legalName: String = "",

    val tradeName: String = "",

    // =========================================================
    // REGISTERED / BUSINESS ADDRESS
    // =========================================================

    val addressLine1: String = "",

    val addressLine2: String = "",

    val city: String = "",

    val district: String = "",

    val state: String = "",

    val pinCode: String = "",

    // =========================================================
    // TAX / REGISTRATION DETAILS
    // =========================================================

    val gstin: String = "",

    val pan: String = "",

    // =========================================================
    // CONTACT DETAILS
    // =========================================================

    val phone: String = "",

    val email: String = "",

    val website: String = "",

    // =========================================================
    // BANK DETAILS
    // =========================================================

    val accountHolderName: String = "",

    val bankName: String = "",

    val accountNumber: String = "",

    val ifscCode: String = "",

    val bankBranch: String = "",

    val upiId: String = "",

    // =========================================================
    // REGULATORY / LICENCE DETAILS
    // =========================================================

    val drugLicenceNo1: String = "",

    val drugLicenceNo2: String = "",

    val medicalDeviceLicenceNo: String = "",

    // =========================================================
    // DOCUMENT / PRINT IDENTITY
    // =========================================================

    /*
     * Logo and signature are intentionally stored as references
     * instead of binary image data inside Room.
     *
     * Actual file/image handling will be implemented separately
     * in the Company Profile presentation layer.
     */
    val logoUri: String = "",

    val authorisedSignatoryName: String = "",

    val signatureUri: String = "",

    val defaultTermsAndConditions: String = "",

    // =========================================================
    // RECORD METADATA
    // =========================================================

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()

) {

    companion object {

        /*
         * Company Profile is a singleton configuration record.
         * All reads and updates will target this same ID.
         */
        const val COMPANY_PROFILE_ID: Long = 1L
    }
}