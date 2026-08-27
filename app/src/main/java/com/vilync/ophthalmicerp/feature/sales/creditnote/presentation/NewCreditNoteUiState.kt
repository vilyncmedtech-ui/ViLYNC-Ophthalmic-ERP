package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import com.vilync.ophthalmicerp.data.entity.SaleEntity

data class NewCreditNoteUiState(
    val editingId: Long? = null,
    val invoices: List<SaleEntity> = emptyList(),
    val invoiceQuery: String = "",
    val selectedInvoice: SaleEntity? = null,
    val creditNoteNumber: String = "",
    val creditNoteDate: String = "",
    val creditNoteType: String = "SALES_RETURN",
    val reason: String = "",
    val remarks: String = "",
    val adjustmentAmount: String = "",
    val invoiceLines: List<CreditNoteInvoiceLineUi> = emptyList(),
    val isLoadingInvoice: Boolean = false,
    val isSaving: Boolean = false,
    val savedCreditNoteId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class CreditNoteInvoiceLineUi(
    val saleItemId: Long,
    val saleLensId: Long,
    val inventoryUnitId: Long,
    val productId: Long,
    val productName: String,
    val serialNumber: String,
    val power: String,
    val batchNumber: String,
    val expiryDate: String,
    val rate: Double,
    val discountPercent: Double,
    val gstPercent: Double,
    val taxableAmount: Double,
    val gstAmount: Double,
    val totalAmount: Double,
    val selected: Boolean = false
)
