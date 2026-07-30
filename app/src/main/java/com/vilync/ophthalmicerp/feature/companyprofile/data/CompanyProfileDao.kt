package com.vilync.ophthalmicerp.feature.companyprofile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyProfileDao {

    // =========================================================
    // SAVE / UPDATE COMPANY PROFILE
    // =========================================================

    /*
     * Company Profile is a singleton record with ID = 1.
     *
     * REPLACE allows the same record to be created initially
     * and subsequently saved again without creating duplicates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompanyProfile(
        companyProfile: CompanyProfileEntity
    )


    // =========================================================
    // COMPANY PROFILE
    // =========================================================

    /*
     * Observable version for UI screens and future document
     * preview/rendering.
     */
    @Query(
        """
        SELECT * FROM company_profile
        WHERE id = :profileId
        LIMIT 1
        """
    )
    fun observeCompanyProfile(
        profileId: Long = CompanyProfileEntity.COMPANY_PROFILE_ID
    ): Flow<CompanyProfileEntity?>


    // =========================================================
    // COMPANY PROFILE — ONE-TIME READ
    // =========================================================

    /*
     * Used where a one-time snapshot is required, for example
     * PDF generation, print preparation or repository operations.
     */
    @Query(
        """
        SELECT * FROM company_profile
        WHERE id = :profileId
        LIMIT 1
        """
    )
    suspend fun getCompanyProfile(
        profileId: Long = CompanyProfileEntity.COMPANY_PROFILE_ID
    ): CompanyProfileEntity?


    // =========================================================
    // PROFILE EXISTS
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM company_profile
            WHERE id = :profileId
            LIMIT 1
        )
        """
    )
    suspend fun companyProfileExists(
        profileId: Long = CompanyProfileEntity.COMPANY_PROFILE_ID
    ): Boolean
}