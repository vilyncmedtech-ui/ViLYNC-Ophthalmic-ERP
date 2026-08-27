package com.vilync.ophthalmicerp.feature.sales.challan.presentation

import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster

data class NewChallanUiState(

    val customers: List<PartyMaster> = emptyList(),
    val selectedCustomer: PartyMaster? = null,

    val challanNumber: String = "",
    val challanDate: String = "",
    val financialYearStart: Int = 0,
    val asLibrary: Boolean = false,
    val remarks: String = "",

    val serialQuery: String = "",
    val serialMatches: List<InventoryUnitEntity> = emptyList(),
    val isSearchingSerial: Boolean = false,

    val rate: String = "",
    val gstPercent: String = "",

    val items: List<NewChallanItemUi> = emptyList(),

    val isSaving: Boolean = false,
    val savedChallanId: Long? = null,

    val isEditMode: Boolean = false,
    val editingChallanId: Long? = null,
    val isLoading: Boolean = false,

    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class NewChallanItemUi(
    val inventoryUnitId: Long,
    val productId: Long,
    val productName: String,
    val serialNumber: String,
    val power: String = "",
    val batchNumber: String = "",
    val expiryDate: String = "",
    val rate: Double = 0.0,
    val gstPercent: Double = 0.0
)
