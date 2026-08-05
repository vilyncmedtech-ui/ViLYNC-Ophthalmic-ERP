package com.vilync.ophthalmicerp.feature.sales.register

enum class SalesRegisterType {
    INVOICE,
    CHALLAN,
    CREDIT_NOTE,
    PROFORMA,
    SAMPLE_ISSUE;

    val displayName: String
        get() = when (this) {
            INVOICE -> "Sales Invoice"
            CHALLAN -> "Delivery Challan"
            CREDIT_NOTE -> "Credit Note"
            PROFORMA -> "Proforma Invoice"
            SAMPLE_ISSUE -> "Sample Issue"
        }

    val shortName: String
        get() = when (this) {
            INVOICE -> "Invoice"
            CHALLAN -> "Challan"
            CREDIT_NOTE -> "Credit Note"
            PROFORMA -> "Proforma"
            SAMPLE_ISSUE -> "Sample"
        }

    val icon: Int
        get() = when (this) {
            INVOICE -> android.R.drawable.ic_menu_report_image
            CHALLAN -> android.R.drawable.ic_menu_send
            CREDIT_NOTE -> android.R.drawable.ic_menu_revert
            PROFORMA -> android.R.drawable.ic_menu_agenda
            SAMPLE_ISSUE -> android.R.drawable.ic_menu_share
        }

    val documentNumberField: String
        get() = when (this) {
            INVOICE -> "invoiceNumber"
            CHALLAN -> "challanNumber"
            CREDIT_NOTE -> "creditNoteNumber"
            PROFORMA -> "proformaNumber"
            SAMPLE_ISSUE -> "sampleIssueNumber"
        }

    val normalizedField: String
        get() = when (this) {
            INVOICE -> "normalizedInvoiceNumber"
            CHALLAN -> "normalizedChallanNumber"
            CREDIT_NOTE -> "normalizedCreditNoteNumber"
            PROFORMA -> "normalizedProformaNumber"
            SAMPLE_ISSUE -> "normalizedSampleIssueNumber"
        }

    val dateField: String
        get() = when (this) {
            INVOICE -> "invoiceDate"
            CHALLAN -> "challanDate"
            CREDIT_NOTE -> "creditNoteDate"
            PROFORMA -> "proformaDate"
            SAMPLE_ISSUE -> "sampleIssueDate"
        }

    val tableName: String
        get() = when (this) {
            INVOICE -> "sales"
            CHALLAN -> "challans"
            CREDIT_NOTE -> "sales_credit_notes"
            PROFORMA -> "proforma_invoices"
            SAMPLE_ISSUE -> "sample_issues"
        }

    val idField: String = "id"
    val customerIdField: String = "customerId"
    val customerNameField: String = "customerName"
    val statusField: String = "status"
    val financialYearField: String = "financialYearStart"
    val createdAtField: String = "createdAt"
    val updatedAtField: String = "updatedAt"
    val totalAmountField: String = "totalAmount"
    val taxableAmountField: String = "taxableAmount"
    val gstAmountField: String = "gstAmount"
    val cgstAmountField: String = "cgstAmount"
    val sgstAmountField: String = "sgstAmount"
    val igstAmountField: String = "igstAmount"

    val hasGst: Boolean
        get() = when (this) {
            INVOICE, CREDIT_NOTE, PROFORMA -> true
            CHALLAN, SAMPLE_ISSUE -> false
        }

    val isCancellable: Boolean
        get() = when (this) {
            INVOICE, CHALLAN, CREDIT_NOTE -> true
            PROFORMA, SAMPLE_ISSUE -> false
        }

    val isEditable: Boolean
        get() = when (this) {
            INVOICE, CHALLAN, CREDIT_NOTE, PROFORMA -> true
            SAMPLE_ISSUE -> false
        }

    val isExportable: Boolean
        get() = true

    val newDocumentRoute: String
        get() = when (this) {
            INVOICE -> "sales/invoice/new"
            CHALLAN -> "sales/challan/new"
            CREDIT_NOTE -> "sales/creditnote/new"
            PROFORMA -> "sales/proforma/new"
            SAMPLE_ISSUE -> "sales/sample/new"
        }

    fun getDetailRoute(id: Long): String = when (this) {
        INVOICE -> "sales/invoice/$id"
        CHALLAN -> "sales/challan/$id"
        CREDIT_NOTE -> "sales/creditnote/$id"
        PROFORMA -> "sales/proforma/$id"
        SAMPLE_ISSUE -> "sales/sample/$id"
    }

    fun getEditRoute(id: Long): String = when (this) {
        INVOICE -> "sales/invoice/edit/$id"
        CHALLAN -> "sales/challan/edit/$id"
        CREDIT_NOTE -> "sales/creditnote/edit/$id"
        PROFORMA -> "sales/proforma/edit/$id"
        SAMPLE_ISSUE -> "sales/sample/edit/$id"
    }

    fun getExportFileName(): String = when (this) {
        INVOICE -> "Sales_Invoice_Register"
        CHALLAN -> "Delivery_Challan_Register"
        CREDIT_NOTE -> "Credit_Note_Register"
        PROFORMA -> "Proforma_Register"
        SAMPLE_ISSUE -> "Sample_Issue_Register"
    }
}