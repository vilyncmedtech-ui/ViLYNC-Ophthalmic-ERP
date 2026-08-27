package com.vilync.ophthalmicerp.feature.sales.presentation

import com.vilync.ophthalmicerp.data.entity.InventoryUnitEntity
import com.vilync.ophthalmicerp.data.entity.ProductEntity
import com.vilync.ophthalmicerp.data.entity.ChallanEntity
import com.vilync.ophthalmicerp.data.entity.ChallanItemEntity
import com.vilync.ophthalmicerp.feature.master.party.model.PartyMaster


// =============================================================
// SALES UI STATE
// =============================================================
//
// Single source of truth for New Sales Invoice screen.
//
// Covers:
//
// Bill To / Customer
// Ship To
// Invoice Details
// Purchase Order Details
// Place of Supply / GST Type
// Product / Power / Physical Serial selection
// ERP-wide Smart Serial / Unique ID Search
// Rate / Discount / GST
// CGST / SGST / IGST
// Bill Summary
// Save state
//
// InventoryUnitEntity.id remains the authoritative identity of
// every physical serial selected for Sale.
// =============================================================

data class SalesUiState(

    // =========================================================
    // DOCUMENT MODE
    // =========================================================

    val editingSaleId: Long? = null,

    val isEditMode: Boolean = false,

    // =========================================================
    // MASTER DATA
    // =========================================================

    val customers: List<PartyMaster> = emptyList(),

    val products: List<ProductEntity> = emptyList(),

    val inStockUnits: List<InventoryUnitEntity> = emptyList(),


    // =========================================================
    // BILL TO / CUSTOMER
    // =========================================================

    val selectedCustomer: PartyMaster? = null,

    val customerSearchQuery: String = "",

    // Snapshot fields populated from selected Party.
    val billToLegalName: String = "",

    val billToGstin: String = "",

    val billToAddress: String = "",

    val billToState: String = "",


    // =========================================================
    // SHIP TO
    // =========================================================

    val sameAsBillTo: Boolean = true,

    val shipToName: String = "",

    val shipToGstin: String = "",

    val shipToAddress: String = "",

    val shipToState: String = "",


    // =========================================================
    // INVOICE DETAILS
    // =========================================================

    val invoiceNumber: String = "",

    val nextInvoiceNumberPreview: String = "",

    // Selected through Date Picker.
    val invoiceDate: String = "",

    /*
     * Internal only.
     * Not displayed on Sales Invoice UI.
     */
    val financialYearStart: Int = 0,


    // =========================================================
    // PURCHASE ORDER REFERENCE
    // =========================================================

    val poNumber: String = "",

    // Optional. Selected through Date Picker.
    val poDate: String = "",


    // =========================================================
    // GST / PLACE OF SUPPLY
    // =========================================================

    val placeOfSupplyState: String = "",

    /*
     * Expected:
     *
     * INTRA_STATE
     * INTER_STATE
     */
    val gstSupplyType: String = "",


    // =========================================================
    // REMARKS
    // =========================================================

    val remarks: String = "",


    // =========================================================
    // SMART SERIAL / UNIQUE ID SEARCH
    // =========================================================
    //
    // Example stored serials:
    //
    // LMDE232123
    // LMMS234367
    //
    // User may search:
    //
    // 232123
    //
    // Repository resolves:
    //
    // 1. Exact full serial first
    // 2. Serial suffix second
    //
    // If one match exists, ViewModel will select it directly.
    //
    // If multiple matches exist, smartSerialMatches contains
    // all candidates and UI will ask the user to select the
    // correct physical lens.
    // =========================================================

    val smartSerialQuery: String = "",

    val smartSerialMatches: List<InventoryUnitEntity> =
        emptyList(),

    val isSmartSerialSearching: Boolean = false,

    val showSmartSerialMatchSelection: Boolean = false,


    // =========================================================
    // SETTLE CHALLAN
    // =========================================================
    //
    // Loaded only for the currently selected Bill To customer.
    //
    // IMPORTANT:
    // Selecting a Challan lens here is presentation state only.
    // Challan / Inventory database status must NOT be permanently
    // changed until the Sales Invoice save transaction succeeds.
    // =========================================================

    val pendingChallans: List<ChallanEntity> = emptyList(),

    val pendingChallanItems: List<ChallanItemEntity> = emptyList(),

    val selectedChallanItemIds: Set<Long> = emptySet(),

    val challanSerialQuery: String = "",

    val challanSerialMatches: List<ChallanItemEntity> = emptyList(),

    val isChallanLoading: Boolean = false,

    val isChallanSerialSearching: Boolean = false,

    val showChallanSerialMatchSelection: Boolean = false,


    // =========================================================
    // CURRENT PRODUCT ENTRY
    // =========================================================

    val selectedProduct: ProductEntity? = null,

    val selectedPower: String = "",

    val selectedInventoryUnitIds: Set<Long> = emptySet(),

    val rate: String = "",

    val discountPercent: String = "",

    val gstPercent: String = "",


    // =========================================================
    // SALE ITEMS
    // =========================================================

    val items: List<SalesEntryItem> = emptyList(),


    // =========================================================
    // BILL SUMMARY
    // =========================================================

    val subTotal: Double = 0.0,

    val discountAmount: Double = 0.0,

    val taxableAmount: Double = 0.0,

    /*
     * Accounting total:
     *
     * INTRA_STATE:
     * gstAmount = cgstAmount + sgstAmount
     *
     * INTER_STATE:
     * gstAmount = igstAmount
     */
    val gstAmount: Double = 0.0,

    val cgstAmount: Double = 0.0,

    val sgstAmount: Double = 0.0,

    val igstAmount: Double = 0.0,

    val adjustment: Double = 0.0,

    val roundOff: Double = 0.0,

    val totalAmount: Double = 0.0,


    // =========================================================
    // SMART ENTRY / UI STATE
    // =========================================================

    val isLoading: Boolean = false,

    val isSaving: Boolean = false,

    val isDirty: Boolean = false,

    val isSaved: Boolean = false,

    val isSavedSuccessfully: Boolean = false,

    val savedSaleId: Long? = null,


    // =========================================================
    // VALIDATION / MESSAGE
    // =========================================================

    val errorMessage: String? = null,

    val successMessage: String? = null
) {


    // =========================================================
    // AVAILABLE POWERS
    // =========================================================
    //
    // Power options are derived only from physical IN_STOCK
    // inventory belonging to the selected Product.
    // =========================================================

    val availablePowers: List<String>
        get() {

            val productId =
                selectedProduct?.id
                    ?: return emptyList()

            return inStockUnits
                .asSequence()
                .filter {
                    it.productId == productId
                }
                .filter {
                    it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    ) || it.id in selectedInventoryUnitIds
                }
                .map {
                    it.power.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .sorted()
                .toList()
        }


    // =========================================================
    // AVAILABLE SERIALS
    // =========================================================

    val availableSerialUnits: List<InventoryUnitEntity>
        get() {

            val productId =
                selectedProduct?.id
                    ?: return emptyList()

            val inventoryIdsAlreadyAdded =
                items
                    .flatMap {
                        it.selectedUnits
                    }
                    .map {
                        it.inventoryUnitId
                    }
                    .toSet()

            return inStockUnits
                .asSequence()
                .filter {
                    it.productId == productId
                }
                .filter {
                    it.status.equals(
                        other = "IN_STOCK",
                        ignoreCase = true
                    )
                }
                .filter {

                    selectedPower.isBlank() ||
                            it.power
                                .trim()
                                .equals(
                                    other = selectedPower.trim(),
                                    ignoreCase = true
                                )
                }
                .filter {
                    it.id !in inventoryIdsAlreadyAdded
                }
                .sortedWith(
                    compareBy<InventoryUnitEntity> {
                        it.power
                    }.thenBy {
                        it.serialNumber
                    }
                )
                .toList()
        }


    // =========================================================
    // CURRENT SELECTED SERIALS
    // =========================================================

    val selectedSerialUnits: List<InventoryUnitEntity>
        get() {

            if (selectedInventoryUnitIds.isEmpty()) {
                return emptyList()
            }

            return inStockUnits
                .filter {
                    it.id in selectedInventoryUnitIds
                }
                .sortedBy {
                    it.serialNumber
                }
        }


    // =========================================================
    // SELECTED CHALLAN ITEMS
    // =========================================================

    val selectedChallanItems: List<ChallanItemEntity>
        get() {

            if (selectedChallanItemIds.isEmpty()) {
                return emptyList()
            }

            return pendingChallanItems
                .filter {
                    it.id in selectedChallanItemIds
                }
                .sortedWith(
                    compareBy<ChallanItemEntity> {
                        it.challanId
                    }.thenBy {
                        it.productName
                    }.thenBy {
                        it.power
                    }.thenBy {
                        it.serialNumber
                    }
                )
        }


    // =========================================================
    // CHALLAN SELECTION COUNT
    // =========================================================

    val selectedChallanItemCount: Int
        get() =
            selectedChallanItemIds.size


    // =========================================================
    // CURRENT ENTRY QUANTITY
    // =========================================================

    val currentQuantity: Int
        get() =
            selectedInventoryUnitIds.size
}


// =============================================================
// SALES ENTRY ITEM
// =============================================================

data class SalesEntryItem(

    val localId: Long,

    val productId: Long,

    val productName: String,

    val power: String = "",

    val quantity: Int,

    val rate: Double,

    val discountPercent: Double,

    val discountAmount: Double,

    val taxableAmount: Double,

    val gstPercent: Double,

    val gstAmount: Double,

    val totalAmount: Double,

    val selectedUnits: List<SalesSelectedInventoryUnit>
)


// =============================================================
// SELECTED PHYSICAL INVENTORY UNIT
// =============================================================

data class SalesSelectedInventoryUnit(

    val inventoryUnitId: Long,

    val serialNumber: String,

    val power: String = "",

    val batchNumber: String = "",

    val expiryDate: String = "",

    val sourceChallanItemId: Long? = null,

    val sourceSampleIssueItemId: Long? = null
)
