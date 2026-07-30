package com.vilync.ophthalmicerp.master.party.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface PartyDao {


    // =========================================================
    // INSERT
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertParty(
        party: com.vilync.ophthalmicerp.master.party.data.PartyEntity
    ): Long


    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun updateParty(
        party: com.vilync.ophthalmicerp.master.party.data.PartyEntity
    )


    // =========================================================
    // GET ALL PARTIES
    // =========================================================
    //
    // Includes:
    // Active + Inactive
    //
    // Used by Party List so that status filtering can
    // be performed correctly.
    // =========================================================

    @Query(
        """
        SELECT * FROM parties
        ORDER BY partyName COLLATE NOCASE ASC
        """
    )
    fun getAllParties():
            Flow<List<com.vilync.ophthalmicerp.master.party.data.PartyEntity>>


    // =========================================================
    // GET ALL ACTIVE PARTIES
    // =========================================================

    @Query(
        """
        SELECT * FROM parties
        WHERE isActive = 1
        ORDER BY partyName COLLATE NOCASE ASC
        """
    )
    fun getAllActiveParties():
            Flow<List<com.vilync.ophthalmicerp.master.party.data.PartyEntity>>


    // =========================================================
    // GET ALL INACTIVE PARTIES
    // =========================================================

    @Query(
        """
        SELECT * FROM parties
        WHERE isActive = 0
        ORDER BY partyName COLLATE NOCASE ASC
        """
    )
    fun getAllInactiveParties():
            Flow<List<com.vilync.ophthalmicerp.master.party.data.PartyEntity>>


    // =========================================================
    // GET PARTY BY ID
    // =========================================================

    @Query(
        """
        SELECT * FROM parties
        WHERE id = :partyId
        LIMIT 1
        """
    )
    suspend fun getPartyById(
        partyId: Long
    ): com.vilync.ophthalmicerp.master.party.data.PartyEntity?


    // =========================================================
    // SEARCH ALL PARTIES
    // =========================================================
    //
    // Important:
    // No isActive restriction here.
    //
    // This allows search results to respect the
    // All / Active / Inactive filter in Party List.
    // =========================================================

    @Query(
        """
        SELECT * FROM parties
        WHERE (
            partyName LIKE '%' || :query || '%'
            OR legalName LIKE '%' || :query || '%'
            OR tradeName LIKE '%' || :query || '%'
            OR contactPerson LIKE '%' || :query || '%'
            OR mobileNumber LIKE '%' || :query || '%'
            OR alternateMobileNumber LIKE '%' || :query || '%'
            OR email LIKE '%' || :query || '%'
            OR gstin LIKE '%' || :query || '%'
            OR panNumber LIKE '%' || :query || '%'
            OR city LIKE '%' || :query || '%'
            OR district LIKE '%' || :query || '%'
            OR state LIKE '%' || :query || '%'
            OR pinCode LIKE '%' || :query || '%'
            OR partyType LIKE '%' || :query || '%'
        )
        ORDER BY partyName COLLATE NOCASE ASC
        """
    )
    fun searchParties(
        query: String
    ): Flow<List<com.vilync.ophthalmicerp.master.party.data.PartyEntity>>


    // =========================================================
    // DUPLICATE PARTY NAME - NEW PARTY
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM parties
        WHERE isActive = 1
        AND LOWER(TRIM(partyName)) = :normalizedPartyName
        """
    )
    suspend fun countDuplicateParty(
        normalizedPartyName: String
    ): Int


    // =========================================================
    // DUPLICATE PARTY NAME - EDIT MODE
    // =========================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM parties
        WHERE isActive = 1
        AND id != :excludePartyId
        AND LOWER(TRIM(partyName)) = :normalizedPartyName
        """
    )
    suspend fun countDuplicatePartyForEdit(
        normalizedPartyName: String,
        excludePartyId: Long
    ): Int


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @Query(
        """
        UPDATE parties
        SET isActive = 0
        WHERE id = :partyId
        """
    )
    suspend fun deactivateParty(
        partyId: Long
    ): Int


    // =========================================================
    // PERMANENT DELETE
    // =========================================================
    //
    // Once Purchase / Sales transactions are linked with
    // Party ID, transaction history should be checked before
    // allowing permanent deletion.
    // =========================================================

    @Query(
        """
        DELETE FROM parties
        WHERE id = :partyId
        """
    )
    suspend fun deleteParty(
        partyId: Long
    ): Int
}