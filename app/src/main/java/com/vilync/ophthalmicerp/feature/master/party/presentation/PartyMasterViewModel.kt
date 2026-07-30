package com.vilync.ophthalmicerp.feature.master.party.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class PartyMasterViewModel(
    private val repository: com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository,
    private val gstinLookupRepository: com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupRepository
) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterUiState()
        )

    val uiState: StateFlow<com.vilync.ophthalmicerp.feature.master.party.presentation.PartyMasterUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // SAVING STATE
    // =========================================================

    private val _isSaving =
        MutableStateFlow(false)

    val isSaving: StateFlow<Boolean> =
        _isSaving.asStateFlow()


    // =========================================================
    // PARTY TYPE
    // =========================================================

    fun updatePartyType(
        value: com.vilync.ophthalmicerp.feature.master.party.model.PartyType
    ) {

        _uiState.value =
            _uiState.value.copy(
                partyType = value,
                errorMessage = null
            )
    }


    // =========================================================
    // BUSINESS INFORMATION
    // =========================================================

    fun updatePartyName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                partyName = value,
                errorMessage = null
            )
    }


    fun updateLegalName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                legalName = value,
                errorMessage = null
            )
    }


    fun updateTradeName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                tradeName = value,
                errorMessage = null
            )
    }


    // =========================================================
    // CONTACT INFORMATION
    // =========================================================

    fun updateContactPerson(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                contactPerson = value,
                errorMessage = null
            )
    }


    fun updateMobileNumber(
        value: String
    ) {

        val filtered =
            value
                .filter {
                    it.isDigit()
                }
                .take(10)

        _uiState.value =
            _uiState.value.copy(
                mobileNumber = filtered,
                errorMessage = null
            )
    }


    fun updateAlternateMobileNumber(
        value: String
    ) {

        val filtered =
            value
                .filter {
                    it.isDigit()
                }
                .take(10)

        _uiState.value =
            _uiState.value.copy(
                alternateMobileNumber = filtered,
                errorMessage = null
            )
    }


    fun updateEmail(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                email = value.trim(),
                errorMessage = null
            )
    }


    // =========================================================
    // GSTIN
    // =========================================================

    fun updateGstin(
        value: String
    ) {

        val filtered =
            value
                .uppercase()
                .filter {
                    it.isLetterOrDigit()
                }
                .take(15)


        // GSTIN structure:
        // Characters 3 to 12 contain PAN.
        //
        // Example:
        // 09ABCDE1234F1Z5
        //   ABCDE1234F
        //
        // Kotlin index 2 until 12.

        val derivedPan =
            if (filtered.length >= 12) {

                filtered.substring(
                    2,
                    12
                )

            } else {

                ""
            }


        _uiState.value =
            _uiState.value.copy(
                gstin = filtered,
                panNumber = derivedPan,

                // Any GSTIN edit invalidates the previous lookup.
                gstinLookupSuccessful = false,
                gstinLookupMessage = null,

                errorMessage = null
            )
    }


    // =========================================================
    // PAN
    // =========================================================

    fun updatePanNumber(
        value: String
    ) {

        val filtered =
            value
                .uppercase()
                .filter {
                    it.isLetterOrDigit()
                }
                .take(10)

        _uiState.value =
            _uiState.value.copy(
                panNumber = filtered,
                errorMessage = null
            )
    }


    // =========================================================
    // GST LOOKUP - START
    // =========================================================
    //
    // Actual GST API integration will call this before making
    // the network request.
    // =========================================================

    fun startGstinLookup() {

        val gstin =
            _uiState.value.gstin
                .trim()
                .uppercase()


        // =========================================================
        // BASIC GSTIN VALIDATION
        // =========================================================

        if (gstin.length != 15) {

            _uiState.value =
                _uiState.value.copy(
                    isSearchingGstin = false,
                    gstinLookupSuccessful = false,
                    gstinLookupMessage =
                        "Enter a valid 15-character GSTIN."
                )

            return
        }


        // =========================================================
        // START LOOKUP
        // =========================================================

        _uiState.value =
            _uiState.value.copy(
                gstin = gstin,
                isSearchingGstin = true,
                gstinLookupSuccessful = false,
                gstinLookupMessage =
                    "Searching GST details..."
            )


        viewModelScope.launch {

            try {

                when (
                    val result =
                        gstinLookupRepository
                            .lookupGstin(
                                gstin = gstin
                            )
                ) {

                    is GstinLookupRepository.LookupResult.Success -> {

                        // ==========================================
                        // APPLY FETCHED GST DETAILS
                        // ==========================================

                        val data = result.data

                        applyGstinLookupResult(
                            legalName = data.legalName,
                            tradeName = data.tradeName,
                            addressLine1 = data.addressLine1,
                            addressLine2 = data.addressLine2,
                            city = data.city,
                            district = data.district,
                            state = data.state,
                            pinCode = data.pinCode
                        )
                    }


                    is GstinLookupRepository.LookupResult.Error -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isSearchingGstin = false,
                                gstinLookupSuccessful = false,
                                gstinLookupMessage =
                                    result.message
                            )
                    }
                }

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSearchingGstin = false,
                        gstinLookupSuccessful = false,
                        gstinLookupMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to fetch GST details."
                    )
            }
        }
    }



    // =========================================================
    // GST LOOKUP - SUCCESS
    // =========================================================
    //
    // This function is ready for the GST API layer.
    //
    // API result will be passed here and the Party Master
    // fields will automatically update.
    // =========================================================

    fun applyGstinLookupResult(
        legalName: String,
        tradeName: String,
        addressLine1: String,
        addressLine2: String,
        city: String,
        district: String,
        state: String,
        pinCode: String
    ) {

        val currentState =
            _uiState.value


        val preferredPartyName =
            when {

                tradeName.isNotBlank() ->
                    tradeName.trim()

                legalName.isNotBlank() ->
                    legalName.trim()

                else ->
                    currentState.partyName
            }


        _uiState.value =
            currentState.copy(

                partyName =
                    preferredPartyName,

                legalName =
                    legalName.trim(),

                tradeName =
                    tradeName.trim(),

                addressLine1 =
                    addressLine1.trim(),

                addressLine2 =
                    addressLine2.trim(),

                city =
                    city.trim(),

                district =
                    district.trim(),

                state =
                    state.trim(),

                pinCode =
                    pinCode
                        .filter {
                            it.isDigit()
                        }
                        .take(6),

                isSearchingGstin =
                    false,

                gstinLookupSuccessful =
                    true,

                gstinLookupMessage =
                    "GSTIN details found and filled.",

                errorMessage =
                    null
            )
    }


    // =========================================================
    // GST LOOKUP - FAILURE
    // =========================================================

    fun applyGstinLookupError(
        message: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                isSearchingGstin = false,
                gstinLookupSuccessful = false,
                gstinLookupMessage =
                    message.ifBlank {
                        "Unable to fetch GSTIN details."
                    }
            )
    }


    // =========================================================
    // ADDRESS
    // =========================================================

    fun updateAddressLine1(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                addressLine1 = value,
                errorMessage = null
            )
    }


    fun updateAddressLine2(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                addressLine2 = value,
                errorMessage = null
            )
    }


    fun updateCity(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                city = value,
                errorMessage = null
            )
    }


    fun updateDistrict(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                district = value,
                errorMessage = null
            )
    }


    fun updateState(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                state = value,
                errorMessage = null
            )
    }


    fun updatePinCode(
        value: String
    ) {

        val filtered =
            value
                .filter {
                    it.isDigit()
                }
                .take(6)

        _uiState.value =
            _uiState.value.copy(
                pinCode = filtered,
                errorMessage = null
            )
    }


    // =========================================================
    // COMMERCIAL TERMS
    // =========================================================

    fun updateCreditDays(
        value: String
    ) {

        val filtered =
            value
                .filter {
                    it.isDigit()
                }
                .take(4)

        _uiState.value =
            _uiState.value.copy(
                creditDays =
                    filtered.ifBlank {
                        "0"
                    },

                errorMessage = null
            )
    }


    fun updateCreditLimit(
        value: String
    ) {

        val filtered =
            value.filter {
                it.isDigit() ||
                        it == '.'
            }

        val decimalCount =
            filtered.count {
                it == '.'
            }


        if (decimalCount <= 1) {

            _uiState.value =
                _uiState.value.copy(
                    creditLimit = filtered,
                    errorMessage = null
                )
        }
    }


    // =========================================================
    // ACTIVE STATUS
    // =========================================================

    fun updateIsActive(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                isActive = value
            )
    }


    // =========================================================
    // LOAD PARTY FOR EDIT
    // =========================================================

    fun loadParty(
        partyId: Long
    ) {

        if (partyId <= 0L) {
            return
        }


        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoadingParty = true,
                    errorMessage = null
                )


            try {

                val party =
                    repository.getPartyById(
                        partyId
                    )


                if (party == null) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingParty = false,
                            errorMessage =
                                "Party not found."
                        )

                    return@launch
                }


                _uiState.value =
                    PartyMasterUiState(

                        partyId =
                            party.id,

                        isEditMode =
                            true,

                        isLoadingParty =
                            false,

                        partyType =
                            party.partyType,

                        partyName =
                            party.partyName,

                        legalName =
                            party.legalName,

                        tradeName =
                            party.tradeName,

                        contactPerson =
                            party.contactPerson,

                        mobileNumber =
                            party.mobileNumber,

                        alternateMobileNumber =
                            party.alternateMobileNumber,

                        email =
                            party.email,

                        gstin =
                            party.gstin,

                        panNumber =
                            party.panNumber,

                        isSearchingGstin =
                            false,

                        gstinLookupSuccessful =
                            false,

                        gstinLookupMessage =
                            null,

                        addressLine1 =
                            party.addressLine1,

                        addressLine2 =
                            party.addressLine2,

                        city =
                            party.city,

                        district =
                            party.district,

                        state =
                            party.state,

                        pinCode =
                            party.pinCode,

                        creditDays =
                            party.creditDays.toString(),

                        creditLimit =
                            party.creditLimit.toString(),

                        isActive =
                            party.isActive,

                        errorMessage =
                            null
                    )


            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingParty = false,

                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load party."
                    )
            }
        }
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private fun validate():
            String? {

        val state =
            _uiState.value


        if (state.partyName.trim().isBlank()) {

            return "Party Name is required."
        }


        if (
            state.mobileNumber.isNotBlank() &&
            state.mobileNumber.length != 10
        ) {

            return "Mobile Number must contain 10 digits."
        }


        if (
            state.alternateMobileNumber.isNotBlank() &&
            state.alternateMobileNumber.length != 10
        ) {

            return "Alternate Mobile Number must contain 10 digits."
        }


        if (
            state.email.isNotBlank() &&
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(
                    state.email
                )
                .matches()
        ) {

            return "Please enter a valid Email Address."
        }


        if (
            state.gstin.isNotBlank() &&
            state.gstin.length != 15
        ) {

            return "GSTIN must contain 15 characters."
        }


        if (
            state.panNumber.isNotBlank() &&
            state.panNumber.length != 10
        ) {

            return "PAN Number must contain 10 characters."
        }


        if (
            state.pinCode.isNotBlank() &&
            state.pinCode.length != 6
        ) {

            return "PIN Code must contain 6 digits."
        }


        val creditDays =
            state.creditDays
                .toIntOrNull()


        if (
            creditDays == null ||
            creditDays < 0
        ) {

            return "Please enter valid Credit Days."
        }


        val creditLimit =
            state.creditLimit
                .toDoubleOrNull()


        if (
            creditLimit == null ||
            creditLimit < 0.0
        ) {

            return "Please enter valid Credit Limit."
        }


        return null
    }


    // =========================================================
    // CREATE PARTY MASTER
    // =========================================================

    fun createPartyMaster():
            PartyMaster? {

        val validationError =
            validate()


        if (validationError != null) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        validationError
                )

            return null
        }


        val state =
            _uiState.value


        return PartyMaster(

            id =
                state.partyId,

            partyType =
                state.partyType,

            partyName =
                state.partyName.trim(),

            legalName =
                state.legalName.trim(),

            tradeName =
                state.tradeName.trim(),

            contactPerson =
                state.contactPerson.trim(),

            mobileNumber =
                state.mobileNumber.trim(),

            alternateMobileNumber =
                state.alternateMobileNumber.trim(),

            email =
                state.email.trim(),

            gstin =
                state.gstin.trim(),

            panNumber =
                state.panNumber.trim(),

            addressLine1 =
                state.addressLine1.trim(),

            addressLine2 =
                state.addressLine2.trim(),

            city =
                state.city.trim(),

            district =
                state.district.trim(),

            state =
                state.state.trim(),

            pinCode =
                state.pinCode.trim(),

            creditDays =
                state.creditDays
                    .toIntOrNull()
                    ?: 0,

            creditLimit =
                state.creditLimit
                    .toDoubleOrNull()
                    ?: 0.0,

            isActive =
                state.isActive
        )
    }


    // =========================================================
    // SAVE / UPDATE
    // =========================================================

    fun saveParty(
        onSuccess: (Long) -> Unit = {}
    ) {

        if (_isSaving.value) {
            return
        }


        val party =
            createPartyMaster()
                ?: return


        viewModelScope.launch {

            _isSaving.value =
                true


            try {

                when (
                    val result =
                        repository.saveParty(
                            party
                        )
                ) {

                    is PartyRepository.SaveResult.Success -> {

                        _uiState.value =
                            _uiState.value.copy(
                                partyId =
                                    result.partyId,

                                isEditMode =
                                    true,

                                errorMessage =
                                    null
                            )


                        onSuccess(
                            result.partyId
                        )
                    }


                    PartyRepository.SaveResult.DuplicateParty -> {

                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage =
                                    "Party Name already exists."
                            )
                    }


                    is PartyRepository.SaveResult.Error -> {

                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage =
                                    result.message
                            )
                    }
                }


            } finally {

                _isSaving.value =
                    false
            }
        }
    }


    // =========================================================
    // DELETE PARTY
    // =========================================================

    fun deleteParty(
        onSuccess: () -> Unit = {}
    ) {

        val partyId =
            _uiState.value.partyId


        if (
            partyId <= 0L ||
            _isSaving.value
        ) {
            return
        }


        viewModelScope.launch {

            _isSaving.value =
                true


            try {

                val deleted =
                    repository.deleteParty(
                        partyId
                    )


                if (deleted) {

                    onSuccess()

                } else {

                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage =
                                "Unable to delete party."
                        )
                }


            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to delete party."
                    )


            } finally {

                _isSaving.value =
                    false
            }
        }
    }


    // =========================================================
    // CLEAR ERROR
    // =========================================================

    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }


    // =========================================================
    // CLEAR GST LOOKUP MESSAGE
    // =========================================================

    fun clearGstinLookupMessage() {

        _uiState.value =
            _uiState.value.copy(
                gstinLookupMessage = null
            )
    }
}