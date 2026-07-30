package com.vilync.ophthalmicerp.feature.purchase.report

import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
import com.vilync.ophthalmicerp.feature.purchase.presentation.PurchaseUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// =============================================================
// PURCHASE INVOICE REPORT
// =============================================================

data class PurchaseInvoiceReport(

    // ---------------------------------------------------------
    // REPORT INFORMATION
    // ---------------------------------------------------------

    val title: String =
        "Purchase Invoice",

    val generatedAt: String = "",


    // ---------------------------------------------------------
    // PURCHASE INFORMATION
    // ---------------------------------------------------------

    val purchaseId: Long? = null,

    val supplierId: Long? = null,

    val supplierName: String = "",

    val supplierAddress: String = "",

    val supplierCity: String = "",

    val supplierDistrict: String = "",

    val supplierState: String = "",

    val supplierPinCode: String = "",

    val supplierGstin: String = "",


    // ---------------------------------------------------------
    // INVOICE HEADER
    // ---------------------------------------------------------

    val invoiceNumber: String = "",

    val invoiceDate: String = "",

    val receivedDate: String = "",

    val purchaseType: String = "",

    val paymentType: String = "",

    val creditDays: String = "",

    val reference: String = "",


    // ---------------------------------------------------------
    // PRODUCTS
    // ---------------------------------------------------------

    val items: List<PurchaseItem> =
        emptyList(),


    // ---------------------------------------------------------
    // BILL SUMMARY
    // ---------------------------------------------------------

    val grossAmount: Double = 0.0,

    val discountAmount: Double = 0.0,

    val taxableAmount: Double = 0.0,

    val taxAmount: Double = 0.0,

    val adjustmentAmount: Double = 0.0,

    val roundOffAmount: Double = 0.0,

    val netAmount: Double = 0.0,

    val paidAmount: Double = 0.0,

    val dueAmount: Double = 0.0

) {


    // =========================================================
    // SUPPLIER ADDRESS FOR REPORT
    // =========================================================

    val supplierFullAddress: String
        get() {

            return listOf(
                supplierAddress,
                supplierCity,
                supplierDistrict,
                supplierState,
                supplierPinCode
            )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    separator = ", "
                )
        }


    // =========================================================
    // TOTAL QUANTITY
    // =========================================================

    val totalQuantity: Int
        get() =
            items.sumOf {
                it.quantity
            }


    // =========================================================
    // TOTAL SERIAL UNITS
    // =========================================================

    val totalSerialUnits: Int
        get() =
            items.sumOf {
                it.lensDetails.size
            }


    companion object {


        // =====================================================
        // CREATE REPORT FROM PURCHASE UI STATE
        // =====================================================

        fun fromUiState(
            state: PurchaseUiState
        ): PurchaseInvoiceReport {

            val generatedAt =
                SimpleDateFormat(
                    "dd-MM-yyyy HH:mm",
                    Locale.getDefault()
                ).format(
                    Date()
                )


            return PurchaseInvoiceReport(

                generatedAt =
                    generatedAt,

                purchaseId =
                    state.editingPurchaseId,

                supplierId =
                    state.supplierId,

                supplierName =
                    state.supplierName.trim(),

                supplierAddress =
                    state.supplierAddress.trim(),

                supplierCity =
                    state.supplierCity.trim(),

                supplierDistrict =
                    state.supplierDistrict.trim(),

                supplierState =
                    state.supplierState.trim(),

                supplierPinCode =
                    state.supplierPinCode.trim(),

                supplierGstin =
                    state.supplierGstin.trim(),

                invoiceNumber =
                    state.invoiceNumber.trim(),

                invoiceDate =
                    state.invoiceDate.trim(),

                receivedDate =
                    state.receivedDate.trim(),

                purchaseType =
                    state.purchaseType.trim(),

                paymentType =
                    state.paymentType.trim(),

                creditDays =
                    state.creditDays.trim(),

                reference =
                    state.reference.trim(),

                items =
                    state.items,

                grossAmount =
                    state.grossAmount,

                discountAmount =
                    state.discountAmount,

                taxableAmount =
                    state.taxableAmount,

                taxAmount =
                    state.taxAmount,

                adjustmentAmount =
                    state.adjustmentAmount,

                roundOffAmount =
                    state.roundOffAmount,

                netAmount =
                    state.netAmount,

                paidAmount =
                    state.paidAmount,

                dueAmount =
                    state.dueAmount
            )
        }
    }
}