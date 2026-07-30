package com.vilync.ophthalmicerp.feature.master.party.gst


data class GstinApiResponse(

    val success: Boolean = false,

    val cached: Boolean = false,

    val credits_remaining: Int? = null,

    val data: com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiData? = null,

    val message: String? = null
)


data class GstinApiData(

    val gstin: String? = null,

    val legal_name: String? = null,

    val trade_name: String? = null,

    val status: String? = null,

    val constitution: String? = null,

    val taxpayer_type: String? = null,

    val registration_date: String? = null,

    val state: String? = null,

    val pan: String? = null,

    val address: String? = null,

    val nature_of_business: List<String>? = null
)