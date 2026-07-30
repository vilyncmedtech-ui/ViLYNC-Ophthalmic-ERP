package com.vilync.ophthalmicerp.feature.master.party.gst


class GstinLookupRepository(
    private val service: com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupService
) {


    // =========================================================
    // LOOKUP RESULT
    // =========================================================

    sealed class LookupResult {

        data class Success(
            val data: com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupResult
        ) : LookupResult()


        data class Error(
            val message: String
        ) : LookupResult()
    }


    // =========================================================
    // LOOKUP GSTIN
    // =========================================================

    suspend fun lookupGstin(
        gstin: String
    ): LookupResult {

        val response =
            service.verifyGstin(
                gstin = gstin
            )


        if (
            !response.success ||
            response.data == null
        ) {

            return LookupResult.Error(
                message =
                    response.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to fetch GSTIN details."
            )
        }


        val apiData =
            response.data


        // =====================================================
        // RAW ADDRESS
        // =====================================================

        val fullAddress =
            apiData.address
                ?.trim()
                .orEmpty()


        // =====================================================
        // EXTRACT PIN CODE
        // =====================================================
        //
        // PIN code is the safest component to extract from a
        // formatted Indian postal address because it normally
        // contains exactly 6 digits.
        // =====================================================

        val pinCode =
            extractPinCode(
                address = fullAddress
            )


        // =====================================================
        // CLEAN ADDRESS
        // =====================================================
        //
        // Keep the original address, but remove the PIN from
        // Address Line 1 because PIN has its own ERP field.
        // =====================================================

        val addressWithoutPin =
            removePinCode(
                address = fullAddress,
                pinCode = pinCode
            )


        // =====================================================
        // ADDRESS COMPONENTS
        // =====================================================

        val addressParts =
            splitAddress(
                address = addressWithoutPin
            )


        // =====================================================
        // STATE
        // =====================================================

        val state =
            apiData.state
                ?.trim()
                .orEmpty()


        // =====================================================
        // CITY / DISTRICT
        // =====================================================
        //
        // GSTVerify currently gives us a formatted address
        // rather than guaranteed separate city/district fields.
        //
        // Therefore these values are best-effort only.
        //
        // We use the final useful address components while
        // avoiding the state value.
        // =====================================================

        val locationParts =
            addressParts
                .filter {
                        part ->

                    part.isNotBlank() &&
                            !part.equals(
                                state,
                                ignoreCase = true
                            )
                }


        val city =
            detectCity(
                parts = locationParts
            )


        val district =
            detectDistrict(
                parts = locationParts,
                city = city
            )


        // =====================================================
        // ADDRESS LINE 1 / ADDRESS LINE 2
        // =====================================================

        val addressLines =
            buildAddressLines(
                parts = locationParts,
                city = city,
                district = district
            )


        // =====================================================
        // MAP API RESPONSE TO ERP MODEL
        // =====================================================

        val result =
            _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupResult(

                gstin =
                    apiData.gstin
                        ?.trim()
                        .orEmpty(),

                legalName =
                    apiData.legal_name
                        ?.trim()
                        .orEmpty(),

                tradeName =
                    apiData.trade_name
                        ?.trim()
                        .orEmpty(),

                registrationStatus =
                    apiData.status
                        ?.trim()
                        .orEmpty(),

                taxpayerType =
                    apiData.taxpayer_type
                        ?.trim()
                        .orEmpty(),

                constitutionOfBusiness =
                    apiData.constitution
                        ?.trim()
                        .orEmpty(),

                registrationDate =
                    apiData.registration_date
                        ?.trim()
                        .orEmpty(),

                addressLine1 =
                    addressLines.first,

                addressLine2 =
                    addressLines.second,

                city =
                    city,

                district =
                    district,

                state =
                    state,

                pinCode =
                    pinCode
            )


        // =====================================================
        // BASIC RESPONSE VALIDATION
        // =====================================================

        if (
            result.legalName.isBlank() &&
            result.tradeName.isBlank()
        ) {

            return LookupResult.Error(
                message =
                    "GSTIN found, but taxpayer details were not returned."
            )
        }


        return LookupResult.Success(
            data = result
        )
    }


    // =========================================================
    // EXTRACT PIN CODE
    // =========================================================

    private fun extractPinCode(
        address: String
    ): String {

        if (address.isBlank()) {
            return ""
        }


        val pinRegex =
            Regex(
                pattern =
                    """(?<!\d)\d{6}(?!\d)"""
            )


        return pinRegex
            .findAll(address)
            .lastOrNull()
            ?.value
            .orEmpty()
    }


    // =========================================================
    // REMOVE PIN CODE FROM ADDRESS
    // =========================================================

    private fun removePinCode(
        address: String,
        pinCode: String
    ): String {

        if (
            address.isBlank() ||
            pinCode.isBlank()
        ) {
            return address.trim()
        }


        return address
            .replace(
                pinCode,
                ""
            )
            .replace(
                Regex(
                    """\s+,"""
                ),
                ","
            )
            .replace(
                Regex(
                    """,\s*,"""
                ),
                ","
            )
            .replace(
                Regex(
                    """\s{2,}"""
                ),
                " "
            )
            .trim()
            .trim(
                ',',
                '-',
                ' '
            )
    }


    // =========================================================
    // SPLIT ADDRESS
    // =========================================================

    private fun splitAddress(
        address: String
    ): List<String> {

        if (address.isBlank()) {
            return emptyList()
        }


        return address
            .split(
                ",",
                ";",
                "\n"
            )
            .map {
                it
                    .trim()
                    .trim(
                        ',',
                        '-',
                        ' '
                    )
            }
            .filter {
                it.isNotBlank()
            }
    }


    // =========================================================
    // DETECT CITY
    // =========================================================

    private fun detectCity(
        parts: List<String>
    ): String {

        if (parts.isEmpty()) {
            return ""
        }


        /*
         * The city is normally towards the end of a formatted
         * Indian business address.
         *
         * We deliberately avoid aggressive guessing.
         */

        return parts
            .lastOrNull()
            ?.trim()
            .orEmpty()
    }


    // =========================================================
    // DETECT DISTRICT
    // =========================================================

    private fun detectDistrict(
        parts: List<String>,
        city: String
    ): String {

        if (parts.size < 2) {
            return ""
        }


        val candidate =
            parts
                .dropLast(1)
                .lastOrNull()
                ?.trim()
                .orEmpty()


        if (
            candidate.isBlank() ||
            candidate.equals(
                city,
                ignoreCase = true
            )
        ) {
            return ""
        }


        return candidate
    }


    // =========================================================
    // BUILD ADDRESS LINES
    // =========================================================

    private fun buildAddressLines(
        parts: List<String>,
        city: String,
        district: String
    ): Pair<String, String> {

        if (parts.isEmpty()) {
            return Pair(
                "",
                ""
            )
        }


        val remainingParts =
            parts.filterNot {
                    part ->

                (
                        city.isNotBlank() &&
                                part.equals(
                                    city,
                                    ignoreCase = true
                                )
                        ) ||
                        (
                                district.isNotBlank() &&
                                        part.equals(
                                            district,
                                            ignoreCase = true
                                        )
                                )
            }


        if (remainingParts.isEmpty()) {

            return Pair(
                "",
                ""
            )
        }


        // -----------------------------------------------------
        // SHORT ADDRESS
        // -----------------------------------------------------

        if (remainingParts.size <= 2) {

            return Pair(
                remainingParts.joinToString(
                    separator = ", "
                ),
                ""
            )
        }


        // -----------------------------------------------------
        // LONG ADDRESS
        // -----------------------------------------------------
        //
        // Divide approximately in half so Address Line 1 does
        // not contain the entire formatted GST address.
        // -----------------------------------------------------

        val splitIndex =
            (remainingParts.size + 1) / 2


        val line1 =
            remainingParts
                .take(
                    splitIndex
                )
                .joinToString(
                    separator = ", "
                )


        val line2 =
            remainingParts
                .drop(
                    splitIndex
                )
                .joinToString(
                    separator = ", "
                )


        return Pair(
            line1,
            line2
        )
    }
}