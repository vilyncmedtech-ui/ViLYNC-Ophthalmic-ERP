package com.vilync.ophthalmicerp.feature.master.party.data

import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class PartyRepository(
    private val partyDao: com.vilync.ophthalmicerp.master.party.data.PartyDao
) {

    // =========================================================
    // SAVE RESULT
    // =========================================================

    sealed class SaveResult {

        data class Success(
            val partyId: Long
        ) : SaveResult()

        data object DuplicateParty : SaveResult()

        data class Error(
            val message: String
        ) : SaveResult()
    }


    // =========================================================
    // SAVE / UPDATE PARTY
    // =========================================================

    suspend fun saveParty(
        party: com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
    ): SaveResult {

        return try {

            val cleanPartyName =
                cleanDisplayText(
                    party.partyName
                )

            val normalizedPartyName =
                normalizeForDuplicateCheck(
                    cleanPartyName
                )


            // =================================================
            // PARTY NAME REQUIRED
            // =================================================

            if (normalizedPartyName.isBlank()) {

                return SaveResult.Error(
                    "Party Name is required."
                )
            }


            // =================================================
            // DUPLICATE CHECK
            // =================================================

            val duplicateCount =
                if (party.id > 0L) {

                    partyDao.countDuplicatePartyForEdit(
                        normalizedPartyName =
                            normalizedPartyName,
                        excludePartyId =
                            party.id
                    )

                } else {

                    partyDao.countDuplicateParty(
                        normalizedPartyName =
                            normalizedPartyName
                    )
                }


            if (duplicateCount > 0) {

                return SaveResult.DuplicateParty
            }


            // =================================================
            // PARTY MASTER -> PARTY ENTITY
            // =================================================

            val entity =
                _root_ide_package_.com.vilync.ophthalmicerp.master.party.data.PartyEntity(

                    id =
                        party.id,

                    partyType =
                        party.partyType.name,

                    partyName =
                        cleanPartyName,

                    legalName =
                        cleanDisplayText(
                            party.legalName
                        ),

                    tradeName =
                        cleanDisplayText(
                            party.tradeName
                        ),

                    contactPerson =
                        cleanDisplayText(
                            party.contactPerson
                        ),

                    mobileNumber =
                        party.mobileNumber.trim(),

                    alternateMobileNumber =
                        party.alternateMobileNumber.trim(),

                    email =
                        party.email.trim(),

                    gstin =
                        party.gstin
                            .trim()
                            .uppercase(),

                    panNumber =
                        party.panNumber
                            .trim()
                            .uppercase(),

                    addressLine1 =
                        cleanDisplayText(
                            party.addressLine1
                        ),

                    addressLine2 =
                        cleanDisplayText(
                            party.addressLine2
                        ),

                    city =
                        cleanDisplayText(
                            party.city
                        ),

                    district =
                        cleanDisplayText(
                            party.district
                        ),

                    state =
                        cleanDisplayText(
                            party.state
                        ),

                    pinCode =
                        party.pinCode.trim(),

                    creditDays =
                        party.creditDays,

                    creditLimit =
                        party.creditLimit,

                    isActive =
                        party.isActive
                )


            // =================================================
            // INSERT / UPDATE
            // =================================================

            val savedPartyId =
                if (party.id > 0L) {

                    partyDao.updateParty(
                        entity
                    )

                    party.id

                } else {

                    partyDao.insertParty(
                        entity
                    )
                }


            SaveResult.Success(
                partyId =
                    savedPartyId
            )


        } catch (exception: Exception) {

            SaveResult.Error(
                message =
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to save party."
            )
        }
    }


    // =========================================================
    // GET PARTY BY ID
    // =========================================================

    suspend fun getPartyById(
        partyId: Long
    ): com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster? {

        return partyDao
            .getPartyById(
                partyId
            )
            ?.toPartyMaster()
    }


    // =========================================================
    // GET ALL ACTIVE PARTIES
    // =========================================================

    fun getAllActiveParties():
            Flow<List<com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster>> {

        return partyDao
            .getAllActiveParties()
            .map { entities ->

                entities.map { entity ->

                    entity.toPartyMaster()
                }
            }
    }


    // =========================================================
    // SEARCH PARTIES
    // =========================================================

    fun searchParties(
        query: String
    ): Flow<List<com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster>> {

        return partyDao
            .searchParties(
                query.trim()
            )
            .map { entities ->

                entities.map { entity ->

                    entity.toPartyMaster()
                }
            }
    }


    // =========================================================
    // DEACTIVATE PARTY
    // =========================================================

    suspend fun deactivateParty(
        partyId: Long
    ): Boolean {

        return partyDao
            .deactivateParty(
                partyId
            ) > 0
    }


    // =========================================================
    // PERMANENT DELETE PARTY
    // =========================================================

    suspend fun deleteParty(
        partyId: Long
    ): Boolean {

        return partyDao
            .deleteParty(
                partyId
            ) > 0
    }


    // =========================================================
    // PARTY ENTITY -> PARTY MASTER
    // =========================================================

    private fun com.vilync.ophthalmicerp.master.party.data.PartyEntity.toPartyMaster():
            com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster {

        return _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster(

            id =
                id,

            partyType =
                runCatching {

                    _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.valueOf(
                        partyType
                    )

                }.getOrDefault(
                    _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.model.PartyType.CUSTOMER
                ),

            partyName =
                partyName,

            legalName =
                legalName,

            tradeName =
                tradeName,

            contactPerson =
                contactPerson,

            mobileNumber =
                mobileNumber,

            alternateMobileNumber =
                alternateMobileNumber,

            email =
                email,

            gstin =
                gstin,

            panNumber =
                panNumber,

            addressLine1 =
                addressLine1,

            addressLine2 =
                addressLine2,

            city =
                city,

            district =
                district,

            state =
                state,

            pinCode =
                pinCode,

            creditDays =
                creditDays,

            creditLimit =
                creditLimit,

            isActive =
                isActive
        )
    }


    // =========================================================
    // CLEAN DISPLAY TEXT
    // =========================================================

    private fun cleanDisplayText(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }


    // =========================================================
    // NORMALIZE DUPLICATE CHECK
    // =========================================================

    private fun normalizeForDuplicateCheck(
        value: String
    ): String {

        return cleanDisplayText(
            value
        ).lowercase()
    }
}