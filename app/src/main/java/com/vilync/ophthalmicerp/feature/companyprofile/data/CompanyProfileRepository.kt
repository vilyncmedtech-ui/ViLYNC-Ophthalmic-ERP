package com.vilync.ophthalmicerp.feature.companyprofile.data

import kotlinx.coroutines.flow.Flow

class CompanyProfileRepository(
    private val companyProfileDao: CompanyProfileDao
) {

    // =========================================================
    // OBSERVE COMPANY PROFILE
    // =========================================================

    fun observeCompanyProfile():
            Flow<CompanyProfileEntity?> {

        return companyProfileDao.observeCompanyProfile()
    }


    // =========================================================
    // GET COMPANY PROFILE
    // =========================================================

    suspend fun getCompanyProfile():
            CompanyProfileEntity? {

        return companyProfileDao.getCompanyProfile()
    }


    // =========================================================
    // PROFILE EXISTS
    // =========================================================

    suspend fun companyProfileExists(): Boolean {

        return companyProfileDao.companyProfileExists()
    }


    // =========================================================
    // SAVE / UPDATE COMPANY PROFILE
    // =========================================================

    suspend fun saveCompanyProfile(
        companyProfile: CompanyProfileEntity
    ) {

        val existingProfile =
            companyProfileDao.getCompanyProfile()

        val now =
            System.currentTimeMillis()

        val normalizedProfile =
            companyProfile.copy(

                id = CompanyProfileEntity.COMPANY_PROFILE_ID,

                legalName =
                    companyProfile.legalName.trim(),

                tradeName =
                    companyProfile.tradeName.trim(),

                addressLine1 =
                    companyProfile.addressLine1.trim(),

                addressLine2 =
                    companyProfile.addressLine2.trim(),

                city =
                    companyProfile.city.trim(),

                district =
                    companyProfile.district.trim(),

                state =
                    companyProfile.state.trim(),

                pinCode =
                    companyProfile.pinCode.trim(),

                gstin =
                    companyProfile.gstin
                        .trim()
                        .uppercase(),

                pan =
                    companyProfile.pan
                        .trim()
                        .uppercase(),

                phone =
                    companyProfile.phone.trim(),

                email =
                    companyProfile.email
                        .trim()
                        .lowercase(),

                website =
                    companyProfile.website.trim(),

                accountHolderName =
                    companyProfile.accountHolderName.trim(),

                bankName =
                    companyProfile.bankName.trim(),

                accountNumber =
                    companyProfile.accountNumber.trim(),

                ifscCode =
                    companyProfile.ifscCode
                        .trim()
                        .uppercase(),

                bankBranch =
                    companyProfile.bankBranch.trim(),

                upiId =
                    companyProfile.upiId.trim(),

                logoUri =
                    companyProfile.logoUri.trim(),

                authorisedSignatoryName =
                    companyProfile.authorisedSignatoryName.trim(),

                signatureUri =
                    companyProfile.signatureUri.trim(),

                defaultTermsAndConditions =
                    companyProfile.defaultTermsAndConditions.trim(),

                createdAt =
                    existingProfile?.createdAt
                        ?: companyProfile.createdAt,

                updatedAt =
                    now
            )

        companyProfileDao.saveCompanyProfile(
            normalizedProfile
        )
    }
}